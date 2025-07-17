package org.example.schedule;

import org.example.cache.BarCodeCache;
import org.example.listener.IEMSMessageListener;
import org.example.listener.ToDBMessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BarCodeRefreshTask {

    private final BarCodeCache barCodeCache;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final ToDBMessageListener toDBMessageListener;
    private final IEMSMessageListener iemsMessageListener;
    // 缓存上一次的 bar_code 列表
    private Set<String> lastBarCodes = new HashSet<>();
    // 手动记录已注册的监听器和其对应的 Topic
    private final Map<MessageListenerAdapter, Topic> registeredListeners = new ConcurrentHashMap<>();

    public BarCodeRefreshTask(BarCodeCache barCodeCache,
                              RedisMessageListenerContainer redisMessageListenerContainer,
                              ToDBMessageListener toDBMessageListener,
                              IEMSMessageListener iemsMessageListener) {
        this.barCodeCache = barCodeCache;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
        this.toDBMessageListener = toDBMessageListener;
        this.iemsMessageListener = iemsMessageListener;
    }

    @Scheduled(fixedRate = 300000) // 每5分钟执行一次
    public void refreshBarCodes() {
        updateRedisSubscriptions();
    }

    private void updateRedisSubscriptions() {
        Set<String> currentBarCodes = new HashSet<>(barCodeCache.getBarCodes());
        // 如果和上次一样，直接返回，不更新监听器
        if (currentBarCodes.equals(lastBarCodes)) {
            return;
        }

        // 否则继续更新监听器
        redisMessageListenerContainer.stop(() -> {
            // 移除所有已记录的监听器
            registeredListeners.forEach((listener, topic) ->
                    redisMessageListenerContainer.removeMessageListener(listener, topic)
            );
            registeredListeners.clear();

            // 根据缓存中的条码重新订阅
            if (currentBarCodes.isEmpty()) {
                PatternTopic toDBPattern = new PatternTopic("ToDB_*");
                PatternTopic iemsPattern = new PatternTopic("IEMS_*");

                redisMessageListenerContainer.addMessageListener(toDBMessageListener, toDBPattern);
                redisMessageListenerContainer.addMessageListener(iemsMessageListener, iemsPattern);

                registeredListeners.put(new MessageListenerAdapter(toDBMessageListener), toDBPattern);
                registeredListeners.put(new MessageListenerAdapter(iemsMessageListener), iemsPattern);
            } else {
                for (String barCode : currentBarCodes) {
                    ChannelTopic toDBTopic = new ChannelTopic("ToDB_" + barCode);
                    ChannelTopic iemsTopic = new ChannelTopic("IEMS_" + barCode);

                    redisMessageListenerContainer.addMessageListener(toDBMessageListener, toDBTopic);
                    redisMessageListenerContainer.addMessageListener(iemsMessageListener, iemsTopic);

                    registeredListeners.put(new MessageListenerAdapter(toDBMessageListener), toDBTopic);
                    registeredListeners.put(new MessageListenerAdapter(iemsMessageListener), iemsTopic);
                }
            }

            // 更新 lastBarCodes
            lastBarCodes.clear();
            lastBarCodes.addAll(currentBarCodes);

            redisMessageListenerContainer.start(); // 重启容器
        });
    }

}
