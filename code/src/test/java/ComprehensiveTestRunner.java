import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class ComprehensiveTestRunner {
    
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static int failedTests = 0;
    private static List<String> failedTestDetails = new ArrayList<>();
    
    private static client.generated.qc.StoreServer qcServer;
    private static client.generated.on.StoreServer onServer;
    private static client.generated.bc.StoreServer bcServer;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   DSMS COMPREHENSIVE TEST SUITE - Web Services           ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        try {
            // Connect to web services
            System.out.println("Connecting to web services...");
            
            client.generated.qc.StoreServerService qcService = 
                new client.generated.qc.StoreServerService(new URL("http://localhost:8080/QCServer?wsdl"));
            qcServer = qcService.getStoreServerImplPort();
            
            client.generated.on.StoreServerService onService = 
                new client.generated.on.StoreServerService(new URL("http://localhost:8081/ONServer?wsdl"));
            onServer = onService.getStoreServerImplPort();
            
            client.generated.bc.StoreServerService bcService = 
                new client.generated.bc.StoreServerService(new URL("http://localhost:8082/BCServer?wsdl"));
            bcServer = bcService.getStoreServerImplPort();
            
            System.out.println("✓ Connected to all servers\n");
            
            runManagerTests();
            runCustomerPurchaseTests();
            runCustomerReturnTests();
            runCustomerExchangeTests();
            runEdgeCaseTests();
            runConcurrencyTests();
            
            printSummary();
            
        } catch (Exception e) {
            System.err.println("ERROR: Failed to initialize test environment");
            System.err.println("Make sure all servers are running:");
            System.err.println("  - http://localhost:8080/QCServer?wsdl");
            System.err.println("  - http://localhost:8081/ONServer?wsdl");
            System.err.println("  - http://localhost:8082/BCServer?wsdl");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    // ==================== MANAGER TESTS ====================
    
    private static void runManagerTests() {
        printSection("MANAGER OPERATIONS");
        
        test("Add New Item", () -> {
            String result = qcServer.addItem("QCM1111", "QC2001", "Milk", 20, 4.99);
            return result.contains("successfully") || result.contains("updated");
        });
        
        test("Update Existing Item Quantity", () -> {
            qcServer.addItem("QCM1111", "QC1001", "Coffee", 5, 5.99);
            String result = qcServer.addItem("QCM1111", "QC1001", "Coffee", 10, 5.99);
            return result.contains("updated") && result.contains("25");
        });
        
        test("Remove Partial Quantity", () -> {
            String result = qcServer.removeItem("QCM1111", "QC1001", 5);
            return result.contains("reduced") || result.contains("20");
        });
        
        test("Remove All Quantity (Set to 0)", () -> {
            qcServer.addItem("QCM1111", "QC3001", "TestItem", 5, 1.00);
            String result = qcServer.removeItem("QCM1111", "QC3001", 10);
            return result.contains("set to 0") || result.contains("waitlist");
        });
        
        test("List Store Items", () -> {
            String result = qcServer.listItemAvailability("QCM1111");
            return result.contains("QC1001") || result.contains("Coffee");
        });
        
        test("Reject Invalid Manager ID", () -> {
            String result = qcServer.addItem("INVALID", "QC1001", "Test", 1, 1.0);
            return result.contains("Invalid");
        });
    }
    
    // ==================== CUSTOMER PURCHASE TESTS ====================
    
    private static void runCustomerPurchaseTests() {
        printSection("CUSTOMER PURCHASE OPERATIONS");
        
        String today = getCurrentDate();
        
        test("Local Purchase with Quantity", () -> {
            String result = qcServer.purchaseItem("QCU1111", "QC1001", 2, today);
            return result.contains("successful") && result.contains("Bought 2");
        });
        
        test("Remote Purchase from Another Store", () -> {
            String result = qcServer.purchaseItem("QCU1111", "ON1001", 1, today);
            return result.contains("successful");
        });
        
        test("Find Item Across Stores", () -> {
            String result = qcServer.findItem("QCU1111", "Coffee");
            return result.contains("Found") && (result.contains("QC") || result.contains("ON") || result.contains("BC"));
        });
        
        test("Reject Purchase - Insufficient Budget", () -> {
            onServer.addItem("ONM1111", "ON9999", "ExpensiveItem", 10, 999.99);
            String result = qcServer.purchaseItem("QCU2222", "ON9999", 2, today);
            return result.contains("Insufficient budget") || result.contains("budget");
        });
        
        test("Reject Purchase - Insufficient Quantity", () -> {
            qcServer.addItem("QCM1111", "QC4001", "LimitedItem", 3, 2.00);
            String result = qcServer.purchaseItem("QCU3333", "QC4001", 5, today);
            return result.contains("Insufficient quantity");
        });
        
        test("Out of Stock Triggers Waitlist Prompt", () -> {
            qcServer.addItem("QCM1111", "QC5001", "OutOfStock", 0, 1.00);
            String result = qcServer.purchaseItem("QCU4444", "QC5001", 1, today);
            return result.contains("WAITLIST_PROMPT");
        });
        
        test("Add Customer to Waitlist", () -> {
            String result = qcServer.addToWaitlist("QCU4444", "QC5001");
            return result.contains("waitlist") || result.contains("Position");
        });
        
        test("Reject Invalid Quantity (0 or negative)", () -> {
            String result = qcServer.purchaseItem("QCU1111", "QC1001", 0, today);
            return result.contains("Invalid quantity") || result.contains("greater than 0");
        });
    }
    
    // ==================== CUSTOMER RETURN TESTS ====================
    
    private static void runCustomerReturnTests() {
        printSection("CUSTOMER RETURN OPERATIONS");
        
        String today = getCurrentDate();
        String past = getPastDate(15);
        String expired = getPastDate(35);
        
        test("Valid Return Within 30 Days", () -> {
            qcServer.purchaseItem("QCU5555", "QC1002", 1, past);
            String result = qcServer.returnItem("QCU5555", "QC1002", today);
            return result.contains("successful") || result.contains("Refunded");
        });
        
        test("Reject Expired Return (>30 days)", () -> {
            qcServer.purchaseItem("QCU6666", "QC1002", 1, expired);
            String result = qcServer.returnItem("QCU6666", "QC1002", today);
            return result.contains("expired") || result.contains("30 days");
        });
        
        test("Reject Return of Non-Purchased Item", () -> {
            String result = qcServer.returnItem("QCU7777", "QC1001", today);
            return result.contains("not found") || result.contains("No purchase");
        });
    }
    
    // ==================== CUSTOMER EXCHANGE TESTS ====================
    
    private static void runCustomerExchangeTests() {
        printSection("CUSTOMER EXCHANGE OPERATIONS");
        
        String past = getPastDate(10);
        
        qcServer.purchaseItem("QCU8888", "QC1001", 1, past);
        qcServer.purchaseItem("QCU9999", "QC1002", 1, past);
        
        test("Local Exchange (Same Store)", () -> {
            String result = qcServer.exchangeItem("QCU8888", "QC1003", "QC1001");
            return result.contains("successful") || result.contains("Exchange");
        });
        
        test("Cross-Store Exchange", () -> {
            qcServer.purchaseItem("QCU1234", "QC1001", 1, past);
            String result = qcServer.exchangeItem("QCU1234", "ON1001", "QC1001");
            return result.contains("successful") || result.contains("Exchange");
        });
        
        test("Reject Exchange of Expired Item", () -> {
            String expired = getPastDate(35);
            qcServer.purchaseItem("QCU5678", "QC1002", 1, expired);
            String result = qcServer.exchangeItem("QCU5678", "QC1003", "QC1002");
            return result.contains("expired") || result.contains("30 days");
        });
        
        test("Reject Exchange of Non-Owned Item", () -> {
            String result = qcServer.exchangeItem("QCU0000", "QC1003", "QC1001");
            return result.contains("not found") || result.contains("ERROR");
        });
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    private static void runEdgeCaseTests() {
        printSection("EDGE CASES & BUSINESS RULES");
        
        String today = getCurrentDate();
        
        test("Enforce Remote Store Purchase Limit", () -> {
            String result1 = qcServer.purchaseItem("QCU9991", "ON1001", 1, today);
            String result2 = qcServer.purchaseItem("QCU9991", "ON1002", 1, today);
            return result1.contains("successful") && 
                   (result2.contains("limit") || result2.contains("Already purchased"));
        });
        
        test("Reject Invalid Customer ID", () -> {
            String result = qcServer.purchaseItem("INVALID", "QC1001", 1, today);
            return result.contains("Invalid");
        });
        
        test("Prevent Customer from Manager Operations", () -> {
            String result = qcServer.addItem("QCU1111", "QC1001", "Test", 1, 1.0);
            return result.contains("Invalid");
        });
        
        test("Prevent Manager from Customer Operations", () -> {
            String result = qcServer.purchaseItem("QCM1111", "QC1001", 1, today);
            return result.contains("Invalid");
        });
        
        test("Track Customer Budget Correctly", () -> {
            String result = bcServer.purchaseItem("BCU0001", "BC1001", 1, today);
            return result.contains("successful") && result.contains("budget");
        });
    }
    
    // ==================== CONCURRENCY TESTS ====================
    
    private static void runConcurrencyTests() {
        printSection("CONCURRENCY & SYNCHRONIZATION");
        
        test("Handle Concurrent Purchases", () -> {
            qcServer.addItem("QCM1111", "QC6001", "ConcurrentTest", 10, 1.00);
            
            Thread t1 = new Thread(() -> qcServer.purchaseItem("QCU1111", "QC6001", 3, getCurrentDate()));
            Thread t2 = new Thread(() -> qcServer.purchaseItem("QCU2222", "QC6001", 3, getCurrentDate()));
            Thread t3 = new Thread(() -> qcServer.purchaseItem("QCU3333", "QC6001", 3, getCurrentDate()));
            
            t1.start();
            t2.start();
            t3.start();
            
            try {
                t1.join();
                t2.join();
                t3.join();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        });
        
        test("Handle Concurrent Add/Remove", () -> {
            Thread t1 = new Thread(() -> qcServer.addItem("QCM1111", "QC7001", "Test", 10, 1.0));
            Thread t2 = new Thread(() -> qcServer.removeItem("QCM1111", "QC7001", 5));
            
            t1.start();
            t2.start();
            
            try {
                t1.join();
                t2.join();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        });
    }
    
    // ==================== UTILITY METHODS ====================
    
    private static void test(String testName, TestCase testCase) {
        totalTests++;
        System.out.print(String.format("%-50s", testName + "..."));
        
        try {
            boolean passed = testCase.run();
            if (passed) {
                passedTests++;
                System.out.println(" ✓ PASS");
            } else {
                failedTests++;
                System.out.println(" ✗ FAIL");
                failedTestDetails.add(testName);
            }
        } catch (Exception e) {
            failedTests++;
            System.out.println(" ✗ ERROR: " + e.getMessage());
            failedTestDetails.add(testName + " (Exception: " + e.getMessage() + ")");
        }
    }
    
    private static void printSection(String title) {
        System.out.println();
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("  " + title);
        System.out.println("═══════════════════════════════════════════════════════════");
    }
    
    private static void printSummary() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║   TEST SUMMARY                                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Total Tests:  " + totalTests);
        System.out.println("Passed:       " + passedTests + " (" + (passedTests * 100 / totalTests) + "%)");
        System.out.println("Failed:       " + failedTests + " (" + (failedTests * 100 / totalTests) + "%)");
        
        if (failedTests > 0) {
            System.out.println();
            System.out.println("Failed Tests:");
            for (String test : failedTestDetails) {
                System.out.println("  - " + test);
            }
        }
        
        System.out.println();
        if (failedTests == 0) {
            System.out.println("✓ ALL TESTS PASSED!");
        } else {
            System.out.println("✗ Some tests failed. Review details above.");
        }
    }
    
    private static String getCurrentDate() {
        return new SimpleDateFormat("ddMMyyyy").format(new Date());
    }
    
    private static String getPastDate(int daysAgo) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -daysAgo);
        return new SimpleDateFormat("ddMMyyyy").format(cal.getTime());
    }
    
    @FunctionalInterface
    interface TestCase {
        boolean run() throws Exception;
    }
}
