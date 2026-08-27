package io.mosip.digitalcard.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Dhanendra
 * @since 1.1.5.x
 */
@Data


public class TemplateResponseDto implements Serializable {
	/**
	 * Default serial version
	 */
	private static final long serialVersionUID = 1L;
	private List<TemplateDto> templates;
}
