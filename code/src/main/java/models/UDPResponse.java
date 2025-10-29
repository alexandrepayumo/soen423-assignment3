package models;
import java.io.Serializable;
import java.util.List;

public class UDPResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private boolean success;
    private String message;
    private String errorCode;
    private double newBudget;
    private List<Item> foundItems;
    private String transactionID;
    private double itemPrice;
    
    //Constructor for simple success/failure
    public UDPResponse(boolean success, String message, String errorCode) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
    }
    
    //Constructor for purchase operations
    public UDPResponse(boolean success, String message, double newBudget) {
        this.success = success;
        this.message = message;
        this.newBudget = newBudget;
    }
    
    //Constructor for find operations
    public UDPResponse(boolean success, String message, List<Item> foundItems) {
        this.success = success;
        this.message = message;
        this.foundItems = foundItems;
    }
    
    //Constructor for exchange operations
    public UDPResponse(boolean success, String message, double newBudget, String transactionID) {
        this.success = success;
        this.message = message;
        this.newBudget = newBudget;
        this.transactionID = transactionID;
    }
    
    //Constructor for exchange check operations
    public UDPResponse(boolean success, String message, double newBudget, double itemPrice) {
        this.success = success;
        this.message = message;
        this.newBudget = newBudget;
        this.itemPrice = itemPrice;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public double getNewBudget() { return newBudget; }
    public List<Item> getFoundItems() { return foundItems; }
    public String getTransactionID() { return transactionID; }
    public double getItemPrice() { return itemPrice; }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(success).append("|");
        sb.append(message != null ? message : "").append("|");
        sb.append(errorCode != null ? errorCode : "").append("|");
        sb.append(newBudget).append("|");
        sb.append(transactionID != null ? transactionID : "").append("|");
        sb.append(itemPrice).append("|");
        
        //Serialize foundItems
        if (foundItems != null && !foundItems.isEmpty()) {
            for (int i = 0; i < foundItems.size(); i++) {
                Item item = foundItems.get(i);
                if (i > 0) sb.append(";");
                sb.append(item.getItemID()).append(",")
                  .append(item.getItemName()).append(",")
                  .append(item.getQuantity()).append(",")
                  .append(item.getPrice());
            }
        }
        
        return sb.toString();
    }
    
    public static UDPResponse fromString(String str) {
        String[] parts = str.split("\\|", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("Invalid UDPResponse string format");
        }
        
        boolean success = Boolean.parseBoolean(parts[0]);
        String message = parts[1].isEmpty() ? null : parts[1];
        String errorCode = parts[2].isEmpty() ? null : parts[2];
        double newBudget = Double.parseDouble(parts[3]);
        String transactionID = parts[4].isEmpty() ? null : parts[4];
        double itemPrice = Double.parseDouble(parts[5]);
        
        UDPResponse response = new UDPResponse(success, message, newBudget);
        response.errorCode = errorCode;
        response.transactionID = transactionID;
        response.itemPrice = itemPrice;
        
        //Deserialize foundItems if present
        if (parts.length > 6 && !parts[6].isEmpty()) {
            java.util.List<Item> items = new java.util.ArrayList<>();
            String[] itemStrings = parts[6].split(";");
            for (String itemStr : itemStrings) {
                String[] itemParts = itemStr.split(",");
                if (itemParts.length == 4) {
                    Item item = new Item(
                        itemParts[0],
                        itemParts[1],
                        Integer.parseInt(itemParts[2]),
                        Double.parseDouble(itemParts[3])
                    );
                    items.add(item);
                }
            }
            response.foundItems = items;
        }
        
        return response;
    }
}
