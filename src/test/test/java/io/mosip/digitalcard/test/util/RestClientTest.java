package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.exception.ApisResourceAccessException;
import io.mosip.digitalcard.util.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RestClientTest {

    @InjectMocks
    RestClient restClient;

    @Mock
    Environment environment;

    @Mock
    private RestTemplate restTemplate;

    @Test
    public void postApiTest() throws ApisResourceAccessException {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object();
        Class<String> responseClass = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        String expectedResponse = "response";
        when(restTemplate.postForObject(anyString(), any(), eq(responseClass))).thenReturn(expectedResponse);
        RestClient client = Mockito.spy(restClient);

        String result = restClient.postApi(apiName, pathSegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080")
                .pathSegment("segment1")
                .pathSegment("segment2")
                .queryParam("param1", "value1")
                .queryParam("param2", "value2");
        String expectedUri = builder.toUriString();
        verify(restTemplate).postForObject(eq(expectedUri), any(), eq(responseClass));
    }

    @Test
    public void testPostApi_noHostIpPort() throws ApisResourceAccessException {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        MediaType mediaType = MediaType.APPLICATION_JSON;
        Object requestType = new Object();
        Class<String> responseClass = String.class;

        String result = restClient.postApi(apiName, pathSegments, queryParamName, queryParamValue, mediaType, requestType, responseClass);

        assertNull(result);

        verify(restTemplate, never()).postForObject(anyString(), any(), eq(responseClass));
    }

    @Test
    public void testGetForObject_Success() throws Exception {
        String url = "http://example.com/api/resource";
        String expectedResponse = "Expected response";
        when(restTemplate.getForObject(url, String.class)).thenReturn(expectedResponse);

        String actualResponse = restClient.getForObject(url, String.class);

        assertEquals(expectedResponse, actualResponse);
        verify(restTemplate, times(1)).getForObject(url, String.class);
    }
    @Test
    public void testGetForObject_Exception() {
        String url = "http://example.com/api/resource";
        when(restTemplate.getForObject(url, String.class)).thenThrow(new RuntimeException("Test exception"));

        Exception exception = assertThrows(Exception.class, () -> {
            restClient.getForObject(url, String.class);
        });

        assertEquals("java.lang.RuntimeException: Test exception", exception.getMessage());
        verify(restTemplate, times(1)).getForObject(url, String.class);
    }

    @Test
    public void testGetApi_ApiHostIpPortNull() throws Exception {
        ApiName apiName=ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        Class<?> responseType=String.class;

        when(environment.getProperty(apiName.name())).thenReturn(null);

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNull(result);
        verify(environment, times(1)).getProperty(apiName.name());
        verifyNoInteractions(restTemplate);  // Ensure restTemplate is not called
    }
}
