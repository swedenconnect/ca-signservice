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

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.headless.ca.SignServiceCAInstances;
import se.swedenconnect.ca.headless.ca.StorageOnlyCARepository;
import se.swedenconnect.ca.headless.ca.storage.CARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.StorageEncryption;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultCARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultStorageEncryption;
import se.swedenconnect.ca.service.base.configuration.BasicServiceConfig;
import se.swedenconnect.ca.service.base.ca.CAServices;
import se.swedenconnect.ca.service.base.configuration.instance.InstanceConfiguration;
import se.swedenconnect.ca.service.base.configuration.keys.PkiCredentialFactory;
import se.swedenconnect.ca.service.base.configuration.properties.CAConfigData;

/**
 * CA Service configuration class that generates the CA services beans for each CA instance as defined by the
 * configuration properties for each instance.
 */
@Slf4j
@Configuration
public class CAServiceConfiguration implements ApplicationEventPublisherAware {

  private ApplicationEventPublisher applicationEventPublisher;

  /**
   * The CA services bean provide all CA services as defined by the configuration of each instance.
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
  @Bean
  CAServices caServices(final InstanceConfiguration instanceConfiguration,
      final PkiCredentialFactory pkiCredentialFactory, final BasicServiceConfig basicServiceConfig,
      final Map<String, CARepository> caRepositoryMap) throws IOException, CMSException, CertificateException {
    final CAServices caServices =
        new SignServiceCAInstances(instanceConfiguration, pkiCredentialFactory, basicServiceConfig,
            caRepositoryMap, this.applicationEventPublisher);
    return caServices;
  }

  @Override
  public void setApplicationEventPublisher(final ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Provides CA repository implementations for each instance.
   *
   * @param basicServiceConfig basic service configuration
   * @param instanceConfiguration configuration properties for each instance
   * @return map of {@link CARepository} for each instance
   * @throws IOException error parsing data
   */
  @DependsOn("BasicServiceConfig")
  @Bean
  Map<String, CARepository> fileCaRepositoryMap(
      final BasicServiceConfig basicServiceConfig,
      final InstanceConfiguration instanceConfiguration,
      final StorageCryptoConfiguration cryptoConfiguration)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
    final Map<String, CAConfigData> instanceConfigMap = instanceConfiguration.getInstanceConfigMap();
    final Set<String> instances = instanceConfigMap.keySet();
    final Map<String, CARepository> caRepositoryMap = new HashMap<>();
    for (final String instance : instances) {
      final CAConfigData caConfigData = instanceConfigMap.get(instance);
      final String customCertStorageLocation = caConfigData.getCa().getCustomCertStorageLocation();
      final File repositoryDir =
          new File(basicServiceConfig.getDataStoreLocation(), "instances/" + instance + "/repository");
      final File certStorageDir = customCertStorageLocation == null
          ? new File(repositoryDir, "certStorage")
          : new File(customCertStorageLocation);
      if (customCertStorageLocation == null && !certStorageDir.exists()) {
        // If storage is not specified by custom settings, then create the storage dir in the instance folder.
        certStorageDir.mkdirs();
      }
      final File crlFile = new File(repositoryDir, instance + ".crl");
      final StorageEncryption encryption = this.getStorageEncryption(cryptoConfiguration, instance);
      final CARepoStorage storage =
          new DefaultCARepoStorage(certStorageDir, new File(repositoryDir, "revoked.json"), encryption);
      final CARepository caRepository = new StorageOnlyCARepository(storage, crlFile);
      caRepositoryMap.put(instance, caRepository);
    }
    return caRepositoryMap;
  }

  private StorageEncryption getStorageEncryption(final @Nonnull StorageCryptoConfiguration cryptoConfiguration,
      final String instance) throws NoSuchAlgorithmException, InvalidKeySpecException {
    log.info("Setting up storage encryption for instance {}", instance);
    final Map<String, StorageCryptoConfiguration.InstanceStorageCryptoConfig> instanceCrypto =
        cryptoConfiguration.getStorageCrypto();
    if (instanceCrypto == null || !instanceCrypto.containsKey(instance)) {
      log.info("No crypto configuration found. Storage encryption disabled");
      return null;
    }
    final StorageCryptoConfiguration.InstanceStorageCryptoConfig instanceCryptoConfig = instanceCrypto.get(instance);
    if (!instanceCryptoConfig.getEnabled()) {
      log.info("Storage encryption disabled by configuration");
      return null;
    }

    final boolean hasKeyLen = instanceCryptoConfig.getKeyLength() != null;
    final boolean hasIterations = instanceCryptoConfig.getIterations() != null;

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
