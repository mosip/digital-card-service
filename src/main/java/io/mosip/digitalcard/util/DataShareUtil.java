package io.mosip.digitalcard.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.digitalcard.constant.ApiName;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.dto.DataShareDto;
import io.mosip.digitalcard.dto.DataShareResponseDto;
import io.mosip.digitalcard.dto.ErrorDTO;
import io.mosip.digitalcard.exception.ApiNotAccessibleException;
import io.mosip.digitalcard.exception.DataShareException;
import io.mosip.kernel.core.exception.ExceptionUtils;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataShareUtil {
	@Autowired
	RestClient restClient;

	@Autowired
	private ObjectMapper mapper;
	
	
	private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(DataShareUtil.class);

	private static final String CREDENTIALFILE = "credentialfile";

	@Autowired
	private Environment env;

	public DataShareDto getDataShare(byte[] data, String policyId, String partnerId)
			throws ApiNotAccessibleException, IOException, DataShareException {
		long fileLengthInBytes=0;
		try {
			LOGGER.debug(Utility.getUser(), " ", "",
		
					"creating data share entry");
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
			map.add("name", CREDENTIALFILE);
			map.add("filename", CREDENTIALFILE);

			ByteArrayResource contentsAsResource = new ByteArrayResource(data) {
				@Override
				public String getFilename() {
					return CREDENTIALFILE;
				}
			};
			map.add("file", contentsAsResource);
			fileLengthInBytes = contentsAsResource.contentLength();
		List<String> pathsegments = new ArrayList<>();
		pathsegments.add(policyId);
		pathsegments.add(partnerId);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
				map, headers);
			URL dataShareUrl = null;
			String url = null;

			String responseString = (String)restClient.postApi(ApiName.CREATEDATASHARE, pathsegments, "", "",
					MediaType.MULTIPART_FORM_DATA, requestEntity, String.class);

		DataShareResponseDto responseObject = mapper.readValue(responseString, DataShareResponseDto.class);
			if (responseObject == null) {
				LOGGER.debug(Utility.getUser(), " ", "",
						"File size" + " " + fileLengthInBytes);
				LOGGER.error(Utility.getUser(), " ", "",
						DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
		}
		if (responseObject != null && responseObject.getErrors() != null && !responseObject.getErrors().isEmpty()) {

			ErrorDTO error = (ErrorDTO) responseObject.getErrors().get(0);
				LOGGER.debug(Utility.getUser(), " ", "",
						"File size" + " " + fileLengthInBytes);
				LOGGER.error(Utility.getUser(), " ", "",
					error.getMessage());
		} else {
				LOGGER.debug(Utility.getUser(), " ", "",
						"data share created");
			return responseObject.getDataShare();
			}
		} catch (Exception e) {
			LOGGER.debug(Utility.getUser(), " ", "",
					"File size" + " " + fileLengthInBytes);
			LOGGER.error(Utility.getUser(), " ", "",
					ExceptionUtils.getStackTrace(e));
		}
		return null;
	}


}
