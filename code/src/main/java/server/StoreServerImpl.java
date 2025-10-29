package server;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import interfaces.StoreServer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import models.Item;
import models.Purchase;
import models.UDPRequest;
import models.UDPResponse;
import utils.DSMSLogger;

@WebService(
    serviceName = "StoreServerService",
    endpointInterface = "interfaces.StoreServer"
)
public class StoreServerImpl implements StoreServer {
    private final String storePrefix;
    private final Map<String, Item> inventory;
    private final Map<String, Queue<String>> waitlists;
    private final Map<String, Double> customerBudgets;
    private final Map<String, List<Purchase>> purchaseHistory;
    private final Map<String, ReentrantReadWriteLock> itemLocks;
    private final DSMSLogger logger;
    private final UDPCommunicator udpComm;
    
    public StoreServerImpl() {
        //Default constructor required by JAX-WS
        this.storePrefix = null;
        this.inventory = null;
        this.waitlists = null;
        this.customerBudgets = null;
        this.purchaseHistory = null;
        this.itemLocks = null;
        this.logger = null;
        this.udpComm = null;
    }
    
    public StoreServerImpl(String storePrefix, int udpPort) {
        this.storePrefix = storePrefix;
        this.inventory = new ConcurrentHashMap<>();
        this.waitlists = new ConcurrentHashMap<>();
        this.customerBudgets = new ConcurrentHashMap<>();
        this.purchaseHistory = new ConcurrentHashMap<>();
        this.itemLocks = new ConcurrentHashMap<>();
        this.logger = new DSMSLogger(storePrefix + "_server.log");
        this.udpComm = new UDPCommunicator(udpPort, this);
        
        initializeSampleData();
        udpComm.startUDPServer();
    }
    
    private void initializeSampleData() {
        inventory.put(storePrefix + "1001", new Item(storePrefix + "1001", "Coffee", 10, 5.99));
        inventory.put(storePrefix + "1002", new Item(storePrefix + "1002", "Tea", 15, 3.99));
        inventory.put(storePrefix + "1003", new Item(storePrefix + "1003", "Sugar", 20, 2.50));
    }
    
    @Override
    @WebMethod
    public String addItem(
        @WebParam(name = "managerID") String managerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "itemName") String itemName,
        @WebParam(name = "quantity") int quantity,
        @WebParam(name = "price") double price
    ) {
        if (!isValidManager(managerID)) {
            return "Invalid manager ID";
        }
        
        ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(itemID, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            Item existingItem = inventory.get(itemID);
            String result;
            
            if (existingItem != null) {
                existingItem.incrementQuantity(quantity);
                result = "Item quantity updated. New quantity: " + existingItem.getQuantity();
            } else {
                inventory.put(itemID, new Item(itemID, itemName, quantity, price));
                waitlists.put(itemID, new LinkedList<>());
                result = "Item added successfully";
            }
            
            processWaitlist(itemID);
            logger.logOperation("ADD_ITEM", managerID, itemID + "," + itemName + "," + quantity + "," + price, result);
            return result;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    @WebMethod
    public String removeItem(
        @WebParam(name = "managerID") String managerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "quantity") int quantity
    ) {
        if (!isValidManager(managerID)) {
            return "Invalid manager ID";
        }
        
        ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(itemID, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            Item item = inventory.get(itemID);
            if (item == null) {
                String result = "Item not found";
                logger.logOperation("REMOVE_ITEM", managerID, itemID + "," + quantity, result);
                return result;
            }
            
            String result;
            if (quantity >= item.getQuantity()) {
                item.setQuantity(0);
                result = "Item quantity set to 0. Item remains in inventory for waitlist.";
            } else {
                item.setQuantity(item.getQuantity() - quantity);
                result = "Item quantity reduced. New quantity: " + item.getQuantity();
            }
            
            logger.logOperation("REMOVE_ITEM", managerID, itemID + "," + quantity, result);
            return result;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    @WebMethod
    public String listItemAvailability(
        @WebParam(name = "managerID") String managerID
    ) {
        if (!isValidManager(managerID)) {
            return "Invalid manager ID";
        }
        
        StringBuilder result = new StringBuilder("Store " + storePrefix + " Inventory:\n");
        
        for (Item item : inventory.values()) {
            ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(item.getItemID(), k -> new ReentrantReadWriteLock());
            lock.readLock().lock();
            try {
                result.append(item.toString()).append("\n");
            } finally {
                lock.readLock().unlock();
            }
        }
        
        logger.logOperation("LIST_ITEMS", managerID, "", "Listed " + inventory.size() + " items");
        return result.toString();
    }
    
    @Override
    @WebMethod
    public String purchaseItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "quantity") int quantity,
        @WebParam(name = "dateOfPurchase") String dateOfPurchase
    ) {
        if (!isValidCustomer(customerID)) {
            return "Invalid customer ID";
        }

        if (quantity <= 0) {
            return "Invalid quantity. Must be greater than 0.";
        }

        customerBudgets.putIfAbsent(customerID, 1000.0);

        if (itemID.startsWith(storePrefix)) {
            return purchaseLocalItem(customerID, itemID, quantity, dateOfPurchase);
        } else {
            return purchaseRemoteItem(customerID, itemID, quantity, dateOfPurchase);
        }
    }
    
    @Override
    @WebMethod
    public String findItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemName") String itemName
    ) {
        if (!isValidCustomer(customerID)) {
            return "Invalid customer ID";
        }
        
        List<Item> allFoundItems = new ArrayList<>();
        List<Item> localItems = findLocalItems(itemName);
        allFoundItems.addAll(localItems);
        
        for (String store : Arrays.asList("QC", "ON", "BC")) {
            if (!store.equals(storePrefix)) {
                UDPRequest request = new UDPRequest(customerID, itemName);
                UDPResponse response = udpComm.sendMarshalledRequest(store, request);
                
                if (response.isSuccess() && response.getFoundItems() != null) {
                    allFoundItems.addAll(response.getFoundItems());
                }
            }
        }
        
        if (allFoundItems.isEmpty()) {
            return "No items found with name: " + itemName;
        }
        
        StringBuilder result = new StringBuilder("Found items:\n");
        for (Item item : allFoundItems) {
            result.append(item.toString()).append("\n");
        }
        
        logger.logOperation("FIND_ITEM", customerID, itemName, "Found " + allFoundItems.size() + " items");
        return result.toString();
    }
    
    @Override
    @WebMethod
    public String returnItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "dateOfReturn") String dateOfReturn
    ) {
        if (!isValidCustomer(customerID)) {
            return "Invalid customer ID";
        }
        
        List<Purchase> purchases = purchaseHistory.get(customerID);
        if (purchases == null) {
            return "No purchase history found";
        }
        
        Purchase targetPurchase = null;
        for (Purchase purchase : purchases) {
            if (purchase.getItemID().equals(itemID) && purchase.getCustomerID().equals(customerID)) {
                targetPurchase = purchase;
                break;
            }
        }
        
        if (targetPurchase == null) {
            return "Purchase record not found";
        }
        
        if (!targetPurchase.canReturn(dateOfReturn)) {
            return "Return period expired (30 days limit)";
        }
        
        ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(itemID, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            Item item = inventory.get(itemID);
            if (item != null) {
                item.incrementQuantity(1);
            }
            
            double currentBudget = customerBudgets.get(customerID);
            customerBudgets.put(customerID, currentBudget + targetPurchase.getPrice());
            purchases.remove(targetPurchase);
            
            String result = "Return successful. Refunded: $" + targetPurchase.getPrice();
            logger.logOperation("RETURN", customerID, itemID + "," + dateOfReturn, result);
            return result;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    @WebMethod
    public String exchangeItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "newItemID") String newItemID,
        @WebParam(name = "oldItemID") String oldItemID
    ) {
        if (!isValidCustomer(customerID)) {
            return "Invalid customer ID";
        }
        
        logger.logOperation("EXCHANGE_START", customerID, "newItem=" + newItemID + ",oldItem=" + oldItemID, "Started");
        
        List<Purchase> purchases = purchaseHistory.get(customerID);
        if (purchases == null) {
            return "ERROR: No purchase history found";
        }
        
        Purchase oldPurchase = null;
        for (Purchase purchase : purchases) {
            if (purchase.getItemID().equals(oldItemID) && purchase.getCustomerID().equals(customerID)) {
                oldPurchase = purchase;
                break;
            }
        }
        
        if (oldPurchase == null) {
            return "ERROR: Old item not found in purchase history";
        }
        
        String currentDate = getCurrentDate();
        if (!oldPurchase.canReturn(currentDate)) {
            return "ERROR: Exchange period expired (30 days limit)";
        }
        
        String newItemStore = newItemID.substring(0, 2);
        String oldItemStore = oldItemID.substring(0, 2);
        
        if (newItemStore.equals(storePrefix)) {
            return executeLocalExchange(customerID, newItemID, oldItemID, oldPurchase, oldItemStore);
        } else {
            return executeCrossStoreExchange(customerID, newItemID, oldItemID, oldPurchase, newItemStore, oldItemStore);
        }
    }
    
    private String executeLocalExchange(String customerID, String newItemID, String oldItemID, 
                                       Purchase oldPurchase, String oldItemStore) {
        String firstLockItem = newItemID.compareTo(oldItemID) < 0 ? newItemID : oldItemID;
        String secondLockItem = newItemID.compareTo(oldItemID) < 0 ? oldItemID : newItemID;
        
        ReentrantReadWriteLock lock1 = itemLocks.computeIfAbsent(firstLockItem, k -> new ReentrantReadWriteLock());
        ReentrantReadWriteLock lock2 = itemLocks.computeIfAbsent(secondLockItem, k -> new ReentrantReadWriteLock());
        
        lock1.writeLock().lock();
        try {
            lock2.writeLock().lock();
            try {
                Item newItem = inventory.get(newItemID);
                if (newItem == null) {
                    return "ERROR: New item not found";
                }
                
                if (newItem.getQuantity() <= 0) {
                    return "ERROR: New item out of stock";
                }
                
                double priceDifference = newItem.getPrice() - oldPurchase.getPrice();
                double currentBudget = customerBudgets.get(customerID);
                
                if (priceDifference > 0 && currentBudget < priceDifference) {
                    return "ERROR: Insufficient budget. Need additional $" + priceDifference;
                }
                
                if (!oldItemStore.equals(storePrefix) && !canPurchaseFromOtherStore(customerID, newItemID)) {
                    return "ERROR: Already purchased from this store";
                }
                
                if (oldItemStore.equals(storePrefix)) {
                    Item oldItem = inventory.get(oldItemID);
                    if (oldItem != null) {
                        oldItem.incrementQuantity(1);
                    }
                }
                
                newItem.decrementQuantity();
                
                customerBudgets.put(customerID, currentBudget - priceDifference);
                
                purchaseHistory.get(customerID).remove(oldPurchase);
                purchaseHistory.get(customerID).add(new Purchase(customerID, newItemID, getCurrentDate(), newItem.getPrice()));
                
                String result = "Exchange successful. " + 
                               (priceDifference > 0 ? "Paid $" + priceDifference : "Refunded $" + Math.abs(priceDifference));
                logger.logOperation("EXCHANGE", customerID, newItemID + "," + oldItemID, result);
                return result;
                
            } finally {
                lock2.writeLock().unlock();
            }
        } finally {
            lock1.writeLock().unlock();
        }
    }
    
    private String executeCrossStoreExchange(String customerID, String newItemID, String oldItemID,
                                            Purchase oldPurchase, String newItemStore, String oldItemStore) {
        double currentBudget = customerBudgets.get(customerID);
        double priceDifference = currentBudget;
        
        UDPRequest checkRequest = new UDPRequest(customerID, newItemID, oldItemID, currentBudget, oldPurchase.getPrice());
        UDPResponse checkResponse = udpComm.sendMarshalledRequest(newItemStore, checkRequest);
        
        if (!checkResponse.isSuccess()) {
            return "ERROR: " + checkResponse.getMessage();
        }
        
        UDPRequest prepareRequest = new UDPRequest("EXCHANGE_PREPARE", customerID, newItemID, oldItemID, currentBudget, oldPurchase.getPrice());
        UDPResponse prepareResponse = udpComm.sendMarshalledRequest(newItemStore, prepareRequest);
        
        if (!prepareResponse.isSuccess()) {
            return "ERROR: Exchange preparation failed: " + prepareResponse.getMessage();
        }
        
        boolean oldItemReturned = false;
        if (!oldItemStore.equals(storePrefix)) {
            UDPRequest returnRequest = new UDPRequest("EXCHANGE_RETURN", customerID, oldItemID, getCurrentDate());
            UDPResponse returnResponse = udpComm.sendMarshalledRequest(oldItemStore, returnRequest);
            
            if (!returnResponse.isSuccess()) {
                udpComm.sendMarshalledRequest(newItemStore, 
                    new UDPRequest("EXCHANGE_ROLLBACK", customerID, newItemID, oldItemID));
                return "ERROR: Failed to return old item: " + returnResponse.getMessage();
            }
            oldItemReturned = true;
        } else {
            ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(oldItemID, k -> new ReentrantReadWriteLock());
            lock.writeLock().lock();
            try {
                Item oldItem = inventory.get(oldItemID);
                if (oldItem != null) {
                    oldItem.incrementQuantity(1);
                }
            } finally {
                lock.writeLock().unlock();
            }
            oldItemReturned = true;
        }
        
        UDPRequest commitRequest = new UDPRequest("EXCHANGE_COMMIT", customerID, newItemID, oldItemID, currentBudget, oldPurchase.getPrice());
        UDPResponse commitResponse = udpComm.sendMarshalledRequest(newItemStore, commitRequest);
        
        if (!commitResponse.isSuccess()) {
            if (oldItemReturned) {
                if (!oldItemStore.equals(storePrefix)) {
                    udpComm.sendMarshalledRequest(oldItemStore,
                        new UDPRequest("EXCHANGE_UNDO_RETURN", customerID, oldItemID, getCurrentDate()));
                } else {
                    ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(oldItemID, k -> new ReentrantReadWriteLock());
                    lock.writeLock().lock();
                    try {
                        Item oldItem = inventory.get(oldItemID);
                        if (oldItem != null) {
                            oldItem.decrementQuantity();
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }
            }
            return "ERROR: Exchange commit failed: " + commitResponse.getMessage();
        }
        
        customerBudgets.put(customerID, commitResponse.getNewBudget());
        purchaseHistory.get(customerID).remove(oldPurchase);
        
        double newItemPrice = currentBudget - commitResponse.getNewBudget() + oldPurchase.getPrice();
        purchaseHistory.get(customerID).add(new Purchase(customerID, newItemID, getCurrentDate(), newItemPrice));
        
        priceDifference = newItemPrice - oldPurchase.getPrice();
        String result = "Exchange successful. " + 
                       (priceDifference > 0 ? "Paid $" + priceDifference : "Refunded $" + Math.abs(priceDifference));
        logger.logOperation("EXCHANGE", customerID, newItemID + "," + oldItemID, result);
        return result;
    }
    
    @Override
    @WebMethod
    public String addToWaitlist(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID
    ) {
        return handleWaitlist(customerID, itemID);
    }
    
    @Override
    @WebMethod
    public String getStorePrefix() {
        return storePrefix;
    }
    
    private String purchaseLocalItem(String customerID, String itemID, int quantity, String dateOfPurchase) {
        ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(itemID, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            Item item = inventory.get(itemID);
            if (item == null) {
                return "Item not found";
            }
            
            if (item.getQuantity() < quantity) {
                if (item.getQuantity() == 0) {
                    return "WAITLIST_PROMPT," + itemID;
                }
                return "Insufficient quantity. Available: " + item.getQuantity() + ", Requested: " + quantity;
            }
            
            double totalCost = item.getPrice() * quantity;
            double customerBudget = customerBudgets.get(customerID);
            
            if (customerBudget < totalCost) {
                return "Insufficient budget. Available: $" + String.format("%.2f", customerBudget) + 
                       ", Required: $" + String.format("%.2f", totalCost);
            }
            
            if (!canPurchaseFromStore(customerID, itemID)) {
                return "Purchase limit exceeded for this store";
            }
            
            item.setQuantity(item.getQuantity() - quantity);
            
            customerBudgets.put(customerID, customerBudget - totalCost);
            
            purchaseHistory.computeIfAbsent(customerID, k -> new ArrayList<>())
                          .add(new Purchase(customerID, itemID, dateOfPurchase, totalCost));
            
            String result = "Purchase successful! Bought " + quantity + " x " + item.getItemName() + 
                           " for $" + String.format("%.2f", totalCost) + 
                           ". Remaining budget: $" + String.format("%.2f", customerBudgets.get(customerID));
            logger.logOperation("PURCHASE", customerID, itemID + "," + quantity + "," + dateOfPurchase, result);
            return result;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    private String purchaseRemoteItem(String customerID, String itemID, int quantity, String dateOfPurchase) {
        String targetStore = itemID.substring(0, 2);
        double customerBudget = customerBudgets.get(customerID);
        
        UDPRequest request = new UDPRequest(customerID, itemID, quantity, dateOfPurchase, customerBudget);
        UDPResponse response = udpComm.sendMarshalledRequest(targetStore, request);
        
        if (response.isSuccess()) {
            customerBudgets.put(customerID, response.getNewBudget());
            double purchasePrice = customerBudget - response.getNewBudget();
            purchaseHistory.computeIfAbsent(customerID, k -> new ArrayList<>())
                        .add(new Purchase(customerID, itemID, dateOfPurchase, purchasePrice));
            
            String result = "Purchase successful. Remaining budget: $" + String.format("%.2f", response.getNewBudget());
            logger.logOperation("REMOTE_PURCHASE", customerID, itemID + "," + quantity + "," + dateOfPurchase, result);
            return result;
        } else {
            String result = "Purchase failed: " + response.getMessage();
            logger.logOperation("REMOTE_PURCHASE_FAILED", customerID, itemID + "," + quantity + "," + dateOfPurchase, result);
            return result;
        }
    }
    
    public String processRemotePurchase(String customerID, String itemID, int quantity, String date, double customerBudget) {
        ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(itemID, k -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        
        try {
            Item item = inventory.get(itemID);
            if (item == null) {
                return "ERROR,Item not found";
            }
            
            if (item.getQuantity() < quantity) {
                return "ERROR,Insufficient quantity. Available: " + item.getQuantity();
            }
            
            double totalCost = item.getPrice() * quantity;
            if (customerBudget < totalCost) {
                return "ERROR,Insufficient budget";
            }
            
            List<Purchase> purchases = purchaseHistory.get(customerID);
            if (purchases != null) {
                for (Purchase purchase : purchases) {
                    if (purchase.getItemID().startsWith(storePrefix)) {
                        return "ERROR,Already purchased from " + storePrefix + " store. Limit: 1 item per remote store.";
                    }
                }
            }
            
            item.setQuantity(item.getQuantity() - quantity);
            double newBudget = customerBudget - totalCost;
            
            purchaseHistory.computeIfAbsent(customerID, k -> new ArrayList<>())
                          .add(new Purchase(customerID, itemID, date, totalCost));
            
            logger.logOperation("REMOTE_PURCHASE", customerID, itemID + "," + quantity + "," + date, "SUCCESS");
            return "SUCCESS," + newBudget;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public List<Item> findLocalItems(String itemName) {
        List<Item> foundItems = new ArrayList<>();
        
        for (Item item : inventory.values()) {
            ReentrantReadWriteLock lock = itemLocks.computeIfAbsent(item.getItemID(), k -> new ReentrantReadWriteLock());
            lock.readLock().lock();
            try {
                if (item.getItemName().equalsIgnoreCase(itemName) && item.getQuantity() > 0) {
                    Item itemCopy = new Item(item.getItemID(), item.getItemName(), item.getQuantity(), item.getPrice());
                    foundItems.add(itemCopy);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        
        return foundItems;
    }
    
    private boolean canPurchaseFromOtherStore(String customerID, String itemID) {
        String itemStore = itemID.substring(0, 2);
        String customerStore = customerID.substring(0, 2);
        
        if (itemStore.equals(customerStore)) {
            return true;
        }
        
        List<Purchase> purchases = purchaseHistory.get(customerID);
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                String purchaseStore = purchase.getItemID().substring(0, 2);
                if (purchaseStore.equals(itemStore)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public String getLocalStorePrefix() {
        return this.storePrefix;
    }
    
    private boolean isValidManager(String managerID) {
        return managerID.startsWith(storePrefix + "M") && managerID.length() == 7;
    }
    
    private boolean isValidCustomer(String customerID) {
        // Allow customers from any store (for cross-store operations)
        return customerID.matches("[A-Z]{2}U\\d{4}");
    }
    
    private String handleWaitlist(String customerID, String itemID) {
        Queue<String> waitlist = waitlists.computeIfAbsent(itemID, k -> new LinkedList<>());
        if (!waitlist.contains(customerID)) {
            waitlist.offer(customerID);
            return "Item out of stock. Added to waitlist. Position: " + waitlist.size();
        }
        return "Already in waitlist for this item";
    }
    
    private void processWaitlist(String itemID) {
        Queue<String> waitlist = waitlists.get(itemID);
        Item item = inventory.get(itemID);
        
        if (waitlist != null && item != null && item.getQuantity() > 0) {
            String customerID = waitlist.poll();
            if (customerID != null) {
                purchaseLocalItem(customerID, itemID, 1, getCurrentDate());
            }
        }
    }
    
    private boolean canPurchaseFromStore(String customerID, String itemID) {
        String itemStore = itemID.substring(0, 2);
        String customerStore = customerID.substring(0, 2);
        
        if (itemStore.equals(customerStore)) {
            return true;
        }
        
        List<Purchase> purchases = purchaseHistory.get(customerID);
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase.getItemID().startsWith(itemStore)) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private String getCurrentDate() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy"));
    }
    
    public Item getItem(String itemID) {
        return inventory.get(itemID);
    }
    
    public List<Purchase> getCustomerPurchases(String customerID) {
        return purchaseHistory.get(customerID);
    }
}
