package io.mosip.digitalcard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDTO implements Serializable {

	private static final long serialVersionUID = 2452990684776944908L;

	/** The errorcode. */
	private String errorCode;
	
	/** The message. */
	private String message;
}
