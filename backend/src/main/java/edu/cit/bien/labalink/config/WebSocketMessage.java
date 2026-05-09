package edu.cit.bien.labalink.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    private String type;
    private String message;
    private Object data;
    private LocalDateTime timestamp = 
        LocalDateTime.now();

    public WebSocketMessage(
            String type,
            String message,
            Object data) {
        this.type = type;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // Message types
    public static final String MACHINE_UPDATED = 
        "MACHINE_UPDATED";
    public static final String BOOKING_CREATED = 
        "BOOKING_CREATED";
    public static final String BOOKING_UPDATED = 
        "BOOKING_UPDATED";
    public static final String WALLET_UPDATED = 
        "WALLET_UPDATED";
}