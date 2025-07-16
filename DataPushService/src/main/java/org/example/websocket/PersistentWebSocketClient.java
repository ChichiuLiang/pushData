    package org.example.websocket;

    import lombok.extern.slf4j.Slf4j;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Component;
    import org.springframework.web.socket.*;
    import org.springframework.web.socket.client.standard.StandardWebSocketClient;
    import org.springframework.web.socket.handler.TextWebSocketHandler;
    import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

    import javax.annotation.PostConstruct;
    import javax.annotation.PreDestroy;
    import java.io.IOException;
    import java.net.URI;
    import java.util.*;
    import java.util.concurrent.*;
    import java.util.concurrent.atomic.AtomicLong;
    import java.util.concurrent.locks.ReentrantLock;

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

        @Autowired
        public PersistentWebSocketClient(StandardWebSocketClient webSocketClient) {
            this.webSocketClient = webSocketClient;
            initializeConnections();
        }

        private void initializeConnections() {
            for (String endpoint : ENDPOINTS) {
                connect(endpoint);
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

        public void sendMessage(String endpoint, String message) {
            WebSocketSession session = activeSessions.get(endpoint);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.info("Message sent to {}: {}", endpoint, message);
                } catch (IOException e) {
                    log.error("Failed to send message to {}: {}", endpoint, e.getMessage());
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
    }