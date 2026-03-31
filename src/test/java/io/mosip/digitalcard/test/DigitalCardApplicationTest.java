package io.mosip.digitalcard.test;

import io.mosip.digitalcard.DigitalCardApplication;
import io.mosip.vercred.CredentialsVerifier;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DigitalCardApplicationTest {

    @Test
    public void credentialsVerifierBeanShouldBeCreated() {
        DigitalCardApplication application = new DigitalCardApplication();

        CredentialsVerifier verifier = application.credentialsVerifier();

        assertNotNull(verifier);
    }

    @Test
    public void taskSchedulerBeanShouldUseConfiguredPoolAndPrefix() {
        DigitalCardApplication application = new DigitalCardApplication();

        ThreadPoolTaskScheduler scheduler = application.taskScheduler();

        assertEquals(5, scheduler.getPoolSize());
        assertEquals("ThreadPoolTaskScheduler", scheduler.getThreadNamePrefix());
    }
}
