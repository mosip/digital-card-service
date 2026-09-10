package io.mosip.digitalcard.test.controller;

import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.DataShareResponseDto;
import io.mosip.digitalcard.dto.DigitalCardStatusResponseDto;
import io.mosip.digitalcard.service.DigitalCardService;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.websub.model.Event;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.kernel.core.websub.model.Type;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.AssertionErrors.assertEquals;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(MockitoJUnitRunner.class)
public class DigitalCardControllerTest {
    @InjectMocks
    private DigitalCardController digitalCardController;
    @Mock
    private DigitalCardService digitalCardService;
    @Mock
    private Environment environment;
    @Test
    public void handleIdentityCreateEventTest(){
        EventModel eventModel = getEventModel();
        doNothing().when(digitalCardService).initiateCredentialRequest(anyString(), anyString());
        ResponseEntity<?> responseEntity = digitalCardController.handleIdentityCreateEvent(eventModel);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testHandleIdentityUpdateEvent_Success() {
        EventModel eventModel = getEventModel();
        doNothing().when(digitalCardService).initiateCredentialRequest(anyString(), anyString());
        ResponseEntity<?> responseEntity = digitalCardController.handleIdentityUpdateEvent(eventModel);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    public void testHandleIdentityUpdateEvent_Exception() {
        EventModel eventModel = getEventModel();
        ResponseEntity<?> responseEntity = digitalCardController.handleIdentityUpdateEvent(eventModel);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @NotNull
    private static EventModel getEventModel() {
        EventModel eventModel=new EventModel();
        Event event=new Event();
        event.setId("113534");
        Type type=new Type();
        type.setName("rwctw");
        type.setNamespace("huea");
        Map<String, Object> data = new HashMap<>();
        data.put("registration_id", "test_registration_id");
        data.put("id_hash", "test_id_hash");
        event.setData(data);
        event.setType(type);
        event.setTimestamp("211");
        event.setTransactionId("edfvgghdfghjdfghj");
        event.setDataShareUri("dfgwyyw");
        eventModel.setEvent(event);
        eventModel.setPublisher("ytft");
        eventModel.setPublishedOn("yguygyug");
        eventModel.setTopic("fgwqdtwdqv");
        return eventModel;
    }
    @Test
    public void credentialEventTest(){
        EventModel eventModel = getEventModel();
        ResponseEntity<?> responseEntity = digitalCardController.credentialEvent(eventModel);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    @Test
    public void getDigitalCardTest(){
        String rid = "123456";
        DigitalCardStatusResponseDto digitalCardStatusResponseDto = new DigitalCardStatusResponseDto();
        digitalCardStatusResponseDto.setStatusCode("123");
        digitalCardStatusResponseDto.setUrl("url");
        digitalCardStatusResponseDto.setId("id_121");
        ResponseWrapper<DigitalCardStatusResponseDto> expectedResponse = new DataShareResponseDto();
        expectedResponse.setResponse(digitalCardStatusResponseDto);
        when(digitalCardService.getDigitalCard(rid)).thenReturn(digitalCardStatusResponseDto);
        ResponseWrapper<DigitalCardStatusResponseDto> actualResponse = digitalCardController.getDigitalCard(rid);
        assertEquals(expectedResponse.getResponse().getId(), actualResponse.getResponse().getStatusCode(), expectedResponse.getResponse().getStatusCode());
    }
}