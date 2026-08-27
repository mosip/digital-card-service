package io.mosip.digitalcard.exception;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;


public class ApiNotAccessibleException extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ApiNotAccessibleException() {
		super(DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage());
    }

    public ApiNotAccessibleException(String message) {
		super(DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
                message);
    }

    public ApiNotAccessibleException(Throwable e) {
		super(DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorMessage(), e);
    }

    public ApiNotAccessibleException(String errorMessage, Throwable t) {
		super(DigitalCardServiceErrorCodes.API_NOT_ACCESSIBLE_EXCEPTION.getErrorCode(), errorMessage, t);
    }


}
