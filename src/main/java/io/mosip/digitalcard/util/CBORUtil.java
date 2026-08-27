package io.mosip.digitalcard.util;

import COSE.AlgorithmID;
import COSE.OneKey;
import com.upokecenter.cbor.CBORObject;
import io.mosip.digitalcard.service.impl.CwtCryptoCtx;
import io.mosip.kernel.core.logger.spi.Logger;
import io.mosip.kernel.core.qrcodegenerator.exception.QrcodeGenerationException;
import io.mosip.kernel.core.qrcodegenerator.spi.QrCodeGenerator;
import io.mosip.kernel.qrcode.generator.zxing.QrcodeGeneratorImpl;
import io.mosip.kernel.qrcode.generator.zxing.constant.QrVersion;
import nl.minvws.encoding.Base45;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class CBORUtil {

    public static final short ISS = 1; // Major type 3 (text string)

    /**
     * The proof-of-possession key selected by the AS
     */
    public static final short CNF = 8; //Major type 5 (map)

    /**
     * The access token identifier
     */
    public static final short CTI = 169; // Major type 2 (byte string)

    /**
     * A cnf containing just a key identifier
     */
    public static final short COSE_KID = 3;

    public static final short IAT = 6; // 6t1

    private static final Logger LOGGER = DigitalCardRepoLogger.getLogger(CBORUtil.class);

 //   @Value("${mosip.digitalcard.service.cose.privatekey}")
    private String privateKeyStr="MC4CAQAwBQYDK2VwBCIEIDikhx6ZexMLF1UtIvqkCg9JWJ29lny7GfvUuNHzFour";

   // @Value("${mosip.digitalcard.service.cose.publickey}")
    private String publicKeyStr="MCowBQYDK2VwAyEAAF8LPSpgm1XFXR8pZtuT3c80Jxjmub3Q-17gV3sCftU";

    private static String coseId="key";

    private static String countryCode="PH";

   // @Value("${mosip.digitalcard.service.cose.id:key}")
    //public void setCoseId(String coseid) {
   //     coseId = coseid;
 //   }

 //   @Value("${mosip.digitalcard.service.country.code:PH}")
   // public void setCountryCode(String countrycode) {
   //     countryCode = countrycode;
    //}


    public byte[] encodeToCborAndSign(JSONObject jsonObject) throws IOException, QrcodeGenerationException {
        byte[] signCborData=null;
        try {
            OneKey oneKey=KeyUtil.getOneKey(privateKeyStr,publicKeyStr);
            signCborData=createSign(jsonObject,oneKey);
            decode(signCborData,oneKey);
        } catch (Exception e) {
            LOGGER.error("not able to sign Cbor data.",e);
        }
        // ToDo : After reading qr code you need to split the qr code because we have added country code, then pass splitedQrCode to decode or verifySignature method
        // String splitedQrCode=decodedQrCode.split("PH1:")[1];
        //verifySignature(Base45.getDecoder().decode(splitedQrCode),oneKey);
        return signCborData;
    }

    public byte[] createSign(JSONObject jsonObject,OneKey oneKey) throws Exception {
        HashMap<Short, CBORObject> claims = new HashMap<>();
        long iatEpoch = new Date().getTime()/1000;
        claims.put(IAT, CBORObject.FromObject(iatEpoch));
        claims.put(ISS, CBORObject.FromObject(countryCode));
        Map<Short, CBORObject> keyMap = new HashMap<Short, CBORObject>();
        keyMap.put(COSE_KID, CBORObject.FromObject(coseId));
        claims.put(CNF, CBORObject.FromObject(keyMap));
        claims.put(CTI, CBORObject.FromObject(jsonObject));
        CBORObject alg =  AlgorithmID.EDDSA.AsCBOR();

        CwtCryptoCtx ctx = CwtCryptoCtx.sign1Create(oneKey, alg);
        CWT cwt = new CWT(claims);
        CBORObject msg = cwt.encode(ctx);
        ctx = CwtCryptoCtx.sign1Verify(oneKey.PublicKey(), alg);
        byte[] rawCWT = msg.EncodeToBytes();
        CWT cwt2 = CWT.processCOSE(rawCWT, ctx);
        LOGGER.info("created sign cwt.");
        return rawCWT;
    }

    public static String decode(byte[] rawCbor, OneKey oneKey) throws IOException, ParseException {
        System.out.println("public key"+oneKey.PublicKey());
        CBORObject alg = AlgorithmID.ECDSA_256.AsCBOR();
        CwtCryptoCtx ctx = CwtCryptoCtx.sign1Verify(oneKey.PublicKey(), alg);
        CWT cwt=null;
        try {
            cwt = CWT.processCOSE(rawCbor, ctx);
        } catch (Exception e) {
            LOGGER.error("error while converting to CWT object");
        }
        CBORObject cborObject=cwt.getClaim((short) 169);
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(cborObject.ToJSONString());
     //   byte[] bytes = cborObject.get("img").GetByteString();
      //  json.put("img",Base64.encodeBase64String(bytes));
        return json.toJSONString();
    }
    public static void verifySignature(byte[] cborData, OneKey oneKey)  {
        CBORObject alg = AlgorithmID.EDDSA.AsCBOR();
        CwtCryptoCtx ctx = CwtCryptoCtx.sign1Verify(oneKey.PublicKey(), alg);
        try {
            CWT cwt2 = CWT.processCOSE(cborData, ctx);
        } catch (Exception e) {
            LOGGER.error("signature is invalid",e);
        }
    }

    public static void main(String[] args) throws QrcodeGenerationException, IOException {
        CBORUtil cborUtil=new CBORUtil();
        QrcodeGeneratorImpl qrcodeGenerator=new QrcodeGeneratorImpl();
        JSONObject jsonObject=new JSONObject();
        jsonObject.put("surName","BASSA");
        jsonObject.put("givenName","FRANCK WILFRIED HERVE");
        jsonObject.put("dateOfBirth","6/23/1974");
        jsonObject.put("gender","M");
        jsonObject.put("NFI","2272979887");
        jsonObject.put("img",Base64.decodeBase64("UklGRpQBAABXRUJQVlA4IIgBAABwCACdASotAC0APpFAl0glpCIhKrqtULASCUAWjQFNGmLOJfzbKFW6FXP2wUx8zweh37ps3n05JlNybNACslBwSCS/ZyCIjz59GSSAAP6ns/8OoNtWDmU7P8KOyV/ktbTZXqaXhiLj7NyYixvarwbs4HtWrt0qv+c5SkgGg+xCrN96PgmoFjdOueYeArRVMnDc51Bo9qmyygNHj0gLUTHHam6j5BZ2P51Lw6QwnI1YtKpXNH9OaxrQraiGZGYWNvLtzGRSskC0qRwO1v5BUuWORhr2RqxnGVNsEBMpGEcDlAucPGgxQvDRRbWJh/UifEbwwJHj5JiJIqzWrhTDLEL9XjGNbsRQnLUP0sb+ufTb2eKc3kYwXaMHl6tebQTclWIVfkkSbiSA9pf/yYz330BLKXbPV1gS6jN7YbpbibamLShc4zS6IRwghzy9T45HrJ18ktyeFNlJwhYH+HoyrHmPZT1tzrnvkD7a8OSntMOPdJuF80SDujmu5zl5wqoqmeC4ynjACqTAAA=="));
        byte[] cborData=cborUtil.encodeToCborAndSign(jsonObject);
        byte[] qrCodeBytes = qrcodeGenerator.generateQrCode("PH1"+":"+Base45.getEncoder().encodeToString(cborData), QrVersion.valueOf("V22"));
        String imageString = Base64.encodeBase64String(qrCodeBytes);

    }
}
