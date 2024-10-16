package org.example.redis;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.factory.PushDataFactory;
import org.example.service.PushDataService;
import org.example.strategy.CollectionRedisData;
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
    private PushDataFactory pushDataFactory;
    @Resource
    private PushDataService pushDataService;

    private static final Logger logger = LoggerFactory.getLogger(RedisMessageUtils.class);


    public void inputRedisMsg(String topic,String context){

        try {
            JSONObject jsonObject = JSON.parseObject(context);//解析内容信息JSON串
            String frameType = jsonObject.getString("frameType");//帧类型
            String deviceType = jsonObject.getString( "deviceType");//设备类型
            String dataType = jsonObject.getString( "dataType");//数据类型，能源数据，状态数据，控制数据
            String deviceAddress = jsonObject.getString( "address");//设备地址
            String frameDatetime = jsonObject.getString("dateTime");//数据帧上报时间
            JSONObject subJsonObj = jsonObject.getJSONObject("data");//有效数据

            //不同类型的数据推送到不同的数据接收接口
            CollectionRedisData collectionRedisData = pushDataFactory.getPushUrlStrategy(deviceType);

            if (null != collectionRedisData){
                pushDataService.pushData(context,topic,collectionRedisData.getPushUrl());
            }
        }
        catch (Exception e){
            //logger.info("不正确的json串："+context);
        }

    }
}
