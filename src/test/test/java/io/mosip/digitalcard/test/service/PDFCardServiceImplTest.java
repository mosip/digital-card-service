package io.mosip.digitalcard.test.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.dto.SignatureResponseDto;
import io.mosip.digitalcard.exception.IdentityNotFoundException;
import io.mosip.digitalcard.service.impl.PDFCardServiceImpl;
import io.mosip.digitalcard.test.TestBootApplication;
import io.mosip.digitalcard.util.CbeffToBiometricUtil;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.digitalcard.util.TemplateGenerator;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = TestBootApplication.class)
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
    private RestClient restApiClient;

    private static final String APPLICANT_PHOTO = "ApplicantPhoto";
    private static final String FACE = "Face";
    private static final String DATETIME_PATTERN ="yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    @Mock
    CbeffUtil cbeffutil;

    @Test
    public void generateCardTest() throws Exception {
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

        String uin = decryptedCredentialJson.getString("UIN");

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

    @Test
    public void setQrCodeTest() throws QrcodeGenerationException, IOException {

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
    public void testSetApplicantPhoto_NullInput() throws Exception {
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
    public void testSetTemplateAttributes_DemographicIdentityIsNull() {
        Map<String, Object> attribute = new HashMap<>();
        IdentityNotFoundException thrown = assertThrows(
                IdentityNotFoundException.class,
                () -> ReflectionTestUtils.invokeMethod(pdfCardService, "setTemplateAttributes", null, attribute)
        );

        assertEquals(DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorCode(), thrown.getErrorCode());
    }

    @Test
    public void generateUinCardTest() throws IOException {
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
    public void testGenerateUinCard() throws Exception {
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

}
