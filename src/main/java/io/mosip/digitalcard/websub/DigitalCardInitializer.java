package io.mosip.digitalcard.websub;

import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.Utility;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class DigitalCardInitializer implements ApplicationListener<ApplicationReadyEvent> {
	@Value("${retry-count:3}")
	private int retryCount;

	@Value("${mosip.digitalcard.resubscription-delay-millisecs:100000}")
	private int reSubscriptionDelaySecs;

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Value("${mosip.digitalcard.generate.identity.create.websub.topic}")
	private String identityCreateTopic;

	@Value("${mosip.digitalcard.generate.identity.update.websub.topic}")
	private String identityUpdateTopic;

	@Value("${mosip.digitalcard.generate.credential.websub.topic}")
	private String credentialTopic;

	@Value("${mosip.digitalcard.generate.identity.create.callbackurl}")
	private String identityCreateCallBackUrl;

	@Value("${mosip.digitalcard.generate.identity.update.callbackurl}")
	private String identityUpdateCallBackUrl;

	@Value("${mosip.digitalcard.generate.credential.callbackurl}")
	private String credentialCallBackUrl;

	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	private static final String DIGITALCARDINITIALIZER = "DigitalCardInitializer";

	private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(DigitalCardInitializer.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		if (reSubscriptionDelaySecs > 0) {
		LOGGER.info(Utility.getUser(), DIGITALCARDINITIALIZER, ONAPPLICATIONEVENT,
				"Work around for web-sub notification issue after some time.");

		scheduleRetrySubscriptions();
		}
		else {
			LOGGER.info(Utility.getUser(), DIGITALCARDINITIALIZER, ONAPPLICATIONEVENT,
					"Scheduling for re-subscription is Disabled as the re-subsctription delay value is: "
							+ reSubscriptionDelaySecs);

		}
	}

	private void scheduleRetrySubscriptions() {
		LOGGER.info(Utility.getUser(), DIGITALCARDINITIALIZER, ONAPPLICATIONEVENT,
				"Scheduling re-subscription every " + reSubscriptionDelaySecs + " seconds");


		taskScheduler.scheduleAtFixedRate(this::retrySubscriptions, Instant.now().plusSeconds(reSubscriptionDelaySecs),
				Duration.ofSeconds(reSubscriptionDelaySecs));
	}

	private void retrySubscriptions() {
		// Call Init Subscriptions for the count until no error in the subscription
		for (int i = 0; i <= retryCount; i++) {
			if (initSubsriptions()) {
				return;
			}
		}
	}

	private boolean initSubsriptions() {
		try {
			LOGGER.info(Utility.getUser(), DIGITALCARDINITIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscriptions..");
			webSubSubscriptionHelper.initSubscriptions(credentialTopic,credentialCallBackUrl);
			webSubSubscriptionHelper.initSubscriptions(identityCreateTopic,identityCreateCallBackUrl);
			webSubSubscriptionHelper.initSubscriptions(identityUpdateTopic,identityUpdateCallBackUrl);
			return true;
		} catch (Exception e) {
			LOGGER.error(Utility.getUser(), DIGITALCARDINITIALIZER, ONAPPLICATIONEVENT,
					"Initializing subscriptions failed: " + e.getMessage());
			return false;
		}
	}
}
