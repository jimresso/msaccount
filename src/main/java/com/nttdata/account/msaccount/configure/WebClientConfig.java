package com.nttdata.account.msaccount.configure;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .baseUrl("http://localhost:8087/creditcards/exists")
                .defaultHeader("Content-Type", "application/json");
    }

}