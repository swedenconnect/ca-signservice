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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import se.swedenconnect.ca.cmc.api.impl.DefaultCMCCaApi;
import se.swedenconnect.ca.cmc.auth.impl.DefaultCMCReplayChecker;
import se.swedenconnect.ca.service.base.configuration.cmc.CMCApiProvider;
import se.swedenconnect.ca.service.base.configuration.cmc.CMCReplayCheckerProvider;

/**
 * Configuration class providing beans for CMC API support
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
@Configuration
public class CMCComponentConfiguration {

  /**
   * Provides a CMC replay checker provider providing replay checkers for CMC API instance creation
   * @return {@link CMCReplayCheckerProvider}
   */
  @Bean CMCReplayCheckerProvider cmcReplayChecker() {
    return (instance) -> new DefaultCMCReplayChecker();
  }

  /**
   * Provider generating instances of CMC API implementations
   * @return {@link CMCApiProvider}
   */
  @Bean CMCApiProvider cmcApiProvider() {
    return (instance, caService, requestParser, responseFactory) -> new DefaultCMCCaApi(caService, requestParser, responseFactory);
  }

}
