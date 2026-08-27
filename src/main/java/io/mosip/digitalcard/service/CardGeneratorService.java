package io.mosip.digitalcard.service;


import org.json.JSONObject;

public interface CardGeneratorService {

	/**
	 * The PDFCardService
	 *
	 * @param decryptedCredentialJson
	 * @param credentialType
	 * @param password
	 * @return
	 */
	public byte[] generateCard(JSONObject decryptedCredentialJson, String credentialType, String password, String rid) throws Exception;

}