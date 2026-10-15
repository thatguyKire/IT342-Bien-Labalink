package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.BookingResponse;
import edu.cit.bien.labalink.model.WebSocketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Real-time pushes.
 *  /topic/machines        machine status, no secrets (all signed-in users)
 *  /topic/bookings        booking feed (attendants only; see interceptor)
 *  /user/queue/bookings   a customer's own booking changes
 *  /user/queue/wallet     a customer's own balance changes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastMachineUpdate(Object machineData) {
        send("/topic/machines", new WebSocketMessage(
            WebSocketMessage.MACHINE_UPDATED,
            "Machine status updated",
            machineData));
    }

    public void broadcastBookingCreated(BookingResponse booking) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessage.BOOKING_CREATED,
            "New booking created",
            booking);
        send("/topic/bookings", message);
        sendToUser(booking.getUserId(), "/queue/bookings", message);
    }

    public void broadcastBookingUpdate(BookingResponse booking) {
        WebSocketMessage message = new WebSocketMessage(
            WebSocketMessage.BOOKING_UPDATED,
            "Booking status updated",
            booking);
        send("/topic/bookings", message);
        sendToUser(booking.getUserId(), "/queue/bookings", message);
    }

    public void sendWalletUpdate(Long userId, BigDecimal newBalance) {
        sendToUser(userId, "/queue/wallet", new WebSocketMessage(
            WebSocketMessage.WALLET_UPDATED,
            "Wallet balance updated",
            Map.of("newBalance", newBalance)));
    }

    private void send(String destination, WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.warn("WebSocket push to {} failed: {}",
                destination, e.getMessage());
        }
    }

    private void sendToUser(
            Long userId,
            String destination,
            WebSocketMessage message) {
        try {
            messagingTemplate.convertAndSendToUser(
                String.valueOf(userId), destination, message);
        } catch (Exception e) {
            log.warn("WebSocket push to user failed: {}", e.getMessage());
        }
    }
}
