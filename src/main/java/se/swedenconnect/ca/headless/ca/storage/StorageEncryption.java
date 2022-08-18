package se.swedenconnect.ca.headless.ca.storage;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * Interface for implementing functions for encrypting and decrypting storage data
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface StorageEncryption {

  /**
   * Encrypt data with option to apply compression on the data to be signed. Note that applying compression to
   * a short input byte array will increase the size of the data to be encrypted. Only apply compression to
   * input that is expected to be at least 128 bytes long to get length reduction and only apply compression to
   * data that is not random in nature.
   *
   * @param dataToBeEncrypted data to be encrypted
   * @param compression true to apply compression to the data to be encrypted before encryption
   * @return encrypted data
   * @throws IOException on error decrypting or decompressing data
   */
  byte[] encryptData(byte[] dataToBeEncrypted, boolean compression) throws IOException;

  /**
   * Decrypt encrypted data with option to apply decompression on the decrypted data
   *
   * @param encryptedData encrypted data to decrypt
   * @param compression true to decompress the decrypted data
   * @param keyMap optional KeyMap providing key password strings for given kid (key identifier) values for non default keys
   * @return decrypted data
   * @throws IOException on errors decrypting the encrypted data
   */
  byte[] decryptData(byte[] encryptedData, boolean compression, @Nullable Map<String, String> keyMap) throws IOException;

}
