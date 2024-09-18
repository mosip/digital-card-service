package io.mosip.digitalcard.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })

@EnableAsync
@EnableCaching
public class TestBootApplication {

public static void main(String... args) {
    SpringApplication.run(TestBootApplication.class, args);
}
}
