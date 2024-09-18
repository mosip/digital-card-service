package io.mosip.digitalcard.test.config;

import io.mosip.digitalcard.config.SchedulingConfigurerConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SchedulingConfigurerConfigurationTest {

    @InjectMocks
    private SchedulingConfigurerConfiguration schedulingConfigurerConfiguration;

    @Mock
    private ScheduledTaskRegistrar taskRegistrar;

    @Test
    public void testConfigureTasks() {
        schedulingConfigurerConfiguration.configureTasks(taskRegistrar);

        verify(taskRegistrar, times(1)).setTaskScheduler(any(ThreadPoolTaskScheduler.class));

    }

}
