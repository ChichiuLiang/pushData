package org.example.redis;

import org.example.service.PushDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


/**
 * @author lee
 * @date 2023/11/10
 */
@Component
public class RedisMessageUtils {

    @Resource
    private PushDataService pushDataService;

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageUtils.class);

    public void inputRedisMsg(String topic,String context){
        String url = "http://localhost:8001/receive/receiveData";
        pushDataService.pushData(context,topic, url);
    }
}
