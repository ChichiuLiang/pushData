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

/**
 * @author 800416
 * @Date 2024/10/16
 */
@Slf4j
@Service
public class PushDataRedisService {
    @Resource
    private RestTemplate restTemplate;
    @Value("${energyName}")
    private String energyName;

    public void pushData(String context,String topic,String url){
        Map<String,String> dataMap = new HashMap<String, String>();
        if(energyName.equals("shequ")){
            //能源站映射,将社区的网关转成其它的
            topic = topic.replace("01 00 1A 00 00 00 00 00 00 00 00 00 00 00 00","F1 00 1A 00 00 00 00 00 00 00 00 00 00 00 00");
            topic = topic.replace("02 00 02 04 00 04 03 00 00 08 04 04 00 00 01","F2 00 02 04 00 04 03 00 00 08 04 04 00 00 01");
            topic = topic.replace("02 00 02 04 00 06 00 05 01 03 04 06 00 00 01","F2 00 02 04 00 06 00 05 01 03 04 06 00 00 01");
            topic = topic.replace("02 00 12 00 00 00 00 00 00 00 00 00 00 00 00","F2 00 12 00 00 00 00 00 00 00 00 00 00 00 00");
        }

        dataMap.put("topic",topic);
        dataMap.put("data",context);
        try {
            HttpEntity<Map<String,String>> httpEntity =  new HttpEntity<Map<String,String>>(dataMap);

            ResponseEntity<JSONObject> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, JSONObject.class);
            //System.out.println(response.toString());
        }catch (Exception e){
            log.error("数据推送异常:{} pushData(String context,String topic,String url) ",e.getMessage());
        }
    }
}
