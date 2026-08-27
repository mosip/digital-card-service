package io.mosip.digitalcard.exception;


import io.mosip.kernel.core.exception.BaseUncheckedException;

/**
 * The Class IdentityNotFoundException.
 */
public class IdentityNotFoundException extends BaseUncheckedException {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Instantiates a new identity not found exception.
	 */
	public IdentityNotFoundException() {
		super();
	}
	
	/**
	 * Instantiates a new identity not found exception.
	 *
	 * @param errorMessage the error message
	 */
	public IdentityNotFoundException(String errorMessage) {
		super( errorMessage);
	}

	/**
	 * Instantiates a new identity not found exception.
	 *
	 * @param message the message
	 * @param cause the cause
	 */
	public IdentityNotFoundException(String message, Throwable cause) {
		super(message, String.valueOf(cause));
	}
	public IdentityNotFoundException(String errorCode,String message) {
		super(errorCode,message);
	}
}
