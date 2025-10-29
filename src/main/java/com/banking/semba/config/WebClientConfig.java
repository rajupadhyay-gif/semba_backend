package com.banking.semba.config;

import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.context.annotation.Configuration;


@Configuration
public class WebClientConfig {

    @Bean
    public WebClient bankWebClient() {
        return WebClient.builder()
                .baseUrl("https://jsonplaceholder.typicode.com") // Replace with real Bank base URL
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
