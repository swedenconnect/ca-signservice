/*
 * Copyright (c) 2021-2022.  Agency for Digital Government (DIGG)
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

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.headless.ca.SignServiceCAInstances;
import se.swedenconnect.ca.headless.ca.StorageOnlyCARepository;
import se.swedenconnect.ca.headless.ca.storage.CARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.StorageEncryption;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultCARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultStorageEncryption;
import se.swedenconnect.ca.service.base.configuration.BasicServiceConfig;
import se.swedenconnect.ca.service.base.configuration.instance.CAServices;
import se.swedenconnect.ca.service.base.configuration.instance.InstanceConfiguration;
import se.swedenconnect.ca.service.base.configuration.keys.PkiCredentialFactory;
import se.swedenconnect.ca.service.base.configuration.properties.CAConfigData;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * CA Service configuration class that generates the CA services beans for each CA instance as defined by the configuration properties
 * for each instance.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Configuration
public class CAServiceConfiguration implements ApplicationEventPublisherAware {

  private ApplicationEventPublisher applicationEventPublisher;

  /**
   * The CA services bean provide all CA services as defined by the configuration of each instance
   *
   * @param instanceConfiguration instance configuration properties
   * @param pkiCredentialFactory the pkcs11 provider if such provider is configured (or null)
   * @param basicServiceConfig basic service configuration data
   * @param caRepositoryMap CA repositories for each instance
   * @return {@link CAServices}
   * @throws IOException error parsing data
   * @throws CMSException error handling CMS data
   * @throws CertificateException error parsing certificate data
   */
  @Bean CAServices caServices(InstanceConfiguration instanceConfiguration, PkiCredentialFactory pkiCredentialFactory,
    BasicServiceConfig basicServiceConfig, Map<String, CARepository> caRepositoryMap
  ) throws IOException, CMSException, CertificateException {
    CAServices caServices = new SignServiceCAInstances(instanceConfiguration, pkiCredentialFactory, basicServiceConfig,
      caRepositoryMap, applicationEventPublisher);
    return caServices;
  }

  @Override public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Provides CA repository implementations for each instance
   *
   * @param basicServiceConfig basic service configuration
   * @param instanceConfiguration configuration properties for each instance
   * @return map of {@link CARepository} for each instance
   * @throws IOException error parsing data
   */
  @DependsOn("BasicServiceConfig")
  @Bean Map<String, CARepository> fileCaRepositoryMap(
    BasicServiceConfig basicServiceConfig,
    InstanceConfiguration instanceConfiguration,
    StorageCryptoConfiguration cryptoConfiguration
  ) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    Map<String, CAConfigData> instanceConfigMap = instanceConfiguration.getInstanceConfigMap();
    Set<String> instances = instanceConfigMap.keySet();
    Map<String, CARepository> caRepositoryMap = new HashMap<>();
    for (String instance : instances) {
      CAConfigData caConfigData = instanceConfigMap.get(instance);
      String customCertStorageLocation = caConfigData.getCa().getCustomCertStorageLocation();
      File repositoryDir = new File(basicServiceConfig.getDataStoreLocation(), "instances/" + instance + "/repository");
      File certStorageDir = customCertStorageLocation == null
        ? new File(repositoryDir, "certStorage")
        : new File(customCertStorageLocation);
      if (customCertStorageLocation == null && !certStorageDir.exists()){
        // If storage is not specified by custom settings, then create the storage dir in the instance folder.
        certStorageDir.mkdirs();
      }
      File crlFile = new File(repositoryDir, instance + ".crl");
      StorageEncryption encryption = getStorageEncryption(cryptoConfiguration, instance);
      CARepoStorage storage = new DefaultCARepoStorage(certStorageDir, new File(repositoryDir, "revoked.json"), encryption);
      CARepository caRepository = new StorageOnlyCARepository(storage, crlFile);
      caRepositoryMap.put(instance, caRepository);
    }
    return caRepositoryMap;
  }

  private StorageEncryption getStorageEncryption(final @Nonnull StorageCryptoConfiguration cryptoConfiguration,
    String instance) throws NoSuchAlgorithmException, InvalidKeySpecException {
    log.info("Setting up storage encryption for instance {}", instance);
    Map<String, StorageCryptoConfiguration.InstanceStorageCryptoConfig> instanceCrypto = cryptoConfiguration.getStorageCrypto();
    if (instanceCrypto == null || !instanceCrypto.containsKey(instance)) {
      log.info("No crypto configuration found. Storage encryption disabled");
      return null;
    }
    StorageCryptoConfiguration.InstanceStorageCryptoConfig instanceCryptoConfig = instanceCrypto.get(instance);
    if (!instanceCryptoConfig.getEnabled()) {
      log.info("Storage encryption disabled by configuration");
      return null;
    }

    boolean hasKeyLen = instanceCryptoConfig.getKeyLength() != null;
    boolean hasIterations = instanceCryptoConfig.getIterations() != null;

    if (hasKeyLen && hasIterations) {
      log.info("Setting up encryption with custom key size={} and custom iterations={}",
        instanceCryptoConfig.getKeyLength(), instanceCryptoConfig.getIterations());
      return new DefaultStorageEncryption(
        instanceCryptoConfig.getKey().toCharArray(),
        instanceCryptoConfig.getKid(),
        Base64.decode(instanceCryptoConfig.getSalt()),
        instanceCryptoConfig.getKeyLength(),
        instanceCryptoConfig.getIterations());
    }
    if (hasKeyLen) {
      log.info("Setting up encryption with custom key size={}", instanceCryptoConfig.getKeyLength());
      return new DefaultStorageEncryption(
        instanceCryptoConfig.getKey().toCharArray(),
        instanceCryptoConfig.getKid(),
        Base64.decode(instanceCryptoConfig.getSalt()),
        instanceCryptoConfig.getKeyLength());
    }
    if (hasIterations) {
      log.info("Setting up encryption with custom iterations={}", instanceCryptoConfig.getIterations());
      return new DefaultStorageEncryption(
        instanceCryptoConfig.getKey().toCharArray(),
        instanceCryptoConfig.getKid(),
        Base64.decode(instanceCryptoConfig.getSalt()),
        128,
        instanceCryptoConfig.getIterations());
    }
    log.info("Setting up encryption with default key size and interations");
    return new DefaultStorageEncryption(
      instanceCryptoConfig.getKey().toCharArray(),
      instanceCryptoConfig.getKid(),
      Base64.decode(instanceCryptoConfig.getSalt()));
  }
}
