package org.example.schedule;

import lombok.extern.slf4j.Slf4j;
import org.example.websocket.PersistentWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class WebSocketHealthCheckTask {

    @Autowired
    private PersistentWebSocketClient webSocketClientManager;

    /**
     * 连接健康检查 - 每10秒执行一次
     */
    @Scheduled(fixedRate = 10000)
    public void checkWebSocketConnections() {
        webSocketClientManager.checkConnections();
    }

    /**
     * 性能监控 - 每30秒输出一次统计信息
     */
    @Scheduled(fixedRate = 30000)
    public void monitorPerformance() {
        try {
            Map<String, Long> queueSizes = webSocketClientManager.getQueueSizes();
            Map<String, Long> sentCounts = webSocketClientManager.getSentMessageCounts();
            Map<String, Boolean> connectionStatus = webSocketClientManager.getConnectionStatus();

//            log.info("=== WebSocket Performance Report ===");
//
//            for (String endpoint : queueSizes.keySet()) {
//                log.info("Endpoint: {} | Queue Size: {} | Sent Messages: {} | Connected: {}",
//                        endpoint,
//                        queueSizes.get(endpoint),
//                        sentCounts.get(endpoint),
//                        connectionStatus.get(endpoint)
//                );
//            }

            // 队列大小告警
            for (Map.Entry<String, Long> entry : queueSizes.entrySet()) {
                if (entry.getValue() > 10000) {
                    log.warn("HIGH QUEUE SIZE ALERT: {} has {} messages in queue",
                            entry.getKey(), entry.getValue());
                }
            }

        } catch (Exception e) {
            log.error("Error during performance monitoring", e);
        }
    }

    /**
     * 队列清理 - 每5分钟执行一次（可选）
     * 如果某个endpoint长时间无法连接，可以考虑清理部分积压消息
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupStaleMessages() {
        try {
            Map<String, Long> queueSizes = webSocketClientManager.getQueueSizes();
            Map<String, Boolean> connectionStatus = webSocketClientManager.getConnectionStatus();

            for (Map.Entry<String, Long> entry : queueSizes.entrySet()) {
                String endpoint = entry.getKey();
                Long queueSize = entry.getValue();
                Boolean connected = connectionStatus.get(endpoint);

                // 如果连接断开且队列过大，发出警告
                if (!connected && queueSize > 50000) {
                    log.warn("QUEUE OVERFLOW WARNING: {} is disconnected with {} messages queued. " +
                            "Consider manual intervention if this persists.", endpoint, queueSize);
                }
            }
        } catch (Exception e) {
            log.error("Error during queue cleanup check", e);
        }
    }
}