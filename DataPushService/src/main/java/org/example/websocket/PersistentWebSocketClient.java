package org.example.websocket;

import lombok.extern.slf4j.Slf4j;
import org.example.config.WebSocketEndpointsConfig;
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
            "ws://10.40.255.22:9118/ws/data/alarm",
            "ws://10.40.255.22:9118/ws/data/statistic",
            "ws://10.40.255.22:9118/ws/data/redis"
    );

    private final StandardWebSocketClient webSocketClient;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();
    private final ScheduledExecutorService messageProcessor = Executors.newSingleThreadScheduledExecutor();

    private static final int QUEUE_MAX_SIZE = 10000; // 队列最大大小
    private final Map<String, BlockingQueue<String>> backupMessageQueues = new ConcurrentHashMap<>();
    private static final int BACKUP_QUEUE_MAX_SIZE = 5000; // 备用队列最大容量
    private final ScheduledExecutorService backupMessageProcessor = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    public PersistentWebSocketClient(StandardWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        initializeConnections();
        startMessageProcessing();
        startBackupMessageProcessing();
    }

    private void initializeConnections() {
        for (String endpoint : ENDPOINTS) {
            connect(endpoint);
            messageQueues.put(endpoint, new LinkedBlockingQueue<>(QUEUE_MAX_SIZE));
            backupMessageQueues.put(endpoint, new LinkedBlockingQueue<>(BACKUP_QUEUE_MAX_SIZE));
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
            // 重连机制
            try {
                Thread.sleep(5000); // 5秒后重试
                connect(endpoint);
            } catch (InterruptedException ie) {
                log.error("Reconnect interrupted for {}", endpoint);
            }
        }
    }

    private void startMessageProcessing() {
        messageProcessor.scheduleAtFixedRate(this::processMessages, 0, 20, TimeUnit.MILLISECONDS);
    }

    private void startBackupMessageProcessing() {
        backupMessageProcessor.scheduleAtFixedRate(this::processBackupMessages, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理消息队列中的消息并将其发送到相应的WebSocket会话
     * 此方法遍历所有端点，并尝试从每个端点的消息队列中获取消息如果队列不为空且对应的会话是开启状态，
     * 则将消息从队列中移除并发送到该会话
     */
    private void processMessages() {
        // 遍历所有端点
        for (String endpoint : ENDPOINTS) {
            // 获取当前端点的WebSocket会话
            WebSocketSession session = activeSessions.get(endpoint);
            // 获取当前端点的消息队列
            BlockingQueue<String> queue = messageQueues.get(endpoint);
            // 如果会话是开启状态且队列不为空，则准备发送消息
            if (session != null && session.isOpen() && !queue.isEmpty()) {
                // 从队列中移除并获取一条消息
                String message = queue.poll();
                try {
                    // 尝试将消息发送到会话
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    // 如果发送消息失败，则记录错误日志
                    log.error("Failed to send message to {}: {}", endpoint, e.getMessage());
                }
            }
        }
    }

    /**
     * 向指定端点发送消息
     * 该方法首先尝试将消息添加到主队列中，如果主队列已满，则尝试添加到备用队列中
     * 如果两个队列都满，则丢弃消息并记录警告日志
     *
     * @param endpoint 消息接收端点的标识符
     * @param message 要发送的消息内容
     */
    public void sendMessage(String endpoint, String message) {
        // 获取主消息队列
        BlockingQueue<String> queue = messageQueues.get(endpoint);
        // 获取备用消息队列
        BlockingQueue<String> backupQueue = backupMessageQueues.get(endpoint);

        // 检查主队列是否存在
        if (queue != null) {
            // 尝试将消息添加到主队列
            if (!queue.offer(message)) {
                // 主队列满，尝试放入备用队列
                if (backupQueue != null && !backupQueue.offer(message)) {
                    // 备用队列也满，记录警告日志并丢弃消息
                    log.warn("Backup queue for {} is full. Discarding message: {}", endpoint, message);
                } else {
                    // 成功将消息添加到备用队列，记录日志
                    log.warn("Main queue full, moved message to backup queue for {}", endpoint);
                }
            } else {
                // 成功将消息添加到主队列，可选的日志记录（已注释）
                //log.info("Message queued for {}: {}", endpoint, message);
            }
        } else {
            // 没有找到主队列，记录警告日志并丢弃消息
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