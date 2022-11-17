package net.minecraft.util;

import com.mojang.authlib.yggdrasil.ServicesKeyInfo;
import com.mojang.logging.LogUtils;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import org.slf4j.Logger;

public interface SignatureValidator {
    SignatureValidator NO_VALIDATION = (updatable, signatureData) -> {
        return true;
    };
    Logger LOGGER = LogUtils.getLogger();

    boolean validate(SignatureUpdater updatable, byte[] signatureData);

    default boolean validate(byte[] signedData, byte[] signatureData) {
        return this.validate((SignatureUpdater)((updater) -> {
            updater.update(signedData);
        }), signatureData);
    }

    private static boolean verifySignature(SignatureUpdater updatable, byte[] signatureData, Signature signature) throws SignatureException {
        updatable.update(signature::update);
        return signature.verify(signatureData);
    }

    static SignatureValidator from(PublicKey publicKey, String algorithm) {
        return (updatable, signatureData) -> {
            try {
                Signature signature = Signature.getInstance(algorithm);
                signature.initVerify(publicKey);
                return verifySignature(updatable, signatureData, signature);
            } catch (Exception var5) {
                LOGGER.error("Failed to verify signature", (Throwable)var5);
                return false;
            }
        };
    }

    static SignatureValidator from(ServicesKeyInfo servicesKeyInfo) {
        return (signatureUpdater, bs) -> {
            Signature signature = servicesKeyInfo.signature();

            try {
                return verifySignature(signatureUpdater, bs, signature);
            } catch (SignatureException var5) {
                LOGGER.error("Failed to verify Services signature", (Throwable)var5);
                return false;
            }
        };
    }
}
