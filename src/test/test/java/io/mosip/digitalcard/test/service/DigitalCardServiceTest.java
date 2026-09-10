package io.mosip.digitalcard.test.service;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.*;
import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.service.CardGeneratorService;
import io.mosip.digitalcard.service.impl.DigitalCardServiceImpl;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.digitalcard.util.*;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import io.mosip.kernel.core.logger.spi.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
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

import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(MockitoJUnitRunner.class)
public class DigitalCardServiceTest {

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
    private String rid = "testRid";
    private String partnerId = "partnerId";
    private String credentialType = "credentialType";
    private boolean isInitiateFlag = true;
    @Value("${mosip.digitalcard.datashare.policy.id}")
    private String dataSharePolicyId;
    @Value("${mosip.digitalcard.datashare.partner.id}")
    private String dataSharePartnerId;
    @Value("${mosip.digitalcard.websub.publish.topic:CREDENTIAL_STATUS_UPDATE}")
    private String topic;
    @Value("${mosip.digitalcard.uincard.password}")
    private String digitalCardPassword;

    @Test
    public void generateDigitalCardTest() throws Exception {
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
    public void testGenerateDigitalCard_Failure_VerificationFailed() throws Exception {
        String credential = "encryptedCredential";
        String decryptedCredential = "{ \"credentialSubject\": { \"id\": \"12345\" } }";
        String credentialType = "someType";
        String dataShareUrl = null;
        String eventId = "eventId";
        String transactionId = "transactionId";
        Map<String, Object> additionalAttributes = new HashMap<>();

        when(encryptionUtil.decryptData(credential)).thenReturn(decryptedCredential);

        JSONObject jsonObject = new JSONObject(decryptedCredential);
        JSONObject decryptedCredentialJson = jsonObject.getJSONObject("credentialSubject");

        assertThrows(DigitalCardServiceException.class, () -> {
            ReflectionTestUtils.invokeMethod(digitalCardService, "generateDigitalCard", credential, credentialType, dataShareUrl, eventId, transactionId, additionalAttributes);
        });

        verify(pdfCardServiceImpl, never()).generateCard(any(), anyString(), anyString(), anyMap());
        verify(webSubSubscriptionHelper, never()).digitalCardStatusUpdateEvent(anyString(), any());
    }

    @Test
    public void testGetDigitalCard_Success() {
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
        ((Field) field).setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testGetDigitalCard_InitiateFlagTrue() throws Exception {
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
    public void testInitiateCredentialRequest_DigitalCardServiceException() throws Exception {
        String rid = "testRid";
        String ridHash = "testRidHash";

        when(credentialUtil.reqCredential(any(CredentialRequestDto.class))).thenThrow(new DigitalCardServiceException("Error"));

        digitalCardService.initiateCredentialRequest(rid, ridHash);
        verify(logger).error(anyString(), any(DigitalCardServiceException.class));
    }

    @Test
    public void saveTransactionDetailsTest() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
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
        ((Method) saveTransactionDetailsMethod).setAccessible(true);

        saveTransactionDetailsMethod.invoke(digitalCardService, credentialResponse, idHash);
    }

    @Test
    public void testDigitalCardStatusUpdate_NewTransaction() throws Exception {
        byte[] data = new byte[]{1, 2, 3, 4};
        String dataSharePolicyId="mpolicy-default-digitalcard";
        String dataSharePartnerId="mpartner-default-digitalcard";
        String requestId = UUID.randomUUID().toString();
        String credentialType = "credentialType";
        String rid = "sampleRID";
        String topic = "sampleTopic";

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
    public void testGetRid() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = DigitalCardServiceImpl.class.getDeclaredMethod("getRid", Object.class);
        method.setAccessible(true);
        Object id = "http://example.com/credentials/123";
        String result = (String) method.invoke(digitalCardService, id);

        assertEquals("123", result);
    }

    @Test
    public void getPasswordTest() throws NoSuchMethodException {
        Method getPasswordMethod = DigitalCardServiceImpl.class.getDeclaredMethod("getPassword", org.json.JSONObject.class);
        getPasswordMethod.setAccessible(true);
        assertThrows(Exception.class, () -> {
            getPasswordMethod.invoke(digitalCardService, (JSONObject) null);
        });
    }

    @Test
    public void testGetPassword() throws Exception {
        String digitalCardPassword="attr1|attr2|attr3";
        String templateLang="eng";
        String[] attributes = digitalCardPassword.split("\\|");

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("attr1", "value1");
        jsonObject.put("attr2", "value2");
        jsonObject.put("attr3", "value3");
        ReflectionTestUtils.setField(digitalCardService, "digitalCardPassword", digitalCardPassword);
        ReflectionTestUtils.setField(digitalCardService, "templateLang", templateLang);
        ReflectionTestUtils.invokeMethod(digitalCardService, "getPassword", jsonObject);
    }

    @Test
    public void getFormattedPasswordAttributeTestNew(){
        String password="hvhjeyeyd#hvhdv@";
        ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
    }

    @Test
    public void testGetFormattedPasswordAttribute_LengthThree() {
        String password = "abc";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("abca", result);
    }

    @Test
    public void testGetFormattedPasswordAttribute_LengthTwo() {
        String password = "ab";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("abab", result);
    }

    @Test
    public void testGetFormattedPasswordAttribute_LengthOne() {
        String password = "a";
        String result = ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
        assertEquals("aaaa", result);
    }

    @Test
    public void loginErrorDetailsTest(){
        String rid = "sampleRid";
        String errorMsg = "sampleErrorMsg";
        LocalDateTime currentTime = LocalDateTime.now();
        String user = "sampleUser";

        digitalCardService.loginErrorDetails(rid, errorMsg);
    }
}
