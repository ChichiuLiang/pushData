package org.example.schedule;
import org.example.websocket.PersistentWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WebSocketHealthCheckTask {

    @Autowired
    private PersistentWebSocketClient webSocketClientManager;

    @Scheduled(fixedRate = 10000) // 每30秒执行一次
    public void checkWebSocketConnections() {
        webSocketClientManager.checkConnections();
    }
}