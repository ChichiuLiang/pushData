package org.example.redis;

import org.example.service.PushDataRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @author lee
 * @date 2023/11/10
 */
@Component
public class RedisMessageUtils {

    @Resource
    private PushDataRedisService pushDataRedisService;

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageUtils.class);

    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;
    public void inputRedisMsg(String topic,String context){
        String url =  remoteReceiveUrl + "/receiveData";
        pushDataRedisService.pushData(context,topic, url);
    }
}
