package io.mosip.digitalcard.dto;

import io.mosip.kernel.core.http.RequestWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 
 * @author Loganathan Sekaran
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CredentialRequestWrapperDto extends RequestWrapper<CredentialRequestDto> {
}
