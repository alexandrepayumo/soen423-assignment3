package server;

import models.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UDPCommunicator {
    private final int port;
    private final StoreServerImpl server;
    private DatagramSocket socket;
    private final Map<String, Integer> storePorts;
    private final Map<String, ExchangeTransaction> pendingExchanges;
    
    public UDPCommunicator(int port, StoreServerImpl server) {
        this.port = port;
        this.server = server;
        this.pendingExchanges = new ConcurrentHashMap<>();
        this.storePorts = new HashMap<>();
        storePorts.put("QC", 8001);
        storePorts.put("ON", 8002);
        storePorts.put("BC", 8003);
    }
    
    public void startUDPServer() {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(port);
                byte[] buffer = new byte[65536];
                
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    new Thread(() -> handleRequest(packet)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private void handleRequest(DatagramPacket packet) {
        try {
            String received = new String(packet.getData(), 0, packet.getLength());
            UDPRequest request = UDPRequest.fromString(received);
            
            UDPResponse response = processMarshalledRequest(request);
            
            String responseStr = response.toString();
            byte[] responseData = responseStr.getBytes();
            
            DatagramPacket responsePacket = new DatagramPacket(
                responseData, 
                responseData.length,
                packet.getAddress(),
                packet.getPort()
            );
            
            socket.send(responsePacket);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public UDPResponse sendMarshalledRequest(String storePrefix, UDPRequest request) {
        try {
            Integer targetPort = storePorts.get(storePrefix);
            if (targetPort == null) {
                return new UDPResponse(false, "Unknown store: " + storePrefix, "UNKNOWN_STORE");
            }
            
            String requestStr = request.toString();
            byte[] sendData = requestStr.getBytes();
            
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(5000);
            
            InetAddress address = InetAddress.getByName("localhost");
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, targetPort);
            clientSocket.send(sendPacket);
            
            byte[] receiveData = new byte[65536];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            
            String responseStr = new String(receivePacket.getData(), 0, receivePacket.getLength());
            UDPResponse response = UDPResponse.fromString(responseStr);
            
            clientSocket.close();
            return response;
            
        } catch (SocketTimeoutException e) {
            return new UDPResponse(false, "Request timeout", "TIMEOUT");
        } catch (Exception e) {
            return new UDPResponse(false, "Communication error: " + e.getMessage(), "COMM_ERROR");
        }
    }
    
    private UDPResponse processMarshalledRequest(UDPRequest request) {
        try {
            switch (request.getOperation()) {
                case "PURCHASE":
                    return processPurchaseRequest(request);
                case "FIND":
                    return processFindRequest(request);
                case "EXCHANGE_CHECK":
                    return processExchangeCheck(request);
                case "EXCHANGE_PREPARE":
                    return processExchangePrepare(request);
                case "EXCHANGE_COMMIT":
                    return processExchangeCommit(request);
                case "EXCHANGE_ROLLBACK":
                    return processExchangeRollback(request);
                case "EXCHANGE_RETURN":
                    return processExchangeReturn(request);
                case "EXCHANGE_UNDO_RETURN":
                    return processExchangeUndoReturn(request);
                default:
                    return new UDPResponse(false, "Unknown operation: " + request.getOperation(), "UNKNOWN_OP");
            }
        } catch (Exception e) {
            return new UDPResponse(false, "Processing error: " + e.getMessage(), "PROCESSING_ERROR");
        }
    }
    
    private UDPResponse processPurchaseRequest(UDPRequest request) {
        String result = server.processRemotePurchase(
            request.getCustomerID(),
            request.getItemID(),
            request.getQuantity(),
            request.getDate(),
            request.getBudget()
        );
        
        if (result.startsWith("SUCCESS")) {
            String[] parts = result.split(",");
            double newBudget = Double.parseDouble(parts[1]);
            return new UDPResponse(true, "Purchase successful", newBudget);
        } else {
            String[] parts = result.split(",");
            return new UDPResponse(false, parts[1], "PURCHASE_FAILED");
        }
    }
    
    private UDPResponse processFindRequest(UDPRequest request) {
        List<Item> items = server.findLocalItems(request.getItemName());
        return new UDPResponse(true, "Items found", items);
    }
    
    private UDPResponse processExchangeCheck(UDPRequest request) {
        String customerID = request.getCustomerID();
        String newItemID = request.getItemID();
        double currentBudget = request.getBudget();
        double oldItemPrice = request.getOldItemPrice();
        
        Item newItem = server.getItem(newItemID);
        if (newItem == null) {
            return new UDPResponse(false, "New item not found", "ITEM_NOT_FOUND");
        }
        
        if (newItem.getQuantity() <= 0) {
            return new UDPResponse(false, "New item out of stock", "OUT_OF_STOCK");
        }
        
        double priceDifference = newItem.getPrice() - oldItemPrice;
        if (priceDifference > 0 && currentBudget < priceDifference) {
            return new UDPResponse(false, "Insufficient budget for exchange", "INSUFFICIENT_BUDGET");
        }
        
        String customerStore = customerID.substring(0, 2);
        String newItemStore = newItemID.substring(0, 2);
        
        if (!customerStore.equals(newItemStore)) {
            List<Purchase> purchases = server.getCustomerPurchases(customerID);
            if (purchases != null) {
                for (Purchase p : purchases) {
                    if (p.getItemID().startsWith(newItemStore)) {
                        return new UDPResponse(false, "Already purchased from this store", "PURCHASE_LIMIT");
                    }
                }
            }
        }
        
        double newBudget = currentBudget - priceDifference;
        return new UDPResponse(true, "Exchange check passed", newBudget, newItem.getPrice());
    }
    
    private UDPResponse processExchangePrepare(UDPRequest request) {
        String customerID = request.getCustomerID();
        String newItemID = request.getItemID();
        String transactionID = UUID.randomUUID().toString();
        
        Item newItem = server.getItem(newItemID);
        if (newItem == null || newItem.getQuantity() <= 0) {
            return new UDPResponse(false, "Item unavailable", "UNAVAILABLE");
        }
        
        synchronized (newItem) {
            if (newItem.getQuantity() <= 0) {
                return new UDPResponse(false, "Item out of stock", "OUT_OF_STOCK");
            }
            newItem.decrementQuantity();
        }
        
        pendingExchanges.put(transactionID, new ExchangeTransaction(customerID, newItemID, request.getOldItemID()));
        
        return new UDPResponse(true, "Item reserved", 0.0, transactionID);
    }
    
    private UDPResponse processExchangeCommit(UDPRequest request) {
        String customerID = request.getCustomerID();
        String newItemID = request.getItemID();
        double budget = request.getBudget();
        double oldItemPrice = request.getOldItemPrice();
        
        Item newItem = server.getItem(newItemID);
        if (newItem == null) {
            return new UDPResponse(false, "Item not found", "NOT_FOUND");
        }
        
        double priceDifference = newItem.getPrice() - oldItemPrice;
        double newBudget = budget - priceDifference;
        
        pendingExchanges.values().removeIf(t -> 
            t.customerID.equals(customerID) && t.newItemID.equals(newItemID));
        
        return new UDPResponse(true, "Exchange committed", newBudget);
    }
    
    private UDPResponse processExchangeRollback(UDPRequest request) {
        String newItemID = request.getItemID();
        
        Item newItem = server.getItem(newItemID);
        if (newItem != null) {
            synchronized (newItem) {
                newItem.incrementQuantity(1);
            }
        }
        
        pendingExchanges.values().removeIf(t -> t.newItemID.equals(newItemID));
        
        return new UDPResponse(true, "Exchange rolled back", "ROLLBACK");
    }
    
    private UDPResponse processExchangeReturn(UDPRequest request) {
        String customerID = request.getCustomerID();
        String oldItemID = request.getItemID();
        
        Item oldItem = server.getItem(oldItemID);
        if (oldItem != null) {
            synchronized (oldItem) {
                oldItem.incrementQuantity(1);
            }
            return new UDPResponse(true, "Item returned", "SUCCESS");
        }
        
        return new UDPResponse(false, "Failed to return item", "RETURN_FAILED");
    }
    
    private UDPResponse processExchangeUndoReturn(UDPRequest request) {
        String oldItemID = request.getItemID();
        
        Item oldItem = server.getItem(oldItemID);
        if (oldItem != null) {
            synchronized (oldItem) {
                if (oldItem.getQuantity() > 0) {
                    oldItem.decrementQuantity();
                }
            }
            return new UDPResponse(true, "Return undone", "SUCCESS");
        }
        
        return new UDPResponse(false, "Failed to undo return", "UNDO_FAILED");
    }
    
    private static class ExchangeTransaction {
        String customerID;
        String newItemID;
        String oldItemID;
        long timestamp;
        
        ExchangeTransaction(String customerID, String newItemID, String oldItemID) {
            this.customerID = customerID;
            this.newItemID = newItemID;
            this.oldItemID = oldItemID;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
