package io.mosip.digitalcard.test.websub;

import io.mosip.digitalcard.websub.StatusEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class StatusEventTest {

    @Test
    public void shouldSetAndGetAllFields() {
        StatusEvent event = new StatusEvent();

        event.setId("id-123");
        event.setRequestId("req-456");
        event.setTimestamp("2025-01-01T10:00:00Z");
        event.setStatus("SUCCESS");
        event.setUrl("https://websub/v1/callback");

        assertEquals("id-123", event.getId());
        assertEquals("req-456", event.getRequestId());
        assertEquals("2025-01-01T10:00:00Z", event.getTimestamp());
        assertEquals("SUCCESS", event.getStatus());
        assertEquals("https://websub/v1/callback", event.getUrl());
    }

    @Test
    public void shouldBeEqualWhenAllFieldsMatch() {
        StatusEvent event1 = new StatusEvent();
        event1.setId("1");
        event1.setRequestId("req1");
        event1.setTimestamp("ts");
        event1.setStatus("SUCCESS");
        event1.setUrl("url");

        StatusEvent event2 = new StatusEvent();
        event2.setId("1");
        event2.setRequestId("req1");
        event2.setTimestamp("ts");
        event2.setStatus("SUCCESS");
        event2.setUrl("url");

        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    public void shouldNotBeEqualWhenAnyFieldDiffers() {
        StatusEvent event1 = new StatusEvent();
        event1.setId("1");

        StatusEvent event2 = new StatusEvent();
        event2.setId("2");

        assertNotEquals(event1, event2);
    }

    @Test
    public void shouldReturnMeaningfulToString() {
        StatusEvent event = new StatusEvent();
        event.setId("id");
        event.setRequestId("req");
        event.setStatus("SUCCESS");

        String toString = event.toString();

        assertTrue(toString.contains("id=id"));
        assertTrue(toString.contains("requestId=req"));
        assertTrue(toString.contains("status=SUCCESS"));
    }

    @Test
    public void shouldAllowNullValues() {
        StatusEvent event = new StatusEvent();

        assertNull(event.getId());
        assertNull(event.getRequestId());
        assertNull(event.getTimestamp());
        assertNull(event.getStatus());
        assertNull(event.getUrl());
    }
}
