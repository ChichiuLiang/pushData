package org.example.strategy;


import com.alibaba.fastjson.JSONObject;

/**
 * @author lee
 * @date 2023/11/10
 */
public interface CollectionRedisData {
    String dataHandler(JSONObject data);

    boolean verify(String deviceType);
}
