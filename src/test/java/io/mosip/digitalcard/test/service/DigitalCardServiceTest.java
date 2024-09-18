package io.mosip.digitalcard.test.service;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.*;
import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.service.impl.DigitalCardServiceImpl;
import io.mosip.digitalcard.service.impl.PDFCardServiceImpl;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.digitalcard.util.*;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.vercred.CredentialsVerifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = TestBootApplication.class)
@RunWith(MockitoJUnitRunner.class)
public class DigitalCardServiceTest {
    @InjectMocks
    DigitalCardServiceImpl digitalCardService;
    @Mock
    private RestClient restClient;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private PDFCardServiceImpl pdfCardServiceImpl;

    @Mock
    private CredentialsVerifier credentialsVerifier;
    @Mock
    private DigitalCardTransactionRepository digitalCardTransactionRepository;
    @Mock
    private CredentialUtil credentialUtil;
    @Mock
    Logger logger = DigitalCardRepoLogger.getLogger(DigitalCardController.class);

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

    @Test
    public void generateDigitalCardTest() throws Exception {
        String credential="esrsrr";
        String credentialType="c_type";
        String dataShareUrl="gfeuygfeygu";
        String eventId="54154f54";
        String transactionId="de5fefe673r";
        Map<String, Object> additionalAttributes = new HashMap<>();

        when(restClient.getForObject(dataShareUrl, String.class)).thenReturn(credential);
        when(encryptionUtil.decryptData(credential)).thenReturn("decryptedCredential");
        when(credentialsVerifier.verifyCredentials(anyString())).thenReturn(true);
        when(pdfCardServiceImpl.generateCard(any(org.json.JSONObject.class), eq(credentialType), anyString(), anyMap()))
                .thenReturn(new byte[]{});

        digitalCardService.generateDigitalCard(credential, credentialType, dataShareUrl, eventId, transactionId, additionalAttributes);
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
    public void digitalCardStatusUpdateTest() throws Throwable {
        DataShareDto dataShareDto = new DataShareDto();
        dataShareDto.setUrl("hgvv");
        dataShareDto.setSignature("dadw");
        String rid = "mockedRid";
        byte[] data = "someData".getBytes();
        String requestId = "123";
        String credentialType = "type";
        DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
        digitalCardEntity.setrid(rid);
        digitalCardEntity.setCreateDateTime(LocalDateTime.now());
        digitalCardEntity.setCreatedBy(Utility.getUser());
        digitalCardEntity.setDataShareUrl(dataShareDto.getUrl());
        digitalCardEntity.setStatusCode("AVAILABLE");
        digitalCardTransactionRepository.save(digitalCardEntity);

        ReflectionTestUtils.invokeMethod(digitalCardService, "digitalCardStatusUpdate", requestId, data, credentialType, rid);
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
    public void getFormattedPasswordAttributeTestNew(){
        String password="hvhjeyeyd#hvhdv@";
        ReflectionTestUtils.invokeMethod(digitalCardService, "getFormattedPasswordAttribute", password);
    }
    @Test
    public void getParameterTest(){
        SimpleType[] jsonValues = {
                new SimpleType()
        };
        String langCode="eng";
        int count=0;
        String lang = jsonValues[count].getLanguage();

        ReflectionTestUtils.invokeMethod(digitalCardService, "getParameter", jsonValues, langCode);
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
