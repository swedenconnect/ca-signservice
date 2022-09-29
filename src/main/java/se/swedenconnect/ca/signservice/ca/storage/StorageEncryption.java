/*
 * Copyright (c) 2022 Sweden Connect
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
package se.swedenconnect.ca.signservice.ca.storage;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Interface for implementing functions for encrypting and decrypting storage data
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface StorageEncryption {

  /**
   * Encrypt data with option to apply compression on the data to be signed. Note that applying compression to a short
   * input byte array will increase the size of the data to be encrypted. Only apply compression to input that is
   * expected to be at least 128 bytes long to get length reduction and only apply compression to data that is not
   * random in nature.
   *
   * @param dataToBeEncrypted data to be encrypted
   * @param compression true to apply compression to the data to be encrypted before encryption
   * @return encrypted data
   * @throws IOException on error decrypting or decompressing data
   */
  byte[] encryptData(final byte[] dataToBeEncrypted, final boolean compression) throws IOException;

  /**
   * Decrypt encrypted data with option to apply decompression on the decrypted data
   *
   * @param encryptedData encrypted data to decrypt
   * @param compression true to decompress the decrypted data
   * @param keyMap optional KeyMap providing key password strings for given kid (key identifier) values for non default
   *          keys
   * @return decrypted data
   * @throws IOException on errors decrypting the encrypted data
   */
  byte[] decryptData(final byte[] encryptedData, final boolean compression, @Nullable final Map<String, String> keyMap)
      throws IOException;

}
