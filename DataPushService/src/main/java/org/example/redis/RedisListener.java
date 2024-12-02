package org.example.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import javax.annotation.Resource;

/**
 * @author lee
 * @date 2023/11/10
 */
@Configuration
public class RedisListener implements MessageListener {
    @Resource
    private RedisMessageUtils redisMessageUtils;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        //获取订阅内容
        String topic = new String(message.getChannel());
        String context = new String(message.getBody());
        redisMessageUtils.inputRedisMsg(topic,context);
    }


    //重写此方法，订阅要监听的topic,方法中的RedisListener是当前类
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,RedisListener redisListener){
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(factory);
        //订阅topic
        redisMessageListenerContainer.addMessageListener(redisListener,new PatternTopic("ToDB_*"));   // 数据库入库 频道
        redisMessageListenerContainer.addMessageListener(redisListener, new PatternTopic("IEMS_*"));  // 订阅 IEMS_* 频道
        return redisMessageListenerContainer;
    }

}
