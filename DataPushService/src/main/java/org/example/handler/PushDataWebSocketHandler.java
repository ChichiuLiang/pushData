//package org.example.handler;
//
//import org.springframework.web.socket.TextMessage;
//import org.springframework.web.socket.WebSocketHttpHeaders;
//import org.springframework.web.socket.WebSocketSession;
//import org.springframework.web.socket.client.standard.StandardWebSocketClient;
//import org.springframework.web.socket.handler.TextWebSocketHandler;
//
//import java.io.IOException;
//import java.net.URI;
//
//public class PushDataWebSocketHandler extends TextWebSocketHandler {
//
//    private final String url;
//    private final WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
//    private WebSocketSession session; // 新增session成员变量
//
//    public PushDataWebSocketHandler(String url) {
//        this.url = url;
//    }
//
//    public void connect() throws Exception {
//        StandardWebSocketClient client = new StandardWebSocketClient();
//        client.doHandshake(this, headers, new URI(url));
//    }
//
//    public void sendData(String data) {
//        if (session != null && session.isOpen()) {
//            try {
//                session.sendMessage(new TextMessage(data));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    // 在连接建立时保存session
//    @Override
//    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
//        this.session = session;
//        System.out.println("WebSocket connection established.");
//    }
//
//    // 可选：重写 handleTextMessage 方法来处理服务器返回的消息
//    @Override
//    public void handleTextMessage(WebSocketSession session, TextMessage message) {
//        System.out.println("Received response: " + message.getPayload());
//    }
//
//    // 在连接关闭时清空session
//    @Override
//    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
//        if (this.session == session) {
//            this.session = null;
//        }
//        System.out.println("WebSocket connection closed.");
//    }
//}
