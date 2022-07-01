package io.mosip.digitalcard.websub;

import lombok.Data;

@Data
public class StatusEvent {

		private String id;
	
		private String requestId;
		
		private String timestamp;
		
		private String status;
		
		private String url;
}