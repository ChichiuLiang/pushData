package org.example.utils;

import org.example.service.PushDataRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;



@Component
public class RedisMessageUtils {

    @Resource
    private PushDataRedisService pushDataRedisService;

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;
    public void inputRedisMsg(String topic,String context){
        String url =  remoteReceiveUrl + "/receiveData";
        pushDataRedisService.pushData(context,topic, url);
    }
}
