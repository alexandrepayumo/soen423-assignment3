package utils;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DSMSLogger {
    private final String logFileName;
    private final DateTimeFormatter formatter;
    
    public DSMSLogger(String logFileName) {
        this.logFileName = logFileName;
        this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }
    
    public synchronized void logOperation(String operation, String userID, String params, String result) {
        try (FileWriter writer = new FileWriter(logFileName, true)) {
            String timestamp = LocalDateTime.now().format(formatter);
            String logEntry = String.format("[%s] %s - User: %s - Params: %s - Result: %s%n", 
                                           timestamp, operation, userID, params, result);
            writer.write(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log: " + e.getMessage());
        }
    }
}
