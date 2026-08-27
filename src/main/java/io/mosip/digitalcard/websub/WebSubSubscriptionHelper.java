package io.mosip.digitalcard.websub;

import io.mosip.digitalcard.util.DigitalCardRepoLogger;
import io.mosip.digitalcard.util.Utility;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.websub.spi.PublisherClient;
import io.mosip.kernel.core.websub.spi.SubscriptionClient;
import io.mosip.kernel.websub.api.exception.WebSubClientException;
import io.mosip.kernel.websub.api.model.SubscriptionChangeRequest;
import io.mosip.kernel.websub.api.model.SubscriptionChangeResponse;
import io.mosip.kernel.websub.api.model.UnsubscriptionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WebSubSubscriptionHelper {

	@Autowired
	SubscriptionClient<SubscriptionChangeRequest, UnsubscriptionRequest, SubscriptionChangeResponse> sb;

	@Value("${websub.hub.url}")
	private String webSubHubUrl;

	@Value("${mosip.digitalcard.websub.secret}")
	private String webSubSecret;

	@Value("${websub.publish.url}")
	private String webSubPublishUrl;

	@Autowired
	private PublisherClient<String, CredentialStatusEvent, HttpHeaders> pb;


	@Autowired
	RestTemplate restTemplate;


	private static final String WEBSUBSUBSCRIPTIONHEPLER = "WebSubSubscriptionHelper";

	private static final String INITSUBSCRIPTION = "initSubsriptions";

	private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(WebSubSubscriptionHelper.class);

	public void initSubscriptions(String topic,String callBackUrl) {
		LOGGER.info(Utility.getUser(), WEBSUBSUBSCRIPTIONHEPLER, INITSUBSCRIPTION,
				"Initializing subscribptions for topic : "+topic);
		subscribeForDigitalCardServiceEvents(topic,callBackUrl);
	}

	private void subscribeForDigitalCardServiceEvents(String topic,String callBackUrl) {
		try {
			SubscriptionChangeRequest subscriptionRequest = new SubscriptionChangeRequest();
			subscriptionRequest.setCallbackURL(callBackUrl);
			subscriptionRequest.setHubURL(webSubHubUrl);
			subscriptionRequest.setSecret(webSubSecret);
			subscriptionRequest.setTopic(topic);
			sb.subscribe(subscriptionRequest);
		} catch (WebSubClientException e) {
			e.printStackTrace();
			LOGGER.info(Utility.getUser(), WEBSUBSUBSCRIPTIONHEPLER, INITSUBSCRIPTION,
					"websub subscription error");
		}
	}
	public void digitalCardStatusUpdateEvent(String topic, CredentialStatusEvent credentialStatusEvent) {
		try {
			HttpHeaders headers = new HttpHeaders();
			pb.publishUpdate(topic, credentialStatusEvent, MediaType.APPLICATION_JSON_UTF8_VALUE, headers,
					webSubHubUrl);
		} catch (WebSubClientException e) {
			e.printStackTrace();
			LOGGER.info("websub publish update error {} {}", WEBSUBSUBSCRIPTIONHEPLER, INITSUBSCRIPTION);
		}
	}
}
