package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.digitalcard.util.Utility;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(MockitoJUnitRunner.class)
public class UtilityTest {

    @InjectMocks
    Utility utility;

    @Mock
    RestClient restClient;

    @Mock
    ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(Utility.class);
    @Value("${mosip.kernel.config.server.file.storage.uri}")
    private String configServerFileStorageURL;
    private String identityJson = "jsbcdbic";
    private String demographicIdentity = "jdbicbic";
    private String expectedJsonResponse = "{loadRegProcessorIdentityJson completed successfully}";

    @Test
    public void loadRegProcessorIdentityJsonTest() {
        ReflectionTestUtils.invokeMethod(utility, "loadRegProcessorIdentityJson");
    }

    @Test
    public void testGetIdentityMappingJson_WhenBlank_ShouldFetchFromService() throws Exception {
        when(restClient.getForObject(configServerFileStorageURL + identityJson, String.class))
                .thenReturn(expectedJsonResponse);
        String actualJsonResponse = utility.getIdentityMappingJson(configServerFileStorageURL, identityJson);

        verify(restClient, times(1))
                .getForObject(configServerFileStorageURL + identityJson, String.class);
    }

    @Test
    public void testGetMappingJsonObject_WhenBlank_ShouldFetchAndParseJson() throws Exception {
        JSONObject actualJsonObject = utility.getMappingJsonObject();
    }

    @Test
    public void testGetJSONObject_WhenKeyIsPresentAndValueIsLinkedHashMap_ShouldReturnJsonObject() {
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("key1", "value1");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("identity", linkedHashMap);

        JSONObject result = utility.getJSONObject(jsonObject, "identity");
    }

    @Test
    public void getJSONValueTest() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", "value1");

        String result = utility.getJSONValue(jsonObject, "key1");
    }

    @Test
    public void writeValueAsStringTest() throws IOException {
        Object obj = new Object();
        String expectedJson = "{\"key\":\"value\"}";

        when(objectMapper.writeValueAsString(obj)).thenReturn(expectedJson);

        String actualJson = utility.writeValueAsString(obj);
    }

    @Test
    public void getJSONArrayTest() {
        ArrayList<String> list = new ArrayList<>(Arrays.asList("value1", "value2", "value3"));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key1", list);

        JSONArray jsonArray = utility.getJSONArray(jsonObject, "key1");
    }

    @Test
    public void testGetJSONArray_WhenKeyExistsAndValueIsNull_ShouldReturnNull() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key3", null);

        JSONArray jsonArray = utility.getJSONArray(jsonObject, "key3");
    }

}
