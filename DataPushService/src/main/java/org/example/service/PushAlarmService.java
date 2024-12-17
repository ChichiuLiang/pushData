package org.example.service;

import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.example.model.AlarmConfigModel;
import org.example.model.HomeInfoModel;
import org.example.utils.ReplaceGatewayUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Slf4j
@Service
public class PushAlarmService {

    @Resource
    private RestTemplate restTemplate;
    @Value("${energyName}")
    private String energyName;
    @Value("${remoteReceiveUrl}")
    private String remoteReceiveUrl;

    public void pushAlarm(AlarmConfigModel model, HomeInfoModel homeInfoModel,String convertedDeviceId) {
        String url = remoteReceiveUrl + "/alarms";
        String barCode = model.getBarCode();
        if(energyName.equals("shequ")){
            //能源站映射,将社区的网关转成其它的
            barCode = ReplaceGatewayUtil.replaceGatewayIds(barCode);
        }
        else if(energyName.equals("shangJiaoDa")){
            //上交大
            barCode = ReplaceGatewayUtil.replaceGatewayIdsBySJD(barCode);
        }
        model.setBarCode(barCode);
        homeInfoModel.setBarCode(barCode);
        model.setDeviceId(convertedDeviceId);
        try {
            HttpEntity<AlarmConfigModel> httpEntity =  new HttpEntity<AlarmConfigModel>(model);

            ResponseEntity<JSONObject> response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, JSONObject.class);
            log.info("告警数据推送结果:{} {}",response.toString(),model.toString());
        }catch (Exception e){
            log.error("告警数据推送异常:{} pushAlarm(AlarmConfigModel model{}, HomeInfoModel homeInfoModel{},String convertedDeviceId){} ",e.getMessage(),model,homeInfoModel,convertedDeviceId);
        }
    }

}
