package io.mosip.digitalcard.dto;

import lombok.Data;

import java.util.List;

@Data
public class CredentialResponseDto  {
	private static final long serialVersionUID = 1L;

	private CredentialResponse response;

	private List<ErrorDTO> errors;
}
