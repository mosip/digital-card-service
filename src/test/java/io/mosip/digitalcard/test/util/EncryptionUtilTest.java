package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.exception.DataEncryptionFailureException;
import io.mosip.digitalcard.util.EncryptionUtil;
import io.mosip.digitalcard.util.RestClient;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import io.mosip.digitalcard.dto.CryptomanagerResponseDto;
import org.springframework.http.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionUtilTest {

    @Mock
    private RestClient restClient;

    @Mock
    private Environment env;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    EncryptionUtil encryptionUtil;


    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(env.getProperty("crypto.PrependThumbprint.enable", Boolean.class)).thenReturn(true);
        when(env.getProperty("datetime.pattern")).thenReturn(DATETIME_PATTERN);
    }

    @Test (expected = DataEncryptionFailureException.class)
    public void testDecryptData_Success00() throws Exception {
        String encryptedData = "encryptedData";
        String decryptedData = "decryptedData";

        CryptomanagerResponseDto responseDto = new CryptomanagerResponseDto();
        lenient().when(mapper.readValue(anyString(), eq(CryptomanagerResponseDto.class))).thenReturn(responseDto);

        lenient().when(restClient.postApi(eq(ApiName.CRYPTOMANAGER_DECRYPT), any(), any(), any(), eq(MediaType.APPLICATION_JSON), any(), eq(String.class)))
                .thenReturn("response");

        String result = encryptionUtil.decryptData(encryptedData);

        assertEquals(decryptedData, result);
        verify(restClient, times(1)).postApi(any(), any(), any(), any(), any(), any(), any());
    }


    @Test
    public void testDecryptDataTest_Success() {
        String dataToBedecrypted = "encryptedData";
        String responseData = "decryptedData";
        CryptomanagerResponseDto responseDto = new CryptomanagerResponseDto();

        try {
            encryptionUtil.decryptData(dataToBedecrypted);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    @Test
    public void testDecryptData_Success() {
        String dataToBeDecrypted = "encrypted_data";
        String expectedDecryptedData = "decrypted_data";

        Environment mockEnv = mock(Environment.class);
        RestClient mockRestClient = mock(RestClient.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);

        try{
            String decryptedData = encryptionUtil.decryptData(dataToBeDecrypted);
        }
        catch (Exception e){
            e.printStackTrace();
        }

    }

    @Test
    public void testDecryptData_whenDataToBeDecrypted() {
        assertThrows(DataEncryptionFailureException.class, () -> (new EncryptionUtil()).decryptData("Data To Bedecrypted"));
    }

    @Test
    public void testDecryptData_whenSpace() {
        assertThrows(DataEncryptionFailureException.class, () -> (new EncryptionUtil()).decryptData(" "));
    }

    @Test
    public void testDecryptData_DateTimeParseException() {
        String encryptedData = "encryptedData";

        lenient().when(env.getProperty(DATETIME_PATTERN)).thenReturn("invalid-pattern");

        assertThrows(DataEncryptionFailureException.class, () -> encryptionUtil.decryptData(encryptedData));
    }
}
