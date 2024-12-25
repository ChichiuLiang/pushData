package org.example.service.impl;


import lombok.extern.slf4j.Slf4j;
import org.example.model.AlarmConfigModel;
import org.example.model.HomeInfoModel;
import org.example.repository.DeviceMonitorRecordV2Repository;
import org.example.service.AlarmTriggerService;
import org.example.service.ConfigQueryService;
import org.example.service.PushAlarmService;
import org.example.utils.JsonUtil;
import org.example.utils.ListUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
@Slf4j
@Component
public class AlarmTriggerServiceImpl implements AlarmTriggerService {
    private static final long ALARM_COOLDOWN_MILLIS = 30L * 24 * 60 * 60 * 1000; // 30天
    private static final long CANCEL_ALARM_COOLDOWN_MILLIS = 20 * 60 * 1000; // 10 minutes in milliseconds
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DeviceMonitorRecordV2Repository deviceMonitorRecordV2Repository;
//    @Autowired
//    private AlarmMethodsServiceImpl alarmMethodsService;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private AlarmConfigAttributeServiceImpl alarmConfigAttributeServiceImpl;
    @Autowired
    private ConfigQueryService configQueryService;

    @Autowired
    private PushAlarmService pushAlarmService;
    @Override
    @Async
    public void triggerAlarmAsync(String redisKey, AlarmConfigModel model, String message,String barCode,String  deviceAddress,String  fieldValue,String  dateTime) {
        //System.out.println(model.toString());
        Integer attributeId = Integer.parseInt(model.getAttributeId());
        String thresholdValueJson = model.getAlarmValue();
        String thresholdValue = model.getAlarmValue().replace("{","").replace("}","").replace("\"","");
        String attributeName = model.getAttributeNameCn();
        HomeInfoModel homeInfoModel = alarmConfigAttributeServiceImpl.getHomeInfoModel(barCode);
        String areaName = homeInfoModel.getAreaName();
        String homeName = homeInfoModel.getHomeName();
        String alarmLevelId = model.getAlarmLevelId();
        String alarmLevelName = model.getAlarmLevelName();
        String errorInfo = JsonUtil.getValue(model.getAlarmValue(),fieldValue);
        String indexKey = barCode + model.getTypeCode() + deviceAddress;
        String childName = alarmConfigAttributeServiceImpl.getIndexFromMap(indexKey);
        String deviceName = areaName + " " +homeName + model.getMoudleName() + childName;
        if (errorInfo == null || errorInfo.isEmpty()) {
            log.warn(String.format("上报错误值 箱子:%s deviceId:  time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s", deviceName,   dateTime, model.getAttributeNameCn(), fieldValue, errorInfo, message));
            return;
        }
        if (Long.parseLong(fieldValue, 16) < 1) {
            log.warn(String.format("Long.parseLong(fieldValue, 16)<1值 箱子:%s deviceId:  time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s", deviceName,  dateTime, model.getAttributeNameCn(), fieldValue, errorInfo, message));
            return;
        }
        if (errorInfo.equals("正常")) {
            log.warn(String.format("正常状态还报警? 异常 箱子:%s deviceId:  time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s", deviceName,  dateTime, model.getAttributeNameCn(), fieldValue, errorInfo, message));
            return;
        }

        //设置报警缓存
        boolean isAlarm = isAllowAlarm(redisKey,ALARM_COOLDOWN_MILLIS);
        if (!isAlarm) {
            return;
        }

        String sql = String.format("select id from device_base_info where home_id = (select id from home_info where bar_code ='%s' ) and device_type_id = %s and address = '%s' ", barCode, model.getDeviceTypeId(), deviceAddress);
        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
        if (ListUtil.isNull(query)) {
            log.warn(String.format("查无此报警设备 barCode:%s dataTypeId:%s address:%s", barCode, model.getDeviceTypeId(), deviceAddress));
            return;
        }
        Integer deviceId = Integer.parseInt(query.get(0).toString());
        Integer ifExistsAlarm = deviceMonitorRecordV2Repository.queryAlarmRecordExist(deviceId, attributeId);
        if (ifExistsAlarm > 0) {
            //存在该报警记录
            return;
        }

        //小于1及没有记录则插入
        try {
            //数据库录入
            deviceMonitorRecordV2Repository.insertRecords(attributeId,deviceId,dateTime,1,1,0,fieldValue);
            String convertedDeviceId = queryDeviceIdConversion(String.valueOf(deviceId));
            pushAlarmService.pushAlarm(model,homeInfoModel,convertedDeviceId);
            log.info(String.format("报警录入正常  ifExistsAlarm=%d ,箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s",ifExistsAlarm,deviceName,deviceId,dateTime,model.getAttributeNameCn(),fieldValue,errorInfo));
        }catch (Exception e){
            redisTemplate.delete(redisKey);
            log.error(String.format("开启报警 ifExistsAlarm<1:%d ,箱子:%s deviceId:%d time:%s 报警字段:%s 最后值:%s 错误值:%s",ifExistsAlarm,deviceName,deviceId,dateTime,model.getAttributeNameCn(),fieldValue,errorInfo));
        }
     }

    private String queryDeviceIdConversion(String deviceId) {
        try {
            return String.valueOf(configQueryService.getRemoteDeviceIdByLocalDeviceId(Integer.parseInt(deviceId)));
        } catch (Exception e) {
            log.error("设备ID转换失败: {}", e.getMessage(), e);
            return null; // 返回null表示转换失败
        }
    }

    @Override
    @Async
    public void cancelAlarmAsync(String redisKey, AlarmConfigModel model,String barCode,String  deviceAddress,String  fieldValue,String  dateTime) {
        if (!isAllowCancel(redisKey,CANCEL_ALARM_COOLDOWN_MILLIS)){
            //没有该报警缓存 或 没到取消缓存的时间
            return;
        }
        String sql  = String.format("select id from device_base_info where home_id = (select id from home_info where bar_code ='%s' ) and device_type_id = %s and address = '%s' ",barCode,model.getDeviceTypeId(),deviceAddress);
        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
        Integer deviceId = Integer.parseInt(query.get(0).toString());
        Integer attributeId = Integer.parseInt(model.getAttributeId());
        String attributeName = model.getAttributeNameCn();
        HomeInfoModel homeInfoModel = alarmConfigAttributeServiceImpl.getHomeInfoModel(barCode);
        String areaName = homeInfoModel.getAreaName();
        String homeName = homeInfoModel.getHomeName();
        String deviceName = areaName + " " +homeName + model.getMoudleName() ;
        String timeAndDeviceName = dateTime+" "+deviceName;

        //数据库写入
        List<Integer> ids = deviceMonitorRecordV2Repository.queryIdsRecordExist(deviceId,attributeId);
        if (ListUtil.isNull(ids)){
             return;
        }
        try{
            deviceMonitorRecordV2Repository.updateRecords(attributeId,deviceId,dateTime,0,1,1,ids.get(0));
            redisTemplate.delete(redisKey);
        }catch(Exception e){
            log.error(String.format("取消报警失败 barCode:%s dviceTypeId:%s address:%s deviceId:%d attributeId:%d",model.getBarCode(),model.getDeviceTypeId(),model.getDeviceAddress(),deviceId,attributeId));
        }

    }


    private boolean isAllowAlarm(String redisKey, Long time) {
        // Lua 脚本，用于原子操作
        String script =
                "local lastAlarmTime = tonumber(redis.call('GET', KEYS[1])) " +
                        "if lastAlarmTime == nil or (tonumber(ARGV[1]) - lastAlarmTime >= tonumber(ARGV[2])) then " +
                        "  redis.call('SET', KEYS[1], ARGV[1]) " +
                        "  return 1 " +
                        "else " +
                        "  return 0 " +
                        "end";

        // 获取当前时间戳
        long currentTime = System.currentTimeMillis();

        // 执行 Lua 脚本
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(redisKey),
                String.valueOf(currentTime),
                String.valueOf(time));

        // 如果脚本返回 1，表示可以触发报警
        return result != null && result == 1 ;
    }

    private boolean isAllowCancel(String redisKey, Long time) {
        // Lua 脚本，用于原子操作
        String script =
                "local lastAlarmTime = tonumber(redis.call('GET', KEYS[1])) " +
                        "if lastAlarmTime ~= nil and (tonumber(ARGV[1]) - lastAlarmTime >= tonumber(ARGV[2])) then " +
                        "  redis.call('DEL', KEYS[1]) " +  // 删除 key，表示取消报警
                        "  return 1 " +
                        "else " +
                        "  return 0 " +
                        "end";

        // 获取当前时间戳
        long currentTime = System.currentTimeMillis();

        // 执行 Lua 脚本
        Long result = (Long) redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(redisKey),
                String.valueOf(currentTime),
                String.valueOf(time));

        // 如果脚本返回 1，表示可以取消报警
        return result != null && result == 1 ;
    }

}
