package org.example.web;

import org.example.schedule.DataPushStatisticScheduledTask;
import org.example.service.impl.AlarmHandleServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestControl {
    @Autowired
    private DataPushStatisticScheduledTask dataPushStatisticService;
    @Autowired
    private AlarmHandleServiceImpl alarmHandleService;
    @GetMapping("/doPush")
        public void doPush(String startTimeStr, String endTimeStr) {
        dataPushStatisticService.doPush( startTimeStr, endTimeStr);
    }

    @GetMapping("/handle")
    public void handle() {
        String topic = "IEMS_02 00 12 00 00 00 00 00 00 00 00 00 00 00 00";
        String message = "{\"frameType\":\"17\",\"deviceType\":\"1312_V1_1\",\"factorySign\":\"0000\",\"address\":\"13\",\"dataType\":\"3\",\"data\":{\"B0\":0,\"B2\":0,\"B4\":\"0000\",\"B6\":0,\"B8\":0,\"B10\":0,\"B12\":0,\"B14\":\"0000\",\"B16\":0,\"B18\":0,\"B20\":0,\"B22\":0,\"B24\":\"0000\",\"B26\":0,\"B28\":0,\"B30\":0,\"B32\":0,\"B34\":\"0000\",\"B36\":0,\"B38\":170,\"B40\":0,\"B42\":0,\"B44\":\"0000\",\"B46\":0,\"B48\":0,\"B50\":0,\"B52\":0,\"B54\":\"0000\",\"B56\":0,\"B58\":0},\"dateTime\":\"2024-12-09 20:50:41\"}";
        alarmHandleService.processMessage(topic,message);
    }

}
