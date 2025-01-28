package io.mosip.digitalcard.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"io.mosip.digitalcard"})
public class DigitalCardServiceTest {

    public static void main(String[] args) {
        SpringApplication.run(DigitalCardServiceTest.class, args);
    }

}
