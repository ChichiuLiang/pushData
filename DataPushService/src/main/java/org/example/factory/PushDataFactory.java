package org.example.factory;

import org.example.strategy.CollectionRedisData;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 800416
 * @Date 2024/10/16
 */
@Component
public class PushDataFactory {

    @Resource
    private List<CollectionRedisData> strategies;


    //获取策略
    public CollectionRedisData getPushUrlStrategy(String deviceType){

        for (CollectionRedisData strategy:strategies){
            if (strategy.verify(deviceType)){
                return strategy;
            }
        }

        return null;
    }
}
