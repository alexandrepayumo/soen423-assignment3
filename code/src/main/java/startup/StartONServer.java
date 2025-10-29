package startup;

import javax.xml.ws.Endpoint;
import server.StoreServerImpl;

public class StartONServer {
    public static void main(String[] args) {
        try {
            String storePrefix = "ON";
            int udpPort = 9002;
            String serviceUrl = "http://localhost:8081/ONServer";
            
            System.out.println("Starting ON Server...");
            
            StoreServerImpl serverImpl = new StoreServerImpl(storePrefix, udpPort);
            
            Endpoint endpoint = Endpoint.publish(serviceUrl, serverImpl);
            
            System.out.println("ON Server started successfully!");
            System.out.println("Service URL: " + serviceUrl);
            System.out.println("WSDL available at: " + serviceUrl + "?wsdl");
            System.out.println("UDP Port: " + udpPort);
            System.out.println("\nPress Ctrl+C to stop the server...");
            
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
