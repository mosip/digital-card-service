package io.mosip.digitalcard.exception;

import io.mosip.kernel.core.exception.BaseCheckedException;
/**
 *A base class which covers the range of exceptions which may occur.
 *
 *  @author Dhanendra
 * @since 1.1.5.x
 */

public class ImageVerificationException extends BaseCheckedException {

    /**
     * Unique id for serialization
     */
    private static final long serialVersionUID = 473719335574042491L;

    /**
     * Constructor with errorCode, errorMessage, and rootCause
     *
     * @param errorCode    The error code for this exception
     * @param errorMessage The error message for this exception
     * @param rootCause    Cause of this exception
     */
    public ImageVerificationException(String errorCode, String errorMessage, Throwable rootCause) {
        super(errorCode, errorMessage, rootCause);
    }

    public ImageVerificationException() {
        super();
    }

    public ImageVerificationException(String errorMsg) {
        super(errorMsg);
    }
}
