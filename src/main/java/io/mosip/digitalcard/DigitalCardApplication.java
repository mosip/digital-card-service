package io.mosip.digitalcard;

//import io.mosip.vercred.CredentialsVerifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
@ComponentScan(basePackages={ "io.mosip.*" ,"${mosip.auth.adapter.impl.basepackage}"})
@EnableScheduling
public class DigitalCardApplication {

    //@Bean
  //  public CredentialsVerifier credentialsVerifier() {
      //  return new CredentialsVerifier();
    //}
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(5);
        threadPoolTaskScheduler.setThreadNamePrefix("ThreadPoolTaskScheduler");
        return threadPoolTaskScheduler;
    }

    public static void main(String[] args) {
        SpringApplication.run(DigitalCardApplication.class, args);
    }
}
