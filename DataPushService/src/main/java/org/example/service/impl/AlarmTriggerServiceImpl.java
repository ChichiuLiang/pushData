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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
@Slf4j
@Component
public class AlarmTriggerServiceImpl implements AlarmTriggerService {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private DeviceMonitorRecordV2Repository deviceMonitorRecordV2Repository;
//    @Autowired
//    private AlarmMethodsServiceImpl alarmMethodsService;

    @Autowired
    private AlarmConfigAttributeServiceImpl alarmConfigAttributeServiceImpl;
    @Autowired
    private ConfigQueryService configQueryService;

    @Autowired
    private PushAlarmService pushAlarmService;
    @Override
    @Async
    public void triggerAlarmAsync(String redisKey, AlarmConfigModel model, String message) {
        //System.out.println(model.toString());
        String sql  = String.format("select id from device_base_info where home_id = (select id from home_info where bar_code ='%s' ) and device_type_id = %s and address = '%s' ",model.getBarCode(),model.getDeviceTypeId(),model.getDeviceAddress());
        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
        if (ListUtil.isNull(query)){
            log.error(String.format("查无此报警设备 barCode:%s dataTypeId:%s address:%s",model.getBarCode(),model.getDeviceTypeId(),model.getDeviceAddress()));
        }
        Integer deviceId = Integer.parseInt(query.get(0).toString());
        Integer attributeId = Integer.parseInt(model.getAttributeId());
        String time = model.getTime();
        String lastValue = model.getLastValue();
        String thresholdValueJson = model.getAlarmValue();
        String thresholdValue = model.getAlarmValue().replace("{","").replace("}","").replace("\"","");
        String attributeName = model.getAttributeNameCn();
        HomeInfoModel homeInfoModel = alarmConfigAttributeServiceImpl.getHomeInfoModel(model.getBarCode());
        String areaName = homeInfoModel.getAreaName();
        String homeName = homeInfoModel.getHomeName();
        String alarmLevelId = model.getAlarmLevelId();
        String alarmLevelName = model.getAlarmLevelName();
        String errorInfo = JsonUtil.getValue(model.getAlarmValue(),lastValue);
        String indexKey = model.getBarCode()+model.getTypeCode()+model.getDeviceAddress();
        String childName = alarmConfigAttributeServiceImpl.getIndexFromMap(indexKey);
        String deviceName = areaName + " " +homeName + model.getMoudleName() + childName;
        if (errorInfo==null|| errorInfo.isEmpty()){
            //错误值不在定义范围
            log.info(String.format("上报错误值 箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s",deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo,message));
            return;
        }
        if ("0".equals(lastValue)){
            //离奇，为什么值为0还会判断为报警？？？难道是异步问题？？
            //错误值不在定义范围
            log.info(String.format("0值    箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s",deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo,message));
            return;
        }
        if (errorInfo.equals("正常")){
            //错误值不在定义范围
            log.info(String.format("正常状态? 异常  箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s 消息:%s",deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo,message));
            return;
        }


        Integer ifExistsAlarm = deviceMonitorRecordV2Repository.queryAlarmRecordExist(deviceId,attributeId);
        if (ifExistsAlarm<1){
            //小于1及没有记录则插入
            try {
                //紧急的才报警
                if ("3".equals(model.getAlarmLevelId())){
//                    //页面弹窗报警
//                    if (homeInfoModel.getBdAreaIdsSet().size()>0){
//                        for (Integer areaId:homeInfoModel.getBdAreaIdsSet()){
//                            Set<Integer> areaLevelIds = alarmConfigAttributeServiceImpl.getBDAreaLevelId(areaId);
//                            for (Integer areaLevelId:areaLevelIds){
//                                //页面弹窗报警
//                                //String response = alarmMethodsService.sendToSocket(areaId,time,deviceId,deviceName,lastValue,thresholdValue,attributeName,errorInfo,areaName,homeName,alarmLevelId,alarmLevelName,areaLevelId);
//                            }
//                        }
//                    }

//                    //短信弹窗报警
//                    if (homeInfoModel.getBdAreaLevelIdsSet().size()>0){
//                        List<String> phoneList = deviceMonitorRecordV2Repository.queryPhoneNumber(new ArrayList<>(homeInfoModel.getBdAreaLevelIdsSet()));
//                        String msg  = String.format("%s %s 报警等级:[%s:%s],%s %s %s",areaName,homeName,alarmLevelId,alarmLevelName,model.getMoudleName(),attributeName,errorInfo);
//                        String[] keywords = {time, msg , String.valueOf(lastValue)," " ," ",thresholdValue};
//                        //alarmMethodsService.sendSMSForStorage(134,phoneList,keywords );
//                    }
                }
                //数据库录入
                deviceMonitorRecordV2Repository.insertRecords(attributeId,deviceId,time,1,1,0,model.getLastValue());
                String convertedDeviceId = queryDeviceIdConversion(String.valueOf(deviceId));
                pushAlarmService.pushAlarm(model,homeInfoModel,convertedDeviceId);
                //log.info(String.format("报警录入正常  ifExistsAlarm=%d ,箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s",ifExistsAlarm,deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo));
            }catch (Exception e){
                log.error(String.format("开启报警 ifExistsAlarm<1:%d ,箱子:%s deviceId:%d time:%s 报警字段:%s 最后值:%s 错误值:%s",ifExistsAlarm,deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo));
            }

            return;
        }
        //log.info(String.format("已报警，未恢复 ifExistsAlarm<1:%d ,箱子:%s deviceId:%d time:%s 报警字段%s 最后值:%s 错误值:%s",ifExistsAlarm,deviceName,deviceId,model.getTime(),model.getAttributeNameCn(),model.getLastValue(),errorInfo));
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
    public void cancelAlarmAsync(String redisKey, AlarmConfigModel model) {
        String sql  = String.format("select id from device_base_info where home_id = (select id from home_info where bar_code ='%s' ) and device_type_id = %s and address = '%s' ",model.getBarCode(),model.getDeviceTypeId(),model.getDeviceAddress());
        List<Object> query = entityManager.createNativeQuery(sql).getResultList();
        Integer deviceId = Integer.parseInt(query.get(0).toString());
        Integer attributeId = Integer.parseInt(model.getAttributeId());
        String time = model.getTime();
        String lastValue = model.getLastValue();
        String attributeName = model.getAttributeNameCn();
        HomeInfoModel homeInfoModel = alarmConfigAttributeServiceImpl.getHomeInfoModel(model.getBarCode());
        String areaName = homeInfoModel.getAreaName();
        String homeName = homeInfoModel.getHomeName();
        String deviceName = areaName + " " +homeName + model.getMoudleName() ;
        String timeAndDeviceName = time+deviceName;
//        //发送短信
//        String[] keywords = {timeAndDeviceName,attributeName,lastValue};
//        List<String> phoneList = deviceMonitorRecordV2Repository.queryPhoneNumber(new ArrayList<>(homeInfoModel.getBdAreaLevelIdsSet()));
        //alarmMethodsService.sendSMSForStorage(134,phoneList,keywords);

        //数据库写入
        List<Integer> ids = deviceMonitorRecordV2Repository.queryIdsRecordExist(deviceId,attributeId);
        if (ListUtil.isNull(ids)){
            //log.error(String.format("查无此设备报警记录 barCode:%s dviceTypeId:%s address:%s deviceId:%d attributeId:%d",model.getBarCode(),model.getDeviceTypeId(),model.getDeviceAddress(),deviceId,attributeId));
            return;
        }
        deviceMonitorRecordV2Repository.updateRecords(attributeId,deviceId,model.getTime(),0,1,1,ids.get(0));
    }



}
