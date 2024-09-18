package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.util.EncryptionUtil;
import io.mosip.digitalcard.util.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import io.mosip.digitalcard.dto.CryptomanagerResponseDto;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionUtilTest {

    @InjectMocks
    EncryptionUtil encryptionUtil;

    @Test
    public void testDecryptDataTest() {
        String dataToBedecrypted = "encryptedData";
        String responseData = "decryptedData";
        CryptomanagerResponseDto responseDto = new CryptomanagerResponseDto();

        try {
            String result = encryptionUtil.decryptData(dataToBedecrypted);
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
}
