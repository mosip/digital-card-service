package io.mosip.digitalcard.test.websub;

import io.mosip.digitalcard.websub.SubscribeEvent;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SubscribeEventTest {

    @InjectMocks
    private SubscribeEvent subscribeEvent;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @Mock
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    @Test
    public void onApplicationEventSchedulesWithConfiguredDelay() {
        int delayMs = 1000;
        ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", delayMs);

        long start = System.currentTimeMillis();
        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);

        verify(taskScheduler, times(1)).schedule(runnableCaptor.capture(), dateCaptor.capture());

        assertNotNull("Scheduled runnable should not be null", runnableCaptor.getValue());
        assertNotNull("Scheduled date should not be null", dateCaptor.getValue());

        long scheduledAt = dateCaptor.getValue().getTime();
        long diff = scheduledAt - start;

        assertTrue("Scheduled delay should be close to configured value", diff >= delayMs - 150 && diff <= delayMs + 2500);
    }

    @Test
    public void scheduledRunnableExecutesAllSubscriptionsInOrder() {
        ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", 10);
        ReflectionTestUtils.setField(subscribeEvent, "credentialTopic", "cred-topic");
        ReflectionTestUtils.setField(subscribeEvent, "identityCreateTopic", "create-topic");
        ReflectionTestUtils.setField(subscribeEvent, "identityUpdateTopic", "update-topic");
        ReflectionTestUtils.setField(subscribeEvent, "credentialCallBackUrl", "cred-callback");
        ReflectionTestUtils.setField(subscribeEvent, "identityCreateCallBackUrl", "create-callback");
        ReflectionTestUtils.setField(subscribeEvent, "identityUpdateCallBackUrl", "update-callback");

        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), dateCaptor.capture());

        Runnable scheduled = runnableCaptor.getValue();
        assertNotNull(scheduled);

        scheduled.run();

        InOrder inOrder = inOrder(webSubSubscriptionHelper);
        inOrder.verify(webSubSubscriptionHelper).initSubscriptions("cred-topic", "cred-callback");
        inOrder.verify(webSubSubscriptionHelper).initSubscriptions("create-topic", "create-callback");
        inOrder.verify(webSubSubscriptionHelper).initSubscriptions("update-topic", "update-callback");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void scheduledRunnable_stopsOnExceptionDoesNotCallRemainingSubscriptions() {
        ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", 10);
        ReflectionTestUtils.setField(subscribeEvent, "credentialTopic", "cred-topic");
        ReflectionTestUtils.setField(subscribeEvent, "identityCreateTopic", "create-topic");
        ReflectionTestUtils.setField(subscribeEvent, "identityUpdateTopic", "update-topic");
        ReflectionTestUtils.setField(subscribeEvent, "credentialCallBackUrl", "cred-callback");
        ReflectionTestUtils.setField(subscribeEvent, "identityCreateCallBackUrl", "create-callback");
        ReflectionTestUtils.setField(subscribeEvent, "identityUpdateCallBackUrl", "update-callback");

        doThrow(new RuntimeException("fail first"))
                .when(webSubSubscriptionHelper).initSubscriptions("cred-topic", "cred-callback");

        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Date.class));

        Runnable scheduled = runnableCaptor.getValue();
        assertNotNull(scheduled);
        try {
            scheduled.run();
            fail("Expected exception from first subscription to propagate");
        } catch (RuntimeException ex) {
            assertEquals("fail first", ex.getMessage());
        }

        verify(webSubSubscriptionHelper, times(1)).initSubscriptions("cred-topic", "cred-callback");
        verify(webSubSubscriptionHelper, never()).initSubscriptions("create-topic", "create-callback");
        verify(webSubSubscriptionHelper, never()).initSubscriptions("update-topic", "update-callback");
    }

    @Test
    public void onApplicationEventWithNegativeDelayStillSchedulesRunnable() {
        int delayMs = -1000;
        ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", delayMs);

        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));

        ArgumentCaptor<Date> dateCaptor = ArgumentCaptor.forClass(Date.class);
        verify(taskScheduler).schedule(any(Runnable.class), dateCaptor.capture());

        assertTrue("Scheduled date should be in the past or near-present for negative delay",
                dateCaptor.getValue().getTime() <= System.currentTimeMillis());
    }

    @Test
    public void onApplicationEventCalledMultipleTimesSchedulesMultipleTasks() {
        ReflectionTestUtils.setField(subscribeEvent, "taskSubsctiptionDelay", 1);

        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));
        subscribeEvent.onApplicationEvent(mock(ApplicationReadyEvent.class));

        verify(taskScheduler, times(2)).schedule(any(Runnable.class), any(Date.class));
    }
}
