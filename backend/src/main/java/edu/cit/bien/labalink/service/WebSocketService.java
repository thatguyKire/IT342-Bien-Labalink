package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.model.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate 
        messagingTemplate;

    // Broadcast machine status update
    public void broadcastMachineUpdate(
            Object machineData) {
        WebSocketMessage message = 
            new WebSocketMessage(
                WebSocketMessage.MACHINE_UPDATED,
                "Machine status updated",
                machineData);

        messagingTemplate.convertAndSend(
            "/topic/machines", message);

        log.info("WebSocket: Machine update broadcast");
    }

    // Broadcast new booking created
    public void broadcastBookingCreated(
            Object bookingData) {
        WebSocketMessage message =
            new WebSocketMessage(
                WebSocketMessage.BOOKING_CREATED,
                "New booking created",
                bookingData);

        messagingTemplate.convertAndSend(
            "/topic/bookings", message);

        log.info(
            "WebSocket: Booking created broadcast");
    }

    // Broadcast booking status update
    public void broadcastBookingUpdate(
            Object bookingData) {
        WebSocketMessage message =
            new WebSocketMessage(
                WebSocketMessage.BOOKING_UPDATED,
                "Booking status updated",
                bookingData);

        messagingTemplate.convertAndSend(
            "/topic/bookings", message);

        log.info(
            "WebSocket: Booking update broadcast");
    }

    // Broadcast wallet balance update
    public void broadcastWalletUpdate(
            String userEmail,
            Double newBalance) {
        WebSocketMessage message =
            new WebSocketMessage(
                WebSocketMessage.WALLET_UPDATED,
                "Wallet balance updated",
                java.util.Map.of(
                    "email", userEmail,
                    "newBalance", newBalance));

        messagingTemplate.convertAndSend(
            "/topic/wallet/" + userEmail,
            message);

        log.info(
            "WebSocket: Wallet update broadcast " +
            "for {}", userEmail);
    }
}