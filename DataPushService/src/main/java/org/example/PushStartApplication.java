package org.example;

//import org.example.service.WebSocketService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // 启用定时任务
public class PushStartApplication
{
    public static void main(String[] args) {
        SpringApplication.run(PushStartApplication.class, args);
    }

//    @Bean
//    public WebSocketService webSocketService() {
//        return new WebSocketService();
//    }
}
