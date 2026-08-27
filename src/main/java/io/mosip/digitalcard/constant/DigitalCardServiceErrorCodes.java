package io.mosip.digitalcard.constant;

/**
 * The Enum CredentialServiceErrorCodes.
 * 
 * @author Sowmya
 */
public enum DigitalCardServiceErrorCodes {

	/** The api not accessible exception. */
	API_NOT_ACCESSIBLE_EXCEPTION("DCS-001", "API not accessible"),

	/** The iprepo exception. */
	IPREPO_EXCEPTION("DCS-003", "ID repo response is null"),

	/** The instantiation exception. */
	INSTANTIATION_EXCEPTION("DCS-004", "Error while creating object of JsonValue class"),

	/** The no such field exception. */
	NO_SUCH_FIELD_EXCEPTION("DCS-005", "Could not find the field"),

	/** The credential formatter exception. */
	CREDENTIAL_FORMATTER_EXCEPTION("DCS-006", "exception while formatting"),

	/** The unknown exception. */
	UNKNOWN_EXCEPTION("DCS-007", "unknown exception"),

	/** The policy exception. */
	POLICY_EXCEPTION("DCS-008", "Failed to get policy details"),

	/** The io exception. */
	IO_EXCEPTION("DCS-009", "IO exception"),
	/** The datashare exception. */
	DATASHARE_EXCEPTION("DCS-011", "Datashare response is null"),

	SIGNATURE_EXCEPTION("DCS-012", "Failed to generate digital signature"),
	DATA_ENCRYPTION_FAILURE_EXCEPTION("DCS-013", "Data Encryption failed"),
	
	WEBSUB_FAIL_EXCEPTION("DCS-014", "Websub event failed"),

	POLICY_SCHEMA_VALIDATION_EXCEPTION("DCS-015", "Policy Schema validation failed"),

	VC_CONTEXT_FILE_NOT_FOUND("DCS-016", "Error downloading VC Context file or JSON parsing error."),

	PIN_NOT_PROVIDER("DCS-017", "Pin not available to encrypt the data."),

	PARTNER_EXCEPTION("DCS-018", "Failed to get partner extraction policy details"),

	QRCODE_NOT_GENERATED("DCS-019", "Error while generating QR Code"),

	PDF_NOT_GENERATED("DCS-020", "Error while generating PDF for UIN Card"),

	DATA_NOT_FOUND("DCS-021","Applicant Photo Not Found"),

	IDENTITY_NOT_FOUND("DCS-022", "Unable to Find Identity Field in ID JSON"),

	APPLICANT_PHOTO_NOT_SET( "DCS-023", "Error while setting applicant photo"),

	QRCODE_NOT_SET( "DCS-024", "Error while setting qrCode for uin card"),

	ATTRIBUTE_NOT_SET( "DCS-024", "Error while setting template attributes for uin card"),

	TEM_PROCESSING_FAILURE("DCS-025", "The Processing of Template Failed "),

	DIGITAL_CARD_NOT_GENERATED("DCS-026", "Error while generating PDF for Digital Card"),

	DIGITAL_CARD_NOT_CREATED("DCS-027", "Digital Card is not generated try after some time."),

	VC_VERIFICATION_FAILED("DCS-028", "VC verification is failed."),

	IMAGE_VERIFICATION_FAILED("DCS-029", "Image is not correct");

	/** The error code. */
	private final String errorCode;

	/** The error message. */
	private final String errorMessage;

	/**
	 * Instantiates a new credential service error codes.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 */
	private DigitalCardServiceErrorCodes(final String errorCode, final String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets the error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Gets the error message.
	 *
	 * @return the error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Gets the error code and error message.
	 *
	 * @return the error message
	 */
	public String getError() {
		return errorCode+" "+errorMessage;
	}
}