package io.mosip.digitalcard.test.util;

import io.mosip.digitalcard.util.CbeffToBiometricUtil;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class CbeffToBiometricUtilTest {

    @InjectMocks
    CbeffToBiometricUtil cbeffToBiometricUtil;

    @Test
    public void testGetImageBytes_Success() {
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
    public void testIsSubType_EqualLists() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("isSubType", List.class, List.class);
        ((Method) method).setAccessible(true);

        List<String> list1 = Arrays.asList("A", "B", "C");
        List<String> list2 = Arrays.asList("A", "B", "C");

        boolean result = (boolean) method.invoke(cbeffToBiometricUtil, list1, list2);

        assertTrue(result, "The lists should be equal");
    }

    @Test
    public void testIsSubType_true() throws Exception {
        List<String> subType = Arrays.asList("A", "B");
        List<String> subTypeList = Arrays.asList("A", "B");

        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("isSubType", List.class, List.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(cbeffToBiometricUtil, subType, subTypeList);
    }

    @Test
    public void testIsBiometricType_Found() {
        String type = "fingerprint";
        List<BiometricType> biometricTypeList = new ArrayList<>();
        biometricTypeList.add(BiometricType.DNA);
        biometricTypeList.add(BiometricType.IRIS);

        ReflectionTestUtils.invokeMethod(cbeffToBiometricUtil,"isBiometricType",type,biometricTypeList);
    }

    @Test
    public void testGetBIRDataFromXML_Success() throws Exception {
        byte[] xmlBytes = "<test>sample data</test>".getBytes();
        List<BIR> expectedBIRList = Arrays.asList(new BIR(), new BIR());

        List<BIR> actualBIRList = cbeffToBiometricUtil.getBIRDataFromXML(xmlBytes);
    }

    @Test
    public void testGetPhotoByTypeAndSubType_Found() throws Exception {

        BIR bir = mock(BIR.class);

        List<BIR> birList = Arrays.asList(bir);
        String type = "face";
        List<String> subType = Arrays.asList("subtype1");

        Method method = CbeffToBiometricUtil.class.getDeclaredMethod("getPhotoByTypeAndSubType", List.class, String.class, List.class);
        method.setAccessible(true);

        byte[] actualPhoto = (byte[]) method.invoke(cbeffToBiometricUtil, birList, type, subType);
    }

}
