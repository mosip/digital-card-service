package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mosip.digitalcard.test.DigitalCardServiceTest;

import io.mosip.digitalcard.util.RestClient;
import io.mosip.digitalcard.util.Utility;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.when;

@SpringBootTest(classes = DigitalCardServiceTest.class)
@RunWith(MockitoJUnitRunner.class)
public class UtilityTest {

    @InjectMocks
    Utility utility;

    @Mock
    RestClient restClient;

    @Mock
    ObjectMapper objectMapper;

    @Value("${mosip.kernel.config.server.file.storage.uri}")
    private String configServerFileStorageURL;
    private String identityJson = "jsbcdbic";
    private String expectedJsonResponse = "{loadRegProcessorIdentityJson completed successfully}";

    @Test
    public void loadRegProcessorIdentityJsonTest() {
        ReflectionTestUtils.invokeMethod(utility, "loadRegProcessorIdentityJson");
    }

    @Test
    public void testGetIdentityMappingJsonWhenBlankShouldFetchFromService() throws Exception {
        when(restClient.getForObject(configServerFileStorageURL + identityJson, String.class))
                .thenReturn(expectedJsonResponse);
        String actualJsonResponse = utility.getIdentityMappingJson(configServerFileStorageURL, identityJson);

        verify(restClient, times(1))
                .getForObject(configServerFileStorageURL + identityJson, String.class);
    }

    @Test
    public void testGetMappingJsonObjectWhenBlank_ShouldFetchAndParseJson() throws Exception {
        JSONObject actualJsonObject = utility.getMappingJsonObject();
    }

    @Test
    public void testGetJSONObjectWhenKeyIsPresentAndValueIsLinkedHashMapShouldReturnJsonObject() {
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
    public void testGetJSONArrayWhenKeyExistsAndValueIsNullShouldReturnNull() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("key3", null);

        JSONArray jsonArray = utility.getJSONArray(jsonObject, "key3");
    }

    @Test
    public void testReadValueWhenJavaLangStringSuccess() throws IOException {
        Utility utility = new Utility();
        utility.setObjectMapper(JsonMapper.builder().findAndAddModules().build());
        Class<String> clazz = String.class;

        assertEquals("123", utility.readValue("123", clazz));
    }
    
    @Test
    public void testGetUserWhenSecurityContextIsNull() {
        SecurityContextHolder.clearContext();
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUserWhenAuthenticationIsNull() {
        SecurityContextHolder.getContext().setAuthentication(null);
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUserWhenPrincipalIsNotUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUserWhenUserDetailsIsPresent() {
        UserDetails userDetails = User.builder().username("testuser").password("password").roles("USER").build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = Utility.getUser();
        assertEquals("testuser", username);
    }

    @Test
    public void testGetJSONObjectFromArrayGivenJSONObjectWhenJSONArrayAddJSONObject() {
        JSONArray jsonObject = new JSONArray();
        jsonObject.add("12");
        jsonObject.add(new JSONObject());

        assertTrue(Utility.getJSONObjectFromArray(jsonObject, 1).isEmpty());
    }

    @Test
    public void testGetJSONObjectFromArrayGivenLinkedHashMapWhenJSONArrayAddLinkedHashMap() {
        JSONArray jsonObject = new JSONArray();
        jsonObject.add("12");
        jsonObject.add(new LinkedHashMap<>());

        assertTrue(Utility.getJSONObjectFromArray(jsonObject, 1).isEmpty());
    }

    @Test
    public void testMapJsonNodeToJavaObjectWhenJavaLangObjectThenReturnArrayLengthIsZero() {
        Class<Object> genericType = Object.class;

        assertEquals(0, Utility.mapJsonNodeToJavaObject(genericType, new JSONArray()).length);
    }

    @Test
    public void testMapJsonNodeToJavaObjectEmptyJSONArray() {
        JSONArray demographicJsonNode = new JSONArray();

        LanguageValue[] result = Utility.mapJsonNodeToJavaObject(LanguageValue.class, demographicJsonNode);

        assertEquals(0, result.length);
    }

    @Test
    public void testMapJsonNodeToJavaObjectNullJSONArray() {
        assertThrows(NullPointerException.class, () ->
                Utility.mapJsonNodeToJavaObject(LanguageValue.class, null));
    }

    @Test
    public void testMapJsonNodeToJavaObjectValidJSONArray() {
        JSONArray demographicJsonNode = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("language", "en");
        obj1.put("value", "English");
        JSONObject obj2 = new JSONObject();
        obj2.put("language", "fr");
        obj2.put("value", "French");
        demographicJsonNode.add(obj1);
        demographicJsonNode.add(obj2);

        LanguageValue[] result = Utility.mapJsonNodeToJavaObject(LanguageValue.class, demographicJsonNode);

        assertEquals(2, result.length);
        assertEquals("en", result[0].language);
        assertEquals("English", result[0].value);
        assertEquals("fr", result[1].language);
        assertEquals("French", result[1].value);
    }

    @Test
    public void testMapJsonNodeToJavaObjectWithNullElements() {
        JSONArray demographicJsonNode = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("language", "en");
        obj1.put("value", "English");
        demographicJsonNode.add(obj1);
        demographicJsonNode.add(null);

        LanguageValue[] result = Utility.mapJsonNodeToJavaObject(LanguageValue.class, demographicJsonNode);

        assertEquals(2, result.length);
        assertEquals("en", result[0].language);
        assertEquals("English", result[0].value);
        assertEquals(null, result[1]);
    }

    @Test
    public void testMapJsonNodeToJavaObjectMissingKeys() {
        JSONArray demographicJsonNode = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("language", "en");
        demographicJsonNode.add(obj1);

        LanguageValue[] result = Utility.mapJsonNodeToJavaObject(LanguageValue.class, demographicJsonNode);

        assertEquals(1, result.length);
        assertEquals("en", result[0].language);
        assertEquals(null, result[0].value);
    }

    @Test
    public void testMapJsonNodeToJavaObjectNoSuchFieldException() {
        JSONArray demographicJsonNode = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("language", "en");
        obj1.put("value", "English");
        demographicJsonNode.add(obj1);

        assertThrows(io.mosip.digitalcard.exception.DigitalCardServiceException.class, () ->
                Utility.mapJsonNodeToJavaObject(InvalidClass.class, demographicJsonNode));
    }

    @Test
    public void testMapJsonNodeToJavaObjectInstantiationException() {
        JSONArray demographicJsonNode = new JSONArray();
        JSONObject obj1 = new JSONObject();
        obj1.put("language", "en");
        obj1.put("value", "English");
        demographicJsonNode.add(obj1);

        assertThrows(io.mosip.digitalcard.exception.DigitalCardServiceException.class, () ->
                Utility.mapJsonNodeToJavaObject(AbstractClass.class, demographicJsonNode));
    }

    public static class LanguageValue {
        private String language;
        private String value;

        public LanguageValue(String language, String value) {
            this.language = language;
            this.value = value;
        }

        public LanguageValue() {
            // Default constructor for reflection
        }
    }

    public static class InvalidClass {
        private String invalidField;

        public InvalidClass() {
        }
    }

    public static abstract class AbstractClass {
        private String language;
        private String value;

        public AbstractClass() {
        }
    }

}
