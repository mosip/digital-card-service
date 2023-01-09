package io.mosip.digitalcard.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.kernel.core.util.StringUtils;
import lombok.Data;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Component
@Data
public class Utility {


    private static final Logger logger = LoggerFactory.getLogger(Utility.class);

    @Value("${mosip.kernel.config.server.file.storage.uri}")
    private String configServerFileStorageURL;


    private static final String LANGUAGE = "language";

    @Autowired
    private RestClient restClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Environment env;

    private static final String IDENTITY = "identity";
    private static final String VALUE = "value";

    private static String regProcessorIdentityJson = "";


    /** The get reg processor identity json. */
    @Value("${mosip.digitalcard.identityjson}")
    private String identityJson;

    /** The get reg processor demographic identity. */
    @Value("${mosip.digitalcard.demographic.identity}")
    private String demographicIdentity;

    private String mappingJsonString = null;

    private String uinCardTemplateData = null;


    @PostConstruct
    private void loadRegProcessorIdentityJson() throws Exception {
        regProcessorIdentityJson = restClient.getForObject(configServerFileStorageURL + identityJson, String.class);
        logger.info("loadRegProcessorIdentityJson completed successfully");
    }

    public String getIdentityMappingJson(String configServerFileStorageURL, String identityJson) throws Exception {
        if (StringUtils.isBlank(regProcessorIdentityJson)) {
            regProcessorIdentityJson=restClient.getForObject(configServerFileStorageURL + identityJson, String.class);
        }
        return regProcessorIdentityJson;
    }

    public JSONObject getMappingJsonObject() throws Exception {
        if (StringUtils.isBlank(regProcessorIdentityJson)) {
            regProcessorIdentityJson=restClient.getForObject(configServerFileStorageURL + identityJson, String.class);
        }

        return getJSONObject(objectMapper.readValue(regProcessorIdentityJson,JSONObject.class),IDENTITY);
    }

    public String getUinCardTemplate(String cardTemplate) throws Exception {
        return restClient.getForObject(configServerFileStorageURL + cardTemplate, String.class);
    }

    @SuppressWarnings("unchecked")
    public JSONObject getJSONObject(JSONObject jsonObject, Object key)  {
        if(jsonObject == null)
            return null;
        LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
        return identity != null ? new JSONObject(identity) : null;
    }
    @SuppressWarnings("unchecked")
    public <T> T getJSONValue(JSONObject jsonObject, Object key)  {
        T value = (T) jsonObject.get(key);
        return value;
    }
    /**
     * Object mapper read value. This method maps the jsonString to particular type
     *
     * @param            <T> the generic type
     * @param jsonString the json string
     * @param clazz      the clazz
     * @return the t
     * @throws JsonParseException   the json parse exception
     * @throws JsonMappingException the json mapping exception
     * @throws IOException          Signals that an I/O exception has occurred.
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(String jsonString, Class<?> clazz) throws IOException {
        return (T) objectMapper.readValue(jsonString, clazz);
    }

    public String writeValueAsString(Object obj) throws IOException {
        return objectMapper.writeValueAsString(obj);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public JSONArray getJSONArray(JSONObject jsonObject, Object key) {
        ArrayList value = (ArrayList) jsonObject.get(key);
        if (value == null)
            return null;
        JSONArray jsonArray = new JSONArray();
        jsonArray.addAll(value);

        return jsonArray;

    }

    public static String getUser() {
        if (Objects.nonNull(SecurityContextHolder.getContext())
                && Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())
                && Objects.nonNull(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails) {
            return ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .getUsername();
        } else {
            return "";
        }
    }

    /**
     * Iterates the JSONArray and returns JSONObject for given index.
     *
     * @param jsonObject
     *            the json object
     * @param key
     *            the key
     * @return the JSON object
     */
    public static JSONObject getJSONObjectFromArray(JSONArray jsonObject, int key) {
        Object object = jsonObject.get(key);
        if(object instanceof LinkedHashMap) {
            LinkedHashMap identity = (LinkedHashMap) jsonObject.get(key);
            return identity != null ? new JSONObject(identity) : null;
        }else {
            return (JSONObject)object;
        }
    }
    public static <T> T[] mapJsonNodeToJavaObject(Class<? extends Object> genericType, JSONArray demographicJsonNode) {
        String language;
        String value;
        T[] javaObject = (T[]) Array.newInstance(genericType, demographicJsonNode.size());
        try {
            for (int i = 0; i < demographicJsonNode.size(); i++) {

                T jsonNodeElement = (T) genericType.newInstance();

                JSONObject objects = Utility.getJSONObjectFromArray(demographicJsonNode, i);
                if (objects != null) {
                    language = (String) objects.get(LANGUAGE);
                    value = (String) objects.get(VALUE);

                    Field languageField = jsonNodeElement.getClass().getDeclaredField(LANGUAGE);
                    languageField.setAccessible(true);
                    languageField.set(jsonNodeElement, language);

                    Field valueField = jsonNodeElement.getClass().getDeclaredField(VALUE);
                    valueField.setAccessible(true);
                    valueField.set(jsonNodeElement, value);

                    javaObject[i] = jsonNodeElement;
                }
            }
        } catch (InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            throw new DigitalCardServiceException(e);

        }
        return javaObject;

    }

}
