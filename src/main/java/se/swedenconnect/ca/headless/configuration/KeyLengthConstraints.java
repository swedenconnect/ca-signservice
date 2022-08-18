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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import se.swedenconnect.ca.service.base.configuration.keys.PublicKeyPolicyException;
import se.swedenconnect.ca.service.base.configuration.keys.PublicKeyValidator;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;

/**
 * Implementation of key constraint policy enforcement restricting keys allowed to be certified by this service
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Component
@Slf4j
public class KeyLengthConstraints implements PublicKeyValidator {

  @Value("${ca-service.policy.rsa-keys-allowed:true}") boolean allowRsa;
  @Value("${ca-service.policy.rsa-min-key-len:2048}") int minRsaLen;
  @Value("${ca-service.policy.ec-keys-allowed:true}") boolean allowEc;
  @Value("${ca-service.policy.ec-min-key-len:256}") int minEcLen;

  /** {@inheritDoc} */
  @Override public void validatePublicKey(final PublicKey publicKey) throws PublicKeyPolicyException {

    if (publicKey == null){
      throw new PublicKeyPolicyException("Null public key");
    }

    if (publicKey instanceof RSAPublicKey){
      if (!allowRsa){
        throw new PublicKeyPolicyException("RSA public keys keys are not allowed");
      }
      final RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
      final int keyLen = rsaPublicKey.getModulus().bitLength();
      if (keyLen < minRsaLen){
        throw new PublicKeyPolicyException("RSA key length of " + keyLen + " bits is to short (Minimum requirement is " + minRsaLen + ")");
      }
      return;
    }

    if (publicKey instanceof ECPublicKey) {
      if (!allowEc){
        throw new PublicKeyPolicyException("Elliptic Curve (EC) public keys keys are not allowed");
      }
      final ECPublicKey ecPublicKey = (ECPublicKey) publicKey;
      final ECParameterSpec spec = ecPublicKey.getParams();
      final int keyLen = spec.getOrder().bitLength();

      if (keyLen < minEcLen) {
        throw new PublicKeyPolicyException("Elliptic Curve (EC) key length of " + keyLen + " bits is to short (Minimum requirement is " + minEcLen + ")");
      }
      return;
    }
    throw new PublicKeyPolicyException("Public key type " + publicKey.getClass().getName() + "is not supported");
  }
}
