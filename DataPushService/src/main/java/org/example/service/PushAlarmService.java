package org.example.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.example.model.AlarmConfigModel;
import org.example.model.HomeInfoModel;
import org.example.utils.ReplaceGatewayUtil;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class PushAlarmService {

    @Resource
    private RestTemplate restTemplate;
    @Value("${energyName}")
    private String energyName;
    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;
    private WebSocketService webSocketService;
//    @Resource
//    private RabbitTemplate rabbitTemplate;

    public void pushAlarm(AlarmConfigModel model, HomeInfoModel homeInfoModel, String convertedDeviceId) {
        String barCode = model.getBarCode();
        if ("shequ".equals(energyName)) {
            barCode = ReplaceGatewayUtil.replaceGatewayIds(barCode);
        } else if ("shangJiaoDa".equals(energyName)) {
            barCode = ReplaceGatewayUtil.replaceGatewayIdsBySJD(barCode);
        }
        model.setBarCode(barCode);
        homeInfoModel.setBarCode(barCode);
        model.setDeviceId(convertedDeviceId);

//        try {
            // 构建发送的消息体
            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("alarm", model);
            messageBody.put("homeInfo", homeInfoModel);

            // 转换为 JSON 字符串
           String jsonMessage = JSONObject.fromObject(messageBody).toString();
            webSocketService.sendMessage(jsonMessage);
//            // 使用 RabbitMQ 发送消息到队列 alarms-queue
//            rabbitTemplate.convertAndSend("alarm-msg", jsonMessage); // 队列名或路由键
//
//            log.info("告警数据已通过 RabbitMQ 推送至队列: alarms");
//        } catch (Exception e) {
//            log.error("通过 RabbitMQ 推送告警数据失败: {}", e.getMessage(), e);
//        }
    }


}
