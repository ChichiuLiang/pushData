package org.example.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Component
public class PersistentWebSocketClient {

    private static final List<String> ENDPOINTS = Arrays.asList(
            "ws://service.iems.gree.com:8092/ws/data/alarm",
            "ws://service.iems.gree.com:8092/ws/data/statistic",
            "ws://service.iems.gree.com:8092/ws/data/redis"
    );

    private final StandardWebSocketClient webSocketClient;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService messageProcessor = Executors.newSingleThreadScheduledExecutor();

    private static final int QUEUE_MAX_SIZE = 2000; // 队列最大大小

    @Autowired
    public PersistentWebSocketClient(StandardWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        initializeConnections();
        startMessageProcessing();
    }

    private void initializeConnections() {
        for (String endpoint : ENDPOINTS) {
            connect(endpoint);
            messageQueues.put(endpoint, new ArrayBlockingQueue<>(QUEUE_MAX_SIZE));
        }
    }

    private void connect(String endpoint) {
        try {
            WebSocketHandler handler = new InternalWebSocketHandler(endpoint);
            WebSocketSession session = webSocketClient.doHandshake(handler, new WebSocketHttpHeaders(), new URI(endpoint)).get();
            activeSessions.put(endpoint, session);
            log.info("Connected to {}", endpoint);
        } catch (Exception e) {
            log.error("Failed to connect to {}: {}", endpoint, e.getMessage());
        }
    }

    private void startMessageProcessing() {
        messageProcessor.scheduleAtFixedRate(this::processMessages, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void processMessages() {
        for (String endpoint : ENDPOINTS) {
            WebSocketSession session = activeSessions.get(endpoint);
            BlockingQueue<String> queue = messageQueues.get(endpoint);
            if (session != null && session.isOpen() && !queue.isEmpty()) {
                String message = queue.poll();
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send message to {}: {}", endpoint, e.getMessage());
                }
            }
        }
    }

    public void sendMessage(String endpoint, String message) {
        BlockingQueue<String> queue = messageQueues.get(endpoint);
        if (queue != null) {
            if (!queue.offer(message)) {
                log.warn("Message queue for {} is full. Discarding message: {}", endpoint, message);
            } else {
                log.info("Message queued for {}: {}", endpoint, message);
            }
        } else {
            log.warn("No active session for {}, message discarded", endpoint);
        }
    }

    // ====================== 便捷发送方法 ======================
    public void sendAlarm(String message) {
        sendMessage(ENDPOINTS.get(0), message );
    }

    public void sendStatistic(String message) {
        // 统计信息通常较为重要，使用高优先级
        sendMessage(ENDPOINTS.get(1), message );
    }

    public void sendRedis(String message) {
        sendMessage(ENDPOINTS.get(2), message );
    }



    public void checkConnections() {
        for (String endpoint : ENDPOINTS) {
            WebSocketSession session = activeSessions.get(endpoint);
            if (session == null || !session.isOpen()) {
                log.warn("Connection to {} is closed or not established, reconnecting...", endpoint);
                cleanupConnection(endpoint);
                connect(endpoint);
            }
        }
    }

    private void cleanupConnection(String endpoint) {
        WebSocketSession session = activeSessions.remove(endpoint);
        if (session != null && session.isOpen()) {
            try {
                session.close();
                log.info("Closed session for {}", endpoint);
            } catch (IOException e) {
                log.error("Error closing session for {}: {}", endpoint, e.getMessage());
            }
        }
    }

    private class InternalWebSocketHandler extends TextWebSocketHandler {
        private final String endpoint;

        public InternalWebSocketHandler(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            log.info("Connection established to {}", endpoint);
            activeSessions.put(endpoint, session);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            log.warn("Connection closed for {}: {} (code: {}, reason: {})", endpoint, status, status.getCode(), status.getReason());
            cleanupConnection(endpoint);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            log.error("Transport error for {}: {}", endpoint, exception.getMessage());
            cleanupConnection(endpoint);
        }
    }

    @PreDestroy
    public void destroy() {
        messageProcessor.shutdownNow();
        for (String endpoint : ENDPOINTS) {
            cleanupConnection(endpoint);
        }
    }
}