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
package se.swedenconnect.ca.headless.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Dataclass for storage crypto configuration.
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
