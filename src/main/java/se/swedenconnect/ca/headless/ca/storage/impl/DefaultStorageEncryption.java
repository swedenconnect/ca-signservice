/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.swedenconnect.ca.headless.ca.storage.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.headless.ca.storage.StorageEncryption;
import se.swedenconnect.ca.headless.ca.storage.data.EncryptedData;

import javax.annotation.Nullable;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Implementation of storage encryption
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class DefaultStorageEncryption implements StorageEncryption {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private char[] password;
    private String kid;
    private byte[] salt;
    private int iterations;
    private int keylength;

    private SecretKey encryptionKey;

    /**
     * Constructor specifying all parameters
     *
     * @param password encryption password key
     * @param kid key identifier used to identify the specified password key
     * @param salt salt
     * @param keylength key length
     * @param iterations the number of times that the password is hashed during the derivation of the symmetric key
     */
    public DefaultStorageEncryption(char[] password, String kid, byte[] salt, int keylength, int iterations)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
        this.password = password;
        this.kid = kid;
        this.salt = salt;
        this.iterations = iterations;
        this.keylength = keylength;

        this.encryptionKey = getKey();
    }

    /**
     * Constructor with default iteration count set to 65536 and key length set to 128
     *
     * @param password encryption password
     * @param kid key identifier used to identify the specified password key
     * @param salt salt
     */
    public DefaultStorageEncryption(char[] password, String kid, byte[] salt)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
        this(password, kid, salt, 128, 65536);
    }

    /**
     * Constructor with default iteration count set to 65536
     *
     * @param password encryption password
     * @param kid key identifier used to identify the specified password key
     * @param salt salt
     * @param keylength key length
     */
    public DefaultStorageEncryption(char[] password, String kid, byte[] salt, int keylength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
        this(password, kid, salt, keylength, 65536);
    }


    /** {@inheritDoc} */
    @Override
    public byte[] encryptData(byte[] dataToBeEncrypted, boolean compress) throws IOException {
        try {
            byte[] toBeEncryptedBytes = getSerializedData(dataToBeEncrypted, compress);
            return encrypt(toBeEncryptedBytes);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] decryptData(byte[] encData, boolean compressed, Map<String, String> keyMap) throws IOException {
        try {
            byte[] decrypt = decrypt(encData, keyMap);
            return getResultFromSerializedData(decrypt, compressed);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private byte[] getSerializedData(byte[] toBeEncryptedBytes, boolean compress) throws IOException {
        if (compress){
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            gzipOut.write(toBeEncryptedBytes);
            gzipOut.close();
            return baos.toByteArray();
        }
        return toBeEncryptedBytes;
    }
    
    private byte[] getResultFromSerializedData(byte[] data, boolean compressed) throws Exception {
        if (compressed) {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            GZIPInputStream gzipIn = new GZIPInputStream(bais);
            data = gzipIn.readAllBytes();
        }
        return data;
    }
    
    private byte[] encrypt(byte[] toBeEncrypted) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException,
      InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException,
      JsonProcessingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] ciphertext = cipher.doFinal(toBeEncrypted);
        EncryptedData encDataObject = new EncryptedData(kid, iv, ciphertext);
        return OBJECT_MAPPER.writeValueAsBytes(encDataObject);
    }

    private byte[] decrypt(byte[] encryptedData, Map<String, String> keyMap) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException,
      InvalidKeySpecException, IllegalBlockSizeException,
      BadPaddingException, IOException {
        EncryptedData encDataObj = OBJECT_MAPPER.readValue(encryptedData, EncryptedData.class);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(encDataObj.getKid(), keyMap), new IvParameterSpec(encDataObj.getIv()));
        return cipher.doFinal(encDataObj.getCiphertext());
    }

    private SecretKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return getKey(null, null);
    }
    private SecretKey getKey(final @Nullable String keyId, final @Nullable Map<String, String> keyMap) throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] encPassword;
        if (keyId == null || keyId.equalsIgnoreCase(kid)) {
            log.debug("Decrypting with default key: {}", kid);
            if (encryptionKey != null) {
                // We have a default key. Use it
                return encryptionKey;
            }
            // No key has been generated. Generate it.
            encPassword = password;
        } else {
            if (keyMap == null || !keyMap.containsKey(keyId)) {
                throw new IllegalArgumentException("Key ID provided by no corresponding key was provided");
            }
            log.debug("Decrypting with provided key: {}", keyId);
            encPassword = keyMap.get(keyId).toCharArray();
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(encPassword, salt, iterations, keylength);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }

}
