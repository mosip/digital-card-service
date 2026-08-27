package io.mosip.digitalcard.service;

import io.mosip.digitalcard.dto.DigitalCardStatusResponseDto;
import io.mosip.kernel.core.websub.model.EventModel;
/**
 * The Service DigitalCardService.
 *
 * @author Dhanendra
 */

public interface DigitalCardService {
    void generateDigitalCard(String credential, String credentialType,String dataShareUrl,String eventId,String transactionId);

    DigitalCardStatusResponseDto getDigitalCard(String rid);

    void initiateCredentialRequest(String rid,String ridHash);
}
