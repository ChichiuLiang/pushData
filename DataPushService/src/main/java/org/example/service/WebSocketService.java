package org.example.service;

import org.example.websocket.PersistentWebSocketClient;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    private final PersistentWebSocketClient persistentWebSocketClient;

    public WebSocketService(PersistentWebSocketClient persistentWebSocketClient) {
        this.persistentWebSocketClient = persistentWebSocketClient;
    }

    public void sendMessage(String jsonMessage) {
        persistentWebSocketClient.sendMessage(jsonMessage);
    }
}
