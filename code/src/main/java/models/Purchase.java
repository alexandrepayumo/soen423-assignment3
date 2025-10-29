package models;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Purchase implements Serializable {
    private static final long serialVersionUID = 1L;

    private String customerID;
    private String itemID;
    private LocalDate purchaseDate;
    private double price;
    
    public Purchase(String customerID, String itemID, String dateStr, double price) {
        this.customerID = customerID;
        this.itemID = itemID;
        this.purchaseDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("ddMMyyyy"));
        this.price = price;
    }
    
    public boolean canReturn(String returnDateStr) {
        LocalDate returnDate = LocalDate.parse(returnDateStr, DateTimeFormatter.ofPattern("ddMMyyyy"));
        return purchaseDate.plusDays(30).isAfter(returnDate) || purchaseDate.plusDays(30).isEqual(returnDate);
    }
    
    // Getters
    public String getCustomerID() { return customerID; }
    public String getItemID() { return itemID; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public double getPrice() { return price; }
}
