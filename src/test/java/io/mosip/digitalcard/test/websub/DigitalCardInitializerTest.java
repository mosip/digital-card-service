package io.mosip.digitalcard.test.websub;

import io.mosip.digitalcard.websub.DigitalCardInitializer;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DigitalCardInitializerTest {

    @InjectMocks
    private DigitalCardInitializer digitalCardInitializer;

    @Mock
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Test
    public void onApplicationEventWithPositiveDelaySchedulesAtFixedRate() {
        int delaySecs = 10;
        ReflectionTestUtils.setField(digitalCardInitializer, "reSubscriptionDelaySecs", delaySecs);

        digitalCardInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> startCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Duration> periodCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(taskScheduler, times(1)).scheduleAtFixedRate(runnableCaptor.capture(), startCaptor.capture(), periodCaptor.capture());

        assertEquals(Duration.ofSeconds(delaySecs), periodCaptor.getValue());
        assertNotNull(runnableCaptor.getValue());
        assertNotNull(startCaptor.getValue());
    }

    @Test
    public void onApplicationEventWithNonPositiveDelayDoesNotSchedule() {
        ReflectionTestUtils.setField(digitalCardInitializer, "reSubscriptionDelaySecs", 0);

        digitalCardInitializer.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(taskScheduler, never()).scheduleAtFixedRate(any(Runnable.class), any(Instant.class), any(Duration.class));
    }

    @Test
    public void initSubscriptionsSuccessCallsHelperForAllTopics() {
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialTopic", "cred-topic");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityCreateTopic", "create-topic");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityUpdateTopic", "update-topic");
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialCallBackUrl", "cred-callback");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityCreateCallBackUrl", "create-callback");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityUpdateCallBackUrl", "update-callback");

        boolean result = ReflectionTestUtils.invokeMethod(digitalCardInitializer, "initSubsriptions");

        assertTrue(result);
        verify(webSubSubscriptionHelper, times(1)).initSubscriptions(eq("cred-topic"), eq("cred-callback"));
        verify(webSubSubscriptionHelper, times(1)).initSubscriptions(eq("create-topic"), eq("create-callback"));
        verify(webSubSubscriptionHelper, times(1)).initSubscriptions(eq("update-topic"), eq("update-callback"));
    }

    @Test
    public void initSubscriptionsWhenHelperThrowsReturnsFalse() {
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialTopic", "t1");
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialCallBackUrl", "cb1");

        doThrow(new RuntimeException("boom")).when(webSubSubscriptionHelper).initSubscriptions(anyString(), anyString());

        boolean result = ReflectionTestUtils.invokeMethod(digitalCardInitializer, "initSubsriptions");

        assertFalse(result);
        verify(webSubSubscriptionHelper, times(1)).initSubscriptions(eq("t1"), eq("cb1"));
    }

    @Test
    public void retrySubscriptionsRetriesUntilSuccessThenStops() {
        ReflectionTestUtils.setField(digitalCardInitializer, "retryCount", 5);
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialTopic", "t1");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityCreateTopic", "t2");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityUpdateTopic", "t3");
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialCallBackUrl", "c1");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityCreateCallBackUrl", "c2");
        ReflectionTestUtils.setField(digitalCardInitializer, "identityUpdateCallBackUrl", "c3");

        AtomicInteger counter = new AtomicInteger(0);
        doAnswer(invocation -> {
            if (counter.getAndIncrement() < 2) {
                throw new RuntimeException("fail early attempts");
            }
            return null;
        }).when(webSubSubscriptionHelper).initSubscriptions(anyString(), anyString());

        ReflectionTestUtils.invokeMethod(digitalCardInitializer, "retrySubscriptions");

        verify(webSubSubscriptionHelper, times(5)).initSubscriptions(anyString(), anyString());
    }

    @Test
    public void retrySubscriptionsWhenAlwaysFailDoesAtMostRetryCountPlusOneAttempts() {
        ReflectionTestUtils.setField(digitalCardInitializer, "retryCount", 2); // should attempt 3 times total
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialTopic", "t1");
        ReflectionTestUtils.setField(digitalCardInitializer, "credentialCallBackUrl", "c1");

        doThrow(new RuntimeException("always fail")).when(webSubSubscriptionHelper).initSubscriptions(anyString(), anyString());

        ReflectionTestUtils.invokeMethod(digitalCardInitializer, "retrySubscriptions");

        verify(webSubSubscriptionHelper, times(3)).initSubscriptions(anyString(), anyString());
    }
}
