package com.roomwallah.trust.infrastructure.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TrustEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final SecretKeySpec keySpec;

    public TrustEncryptionService(@Value("${roomwallah.trust.encryption.key:RoomwallahTrustSecretKeySecretKey!}") String key) {
        byte[] keyBytes = new byte[32];
        byte[] sourceBytes = key.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(sourceBytes, 0, keyBytes, 0, Math.min(sourceBytes.length, 32));
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTE];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] cipherTextWithIv = new byte[IV_LENGTH_BYTE + cipherText.length];
            System.arraycopy(iv, 0, cipherTextWithIv, 0, IV_LENGTH_BYTE);
            System.arraycopy(cipherText, 0, cipherTextWithIv, IV_LENGTH_BYTE, cipherText.length);

            return Base64.getEncoder().encodeToString(cipherTextWithIv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt data", e);
        }
    }

    public String decrypt(String cipherTextWithIvBase64) {
        if (cipherTextWithIvBase64 == null) return null;
        try {
            byte[] cipherTextWithIv = Base64.getDecoder().decode(cipherTextWithIvBase64);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            System.arraycopy(cipherTextWithIv, 0, iv, 0, IV_LENGTH_BYTE);

            int cipherTextLength = cipherTextWithIv.length - IV_LENGTH_BYTE;
            byte[] cipherText = new byte[cipherTextLength];
            System.arraycopy(cipherTextWithIv, IV_LENGTH_BYTE, cipherText, 0, cipherTextLength);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH_BIT, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

            byte[] decryptedTextBytes = cipher.doFinal(cipherText);
            return new String(decryptedTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt data", e);
        }
    }
}
