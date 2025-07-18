package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "web-socket-endpoints")
@Data
public class WebSocketEndpointsConfig {
    private List<String> endpoints;
}