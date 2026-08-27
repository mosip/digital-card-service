package io.mosip.digitalcard.constant;

/**
 * The Enum IdRepoConstants - contains constants used internally by the
 * application.
 *
 * @author Manoj SP
 */
public class DigitalCardConstants {

	/** The application version. */
	public static final String APPLICATION_VERSION = "mosip.digitalcard.application.version";

	/** The application id. */
	public static final String APPLICATION_ID = "mosip.digitalcard.app-id";

	/** The application name. */
	public static final String APPLICATION_NAME = "mosip.digitalcard.application.name";

	/** The mosip primary language. */
	public static final String MOSIP_PRIMARY_LANGUAGE = "mosip.primary-language";

	public static final String WEB_SUB_PUBLISH_URL = "websub.publish.url";

	public static final String WEB_SUB_HUB_URL = "websub.hub.url";

	public static final String CALLBACKURL = "mosip.digitalcard.websub.callback.url";

	public static final String SUBSCRIBE_TOPIC = "mosip.digitalcard.event.subscribe.topic";

	/** The value. */
	private final String value;

	public static final int DEFAULT_SALT_KEY_LENGTH = 3;


	/**
	 * Instantiates a new id repo constants.
	 *
	 * @param value the value
	 */
	private DigitalCardConstants(String value) {
		this.value = value;
	}

	/**
	 * Gets the value.
	 *
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
}