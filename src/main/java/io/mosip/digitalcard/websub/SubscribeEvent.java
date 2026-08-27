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

import java.util.Date;

@Component
public class SubscribeEvent implements ApplicationListener<ApplicationReadyEvent> {

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Value("${mosip.digitalcard.subscription-delay-millisecs:60000}")
	private int taskSubsctiptionDelay;

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

	@Autowired
	private WebSubSubscriptionHelper webSubSubscriptionHelper;

	/** The Constant BIOMETRICS. */
	private static final String ONAPPLICATIONEVENT = "onApplicationEvent";

	private static final String SUBSCIRBEEVENT = "SubscribeEvent";

	private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(SubscribeEvent.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		LOGGER.info(Utility.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
				"Scheduling event subscriptions after (milliseconds): " + taskSubsctiptionDelay);
		taskScheduler.schedule(this::initSubsriptions, new Date(System.currentTimeMillis() + taskSubsctiptionDelay));
	}

	private void initSubsriptions() {
		LOGGER.info(Utility.getUser(), SUBSCIRBEEVENT, ONAPPLICATIONEVENT,
				"Initializing subscribptions..");
		webSubSubscriptionHelper.initSubscriptions(credentialTopic,credentialCallBackUrl);
		webSubSubscriptionHelper.initSubscriptions(identityCreateTopic,identityCreateCallBackUrl);
		webSubSubscriptionHelper.initSubscriptions(identityUpdateTopic,identityUpdateCallBackUrl);
	}
}
