//package org.example.handler;
//
//import org.example.service.WebSocketService;
//import org.springframework.web.socket.CloseStatus;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//public class ClientWebSocketHandler extends TextWebSocketHandler {
//    private final WebSocketService webSocketService;
//
//    public ClientWebSocketHandler(WebSocketService webSocketService) {
//        this.webSocketService  = webSocketService;
//    }
//
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) {
//        webSocketService.updateSession(session);
//    }
//
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
//        webSocketService.updateSession(null);
//    }
//}