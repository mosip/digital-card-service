package io.mosip.digitalcard.dto;

import lombok.Data;

@Data
public class CredentialStatusResponse {

	
	private String requestId;
	
	private String id;
	
	private String statusCode;

	private String url;
}
