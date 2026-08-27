package io.mosip.digitalcard.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CredentialRequestDto {

	private String id;
	private String credentialType;
	private String issuer;
    private String  recepiant;
	private String user;
	private boolean encrypt;
	private String encryptionKey;
    private List<String> sharableAttributes;
    private Map<String,Object> additionalData;
	
}
