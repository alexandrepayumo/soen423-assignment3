package startup;

import javax.xml.ws.Endpoint;
import server.StoreServerImpl;

public class StartQCServer {
    public static void main(String[] args) {
        try {
            String storePrefix = "QC";
            int udpPort = 9001;
            String serviceUrl = "http://localhost:8080/QCServer";
            
            System.out.println("Starting QC Server...");
            
            // Create server implementation
            StoreServerImpl serverImpl = new StoreServerImpl(storePrefix, udpPort);
            
            // Publish web service
            Endpoint endpoint = Endpoint.publish(serviceUrl, serverImpl);
            
            System.out.println("QC Server started successfully!");
            System.out.println("Service URL: " + serviceUrl);
            System.out.println("WSDL available at: " + serviceUrl + "?wsdl");
            System.out.println("UDP Port: " + udpPort);
            System.out.println("\nPress Ctrl+C to stop the server...");
            
            // Keep server running
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            System.out.println("Server interrupted");
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
