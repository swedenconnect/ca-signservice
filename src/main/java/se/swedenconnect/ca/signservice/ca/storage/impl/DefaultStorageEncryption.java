/*
 * Copyright 2024.  Agency for Digital Government (DIGG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.swedenconnect.ca.signservice.ca.storage.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.signservice.ca.storage.StorageEncryption;
import se.swedenconnect.ca.signservice.ca.storage.data.EncryptedData;

/**
 * Implementation of storage encryption.
 */
@Slf4j
public class DefaultStorageEncryption implements StorageEncryption {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private final char[] password;
  private final String kid;
  private final byte[] salt;
  private final int iterations;
  private final int keylength;

  private final SecretKey encryptionKey;

  /**
   * Constructor specifying all parameters.
   *
   * @param password encryption password key
   * @param kid key identifier used to identify the specified password key
   * @param salt salt
   * @param keylength key length
   * @param iterations the number of times that the password is hashed during the derivation of the symmetric key
   */
  public DefaultStorageEncryption(final char[] password, final String kid, final byte[] salt, final int keylength,
      final int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException {
    this.password = password;
    this.kid = kid;
    this.salt = salt;
    this.iterations = iterations;
    this.keylength = keylength;

    this.encryptionKey = this.getKey();
  }

  /**
   * Constructor with default iteration count set to 65536 and key length set to 128
   *
   * @param password encryption password
   * @param kid key identifier used to identify the specified password key
   * @param salt salt
   */
  public DefaultStorageEncryption(final char[] password, final String kid, final byte[] salt)
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
  public DefaultStorageEncryption(final char[] password, final String kid, final byte[] salt, final int keylength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    this(password, kid, salt, keylength, 65536);
  }

  /** {@inheritDoc} */
  @Override
  public byte[] encryptData(final byte[] dataToBeEncrypted, final boolean compress) throws IOException {
    try {
      final byte[] toBeEncryptedBytes = this.getSerializedData(dataToBeEncrypted, compress);
      return this.encrypt(toBeEncryptedBytes);
    }
    catch (final Exception e) {
      throw new IOException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public byte[] decryptData(final byte[] encData, final boolean compressed, final Map<String, String> keyMap)
      throws IOException {
    try {
      final byte[] decrypt = this.decrypt(encData, keyMap);
      return this.getResultFromSerializedData(decrypt, compressed);
    }
    catch (final Exception e) {
      throw new IOException(e);
    }
  }

  private byte[] getSerializedData(final byte[] toBeEncryptedBytes, final boolean compress) throws IOException {
    if (compress) {
      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (final GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
        gzipOut.write(toBeEncryptedBytes);
        gzipOut.close();
        return baos.toByteArray();
      }
    }
    return toBeEncryptedBytes;
  }

  private byte[] getResultFromSerializedData(byte[] data, final boolean compressed) throws Exception {
    if (compressed) {
      final ByteArrayInputStream bais = new ByteArrayInputStream(data);
      final GZIPInputStream gzipIn = new GZIPInputStream(bais);
      data = gzipIn.readAllBytes();
    }
    return data;
  }

  private byte[] encrypt(final byte[] toBeEncrypted) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeySpecException, InvalidKeyException,
      InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException,
      JsonProcessingException {
    final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, this.encryptionKey);
    final AlgorithmParameters params = cipher.getParameters();
    final byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
    final byte[] ciphertext = cipher.doFinal(toBeEncrypted);
    final EncryptedData encDataObject = new EncryptedData(this.kid, iv, ciphertext);
    return OBJECT_MAPPER.writeValueAsBytes(encDataObject);
  }

  private byte[] decrypt(final byte[] encryptedData, final Map<String, String> keyMap) throws NoSuchAlgorithmException,
      NoSuchPaddingException, InvalidKeyException,
      InvalidAlgorithmParameterException,
      InvalidKeySpecException, IllegalBlockSizeException,
      BadPaddingException, IOException {
    final EncryptedData encDataObj = OBJECT_MAPPER.readValue(encryptedData, EncryptedData.class);
    final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, this.getKey(encDataObj.getKid(), keyMap), new IvParameterSpec(encDataObj.getIv()));
    return cipher.doFinal(encDataObj.getCiphertext());
  }

  private SecretKey getKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
    return this.getKey(null, null);
  }

  private SecretKey getKey(final @Nullable String keyId, final @Nullable Map<String, String> keyMap)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    char[] encPassword;
    if (keyId == null || keyId.equalsIgnoreCase(this.kid)) {
      log.debug("Decrypting with default key: {}", this.kid);
      if (this.encryptionKey != null) {
        // We have a default key. Use it
        return this.encryptionKey;
      }
      // No key has been generated. Generate it.
      encPassword = this.password;
    }
    else {
      if (keyMap == null || !keyMap.containsKey(keyId)) {
        throw new IllegalArgumentException("Key ID provided by no corresponding key was provided");
      }
      log.debug("Decrypting with provided key: {}", keyId);
      encPassword = keyMap.get(keyId).toCharArray();
    }
    final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    final KeySpec spec = new PBEKeySpec(encPassword, this.salt, this.iterations, this.keylength);
    final SecretKey tmp = factory.generateSecret(spec);
    return new SecretKeySpec(tmp.getEncoded(), "AES");
  }

}
