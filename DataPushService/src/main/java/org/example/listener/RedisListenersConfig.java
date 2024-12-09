//package org.example.listener;
//
//import org.example.service.impl.AlarmHandleServiceImpl;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.Message;
//import org.springframework.data.redis.connection.MessageListener;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.listener.PatternTopic;
//import org.springframework.data.redis.listener.RedisMessageListenerContainer;
//
///**
// * @author lee
// * @date 2024/12/8
// */
//@Configuration
//public class RedisListenersConfig {
//
//    private final RedisMessageUtils redisMessageUtils;
//
//    public RedisListenersConfig(RedisMessageUtils redisMessageUtils) {
//        this.redisMessageUtils = redisMessageUtils;
//    }
//
//    @Autowired
//    private AlarmHandleServiceImpl handleService;
//
//    // 定义一个监听器用于监听 ToDB_* 频道
//    public static class ToDBMessageListener implements MessageListener {
//        private final RedisMessageUtils utils;
//
//        public ToDBMessageListener(RedisMessageUtils utils) {
//            this.utils = utils;
//        }
//
//        @Override
//        public void onMessage(Message message, byte[] pattern) {
//            String topic = new String(message.getChannel());
//            String context = new String(message.getBody());
//            utils.inputRedisMsg(topic, context);
//            // 可以添加更多特定于 ToDB_* 的处理逻辑
//        }
//    }
//
//    // 定义另一个监听器用于监听 IEMS_* 频道
//    public static class IEMSMessageListener implements MessageListener {
//        private final RedisMessageUtils utils;
//
//        public IEMSMessageListener(RedisMessageUtils utils) {
//            this.utils = utils;
//        }
//
//        @Override
//        public void onMessage(Message message, byte[] pattern) {
//            String topic = new String(message.getChannel());
//            String context = new String(message.getBody());
//            utils.inputRedisMsg(topic, context);
//            // 可以添加更多特定于 IEMS_* 的处理逻辑
//            handleService.processMessage(message);
//        }
//    }
//
//    // 创建并配置监听器及其对应的频道
//    @Bean
//    public RedisMessageListenerContainer redisMessageListenerContainer(
//            RedisConnectionFactory connectionFactory,
//            ToDBMessageListener toDBMessageListener,
//            IEMSMessageListener iemsMessageListener) {
//
//        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
//        container.setConnectionFactory(connectionFactory);
//
//        // 使用各自的监听器订阅对应的频道
//        container.addMessageListener(toDBMessageListener, new PatternTopic("ToDB_*"));
//        container.addMessageListener(iemsMessageListener, new PatternTopic("IEMS_*"));
//
//        return container;
//    }
//
//    // 将监听器定义为单独的bean，让Spring负责创建这些监听器的实例，并自动注入必要的依赖项
//    @Bean
//    public ToDBMessageListener toDBMessageListener() {
//        return new ToDBMessageListener(redisMessageUtils);
//    }
//
//    @Bean
//    public IEMSMessageListener iemsMessageListener() {
//        return new IEMSMessageListener(redisMessageUtils);
//    }
//}