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

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * This component enforces configured port restrictions on the admin UI to ensure that the UI is only available in accordance with set
 * policy.
 *
 * This class typically enforces service port only, but may also implement other controls such as IP address whitelisting etc.
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Component
@NoArgsConstructor
@Slf4j
public class ServicePortConstraints {

  @Value("${ca-service.policy.admin.enabled-ui-ports}") int[] enabledUiPorts;

  public void validateRequestPort(HttpServletRequest request) throws IOException {
    if (enabledUiPorts == null) {
      log.error("No UI ports are enabled - Illegal configuration");
      throw new IOException("No UI ports are enabled");
    }
    int localPort = request.getLocalPort();
    for (int enbledPort : enabledUiPorts){
      if (localPort == enbledPort){
        log.trace("UI request to enabled port {}", localPort);
        return;
      }
    }
    log.debug("Blocked access to requested service port {}", localPort);
    throw new IOException("Illegal requested service port" + localPort);
  }

}
