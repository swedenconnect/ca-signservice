package se.swedenconnect.ca.headless.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Dataclass for storage crypto configuration
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Configuration
@ConfigurationProperties(prefix = "ca-service")
@Data
public class StorageCryptoConfiguration {

  /** A map indexed by the instance id containing crypto configuration properties for encrypting stored data */
  private Map<String, InstanceStorageCryptoConfig> storageCrypto;


  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InstanceStorageCryptoConfig {

    /** Defines if crypto of stored data is enabled */
    private Boolean enabled;
    /** base64 encoded value of salt byte data */
    private String salt;
    /** A password string used to derive an encryption key */
    private String key;
    /** A string key identifier used to identify this key */
    private String kid;
    /** The length of the derived symmetric encryption key  */
    private Integer keyLength;
    /** The number of iterations the key is hashed to form the encryption key. Default 65536 */
    private Integer iterations;
    /** An optional map of keys mapped under their kid, providing the decryption keys this encryptor may use to decrypt data */
    private Map<String, String> keyStore;

  }
}
