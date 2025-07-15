package org.example.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    // ====================== 配置常量 ======================
    private static final String[] ENDPOINTS = {
            "ws://localhost:8092/ws/data/alarm",
            "ws://localhost:8092/ws/data/statistic",
            "ws://localhost:8092/ws/data/redis"
    };

    // 队列配置
    private static final int INITIAL_QUEUE_CAPACITY = 2000;
    private static final int MAX_QUEUE_CAPACITY = 5000;
    private static final int QUEUE_WARNING_THRESHOLD = 3500;    // 70%
    private static final int QUEUE_CRITICAL_THRESHOLD = 4500;   // 90%
    private static final int QUEUE_EMERGENCY_THRESHOLD = 4750;  // 95%
    private static final int EMERGENCY_RETAIN_SIZE = 1000;      // 紧急保留消息数

    // 重连策略
    private static final int MAX_RECONNECT_ATTEMPTS = 30;
    private static final long INITIAL_RECONNECT_DELAY_MS = 2000;
    private static final long MAX_RECONNECT_DELAY_MS = 300000; // 5分钟
    private static final double RECONNECT_BACKOFF_FACTOR = 1.5;

    // 发送策略
    private static final int NORMAL_SEND_INTERVAL_MS = 50;
    private static final int HIGH_LOAD_SEND_INTERVAL_MS = 10;
    private static final int EMERGENCY_SEND_INTERVAL_MS = 1;
    private static final int MAX_BATCH_SIZE = 20;

    // ====================== 实例变量 ======================
    private final StandardWebSocketClient webSocketClient;
    private final ThreadPoolTaskScheduler taskScheduler;

    // 连接状态
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> senderTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> reconnectTasks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> queueLocks = new ConcurrentHashMap<>();

    // 新增：连接状态跟踪
    private final Map<String, AtomicLong> reconnectAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lastConnectTime = new ConcurrentHashMap<>();

    // 监控统计
    private final Map<String, AtomicLong> sentCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> discardCounters = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> failureCounters = new ConcurrentHashMap<>();

    private final Map<String, ReentrantLock> sendLocks = new ConcurrentHashMap<>();


    @Autowired
    public PersistentWebSocketClient(StandardWebSocketClient webSocketClient,
                                     ThreadPoolTaskScheduler taskScheduler) {
        this.webSocketClient = webSocketClient;
        this.taskScheduler = taskScheduler;

        // 初始化所有端点资源
        for (String endpoint : ENDPOINTS) {
            sendLocks.put(endpoint, new ReentrantLock(true));
            messageQueues.put(endpoint, new LinkedBlockingQueue<>(INITIAL_QUEUE_CAPACITY));
            queueLocks.put(endpoint, new ReentrantLock(true)); // 公平锁
            sentCounters.put(endpoint, new AtomicLong(0));
            discardCounters.put(endpoint, new AtomicLong(0));
            failureCounters.put(endpoint, new AtomicLong(0));
            reconnectAttempts.put(endpoint, new AtomicLong(0));
            lastConnectTime.put(endpoint, 0L);
        }

        // 启动监控任务
        startMonitorTask();
    }

    // ====================== 初始化方法 ======================
    @PostConstruct
    public void initialize() {
        //log.info("Initializing WebSocket connections...");

        // 延迟启动，确保所有Bean都已初始化
        taskScheduler.schedule(() -> {
            //log.info("Starting initial connections...");
            for (String endpoint : ENDPOINTS) {
                connectWithRetry(endpoint, 0);
            }
        }, new Date(System.currentTimeMillis() + 1000));

        // 启动健康检查任务
        startHealthCheckTask();
    }

    /**
     * 健康检查任务
     */
    private void startHealthCheckTask() {
        taskScheduler.scheduleAtFixedRate(() -> {
            for (String endpoint : ENDPOINTS) {
                try {
                    WebSocketSession session = activeSessions.get(endpoint);
                    ScheduledFuture<?> reconnectTask = reconnectTasks.get(endpoint);
                    long reconnectAttempt = reconnectAttempts.get(endpoint).get();

                    boolean isConnected = session != null && session.isOpen();
                    boolean isReconnecting = reconnectTask != null && !reconnectTask.isCancelled() && !reconnectTask.isDone();

                    // 如果连接断开且没有重连任务在进行，强制重连
                    if (!isConnected && !isReconnecting) {
                        log.warn("Health check detected disconnected endpoint {} without reconnection task, forcing reconnect", endpoint);
                        connectWithRetry(endpoint, (int)reconnectAttempt);
                    }

                    // 如果重连尝试过多，重置计数器
                    if (reconnectAttempt >= MAX_RECONNECT_ATTEMPTS) {
                        log.warn("Resetting reconnect attempts for {} after reaching maximum", endpoint);
                        reconnectAttempts.get(endpoint).set(0);
                        connectWithRetry(endpoint, 0);
                    }

                } catch (Exception e) {
                    log.error("Health check error for {}: {}", endpoint, e.getMessage());
                }
            }
        }, 10000); // 每10秒健康检查一次
    }

    // ====================== 连接管理 ======================
    private void connectWithRetry(String endpoint, int attempt) {
        if (attempt >= MAX_RECONNECT_ATTEMPTS) {
            log.error("Maximum reconnection attempts ({}) reached for {}", MAX_RECONNECT_ATTEMPTS, endpoint);
            reconnectAttempts.get(endpoint).set(attempt);
            return;
        }

        cancelReconnectTask(endpoint);
        reconnectAttempts.get(endpoint).set(attempt);

        //log.info("Attempting to connect to {} (attempt {}/{})", endpoint, attempt + 1, MAX_RECONNECT_ATTEMPTS);

        try {
            URI uri = new URI(endpoint);
            WebSocketHandler handler = new InternalWebSocketHandler(endpoint);

            // 同步调用，等待连接结果
            WebSocketSession session = webSocketClient.doHandshake(handler, new WebSocketHttpHeaders(), uri).get(10, TimeUnit.SECONDS);

            // 如果到这里说明连接成功了，但我们还是需要等待 afterConnectionEstablished 回调
            //log.info("Handshake completed for {}, waiting for connection establishment...", endpoint);

        } catch (TimeoutException e) {
            log.error("Connection timeout for {}: {}", endpoint, e.getMessage());
            scheduleReconnect(endpoint, attempt);
        } catch (Exception e) {
            log.error("Connection attempt failed for {}: {}", endpoint, e.getMessage());
            scheduleReconnect(endpoint, attempt);
        }
    }

    private void scheduleReconnect(String endpoint, int attempt) {
        long delay = calculateReconnectDelay(attempt);
        log.warn("Scheduling reconnection in {}ms for {} (attempt will be {})", delay, endpoint, attempt + 1);

        ScheduledFuture<?> reconnectTask = taskScheduler.schedule(() -> {
            //log.info("Executing scheduled reconnection for {} (attempt {})", endpoint, attempt + 1);
            connectWithRetry(endpoint, attempt + 1);
        }, new Date(System.currentTimeMillis() + delay));

        reconnectTasks.put(endpoint, reconnectTask);
        log.debug("Reconnect task scheduled for {}", endpoint);
    }

    private long calculateReconnectDelay(int attempt) {
        if (attempt == 0) return INITIAL_RECONNECT_DELAY_MS;

        long delay = (long)(INITIAL_RECONNECT_DELAY_MS * Math.pow(RECONNECT_BACKOFF_FACTOR, attempt));
        return Math.min(delay, MAX_RECONNECT_DELAY_MS);
    }

    // ====================== WebSocket处理器 ======================
    private class InternalWebSocketHandler extends TextWebSocketHandler {
        private final String endpoint;

        InternalWebSocketHandler(String endpoint) {
            this.endpoint = endpoint;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            //log.info("Connected to {}", endpoint);
            activeSessions.put(endpoint, session);
            lastConnectTime.put(endpoint, System.currentTimeMillis());
            reconnectAttempts.get(endpoint).set(0);

            // 关键修复：确保重连后启动发送器
            startMessageSender(endpoint);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            log.warn("Connection closed for {}: {} (code: {}, reason: {})",
                    endpoint, status, status.getCode(), status.getReason());
            cleanupConnection(endpoint);

            // 立即开始重连，不要延迟
            //log.info("Starting immediate reconnection for {}", endpoint);
            connectWithRetry(endpoint, 0);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            log.error("Transport error for {}: {}", endpoint, exception.getMessage(), exception);
            cleanupConnection(endpoint);

            // 立即开始重连
            //log.info("Starting immediate reconnection after transport error for {}", endpoint);
            connectWithRetry(endpoint, 0);
        }
    }

    // ====================== 消息发送管理 ======================
    private void startMessageSender(String endpoint) {
        // 修复：先取消旧任务，避免重复启动
        cancelSenderTask(endpoint);

        // 确保会话存在且有效
        WebSocketSession session = activeSessions.get(endpoint);
        if (session == null || !session.isOpen()) {
            log.warn("Cannot start sender for {}: no active session", endpoint);
            return;
        }

        //log.info("Starting message sender for {}", endpoint);
        SmartSender sender = new SmartSender(endpoint);

        // 修复：使用更稳定的调度方式
        ScheduledFuture<?> task = taskScheduler.scheduleWithFixedDelay(
                sender,
                new Date(System.currentTimeMillis() + 100), // 稍微延迟启动
                sender.getCurrentInterval()
        );

        senderTasks.put(endpoint, task);
        //log.info("Message sender started for {}", endpoint);
    }

    private class SmartSender implements Runnable {
        private final String endpoint;
        private volatile int currentInterval;
        private volatile int currentBatchSize;
        private volatile long lastRunTime = 0;

        SmartSender(String endpoint) {
            this.endpoint = endpoint;
            this.currentInterval = NORMAL_SEND_INTERVAL_MS;
            this.currentBatchSize = 1;
        }

        @Override
        public void run() {
            try {
                WebSocketSession session = activeSessions.get(endpoint);
                BlockingQueue<String> queue = messageQueues.get(endpoint);

                // 检查会话状态
                if (session == null || !session.isOpen()) {
                    log.warn("No active session for {}, pausing sender", endpoint);
                    return;
                }

                // 调整发送参数
                adjustSendingParameters(queue.size());

                // 批量发送消息
                int sent = sendBatchMessages(session, queue, currentBatchSize);
                if (sent > 0) {
                    sentCounters.get(endpoint).addAndGet(sent);
                    log.debug("Sent {} messages to {}", sent, endpoint);
                }

                // 更新运行时间
                lastRunTime = System.currentTimeMillis();

                // 紧急检查
                if (queue.size() >= QUEUE_EMERGENCY_THRESHOLD) {
                    handleQueueEmergency(endpoint);
                }

                // 修复：动态调整调度间隔
                rescheduleIfNeeded();

            } catch (Exception e) {
                failureCounters.get(endpoint).incrementAndGet();
                log.error("Sender error for {}: {}", endpoint, e.getMessage());

                // 如果是连接问题，触发重连
                if (e instanceof IOException) {
                    cleanupConnection(endpoint);
                    scheduleReconnect(endpoint, 0);
                }
            }
        }

        private void adjustSendingParameters(int queueSize) {
            int newInterval;
            int newBatchSize;

            if (queueSize > QUEUE_CRITICAL_THRESHOLD) {
                newInterval = EMERGENCY_SEND_INTERVAL_MS;
                newBatchSize = Math.min(MAX_BATCH_SIZE, queueSize / 10);
            } else if (queueSize > QUEUE_WARNING_THRESHOLD) {
                newInterval = HIGH_LOAD_SEND_INTERVAL_MS;
                newBatchSize = Math.min(MAX_BATCH_SIZE / 2, queueSize / 20);
            } else {
                newInterval = NORMAL_SEND_INTERVAL_MS;
                newBatchSize = 1;
            }

            // 只有当参数改变时才重新调度
            if (newInterval != currentInterval) {
                currentInterval = newInterval;
                currentBatchSize = newBatchSize;
                log.debug("Adjusted sending parameters for {}: interval={}ms, batchSize={}",
                        endpoint, currentInterval, currentBatchSize);
            }
        }

        private int sendBatchMessages(WebSocketSession session, BlockingQueue<String> queue, int batchSize) throws IOException {
            ReentrantLock sendLock = sendLocks.get(endpoint);
            sendLock.lock();
            try {
                int sent = 0;
                for (int i = 0; i < batchSize; i++) {
                    String message = queue.poll();
                    if (message == null) break;

                    try {
                        session.sendMessage(new TextMessage(message));
                        sent++;
                    } catch (IOException e) {
                        failureCounters.get(endpoint).incrementAndGet();
                        log.error("Failed to send message to {}: {}", endpoint, e.getMessage());
                        queue.offer(message); // 可选：重入队
                        throw e;
                    }
                }
                return sent;
            } finally {
                sendLock.unlock();
            }
        }

        private void rescheduleIfNeeded() {
            // 如果间隔改变，重新调度任务
            ScheduledFuture<?> currentTask = senderTasks.get(endpoint);
            if (currentTask != null) {
                // 检查是否需要重新调度（这里简化处理）
                // 在实际应用中，可能需要更复杂的逻辑
            }
        }

        int getCurrentInterval() {
            return currentInterval;
        }

        long getLastRunTime() {
            return lastRunTime;
        }
    }

    // ====================== 队列紧急处理 ======================
    private void handleQueueEmergency(String endpoint) {
        ReentrantLock lock = queueLocks.get(endpoint);
        if (lock.tryLock()) {
            try {
                BlockingQueue<String> queue = messageQueues.get(endpoint);
                if (queue.size() >= QUEUE_EMERGENCY_THRESHOLD) {
                    // 保留最新消息
                    List<String> retainedMessages = new ArrayList<>();
                    queue.drainTo(retainedMessages);

                    int originalSize = retainedMessages.size();
                    if (originalSize > EMERGENCY_RETAIN_SIZE) {
                        retainedMessages = retainedMessages.subList(
                                originalSize - EMERGENCY_RETAIN_SIZE,
                                originalSize
                        );
                        long discarded = originalSize - EMERGENCY_RETAIN_SIZE;
                        discardCounters.get(endpoint).addAndGet(discarded);
                        log.error("🚨 EMERGENCY: Discarded {} old messages from {}", discarded, endpoint);
                    }

                    // 创建新队列
                    BlockingQueue<String> newQueue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
                    newQueue.addAll(retainedMessages);
                    messageQueues.put(endpoint, newQueue);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    // ====================== 公共API ======================
    public enum SendPriority {
        NORMAL,     // 普通消息，可能被丢弃
        HIGH,       // 高优先级消息，尽量保留
        CRITICAL    // 关键消息，必须入队
    }

    /**
     * 发送消息到指定端点
     */
    public boolean sendToEndpoint(String endpoint, String message, SendPriority priority) {
        try {
            BlockingQueue<String> queue = messageQueues.get(endpoint);
            if (queue == null) {
                log.error("Endpoint not found: {}", endpoint);
                return false;
            }

            ReentrantLock lock = queueLocks.get(endpoint);
            lock.lock();
            try {
                switch (priority) {
                    case CRITICAL:
                        // 关键消息 - 确保入队
                        if (queue.remainingCapacity() == 0) {
                            queue.poll();  // 丢弃最旧的一条
                        }
                        queue.put(message);
                        return true;

                    case HIGH:
                        // 高优先级 - 尝试不丢弃
                        if (queue.remainingCapacity() > 0 || queue.size() < QUEUE_CRITICAL_THRESHOLD) {
                            return queue.offer(message, 2, TimeUnit.SECONDS);
                        }
                        // 队列过满时降级为普通消息处理
                        // 继续执行NORMAL逻辑

                    case NORMAL:
                    default:
                        // 普通消息 - 可以丢弃
                        if (queue.size() >= QUEUE_CRITICAL_THRESHOLD) {
                            if (!queue.offer(message, 100, TimeUnit.MILLISECONDS)) {
                                discardCounters.get(endpoint).incrementAndGet();
                                log.warn("Queue full, discarded message for {}", endpoint);
                                return false;
                            }
                        } else {
                            if (!queue.offer(message, 5, TimeUnit.SECONDS)) {
                                log.warn("Queue busy for {}", endpoint);
                                return false;
                            }
                        }
                        return true;
                }
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while sending to {}", endpoint);
            return false;
        } catch (Exception e) {
            failureCounters.get(endpoint).incrementAndGet();
            log.error("Error sending to {}: {}", endpoint, e.getMessage());
            return false;
        }
    }

    /**
     * 手动触发重连
     */
    public void manualReconnect(String endpoint) {
        //log.info("Manual reconnection triggered for {}", endpoint);
        cleanupConnection(endpoint);
        connectWithRetry(endpoint, 0);
    }

    /**
     * 强制重连所有端点
     */
    public void forceReconnectAll() {
        //log.info("Force reconnecting all endpoints...");
        for (String endpoint : ENDPOINTS) {
            manualReconnect(endpoint);
        }
    }

    /**
     * 检查发送器状态
     */
    public boolean isSenderActive(String endpoint) {
        ScheduledFuture<?> task = senderTasks.get(endpoint);
        return task != null && !task.isCancelled() && !task.isDone();
    }

    /**
     * 清空指定端点的消息队列
     */
    public void purgeQueue(String endpoint) {
        ReentrantLock lock = queueLocks.get(endpoint);
        lock.lock();
        try {
            BlockingQueue<String> queue = messageQueues.get(endpoint);
            if (queue != null) {
                int cleared = queue.size();
                queue.clear();
                discardCounters.get(endpoint).addAndGet(cleared);
                log.warn("Purged {} messages from {}", cleared, endpoint);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 替换指定端点的消息队列内容
     */
    public void replaceQueue(String endpoint, Collection<String> newMessages) {
        ReentrantLock lock = queueLocks.get(endpoint);
        lock.lock();
        try {
            BlockingQueue<String> newQueue = new LinkedBlockingQueue<>(MAX_QUEUE_CAPACITY);
            newQueue.addAll(newMessages);
            messageQueues.put(endpoint, newQueue);
            //log.info("Replaced queue for {} with {} messages", endpoint, newMessages.size());
        } finally {
            lock.unlock();
        }
    }

    // ====================== 便捷发送方法 ======================
    public boolean sendAlarm(String message) {
        return sendToEndpoint(ENDPOINTS[0], message, SendPriority.NORMAL);
    }

    public boolean sendStatistic(String message) {
        // 统计信息通常较为重要，使用高优先级
        return sendToEndpoint(ENDPOINTS[1], message, SendPriority.HIGH);
    }

    public boolean sendRedis(String message) {
        return sendToEndpoint(ENDPOINTS[2], message, SendPriority.NORMAL);
    }

    // ====================== 监控与统计 ======================
    private void startMonitorTask() {
        taskScheduler.scheduleAtFixedRate(() -> {
            for (String endpoint : ENDPOINTS) {
                try {
                    BlockingQueue<String> queue = messageQueues.get(endpoint);
                    WebSocketSession session = activeSessions.get(endpoint);
                    ScheduledFuture<?> senderTask = senderTasks.get(endpoint);
                    ScheduledFuture<?> reconnectTask = reconnectTasks.get(endpoint);

                    int size = queue != null ? queue.size() : -1;
                    boolean isConnected = session != null && session.isOpen();
                    boolean isSenderActive = senderTask != null && !senderTask.isCancelled() && !senderTask.isDone();
                    boolean isReconnecting = reconnectTask != null && !reconnectTask.isCancelled() && !reconnectTask.isDone();
                    long reconnectAttempt = reconnectAttempts.get(endpoint).get();

//                    log.info("Endpoint {} - Connected: {}, Sender: {}, Reconnecting: {}, Queue: {}/{} ({}%), " +
//                                    "Sent: {}, Failed: {}, Discarded: {}, Reconnects: {}",
//                            endpoint,
//                            isConnected,
//                            isSenderActive,
//                            isReconnecting,
//                            size,
//                            queue != null ? queue.remainingCapacity() + size : -1,
//                            queue != null ? (int)(100.0 * size / (size + queue.remainingCapacity())) : -1,
//                            sentCounters.get(endpoint).get(),
//                            failureCounters.get(endpoint).get(),
//                            discardCounters.get(endpoint).get(),
//                            reconnectAttempt);

                    // 修复：检测发送器异常并重启
                    if (isConnected && !isSenderActive && queue.size() > 0) {
                        log.warn("Detected inactive sender for {} with {} queued messages, restarting...",
                                endpoint, queue.size());
                        startMessageSender(endpoint);
                    }

                    // 修复：检测连接异常并强制重连
                    if (!isConnected && !isReconnecting && reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                        log.warn("Detected disconnected endpoint {} without active reconnection, forcing reconnect...", endpoint);
                        connectWithRetry(endpoint, (int)reconnectAttempt);
                    }

                    // 触发警告级别日志
                    if (size > QUEUE_EMERGENCY_THRESHOLD) {
                        log.error("🚨 EMERGENCY: Queue overloaded at {} for {}", size, endpoint);
                    } else if (size > QUEUE_CRITICAL_THRESHOLD) {
                        log.warn("⚠️ CRITICAL: Queue size {} for {}", size, endpoint);
                    } else if (size > QUEUE_WARNING_THRESHOLD) {
                        //log.info("Queue approaching limit: {} for {}", size, endpoint);
                    }
                } catch (Exception e) {
                    log.error("Monitor error for {}: {}", endpoint, e.getMessage());
                }
            }
        }, 5000); // 每5秒监控一次
    }

    // ====================== 资源清理 ======================
    private void cleanupConnection(String endpoint) {
        log.debug("Cleaning up connection for {}", endpoint);

        cancelSenderTask(endpoint);

        WebSocketSession session = activeSessions.remove(endpoint);
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("Error closing session for {}: {}", endpoint, e.getMessage());
            }
        }
    }

    private void cancelSenderTask(String endpoint) {
        ScheduledFuture<?> task = senderTasks.remove(endpoint);
        if (task != null) {
            task.cancel(false);
            log.debug("Cancelled sender task for {}", endpoint);
        }
    }

    private void cancelReconnectTask(String endpoint) {
        ScheduledFuture<?> task = reconnectTasks.remove(endpoint);
        if (task != null) {
            task.cancel(false);
            log.debug("Cancelled reconnect task for {}", endpoint);
        }
    }

    @PreDestroy
    public void shutdown() {
        //log.info("Shutting down WebSocket client...");

        // 关闭所有连接
        for (String endpoint : ENDPOINTS) {
            cleanupConnection(endpoint);
            cancelReconnectTask(endpoint);
        }

        // 清空所有队列
        messageQueues.values().forEach(Queue::clear);
    }
}