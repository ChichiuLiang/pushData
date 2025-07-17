package org.example.listener;

import org.example.service.impl.AlarmHandleServiceImpl;
import org.example.utils.RedisMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
public class IEMSMessageListener implements MessageListener {
    private final RedisMessageUtils utils;
    @Autowired
    private AlarmHandleServiceImpl handleService;

    public IEMSMessageListener(RedisMessageUtils utils) {
        this.utils = utils;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel());
        String context = new String(message.getBody());
        utils.inputRedisMsg(topic, context);
        // 可以添加更多特定于 IEMS_* 的处理逻辑
        //handleService.processMessage(topic,context);
    }
}
