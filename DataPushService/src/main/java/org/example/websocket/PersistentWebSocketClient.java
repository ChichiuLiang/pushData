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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class PersistentWebSocketClient {

    private static final List<String> ENDPOINTS = Arrays.asList(
//            "ws://service.iems.gree.com:8092/ws/data/alarm",
//            "ws://service.iems.gree.com:8092/ws/data/statistic",
//            "ws://service.iems.gree.com:8092/ws/data/redis"
//            "ws://iems.neiic.com:9018/ws/data/statistic",
//            "ws://iems.neiic.com:9018/ws/data/redis"
            "ws://10.40.255.22:9118/ws/data/alarm",
            "ws://10.40.255.22:9118/ws/data/statistic",
            "ws://10.40.255.22:9118/ws/data/redis"
    );

    private final StandardWebSocketClient webSocketClient;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    // 使用无界队列，避免队列满的问题
    private final Map<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();

    // 每个endpoint独立的线程池
    private final Map<String, ExecutorService> messageProcessors = new ConcurrentHashMap<>();

    // 批量发送相关
    private static final int BATCH_SIZE = 200; // 批量发送大小
    private static final int BATCH_TIMEOUT_MS = 10; // 批量发送超时时间

    // 统计信息
    private final Map<String, AtomicLong> sentMessageCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queueSizeCount = new ConcurrentHashMap<>();

    // 连接状态
    private final Map<String, AtomicBoolean> connectionStatus = new ConcurrentHashMap<>();

    // 重连机制
    private final ScheduledExecutorService reconnectExecutor = Executors.newScheduledThreadPool(5);

    @Autowired
    public PersistentWebSocketClient(StandardWebSocketClient webSocketClient) {
        this.webSocketClient = webSocketClient;
        initializeConnections();
        startMessageProcessing();
    }

    private void initializeConnections() {
        for (String endpoint : ENDPOINTS) {
            // 使用LinkedBlockingQueue无界队列
            messageQueues.put(endpoint, new LinkedBlockingQueue<>());

            // 每个endpoint独立的线程池，提高并发处理能力
            messageProcessors.put(endpoint, Executors.newFixedThreadPool(3)); // 3个处理线程

            // 初始化统计信息
            sentMessageCount.put(endpoint, new AtomicLong(0));
            queueSizeCount.put(endpoint, new AtomicLong(0));
            connectionStatus.put(endpoint, new AtomicBoolean(false));

            connect(endpoint);
        }
    }

    private void connect(String endpoint) {
        try {
            WebSocketHandler handler = new InternalWebSocketHandler(endpoint);
            WebSocketSession session = webSocketClient.doHandshake(handler, new WebSocketHttpHeaders(), new URI(endpoint)).get();
            activeSessions.put(endpoint, session);
            connectionStatus.get(endpoint).set(true);
            log.info("Connected to {}", endpoint);
        } catch (Exception e) {
            log.error("Failed to connect to {}: {}", endpoint, e.getMessage());
            connectionStatus.get(endpoint).set(false);
            // 启动重连
            scheduleReconnect(endpoint);
        }
    }

    private void scheduleReconnect(String endpoint) {
        reconnectExecutor.schedule(() -> {
            if (!connectionStatus.get(endpoint).get()) {
                log.info("Attempting to reconnect to {}", endpoint);
                connect(endpoint);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void startMessageProcessing() {
        for (String endpoint : ENDPOINTS) {
            ExecutorService processor = messageProcessors.get(endpoint);

            // 为每个endpoint启动多个处理线程
            for (int i = 0; i < 3; i++) {
                processor.submit(() -> processMessagesForEndpoint(endpoint));
            }
        }
    }

    private void processMessagesForEndpoint(String endpoint) {
        BlockingQueue<String> queue = messageQueues.get(endpoint);
        List<String> batch = new ArrayList<>(BATCH_SIZE);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 批量收集消息
                String message = queue.poll(BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (message != null) {
                    batch.add(message);

                    // 尝试收集更多消息到批次中
                    while (batch.size() < BATCH_SIZE) {
                        String nextMessage = queue.poll();
                        if (nextMessage == null) break;
                        batch.add(nextMessage);
                    }

                    // 发送批次
                    sendBatch(endpoint, batch);
                    batch.clear();
                }

                // 更新队列大小统计
                queueSizeCount.get(endpoint).set(queue.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void sendBatch(String endpoint, List<String> messages) {
        if (messages.isEmpty()) return;

        WebSocketSession session = activeSessions.get(endpoint);
        if (session == null || !session.isOpen()) {
            log.warn("Session not available for {}, messages will be requeued", endpoint);
            connectionStatus.get(endpoint).set(false);
            scheduleReconnect(endpoint);

            // 将消息重新放回队列
            BlockingQueue<String> queue = messageQueues.get(endpoint);
            for (String message : messages) {
                queue.offer(message);
            }
            return;
        }

        // 异步发送消息
        CompletableFuture.runAsync(() -> {
            for (String message : messages) {
                try {
                    session.sendMessage(new TextMessage(message));
                    sentMessageCount.get(endpoint).incrementAndGet();
                } catch (IOException e) {
                    log.error("Failed to send message to {}: {}", endpoint, e.getMessage());
                    connectionStatus.get(endpoint).set(false);
                    scheduleReconnect(endpoint);

                    // 发送失败的消息重新入队
                    messageQueues.get(endpoint).offer(message);
                    break;
                }
            }
        });
    }

    public void sendMessage(String endpoint, String message) {
        BlockingQueue<String> queue = messageQueues.get(endpoint);
        if (queue != null) {
            // 直接入队，无界队列不会阻塞
            queue.offer(message);
        } else {
            log.warn("No queue found for endpoint: {}", endpoint);
        }
    }

    // ====================== 便捷发送方法 ======================
    public void sendAlarm(String message) {
        sendMessage(ENDPOINTS.get(0), message);
    }

    public void sendStatistic(String message) {
        sendMessage(ENDPOINTS.get(1), message);
    }

    public void sendRedis(String message) {
        sendMessage(ENDPOINTS.get(2), message);
    }

    // ====================== 高级发送方法 ======================

    /**
     * 批量发送消息
     */
    public void sendMessagesBatch(String endpoint, List<String> messages) {
        BlockingQueue<String> queue = messageQueues.get(endpoint);
        if (queue != null) {
            queue.addAll(messages);
        }
    }

    /**
     * 优先级发送（立即处理）
     */
    public void sendMessageUrgent(String endpoint, String message) {
        WebSocketSession session = activeSessions.get(endpoint);
        if (session != null && session.isOpen()) {
            CompletableFuture.runAsync(() -> {
                try {
                    session.sendMessage(new TextMessage(message));
                    sentMessageCount.get(endpoint).incrementAndGet();
                } catch (IOException e) {
                    log.error("Failed to send urgent message to {}: {}", endpoint, e.getMessage());
                    // 发送失败则降级为普通消息
                    sendMessage(endpoint, message);
                }
            });
        } else {
            // 连接不可用，降级为普通消息
            sendMessage(endpoint, message);
        }
    }

    // ====================== 监控方法 ======================

    public Map<String, Long> getQueueSizes() {
        Map<String, Long> sizes = new HashMap<>();
        for (String endpoint : ENDPOINTS) {
            sizes.put(endpoint, queueSizeCount.get(endpoint).get());
        }
        return sizes;
    }

    public Map<String, Long> getSentMessageCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (String endpoint : ENDPOINTS) {
            counts.put(endpoint, sentMessageCount.get(endpoint).get());
        }
        return counts;
    }

    public Map<String, Boolean> getConnectionStatus() {
        Map<String, Boolean> status = new HashMap<>();
        for (String endpoint : ENDPOINTS) {
            status.put(endpoint, connectionStatus.get(endpoint).get());
        }
        return status;
    }

    public void checkConnections() {
        for (String endpoint : ENDPOINTS) {
            WebSocketSession session = activeSessions.get(endpoint);
            if (session == null || !session.isOpen()) {
                log.warn("Connection to {} is closed or not established, reconnecting...", endpoint);
                connectionStatus.get(endpoint).set(false);
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
            connectionStatus.get(endpoint).set(true);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            log.warn("Connection closed for {}: {} (code: {}, reason: {})", endpoint, status, status.getCode(), status.getReason());
            connectionStatus.get(endpoint).set(false);
            cleanupConnection(endpoint);
            scheduleReconnect(endpoint);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            log.error("Transport error for {}: {}", endpoint, exception.getMessage());
            connectionStatus.get(endpoint).set(false);
            cleanupConnection(endpoint);
            scheduleReconnect(endpoint);
        }
    }

    @PreDestroy
    public void destroy() {
        // 关闭所有线程池
        for (ExecutorService processor : messageProcessors.values()) {
            processor.shutdownNow();
        }

        reconnectExecutor.shutdownNow();

        // 清理连接
        for (String endpoint : ENDPOINTS) {
            cleanupConnection(endpoint);
        }
    }
}