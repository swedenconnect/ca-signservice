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

package se.swedenconnect.ca.headless.ca;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import se.swedenconnect.ca.engine.ca.issuer.CertificateIssuanceException;
import se.swedenconnect.ca.engine.ca.issuer.CertificateIssuerModel;
import se.swedenconnect.ca.engine.ca.models.cert.CertNameModel;
import se.swedenconnect.ca.engine.ca.models.cert.impl.DefaultCertificateModelBuilder;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.engine.ca.repository.CertificateRecord;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.CRLIssuerModel;
import se.swedenconnect.ca.service.base.configuration.instance.ca.AbstractBasicCA;
import se.swedenconnect.security.credential.PkiCredential;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.List;

/**
 * The implementation of a CA instance
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class SignServiceCA extends AbstractBasicCA {

  @Getter private List<X509CertificateHolder> caCertificateChain;

  public SignServiceCA(PkiCredential issuerCredential,
    CARepository caRepository, CertificateIssuerModel certIssuerModel,
    CRLIssuerModel crlIssuerModel, List<String> crlDistributionPoints)
    throws NoSuchAlgorithmException, IOException, CertificateEncodingException {
    super(issuerCredential, caRepository, certIssuerModel, crlIssuerModel, crlDistributionPoints);
    this.caCertificateChain = caCertificateChain;
    log.info("Instantiated Headless CA service instance");
  }

  @Override protected DefaultCertificateModelBuilder getBaseCertificateModelBuilder(CertNameModel subject, PublicKey publicKey,
    X509CertificateHolder issuerCertificate, CertificateIssuerModel certificateIssuerModel) throws CertificateIssuanceException {
    DefaultCertificateModelBuilder certModelBuilder = DefaultCertificateModelBuilder.getInstance(publicKey, getCaCertificate(),
      certificateIssuerModel);
    certModelBuilder
      .subject(subject)
      .includeAki(true)
      .crlDistributionPoints(crlDistributionPoints.isEmpty() ? null : crlDistributionPoints)
      .ocspServiceUrl(StringUtils.isBlank(ocspResponderUrl) ? null : ocspResponderUrl);
    return certModelBuilder;
  }

  @Override
  public void revokeCertificate(BigInteger serialNumber, int reason, Date revocationDate) throws CertificateRevocationException {
    if (revocationDate == null || revocationDate.after(new Date())) {
      revocationDate = new Date();
    }
    getCaRepository().revokeCertificate(serialNumber, reason, revocationDate);
  }

}
