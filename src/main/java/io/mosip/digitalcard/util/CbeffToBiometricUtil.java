package io.mosip.digitalcard.util;

import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.exception.DataNotFoundException;
import io.mosip.kernel.biometrics.constant.BiometricType;
import io.mosip.kernel.biometrics.entities.BIR;
import io.mosip.kernel.biometrics.spi.CbeffUtil;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The Class CbeffToBiometricUtil.
 *
 * @author Dhanendra
 */
@Component
public class CbeffToBiometricUtil {

	/** The print logger. */
	private static final Logger logger = LoggerFactory.getLogger(CbeffToBiometricUtil.class);

	/** The cbeffutil. */
	private final CbeffUtil cbeffutil;

	/**
	 * Instantiates biometric util.
	 *
	 * @param cbeffutil the CbeffUtil implementation to use
	 */
	public CbeffToBiometricUtil(CbeffUtil cbeffutil) {
		this.cbeffutil = cbeffutil;
	}

	/**
	 * Gets the first BIR matching the given biometric type string.
	 *
	 * @param cbeffFileString the base64-encoded CBEFF XML string
	 * @param type            the biometric type name (e.g. "Face", "Finger")
	 * @param subType         sub-type list (used for future filtering; currently
	 *                        returns the first type match)
	 * @return the matching BIR
	 * @throws Exception if parsing fails or no matching BIR is found
	 */
	public BIR getBIR(String cbeffFileString, String type, List<String> subType) throws Exception {
		logger.debug("CbeffToBiometricUtil::getBIR()::entry type={}", type);
		List<BIR> birList = cbeffutil.getBIRDataFromXML(Base64.decodeBase64(cbeffFileString));
		for (BIR bir : birList) {
			if (bir.getBdbInfo() != null) {
				for (BiometricType biometricType : bir.getBdbInfo().getType()) {
					if (biometricType.value().equalsIgnoreCase(type)) {
						logger.debug("CbeffToBiometricUtil::getBIR()::exit found match");
						return bir;
					}
				}
			}
		}
		throw new DataNotFoundException(
				DigitalCardServiceErrorCodes.DATA_NOT_FOUND.getErrorCode(),
				DigitalCardServiceErrorCodes.DATA_NOT_FOUND.getErrorMessage());
	}

	/**
	 * Gets image bytes for the first BIR matching the given type.
	 *
	 * @param cbeffFileString the base64-encoded CBEFF XML string
	 * @param type            the biometric type name (e.g. "Face")
	 * @param subType         sub-type list
	 * @return the raw BDB image bytes, or null if cbeffFileString is null
	 */
	public byte[] getImageBytes(String cbeffFileString, String type, List<String> subType) {
		logger.debug("CbeffToBiometricUtil::getImageBytes()::entry");
		byte[] photoBytes = null;
		if (cbeffFileString != null) {
			try {
				photoBytes = getBIR(cbeffFileString, type, subType).getBdb();
			} catch (Exception e) {
				throw new DataNotFoundException(
						DigitalCardServiceErrorCodes.DATA_NOT_FOUND.getErrorCode(),
						DigitalCardServiceErrorCodes.DATA_NOT_FOUND.getErrorMessage());
			}
		}
		logger.debug("CbeffToBiometricUtil::getImageBytes()::exit");
		return photoBytes;
	}

	/**
	 * Gets BIR list from raw CBEFF XML bytes.
	 *
	 * @param xmlBytes byte array of XML data
	 * @return list of BIR
	 * @throws Exception on parse failure
	 */
	public List<BIR> getBIRDataFromXML(byte[] xmlBytes) throws Exception {
		return cbeffutil.getBIRDataFromXML(xmlBytes);
	}
}
