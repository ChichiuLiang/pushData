package org.example.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;

/**
 * @author 800416
 * @Date 2024/10/16
 */
@Service
public class ReceiverService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void receiveData(String data){
        JSONObject msgObject = JSON.parseObject(data);

        String channel = msgObject.getString("topic");
        String context = msgObject.getString("data");

        redisTemplate.convertAndSend(channel,context);

        JSONObject jsonObject = JSON.parseObject(context);//解析内容信息JSON串
        String frameType = jsonObject.getString("frameType");//帧类型
        String deviceType = jsonObject.getString( "deviceType");//设备类型
        String dataType = jsonObject.getString( "dataType");//数据类型，能源数据，状态数据，控制数据
        String deviceAddress = jsonObject.getString( "address");//设备地址
        String frameDatetime = jsonObject.getString("dateTime");//数据帧上报时间
        JSONObject subJsonObj = jsonObject.getJSONObject("data");//有效数据
    }

}
