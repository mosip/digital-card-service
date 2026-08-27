package io.mosip.digitalcard.dto;

import io.mosip.kernel.core.http.ResponseWrapper;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataShareResponseDto extends ResponseWrapper {

	private static final long serialVersionUID = 1L;


    private DataShareDto dataShare;
}
