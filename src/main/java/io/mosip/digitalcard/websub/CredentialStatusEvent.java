package io.mosip.digitalcard.websub;

import lombok.Data;
@Data
public class CredentialStatusEvent {
	private String publisher;
	private String topic;
	private String publishedOn;
	private StatusEvent event;
}
