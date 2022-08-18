/*
 * Copyright (c) 2021. Agency for Digital Government (DIGG)
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

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.CRLNumber;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import se.swedenconnect.ca.engine.ca.repository.CARepository;
import se.swedenconnect.ca.engine.ca.repository.CertificateRecord;
import se.swedenconnect.ca.engine.ca.repository.SortBy;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.CRLRevocationDataProvider;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.headless.ca.storage.CARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.CertificateStorageException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 * CA repository for high volume setups where no access to previously issued certificates is provided through
 * this API. Access to issued certificates must be handled by direct access to the repository data
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class HighVolumeCARepository implements CARepository, CRLRevocationDataProvider {

  private final CARepoStorage storage;
  private final File crlFile;
  private BigInteger crlNumber;

  public HighVolumeCARepository(CARepoStorage storage, File crlFile) throws IOException {
    this.storage = storage;
    this.crlFile = crlFile;

    // Get crlNumber
    if (!crlFile.exists()) {
      this.crlNumber = BigInteger.ZERO;
      log.info("Starting new CRL sequence with CRL number 0");
    }
    else {
      crlNumber = getCRLNumberFromCRL();
      log.info("CRL number counter initialized with CRL number {}", crlNumber.toString());
    }
  }

  private BigInteger getCRLNumberFromCRL() throws IOException {
    X509CRLHolder crlHolder = new X509CRLHolder(new FileInputStream(crlFile));
    Extension crlNumberExtension = crlHolder.getExtension(Extension.cRLNumber);
    CRLNumber crlNumberFromCrl = CRLNumber.getInstance(crlNumberExtension.getParsedValue());
    return crlNumberFromCrl.getCRLNumber();
  }

  /** {@inheritDoc} */
  @Override public List<BigInteger> getAllCertificates() {
    return Collections.emptyList();
  }

  /** {@inheritDoc} */
  @Override public CertificateRecord getCertificate(BigInteger bigInteger) {
    return null;
  }

  /** {@inheritDoc} */
  @Override public CRLRevocationDataProvider getCRLRevocationDataProvider() {
    return this;
  }

  /** {@inheritDoc} */
  @Override public int getCertificateCount(boolean notRevoked) {
    return 0;
  }

  /** {@inheritDoc} */
  @Override public List<CertificateRecord> getCertificateRange(int page, int pageSize, boolean notRevoked, SortBy sortBy, boolean descending) {
    return Collections.emptyList();
  }

  @Override public List<RevokedCertificate> getRevokedCertificates() {
    return storage.getRevokedCertificates();
  }

  @Override public BigInteger getNextCrlNumber() {
    crlNumber = crlNumber.add(BigInteger.ONE);
    return crlNumber;
  }

  @SneakyThrows @Override public void publishNewCrl(X509CRLHolder crl) {
    FileUtils.writeByteArrayToFile(crlFile, crl.getEncoded());
  }

  @Override public X509CRLHolder getCurrentCrl() {
    try {
      return new X509CRLHolder(new FileInputStream(crlFile));
    }
    catch (Exception e) {
      log.debug("No current CRL is available. Returning null");
      return null;
    }
  }

  /*
   * From this point we only deal with functions that updates the repository
   */


  /** {@inheritDoc} */
  @Override public void addCertificate(final @Nonnull X509CertificateHolder certificate) throws IOException {
    try {
      internalRepositoryUpdate(UpdateType.addCert, new Object[]{certificate});
    }
    catch (IOException e) {
      throw e instanceof IOException
        ? (IOException) e
        : new IOException(e);
    }
    catch (CertificateStorageException e) {
      // This is a critical error that should require the system to stop
      e.printStackTrace();
      // Throw an unchecked runtime exception
      throw new RuntimeException(e);
    }
  }

  private void internalAddCertificate(final @Nonnull X509CertificateHolder certificate)
    throws IOException, CertificateStorageException {
    Objects.requireNonNull(certificate, "Certificate must no be null");
    storage.storeCertificate(certificate.getEncoded());
  }

  /** {@inheritDoc} */
  @Override public void revokeCertificate(final @Nonnull BigInteger serialNumber, final int reason, final @Nullable Date revocationTime) throws CertificateRevocationException {
    try {
      internalRepositoryUpdate(UpdateType.revokeCert, new Object[]{serialNumber, reason, revocationTime});
    }
    catch (Exception e) {
      throw e instanceof CertificateRevocationException
        ? (CertificateRevocationException) e
        : new CertificateRevocationException(e);
    }
  }

  private void internalRevokeCertificate(final @Nonnull BigInteger serialNumber, final int reason, final @Nullable Date revocationTime) throws CertificateRevocationException {
    Objects.requireNonNull(serialNumber, "Serial number must not be null");
    CertificateRecord certificateRecord = getCertificate(serialNumber);
    if (certificateRecord == null) {
      throw new CertificateRevocationException("No such certificate (" + serialNumber.toString(16) + ")");
    }
    RevokedCertificate revokedCertificate = new RevokedCertificate(serialNumber, revocationTime, reason);
    storage.revokeCertificate(revokedCertificate);
  }

  /** {@inheritDoc} */
  @Override public List<BigInteger> removeExpiredCerts(final int gracePeriodSeconds){
    // This function has no meaning in this repository.
    return Collections.emptyList();
  }

  /**
   * All requests to modify the CA repository must go through this function to ensure that all updates are thread safe
   * @param updateType type of repository update
   * @param args input arguments to the update request
   * @throws Exception On errors performing the update request
   */
  private synchronized void internalRepositoryUpdate(UpdateType updateType, Object[] args)
    throws IOException, CertificateStorageException {
    switch (updateType){
    case addCert:
      internalAddCertificate((X509CertificateHolder) args[0]);
      return;
    case revokeCert:
      internalRevokeCertificate((BigInteger) args[0], (int) args[1], (Date) args[2]);
      return;
    }
    throw new IOException("Unsupported action");
  }

  public enum UpdateType {
    addCert, revokeCert
  }



}
