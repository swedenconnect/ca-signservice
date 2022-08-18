package se.swedenconnect.ca.headless.ca.storage.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedData {

  private String kid;
  private byte[] iv;
  private byte[] ciphertext;

}
