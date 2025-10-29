package models;
import java.io.Serializable;

public class UDPRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String operation;
    private String customerID;
    private String itemID;
    private String itemName;
    private String date;
    private double budget;
    private String oldItemID;
    private double oldItemPrice;
    private int quantity;
    
    // Constructor for PURCHASE operation
    public UDPRequest(String customerID, String itemID, int quantity, String date, double budget) {
        this.operation = "PURCHASE";
        this.customerID = customerID;
        this.itemID = itemID;
        this.quantity = quantity;
        this.date = date;
        this.budget = budget;
    }
    
    // Constructor for FIND operation
    public UDPRequest(String customerID, String itemName) {
        this.operation = "FIND";
        this.customerID = customerID;
        this.itemName = itemName;
    }
    
    // Constructor for EXCHANGE_CHECK operation
    public UDPRequest(String customerID, String newItemID, String oldItemID, double budget, double oldItemPrice) {
        this.operation = "EXCHANGE_CHECK";
        this.customerID = customerID;
        this.itemID = newItemID;
        this.oldItemID = oldItemID;
        this.budget = budget;
        this.oldItemPrice = oldItemPrice;
    }
    
    // Constructor for other exchange operations with budget
    public UDPRequest(String operation, String customerID, String itemID, String oldItemID, double budget, double oldItemPrice) {
        this.operation = operation;
        this.customerID = customerID;
        this.itemID = itemID;
        this.oldItemID = oldItemID;
        this.budget = budget;
        this.oldItemPrice = oldItemPrice;
    }
    
    // Constructor for operations with 4 string parameters (EXCHANGE_ROLLBACK, EXCHANGE_RETURN, EXCHANGE_UNDO_RETURN)
    // The 4th parameter is used as oldItemID for ROLLBACK, or date for RETURN operations
    public UDPRequest(String operation, String customerID, String itemID, String param4) {
        this.operation = operation;
        this.customerID = customerID;
        this.itemID = itemID;
        if (operation.equals("EXCHANGE_ROLLBACK")) {
            this.oldItemID = param4;
        } else {
            this.date = param4;
        }
    }
    
    // Getters
    public String getOperation() { return operation; }
    public String getCustomerID() { return customerID; }
    public String getItemID() { return itemID; }
    public String getItemName() { return itemName; }
    public String getDate() { return date; }
    public double getBudget() { return budget; }
    public String getOldItemID() { return oldItemID; }
    public double getOldItemPrice() { return oldItemPrice; }
    public int getQuantity() { return quantity; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(operation).append("|");
        sb.append(customerID != null ? customerID : "").append("|");
        sb.append(itemID != null ? itemID : "").append("|");
        sb.append(itemName != null ? itemName : "").append("|");
        sb.append(date != null ? date : "").append("|");
        sb.append(budget).append("|");
        sb.append(oldItemID != null ? oldItemID : "").append("|");
        sb.append(oldItemPrice).append("|");
        sb.append(quantity);
        return sb.toString();
    }
    
    public static UDPRequest fromString(String str) {
        String[] parts = str.split("\\|", -1);
        if (parts.length < 9) {
            throw new IllegalArgumentException("Invalid UDPRequest string format");
        }
        
        UDPRequest request = new UDPRequest(
            parts[1].isEmpty() ? null : parts[1],
            parts[2].isEmpty() ? null : parts[2],
            Integer.parseInt(parts[8]),
            parts[4].isEmpty() ? null : parts[4],
            Double.parseDouble(parts[5])
        );
        
        request.operation = parts[0];
        request.itemName = parts[3].isEmpty() ? null : parts[3];
        request.oldItemID = parts[6].isEmpty() ? null : parts[6];
        request.oldItemPrice = Double.parseDouble(parts[7]);
        
        return request;
    }
}
