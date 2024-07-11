package io.mosip.digitalcard.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.mosip.digitalcard.constant.DigitalCardServiceErrorCodes;
import io.mosip.digitalcard.controller.DigitalCardController;
import io.mosip.digitalcard.dto.*;
import io.mosip.digitalcard.entity.DigitalCardTransactionEntity;
import io.mosip.digitalcard.exception.ApiNotAccessibleException;
import io.mosip.digitalcard.exception.DataNotFoundException;
import io.mosip.digitalcard.exception.DataShareException;
import io.mosip.digitalcard.exception.DigitalCardServiceException;
import io.mosip.digitalcard.repositories.DigitalCardTransactionRepository;
import io.mosip.digitalcard.service.DigitalCardService;
import io.mosip.digitalcard.service.CardGeneratorService;
import io.mosip.digitalcard.util.*;
import io.mosip.digitalcard.websub.CredentialStatusEvent;
import io.mosip.digitalcard.websub.StatusEvent;
import io.mosip.digitalcard.websub.WebSubSubscriptionHelper;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.pdfgenerator.exception.PDFGeneratorException;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.kernel.core.websub.model.EventModel;
import io.mosip.vercred.CredentialsVerifier;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The DigitalCardServiceImpl.
 *
 * @author Dhanendra
 */
@Service
public class DigitalCardServiceImpl implements DigitalCardService {

    @Autowired
    private CardGeneratorService pdfCardServiceImpl;

    @Autowired
    private CredentialUtil credentialUtil;

    @Autowired
    Utility utility;

    @Autowired
    RestClient restClient;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private CredentialsVerifier credentialsVerifier;

    @Autowired
    private DataShareUtil dataShareUtil;

    @Autowired
    private WebSubSubscriptionHelper webSubSubscriptionHelper;

    @Autowired
    DigitalCardTransactionRepository digitalCardTransactionRepository;

    /** The Constant VALUE. */
    private static final String VALUE = "value";

    @Value("${mosip.digitalcard.datashare.partner.id}")
    private String dataSharePartnerId;

    @Value("${mosip.digitalcard.datashare.policy.id}")
    private String dataSharePolicyId;

    @Value("${mosip.digitalcard.verify.credentials.flag:true}")
    private boolean verifyCredentialsFlag;

    @Value("${mosip.digitalcard.credentials.request.initiate.flag:true}")
    private boolean isInitiateFlag;

    @Value("${mosip.digitalcard.pdf.password.enable.flag:true}")
    private boolean isPasswordProtected;

    @Value("${mosip.digitalcard.credential.request.partner.id}")
    private String partnerId;

    @Value("${mosip.digitalcard.credential.type}")
    private String credentialType;

    @Value("${mosip.digitalcard.websub.publish.topic:CREDENTIAL_STATUS_UPDATE}")
    private String topic;

    @Value("${mosip.digitalcard.uincard.password}")
    private String digitalCardPassword;

    @Value("${mosip.template-language}")
    private String templateLang;


    Logger logger = DigitalCardRepoLogger.getLogger(DigitalCardController.class);

    public void generateDigitalCard(String credential, String credentialType,String dataShareUrl,String eventId,String transactionId,Map<String,Object> additionalAttributes) {
        boolean isGenerated = false;
        String decryptedCredential=null;
        String password=null;
        String rid=null;
        try {
            if (dataShareUrl != null) {
                credential = restClient.getForObject(dataShareUrl, String.class);
            }
            decryptedCredential = encryptionUtil.decryptData(credential);
            logger.info("decrypted data: {}",decryptedCredential);
            JSONObject jsonObject = new org.json.JSONObject(decryptedCredential);
            JSONObject decryptedCredentialJson = jsonObject.getJSONObject("credentialSubject");
            rid=getRid(decryptedCredentialJson.get("id"));
            if (verifyCredentialsFlag){
                logger.info("Configured received credentials to be verified. Flag {}", verifyCredentialsFlag);
                boolean verified =credentialsVerifier.verifyCredentials(decryptedCredential);
                if (!verified) {
                    loginErrorDetails(rid,DigitalCardServiceErrorCodes.VC_VERIFICATION_FAILED.getError());
                    logger.error("Received Credentials failed in verifiable credential verify method. So, digital card is not getting generated." +
                            " Id: {}, Transaction Id: {}",eventId, transactionId);
                    throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
                }
            }
            if (isPasswordProtected) {
                password = getPassword(decryptedCredentialJson);
                logger.info("password: {}",password);
            }
            byte[] pdfBytes=pdfCardServiceImpl.generateCard(decryptedCredentialJson, credentialType,password,additionalAttributes);
            digitalCardStatusUpdate(transactionId,pdfBytes,credentialType,rid);
        }catch (QrcodeGenerationException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.QRCODE_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.QRCODE_NOT_GENERATED.getErrorMessage(), e);
        } catch (PDFGeneratorException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.PDF_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.PDF_NOT_GENERATED.getErrorMessage() ,e);
        }catch (JsonParseException | JsonMappingException e) {
            loginErrorDetails(rid,DigitalCardServiceErrorCodes.ATTRIBUTE_NOT_SET.getError());
            logger.error(DigitalCardServiceErrorCodes.ATTRIBUTE_NOT_SET.getErrorMessage() ,e);
        } catch (Exception e){
            loginErrorDetails(rid, DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getError());
            logger.error(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage() , e);
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
        }
    }

    @Override
    public DigitalCardStatusResponseDto getDigitalCard(String rid) {
        String pdfByteString=null;
        try {
            DigitalCardTransactionEntity digitalCardTransactionEntity=digitalCardTransactionRepository.findByRID(rid);
            if(digitalCardTransactionEntity!=null && digitalCardTransactionEntity.getDataShareUrl()!=null){
                DigitalCardStatusResponseDto digitalCardStatusResponseDto=new DigitalCardStatusResponseDto();
                digitalCardStatusResponseDto.setId(digitalCardTransactionEntity.getrid());
                digitalCardStatusResponseDto.setStatusCode(digitalCardTransactionEntity.getStatusCode());
                digitalCardStatusResponseDto.setUrl(digitalCardTransactionEntity.getDataShareUrl());
                return digitalCardStatusResponseDto;
            } else if(isInitiateFlag && digitalCardTransactionEntity==null) {
                CredentialRequestDto credentialRequestDto=new CredentialRequestDto();
                credentialRequestDto.setCredentialType(credentialType);
                credentialRequestDto.setIssuer(partnerId);
                credentialRequestDto.setId(rid);
                CredentialResponse credentialResponse = credentialUtil.reqCredential(credentialRequestDto);
                saveTransactionDetails(credentialResponse, null);
            }
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorMessage());
        } catch (DataNotFoundException | DataAccessException | DataAccessLayerException e) {
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage());
        }
    }

    @Override
    public void initiateCredentialRequest(String rid,String ridHash) {
        String pdfByteString = null;
        CredentialRequestDto credentialRequestDto = new CredentialRequestDto();
        credentialRequestDto.setCredentialType(credentialType);
        credentialRequestDto.setIssuer(partnerId);
        credentialRequestDto.setId(rid);
        try {
            CredentialResponse credentialResponse = credentialUtil.reqCredential(credentialRequestDto);
            saveTransactionDetails(credentialResponse, ridHash);
        } catch (DigitalCardServiceException e) {
            logger.error(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_GENERATED.getErrorMessage(),e);
            throw new DigitalCardServiceException(DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorCode(),DigitalCardServiceErrorCodes.DIGITAL_CARD_NOT_CREATED.getErrorMessage());
        }
    }


    private void saveTransactionDetails(CredentialResponse credentialResponse, String idHash){
        DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
        digitalCardEntity.setrid(credentialResponse.getId());
        digitalCardEntity.setUinSaltedHash(idHash);
        digitalCardEntity.setCredentialId(credentialResponse.getRequestId());
        digitalCardEntity.setCreateDateTime(LocalDateTime.now());
        digitalCardEntity.setCreatedBy(Utility.getUser());
        digitalCardEntity.setStatusCode("NEW");
        digitalCardTransactionRepository.save(digitalCardEntity);

    }
    private void digitalCardStatusUpdate(String requestId, byte[] data, String credentialType, String rid)
            throws DataShareException, ApiNotAccessibleException, IOException, Exception {
        DataShareDto dataShareDto = null;
        dataShareDto = dataShareUtil.getDataShare(data, dataSharePolicyId, dataSharePartnerId);
        CredentialStatusEvent creEvent = new CredentialStatusEvent();
        LocalDateTime currentDtime = DateUtils.getUTCCurrentDateTime();
        DigitalCardTransactionEntity digitalCardTransactionEntity=digitalCardTransactionRepository.findByRID(rid);
        if(digitalCardTransactionEntity==null){
            DigitalCardTransactionEntity digitalCardEntity=new DigitalCardTransactionEntity();
            digitalCardEntity.setrid(rid);
            digitalCardEntity.setCreateDateTime(LocalDateTime.now());
            digitalCardEntity.setCreatedBy(Utility.getUser());
            digitalCardEntity.setDataShareUrl(dataShareDto.getUrl());
            digitalCardEntity.setStatusCode("AVAILABLE");
            digitalCardTransactionRepository.save(digitalCardEntity);
        }else{
            digitalCardTransactionRepository.updateTransactionDetails(rid,"AVAILABLE", dataShareDto.getUrl(),LocalDateTime.now(),Utility.getUser());
        }
        StatusEvent sEvent = new StatusEvent();
        sEvent.setId(UUID.randomUUID().toString());
        sEvent.setRequestId(requestId);
        sEvent.setStatus("STORED");
        sEvent.setUrl(dataShareDto.getUrl());
        sEvent.setTimestamp(Timestamp.valueOf(currentDtime).toString());
        creEvent.setPublishedOn(LocalDateTime.now().toString());
        creEvent.setPublisher("DIGITAL_CARD_SERVICE");
        creEvent.setTopic(topic);
        creEvent.setEvent(sEvent);
        webSubSubscriptionHelper.digitalCardStatusUpdateEvent(topic, creEvent);
        logger.info("publish event for topic : {} and rid : {}",topic,rid);
    }
    private String getRid(Object id) {
        String rid= id.toString().split("/credentials/")[1];
        return rid;
    }
    /**
     * Gets the password.
     *
     * @param jsonObject
     * @return
     * @throws Exception
     */
    private String getPassword(JSONObject jsonObject) throws Exception {
        String[] attributes = digitalCardPassword.split("\\|");
        List<String> list = new ArrayList<>(Arrays.asList(attributes));

        Iterator<String> it = list.iterator();
        String uinCardPd = "";
        Object obj=null;
        while (it.hasNext()) {
            String key = it.next().trim();

            Object object = jsonObject.get(key);
            if (object != null) {
                try {
                    obj = new JSONParser().parse(object.toString());
                } catch (Exception e) {
                    obj = object;
                }
            }
            if (obj instanceof JSONArray) {
                // JSONArray node = JsonUtil.getJSONArray(demographicIdentity, value);
                SimpleType[] jsonValues = Utility.mapJsonNodeToJavaObject(SimpleType.class, (JSONArray) obj);
                uinCardPd = uinCardPd.concat(getFormattedPasswordAttribute(getParameter(jsonValues, templateLang)).substring(0,4));
            } else if (object instanceof org.json.simple.JSONObject) {
                org.json.simple.JSONObject json = (org.json.simple.JSONObject) object;
                uinCardPd = uinCardPd.concat((String) json.get(VALUE));
            } else {
                uinCardPd = uinCardPd.concat(getFormattedPasswordAttribute((String) object.toString()).substring(0,4));
            }
        }
        return uinCardPd.toUpperCase();
    }

    private String getFormattedPasswordAttribute(String password){
        if(password.length()==3){
            return password=password.concat(password.substring(0,1));
        }else if(password.length()==2){
            return password=password.repeat(2);
        }else if(password.length()==1) {
            return password=password.repeat(4);
        }else {
            return password;
        }
    }

    /**
     * Gets the parameter.
     *
     * @param jsonValues
     *            the json values
     * @param langCode
     *            the lang code
     * @return the parameter
     */
    private String getParameter(SimpleType[] jsonValues, String langCode) {

        String parameter = null;
        if (jsonValues != null) {
            for (int count = 0; count < jsonValues.length; count++) {
                String lang = jsonValues[count].getLanguage();
                if (langCode.contains(lang)) {
                    parameter = jsonValues[count].getValue();
                    break;
                }
            }
        }
        return parameter;
    }
    public void loginErrorDetails(String rid, String errorMsg){
        digitalCardTransactionRepository.updateErrorTransactionDetails(rid,"ERROR",errorMsg,LocalDateTime.now(),Utility.getUser());
    }

}
