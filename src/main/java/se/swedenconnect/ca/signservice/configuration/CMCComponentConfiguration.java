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
package se.swedenconnect.ca.signservice.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.context.annotation.DependsOn;
import se.swedenconnect.ca.cmc.api.impl.DefaultCMCCaApi;
import se.swedenconnect.ca.cmc.auth.impl.DefaultCMCReplayChecker;
import se.swedenconnect.ca.engine.ca.issuer.CAService;
import se.swedenconnect.ca.engine.ca.models.cert.CertificateModelPolicy;
import se.swedenconnect.ca.service.base.ca.CAServices;
import se.swedenconnect.ca.service.base.ca.policy.CrlDPUrlPolicy;
import se.swedenconnect.ca.service.base.ca.policy.OCSPLocationPolicy;
import se.swedenconnect.ca.service.base.configuration.cmc.CMCApiProvider;
import se.swedenconnect.ca.service.base.configuration.cmc.CMCReplayCheckerProvider;
import se.swedenconnect.ca.service.base.configuration.instance.InstanceConfiguration;
import se.swedenconnect.ca.service.base.configuration.properties.CAConfigData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Configuration class providing beans for CMC API support
 */
@Configuration
public class CMCComponentConfiguration {

  /**
   * Provides a CMC replay checker provider providing replay checkers for CMC API instance creation
   *
   * @return {@link CMCReplayCheckerProvider}
   */
  @Bean
  public CMCReplayCheckerProvider cmcReplayChecker() {
    return (instance) -> new DefaultCMCReplayChecker();
  }

  /**
   * Provider generating instances of CMC API implementations
   * @return {@link CMCApiProvider}
   */
  @Bean CMCApiProvider cmcApiProvider() {
    return (instance, caService, requestParser, responseFactory, policies) -> new DefaultCMCCaApi(caService, requestParser, responseFactory, policies);
  }

  @DependsOn("BasicServiceConfig")
  @Bean
  Map<String, List<CertificateModelPolicy>> certificateModelPolicies(
      CAServices caServices, final InstanceConfiguration instanceConfiguration) {
    Map<String, List<CertificateModelPolicy>> policies = new HashMap<>();
    final List<String> caServiceKeys = caServices.getCAServiceKeys();
    for (final String instanceKey : caServiceKeys) {
      List<CertificateModelPolicy> certificateModelPolicies = new ArrayList<>();
      final CAService caService = caServices.getCAService(instanceKey);
      final CAConfigData caConfigData = instanceConfiguration.getInstanceConfigMap().get(instanceKey);
      final List<String> certReqPolicies = Optional.ofNullable(caConfigData.getCa().getCertReqPolicies()).orElse(new ArrayList<>());
      if (certReqPolicies.contains(CertificateModelPolicy.CRL_DP_POLICY)) {
        certificateModelPolicies.add(new CrlDPUrlPolicy(caConfigData, caService));
      }
      if (certReqPolicies.contains(CertificateModelPolicy.OCSP_URL_POLICY)) {
        certificateModelPolicies.add(new OCSPLocationPolicy(caConfigData, caService));
      }
      policies.put(instanceKey, certificateModelPolicies);
    }
    return policies;
  }

}
