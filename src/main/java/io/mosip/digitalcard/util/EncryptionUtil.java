package io.mosip.digitalcard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.dto.CryptomanagerRequestDto;
import io.mosip.digitalcard.dto.CryptomanagerResponseDto;
import io.mosip.digitalcard.exception.ApiNotAccessibleException;
import io.mosip.digitalcard.exception.DataEncryptionFailureException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.exception.ServiceError;
import io.mosip.kernel.core.http.RequestWrapper;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.util.DateUtils2;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class EncryptionUtil {

	
	private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(EncryptionUtil.class);

	
	/** The Constant DATETIME_PATTERN. */
	private static final String DATETIME_PATTERN = "mosip.digitalcard.service.datetime.pattern";
	

	/** The Constant IO_EXCEPTION. */
	private static final String IO_EXCEPTION = "Exception while reading packet inputStream";

	/** The Constant DATE_TIME_EXCEPTION. */
	private static final String DATE_TIME_EXCEPTION = "Error while parsing packet timestamp";


	/** The env. */
	@Autowired
	private Environment env;

	@Autowired
	RestClient restClient;

	@Autowired
	private ObjectMapper mapper;

	/** The application id. */
	@Value("${mosip.digitalcard.crypto.application.id:DIGITAL_CARD}")
	private String applicationId;

	@Value("${mosip.digitalcard.crypto.partner.id}")
	private String partnerId;
	
	public String decryptData(String dataToBedecrypted) throws DataEncryptionFailureException, ApiNotAccessibleException {
		LOGGER.debug(Utility.getUser()," ","",
				"started decrypting data using partner certificate");
		String decryptedPacket = null;
		try {

			CryptomanagerRequestDto cryptomanagerRequestDto = new CryptomanagerRequestDto();
			RequestWrapper<CryptomanagerRequestDto> request = new RequestWrapper<>();
			cryptomanagerRequestDto.setApplicationId(applicationId);
			cryptomanagerRequestDto.setData(dataToBedecrypted);
			cryptomanagerRequestDto.setReferenceId(partnerId);
			cryptomanagerRequestDto
					.setPrependThumbprint(
							env.getProperty("crypto.PrependThumbprint.enable", Boolean.class));
			DateTimeFormatter format = DateTimeFormatter.ofPattern(env.getProperty(DATETIME_PATTERN));
			LocalDateTime localdatetime = LocalDateTime
					.parse(DateUtils2.getUTCCurrentDateTimeString(env.getProperty(DATETIME_PATTERN)), format);
			request.setRequesttime(localdatetime);

			request.setRequest(cryptomanagerRequestDto);
			cryptomanagerRequestDto.setTimeStamp(localdatetime);
			String response = restClient.postApi(ApiName.CRYPTOMANAGER_DECRYPT, null, "", "", MediaType.APPLICATION_JSON,
					request, String.class);

			CryptomanagerResponseDto responseObject = mapper.readValue(response, CryptomanagerResponseDto.class);
			if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {
				LOGGER.error(Utility.getUser()," ", "",
						"credential decryption failed");
				ServiceError error = responseObject.getErrors().get(0);
				throw new DataEncryptionFailureException(error.getMessage());
			}
			decryptedPacket = new String (Base64.decodeBase64(responseObject.getResponse().getData()));
		} catch (IOException e) {
			LOGGER.error(Utility.getUser()," ", "",
					"Credential Data Decryption error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(IO_EXCEPTION, e);
		} catch (DateTimeParseException e) {
			LOGGER.error(Utility.getUser()," ", "",
					"Credential Data Decryption error with error message" + ExceptionUtils.getStackTrace(e));
			throw new DataEncryptionFailureException(DATE_TIME_EXCEPTION);
		} catch (Exception e) {
			LOGGER.error(Utility.getUser()," ", "",
					"Credential Data Decryption error with error message" + ExceptionUtils.getStackTrace(e));
			if (e.getCause() instanceof HttpClientErrorException) {
				HttpClientErrorException httpClientException = (HttpClientErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpClientException.getResponseBodyAsString());
			} else if (e.getCause() instanceof HttpServerErrorException) {
				HttpServerErrorException httpServerException = (HttpServerErrorException) e.getCause();
				throw new ApiNotAccessibleException(httpServerException.getResponseBodyAsString());
			} else {
				throw new DataEncryptionFailureException(e.getMessage());
			}

		}
		return decryptedPacket;

	}



}
