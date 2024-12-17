package org.example.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
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

    public void pushData(String context, String topic, String url ) {
        // 使用ConcurrentHashMap增强线程安全性
        ConcurrentHashMap<String, String> dataMap = new ConcurrentHashMap<>();

        if ("shequ".equals(energyName)) { // 防止NPE并确保字符串比较正确
            // 能源站网关号映射,将社区的网关转成其它的
            topic = replaceGatewayIds(topic);
            context = replaceGatewayIds(context);
        }

        dataMap.put("topic", topic);
        dataMap.put("data", context);

        HttpEntity<ConcurrentHashMap<String, String>> httpEntity = new HttpEntity<>(dataMap);

        try {
            // 异步处理网络请求（假设你已经配置了异步RestTemplate）
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);

            // 检查响应状态码是否为200 OK或其他成功的状态码
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Data pushed successfully.");
            } else {
                log.warn("Failed to push data. Status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error pushing data to {}: {} - Topic: {}, Context: {}", url, e.getMessage(), topic, context);
        }
    }

    private String replaceGatewayIds(String input) {
        // 重构重复代码，减少冗余
        return input
                .replace("01 00 1A 00 00 00 00 00 00 00 00 00 00 00 00", "F1 00 1A 00 00 00 00 00 00 00 00 00 00 00 00")
                .replace("02 00 02 04 00 04 03 00 00 08 04 04 00 00 01", "F2 00 02 04 00 04 03 00 00 08 04 04 00 00 01")
                .replace("02 00 02 04 00 06 00 05 01 03 04 06 00 00 01", "F2 00 02 04 00 06 00 05 01 03 04 06 00 00 01")
                .replace("02 00 12 00 00 00 00 00 00 00 00 00 00 00 00", "F2 00 12 00 00 00 00 00 00 00 00 00 00 00 00");
    }
}
