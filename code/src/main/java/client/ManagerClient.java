package client;

import java.net.URL;
import java.util.Scanner;
import utils.DSMSLogger;

public class ManagerClient {
    private static DSMSLogger clientLogger;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.print("Enter Manager ID (e.g., QCM1111): ");
        String managerID = scanner.nextLine().trim();
        
        clientLogger = new DSMSLogger(managerID + "_client.log");
        
        try {
            //Determine store prefix and connect to appropriate web service
            String storePrefix = managerID.substring(0, 2).toUpperCase();
            Object server = connectToWebService(storePrefix);
            
            if (server == null) {
                System.err.println("ERROR: Could not connect to " + storePrefix + " server");
                return;
            }
            
            System.out.println("Connected to " + storePrefix + " Store Server");
            
            boolean exit = false;
            while (!exit) {
                System.out.println("\n=== Manager Operations ===");
                System.out.println("1. Add Item");
                System.out.println("2. Remove Item");
                System.out.println("3. List Items");
                System.out.println("4. Exit");
                System.out.print("Choose option: ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        addItem(server, storePrefix, managerID, scanner);
                        break;
                    case "2":
                        removeItem(server, storePrefix, managerID, scanner);
                        break;
                    case "3":
                        listItems(server, storePrefix, managerID);
                        break;
                    case "4":
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
    
    private static void addItem(Object serverObj, String storePrefix, String managerID, Scanner scanner) {
        try {
            System.out.print("Item ID: ");
            String itemID = scanner.nextLine().trim();
            System.out.print("Item Name: ");
            String itemName = scanner.nextLine().trim();
            System.out.print("Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Price: ");
            double price = Double.parseDouble(scanner.nextLine().trim());
            
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).addItem(managerID, itemID, itemName, quantity, price);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).addItem(managerID, itemID, itemName, quantity, price);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).addItem(managerID, itemID, itemName, quantity, price);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println("Result: " + result);
            clientLogger.logOperation("ADD_ITEM", managerID, itemID + "," + itemName + "," + quantity + "," + price, result);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void removeItem(Object serverObj, String storePrefix, String managerID, Scanner scanner) {
        try {
            System.out.print("Item ID: ");
            String itemID = scanner.nextLine().trim();
            System.out.print("Quantity to remove: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).removeItem(managerID, itemID, quantity);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).removeItem(managerID, itemID, quantity);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).removeItem(managerID, itemID, quantity);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println("Result: " + result);
            clientLogger.logOperation("REMOVE_ITEM", managerID, itemID + "," + quantity, result);
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void listItems(Object serverObj, String storePrefix, String managerID) {
        try {
            String result;
            switch (storePrefix) {
                case "QC":
                    result = ((client.generated.qc.StoreServer)serverObj).listItemAvailability(managerID);
                    break;
                case "ON":
                    result = ((client.generated.on.StoreServer)serverObj).listItemAvailability(managerID);
                    break;
                case "BC":
                    result = ((client.generated.bc.StoreServer)serverObj).listItemAvailability(managerID);
                    break;
                default:
                    result = "Error: Unknown store";
            }
            
            System.out.println(result);
            clientLogger.logOperation("LIST_ITEMS", managerID, "", "Success");
            
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
