package io.mosip.digitalcard.test.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.dto.DataShareDto;
import io.mosip.digitalcard.dto.DataShareResponseDto;
import io.mosip.digitalcard.dto.ErrorDTO;
import io.mosip.digitalcard.util.DataShareUtil;
import io.mosip.digitalcard.util.RestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class DataShareUtilTest {

    @InjectMocks
    DataShareUtil dataShareUtil;

    @Mock
    private RestClient restClient;

    @Mock
    private ObjectMapper mapper;

    @Test
    public void testGetDataShareSuccess() throws Exception {
        byte[] data = {1, 2, 3};
        String policyId = "policyId";
        String partnerId = "partnerId";
        DataShareResponseDto mockResponseDto = mock(DataShareResponseDto.class);
        DataShareDto mockDataShareDto = mock(DataShareDto.class);

        DataShareDto result = dataShareUtil.getDataShare(data, "sacdc", "acacad");
    }

    @Test
    public void testGetDataShareSuccessReturnsDataShareDto() throws Exception {
        byte[] data = {1, 2, 3};
        DataShareDto expectedDto = new DataShareDto();
        DataShareResponseDto responseDto = mock(DataShareResponseDto.class);
        when(restClient.postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class)))
                .thenReturn("any-response-string");
        when(mapper.readValue(eq("any-response-string"), eq(DataShareResponseDto.class)))
                .thenReturn(responseDto);
        when(responseDto.getErrors()).thenReturn(null);
        when(responseDto.getDataShare()).thenReturn(expectedDto);

        DataShareDto result = dataShareUtil.getDataShare(data, "policyId", "partnerId");

        assertNotNull(result);
        assertSame(expectedDto, result);
        verify(restClient, times(1)).postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class));
    }

    @Test
    public void testGetDataShareMapperReturnsNullReturnsNull() throws Exception {
        byte[] data = {1, 2};
        when(restClient.postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class)))
                .thenReturn("resp");
        when(mapper.readValue(eq("resp"), eq(DataShareResponseDto.class)))
                .thenReturn(null);

        DataShareDto result = dataShareUtil.getDataShare(data, "p", "q");

        assertNull(result);
        verify(mapper, times(1)).readValue(eq("resp"), eq(DataShareResponseDto.class));
    }

    @Test
    public void testGetDataShareResponseHasErrorsReturnsNull() throws Exception {
        byte[] data = {9};
        DataShareResponseDto responseDto = mock(DataShareResponseDto.class);
        ErrorDTO error = new ErrorDTO();
        when(restClient.postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class)))
                .thenReturn("r");
        when(mapper.readValue(eq("r"), eq(DataShareResponseDto.class)))
                .thenReturn(responseDto);
        when(responseDto.getErrors()).thenReturn(Collections.singletonList(error));

        DataShareDto result = dataShareUtil.getDataShare(data, "policy", "partner");

        assertNull(result);
    }

    @Test
    public void testGetDataShareRestClientThrowsExceptionCaughtAndReturnsNull() throws Exception {
        byte[] data = {0};
        when(restClient.postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("down"));

        DataShareDto result = dataShareUtil.getDataShare(data, "pp", "qq");

        assertNull(result);
        verify(restClient, times(1)).postApi(any(), anyList(), anyString(), anyString(), any(), any(), eq(String.class));
    }

}
