package io.mosip.digitalcard.util;

import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.logger.logback.factory.Logfactory;

/**
 * Logger for DigitalCard which provides implementation from kernel logback.
 * 
 * @author Dhanendra Sahu
 *
 */
public final class DigitalCardRepoLogger {


	/**
	 * Instantiates a new id repo logger.
	 */
	private DigitalCardRepoLogger() {
	}

	/**
	 * Method to get the logger for the class provided.
	 *
	 * @param clazz the clazz
	 * @return the logger
	 */
	public static Logger getLogger(Class<?> clazz) {
		return Logfactory.getSlf4jLogger(clazz);
	}
}
