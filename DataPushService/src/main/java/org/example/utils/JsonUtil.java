package org.example.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class JsonUtil {


    public static String getValue(String json,String key) {
        if (json.isEmpty()){
            return "jsonObject为空";
        }
        // 根据键值获取描述，如果键值不存在，则返回 null 或者默认值
        JSONObject jsonObject = new JSONObject(JSON.parseObject(json));
        return jsonObject.getString(key);
    }


}
