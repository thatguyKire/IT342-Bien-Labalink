package edu.cit.bien.labalink;

import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.JwtProvider;
import edu.cit.bien.labalink.service.WebSocketService;
import edu.cit.bien.labalink.support.TestInfrastructure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestInfrastructure.class)
class WebSocketSecurityTest {

    @LocalServerPort int port;
    @Autowired UserRepository userRepository;
    @Autowired JwtProvider jwtProvider;
    @Autowired WebSocketService webSocketService;
    @MockitoBean JavaMailSender mailSender;

    @Test
    void connectWithoutValidTokenIsRejected() {
        assertThatThrownBy(() -> connect(null).get(10, TimeUnit.SECONDS))
            .isNotNull();
        assertThatThrownBy(() -> connect("garbage").get(10, TimeUnit.SECONDS))
            .isNotNull();
    }

    @Test
    void customersCannotSubscribeToTheAttendantFeed() throws Exception {
        CompletableFuture<Boolean> rejected = new CompletableFuture<>();
        StompSession session = connect(
            tokenFor(newUser(User.Role.CUSTOMER)), rejected)
            .get(10, TimeUnit.SECONDS);

        session.subscribe("/topic/bookings", ignoreFrames());
        assertThat(rejected.get(10, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void walletUpdatesReachOnlyTheirOwner() throws Exception {
        User alice = newUser(User.Role.CUSTOMER);
        User bob = newUser(User.Role.CUSTOMER);

        CompletableFuture<Object> aliceInbox = new CompletableFuture<>();
        CompletableFuture<Object> bobInbox = new CompletableFuture<>();
        connect(tokenFor(alice)).get(10, TimeUnit.SECONDS)
            .subscribe("/user/queue/wallet", collectInto(aliceInbox));
        connect(tokenFor(bob)).get(10, TimeUnit.SECONDS)
            .subscribe("/user/queue/wallet", collectInto(bobInbox));
        Thread.sleep(500); // let SUBSCRIBE frames register

        webSocketService.sendWalletUpdate(alice.getId(), new BigDecimal("42.00"));

        assertThat(aliceInbox.get(10, TimeUnit.SECONDS)).isNotNull();
        Thread.sleep(500);
        assertThat(bobInbox).isNotDone();
    }

    private User newUser(User.Role role) {
        User user = new User();
        user.setUsername("ws-user");
        user.setEmail("ws-" + UUID.randomUUID() + "@test.local");
        user.setPasswordHash("$2a$12$unused");
        user.setRole(role);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        return jwtProvider.generateToken(user);
    }

    private CompletableFuture<StompSession> connect(String token) {
        return connect(token, new CompletableFuture<>());
    }

    private CompletableFuture<StompSession> connect(
            String token, CompletableFuture<Boolean> errorSignal) {
        WebSocketStompClient client = new WebSocketStompClient(
            new SockJsClient(List.of(
                new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter());

        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        httpHeaders.setOrigin("http://localhost:5173");

        return client.connectAsync(
            "http://localhost:{port}/ws", httpHeaders, connectHeaders,
            new StompSessionHandlerAdapter() {
                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    errorSignal.complete(true); // ERROR frame
                }

                @Override
                public void handleTransportError(
                        StompSession session, Throwable exception) {
                    errorSignal.complete(true);
                }
            },
            port);
    }

    private static StompFrameHandler ignoreFrames() {
        return collectInto(new CompletableFuture<>());
    }

    private static StompFrameHandler collectInto(
            CompletableFuture<Object> inbox) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.complete(payload);
            }
        };
    }
}
