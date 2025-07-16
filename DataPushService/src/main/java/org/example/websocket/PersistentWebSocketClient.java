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

    private static final int QUEUE_MAX_SIZE = 3000; // 队列最大大小
    private final Map<String, BlockingQueue<String>> backupMessageQueues = new ConcurrentHashMap<>();
    private static final int BACKUP_QUEUE_MAX_SIZE = 1000; // 备用队列最大容量
    private final ScheduledExecutorService backupMessageProcessor = Executors.newSingleThreadScheduledExecutor();


    @Autowired
    public PersistentWebSocketClient(StandardWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        initializeConnections();
        startMessageProcessing();
        startBackupMessageProcessing(); // 启动备用队列处理任务
    }

    private void initializeConnections() {
        for (String endpoint : ENDPOINTS) {
            connect(endpoint);
            messageQueues.put(endpoint, new ArrayBlockingQueue<>(QUEUE_MAX_SIZE));
            backupMessageQueues.put(endpoint, new ArrayBlockingQueue<>(BACKUP_QUEUE_MAX_SIZE)); // 初始化备用队列
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

    private void startBackupMessageProcessing() {
        backupMessageProcessor.scheduleAtFixedRate(this::processBackupMessages, 0, 500, TimeUnit.MILLISECONDS);
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
        BlockingQueue<String> backupQueue = backupMessageQueues.get(endpoint);

        if (queue != null) {
            if (!queue.offer(message)) {
                // 主队列满，尝试放入备用队列
                if (backupQueue != null && !backupQueue.offer(message)) {
                    log.warn("Backup queue for {} is full. Discarding message: {}", endpoint, message);
                } else {
                    log.warn("Main queue full, moved message to backup queue for {}", endpoint);
                }
            } else {
                //log.info("Message queued for {}: {}", endpoint, message);
            }
        } else {
            log.warn("No active session for {}, message discarded", endpoint);
        }
    }

    private void processBackupMessages() {
        for (String endpoint : ENDPOINTS) {
            BlockingQueue<String> mainQueue = messageQueues.get(endpoint);
            BlockingQueue<String> backupQueue = backupMessageQueues.get(endpoint);
            WebSocketSession session = activeSessions.get(endpoint);

            if (mainQueue == null || backupQueue == null) continue;

            while (!backupQueue.isEmpty()) {
                String message = backupQueue.poll();
                if (message == null) continue;

                // 尝试重新入队主队列
                if (mainQueue.offer(message)) {
                    log.debug("Moved message from backup to main queue for {}", endpoint);
                } else {
                    // 主队列仍然满，尝试直接发送
                    if (session != null && session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(message));
                            //log.info("Sent message directly from backup queue to {}", endpoint);
                        } catch (IOException e) {
                            log.error("Failed to send message from backup queue to {}: {}", endpoint, e.getMessage());
                        }
                    } else {
                        log.warn("Session not open, cannot send message from backup queue to {}", endpoint);
                    }
                }
            }
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
        backupMessageProcessor.shutdownNow();

        for (String endpoint : ENDPOINTS) {
            cleanupConnection(endpoint);
        }
    }

}