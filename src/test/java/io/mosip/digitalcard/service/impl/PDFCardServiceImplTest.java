package io.mosip.digitalcard.service.impl;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.extractor.face.sdk.impl.FaceSDKImpl;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.*;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the now-public PDFCardServiceImpl.generateFace() method.
 *
 * Strategy (no Spring context needed):
 *  - OpenCV loads via PDFCardServiceImpl's static initialiser.
 *  - iBioApi  → real FaceSDKImpl (MTCNN → YuNet → RetinaFace cascade)
 *  - cbeffutil → lightweight inline stub that returns the pre-built BIR directly,
 *                avoiding a real CBEFF XML round-trip in unit tests.
 *
 * Input images (must exist in src/test/resources/test-images/input/):
 *   adult-face.jpg   — classic frontal adult portrait
 *   child-face.jpg   — real child photo (Nikon D300, CC0)
 *   burqa-face.jpg   — real niqab photo, only eye slit visible (CC BY 2.0)
 *
 * Output WebP files are saved to src/test/resources/test-images/output/.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PDFCardServiceImplTest {

    private static final String INPUT_DIR  = "src/test/resources/test-images/input";
    private static final String OUTPUT_DIR = "src/test/resources/test-images/output";

    private static PDFCardServiceImpl service;

    // ------------------------------------------------------------------ setup

    @BeforeAll
    static void setup() throws Exception {
        // PDFCardServiceImpl's static block loads OpenCV — that fires on first class load.
        FaceSDKImpl faceSDK = new FaceSDKImpl();
        faceSDK.init(null);

        service = new PDFCardServiceImpl();
        injectField(service, "iBioApi", faceSDK);

        new File(OUTPUT_DIR).mkdirs();
    }

    // ---------------------------------------------------------- helper methods

    /**
     * Injects a value into a (possibly private) field of the target object.
     */
    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Reads a JPEG/PNG and converts it to ISO 19794-5 bytes.
     * purpose="Registration" is required to prevent NPE in FaceEncoder line 78.
     */
    private byte[] toISO(String imagePath) throws Exception {
        byte[] raw = Files.readAllBytes(Paths.get(imagePath));
        ConvertRequestDto dto = new ConvertRequestDto();
        dto.setVersion("ISO19794_5_2011");
        dto.setModality("Face");
        dto.setBiometricSubType("UNKNOWN");
        dto.setPurpose("Registration");
        dto.setInputBytes(raw);
        return FaceEncoder.convertFaceImageToISO(dto);
    }

    /**
     * Builds a BIR from raw ISO bytes.
     * BIRInfo is required — FaceSDKImpl calls getBirInfo().setPayload(...) internally.
     */
    private BIR buildBIR(byte[] isoBytes) {
        BDBInfo bdbInfo = new BDBInfo.BDBInfoBuilder()
                .withType(Collections.singletonList(BiometricType.FACE))
                .withSubtype(new ArrayList<>())
                .build();
        BIRInfo birInfo = new BIRInfo.BIRInfoBuilder().build();
        return new BIR.BIRBuilder()
                .withBdb(isoBytes)
                .withBdbInfo(bdbInfo)
                .withBirInfo(birInfo)
                .build();
    }

    /**
     * Injects a CbeffUtil stub that always returns the supplied BIR.
     *
     * generateFace() calls  cbeffutil.getBIRDataFromXML(Base64.decode(cbeffString))
     * so the stub intercepts that call and hands back our pre-built BIR, bypassing
     * a real CBEFF XML round-trip in this unit test.
     */
    private void stubCbeff(BIR bir) throws Exception {
        CbeffUtil stub = new CbeffUtil() {
            @Override
            public List<BIR> getBIRDataFromXML(byte[] xmlBytes) {
                return Collections.singletonList(bir);
            }
            @Override public byte[] createXML(List<BIR> l) { throw new UnsupportedOperationException(); }
            @Override public byte[] createXML(List<BIR> l, byte[] b) { throw new UnsupportedOperationException(); }
            @Override public byte[] updateXML(List<BIR> l, byte[] b) { throw new UnsupportedOperationException(); }
            @Override public boolean validateXML(byte[] a, byte[] b) { throw new UnsupportedOperationException(); }
            @Override public boolean validateXML(byte[] b) { throw new UnsupportedOperationException(); }
            @Override public Map<String, String> getBDBBasedOnType(byte[] b, String s1, String s2) { throw new UnsupportedOperationException(); }
            @Override public Map<String, String> getAllBDBData(byte[] b, String s1, String s2) { throw new UnsupportedOperationException(); }
            @Override public List<BIR> getBIRDataFromXMLType(byte[] b, String s) { throw new UnsupportedOperationException(); }
        };
        injectField(service, "cbeffutil", stub);
    }

    /**
     * Returns a dummy base64 CBEFF string. The value is irrelevant because
     * the CbeffUtil stub intercepts getBIRDataFromXML() and returns our BIR directly.
     */
    private String dummyCbeffBase64() {
        return Base64.encodeBase64String("stub-cbeff-xml".getBytes());
    }

    /** Skips the test if the required image file is absent. */
    private void assumeFileExists(String path, String desc) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                new File(path).exists(),
                "Skipping — required image not found: " + path + " (" + desc + ")");
    }

    // ------------------------------------------------------------------ tests

    /**
     * TC-01  Adult face — baseline.
     * Verifies generateFace() returns a non-empty Base64 WebP string for a
     * standard frontal adult portrait.
     */
    @Test
    @Order(1)
    void tc01_generateFace_adultFace() throws Exception {
        String inputPath = INPUT_DIR + "/adult-face.jpg";
        assumeFileExists(inputPath, "adult-face.jpg");

        byte[] isoBytes = toISO(inputPath);
        stubCbeff(buildBIR(isoBytes));

        String result = service.generateFace(dummyCbeffBase64(), "TEST-RID-ADULT-001");

        assertNotNull(result, "generateFace() must not return null for adult face");
        assertFalse(result.isEmpty(), "generateFace() must not return empty string");

        byte[] webpBytes = Base64.decodeBase64(result);
        assertTrue(webpBytes.length > 100,
                "Decoded WebP must be > 100 bytes, got: " + webpBytes.length);

        Files.write(Paths.get(OUTPUT_DIR, "pdfservice-tc01-adult.webp"), webpBytes);
        System.out.println("TC-01 PASSED | generateFace() adult face"
                + " | base64 length: " + result.length()
                + " | webp bytes: " + webpBytes.length);
    }

    /**
     * TC-02  Child face.
     * Real photograph from Wikimedia Commons (CC0).
     * MTCNN detector handles child faces; verifies the full pipeline works.
     */
    @Test
    @Order(2)
    void tc02_generateFace_childFace() throws Exception {
        String inputPath = INPUT_DIR + "/child-face.jpg";
        assumeFileExists(inputPath, "child-face.jpg");

        byte[] isoBytes = toISO(inputPath);
        stubCbeff(buildBIR(isoBytes));

        String result = service.generateFace(dummyCbeffBase64(), "TEST-RID-CHILD-001");

        assertNotNull(result, "generateFace() must not return null for child face");
        assertFalse(result.isEmpty());

        byte[] webpBytes = Base64.decodeBase64(result);
        assertTrue(webpBytes.length > 100,
                "Decoded WebP must be > 100 bytes, got: " + webpBytes.length);

        Files.write(Paths.get(OUTPUT_DIR, "pdfservice-tc02-child.webp"), webpBytes);
        System.out.println("TC-02 PASSED | generateFace() child face"
                + " | base64 length: " + result.length()
                + " | webp bytes: " + webpBytes.length);
    }

    /**
     * TC-03  Burqa / niqab face (only eye slit visible).
     * Real photograph from Wikimedia Commons (CC BY 2.0).
     * MTCNN and YuNet both miss it; RetinaFace picks it up via the fallback chain.
     */
    @Test
    @Order(3)
    void tc03_generateFace_burqaFace() throws Exception {
        String inputPath = INPUT_DIR + "/burqa-face.jpg";
        assumeFileExists(inputPath, "burqa-face.jpg");

        byte[] isoBytes = toISO(inputPath);
        stubCbeff(buildBIR(isoBytes));

        String result = service.generateFace(dummyCbeffBase64(), "TEST-RID-BURQA-001");

        // RetinaFace fallback should extract the face even with partial occlusion
        assertNotNull(result,
                "generateFace() must not return null for burqa face — RetinaFace fallback should fire");
        assertFalse(result.isEmpty());

        byte[] webpBytes = Base64.decodeBase64(result);
        assertTrue(webpBytes.length > 100,
                "Decoded WebP must be > 100 bytes, got: " + webpBytes.length);

        Files.write(Paths.get(OUTPUT_DIR, "pdfservice-tc03-burqa.webp"), webpBytes);
        System.out.println("TC-03 PASSED | generateFace() burqa/niqab face (RetinaFace fallback)"
                + " | base64 length: " + result.length()
                + " | webp bytes: " + webpBytes.length);
    }
}
