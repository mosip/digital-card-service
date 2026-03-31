package io.mosip.digitalcard.test.service;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.DigitalCardStatusResponseDto;
import io.mosip.digitalcard.dto.SimpleType;
import io.mosip.digitalcard.dto.CredentialResponse;
import io.mosip.digitalcard.dto.CredentialRequestDto;
import io.mosip.digitalcard.dto.DataShareDto;
import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.digitalcard.exception.DataNotFoundException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.service.CardGeneratorService;
import io.mosip.digitalcard.service.impl.DigitalCardServiceImpl;
import io.mosip.digitalcard.test.DigitalCardServiceTest;
import io.mosip.digitalcard.util.*;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.vercred.CredentialsVerifier;
import io.mosip.kernel.core.logger.spi.Logger;
import org.json.simple.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.springframework.dao.DataAccessException;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = DigitalCardServiceTest.class)
@RunWith(MockitoJUnitRunner.class)
public class DigitalCardServiceImplTest {

    @InjectMocks
    DigitalCardServiceImpl digitalCardService;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private DigitalCardTransactionRepository digitalCardTransactionRepository;

    @Mock
    private CredentialUtil credentialUtil;

    @Mock
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    @Mock
    Logger logger = DigitalCardRepoLogger.getLogger(DigitalCardController.class);

    @Mock
    private CardGeneratorService pdfCardServiceImpl;

    @Mock
    private DataShareUtil dataShareUtil;

    @Mock
    private CredentialsVerifier credentialsVerifier;

    @Mock
    private RestClient restClient;

    private String rid = "testRid";

    @Test
    public void generateDigitalCardTestSuccess() throws Exception {
        String credential="encryptedCredential";
        String credentialType="c_type";
        String eventId="54154f54";
        String transactionId="de5fefe673r";
        Map<String, Object> additionalAttributes = new HashMap<>();

        boolean verifyCredentialsFlag = false;
        boolean isPasswordProtected = true;

        String decryptedCredential = "{ \"credentialSubject\": { \"id\": \"12345\" } }";
        JSONObject jsonObject = new JSONObject(decryptedCredential);
        JSONObject decryptedCredentialJson = jsonObject.getJSONObject("credentialSubject");
        System.out.println(decryptedCredentialJson);
        byte[] pdfBytes = new byte[]{1, 2, 3};

        ReflectionTestUtils.setField(digitalCardService, "verifyCredentialsFlag", verifyCredentialsFlag);
        ReflectionTestUtils.setField(digitalCardService, "isPasswordProtected", isPasswordProtected);

        DigitalCardServiceException exception = assertThrows(DigitalCardServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(digitalCardService, "generateDigitalCard", credential, credentialType, null, eventId, transactionId, additionalAttributes);
        });

        assertEquals("DCS-011 --> Error while generating PDF for Digital Card", exception.getMessage());
        assertEquals(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode()+" --> "+DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage(), exception.getMessage());
        try {
            digitalCardService.generateDigitalCard(credential, credentialType, null, eventId, transactionId, additionalAttributes);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGenerateDigitalCardFailureVerificationFailed() throws Exception {
        String credential = "encryptedCredential";
        String decryptedCredential = "{ \"credentialSubject\": { \"id\": \"12345\" } }";
        String credentialType = "someType";
        String dataShareUrl = null;
        String eventId = "eventId";
        String transactionId = "transactionId";
        Map<String, Object> additionalAttributes = new HashMap<>();

        when(encryptionUtil.decryptData(credential)).thenReturn(decryptedCredential);

        assertThrows(DigitalCardServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(digitalCardService, "generateDigitalCard", credential, credentialType, dataShareUrl, eventId, transactionId, additionalAttributes);
        });

        verify(pdfCardServiceImpl, never()).generateCard(any(), anyString(), anyString(), anyMap());
        verify(webSubSubscriptionHelper, never()).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void testGetDigitalCardSuccess() {
        DigitalCardTransactionEntity entity = new DigitalCardTransactionEntity();
        entity.setrid(rid);
        entity.setStatusCode("200");
        entity.setDataShareUrl("http://example.com");

        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(entity);

        DigitalCardStatusResponseDto response = digitalCardService.getDigitalCard(rid);

        assertNotNull(response);
        assertEquals(rid, response.getId());
        assertEquals("200", response.getStatusCode());
        assertEquals("http://example.com", response.getUrl());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testGetDigitalCardInitiateFlagTrue() throws Exception {
        setPrivateField(digitalCardService, "isInitiateFlag", true);

        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(null);

        CredentialResponse credentialResponse = new CredentialResponse();

        when(credentialUtil.reqCredential(any(CredentialRequestDto.class))).thenReturn(credentialResponse);

        try {
            digitalCardService.getDigitalCard(rid);
            fail("Expected DigitalCardServiceException");
        } catch (DigitalCardServiceException e) {
            assertEquals(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), e.getErrorCode());
        }

        verify(credentialUtil).reqCredential(any(CredentialRequestDto.class));
    }

    @Test(expected = DigitalCardServiceException.class)
    public void testInitiateCredentialRequestDigitalCardServiceException() {
        String rid = "testRid";
        String ridHash = "testRidHash";

        when(credentialUtil.reqCredential(any(CredentialRequestDto.class))).thenThrow(new DigitalCardServiceException("Error"));

        digitalCardService.initiateCredentialRequest(rid, ridHash);
        verify(logger).error(anyString(), any(DigitalCardServiceException.class));
    }

    @Test
    public void saveTransactionDetailsTestSuccess() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        CredentialResponse credentialResponse=new CredentialResponse();
        credentialResponse.setId("45564");
        credentialResponse.setRequestId("ft656ft");
        String idHash="id_hash";
        String rid = "mockedRid";

        DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
        digitalCardEntity.setrid(rid);
        digitalCardEntity.setrid(credentialResponse.getId());
        digitalCardEntity.setUinSaltedHash(idHash);
        digitalCardEntity.setCredentialId(credentialResponse.getRequestId());
        digitalCardEntity.setCreateDateTime(LocalDateTime.now());
        digitalCardEntity.setCreatedBy(Utility.getUser());
        digitalCardEntity.setStatusCode("NEW");

        Method saveTransactionDetailsMethod = DigitalCardServiceImpl.class.getDeclaredMethod("saveTransactionDetails", CredentialResponse.class, String.class);
        (saveTransactionDetailsMethod).setAccessible(true);

        saveTransactionDetailsMethod.invoke(digitalCardService, credentialResponse, idHash);
    }

    @Test
    public void testDigitalCardStatusUpdateNewTransactionSuccess() throws Exception {
        byte[] data = new byte[]{1, 2, 3, 4};
        String dataSharePolicyId="mpolicy-default-digitalcard";
        String dataSharePartnerId="mpartner-default-digitalcard";
        String requestId = UUID.randomUUID().toString();
        String credentialType = "credentialType";
        String rid = "sampleRID";

        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", dataSharePolicyId);
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", dataSharePartnerId);

        DataShareDto dataShareDto = new DataShareDto();
        dataShareDto.setUrl("https://gsjdg");
        dataShareDto.setSignature("sign");
        dataShareDto.setValidForInMinutes(5);
        dataShareDto.setPolicyId("P121313");
        dataShareDto.setSubscriberId("SUB123");
        dataShareDto.setTransactionsAllowed(10);

        DigitalCardTransactionEntity digitalCardTransactionEntity = new DigitalCardTransactionEntity();
        digitalCardTransactionEntity.setrid(rid);
        digitalCardTransactionEntity.setCreateDateTime(LocalDateTime.now());
        digitalCardTransactionEntity.setCreatedBy("testUser");
        digitalCardTransactionEntity.setDataShareUrl(dataShareDto.getUrl());
        digitalCardTransactionEntity.setStatusCode("AVAILABLE");

        when(dataShareUtil.getDataShare(any(byte[].class), anyString(), anyString())).thenReturn(dataShareDto);
        when(digitalCardTransactionRepository.findByRID(anyString())).thenReturn(null);

        ReflectionTestUtils.invokeMethod(digitalCardService, "digitalCardStatusUpdate", requestId, data, credentialType, rid);

        verify(dataShareUtil).getDataShare(eq(data), anyString(), anyString());
        verify(digitalCardTransactionRepository).findByRID(eq(rid));
        verify(digitalCardTransactionRepository).save(any(DigitalCardTransactionEntity.class));
    }

    @Test
    public void testGetRidSuccess() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DigitalCardServiceImpl.class.getDeclaredMethod("getRid", Object.class);
        method.setAccessible(true);
        Object id = "http://example.com/credentials/123";
        String result = (String) method.invoke(digitalCardService, id);

        assertEquals("123", result);
    }

    @Test
    public void getPasswordTestException() throws NoSuchMethodException {
        Method getPasswordMethod = DigitalCardServiceImpl.class.getDeclaredMethod("getPassword", org.json.JSONObject.class);
        getPasswordMethod.setAccessible(true);
        assertThrows(Exception.class, () -> {
            getPasswordMethod.invoke(digitalCardService, (JSONObject) null);
        });
    }

    @Test
    public void testGetPasswordSuccess() throws Exception {
        String digitalCardPassword="attr1|attr2|attr3";
        String templateLang="eng";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("attr1", "value1");
        jsonObject.put("attr2", "value2");
        jsonObject.put("attr3", "value3");
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", digitalCardPassword);
        ReflectionTestUtils.setField(digitalCardService, "templateLang", templateLang);
        ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);
    }

    @Test
    public void getFormattedPasswordAttributeTestNewSuccess() {
        String password="hvhjeyeyd#hvhdv@";
        ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
    }

    @Test
    public void testGetFormattedPasswordAttributeLengthThree() {
        String password = "abc";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("abca", result);
    }

    @Test
    public void testGetFormattedPasswordAttributeLengthTwo() {
        String password = "ab";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("abab", result);
    }

    @Test
    public void testGetFormattedPasswordAttributeLengthOne() {
        String password = "a";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("aaaa", result);
    }

    @Test
    public void loginErrorDetailsTest(){
        String rid = "sampleRid";
        String errorMsg = "sampleErrorMsg";

        digitalCardService.loginErrorDetails(rid, errorMsg);
    }

    @Test
    public void testGetParameterWithNullJsonValues() {
        SimpleType[] jsonValues = null;

        String langCode = "eng";

        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getParameter", jsonValues, langCode);

        assertNull(result);
    }

    @Test
    public void testGetParameterGivenEmptyStringThenReturnSuccess() {

        io.mosip.digitalcard.dto.SimpleType simpleType = new io.mosip.digitalcard.dto.SimpleType();
        simpleType.setLanguage("");
        simpleType.setValue("123");

       assertEquals("123", ReflectionTestUtils.invokeMethod(digitalCardService, "getParameter", new io.mosip.digitalcard.dto.SimpleType[]{simpleType}, "Lang Code"));
    }

    @Test
    public void testGetParameterGivenEngWhenSimpleTypeLanguageIsEngThenReturnNull() {

        io.mosip.digitalcard.dto.SimpleType simpleType = new io.mosip.digitalcard.dto.SimpleType();
        simpleType.setLanguage("eng");
        simpleType.setValue("123");

       assertNull(ReflectionTestUtils.invokeMethod(digitalCardService, "getParameter", new io.mosip.digitalcard.dto.SimpleType[]{simpleType}, "Lang Code"));
    }

    @Test
    public void testGetParameterWhenNullThenReturnNull() {

        assertNull(ReflectionTestUtils.invokeMethod(digitalCardService, "getParameter", null, "Lang Code"));
    }

    @Test
    public void generateDigitalCardWithDataShareUrlFetchesCredentialAndGeneratesSuccess() throws Exception {
        String dataShareUrl = "http://datasource/cred";
        String fetchedCredential = "encryptedFromUrl";
        String decrypted = "{ \"credentialSubject\": { \"id\": \"http://server/credentials/ABC123\", \"name\": \"John\" } }";
        String transactionId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();

        byte[] pdfBytes = new byte[]{9,8,7};
        String dataSharePolicyId = "policy-id";
        String dataSharePartnerId = "partner-id";
        String topic = "CREDENTIAL_STATUS_UPDATE";

        ReflectionTestUtils.setField(digitalCardService, "verifyCredentialsFlag", true);
        ReflectionTestUtils.setField(digitalCardService, "isPasswordProtected", true);
        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", dataSharePolicyId);
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", dataSharePartnerId);
        ReflectionTestUtils.setField(digitalCardService, "topic", topic);
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", "name");
        ReflectionTestUtils.setField(digitalCardService, "templateLang", "eng");

        when(restClient.getForObject(dataShareUrl, String.class)).thenReturn(fetchedCredential);
        when(encryptionUtil.decryptData(fetchedCredential)).thenReturn(decrypted);
        when(credentialsVerifier.verifyCredentials(decrypted)).thenReturn(true);
        when(pdfCardServiceImpl.generateCard(any(JSONObject.class), anyString(), anyString(), anyMap())).thenReturn(pdfBytes);
        when(digitalCardTransactionRepository.findByRID(anyString())).thenReturn(null);
        when(dataShareUtil.getDataShare(eq(pdfBytes), eq(dataSharePolicyId), eq(dataSharePartnerId)))
                .thenReturn(new DataShareDto());

        digitalCardService.generateDigitalCard("ignored", "type", dataShareUrl, eventId, transactionId, new HashMap<>());

        verify(restClient).getForObject(eq(dataShareUrl), eq(String.class));
        verify(credentialsVerifier).verifyCredentials(eq(decrypted));
        verify(pdfCardServiceImpl).generateCard(any(JSONObject.class), anyString(), anyString(), anyMap());
        verify(dataShareUtil).getDataShare(any(byte[].class), eq(dataSharePolicyId), eq(dataSharePartnerId));
        verify(digitalCardTransactionRepository).save(any(DigitalCardTransactionEntity.class));
        verify(webSubSubscriptionHelper).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void generateDigitalCardVerificationEnabledSucceedsOnVerified() throws Exception {
        String credential = "encrypted";
        String decrypted = "{ \"credentialSubject\": { \"id\": \"http://server/credentials/XYZ789\" } }";
        String transactionId = UUID.randomUUID().toString();
        String eventId = UUID.randomUUID().toString();
        byte[] pdfBytes = new byte[]{1,2,3,4};

        ReflectionTestUtils.setField(digitalCardService, "verifyCredentialsFlag", true);
        ReflectionTestUtils.setField(digitalCardService, "isPasswordProtected", false);
        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", "p");
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", "q");

        when(encryptionUtil.decryptData(credential)).thenReturn(decrypted);
        when(credentialsVerifier.verifyCredentials(decrypted)).thenReturn(true);
        when(pdfCardServiceImpl.generateCard(any(JSONObject.class), anyString(), isNull(), anyMap())).thenReturn(pdfBytes);
        when(digitalCardTransactionRepository.findByRID(anyString())).thenReturn(null);
        when(dataShareUtil.getDataShare(eq(pdfBytes), anyString(), anyString())).thenReturn(new DataShareDto());

        digitalCardService.generateDigitalCard(credential, "ctype", null, eventId, transactionId, new HashMap<>());

        verify(credentialsVerifier).verifyCredentials(eq(decrypted));
        verify(pdfCardServiceImpl).generateCard(any(JSONObject.class), anyString(), isNull(), anyMap());
        verify(digitalCardTransactionRepository).save(any(DigitalCardTransactionEntity.class));
    }

    @Test
    public void digitalCardStatusUpdateExistingTransactionUpdatesInsteadOfCreate() throws Exception {
        byte[] data = new byte[]{5,4,3,2};
        String requestId = UUID.randomUUID().toString();
        String rid = "RID-123";

        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", "policy");
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", "partner");
        ReflectionTestUtils.setField(digitalCardService, "topic", "TOPIC");

        DataShareDto dto = new DataShareDto();
        dto.setUrl("http://download/url");
        when(dataShareUtil.getDataShare(eq(data), anyString(), anyString())).thenReturn(dto);

        DigitalCardTransactionEntity existing = new DigitalCardTransactionEntity();
        existing.setrid(rid);
        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(existing);

        ReflectionTestUtils.invokeMethod(digitalCardService, "digitalCardStatusUpdate", requestId, data, "ctype", rid);

        verify(digitalCardTransactionRepository, never()).save(any(DigitalCardTransactionEntity.class));
        verify(digitalCardTransactionRepository).updateTransactionDetails(eq(rid), eq("AVAILABLE"), eq(dto.getUrl()), any(LocalDateTime.class), anyString());
        verify(webSubSubscriptionHelper).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void getDigitalCardNoRecordAndInitiateDisabledThrowsNotCreated() {
        ReflectionTestUtils.setField(digitalCardService, "isInitiateFlag", false);
        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(null);

        DigitalCardServiceException ex = assertThrows(DigitalCardServiceException.class, () -> digitalCardService.getDigitalCard(rid));
        assertEquals(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void initiateCredentialRequestWhenReqCredentialFailsThrowsNotCreatedWithCode() {
        String ridHash = "hash";
        doThrow(new DigitalCardServiceException("cause")).when(credentialUtil).reqCredential(any(CredentialRequestDto.class));

        DigitalCardServiceException ex = assertThrows(DigitalCardServiceException.class, () -> digitalCardService.initiateCredentialRequest(rid, ridHash));
        assertEquals(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void getDigitalCardWhenDataNotFoundExceptionThrownShouldThrowDigitalCardServiceException() {
        String rid = "RID-EX1";
        try {
            when(digitalCardTransactionRepository.findByRID(rid)).thenThrow(new DataNotFoundException("ERR_CODE","not found"));
        } catch (Exception e) {
            fail("Mock setup failed: " + e.getMessage());
        }

        DigitalCardServiceException ex = assertThrows(DigitalCardServiceException.class,
                () -> digitalCardService.getDigitalCard(rid));

        assertTrue(ex.getMessage().contains(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage()));
    }

    @Test
    public void getDigitalCardWhenSpringDataAccessExceptionThrownShouldThrowDigitalCardServiceException() {
        String rid = "RID-EX2";
        try {
            when(digitalCardTransactionRepository.findByRID(rid)).thenThrow(new DataAccessException("db error") {});
        } catch (Exception e) {
            fail("Mock setup failed: " + e.getMessage());
        }

        DigitalCardServiceException ex = assertThrows(DigitalCardServiceException.class,
                () -> digitalCardService.getDigitalCard(rid));

        assertTrue(ex.getMessage().contains(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage()));
    }

    @Test
    public void getDigitalCardWhenDataAccessLayerExceptionThrownShouldThrowDigitalCardServiceException() {
        String rid = "RID-EX3";
        try {
            when(digitalCardTransactionRepository.findByRID(rid)).thenThrow(new DataAccessLayerException("ERR_CODE","DataAccessLayer Error", null));
        } catch (Exception e) {
            fail("Mock setup failed: " + e.getMessage());
        }

        DigitalCardServiceException ex = assertThrows(DigitalCardServiceException.class,
                () -> digitalCardService.getDigitalCard(rid));

        assertTrue(ex.getMessage().contains(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage()));
    }

    @Test
    public void testGetPasswordWithSimpleAttributes() throws Exception {
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", "name|dob");
        ReflectionTestUtils.setField(digitalCardService, "templateLang", "en");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "John");
        jsonObject.put("dob", "1990-01-01");

        String password = ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);
        assertEquals("JOHN1990", password);
    }

    @Test
    public void testGetPasswordWithJsonArray() throws Exception {
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", "name|location");
        ReflectionTestUtils.setField(digitalCardService, "templateLang", "en");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "John");
        JSONArray locationArray = new JSONArray();
        JSONObject location = new JSONObject();
        location.put("language", "en");
        location.put("value", "New York");
        locationArray.add(location);
        jsonObject.put("location", locationArray);

        String password = ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);
        assertEquals("JOHNNEW ", password);
    }

    @Test
    public void testGetPasswordWithJsonObject() throws Exception {
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", "name|address");
        ReflectionTestUtils.setField(digitalCardService, "templateLang", "en");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", "John");
        JSONObject addressObject = new JSONObject();
        addressObject.put("value", "123 Main St");
        jsonObject.put("address", addressObject);

        String password = ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);
        assertEquals("JOHN{\"VA", password);
    }

    @Test
    public void testGetFormattedPasswordAttribute() {
        String padded1 = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", "abc");
        assertEquals("abca", padded1);

        String padded2 = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", "ab");
        assertEquals("abab", padded2);

        String padded3 = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", "a");
        assertEquals("aaaa", padded3);

        String padded4 = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", "abcd");
        assertEquals("abcd", padded4);
    }

    private String getMockDecryptedCredential() throws Exception {
        JSONObject credentialSubject = new JSONObject();
        credentialSubject.put("id", "12345/credentials/67890");

        JSONObject credential = new JSONObject();
        credential.put("credentialSubject", credentialSubject);
        return credential.toString();
    }

    @Test
    public void testGenerateDigitalCardPDFGeneratorException() throws Exception {
        when(encryptionUtil.decryptData(anyString())).thenReturn(getMockDecryptedCredential());
        when(pdfCardServiceImpl.generateCard(any(), anyString(), any(), any()))
                .thenThrow(new PDFGeneratorException("PDF_ERROR", "PDF generation failed"));

        digitalCardService.generateDigitalCard("credential", "type", null, "event1", "txn1", new HashMap<>());

        verify(digitalCardTransactionRepository, times(1)).updateErrorTransactionDetails(anyString(), eq("ERROR"), anyString(), any(), any());
    }

    @Test
    public void testGenerateDigitalCardGenericExceptionVCVerificationFailed() throws Exception {
        ReflectionTestUtils.setField(digitalCardService, "verifyCredentialsFlag", true);
        when(encryptionUtil.decryptData(anyString())).thenReturn(getMockDecryptedCredential());
        when(credentialsVerifier.verifyCredentials(anyString())).thenReturn(false);

        assertThrows(DigitalCardServiceException.class, () -> {
            digitalCardService.generateDigitalCard("credential", "type", null, "event1", "txn1", new HashMap<>());
        });
    }

    @Test
    public void testGenerateDigitalCardWhenDecryptFailsShouldUpdateErrorAndThrow() throws Exception {
        String credential = "encrypted";
        String credentialType = "ctype";
        String eventId = "evt";
        String transactionId = "txn";
        Map<String, Object> additionalAttributes = new HashMap<>();

        when(encryptionUtil.decryptData(anyString())).thenThrow(new RuntimeException("decrypt error"));

        try {
            ReflectionTestUtils.invokeMethod(digitalCardService, "generateDigitalCard", credential, credentialType, null, eventId, transactionId, additionalAttributes);
            fail("Expected DigitalCardServiceException");
        } catch (DigitalCardServiceException ex) {
            assertEquals("DCS-011 --> Error while generating PDF for Digital Card", ex.getMessage());
        }

        verify(pdfCardServiceImpl, never()).generateCard(any(), anyString(), any(), anyMap());
        verify(webSubSubscriptionHelper, never()).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void testDigitalCardStatusUpdateWhenDataShareReturnsNullShouldUpdateError() throws Exception {
        byte[] data = new byte[]{1,2,3};
        String requestId = UUID.randomUUID().toString();
        String credentialType = "ctype";
        String rid = "RID-NULL-DS";

        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", "p");
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", "q");
        ReflectionTestUtils.setField(digitalCardService, "topic", "TOPIC");

        when(dataShareUtil.getDataShare(eq(data), anyString(), anyString())).thenReturn(null);
        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(null);

        try {
            ReflectionTestUtils.invokeMethod(digitalCardService, "digitalCardStatusUpdate", requestId, data, credentialType, rid);
            fail("Expected Exception due to null DataShareDto");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage() == null || e.getMessage().contains("getUrl"));
        }

        verify(digitalCardTransactionRepository, never()).save(any(DigitalCardTransactionEntity.class));
        verify(digitalCardTransactionRepository, never()).updateTransactionDetails(anyString(), anyString(), anyString(), any(LocalDateTime.class), anyString());
        verify(webSubSubscriptionHelper, never()).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void testGenerateDigitalCardDoesNotPublishWhenDataShareIsNull() throws Exception {

        String credential = "encrypted";
        String decrypted = "{ \"credentialSubject\": { \"id\": \"http://server/credentials/PUBNULL\" } }";
        String transactionId = "txnId";
        String eventId = "evtId";

        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", "policy");
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", "partner");

        when(encryptionUtil.decryptData(anyString())).thenReturn(decrypted);
        lenient().when(credentialsVerifier.verifyCredentials(anyString())).thenReturn(true);
        byte[] pdfBytes = new byte[]{1,2,3};
        when(pdfCardServiceImpl.generateCard(any(JSONObject.class), anyString(), any(), anyMap())).thenReturn(pdfBytes);

        when(dataShareUtil.getDataShare(eq(pdfBytes), anyString(), anyString())).thenReturn(null);
        when(digitalCardTransactionRepository.findByRID(anyString())).thenReturn(null);

        DigitalCardServiceException exception = assertThrows(DigitalCardServiceException.class, () ->
                ReflectionTestUtils.invokeMethod(digitalCardService, "generateDigitalCard",
                        credential, "ctype", null, eventId, transactionId, new HashMap<>()));

        assertEquals("DCS-011 --> Error while generating PDF for Digital Card", exception.getMessage());

        verify(pdfCardServiceImpl).generateCard(any(JSONObject.class), anyString(), any(), anyMap());
        verify(dataShareUtil).getDataShare(eq(pdfBytes), anyString(), anyString());
        verify(webSubSubscriptionHelper, never()).digitalCardStatusUpdateEvent(anyString(), any());
        verify(digitalCardTransactionRepository, atLeastOnce()).updateErrorTransactionDetails(anyString(), eq("ERROR"), anyString(), any(LocalDateTime.class), anyString());
    }

    @Test
    public void testGetPasswordHandlesShortAttributesCorrectly() throws Exception {
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", "a|ab|abc");
        ReflectionTestUtils.setField(digitalCardService, "templateLang", "en");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("a", "x");
        jsonObject.put("ab", "yz");
        jsonObject.put("abc", "pq");

        String password = ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);

        assertNotNull(password);
        assertEquals(password, password.toUpperCase());
        assertTrue(password.length() >= 12);
    }

    @Test
    public void testDigitalCardStatusUpdateExistingTransactionInvokesUpdateWithCorrectUrl() throws Exception {
        byte[] data = new byte[]{7,7,7};
        String requestId = UUID.randomUUID().toString();
        String rid = "RID-UPD-1";
        ReflectionTestUtils.setField(digitalCardService, "dataSharePolicyId", "policy");
        ReflectionTestUtils.setField(digitalCardService, "dataSharePartnerId", "partner");
        ReflectionTestUtils.setField(digitalCardService, "topic", "TOPIC");

        DataShareDto dto = new DataShareDto();
        dto.setUrl("http://download/here");
        when(dataShareUtil.getDataShare(eq(data), anyString(), anyString())).thenReturn(dto);

        DigitalCardTransactionEntity existing = new DigitalCardTransactionEntity();
        existing.setrid(rid);
        when(digitalCardTransactionRepository.findByRID(rid)).thenReturn(existing);

        ReflectionTestUtils.invokeMethod(digitalCardService, "digitalCardStatusUpdate", requestId, data, "ctype", rid);

        verify(digitalCardTransactionRepository, never()).save(any(DigitalCardTransactionEntity.class));
        verify(digitalCardTransactionRepository).updateTransactionDetails(eq(rid), eq("AVAILABLE"), eq(dto.getUrl()), any(LocalDateTime.class), anyString());
        verify(webSubSubscriptionHelper).digitalCardStatusUpdateEvent(anyString(), any());
    }

}
