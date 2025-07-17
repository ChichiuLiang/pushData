package org.example.config;

import org.example.cache.BarCodeCache;
import org.example.listener.IEMSMessageListener;
import org.example.utils.RedisMessageUtils;
import org.example.listener.ToDBMessageListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

    private final RedisMessageUtils redisMessageUtils;
    private final BarCodeCache barCodeCache ;

    public RedisConfig(RedisMessageUtils redisMessageUtils, BarCodeCache barCodeCache) {
        this.redisMessageUtils = redisMessageUtils;
        this.barCodeCache = barCodeCache;
    }

    /**
     * RedisTemplate Settings 反序列化配置
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用 String 序列化器来确保字符串不会被加上双引号
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    // 创建并配置监听器及其对应的频道
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            ToDBMessageListener toDBMessageListener,
            IEMSMessageListener iemsMessageListener) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 使用各自的监听器订阅对应的频道
        if(barCodeCache.getBarCodes().isEmpty()){
            container.addMessageListener(toDBMessageListener, new PatternTopic("ToDB_*"));
            container.addMessageListener(iemsMessageListener, new PatternTopic("IEMS_*"));
        }else{
            for (String barCode : barCodeCache.getBarCodes()) {
                container.addMessageListener(toDBMessageListener, new ChannelTopic("ToDB_" + barCode));
                container.addMessageListener(iemsMessageListener, new ChannelTopic("IEMS_" + barCode));
            }
        }
        return container;
    }

    // 将监听器定义为单独的bean，让Spring负责创建这些监听器的实例，并自动注入必要的依赖项
    @Bean
    public ToDBMessageListener toDBMessageListener() {
        return new ToDBMessageListener(redisMessageUtils);
    }

    @Bean
    public IEMSMessageListener iemsMessageListener() {
        return new IEMSMessageListener(redisMessageUtils);
    }
}