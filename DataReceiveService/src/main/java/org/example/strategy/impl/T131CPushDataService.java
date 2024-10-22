package org.example.strategy.impl;

import com.alibaba.fastjson.JSONObject;
import org.example.strategy.CollectionRedisData;
import org.springframework.stereotype.Service;

/**
 * @author 800416
 * @Date 2024/10/22
 */
@Service
public class T131CPushDataService implements CollectionRedisData {
    @Override
    public String dataHandler(JSONObject data) {
        return null;
    }

    @Override
    public boolean verify(String deviceType) {
        return deviceType.contains("131C")||deviceType.contains("131c");
    }
}
