package io.mosip.digitalcard.exception;


import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;


public class DataEncryptionFailureException extends BaseCheckedException {

	/** Serializable version Id. */
	private static final long serialVersionUID = 1L;

	public DataEncryptionFailureException() {
		super(DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage());
	}

	public DataEncryptionFailureException(Throwable t) {
		super(DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorMessage(), t);
	}

	/**
	 * @param message
	 *            Message providing the specific context of the error.
	 * @param cause
	 *            Throwable cause for the specific exception
	 */
	public DataEncryptionFailureException(String message, Throwable cause) {
		super(DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), message, cause);

	}

	public DataEncryptionFailureException(String errorMessage) {
		super(DigitalCardServiceErrorCodes.DATA_ENCRYPTION_FAILURE_EXCEPTION.getErrorCode(), errorMessage);
	}

}