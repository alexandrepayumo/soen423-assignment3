package models;
import java.io.Serializable;
import java.util.List;

// Base message class for all UDP communications
public abstract class UDPMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String messageType;
    private long timestamp;
    
    public UDPMessage(String messageType) {
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public String getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
}
