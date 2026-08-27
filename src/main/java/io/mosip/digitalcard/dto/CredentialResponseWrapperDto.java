package io.mosip.digitalcard.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author Loganathan Sekaran
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialResponseWrapperDto extends ResponseWrapper<CredentialRequestDto> {
}
