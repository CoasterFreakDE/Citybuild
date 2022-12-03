package net.minecraft.util;

import com.google.common.primitives.Longs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraft.network.FriendlyByteBuf;

public class Crypt {
    private static final String SYMMETRIC_ALGORITHM = "AES";
    private static final int SYMMETRIC_BITS = 128;
    private static final String ASYMMETRIC_ALGORITHM = "RSA";
    private static final int ASYMMETRIC_BITS = 1024;
    private static final String BYTE_ENCODING = "ISO_8859_1";
    private static final String HASH_ALGORITHM = "SHA-1";
    public static final String SIGNING_ALGORITHM = "SHA256withRSA";
    private static final String PEM_RSA_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PEM_RSA_PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
    public static final String RSA_PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String RSA_PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";
    public static final String MIME_LINE_SEPARATOR = "\n";
    public static final Base64.Encoder MIME_ENCODER = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));
    public static final Codec<PublicKey> PUBLIC_KEY_CODEC = Codec.STRING.comapFlatMap((key) -> {
        try {
            return DataResult.success(stringToRsaPublicKey(key));
        } catch (CryptException var2) {
            return DataResult.error(var2.getMessage());
        }
    }, Crypt::rsaPublicKeyToString);
    public static final Codec<PrivateKey> PRIVATE_KEY_CODEC = Codec.STRING.comapFlatMap((key) -> {
        try {
            return DataResult.success(stringToPemRsaPrivateKey(key));
        } catch (CryptException var2) {
            return DataResult.error(var2.getMessage());
        }
    }, Crypt::pemRsaPrivateKeyToString);

    public static SecretKey generateSecretKey() throws CryptException {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            return keyGenerator.generateKey();
        } catch (Exception var1) {
            throw new CryptException(var1);
        }
    }

    public static KeyPair generateKeyPair() throws CryptException {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception var1) {
            throw new CryptException(var1);
        }
    }

    public static byte[] digestData(String baseServerId, PublicKey publicKey, SecretKey secretKey) throws CryptException {
        try {
            return digestData(baseServerId.getBytes("ISO_8859_1"), secretKey.getEncoded(), publicKey.getEncoded());
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    private static byte[] digestData(byte[]... bytes) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");

        for(byte[] bs : bytes) {
            messageDigest.update(bs);
        }

        return messageDigest.digest();
    }

    private static <T extends Key> T rsaStringToKey(String key, String prefix, String suffix, Crypt.ByteArrayToKeyFunction<T> decoder) throws CryptException {
        int i = key.indexOf(prefix);
        if (i != -1) {
            i += prefix.length();
            int j = key.indexOf(suffix, i);
            key = key.substring(i, j + 1);
        }

        try {
            return decoder.apply(Base64.getMimeDecoder().decode(key));
        } catch (IllegalArgumentException var6) {
            throw new CryptException(var6);
        }
    }

    public static PrivateKey stringToPemRsaPrivateKey(String key) throws CryptException {
        return rsaStringToKey(key, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----", Crypt::byteToPrivateKey);
    }

    public static PublicKey stringToRsaPublicKey(String key) throws CryptException {
        return rsaStringToKey(key, "-----BEGIN RSA PUBLIC KEY-----", "-----END RSA PUBLIC KEY-----", Crypt::byteToPublicKey);
    }

    public static String rsaPublicKeyToString(PublicKey key) {
        if (!"RSA".equals(key.getAlgorithm())) {
            throw new IllegalArgumentException("Public key must be RSA");
        } else {
            return "-----BEGIN RSA PUBLIC KEY-----\n" + MIME_ENCODER.encodeToString(key.getEncoded()) + "\n-----END RSA PUBLIC KEY-----\n";
        }
    }

    public static String pemRsaPrivateKeyToString(PrivateKey key) {
        if (!"RSA".equals(key.getAlgorithm())) {
            throw new IllegalArgumentException("Private key must be RSA");
        } else {
            return "-----BEGIN RSA PRIVATE KEY-----\n" + MIME_ENCODER.encodeToString(key.getEncoded()) + "\n-----END RSA PRIVATE KEY-----\n";
        }
    }

    private static PrivateKey byteToPrivateKey(byte[] key) throws CryptException {
        try {
            EncodedKeySpec encodedKeySpec = new PKCS8EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(encodedKeySpec);
        } catch (Exception var3) {
            throw new CryptException(var3);
        }
    }

    public static PublicKey byteToPublicKey(byte[] key) throws CryptException {
        try {
            EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(key);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(encodedKeySpec);
        } catch (Exception var3) {
            throw new CryptException(var3);
        }
    }

    public static SecretKey decryptByteToSecretKey(PrivateKey privateKey, byte[] encryptedSecretKey) throws CryptException {
        byte[] bs = decryptUsingKey(privateKey, encryptedSecretKey);

        try {
            return new SecretKeySpec(bs, "AES");
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    public static byte[] encryptUsingKey(Key key, byte[] data) throws CryptException {
        return cipherData(1, key, data);
    }

    public static byte[] decryptUsingKey(Key key, byte[] data) throws CryptException {
        return cipherData(2, key, data);
    }

    private static byte[] cipherData(int opMode, Key key, byte[] data) throws CryptException {
        try {
            return setupCipher(opMode, key.getAlgorithm(), key).doFinal(data);
        } catch (Exception var4) {
            throw new CryptException(var4);
        }
    }

    private static Cipher setupCipher(int opMode, String algorithm, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(opMode, key);
        return cipher;
    }

    public static Cipher getCipher(int opMode, Key key) throws CryptException {
        try {
            Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
            cipher.init(opMode, key, new IvParameterSpec(key.getEncoded()));
            return cipher;
        } catch (Exception var3) {
            throw new CryptException(var3);
        }
    }

    interface ByteArrayToKeyFunction<T extends Key> {
        T apply(byte[] key) throws CryptException;
    }

    public static record SaltSignaturePair(long salt, byte[] signature) {
        public static final Crypt.SaltSignaturePair EMPTY = new Crypt.SaltSignaturePair(0L, ByteArrays.EMPTY_ARRAY);

        public SaltSignaturePair(FriendlyByteBuf buf) {
            this(buf.readLong(), buf.readByteArray());
        }

        public boolean isValid() {
            return this.signature.length > 0;
        }

        public static void write(FriendlyByteBuf buf, Crypt.SaltSignaturePair signatureData) {
            buf.writeLong(signatureData.salt);
            buf.writeByteArray(signatureData.signature);
        }

        public byte[] saltAsBytes() {
            return Longs.toByteArray(this.salt);
        }
    }

    public static class SaltSupplier {
        private static final SecureRandom secureRandom = new SecureRandom();

        public static long getLong() {
            return secureRandom.nextLong();
        }
    }
}
