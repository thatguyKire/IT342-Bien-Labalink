package edu.cit.bien.labalink.config;

import edu.cit.bien.labalink.security.JwtAuthenticationService;
import edu.cit.bien.labalink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP sessions and authorises subscriptions:
 *  - CONNECT must carry "Authorization: Bearer <jwt>"
 *  - /topic/machines        any signed-in user (no secrets broadcast there)
 *  - /topic/bookings        attendants only (contains customer emails)
 *  - /user/queue/**         the caller's own private queue
 *  - SEND                   rejected; clients never publish
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtAuthenticationService authenticationService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
            message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            String token = JwtAuthenticationService.extractBearer(
                accessor.getFirstNativeHeader("Authorization"));
            Authentication authentication = authenticationService
                .authenticate(token)
                .orElseThrow(() ->
                    new MessageDeliveryException("Unauthorized"));
            accessor.setUser(authentication);
        } else if (command == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        } else if (command == StompCommand.SEND) {
            throw new MessageDeliveryException("Sending is not allowed");
        }
        return message;
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !(authentication.getPrincipal()
                    instanceof UserPrincipal principal)) {
            throw new MessageDeliveryException("Unauthorized");
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            throw new MessageDeliveryException("Missing destination");
        }
        boolean allowed = destination.equals("/topic/machines")
            || destination.startsWith("/user/queue/")
            || (destination.equals("/topic/bookings")
                && principal.isAttendant());
        if (!allowed) {
            throw new MessageDeliveryException("Forbidden destination");
        }
    }
}
