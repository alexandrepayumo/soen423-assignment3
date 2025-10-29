package client;

import java.net.URL;
import java.util.Scanner;
import utils.DSMSLogger;

public class CustomerClient {
    private static DSMSLogger clientLogger;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter Customer ID (e.g., QCU1111): ");
        String customerID = scanner.nextLine().trim();
        
        clientLogger = new DSMSLogger(customerID + "_client.log");
        
        try {
            //Determine store prefix and connect to appropriate web service
            String storePrefix = customerID.substring(0, 2).toUpperCase();
            Object server = connectToWebService(storePrefix);
            
            if (server == null) {
                System.err.println("ERROR: Could not connect to " + storePrefix + " server");
                return;
            }
            
            System.out.println("Connected to " + storePrefix + " Store Server");
            
            boolean exit = false;
            while (!exit) {
                System.out.println("\n=== Customer Operations ===");
                System.out.println("1. Purchase Item");
                System.out.println("2. Find Item");
                System.out.println("3. Return Item");
                System.out.println("4. Exchange Item");
                System.out.println("5. Exit");
                System.out.print("Choose option: ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        purchaseItem(server, storePrefix, customerID, scanner);
                        break;
                    case "2":
                        findItem(server, storePrefix, customerID, scanner);
                        break;
                    case "3":
                        returnItem(server, storePrefix, customerID, scanner);
                        break;
                    case "4":
                        exchangeItem(server, storePrefix, customerID, scanner);
                        break;
                    case "5":
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid choice");
                }
            }
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        scanner.close();
    }
    
    private static Object connectToWebService(String storePrefix) {
        try {
            String wsdlUrl;
            switch (storePrefix) {
                case "QC":
                    wsdlUrl = "http://localhost:8080/QCServer?wsdl";
                    client.generated.qc.StoreServerService qcService = new client.generated.qc.StoreServerService(new URL(wsdlUrl));
                    return qcService.getStoreServerImplPort();
                case "ON":
                    wsdlUrl = "http://localhost:8081/ONServer?wsdl";
                    client.generated.on.StoreServerService onService = new client.generated.on.StoreServerService(new URL(wsdlUrl));
                    return onService.getStoreServerImplPort();
                case "BC":
                    wsdlUrl = "http://localhost:8082/BCServer?wsdl";
                    client.generated.bc.StoreServerService bcService = new client.generated.bc.StoreServerService(new URL(wsdlUrl));
                    return bcService.getStoreServerImplPort();
                default:
                    System.err.println("Unknown store prefix: " + storePrefix);
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error connecting to web service: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    private static void purchaseItem(Object serverObj, String storePrefix, String customerID, Scanner scanner) {
        try {
            System.out.print("Item ID: ");
            String itemID = scanner.nextLine().trim();
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Date (ddmmyyyy): ");
            String date = scanner.nextLine().trim();
            
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).purchaseItem(customerID, itemID, quantity, date);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).purchaseItem(customerID, itemID, quantity, date);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).purchaseItem(customerID, itemID, quantity, date);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            if (result.startsWith("WAITLIST_PROMPT")) {
                itemID = result.split(",")[1];
                System.out.print("Item out of stock. Add to waitlist? (y/n): ");
                String choice = scanner.nextLine().trim().toLowerCase();
                
                if (choice.equals("y") || choice.equals("yes")) {
                    String waitlistResult;
                    switch (storePrefix) {
                        case "QC":
                            waitlistResult = ((client.generated.qc.StoreServer)serverObj).addToWaitlist(customerID, itemID);
                            break;
                        case "ON":
                            waitlistResult = ((client.generated.on.StoreServer)serverObj).addToWaitlist(customerID, itemID);
                            break;
                        case "BC":
                            waitlistResult = ((client.generated.bc.StoreServer)serverObj).addToWaitlist(customerID, itemID);
                            break;
                        default:
                            waitlistResult = "Error: Unknown store";
                    }
                    System.out.println(waitlistResult);
                    clientLogger.logOperation("WAITLIST", customerID, itemID, waitlistResult);
                } else {
                    System.out.println("Purchase cancelled.");
                }
            } else {
                System.out.println("Result: " + result);
                clientLogger.logOperation("PURCHASE", customerID, itemID + "," + date, result);
            }
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void findItem(Object serverObj, String storePrefix, String customerID, Scanner scanner) {
        try {
            System.out.print("Item Name: ");
            String itemName = scanner.nextLine().trim();
            
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).findItem(customerID, itemName);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).findItem(customerID, itemName);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).findItem(customerID, itemName);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println(result);
            clientLogger.logOperation("FIND_ITEM", customerID, itemName, result);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void returnItem(Object serverObj, String storePrefix, String customerID, Scanner scanner) {
        try {
            System.out.print("Item ID: ");
            String itemID = scanner.nextLine().trim();
            System.out.print("Return Date (ddmmyyyy): ");
            String date = scanner.nextLine().trim();
            
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).returnItem(customerID, itemID, date);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).returnItem(customerID, itemID, date);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).returnItem(customerID, itemID, date);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println("Result: " + result);
            clientLogger.logOperation("RETURN", customerID, itemID + "," + date, result);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void exchangeItem(Object serverObj, String storePrefix, String customerID, Scanner scanner) {
        try {
            System.out.print("Old Item ID (item to exchange): ");
            String oldItemID = scanner.nextLine().trim();
            System.out.print("New Item ID (item to get): ");
            String newItemID = scanner.nextLine().trim();
            
            System.out.println("Processing exchange...");
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).exchangeItem(customerID, newItemID, oldItemID);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).exchangeItem(customerID, newItemID, oldItemID);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).exchangeItem(customerID, newItemID, oldItemID);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println("Result: " + result);
            clientLogger.logOperation("EXCHANGE", customerID, newItemID + "," + oldItemID, result);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
