package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.CredentialRequestDto;
import io.mosip.digitalcard.dto.CredentialResponse;
import io.mosip.digitalcard.dto.CredentialStatusResponse;
import io.mosip.digitalcard.exception.ApisResourceAccessException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.digitalcard.util.CredentialUtil;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.powermock.api.mockito.PowerMockito.when;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(MockitoJUnitRunner.class)
public class CredentialUtilTest {

    @InjectMocks
    CredentialUtil credentialUtil;

    @Mock
    private RestClient restClient;

    String requestId="sampleRequestId";

    @Test
    public void reqCredentialTest() throws ApisResourceAccessException {
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
    public void getStatusTest() {
        List<String> pathSegments = new ArrayList<>();
        pathSegments.add(requestId);

        ResponseWrapper<CredentialStatusResponse> responseWrapper = new ResponseWrapper<>();
        CredentialStatusResponse expectedResponse = new CredentialStatusResponse();
        responseWrapper.setResponse(expectedResponse);

        assertThrows(DigitalCardServiceException.class, () -> {
            credentialUtil.getStatus(requestId);
        });
    }

}
