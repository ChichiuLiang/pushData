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

    public void receiveT1803Data(@RequestBody String data){
        JSONObject jsonObject = JSON.parseObject(data);

        String channel = jsonObject.getString("topic");
        String context = jsonObject.getString("data");

        redisTemplate.convertAndSend(channel,context);
    }


    public void receiveT2502Data(@RequestBody String data){
        JSONObject jsonObject = JSON.parseObject(data);

        String channel = jsonObject.getString("topic");
        String context = jsonObject.getString("data");

       redisTemplate.convertAndSend(channel,context);
    }
}
