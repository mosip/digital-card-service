package io.mosip.digitalcard.test;

import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

public class TestBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestBootApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
