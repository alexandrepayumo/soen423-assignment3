package models;
import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemID;
    private String itemName;
    private int quantity;
    private double price;
    
    public Item(String itemID, String itemName, int quantity, double price) {
        this.itemID = itemID;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
    }

    // Add copy constructor for safe serialization
    public Item(Item other) {
        this.itemID = other.itemID;
        this.itemName = other.itemName;
        this.quantity = other.quantity;
        this.price = other.price;
    }
    
    // Getters and setters
    public String getItemID() { return itemID; }
    public String getItemName() { return itemName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void decrementQuantity() { this.quantity--; }
    public void incrementQuantity(int amount) { this.quantity += amount; }
    
    @Override
    public String toString() {
        return itemID + " " + itemName + " " + quantity + " " + price;
    }
}
