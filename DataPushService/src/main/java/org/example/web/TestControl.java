package org.example.web;

import org.example.schedule.DataPushStatisticScheduledTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestControl {
    @Autowired
    private DataPushStatisticScheduledTask dataPushStatisticService;
    @GetMapping("/doPush")
    public void doPush(String startTimeStr, String endTimeStr) {
        dataPushStatisticService.doPush( startTimeStr, endTimeStr);
    }

}
