package io.mosip.digitalcard.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.biometrics.util.ConvertRequestDto;
import io.mosip.biometrics.util.face.FaceDecoder;
import io.mosip.digitalcard.constant.*;
import io.mosip.digitalcard.dto.PDFSignatureRequestDto;
import io.mosip.digitalcard.dto.SignatureResponseDto;
import io.mosip.digitalcard.dto.SimpleType;
import io.mosip.digitalcard.service.CardGeneratorService;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.exception.IdentityNotFoundException;
import io.mosip.digitalcard.exception.ImageVerificationException;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.util.*;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.entities.BiometricRecord;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import io.mosip.kernel.biometrics.spi.IBioApiV2;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.pdfgenerator.spi.PDFGenerator;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
//import io.mosip.vercred.CredentialsVerifier;
import nl.minvws.encoding.Base45;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PDFCardServiceImpl implements CardGeneratorService {


	/** The PDFServiceImpl logger. */
	Logger logger = DigitalCardRepoLogger.getLogger(PDFCardServiceImpl.class);

	private static final String DATETIME_PATTERN = "mosip.digitalcard.service.datetime.pattern";

	/** The Constant FILE_SEPARATOR. */
	public static final String FILE_SEPARATOR = File.separator;

	/** The Constant VALUE. */
	private static final String VALUE = "value";

	/** The Constant FACE. */
	private static final String FACE = "Face";

	/** The Constant APPLICANT_PHOTO. */
	private static final String APPLICANT_PHOTO = "ApplicantPhoto";

	/** The Constant QRCODE. */
	private static final String QRCODE = "QrCode";

	/** The Constant DateIssued. */
	private static final String DATEISSUED = "DateIssued";

	@Autowired
	private RestClient restApiClient;

	/** The pdf generator. */
	@Autowired
	private PDFGenerator pdfGenerator;

	/** The template generator. */
	@Autowired
	private TemplateGenerator templateGenerator;

	/** The utilities. */
	@Autowired
	private Utility utility;

	/** The qr code generator. */
	@Autowired
	private QrCodeGenerator<QrVersion> qrCodeGenerator;

	/** The cbeffutil. */
	@Autowired
	private CbeffUtil cbeffutil;

	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	DigitalCardTransactionRepository digitalCardTransactionRepository;

	//@Autowired
	//private CredentialsVerifier credentialsVerifier;

	@Value("${mosip.template-language}")
	private String templateLang;

	@Value("${mosip.supported-languages}")
	private String supportedLang;

	@Value("${mosip.digitalcard.service.uincard.lowerleftx}")
	private int lowerLeftX;

	@Value("${mosip.digitalcard.service.uincard.lowerlefty}")
	private int lowerLeftY;

	@Value("${mosip.digitalcard.service.uincard.upperrightx}")
	private int upperRightX;

	@Value("${mosip.digitalcard.service.uincard.upperrighty}")
	private int upperRightY;

	@Value("${mosip.digitalcard.service.uincard.signature.reason}")
	private String reason;

	@Value("${mosip.digitalcard.templateTypeCode:RPR_UIN_CARD_TEMPLATE}")
	private String uinCardTemplate;

	@Value("${mosip.kernel.config.server.file.storage.uri}")
	private String configServerFileStorageURL;

	@Value("${mosip.digitalcard.service.qrcode.scanner.version:PH1}")
	private String qrCodeScannerVersion;

	@Value("${mosip.digitalcard.service.qrcode.version:V22}")
	private String qrCodeVersion;

	@Value("${mosip.digitalcard.service.default.image.length:100}")
	private int defaultImgLength;

	@Value("${mosip.digitalcard.service.faceParamScaleFactor:1.01}")
	private String faceParamScaleFactor;

	@Value("${mosip.digitalcard.service.faceParamMinNeighbors:6}")
	private int faceParamMinNeighbors;

	@Value("${mosip.digitalcard.service.faceParamFlags:0}")
	private int faceParamFlags;

	@Value("${mosip.digitalcard.service.faceImage_x_coor:40}")
	private int faceImage_x_coor;

	@Value("${mosip.digitalcard.service.faceImage_y_coor:40}")
	private int faceImage_y_coor;

	@Value("${mosip.digitalcard.service.eyeParamScaleFactor:1.1}")
	private String eyeParamScaleFactor;

	@Value("${mosip.digitalcard.service.eyeParamMinNeighbors:2}")
	private int eyeParamMinNeighbors;

	@Value("${mosip.digitalcard.service.eyeParamFlags:0}")
	private int eyeParamFlags;

	@Value("${mosip.digitalcard.service.eyeImage_x_coor:30}")
	private int eyeImage_x_coor;

	@Value("${mosip.digitalcard.service.eyeImage_y_coor:30}")
	private int eyeImage_y_coor;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private IBioApiV2 iBioApi;

	@Autowired
	private CBORUtil cborUtil;

/*	public PDFCardServiceImpl(){
		iBioApi.init(null);
	}*/

	static {
		// load OpenCV library
		nu.pattern.OpenCV.loadShared();
		System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see io.mosip.digitalcard.service.PDFService#
	 */
	public byte[] generateCard(org.json.JSONObject decryptedCredentialJson, String credentialType,
							   String password, String rid) throws Exception{
		logger.debug("PDFServiceImpl::getDocuments()::entry");
		String uin = null;
		boolean isPhotoSet=false;
		String individualBio = null;
		Map<String, Object> attributes = new LinkedHashMap<>();
		String template = uinCardTemplate;
		byte[] pdfbytes = null;

		attributes.put(DATEISSUED, getDateDisplay(1));

		try {
			if(decryptedCredentialJson.has("biometrics")){
				individualBio = decryptedCredentialJson.getString("biometrics");
				String individualBiometric = new String(individualBio);
				isPhotoSet = setApplicantPhoto(individualBiometric, attributes);
				attributes.put("isPhotoSet",isPhotoSet);
			}
			uin = decryptedCredentialJson.getString("UIN");
			if (credentialType.equalsIgnoreCase("qrcode")) {
				boolean isQRcodeSet = setQrCode(decryptedCredentialJson.toString(), attributes,isPhotoSet,rid,individualBio);
				InputStream uinArtifact = templateGenerator.getTemplate(template, attributes, templateLang);
				pdfbytes = generateUinCard(uinArtifact, password);
			} else {
				if (!isPhotoSet) {
					logger.debug(DigitalCardServiceErrorCodes.APPLICANT_PHOTO_NOT_SET.name());
				}

				setTemplateAttributes(decryptedCredentialJson, attributes);
				attributes.put(IdType.UIN.toString(), uin);
				boolean isQRcodeSet = setQrCode(decryptedCredentialJson.toString(), attributes,isPhotoSet,rid, individualBio);
				if (!isQRcodeSet) {
					logger.debug(DigitalCardServiceErrorCodes.QRCODE_NOT_SET.name());
				}

				// updated attributes value
				String attrPcn = attributes.get("PCN").toString();
				String attrBirthDate = attributes.get("dob").toString();
				String attrFormattedDate = modifyDateLayout(attrBirthDate);
				String attrAddressLine1 = addressReplace(attributes.get("adL1_eng").toString());

				try {
					String attrMaritalStatus = maritalStatusReplace(attributes.get("ms_eng").toString());
					attributes.put("ms_eng", attrMaritalStatus);
				} catch (Exception e2) {
					logger.error("Stat3 for RID : " + rid + ", STATUS: ms_eng is missing.");
					attributes.put("ms_eng", "");
				}

				try {
					String attrBloodType = bloodTypeReplace(attributes.get("bt_eng").toString());
					attributes.put("bt_eng", attrBloodType);
				} catch (Exception e2) {
					logger.error("Stat3 for RID : " + rid + ", STATUS: bt_eng is missing.");
					attributes.put("bt_eng", "UNKNOWN");
				}

				try {
					String attrProvinceRaw = attributes.get("pro_eng").toString().toUpperCase().trim();
					String attrProvince = attrProvinceRaw;

					if(attrProvinceRaw.contains("NCR")){
						attrProvince = "METRO MANILA";
					}
					attributes.put("pro_eng", attrProvince);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					logger.error("Pro_eng is missing. :" + e1.getMessage(), e1);
					throw e1;
				}

				try {
					String pobProvince_eng = attributes.get("pobProvince_eng").toString();
					String cleanProb = naToBlank(pobProvince_eng);
					cleanProb = cleanProb.toUpperCase();
					if(cleanProb.contains("NCR")) {
						cleanProb = "METRO MANILA";
					}
					String pobCountry_eng = attributes.get("pobCountry_eng").toString();
					String cleanCountry = naToBlank(pobCountry_eng);
					String attrPobProvince = "";
					if(cleanProb != "") {
						attrPobProvince = pobStringReplace(cleanProb, cleanCountry);
					} else {
						attrPobProvince = cleanCountry;
					}
					attributes.put("pobProvince_eng", attrPobProvince);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					logger.error("pobProvince_eng is missing" + e1.getMessage());
					throw e1;
				}

				attributes.put("PCN", attrPcn.replaceAll("(.{4})(?!$)", "$1-"));
				attributes.put("dob", attrFormattedDate);
				attributes.put("adL1_eng", attrAddressLine1);

				try {
					String cleanCity = naToBlank(attributes.get("pobCity_eng").toString());
					attributes.put("pobCity_eng", cleanCity);
				} catch (Exception e) {
					logger.error("pobCity_eng is missing from attribute. :" + e.getMessage(), e);
					throw e;
				}

				// getting template and placing original values png
				InputStream uinArtifact = templateGenerator.getTemplate(template, attributes, templateLang);

				if (uinArtifact == null) {
					logger.error(DigitalCardServiceErrorCodes.TEM_PROCESSING_FAILURE.name());
					throw new DigitalCardServiceException(
							DigitalCardServiceErrorCodes.TEM_PROCESSING_FAILURE.getErrorCode(),DigitalCardServiceErrorCodes.TEM_PROCESSING_FAILURE.getErrorMessage());
				}
				pdfbytes = generateUinCard(uinArtifact, password);
				//to be deleted when making Jar
//				InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(pdfbytes));
//                File pdfFile = new File("src/main/resources/uin.pdf");
//                OutputStream os = new FileOutputStream(pdfFile);
//                os.write(pdfbytes);
//                os.close();
			}
		}
		catch (QrcodeGenerationException e) {
			logger.error(DigitalCardServiceErrorCodes.QRCODE_NOT_GENERATED.getErrorMessage(), e);
			throw e;
		}  catch (PDFGeneratorException e) {
			logger.error(DigitalCardServiceErrorCodes.PDF_NOT_GENERATED.getErrorMessage() ,e);
			throw e;
		}catch (JsonParseException | JsonMappingException e) {
			logger.error(DigitalCardServiceErrorCodes.ATTRIBUTE_NOT_SET.getErrorMessage() ,e);
			throw e;
		} catch (ImageVerificationException e){
			logger.error(DigitalCardServiceErrorCodes.IMAGE_VERIFICATION_FAILED.getErrorMessage(),e);
			throw e;
		} catch (Exception e) {
			logger.error(PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorMessage() ,e);
			throw e;
		}
		logger.debug("PDFServiceImpl::getDocuments()::exit");
		return pdfbytes;
	}

	/**
	 * Sets the qr code.
	 *
	 * @param attributes    the attributes
	 * @param individualBio
	 * @return true, if successful
	 * @throws QrcodeGenerationException
	 * @throws Exception
	 */
	private boolean setQrCode(String qrString, Map<String, Object> attributes, boolean isPhotoSet, String rid, String individualBio)
			throws  QrcodeGenerationException, URISyntaxException, ParseException, Exception {
		boolean isQRCodeSet = false;

		JSONObject qrJsonObj = objectMapper.readValue(qrString, JSONObject.class);

		if(isPhotoSet) {
			qrJsonObj.remove("biometrics");
		}

		JSONParser parser = new JSONParser();
		JSONObject attributeObject = (JSONObject) parser.parse(qrString);
		HashMap<String, String> qrList = new HashMap<String, String>();
		attributeObject.remove("biometrics");
		attributeObject.keySet().forEach(keyStr -> {
			if (attributeObject.get(keyStr) instanceof JSONArray) {
				JSONArray keyValue = (JSONArray) attributeObject.get(keyStr);
				String ivValue = "";
				if(keyStr.equals("BF")) {
					ivValue = "[";
					logger.info("BF Value: " + keyStr.equals("BF"));
				}
				for(int i=0; i < keyValue.size(); i++) {
					JSONObject test = (JSONObject) keyValue.get(i);
					if(test.get("value") != null) {
						ivValue = test.get("value").toString();
					} else {
						if(keyStr.equals("BF")) {
							ivValue = ivValue + test.get("rank").toString() + ",";
						}
					}
				}
				if(keyStr.equals("BF")) {
					ivValue = ivValue.substring(0, ivValue.length() - 1);
					ivValue = ivValue + "]";
				}
				qrList.put(keyStr.toString(), ivValue);
			} else if (attributeObject.get(keyStr) instanceof String) {
				String ivValue = (String) attributeObject.get(keyStr);
				qrList.put((String) keyStr, ivValue);
			}
		});

		String compressedBase64 = null;
		if (individualBio != null) {
			compressedBase64 = generateFace(individualBio, rid);
		}
		else {
			compressedBase64 = "";
		}

		String sfx = qrList.getOrDefault("sfx", "");
		String fn = qrList.getOrDefault("fn", "");
		String ln = qrList.getOrDefault("ln", "");
		String mn = qrList.getOrDefault("mn", "");
		String bf = qrList.getOrDefault("BF", "");
		String sx = qrList.getOrDefault("gen", "");
		String dob2 = qrList.getOrDefault("dob", "");
		dob2 = dob2.replace("/", "-");

		String pob = "";
		String pobCity = qrList.getOrDefault("pobCity", "").toUpperCase();
		String pobProvince = qrList.getOrDefault("pobProvince", "").toUpperCase();
		String pobCountry = qrList.getOrDefault("pobCountry", "").toUpperCase();

		if (pobProvince.contains("NCR")) {
			pobProvince = "METRO MANILA";
		}

		if (pobCountry.equalsIgnoreCase("PHILIPPINES") == false) {
			pob = pobCountry;
		} else {
			pob = pobCity + ", " + pobProvince;
		}

		String pcn = qrList.getOrDefault("PCN", "");

		String dIssued = getDateDisplay(2);
		String forQR = "{ \"i\": \"PSA\"," +
				" \"d\": \""+dIssued+"\"," +
				" \"sb\": {   \"sf\": \""+sfx+"\",   " +
				"\"ln\": \""+ln+"\",   "+
				"\"fn\": \""+fn+"\",   "+
				"\"mn\": \""+mn+"\",   "+
				"\"s\": \""+sx+"\",   "+
				"\"BF\": \""+bf+"\",   "+
				"\"DOB\": \""+dob2+"\",   "+
				"\"POB\": \""+pob+"\",   "+
				"\"PCN\": \""+pcn+"\" }"+"}";

		byte[] qrCodeBytes = null;
		byte[] cborData=null;
		try {
			JSONObject json = (JSONObject) parser.parse(forQR);
			json.put("img",Base64.decodeBase64(compressedBase64));
			cborData=cborUtil.encodeToCborAndSign(json);
			logger.info("Stat1 For RID:" + rid+" | CWT:"+cborData.length + " | Encrypted CWT:" + Base45.getEncoder().encodeToString(cborData).length() + " | Image:" + compressedBase64.length()+ " | JSON:" + json.toJSONString().getBytes(StandardCharsets.UTF_8).length);
			verifyImageData(rid,compressedBase64);
			qrCodeBytes = qrCodeGenerator.generateQrCode(qrCodeScannerVersion+":"+Base45.getEncoder().encodeToString(cborData), QrVersion.valueOf(qrCodeVersion));
			logger.info("Stat2 For RID:" + rid+" | QR code:"+qrCodeBytes.length);
			String imageString = Base64.encodeBase64String(qrCodeBytes);
			attributes.put(QRCODE, "data:image/png;base64," + imageString);
			isQRCodeSet = true;
		} catch (QrcodeGenerationException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage(), e);
		}catch (ImageVerificationException e){
			logger.error(e.getMessage(), e);
			throw e;
		}

		return isQRCodeSet;
	}


	private void verifyImageData(String rid, String img) throws ImageVerificationException {
		if(img.length()<=defaultImgLength){
			throw new ImageVerificationException(DigitalCardServiceErrorCodes.IMAGE_VERIFICATION_FAILED.getError()+" | Image size : "+img.length());
		}
	}

	/**
	 * Sets the applicant photo.
	 *
	 *            the response
	 * @param attributes
	 *            the attributes
	 * @return true, if successful
	 * @throws Exception
	 *             the exception
	 */
	private boolean setApplicantPhoto(String individualBio, Map<String, Object> attributes) throws Exception {
		ConvertRequestDto convertRequestDto = new ConvertRequestDto();
		String value = individualBio;
		boolean isPhotoSet = false;

		if (value != null) {
			CbeffToBiometricUtil util = new CbeffToBiometricUtil(cbeffutil);
			List<String> subtype = new ArrayList<>();
			byte[] photoByte = util.getImageBytes(value, FACE, subtype);
			convertRequestDto.setVersion("ISO19794_5_2011");
			convertRequestDto.setInputBytes(photoByte);
			if (photoByte != null) {
				byte[] data = FaceDecoder.convertFaceISOToImageBytes(convertRequestDto);

				logger.info("Byte size after cbef_util : " + data.length);
				String encodedData = StringUtils.newStringUtf8(Base64.encodeBase64(data, false));
				attributes.put(APPLICANT_PHOTO, "data:image/png;base64," + encodedData);
				isPhotoSet = true;
			}
		}
		return isPhotoSet;
	}

	/**
	 * Gets the artifacts.
	 *
	 * @param attribute    the attribute
	 * @return the artifacts
	 * @throws IOException    Signals that an I/O exception has occurred.
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	private void setTemplateAttributes(org.json.JSONObject demographicIdentity, Map<String, Object> attribute)
			throws Exception {
		try {
			if (demographicIdentity == null)
				throw new IdentityNotFoundException(DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorCode(),DigitalCardServiceErrorCodes.IDENTITY_NOT_FOUND.getErrorMessage());

			String mapperJsonString = utility.getIdentityMappingJson(utility.getConfigServerFileStorageURL(),
					utility.getIdentityJson());
			JSONObject mapperJson = objectMapper.readValue(mapperJsonString, JSONObject.class);
			JSONObject mapperIdentity = utility.getJSONObject(mapperJson,
					utility.getDemographicIdentity());

			List<String> mapperJsonKeys = new ArrayList<>(mapperIdentity.keySet());
			for (String key : mapperJsonKeys) {
				LinkedHashMap<String, String> jsonObject = utility.getJSONValue(mapperIdentity, key);
				Object obj = null;
				String values = jsonObject.get(VALUE);
				for (String value : values.split(",")) {
					// Object object = demographicIdentity.get(value);
					Object object = demographicIdentity.has(value)?demographicIdentity.get(value):null;
					if (object != null) {
						try {
							obj = new JSONParser().parse(object.toString());
						} catch (Exception e) {
							obj = object;
						}

						if (obj instanceof JSONArray && !key.equalsIgnoreCase("bestTwoFingers")) {
							// JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
							SimpleType[] jsonValues = Utility.mapJsonNodeToJavaObject(SimpleType.class, (JSONArray) obj);
							for (SimpleType jsonValue : jsonValues) {
								if (supportedLang.contains(jsonValue.getLanguage()))
									attribute.put(value + "_" + jsonValue.getLanguage(), jsonValue.getValue());
							}
						} else if (object instanceof JSONObject) {
							JSONObject json = (JSONObject) object;
							attribute.put(value, (String) json.get(VALUE));
						} else {
							attribute.put(value, String.valueOf(object));
						}
					}

				}
			}
		} catch (JsonParseException | JsonMappingException | DigitalCardServiceException e) {
			logger.error("Error while parsing Json file" ,e);
			throw e;
		}

	}

	private byte[] generateUinCard(InputStream in, String password) {
		logger.debug("UinCardGeneratorImpl::generateUinCard()::entry");
		byte[] pdfSignatured=null;
		ByteArrayOutputStream out = null;
		try {
			out = (ByteArrayOutputStream) pdfGenerator.generate(in);
			PDFSignatureRequestDto request = new PDFSignatureRequestDto(lowerLeftX, lowerLeftY, upperRightX,
					upperRightY, reason, 1, password);
			request.setApplicationId("KERNEL");
			request.setReferenceId("SIGN");
			request.setData(Base64.encodeBase64String(out.toByteArray()));
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);

			request.setTimeStamp(DateUtils.getUTCCurrentDateTimeString());
			RequestWrapper<PDFSignatureRequestDto> requestWrapper = new RequestWrapper<>();

			requestWrapper.setRequest(request);
			requestWrapper.setRequesttime(localdatetime);
			ResponseWrapper<?> responseWrapper;
			SignatureResponseDto signatureResponseDto;

			responseWrapper= restApiClient.postApi(ApiName.PDFSIGN, null, "",""
					, MediaType.APPLICATION_JSON,requestWrapper, ResponseWrapper.class);


			if (responseWrapper.getErrors() != null && !responseWrapper.getErrors().isEmpty()) {
				ServiceError error = responseWrapper.getErrors().get(0);
				throw new DigitalCardServiceException(error.getMessage());
			}
			signatureResponseDto = objectMapper.readValue(objectMapper.writeValueAsString(responseWrapper.getResponse()),
					SignatureResponseDto.class);

			pdfSignatured = Base64.decodeBase64(signatureResponseDto.getData());

		} catch (Exception e) {
			logger.error(io.mosip.kernel.pdfgenerator.itext.constant.PDFGeneratorExceptionCodeConstant.PDF_EXCEPTION.getErrorMessage(),e.getMessage()
					+ ExceptionUtils.getStackTrace(e));
		}
		logger.debug("UinCardGeneratorImpl::generateUinCard()::exit");

		return pdfSignatured;
	}

	public String generateFace(String biometrics, String rid) throws Exception {
		String encodedImageString = null;

		// Get the original BIR (with full BDBInfo) from CBEFF
		CbeffToBiometricUtil util = new CbeffToBiometricUtil(cbeffutil);
		List<String> subtype = new ArrayList<>();
		BIR bir = util.getBIR(biometrics, FACE, subtype);

		// Run face detection + alignment + compression via the custom SDK
		List<BiometricType> biometricTypes = new ArrayList<>();
		biometricTypes.add(BiometricType.FACE);

		// Must be a mutable list — FaceSDKImpl calls birList.set(index, segment) internally.
		// Collections.singletonList() is immutable and would throw UnsupportedOperationException.
		BiometricRecord biometricRecord = new BiometricRecord();
		List<BIR> segments = new ArrayList<>();
		segments.add(bir);
		biometricRecord.setSegments(segments);

		BiometricRecord result = iBioApi.extractTemplate(biometricRecord, biometricTypes, null).getResponse();
		if (result != null && result.getSegments() != null) {
			for (BIR birRes : result.getSegments()) {
				byte[] imageBytes = birRes.getBdb();
				if (imageBytes != null) {
					logger.info("Rid : {}, Image byte size after compress : {}", rid, imageBytes.length);
					encodedImageString = Base64.encodeBase64String(imageBytes);
					logger.info("Rid : {}, Image string size after encoded : {}", rid, encodedImageString.length());
				}
			}
		}
		return encodedImageString;
	}

	/*private String digitallySignSignature(String stringToSign) throws CryptoException {
        var msg = stringToSign.getBytes(StandardCharsets.UTF_8);

        var privateKeyBytes = Base64.decodeBase64("dazPBzb3e32NKQxTUm/HsEKMfHaCqFM+gf2jCQk0SCyBTlmwEgfcDV5QD/Ml9wX3WeYMQy5pMfjX+eDxvODNZA==");
        var publicKeyBytes = Base64.decodeBase64("gU5ZsBIH3A1eUA/zJfcF91nmDEMuaTH41/ng8bzgzWQ=");

        var privateKey = new Ed25519PrivateKeyParameters(privateKeyBytes, 0);
        var publicKey = new Ed25519PublicKeyParameters(publicKeyBytes, 0);

        Signer signer = new Ed25519Signer();

        signer.init(true, privateKey);
        signer.update(msg, 0, msg.length);
        byte[] signature = signer.generateSignature();
        var actualSignature = Base64.encodeBase64String(signature);
        
		return actualSignature;
	}*/

	private String getDateDisplay(int type) {
		LocalDate currentdate = LocalDate.now();
		int currentDay = currentdate.getDayOfMonth();
		Month currentMonth = currentdate.getMonth();
		int currentYear = currentdate.getYear();
		int monthNumber = currentdate.getMonthValue();
		String monthNumberStr = String.format("%02d", monthNumber);

		String displayDate = currentDay + " " + currentMonth + " " + currentYear;
		String qrDate = currentYear + "-"  +  monthNumberStr + "-" + currentDay;

		if(type == 1) {
			return displayDate;
		}
		else {
			return qrDate;
		}
	}

	private static String modifyDateLayout(String inputDate) throws java.text.ParseException{
		SimpleDateFormat format1 = new SimpleDateFormat("yyyy/MM/dd");
		SimpleDateFormat format2 = new SimpleDateFormat("MMMM dd, yyyy");
		Date date = format1.parse(inputDate);
		return format2.format(date);
	}

	private static String addressReplace(String inputString) {
		String formattedString = null;
		HashMap<String, String> dict = new HashMap<String, String>();

		dict.put("Avenue","AVE");
		dict.put("Block ","BLK");
		dict.put("Boulevard","BLVD");
		dict.put("Building","BLDG");
		dict.put("Center","CNTR");
		dict.put("Corner","COR");
		dict.put("Drive","DR");
		dict.put("Extension","EXT");
		dict.put("Heights","HTS");
		dict.put("Highway","HWY");
		dict.put("Island","IS");
		dict.put("Junction","JCT");
		dict.put("Mountain","MT");
		dict.put("Phase","PH");
		dict.put("Place","PL");
		dict.put("Road","RD");
		dict.put("Subdivision","SUBD");
		dict.put("Square","SQ");
		dict.put("Street","ST");
		dict.put("Valley","VLY");
		dict.put("Village","VILL");

		for(HashMap.Entry<String, String> dic : dict.entrySet()) {
			formattedString = inputString.replace(dic.getKey(), dic.getValue());
		}

		return formattedString.trim().toUpperCase();
	}

	private static String maritalStatusReplace(String inputString) {
		String formattedString = null;
		formattedString = inputString.replace("Undisclosed", "");
		return formattedString.trim();
	}

	private static String bloodTypeReplace(String inputString) {
		String formattedString = null;
		formattedString = inputString.trim();
		if(formattedString.isEmpty() || formattedString.isBlank()) {
			formattedString = "UNKNOWN";
		}
		return formattedString.trim().toUpperCase();
	}

	private static String naToBlank(String inputString) {
		String formattedString = null;
		formattedString = inputString.toLowerCase().trim();
		if (formattedString == "n/a" || formattedString == "not appropriate") {
			formattedString = "";
		}
		return formattedString;
	}

	private static String pobStringReplace(String province, String country) {
		String formattedString = null;
		String country2 = country.toLowerCase();
		if (country2.equalsIgnoreCase("philippines") == false) {
			formattedString = province + ", " + country;
		}
		else {
			formattedString = province;
		}
		return formattedString;
	}
}
	
