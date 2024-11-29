package org.example.service;

import net.sf.json.JSONObject;
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
@Service
public class PushDataRedisService {
    @Resource
    private RestTemplate restTemplate;

    public void pushData(String context,String topic,String url){
        Map<String,String> dataMap = new HashMap<String, String>();

        dataMap.put("topic",topic);
        dataMap.put("data",context);
        try {
            HttpEntity<Map<String,String>> httpEntity =  new HttpEntity<Map<String,String>>(dataMap);

            ResponseEntity<JSONObject> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, JSONObject.class);
            System.out.println(response.toString());
        }catch (Exception e){
            System.out.println(e.getMessage());
        }

    }
}
