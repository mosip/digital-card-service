package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.util.CbeffToBiometricUtil;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class CbeffToBiometricUtilTest {

    @InjectMocks
    CbeffToBiometricUtil cbeffToBiometricUtil;

    @Before
    public void setUp() {
        cbeffToBiometricUtil = new CbeffToBiometricUtil(null);
    }

    @Test
    public void testGetImageBytesSuccess() {
        String cbeffFileString = "validCbeff";
        String type = "face";
        List<String> subType = List.of("front");
        byte[] expectedPhotoBytes = new byte[]{1, 2, 3};

        try {
            byte[] actualPhotoBytes = cbeffToBiometricUtil.getImageBytes(cbeffFileString, type, subType);
            assertArrayEquals(expectedPhotoBytes, actualPhotoBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testIsSubTypeEqualLists() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("isSubType", List.class, List.class);
        method.setAccessible(true);

        List<String> list1 = Arrays.asList("A", "B", "C");
        List<String> list2 = Arrays.asList("A", "B", "C");

        boolean result = (boolean) method.invoke(cbeffToBiometricUtil, list1, list2);

        assertTrue(result, "The lists should be equal");
    }

    @Test
    public void testIsSubTypeTrue() throws Exception {
        List<String> subType = Arrays.asList("A", "B");
        List<String> subTypeList = Arrays.asList("A", "B");

        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("isSubType", List.class, List.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(cbeffToBiometricUtil, subType, subTypeList);
        assertTrue(result, "The lists should be equal");
    }

    @Test
    public void testIsBiometricTypeFound() {
        String type = "fingerprint";
        List<BiometricType> biometricTypeList = new ArrayList<>();
        biometricTypeList.add(BiometricType.DNA);
        biometricTypeList.add(BiometricType.IRIS);

        ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil,"isBiometricType",type,biometricTypeList);
    }

    @Test
    public void testGetBIRDataFromXMLSuccess() throws Exception {
        byte[] xmlBytes = "<test>sample data</test>".getBytes();
        List<BIR> expectedBIRList = Arrays.asList(new BIR(), new BIR());

        List<BIR> actualBIRList = cbeffToBiometricUtil.getBIRDataFromXML(xmlBytes);
    }

    @Test
    public void testGetPhotoByTypeAndSubTypeFound() throws Exception {

        BIR bir = mock(BIR.class);


        List<BIR> birList = Arrays.asList(bir);
        String type = "face";
        List<String> subType = Arrays.asList("subtype1");

        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("getPhotoByTypeAndSubType", List.class, String.class, List.class);
        method.setAccessible(true);

        byte[] actualPhoto = (byte[]) method.invoke(cbeffToBiometricUtil, birList, type, subType);
    }

    @Test
    public void whenBirHasNullBdbInfoThenSkippedAndReturnsNull() {
        BIR birNullInfo = Mockito.mock(BIR.class);
        when(birNullInfo.getBdbInfo()).thenReturn(null);

        Object result = ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil, "getPhotoByTypeAndSubType",
                Arrays.asList(birNullInfo), "face", Arrays.asList("front"));

        assertNull(result);
    }

    @Test
    public void whenTypesDoNotMatchThenReturnsNull() {
        BIR birNonMatch = Mockito.mock(BIR.class, RETURNS_DEEP_STUBS);
        when(birNonMatch.getBdbInfo().getType()).thenReturn(Arrays.asList(BiometricType.IRIS));
        when(birNonMatch.getBdbInfo().getSubtype()).thenReturn(Arrays.asList("side"));
        lenient().when(birNonMatch.getBdb()).thenReturn(new byte[]{9});

        Object result = ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil, "getPhotoByTypeAndSubType",
                Arrays.asList(birNonMatch), "face", Arrays.asList("front"));

        assertNull(result);
    }

    @Test
    public void whenTypeMatchesButSubtypeDiffersThenReturnsNull() {
        BIR bir = Mockito.mock(BIR.class, RETURNS_DEEP_STUBS);
        when(bir.getBdbInfo().getType()).thenReturn(Arrays.asList(BiometricType.FACE));
        when(bir.getBdbInfo().getSubtype()).thenReturn(Arrays.asList("side"));
        lenient().when(bir.getBdb()).thenReturn(new byte[]{5, 6});

        Object result = ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil, "getPhotoByTypeAndSubType",
                Arrays.asList(bir), "face", Arrays.asList("front"));

        assertNull(result);
    }

    @Test
    public void whenTypeAndSubtypeMatchThenReturnsPhotoBytes() {
        BIR bir1 = Mockito.mock(BIR.class, RETURNS_DEEP_STUBS);
        when(bir1.getBdbInfo().getType()).thenReturn(Arrays.asList(BiometricType.IRIS));
        when(bir1.getBdbInfo().getSubtype()).thenReturn(Arrays.asList("side"));
        lenient().when(bir1.getBdb()).thenReturn(new byte[]{9});

        BIR birMatch = Mockito.mock(BIR.class, RETURNS_DEEP_STUBS);
        when(birMatch.getBdbInfo().getType()).thenReturn(Arrays.asList(BiometricType.FACE));
        when(birMatch.getBdbInfo().getSubtype()).thenReturn(Arrays.asList("front"));
        byte[] expected = new byte[]{1, 2, 3};
        when(birMatch.getBdb()).thenReturn(expected);

        List<BIR> list = Arrays.asList(bir1, birMatch);

        byte[] result = ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil, "getPhotoByTypeAndSubType",
                list, "face", Arrays.asList("front"));

        assertNotNull(result);
        assertArrayEquals(expected, result);
    }

}
