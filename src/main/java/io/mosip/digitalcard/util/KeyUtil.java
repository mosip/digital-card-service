package io.mosip.digitalcard.util;

import COSE.OneKey;
import net.i2p.crypto.eddsa.EdDSASecurityProvider;

import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class KeyUtil {
    

    private static final String EDDSA_ALGO = "EdDSA";

    public static OneKey getOneKey(String privateKeyStr,String publicKeyStr) throws Exception {

        Provider edDSAProvider = new EdDSASecurityProvider();

        Security.addProvider(edDSAProvider);

        byte[] privateKeyBytes = Base64.getUrlDecoder(). decode(privateKeyStr);
        byte[] publicKeyBytes = Base64.getUrlDecoder().decode(publicKeyStr);
        
        KeyFactory keyFactoryForPrivKey = KeyFactory.getInstance(EDDSA_ALGO, edDSAProvider);
        KeySpec pkcs8Keyspec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKeyObj = keyFactoryForPrivKey.generatePrivate(pkcs8Keyspec);
        KeyFactory keyFactoryForPubKey = KeyFactory.getInstance(EDDSA_ALGO, edDSAProvider);
        KeySpec x509Keyspec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKeyObj = keyFactoryForPubKey.generatePublic(x509Keyspec);
        return new OneKey(publicKeyObj, privateKeyObj);
    }
}
