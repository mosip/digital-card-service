package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.CredentialRequestDto;
import io.mosip.digitalcard.dto.CredentialResponse;
import io.mosip.digitalcard.dto.CredentialStatusResponse;
import io.mosip.digitalcard.exception.ApisResourceAccessException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.test.DigitalCardServiceTest;
import io.mosip.digitalcard.util.CredentialUtil;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.digitalcard.util.Utility;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest(classes = DigitalCardServiceTest.class)
@RunWith(MockitoJUnitRunner.class)
public class CredentialUtilTest {

    @InjectMocks
    CredentialUtil credentialUtil;

    @Mock
    private Utility utility;

    @Mock
    private RestClient restClient;

    String requestId="sampleRequestId";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void reqCredentialTest_Success() throws ApisResourceAccessException {
        RequestWrapper<CredentialRequestDto> requestDto = new RequestWrapper<>();
        CredentialRequestDto credentialRequestDto = new CredentialRequestDto();
        requestDto.setRequest(credentialRequestDto);
        requestDto.setRequesttime(LocalDateTime.now());

        ResponseWrapper<CredentialResponse> responseDto = new ResponseWrapper<>();
        CredentialResponse expectedResponse = new CredentialResponse();
        responseDto.setResponse(expectedResponse);

        lenient().when(restClient.postApi(any(ApiName.class), any(), any(), any(), any(), any(RequestWrapper.class), any(Class.class)))
                .thenReturn(responseDto);

    }
    @Test
    public void getStatusTest_Success() {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(requestId);

        ResponseWrapper<CredentialStatusResponse> responseWrapper = new ResponseWrapper<>();
        CredentialStatusResponse expectedResponse = new CredentialStatusResponse();
        responseWrapper.setResponse(expectedResponse);

        assertThrows(DigitalCardServiceException.class, () -> {
            credentialUtil.getStatus(requestId);
        });
    }

    @Test
    public void testReqCredential_Success() throws Exception {
        CredentialRequestDto requestDto = new CredentialRequestDto();
        CredentialResponse expectedResponse = new CredentialResponse();

        ResponseWrapper<CredentialResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(expectedResponse);

        when(restClient.postApi(eq(ApiName.CREDENTIAL_REQ_URL), isNull(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(ResponseWrapper.class)))
                .thenReturn(responseWrapper);
        when(utility.writeValueAsString(any())).thenReturn("mockedJson");
        when(utility.readValue(eq("mockedJson"), eq(CredentialResponse.class))).thenReturn(expectedResponse);

        CredentialResponse actualResponse = credentialUtil.reqCredential(requestDto);

        assertEquals(expectedResponse, actualResponse);
        verify(restClient, times(1)).postApi(any(), any(), any(), any(), any(), any(), any());
        verify(utility, times(1)).writeValueAsString(any());
        verify(utility, times(1)).readValue(any(), eq(CredentialResponse.class));
    }

    @Test
    public void testReqCredential_ApisResourceAccessException() throws Exception {
        CredentialRequestDto requestDto = new CredentialRequestDto();

        when(restClient.postApi(eq(ApiName.CREDENTIAL_REQ_URL), isNull(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(ResponseWrapper.class)))
                .thenThrow(new ApisResourceAccessException("API Access Error"));

        DigitalCardServiceException exception = assertThrows(DigitalCardServiceException.class, () -> credentialUtil.reqCredential(requestDto));
        assertTrue(exception.getCause() instanceof ApisResourceAccessException);
        assertEquals("API Access Error", exception.getCause().getMessage());
    }

    @Test
    public void testReqCredential_IOException() throws Exception {
        CredentialRequestDto requestDto = new CredentialRequestDto();
        ResponseWrapper<CredentialResponse> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(new CredentialResponse());

        when(restClient.postApi(eq(ApiName.CREDENTIAL_REQ_URL), isNull(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(ResponseWrapper.class)))
                .thenReturn(responseWrapper);
        when(utility.writeValueAsString(any())).thenThrow(new IOException("IO Error"));

        DigitalCardServiceException exception = assertThrows(DigitalCardServiceException.class, () -> credentialUtil.reqCredential(requestDto));
        assertTrue(exception.getCause() instanceof IOException);
        assertEquals("IO Error", exception.getCause().getMessage());
    }

}
