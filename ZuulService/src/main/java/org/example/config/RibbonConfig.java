package org.example.config;

import com.netflix.loadbalancer.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author 800416
 * @Date 2024/10/16
 */
@Configuration
public class RibbonConfig {
    /**
     * 配置ribbon负载均衡策略
     * @return
     */
    @Bean
    public IRule customRule(){
        //return new RandomRule(); //随机

        return new RoundRobinRule(); //轮询
        //return new BestAvailableRule(); //最佳可用
        //return new RetryRule(); //重试策略
    }
}
