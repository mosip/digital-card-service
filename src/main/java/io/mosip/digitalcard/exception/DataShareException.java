package io.mosip.digitalcard.exception;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.kernel.core.exception.BaseCheckedException;

public class DataShareException  extends BaseCheckedException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DataShareException() {
		super(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage());
    }

    public DataShareException(String message) {
		super(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
                message);
    }

    public DataShareException(Throwable e) {
		super(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(),
				DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorMessage(), e);
    }

    public DataShareException(String errorMessage, Throwable t) {
		super(DigitalCardServiceErrorCodes.DATASHARE_EXCEPTION.getErrorCode(), errorMessage, t);
    }

}
