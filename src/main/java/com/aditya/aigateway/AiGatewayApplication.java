package com.aditya.aigateway;

import com.aditya.aigateway.config.ModelRoutingConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ModelRoutingConfig.class)
public class AiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
    }
}