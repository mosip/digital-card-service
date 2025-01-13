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

    @Test
    public void testReadValue_whenJavaLangString_Success() throws IOException {
        Utility utility = new Utility();
        utility.setObjectMapper(JsonMapper.builder().findAndAddModules().build());
        Class<String> clazz = String.class;

        assertEquals("123", utility.readValue("123", clazz));
    }
    
    @Test
    public void testGetUser_WhenSecurityContextIsNull() {
        SecurityContextHolder.clearContext();
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUser_WhenAuthenticationIsNull() {
        SecurityContextHolder.getContext().setAuthentication(null);
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUser_WhenPrincipalIsNotUserDetails() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testuser", "password"));
        String username = Utility.getUser();
        assertEquals("", username);
    }

    @Test
    public void testGetUser_WhenUserDetailsIsPresent() {
        UserDetails userDetails = User.builder().username("testuser").password("password").roles("USER").build();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = Utility.getUser();
        assertEquals("testuser", username);
    }

    @Test
    public void testGetJSONObjectFromArray_givenJSONObject_whenJSONArrayAddJSONObject() {
        JSONArray jsonObject = new JSONArray();
        jsonObject.add("12");
        jsonObject.add(new JSONObject());

        assertTrue(Utility.getJSONObjectFromArray(jsonObject, 1).isEmpty());
    }

    @Test
    public void testGetJSONObjectFromArray_givenLinkedHashMap_whenJSONArrayAddLinkedHashMap() {
        JSONArray jsonObject = new JSONArray();
        jsonObject.add("12");
        jsonObject.add(new LinkedHashMap<>());

        assertTrue(Utility.getJSONObjectFromArray(jsonObject, 1).isEmpty());
    }

    @Test
    public void testMapJsonNodeToJavaObject_whenJavaLangObject_thenReturnArrayLengthIsZero() {
        Class<Object> genericType = Object.class;

        assertEquals(0, Utility.mapJsonNodeToJavaObject(genericType, new JSONArray()).length);
    }

    @Test
    public void testMapJsonNodeToJavaObject_EmptyJSONArray() {
        JSONArray demographicJsonNode = new JSONArray();

        LanguageValue[] result = Utility.mapJsonNodeToJavaObject(LanguageValue.class, demographicJsonNode);

        assertEquals(0, result.length);
    }

    @Test
    public void testMapJsonNodeToJavaObject_NullJSONArray() {
        assertThrows(NullPointerException.class, () ->
                Utility.mapJsonNodeToJavaObject(LanguageValue.class, null));
    }

    public static class LanguageValue {
        private String language;
        private String value;

        public LanguageValue(String language, String value) {
            this.language = language;
            this.value = value;
        }
    }

}
