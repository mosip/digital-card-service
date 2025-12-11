package io.mosip.digitalcard.test.websub;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import io.mosip.digitalcard.websub.CredentialStatusEvent;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;

@RunWith(MockitoJUnitRunner.class)
public class WebSubSubscriptionHelperTest {

    private WebSubSubscriptionHelper helper;

    @Mock
    private SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> sb;

    @Mock
    private PublisherClient<String, CredentialStatusEvent, HttpHeaders> pb;

    @Before
    public void setUp() throws Exception {
        helper = new WebSubSubscriptionHelper();

        setField("sb", sb);
        setField("pb", pb);
        setField("webSubHubUrl", "https://hub.example");
        setField("webSubSecret", "secret-value");
        setField("webSubPublishUrl", "https://publish.example");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = WebSubSubscriptionHelper.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(helper, value);
    }

    @Test
    public void testInitSubscriptionsSubscribeCalledWithCorrectRequest() {
        String topic = "topic-1";
        String callback = "http://callback.example/path";

        helper.initSubscriptions(topic, callback);

        ArgumentCaptor<SubscriptionChangeRequest> captor = ArgumentCaptor.forClass(SubscriptionChangeRequest.class);
        verify(sb, times(1)).subscribe(captor.capture());
        SubscriptionChangeRequest req = captor.getValue();

        assertNotNull(req);
        assertEquals(callback, req.getCallbackURL());
        assertEquals("https://hub.example", req.getHubURL());
        assertEquals("secret-value", req.getSecret());
        assertEquals(topic, req.getTopic());
    }

    @Test
    public void testInitSubscriptionsSubscribeThrowsExceptionIsHandled() {

        helper.initSubscriptions("topic-2", "http://callback.example/2");

        verify(sb, times(1)).subscribe(any(SubscriptionChangeRequest.class));
    }

    @Test
    public void testDigitalCardStatusUpdateEventPublishCalledWithCorrectArgs() {
        String topic = "publish-topic";
        CredentialStatusEvent event = mock(CredentialStatusEvent.class);

        helper.digitalCardStatusUpdateEvent(topic, event);

        ArgumentCaptor<HttpHeaders> headersCaptor = ArgumentCaptor.forClass(HttpHeaders.class);

        verify(pb, times(1)).publishUpdate(eq(topic), eq(event), eq(MediaType.APPLICATION_JSON_UTF8_VALUE), headersCaptor.capture(), eq("https://hub.example"));

        HttpHeaders headers = headersCaptor.getValue();
        assertNotNull(headers);
    }

    @Test
    public void testDigitalCardStatusUpdateEventPublishThrowsExceptionIsHandled() {

        helper.digitalCardStatusUpdateEvent("publish-topic-2", mock(CredentialStatusEvent.class));

        verify(pb, times(1)).publishUpdate(anyString(), any(CredentialStatusEvent.class), anyString(), any(HttpHeaders.class), anyString());
    }

}
