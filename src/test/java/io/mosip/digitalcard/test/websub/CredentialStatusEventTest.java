package io.mosip.digitalcard.test.websub;

import io.mosip.digitalcard.websub.CredentialStatusEvent;
import io.mosip.digitalcard.websub.StatusEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class CredentialStatusEventTest {

    @Test
    public void shouldSetAndGetAllFields() {
        StatusEvent statusEvent = new StatusEvent();
        statusEvent.setId("id-1");
        statusEvent.setStatus("SUCCESS");

        CredentialStatusEvent event = new CredentialStatusEvent();
        event.setPublisher("MOSIP");
        event.setTopic("DIGITAL_CARD");
        event.setPublishedOn("2025-01-01T10:00:00Z");
        event.setEvent(statusEvent);

        assertEquals("MOSIP", event.getPublisher());
        assertEquals("DIGITAL_CARD", event.getTopic());
        assertEquals("2025-01-01T10:00:00Z", event.getPublishedOn());
        assertEquals(statusEvent, event.getEvent());
    }

    @Test
    public void shouldBeEqualWhenAllFieldsMatch() {
        StatusEvent statusEvent1 = new StatusEvent();
        statusEvent1.setId("1");
        statusEvent1.setStatus("SUCCESS");

        StatusEvent statusEvent2 = new StatusEvent();
        statusEvent2.setId("1");
        statusEvent2.setStatus("SUCCESS");

        CredentialStatusEvent event1 = new CredentialStatusEvent();
        event1.setPublisher("PUB");
        event1.setTopic("TOPIC");
        event1.setPublishedOn("TIME");
        event1.setEvent(statusEvent1);

        CredentialStatusEvent event2 = new CredentialStatusEvent();
        event2.setPublisher("PUB");
        event2.setTopic("TOPIC");
        event2.setPublishedOn("TIME");
        event2.setEvent(statusEvent2);

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    public void shouldNotBeEqualWhenAnyFieldDiffers() {
        CredentialStatusEvent event1 = new CredentialStatusEvent();
        event1.setPublisher("PUB1");

        CredentialStatusEvent event2 = new CredentialStatusEvent();
        event2.setPublisher("PUB2");

        assertNotEquals(event1, event2);
    }

    @Test
    public void shouldReturnMeaningfulToString() {
        CredentialStatusEvent event = new CredentialStatusEvent();
        event.setPublisher("MOSIP");
        event.setTopic("CARD");

        String toString = event.toString();

        assertTrue(toString.contains("publisher=MOSIP"));
        assertTrue(toString.contains("topic=CARD"));
    }

    @Test
    public void shouldAllowNullValues() {
        CredentialStatusEvent event = new CredentialStatusEvent();

        assertNull(event.getPublisher());
        assertNull(event.getTopic());
        assertNull(event.getPublishedOn());
        assertNull(event.getEvent());
    }

}
