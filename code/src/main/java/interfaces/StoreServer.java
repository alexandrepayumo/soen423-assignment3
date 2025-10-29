package interfaces;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.WebParam;
import javax.jws.soap.SOAPBinding;

@WebService
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface StoreServer {
    
    @WebMethod
    String addItem(
        @WebParam(name = "managerID") String managerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "itemName") String itemName,
        @WebParam(name = "quantity") int quantity,
        @WebParam(name = "price") double price
    );
    
    @WebMethod
    String removeItem(
        @WebParam(name = "managerID") String managerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "quantity") int quantity
    );
    
    @WebMethod
    String listItemAvailability(
        @WebParam(name = "managerID") String managerID
    );
    
    @WebMethod
    String purchaseItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "quantity") int quantity,
        @WebParam(name = "dateOfPurchase") String dateOfPurchase
    );
    
    @WebMethod
    String findItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemName") String itemName
    );
    
    @WebMethod
    String returnItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID,
        @WebParam(name = "dateOfReturn") String dateOfReturn
    );
    
    @WebMethod
    String exchangeItem(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "newItemID") String newItemID,
        @WebParam(name = "oldItemID") String oldItemID
    );
    
    @WebMethod
    String addToWaitlist(
        @WebParam(name = "customerID") String customerID,
        @WebParam(name = "itemID") String itemID
    );
    
    @WebMethod
    String getStorePrefix();
}
