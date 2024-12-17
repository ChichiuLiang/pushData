 package org.example.listener;

import org.example.utils.RedisMessageUtils;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class ToDBMessageListener implements MessageListener {
    private final RedisMessageUtils utils;

    public ToDBMessageListener(RedisMessageUtils utils) {
        this.utils = utils;
    }



    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel());
        String context = new String(message.getBody());
        utils.inputRedisMsg(topic, context);
        // 可以添加更多特定于 ToDB_* 的处理逻辑
    }
}