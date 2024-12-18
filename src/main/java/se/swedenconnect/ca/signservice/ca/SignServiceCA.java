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
package se.swedenconnect.ca.signservice.ca;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.cert.X509CertificateHolder;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.engine.ca.issuer.CertificateIssuanceException;
import se.swedenconnect.ca.engine.ca.issuer.CertificateIssuerModel;
import se.swedenconnect.ca.engine.ca.models.cert.CertNameModel;
import se.swedenconnect.ca.engine.ca.models.cert.impl.DefaultCertificateModelBuilder;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.CRLIssuerModel;
import se.swedenconnect.ca.service.base.ca.impl.AbstractBasicCA;
import se.swedenconnect.security.credential.PkiCredential;

/**
 * The implementation of a CA instance.
 */
@Slf4j
public class SignServiceCA extends AbstractBasicCA {

  public SignServiceCA(final PkiCredential issuerCredential,
      final CARepository caRepository, final CertificateIssuerModel certIssuerModel,
      final CRLIssuerModel crlIssuerModel, final List<String> crlDistributionPoints)
      throws NoSuchAlgorithmException, IOException, CertificateEncodingException {
    super(issuerCredential, caRepository, certIssuerModel, crlIssuerModel, crlDistributionPoints);
    log.info("Instantiated Headless CA service instance");
  }

  @Override
  protected DefaultCertificateModelBuilder getBaseCertificateModelBuilder(final CertNameModel<?> subject,
      final PublicKey publicKey, final X509CertificateHolder issuerCertificate,
      final CertificateIssuerModel certificateIssuerModel) throws CertificateIssuanceException {
    final DefaultCertificateModelBuilder certModelBuilder =
        DefaultCertificateModelBuilder.getInstance(publicKey, this.getCaCertificate(),
            certificateIssuerModel);
    certModelBuilder
        .subject(subject)
        .includeAki(true)
        .crlDistributionPoints(this.crlDistributionPoints.isEmpty() ? null : this.crlDistributionPoints)
        .ocspServiceUrl(StringUtils.isBlank(this.ocspResponderUrl) ? null : this.ocspResponderUrl);
    return certModelBuilder;
  }

  @Override
  public void revokeCertificate(final BigInteger serialNumber, final int reason, Date revocationDate)
      throws CertificateRevocationException {
    if (revocationDate == null || revocationDate.after(new Date())) {
      revocationDate = new Date();
    }
    this.getCaRepository().revokeCertificate(serialNumber, reason, revocationDate);
  }

}
