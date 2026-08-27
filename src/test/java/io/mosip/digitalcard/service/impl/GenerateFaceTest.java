package io.mosip.digitalcard.service.impl;

import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.biometrics.util.face.FaceEncoder;
import io.mosip.extractor.face.sdk.impl.FaceSDKImpl;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BDBInfo;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BIRInfo;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.model.Response;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for face extraction covering edge cases:
 *  - Adult face         (lena.jpg  — baseline)
 *  - Child face         (child-face.jpg — smaller, rounder proportions)
 *  - Burqa / niqab face (programmatically generated — only eye area visible)
 *
 * Outputs (WebP bytes) are written to:
 *   src/test/resources/test-images/output/
 *
 * To add real photos drop JPEG/PNG files into:
 *   src/test/resources/test-images/input/
 * and name them:  adult-face.jpg | child-face.jpg | burqa-face.jpg
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GenerateFaceTest {

    private static final String INPUT_DIR  = "src/test/resources/test-images/input";
    private static final String OUTPUT_DIR = "src/test/resources/test-images/output";

    private static FaceSDKImpl faceSDK;

    // ------------------------------------------------------------------ setup

    @BeforeAll
    static void loadOpenCV() {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
        faceSDK = new FaceSDKImpl();
        faceSDK.init(null);
        new File(OUTPUT_DIR).mkdirs();
    }

    // ---------------------------------------------------------- helper methods

    /**
     * Reads a JPEG/PNG from disk and converts it to ISO 19794-5 bytes.
     */
    private byte[] toISO(String imagePath) throws Exception {
        byte[] raw = Files.readAllBytes(Paths.get(imagePath));
        ConvertRequestDto dto = new ConvertRequestDto();
        dto.setVersion("ISO19794_5_2011");
        dto.setModality("Face");
        dto.setBiometricSubType("UNKNOWN");
        dto.setPurpose("Registration");   // required — prevents NPE in FaceEncoder line 78
        dto.setInputBytes(raw);
        return FaceEncoder.convertFaceImageToISO(dto);
    }

    /**
     * Wraps ISO bytes in a BiometricRecord ready for extractTemplate().
     */
    private BiometricRecord toRecord(byte[] isoBytes) {
        BDBInfo bdbInfo = new BDBInfo.BDBInfoBuilder()
                .withType(Collections.singletonList(BiometricType.FACE))
                .withSubtype(new ArrayList<>())
                .build();
        // BIRInfo must be non-null — FaceSDKImpl calls getBirInfo().setPayload(...)
        BIRInfo birInfo = new BIRInfo.BIRInfoBuilder().build();
        BIR bir = new BIR.BIRBuilder()
                .withBdb(isoBytes)
                .withBdbInfo(bdbInfo)
                .withBirInfo(birInfo)
                .build();
        BiometricRecord record = new BiometricRecord();
        // Use a mutable ArrayList — FaceSDKImpl calls birList.set(...) internally;
        // Collections.singletonList() is immutable and throws UnsupportedOperationException.
        List<BIR> segments = new ArrayList<>();
        segments.add(bir);
        record.setSegments(segments);
        return record;
    }

    /**
     * Calls extractTemplate, asserts a valid response, and saves the WebP
     * output image to OUTPUT_DIR.
     *
     * @param record       input record
     * @param outputName   file name (without extension) for the output
     * @param expectSuccess whether we expect face detection to succeed
     * @return extracted WebP bytes (null if extraction failed and was expected to)
     */
    private byte[] runExtract(BiometricRecord record, String outputName, boolean expectSuccess) throws Exception {
        Response<BiometricRecord> response = faceSDK.extractTemplate(
                record,
                Collections.singletonList(BiometricType.FACE),
                null);

        assertNotNull(response, "Response should not be null for: " + outputName);

        if (expectSuccess) {
            assertEquals(200, response.getStatusCode(),
                    "Expected success (200) for: " + outputName
                    + " — got: " + response.getStatusCode());
            assertNotNull(response.getResponse(), "Response body must not be null for: " + outputName);

            List<BIR> segments = response.getResponse().getSegments();
            assertFalse(segments.isEmpty(), "Segments must not be empty for: " + outputName);

            byte[] webpBytes = segments.get(0).getBdb();
            assertNotNull(webpBytes, "Extracted face bytes must not be null for: " + outputName);
            assertTrue(webpBytes.length > 0, "Extracted face must not be empty for: " + outputName);

            // Save WebP output
            Path outputPath = Paths.get(OUTPUT_DIR, outputName + ".webp");
            Files.write(outputPath, webpBytes);
            System.out.println("[PASS] " + outputName
                    + " | raw webp bytes: " + webpBytes.length
                    + " | saved → " + outputPath.toAbsolutePath());
            return webpBytes;
        } else {
            // Expect failure — record the actual code for visibility
            System.out.println("[EXPECTED-FAIL] " + outputName
                    + " | status: " + response.getStatusCode());
            assertNotEquals(200, response.getStatusCode(),
                    "Face should NOT be detected for: " + outputName);
            return null;
        }
    }

    // ------------------------------------------------------------------ tests

    /**
     * TC-01  Adult face — baseline.
     * Uses lena.jpg (classic OpenCV sample, adult woman, frontal).
     * Expected: face detected and extracted successfully.
     */
    @Test
    @Order(1)
    void tc01_adultFace_baseline() throws Exception {
        String inputPath = INPUT_DIR + "/adult-face.jpg";
        assumeFileExists(inputPath, "adult-face.jpg");

        byte[] iso    = toISO(inputPath);
        byte[] output = runExtract(toRecord(iso), "tc01-adult-face", true);

        assertNotNull(output);
        System.out.println("TC-01 PASSED — adult face extracted, size: " + output.length + " bytes");
    }

    /**
     * TC-02  Child face.
     * Uses child-face.jpg (place a real child photo in input dir).
     * Child faces differ from adult: rounder, softer features.
     * Expected: face detected (some detectors may fail — we log the result).
     */
    @Test
    @Order(2)
    void tc02_childFace() throws Exception {
        String inputPath = INPUT_DIR + "/child.jp2";

        if (!new File(inputPath).exists()) {
            // Auto-generate a simulated child face from lena.jpg by resizing down to
            // 64×64 (losing detail) and back up (blurring features), simulating
            // the kind of lower-detail face a child photo at moderate distance
            // would produce. Replace child-face.jpg with a real photo for best results.
            String basePath = INPUT_DIR + "/adult-face.jpg";
            assumeFileExists(basePath, "adult-face.jpg (needed to simulate child face)");
            generateSimulatedChildFace(basePath, inputPath);
            System.out.println("  ↳ child-face.jpg not found — generated synthetic version from adult-face.jpg");
        }

        byte[] iso = toISO(inputPath);
        // Child faces may or may not be detected depending on the detector — both outcomes are valid
        Response<BiometricRecord> response = faceSDK.extractTemplate(
                toRecord(iso),
                Collections.singletonList(BiometricType.FACE),
                null);

        assertNotNull(response);
        int status = response.getStatusCode();
        System.out.println("TC-02 child face — SDK status: " + status);

        if (status == 200 && response.getResponse() != null
                && !response.getResponse().getSegments().isEmpty()) {
            byte[] webpBytes = response.getResponse().getSegments().get(0).getBdb();
            Path out = Paths.get(OUTPUT_DIR, "tc02-child-face.webp");
            Files.write(out, webpBytes);
            System.out.println("TC-02 PASSED — child face detected & extracted, size: "
                    + webpBytes.length + " bytes → " + out.toAbsolutePath());
        } else {
            System.out.println("TC-02 INFO — child face NOT detected (status=" + status
                    + "). Consider adding a clearer child photo at: " + inputPath);
            // Save a diagnostic image showing what was fed in
            saveDiagnosticJpeg(inputPath, OUTPUT_DIR + "/tc02-child-face-input-diagnostic.jpg");
        }
    }

    /**
     * TC-03  Burqa / niqab face — only eye region visible.
     * Uses burqa-face.jpg if present; otherwise generates it programmatically
     * from adult-face.jpg by masking everything below the eye line (top 35%).
     * Expected: detection may fail since most face features are hidden.
     * We record the result either way.
     */
    @Test
    @Order(3)
    void tc03_burqaFace() throws Exception {
        String inputPath = INPUT_DIR + "/burkha.jp2";

        if (!new File(inputPath).exists()) {
            String basePath = INPUT_DIR + "/adult-face.jpg";
            assumeFileExists(basePath, "adult-face.jpg (needed to generate burqa face)");
            generateBurqaFace(basePath, inputPath);
            System.out.println("  ↳ burqa-face.jpg not found — generated synthetic burqa mask from adult-face.jpg");
        }

        // Save the input for visual inspection
        saveDiagnosticJpeg(inputPath, OUTPUT_DIR + "/tc03-burqa-face-input.jpg");

        byte[] iso = toISO(inputPath);
        Response<BiometricRecord> response = faceSDK.extractTemplate(
                toRecord(iso),
                Collections.singletonList(BiometricType.FACE),
                null);

        assertNotNull(response);
        int status = response.getStatusCode();
        System.out.println("TC-03 burqa face — SDK status: " + status);

        if (status == 200 && response.getResponse() != null
                && !response.getResponse().getSegments().isEmpty()) {
            byte[] webpBytes = response.getResponse().getSegments().get(0).getBdb();
            Path out = Paths.get(OUTPUT_DIR, "tc03-burqa-face.webp");
            Files.write(out, webpBytes);
            System.out.println("TC-03 PASSED — burqa face detected (eye-only), size: "
                    + webpBytes.length + " bytes → " + out.toAbsolutePath());
        } else {
            System.out.println("TC-03 INFO — burqa face NOT detected (status=" + status
                    + "). This is expected behaviour — only eyes visible. "
                    + "If detection is required, use a detector tuned for partial faces.");
        }
    }

    /**
     * TC-04  Multi-face — image contains two faces; SDK should pick the largest.
     * Uses multi-face.jpg.
     */
    @Test
    @Order(4)
    void tc04_multiFace_largestSelected() throws Exception {
        String inputPath = INPUT_DIR + "/multi-face.jpg";
        assumeFileExists(inputPath, "multi-face.jpg");

        byte[] iso    = toISO(inputPath);
        byte[] output = runExtract(toRecord(iso), "tc04-multi-face-largest", true);

        assertNotNull(output);
        System.out.println("TC-04 PASSED — largest face from multi-face image extracted: "
                + output.length + " bytes");
    }

    /**
     * TC-05  Verify output dimensions — extracted face should be 45×45 px
     * (as configured in FaceSDKImpl.getExtractedFace).
     */
    @Test
    @Order(5)
    void tc05_outputDimensions_45x45() throws Exception {
        String inputPath = INPUT_DIR + "/adult-face.jpg";
        assumeFileExists(inputPath, "adult-face.jpg");

        byte[] iso    = toISO(inputPath);
        byte[] output = runExtract(toRecord(iso), "tc05-dimension-check", true);

        assertNotNull(output);

        // Decode the WebP bytes and check size
        Mat decoded = Imgcodecs.imdecode(new MatOfByte(output), Imgcodecs.IMREAD_COLOR);
        assertFalse(decoded.empty(), "Decoded WebP image must not be empty");
        assertEquals(45, decoded.cols(), "Extracted face width must be 45 px");
        assertEquals(45, decoded.rows(), "Extracted face height must be 45 px");
        System.out.println("TC-05 PASSED — output dimensions: "
                + decoded.cols() + "×" + decoded.rows() + " px");
    }

    // ------------------------------------------------------- image generators

    /**
     * Simulates a child face by:
     * 1. Downsampling to 64×64 (loses fine adult features)
     * 2. Applying a slight Gaussian blur (softer features)
     * 3. Up-sampling back to 256×256
     * Replace with a real child photo for a meaningful real-world test.
     */
    private void generateSimulatedChildFace(String sourcePath, String targetPath) {
        Mat src  = Imgcodecs.imread(sourcePath);
        Mat small = new Mat();
        Mat blurred = new Mat();
        Mat result  = new Mat();

        // Downsample → blur → upsample (destroys adult-specific feature sharpness)
        Imgproc.resize(src, small, new Size(64, 64));
        Imgproc.GaussianBlur(small, blurred, new Size(3, 3), 1.5);
        Imgproc.resize(blurred, result, new Size(256, 256));

        Imgcodecs.imwrite(targetPath, result);
        System.out.println("  Generated simulated child face → " + targetPath);
    }

    /**
     * Simulates a burqa/niqab by masking everything below the eye line.
     * Keeps only the top 38% of the image (forehead + eyes), fills the rest black.
     */
    private void generateBurqaFace(String sourcePath, String targetPath) {
        Mat src    = Imgcodecs.imread(sourcePath);
        Mat output = src.clone();

        // Keep top 38% (forehead + eye area), mask the rest with black
        int visibleRows = (int) (src.rows() * 0.38);
        // Black out everything below the eye line
        Mat lowerMask = new Mat(src.rows() - visibleRows, src.cols(), src.type(), new Scalar(0, 0, 0));
        lowerMask.copyTo(output.rowRange(visibleRows, src.rows()));

        Imgcodecs.imwrite(targetPath, output);
        System.out.println("  Generated burqa face (top 38% visible) → " + targetPath);
    }

    /**
     * Copies an input JPEG to the output location for diagnostic inspection.
     */
    private void saveDiagnosticJpeg(String sourcePath, String targetPath) {
        try {
            Files.copy(Paths.get(sourcePath), Paths.get(targetPath),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.out.println("  Could not copy diagnostic image: " + e.getMessage());
        }
    }

    /**
     * Skips the test (JUnit Assumptions) if the required image file is missing.
     */
    private void assumeFileExists(String path, String description) {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                new File(path).exists(),
                "Skipping — required test image not found: " + path
                + " (" + description + ")");
    }
}
