package org.example.strategy.impl;

import net.sf.json.JSONObject;
import org.example.strategy.CollectionRedisData;
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
public class T2502PushDataService implements CollectionRedisData {


    public String getPushUrl(){
        return "http://localhost:6666/receiveT2502Data";
    }

    @Override
    public boolean verify(String deviceType) {
        return deviceType.contains("2502");
    }
}
