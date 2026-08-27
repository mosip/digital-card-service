package io.mosip.digitalcard.service;

import com.upokecenter.cbor.CBORObject;
import io.mosip.digitalcard.exception.AceException;

/**
 * An interface with methods that access tokens need to implement.
 *  
 * @author Dhanendra
 *
 */
public interface AccessToken {

	/**
	 * Checks if the token is expired at the given time
	 * 
	 * @param now  the time for which the expiry should be checked
	 * 
	 * @return  true if the token is expired, false if it is still valid
	 * @throws AceException
	 */
	public boolean expired(long now) throws AceException;
	
	/**
	 * Checks if the token is still valid (including expiration).
	 * Note that this method may need to perform introspection.
	 * 
	 * @param now  the time for which validity should be checked
	 * 
	 * @return  true if the token is valid, false if it is invalid
	 * @throws AceException 
	 */
	public boolean isValid(long now) throws AceException;
	
	
	/**
	 * Encodes this Access Token as a CBOR Object.
	 * 
	 * @return  the encoding of the token.
	 */
	public CBORObject encode();
	
	/**
	 * @return  the string representation of the cti by Base64 encoding it
	 * 
	 * @throws AceException  if the token has no cti
	 */
	public String getCti() throws AceException;
	
}
