package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.exception.ApisResourceAccessException;
import io.mosip.digitalcard.util.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@RunWith(MockitoJUnitRunner.class)
public class RestClientTest {

    @InjectMocks
    RestClient restClient;

    @Mock
    Environment environment;

    @Mock
    private RestTemplate restTemplate;

    @Test
    public void postApiTestSuccess() throws ApisResourceAccessException {
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
    public void testPostApiNoHostIpPort() throws ApisResourceAccessException {
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
    public void testGetForObjectSuccess() throws Exception {
        String url = "http://example.com/api/resource";
        String expectedResponse = "Expected response";
        when(restTemplate.getForObject(url, String.class)).thenReturn(expectedResponse);

        String actualResponse = restClient.getForObject(url, String.class);

        assertEquals(expectedResponse, actualResponse);
        verify(restTemplate, times(1)).getForObject(url, String.class);
    }
    @Test
    public void testGetForObjectException() {
        String url = "http://example.com/api/resource";
        when(restTemplate.getForObject(url, String.class)).thenThrow(new RuntimeException("Test exception"));

        Exception exception = assertThrows(Exception.class, () -> {
            restClient.getForObject(url, String.class);
        });

        assertEquals("java.lang.RuntimeException: Test exception", exception.getMessage());
        verify(restTemplate, times(1)).getForObject(url, String.class);
    }

    @Test
    public void testGetApiApiHostIpPortNull() throws Exception {
        ApiName apiName=ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        Class<?> responseType=String.class;

        when(environment.getProperty(apiName.name())).thenReturn(null);

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNull(result);
        verify(environment, times(1)).getProperty(apiName.name());
        verifyNoInteractions(restTemplate);
    }

    @Test
    public void testGetApiSuccessWithPathSegmentsAndQueryParams() throws Exception {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        Class<String> responseType = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        String expectedResponse = "response";
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(responseType))).thenReturn(new org.springframework.http.ResponseEntity<>(expectedResponse, org.springframework.http.HttpStatus.OK));

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080")
                .pathSegment("segment1")
                .pathSegment("segment2")
                .queryParam("param1", "value1")
                .queryParam("param2", "value2");
        String expectedUri = builder.build(false).encode().toUriString();
        verify(restTemplate).exchange(eq(java.net.URI.create(expectedUri)), eq(HttpMethod.GET), any(), eq(responseType));
    }

    @Test
    public void testGetApiSuccessWithoutPathSegments() throws Exception {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = null;
        String queryParamName = "param1,param2";
        String queryParamValue = "value1,value2";
        Class<String> responseType = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        String expectedResponse = "response";
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(responseType))).thenReturn(new org.springframework.http.ResponseEntity<>(expectedResponse, org.springframework.http.HttpStatus.OK));

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080")
                .queryParam("param1", "value1")
                .queryParam("param2", "value2");
        String expectedUri = builder.build(false).encode().toUriString();
        verify(restTemplate).exchange(eq(java.net.URI.create(expectedUri)), eq(HttpMethod.GET), any(), eq(responseType));
    }

    @Test
    public void testGetApiSuccessWithoutQueryParams() throws Exception {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1", "segment2");
        String queryParamName = null;
        String queryParamValue = "value1,value2";
        Class<String> responseType = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        String expectedResponse = "response";
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(responseType))).thenReturn(new org.springframework.http.ResponseEntity<>(expectedResponse, org.springframework.http.HttpStatus.OK));

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080")
                .pathSegment("segment1")
                .pathSegment("segment2");
        String expectedUri = builder.build(false).encode().toUriString();
        verify(restTemplate).exchange(eq(java.net.URI.create(expectedUri)), eq(HttpMethod.GET), any(), eq(responseType));
    }

    @Test
    public void testGetApiSuccessWithEmptyPathSegments() throws Exception {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("", null, "segment1");
        String queryParamName = "param1";
        String queryParamValue = "value1";
        Class<String> responseType = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        String expectedResponse = "response";
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(responseType))).thenReturn(new org.springframework.http.ResponseEntity<>(expectedResponse, org.springframework.http.HttpStatus.OK));

        String result = restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);

        assertNotNull(result);
        assertEquals(expectedResponse, result);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString("http://localhost:8080")
                .pathSegment("segment1")
                .queryParam("param1", "value1");
        String expectedUri = builder.build(false).encode().toUriString();
        verify(restTemplate).exchange(eq(java.net.URI.create(expectedUri)), eq(HttpMethod.GET), any(), eq(responseType));
    }

    @Test
    public void testGetApiException() {
        ApiName apiName = ApiName.CREDENTIAL_STATUS_URL;
        List<String> pathSegments = Arrays.asList("segment1");
        String queryParamName = "param1";
        String queryParamValue = "value1";
        Class<String> responseType = String.class;

        when(environment.getProperty(apiName.name())).thenReturn("http://localhost:8080");
        when(restTemplate.exchange(any(), eq(HttpMethod.GET), any(), eq(responseType))).thenThrow(new RuntimeException("Test exception"));

        Exception exception = assertThrows(Exception.class, () -> {
            restClient.getApi(apiName, pathSegments, queryParamName, queryParamValue, responseType);
        });

        assertEquals("java.lang.RuntimeException: Test exception", exception.getMessage());
        verify(restTemplate).exchange(any(), eq(HttpMethod.GET), any(), eq(responseType));
    }

    private Object invokeSetRequestHeader(RestClient client, Object requestType, MediaType mediaType) throws Exception {
        Method m = RestClient.class.getDeclaredMethod("setRequestHeader", Object.class, MediaType.class);
        m.setAccessible(true);
        return m.invoke(client, requestType, mediaType);
    }

    @Test
    public void testRequestTypeNullWithMediaType() throws Exception {
        RestClient client = new RestClient();

        Object result = invokeSetRequestHeader(client, null, MediaType.APPLICATION_JSON);

        assertNotNull(result);
        assertTrue(result instanceof HttpEntity);
        HttpEntity<?> entity = (HttpEntity<?>) result;

        assertNull(entity.getHeaders().getFirst("X-Test"));
    }

    @Test
    public void testRequestTypeIsHttpEntityNoMediaType() throws Exception {
        RestClient client = new RestClient();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "value1");
        HttpEntity<String> requestEntity = new HttpEntity<>("myBody", headers);

        Object result = invokeSetRequestHeader(client, requestEntity, null);

        assertNotNull(result);

        HttpEntity<?> entity = (HttpEntity<?>) result;
        assertEquals("myBody", entity.getBody());
        assertEquals("value1", entity.getHeaders().getFirst("X-Test"));
        assertNull(entity.getHeaders().getFirst("Content-Type"));
    }

    @Test
    public void testRequestTypeIsHttpEntityWithMediaTypeContentTypeOverride() throws Exception {
        RestClient client = new RestClient();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_XML.toString());
        headers.add("X-Test", "value2");
        HttpEntity<String> requestEntity = new HttpEntity<>("body2", headers);

        Object result = invokeSetRequestHeader(client, requestEntity, MediaType.APPLICATION_JSON);

        assertNotNull(result);

        HttpEntity<?> entity = (HttpEntity<?>) result;
        assertEquals("body2", entity.getBody());
        assertEquals(MediaType.APPLICATION_JSON.toString(), entity.getHeaders().getFirst("Content-Type"));
        assertEquals("value2", entity.getHeaders().getFirst("X-Test"));
    }

    @Test
    public void testRequestTypeNotHttpEntityClassCastPath() throws Exception {
        RestClient client = new RestClient();

        String req = "plainStringBody";

        Object result = invokeSetRequestHeader(client, req, MediaType.APPLICATION_JSON);

        assertNotNull(result);

        HttpEntity<?> entity = (HttpEntity<?>) result;
        assertEquals(req, entity.getBody());
        assertEquals(MediaType.APPLICATION_JSON.toString(), entity.getHeaders().getFirst("Content-Type"));
    }

}
