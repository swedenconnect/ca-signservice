/*
 * Copyright (c) 2022-2023.  Agency for Digital Government (DIGG)
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.signservice.ca.storage.StorageEncryption;

/**
 * Tests for the storage data encryption.
 */
@Slf4j
class DefaultStorageEncryptionTest {

  @Test
  void testEncryption() throws Exception {

    DefaultStorageEncryption encryption = new DefaultStorageEncryption("S3cr3t".toCharArray(), "kid01",
      "euhrt123!".getBytes(StandardCharsets.UTF_8));

    byte[] dataToeBeEncrypted = "DataToBeEncrypted-No-Compression".getBytes(StandardCharsets.UTF_8);
    log.info("Encrypting string: {}", new String(dataToeBeEncrypted, StandardCharsets.UTF_8));
    byte[] encryptedData = encryption.encryptData(dataToeBeEncrypted, false);

    log.info("Encrypted data: {}", new String(encryptedData, StandardCharsets.UTF_8));

    byte[] decryptedData = encryption.decryptData(encryptedData, false, null);
    assertArrayEquals(dataToeBeEncrypted, decryptedData);
    log.info("Successfully decrypted encrypted data to: {}", new String(decryptedData, StandardCharsets.UTF_8));

    dataToeBeEncrypted = "DataToBeEncryptedWithCompression".getBytes(StandardCharsets.UTF_8);
    log.info("Encrypting and compressing string: {}", new String(dataToeBeEncrypted, StandardCharsets.UTF_8));
    encryptedData = encryption.encryptData(dataToeBeEncrypted, true);

    log.info("Encrypted data: {}", new String(encryptedData, StandardCharsets.UTF_8));

    decryptedData = encryption.decryptData(encryptedData, true, null);
    assertArrayEquals(dataToeBeEncrypted, decryptedData);
    log.info("Successfully decrypted and decompressed encrypted data to: {}",
      new String(decryptedData, StandardCharsets.UTF_8));

    String longMessage =
      "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
        + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure\n"
        + "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non\n"
        + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n"
        + "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
        + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure\n"
        + "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non\n"
        + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n"
        + "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n"
        + "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure\n"
        + "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non\n"
        + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n";

    // Encrypting long string with compression
    dataToeBeEncrypted = longMessage.getBytes(StandardCharsets.UTF_8);
    log.info("Long message test with message length: {} bytes", dataToeBeEncrypted.length);

    log.info("Encrypting and compressing long string:\n{}", new String(dataToeBeEncrypted, StandardCharsets.UTF_8));
    encryptedData = encryption.encryptData(dataToeBeEncrypted, true);
    log.info("Encrypted data:\n{}", new String(encryptedData, StandardCharsets.UTF_8));
    int lenCompressedEncrypted = encryptedData.length;
    log.info("Data length - Encrypted data with compression: {}", lenCompressedEncrypted);
    decryptedData = encryption.decryptData(encryptedData, true, null);
    assertArrayEquals(dataToeBeEncrypted, decryptedData);
    log.info("Successfully decrypted and decompressed encrypted data to:\n{}",
      new String(decryptedData, StandardCharsets.UTF_8));

    log.info("Encrypting and compressing long string without compression:\n{}",
      new String(dataToeBeEncrypted, StandardCharsets.UTF_8));
    encryptedData = encryption.encryptData(dataToeBeEncrypted, false);
    log.info("Encrypted data:\n{}", new String(encryptedData, StandardCharsets.UTF_8));
    int lenNoCompressedEncrypted = encryptedData.length;
    log.info("Data length - Encrypted data with no compression: {}", lenNoCompressedEncrypted);
    decryptedData = encryption.decryptData(encryptedData, false, null);
    assertArrayEquals(dataToeBeEncrypted, decryptedData);
    log.info("Successfully decrypted and decompressed encrypted data to:\n{}",
      new String(decryptedData, StandardCharsets.UTF_8));

    assertTrue(lenCompressedEncrypted < lenNoCompressedEncrypted);
    log.info("Uncompressed length {} is greater than compressed length {} of the same long message encryption",
      lenNoCompressedEncrypted, lenCompressedEncrypted);

    byte[] finalEncryptedData = encryptedData;
    IOException ex = assertThrows(IOException.class, () -> encryption.decryptData(finalEncryptedData, true, null));
    log.info("Attemtpting to decompress non compressed data results in exception: {}", ex.getMessage());

  }

  @Test
  void keyMapTest() throws Exception {

    Map<String, String> keyMap = new HashMap<>();
    keyMap.put("key01", "sdfjIu98#%6&f");
    keyMap.put("key02", "Jiurf487dfvn3#€");
    keyMap.put("key03", "Juhr4589jhnSwlsdkjfklsdjflksdjf");
    Map<String, String> incompleteKeyMap = new HashMap<>();
    keyMap.put("key01", "sdfjIu98#%6&f");
    keyMap.put("key02", "Jiurf487dfvn3#€");
    byte[] salt = "salt01!#".getBytes(StandardCharsets.UTF_8);

    StorageEncryption encryption01 = new DefaultStorageEncryption(keyMap.get("key01").toCharArray(), "key01", salt);
    StorageEncryption encryption02 = new DefaultStorageEncryption(keyMap.get("key02").toCharArray(), "key02", salt);
    StorageEncryption encryption03 = new DefaultStorageEncryption(keyMap.get("key03").toCharArray(), "key03", salt);

    String dataToEncrypt = "This data is encrypted";

    // Tests
    encryptTestWithKey("Encrypted with key01 encrypter and decrypted with key01 decrypter", encryption01, encryption01,
      null, dataToEncrypt, false, false, null);

    encryptTestWithKey("Encrypted with key01 encrypter and decrypted with key02 decrypter", encryption01, encryption02,
      keyMap, dataToEncrypt, false, false, null);

    encryptTestWithKey("Encrypted with key02 encrypter and decrypted with key03 decrypter with compression", encryption02, encryption03,
      keyMap, dataToEncrypt, true, true, null);

    encryptTestWithKey("Encrypted with key03 encrypter and incomplete key map", encryption03, encryption01,
      incompleteKeyMap, dataToEncrypt, false, false, IOException.class);

  }

  void encryptTestWithKey(String message, StorageEncryption encryption, StorageEncryption decryption,
    Map<String, String> keyMap, String data, boolean compressEncrypt, boolean compressDecrypt,
    Class<? extends Exception> exceptionClass) throws IOException {

    log.info("Encryption test with specific key: " + message);
    log.info("Encrypting: {}", data);

    if (exceptionClass != null) {
      Exception exception = assertThrows(exceptionClass, () -> {
        byte[] encData = encryption.encryptData(data.getBytes(StandardCharsets.UTF_8), compressEncrypt);
        decryption.decryptData(encData, compressDecrypt, keyMap);
      });
      log.info("Successfully encountered expected exception: {}", exception.toString());
      return;
    }

    byte[] encData = encryption.encryptData(data.getBytes(StandardCharsets.UTF_8), compressEncrypt);
    log.info("Encrypted data: {}", new String(encData));
    byte[] decryptedData = decryption.decryptData(encData, compressDecrypt, keyMap);
    log.info("Decrypted data: {}", new String(decryptedData, StandardCharsets.UTF_8));

  }

}