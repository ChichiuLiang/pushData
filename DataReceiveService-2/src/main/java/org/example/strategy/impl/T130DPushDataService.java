package org.example.strategy.impl;

import com.alibaba.fastjson.JSONObject;
import org.example.strategy.CollectionRedisData;
import org.springframework.stereotype.Service;

/**
 * @author 800416
 * @Date 2024/10/22
 */
@Service
public class T130DPushDataService implements CollectionRedisData {
    @Override
    public String dataHandler(JSONObject data) {
        return null;
    }

    @Override
    public boolean verify(String deviceType) {
        return deviceType.contains("130D")||deviceType.contains("130d");
    }
}
