package org.example.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.example.utils.ReplaceGatewayUtil;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.example.websocket.PersistentWebSocketClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PushDataRedisService {
    @Resource
    private RestTemplate restTemplate;
    @Value("${energyName}")
    private String energyName;
//    @Resource
//    private RabbitTemplate rabbitTemplate;
    @Autowired
    private PersistentWebSocketClient webSocketService;


    public void pushData(String context, String topic, String url) {
        // 使用ConcurrentHashMap增强线程安全性
        ConcurrentHashMap<String, String> dataMap = new ConcurrentHashMap<>();

        if ("shequ".equals(energyName)) {
            // 能源站网关号映射,将社区的网关转成其它的
            topic = ReplaceGatewayUtil.replaceGatewayIds(topic);
            context = ReplaceGatewayUtil.replaceGatewayIds(context);
        } else if ("shangJiaoDa".equals(energyName)) {
            // 能源站网关号映射,将社区的网关转成其它的
            topic = ReplaceGatewayUtil.replaceGatewayIdsBySJD(topic);
            context = ReplaceGatewayUtil.replaceGatewayIdsBySJD(context);
        }

        dataMap.put("topic", topic);
        dataMap.put("data", context);

        String jsonMessage = JSONObject.fromObject(dataMap).toString();

        webSocketService.sendRedis(jsonMessage);
    }



}
