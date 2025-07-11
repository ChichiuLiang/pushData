package org.example.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;

@Component
@Slf4j
public class PersistentWebSocketClient {

    private WebSocketSession session;
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    private final StandardWebSocketClient webSocketClient;

    public PersistentWebSocketClient() {
        this.webSocketClient = new StandardWebSocketClient();
    }

    @PostConstruct
    public void connect() {
        String uriStr = "ws://localhost:8092/ws/data";
        URI uri;
        try {
            uri = new URI(uriStr);
        } catch (Exception e) {
            log.error("Invalid WebSocket URI", e);
            return;
        }

        try {
            webSocketClient.doHandshake(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                    log.info("WebSocket connected.");
                    PersistentWebSocketClient.this.session = session;
                    startSenderTask();
                }

                @Override
                public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                    log.info("Received message: {}", message.getPayload());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                    log.warn("WebSocket closed: {}", status);
                    reconnect(); // 断开后尝试重连
                }
            }, new WebSocketHttpHeaders(), uri); // 使用 URI 而不是 String

        } catch (Exception e) {
            log.error("Failed to connect WebSocket", e);
            reconnect();
        }
    }

    private void startSenderTask() {
        scheduler.scheduleAtFixedRate(() -> {
            if (session != null && session.isOpen()) {
                String msg = messageQueue.poll();
                if (msg != null) {
                    try {
                        session.sendMessage(new TextMessage(msg));
                    } catch (IOException e) {
                        log.error("Send message error", e);
                    }
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // 每 100ms 发送一次
    }

    public void sendMessage(String jsonMessage) {
        messageQueue.offer(jsonMessage);
    }

    private void reconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒后重连
                log.info("Reconnecting...");
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void destroy() {
        running = false;
        scheduler.shutdownNow();
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("Error closing session", e);
            }
        }
    }
}
