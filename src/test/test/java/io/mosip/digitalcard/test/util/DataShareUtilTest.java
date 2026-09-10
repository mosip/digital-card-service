package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.dto.DataShareDto;
import io.mosip.digitalcard.dto.DataShareResponseDto;
import io.mosip.digitalcard.util.DataShareUtil;
import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.RestClient;
import io.mosip.kernel.core.logger.spi.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.Environment;

import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DataShareUtilTest {

    @InjectMocks
    DataShareUtil dataShareUtil;

    @Test
    public void testGetDataShareSuccess() throws Exception {
        byte[] data = {1, 2, 3};
        String policyId = "policyId";
        String partnerId = "partnerId";
        DataShareResponseDto mockResponseDto = mock(DataShareResponseDto.class);
        DataShareDto mockDataShareDto = mock(DataShareDto.class);

        DataShareDto result = dataShareUtil.getDataShare(data, "sacdc", "acacad");
    }

}
