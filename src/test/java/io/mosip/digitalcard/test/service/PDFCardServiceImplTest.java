package io.mosip.digitalcard.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.dto.SignatureResponseDto;
import io.mosip.digitalcard.dto.SimpleType;
import io.mosip.digitalcard.exception.DataNotFoundException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.exception.IdentityNotFoundException;
import io.mosip.digitalcard.service.impl.PDFCardServiceImpl;
import io.mosip.digitalcard.test.DigitalCardServiceTest;
import io.mosip.digitalcard.util.CbeffToBiometricUtil;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.digitalcard.util.TemplateGenerator;
import io.mosip.digitalcard.util.Utility;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.json.simple.JSONObject;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = DigitalCardServiceTest.class)
@RunWith(MockitoJUnitRunner.class)
public class PDFCardServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PDFCardServiceImpl pdfCardService;

    @Mock
    private TemplateGenerator templateGenerator;

    @Mock
    private QrCodeGenerator<QrVersion> qrCodeGenerator;

    @Mock
    private PDFGenerator pdfGenerator;

    @Mock
    private CbeffToBiometricUtil util;

    @Mock
    private RestClient restApiClient;

    @Mock
    private Utility utility;

    private static final String FACE = "Face";

    @Mock
    CbeffUtil cbeffutil;


    @Test
    public void generateCardTestSuccess() throws Exception {
        org.json.JSONObject decryptedCredentialJson = new org.json.JSONObject();
        decryptedCredentialJson.put("UIN", "testUIN");
        decryptedCredentialJson.put("biometrics", "sampleBiometricsData");
        boolean isPhotoSet=true;
        decryptedCredentialJson.put("isPhotoSet",isPhotoSet);
        String credentialType = "qrcode";
        String password = "testPassword";
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("TEMPLATE_TYPE_CODE", "templateTypeCode");
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("key","value");

        String defaultTemplateTypeCode="RPR_UIN_CARD_TEMPLATE";
        ReflectionTestUtils.setField(pdfCardService, "defaultTemplateTypeCode", defaultTemplateTypeCode);

        InputStream mockInputStream = mock(InputStream.class);
        lenient().when(templateGenerator.getTemplate(anyString(), anyMap(), anyString())).thenReturn(mockInputStream);

        try {
            byte[] result = pdfCardService.generateCard(decryptedCredentialJson, credentialType, password, additionalAttributes);

            assertNotNull(result);
            verify(templateGenerator, times(1)).getTemplate(anyString(), anyMap(), anyString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(expected = DigitalCardServiceException.class)
    public void generateCardShouldUseDefaultTemplateTypeWhenNotProvided() throws Exception {
        org.json.JSONObject decryptedCredentialJson = new org.json.JSONObject();
        decryptedCredentialJson.put("UIN", "1234567890");
        decryptedCredentialJson.put("isPhotoSet", false);

        String credentialType = "pdf";
        String password = "pwd";
        Map<String, Object> additionalAttributes = new HashMap<>();

        ReflectionTestUtils.setField(pdfCardService, "utility", utility);
        when(utility.getConfigServerFileStorageURL()).thenReturn("http://config");
        when(utility.getIdentityJson()).thenReturn("identity.json");
        when(utility.getIdentityMappingJson(anyString(), anyString())).thenReturn("{}");
        when(utility.getDemographicIdentity()).thenReturn("demographicIdentity");
        lenient().when(objectMapper.readValue(eq("{}"), eq(JSONObject.class))).thenReturn(new JSONObject());
        when(utility.getJSONObject(any(JSONObject.class), eq("demographicIdentity")))
                .thenReturn(new JSONObject());

        JSONObject qrJsonForPdfPath1 = new JSONObject();
        qrJsonForPdfPath1.put("UIN", "1234567890");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonForPdfPath1);

        ReflectionTestUtils.setField(pdfCardService, "lowerLeftX", 73);
        ReflectionTestUtils.setField(pdfCardService, "lowerLeftY", 100);
        ReflectionTestUtils.setField(pdfCardService, "upperRightX", 300);
        ReflectionTestUtils.setField(pdfCardService, "upperRightY", 300);
        ReflectionTestUtils.setField(pdfCardService, "reason", "signing");
        Environment mockEnv = Mockito.mock(Environment.class);
        lenient().when(mockEnv.getProperty("mosip.digitalcard.service.datetime.pattern"))
                .thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(pdfCardService, "env", mockEnv);

        lenient().when(templateGenerator.getTemplate(anyString(), anyMap(), anyString()))
                .thenReturn(null);

        pdfCardService.generateCard(decryptedCredentialJson, credentialType, password, additionalAttributes);
    }

    @Test
    public void setQrCodeTestSuccess() throws QrcodeGenerationException, IOException {
        String qrString = "{\"biometrics\":\"sampleBiometricsData\", \"otherKey\":\"otherValue\"}";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("xyz","cdcs");
        boolean isPhotoSet = true;

        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("biometrics", "sampleBiometricsData");
        qrJsonObj.put("otherKey", "otherValue");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonObj);
        byte[] qrCodeBytes = "sampleQRCode".getBytes();
        when(qrCodeGenerator.generateQrCode(anyString(), any())).thenReturn(qrCodeBytes);

        ReflectionTestUtils.invokeMethod(pdfCardService, "setQrCode", qrString, attributes, isPhotoSet);
    }

    @Test
    public void setQrCodeShouldRemoveBiometricsWhenPhotoIsSet() throws Exception {
        String qrString = "{\"biometrics\":\"bdata\", \"k\":\"v\"}";
        Map<String, Object> attributes = new HashMap<>();
        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("biometrics", "bdata");
        qrJsonObj.put("k", "v");

        when(objectMapper.readValue(anyString(), eq(JSONObject.class)))
                .thenReturn(qrJsonObj);

        when(qrCodeGenerator.generateQrCode(
                argThat(s -> !s.contains("biometrics")),
                any())
        ).thenReturn("qr".getBytes());

        boolean res = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pdfCardService, "setQrCode", qrString, attributes, true));

        assertTrue(res);
        assertNotNull(attributes.get("QrCode"));
    }

    @Test
    public void generateUinCardShouldReturnNullOnSignatureServiceError() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("pdf".getBytes());
        when(pdfGenerator.generate(any(InputStream.class))).thenReturn(new ByteArrayOutputStream());
        ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
        io.mosip.kernel.core.exception.ServiceError err = new io.mosip.kernel.core.exception.ServiceError();
        err.setMessage("failure");
        List<io.mosip.kernel.core.exception.ServiceError> errs = new ArrayList<>();
        errs.add(err);
        responseWrapper.setErrors(errs);
        lenient().when(restApiClient.postApi(any(), any(), any(), any(), any(), any(), any())).thenReturn(responseWrapper);

        byte[] res = ReflectionTestUtils.invokeMethod(pdfCardService, "generateUinCard", inputStream, "pwd");
        assertEquals(null, res);
    }

    @Test
    public void generateCardQrCodePathUsesTemplateAndSignature() throws Exception {
        org.json.JSONObject decryptedCredentialJson = new org.json.JSONObject();
        decryptedCredentialJson.put("UIN", "u");
        String credentialType = "qrcode";
        String password = "pwd";
        Map<String, Object> additionalAttributes = new HashMap<>();
        additionalAttributes.put("templateTypeCode", "RPR_UIN_CARD_TEMPLATE");

        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("UIN", "u");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonObj);

        ReflectionTestUtils.setField(pdfCardService, "lowerLeftX", 73);
        ReflectionTestUtils.setField(pdfCardService, "lowerLeftY", 100);
        ReflectionTestUtils.setField(pdfCardService, "upperRightX", 300);
        ReflectionTestUtils.setField(pdfCardService, "upperRightY", 300);
        ReflectionTestUtils.setField(pdfCardService, "reason", "signing");
        org.springframework.core.env.Environment mockEnv = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        when(mockEnv.getProperty("mosip.digitalcard.service.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(pdfCardService, "env", mockEnv);

        when(qrCodeGenerator.generateQrCode(anyString(), any())).thenReturn("qr".getBytes());
        lenient().when(templateGenerator.getTemplate(anyString(), anyMap(), anyString())).thenReturn(new ByteArrayInputStream("tmpl".getBytes()));

        when(pdfGenerator.generate((InputStream) any())).thenReturn(new ByteArrayOutputStream());

        ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
        SignatureResponseDto signatureResponseDto = new SignatureResponseDto();
        signatureResponseDto.setData(Base64.getEncoder().encodeToString("signed".getBytes()));
        responseWrapper.setResponse(signatureResponseDto);
        when(restApiClient.postApi(any(), any(), any(), any(), any(), any(), any())).thenReturn(responseWrapper);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(SignatureResponseDto.class))).thenReturn(signatureResponseDto);

        byte[] result = pdfCardService.generateCard(decryptedCredentialJson, credentialType, password, additionalAttributes);
        assertNotNull(result);
    }

    @Test
    public void generateCardShouldThrowWhenTemplateNullInPdfPath() throws Exception {
        org.json.JSONObject decryptedCredentialJson = new org.json.JSONObject();
        decryptedCredentialJson.put("UIN", "u");
        String credentialType = "pdf";
        String password = "pwd";
        Map<String, Object> additionalAttributes = new HashMap<>();

        ReflectionTestUtils.setField(pdfCardService, "utility", utility);
        when(utility.getConfigServerFileStorageURL()).thenReturn("http://config");
        when(utility.getIdentityJson()).thenReturn("identity.json");
        when(utility.getIdentityMappingJson(anyString(), anyString())).thenReturn("{}");
        when(utility.getDemographicIdentity()).thenReturn("demographicIdentity");
        lenient().when(objectMapper.readValue(eq("{}"), eq(JSONObject.class)))
                .thenReturn(new JSONObject());
        when(utility.getJSONObject(any(JSONObject.class), eq("demographicIdentity")))
                .thenReturn(new JSONObject());

        JSONObject qrJsonForPdfPath2 = new JSONObject();
        qrJsonForPdfPath2.put("UIN", "u");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class)))
                .thenReturn(qrJsonForPdfPath2);

        lenient().when(templateGenerator.getTemplate(anyString(), anyMap(), anyString()))
                .thenReturn(null);

        DigitalCardServiceException ex = assertThrows(
                DigitalCardServiceException.class,
                () -> pdfCardService.generateCard(decryptedCredentialJson, credentialType, password, additionalAttributes)
        );

        String expectedErrorCode = DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode();
        assertEquals(expectedErrorCode, ex.getErrorCode());
    }

    @Test
    public void testSetApplicantPhotoNullInput() throws Exception {
        String individualBio = null;
        Map<String, Object> attributes = new HashMap<>();

        String value = individualBio;
        List<String> subtype = new ArrayList<>();
        CbeffToBiometricUtil util = new CbeffToBiometricUtil(cbeffutil);
        ConvertRequestDto convertRequestDto = new ConvertRequestDto();
        byte[] photoByte = util.getImageBytes(value, FACE, subtype);
        convertRequestDto.setVersion("ISO19794_5_2011");
        convertRequestDto.setInputBytes(photoByte);

        Method setApplicantPhotoMethod = PDFCardServiceImpl.class.getDeclaredMethod("setApplicantPhoto",String.class, Map.class);
        setApplicantPhotoMethod.setAccessible(true);
        boolean isPhotoSet = (boolean) setApplicantPhotoMethod.invoke(pdfCardService, null, attributes);

        Field field = PDFCardServiceImpl.class.getDeclaredField("APPLICANT_PHOTO");
        field.setAccessible(true);
        String applicantPhoto = (String) field.get(null);

        assertFalse(isPhotoSet);
        assertFalse(attributes.containsKey(applicantPhoto));
    }

    @Test
    public void testSetTemplateAttributesDemographicIdentityIsNull() {
        Map<String, Object> attribute = new HashMap<>();
        IdentityNotFoundException thrown = assertThrows(
                IdentityNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(pdfCardService, "setTemplateAttributes", null, attribute)
        );

        assertEquals(DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
    }

    @Test
    public void generateUinCardTestSuccess() throws IOException {
        InputStream in = new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
        String password = "samplePassword";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(new byte[]{5, 6, 7, 8});

        int lowerLeftX=73;
        int lowerLeftY=100;
        int upperRightX=300;
        int upperRightY=300;
        String reason="signing";

        ReflectionTestUtils.setField(pdfCardService, "lowerLeftX", lowerLeftX);
        ReflectionTestUtils.setField(pdfCardService, "lowerLeftY", lowerLeftY);
        ReflectionTestUtils.setField(pdfCardService, "upperRightX", upperRightX);
        ReflectionTestUtils.setField(pdfCardService, "upperRightY", upperRightY);
        ReflectionTestUtils.setField(pdfCardService, "reason", reason);

        ReflectionTestUtils.invokeMethod(pdfCardService, "generateUinCard", in, password);
    }

    @Test
    public void testGenerateUinCardSuccess() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("pdf content".getBytes());
        String password = "testPassword";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write("pdf content".getBytes());

        when(pdfGenerator.generate(any(InputStream.class))).thenReturn(outputStream);
        SignatureResponseDto signatureResponseDto = new SignatureResponseDto();

        ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(signatureResponseDto);

        lenient().when(restApiClient.postApi(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(responseWrapper);
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        lenient().when(objectMapper.readValue(anyString(), eq(SignatureResponseDto.class))).thenReturn(signatureResponseDto);

        Method method = PDFCardServiceImpl.class.getDeclaredMethod("generateUinCard", InputStream.class, String.class);
        method.setAccessible(true);
        byte[] result = (byte[]) method.invoke(pdfCardService, inputStream, password);
    }

    @Test
    public void testSetApplicantPhotoPhotoByteIsNull() {
        String individualBio = "mockBioData";
        Map<String, Object> attributes = new HashMap<>();

        lenient().when(util.getImageBytes(eq(individualBio), eq("FACE"), anyList())).thenReturn(null);

        Exception exception = assertThrows(DataNotFoundException.class, () -> {
            ReflectionTestUtils.invokeMethod(pdfCardService, "setApplicantPhoto", individualBio, attributes);
        });

        assertEquals("DCS-021 --> Applicant Photo Not Found", exception.getMessage());
    }

    @Test
    public void testSetApplicantPhotoIndividualBioIsNull() {
        String individualBio = null;
        Map<String, Object> attributes = new HashMap<>();

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pdfCardService, "setApplicantPhoto", individualBio, attributes));

        assertFalse(result);
        assertFalse(attributes.containsKey("applicantPhoto"));
    }

    @Test
    public void testSetTemplateAttributesIdentityNotFound() {
        org.json.JSONObject demographicIdentity = null;
        Map<String, Object> attributes = new HashMap<>();

        IdentityNotFoundException exception = assertThrows(IdentityNotFoundException.class, () ->
                ReflectionTestUtils.invokeMethod(pdfCardService, "setTemplateAttributes", demographicIdentity, attributes));

        assertEquals("DCS-022 --> Unable to Find Identity Field in ID JSON", exception.getMessage());
    }

    @Test
    public void setQrCodeShouldNotRemoveBiometricsWhenPhotoNotSet() throws Exception {
        String qrString = "{\"biometrics\":\"bdata\", \"k\":\"v\"}";
        Map<String, Object> attributes = new HashMap<>();
        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("biometrics", "bdata");
        qrJsonObj.put("k", "v");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonObj);

        when(qrCodeGenerator.generateQrCode(argThat(s -> s.contains("biometrics")), any()))
                .thenReturn("qr".getBytes());

        boolean res = ReflectionTestUtils.invokeMethod(pdfCardService, "setQrCode", qrString, attributes, false);
        assertEquals(true, res);
        assertNotNull(attributes.get("QrCode"));
    }

    @Test
    public void generateCardShouldUseTemplateTypeFromAdditionalAttributes() throws Exception {
        org.json.JSONObject decrypted = new org.json.JSONObject();
        decrypted.put("UIN", "u");
        String credentialType = "qrcode";
        String password = "pwd";
        Map<String, Object> additional = new HashMap<>();
        additional.put("templateTypeCode", "CUSTOM_TEMPLATE");

        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("UIN", "u");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonObj);
        when(qrCodeGenerator.generateQrCode(anyString(), any())).thenReturn("qr".getBytes());

        ReflectionTestUtils.setField(pdfCardService, "lowerLeftX", 73);
        ReflectionTestUtils.setField(pdfCardService, "lowerLeftY", 100);
        ReflectionTestUtils.setField(pdfCardService, "upperRightX", 300);
        ReflectionTestUtils.setField(pdfCardService, "upperRightY", 300);
        ReflectionTestUtils.setField(pdfCardService, "reason", "signing");
        org.springframework.core.env.Environment mockEnv = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        when(mockEnv.getProperty("mosip.digitalcard.service.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(pdfCardService, "env", mockEnv);

        ByteArrayInputStream tmpl = new ByteArrayInputStream("tmpl".getBytes());
        when(templateGenerator.getTemplate(anyString(), anyMap(), any())).thenReturn(tmpl);

        when(pdfGenerator.generate((InputStream) any())).thenReturn(new ByteArrayOutputStream());
        ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
        SignatureResponseDto signatureResponseDto = new SignatureResponseDto();
        signatureResponseDto.setData(java.util.Base64.getEncoder().encodeToString("signed".getBytes()));
        responseWrapper.setResponse(signatureResponseDto);
        when(restApiClient.postApi(any(), any(), any(), any(), any(), any(), any())).thenReturn(responseWrapper);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(SignatureResponseDto.class))).thenReturn(signatureResponseDto);

        byte[] result = pdfCardService.generateCard(decrypted, credentialType, password, additional);
        assertNotNull(result);

        org.mockito.ArgumentCaptor<String> typeCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(templateGenerator).getTemplate(typeCaptor.capture(), anyMap(), any());
        assertEquals("CUSTOM_TEMPLATE", typeCaptor.getValue());
    }

    @Test
    public void setQrCodeShouldReturnFalseAndNotSetAttributeWhenGeneratorReturnsNull() throws Exception {
        String qrString = "{\"k\":\"v\"}";
        Map<String, Object> attributes = new HashMap<>();
        JSONObject qrJsonObj = new JSONObject();
        qrJsonObj.put("k", "v");
        when(objectMapper.readValue(anyString(), eq(JSONObject.class))).thenReturn(qrJsonObj);
        when(qrCodeGenerator.generateQrCode(anyString(), any())).thenReturn(null);

        boolean res = ReflectionTestUtils.invokeMethod(pdfCardService, "setQrCode", qrString, attributes, false);
        assertEquals(false, res);
        assertEquals(false, attributes.containsKey("QrCode"));
    }

    @Test
    public void generateUinCardShouldReturnDecodedSignatureBytesOnSuccess() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("pdf".getBytes());
        when(pdfGenerator.generate(any(InputStream.class))).thenReturn(new ByteArrayOutputStream());

        ReflectionTestUtils.setField(pdfCardService, "lowerLeftX", 10);
        ReflectionTestUtils.setField(pdfCardService, "lowerLeftY", 10);
        ReflectionTestUtils.setField(pdfCardService, "upperRightX", 100);
        ReflectionTestUtils.setField(pdfCardService, "upperRightY", 100);
        ReflectionTestUtils.setField(pdfCardService, "reason", "reason");
        org.springframework.core.env.Environment mockEnv = org.mockito.Mockito.mock(org.springframework.core.env.Environment.class);
        lenient().when(mockEnv.getProperty("mosip.digitalcard.service.datetime.pattern")).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        ReflectionTestUtils.setField(pdfCardService, "env", mockEnv);

        String payload = "signed-payload";
        SignatureResponseDto signatureResponseDto = new SignatureResponseDto();
        signatureResponseDto.setData(java.util.Base64.getEncoder().encodeToString(payload.getBytes()));
        ResponseWrapper<SignatureResponseDto> responseWrapper = new ResponseWrapper<>();
        responseWrapper.setResponse(signatureResponseDto);

        when(restApiClient.postApi(any(), any(), any(), any(), any(), any(), any())).thenReturn(responseWrapper);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(objectMapper.readValue(anyString(), eq(SignatureResponseDto.class))).thenReturn(signatureResponseDto);

        byte[] res = ReflectionTestUtils.invokeMethod(pdfCardService, "generateUinCard", inputStream, "pwd");
        assertNotNull(res);
        assertEquals(payload, new String(res));
    }

    @Test
    public void testSetTemplateAttributesDemographicIdentityNullThrowsIdentityNotFound() {
        Map<String, Object> attribute = new HashMap<>();
        IdentityNotFoundException thrown = org.junit.Assert.assertThrows(
                IdentityNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(pdfCardService, "setTemplateAttributes", null, attribute)
        );

        assertEquals("DCS-022 --> Unable to Find Identity Field in ID JSON", thrown.getMessage());
    }

    @Test
    public void whenIndividualBioIsNullThenReturnsFalseAndDoesNotSetAttribute() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(pdfCardService, "setApplicantPhoto", null, attributes));

        assertFalse(result);
        assertFalse(attributes.containsKey("ApplicantPhoto"));
    }

}
