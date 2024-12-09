package org.example.service;

import org.example.model.AlarmConfigModel;
import org.springframework.stereotype.Service;

@Service
public interface AlarmTriggerService {

    void cancelAlarmAsync(String redisKey, AlarmConfigModel model);

    void triggerAlarmAsync(String redisKey, AlarmConfigModel model, String message);
}
