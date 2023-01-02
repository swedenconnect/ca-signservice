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
package se.swedenconnect.ca.signservice.ca;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.engine.ca.repository.CertificateRecord;
import se.swedenconnect.ca.engine.ca.repository.SortBy;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.CRLMetadata;
import se.swedenconnect.ca.engine.revocation.crl.CRLRevocationDataProvider;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.signservice.ca.storage.CARepoStorage;
import se.swedenconnect.ca.signservice.ca.storage.CertificateStorageException;

/**
 * CA repository for high volume setups where no access to previously issued certificates is provided through this API.
 * Access to issued certificates must be handled by direct access to the repository data
 */
@Slf4j
public class StorageOnlyCARepository implements CARepository, CRLRevocationDataProvider {

  private final CARepoStorage storage;
  private final File crlFile;
  private BigInteger crlNumber;

  public StorageOnlyCARepository(final CARepoStorage storage, final File crlFile) throws IOException {
    this.storage = storage;
    this.crlFile = crlFile;

    // Get crlNumber
    if (!crlFile.exists()) {
      this.crlNumber = BigInteger.ZERO;
      log.info("Starting new CRL sequence with CRL number 0");
    }
    else {
      this.crlNumber = this.getCRLNumberFromCRL();
      log.info("CRL number counter initialized with CRL number {}", this.crlNumber.toString());
    }
  }

  private BigInteger getCRLNumberFromCRL() throws IOException {
    final X509CRLHolder crlHolder = new X509CRLHolder(new FileInputStream(this.crlFile));
    final Extension crlNumberExtension = crlHolder.getExtension(Extension.cRLNumber);
    final CRLNumber crlNumberFromCrl = CRLNumber.getInstance(crlNumberExtension.getParsedValue());
    return crlNumberFromCrl.getCRLNumber();
  }

  /** {@inheritDoc} */
  @Override
  public List<BigInteger> getAllCertificates() {
    return Collections.emptyList();
  }

  /** {@inheritDoc} */
  @Override
  public CertificateRecord getCertificate(final BigInteger bigInteger) {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public CRLRevocationDataProvider getCRLRevocationDataProvider() {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public int getCertificateCount(final boolean notRevoked) {
    return 0;
  }

  /** {@inheritDoc} */
  @Override
  public List<CertificateRecord> getCertificateRange(final int page, final int pageSize, final boolean notRevoked,
      final SortBy sortBy, final boolean descending) {
    return Collections.emptyList();
  }

  @Override
  public List<RevokedCertificate> getRevokedCertificates() {
    return this.storage.getRevokedCertificates();
  }

  @Override
  public BigInteger getNextCrlNumber() {
    this.crlNumber = this.crlNumber.add(BigInteger.ONE);
    return this.crlNumber;
  }

  @SneakyThrows
  @Override
  public void publishNewCrl(final X509CRLHolder crl) {
    FileUtils.writeByteArrayToFile(this.crlFile, crl.getEncoded());
  }

  @Override
  public X509CRLHolder getCurrentCrl() {
    try {
      return new X509CRLHolder(new FileInputStream(this.crlFile));
    }
    catch (final Exception e) {
      log.debug("No current CRL is available. Returning null");
      return null;
    }
  }

  @Override public CRLMetadata getCurrentCRLMetadata() {
    final X509CRLHolder currentCrl = getCurrentCrl();
    if (currentCrl == null) {
      log.debug("No CRL file is available - Resetting CRL metadata to support initial CRL creation");
      // No CRL is available. Return empty metadata to allow initial CRL creation;
      return CRLMetadata.builder()
        .crlNumber(BigInteger.ZERO)
        .issueTime(Instant.ofEpochMilli(0L))
        .nextUpdate(Instant.ofEpochMilli(0L))
        .revokedCertCount(0)
        .build();
    }

    log.debug("Returning CRL metadata from current CRL");
    return CRLMetadata.builder()
      .crlNumber(crlNumber)
      .issueTime(currentCrl.getThisUpdate().toInstant())
      .nextUpdate(currentCrl.getNextUpdate().toInstant())
      .revokedCertCount(currentCrl.getRevokedCertificates().size())
      .build();
  }

  /*
   * From this point we only deal with functions that updates the repository
   */

  /** {@inheritDoc} */
  @Override
  public void addCertificate(final @Nonnull X509CertificateHolder certificate) throws IOException {
    try {
      Objects.requireNonNull(certificate, "Certificate must no be null");
      this.storage.storeCertificate(certificate.getEncoded());
    }
    catch (final CertificateStorageException e) {
      // This is a critical error that should require the system to stop
      e.printStackTrace();
      // Throw an unchecked runtime exception
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void revokeCertificate(final @Nonnull BigInteger serialNumber, final int reason,
      final @Nullable Date revocationTime) throws CertificateRevocationException {
    Objects.requireNonNull(serialNumber, "Serial number must not be null");
    final RevokedCertificate revokedCertificate = new RevokedCertificate(serialNumber, revocationTime, reason);
    this.storage.revokeCertificate(revokedCertificate);
  }

  /** {@inheritDoc} */
  @Override
  public List<BigInteger> removeExpiredCerts(final int gracePeriodSeconds) {
    // This function has no meaning in this repository.
    return Collections.emptyList();
  }

}
