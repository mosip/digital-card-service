package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.dto.DataShareDto;
import io.mosip.digitalcard.dto.DataShareResponseDto;
import io.mosip.digitalcard.util.DataShareUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

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
