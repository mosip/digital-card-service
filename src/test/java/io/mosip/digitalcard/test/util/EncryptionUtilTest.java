package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.CryptomanagerResponseDto;
import io.mosip.digitalcard.dto.DecryptResponseDto;
import io.mosip.digitalcard.exception.ApiNotAccessibleException;
import io.mosip.digitalcard.exception.DataEncryptionFailureException;
import io.mosip.digitalcard.util.EncryptionUtil;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.kernel.core.exception.ServiceError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionUtilTest {

    @Mock
    private RestClient restClient;

    @Mock
    private Environment env;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private EncryptionUtil encryptionUtil;

    private static final String DATETIME_PATTERN_KEY = "mosip.digitalcard.service.datetime.pattern";
    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(env.getProperty("crypto.PrependThumbprint.enable", Boolean.class)).thenReturn(true);
        when(env.getProperty(DATETIME_PATTERN_KEY)).thenReturn(DATETIME_PATTERN);
    }

    @Test
    public void shouldDecryptDataOnSuccessfulResponse() throws Exception {
        String plain = "hello-world";
        String b64 = java.util.Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));

        CryptomanagerResponseDto responseDto = new CryptomanagerResponseDto();
        responseDto.setResponse(new DecryptResponseDto(b64));

        when(restClient.postApi(eq(ApiName.CRYPTOMANAGER_DECRYPT), any(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(String.class)))
                .thenReturn("ignored-raw-json");
        when(mapper.readValue(anyString(), eq(CryptomanagerResponseDto.class))).thenReturn(responseDto);

        String result = encryptionUtil.decryptData("ciphertext");
        assertEquals(plain, result);
        verify(restClient, times(1)).postApi(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    public void shouldThrowDataEncryptionFailureWhenCryptoManagerErrors() throws Exception {
        CryptomanagerResponseDto responseDto = new CryptomanagerResponseDto();
        List<ServiceError> errs = new ArrayList<>();
        errs.add(new ServiceError("code", "bad things"));
        responseDto.setErrors(errs);

        when(restClient.postApi(eq(ApiName.CRYPTOMANAGER_DECRYPT), any(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(String.class)))
            .thenReturn("ignored-raw-json");
        when(mapper.readValue(anyString(), eq(CryptomanagerResponseDto.class))).thenReturn(responseDto);

        assertThrows(DataEncryptionFailureException.class, () -> encryptionUtil.decryptData("ciphertext"));
    }

    @Test
    public void shouldMapHttpClientErrorToApiNotAccessible() throws Exception {
        when(restClient.postApi(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException(new HttpClientErrorException(
                HttpStatus.BAD_REQUEST, "bad", "client-error-body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        assertThrows(ApiNotAccessibleException.class, () -> encryptionUtil.decryptData("ciphertext"));
    }

    @Test
    public void shouldMapHttpServerErrorToApiNotAccessible() throws Exception {
        when(restClient.postApi(any(), any(), any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException(new HttpServerErrorException(
                HttpStatus.INTERNAL_SERVER_ERROR, "srv", "server-error-body".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8)));

        assertThrows(ApiNotAccessibleException.class, () -> encryptionUtil.decryptData("ciphertext"));
    }

    @Test
    public void shouldThrowOnInvalidDatetimePattern() {
        when(env.getProperty(DATETIME_PATTERN_KEY)).thenReturn("invalid");
        assertThrows(DataEncryptionFailureException.class, () -> encryptionUtil.decryptData("ciphertext"));
    }

}
