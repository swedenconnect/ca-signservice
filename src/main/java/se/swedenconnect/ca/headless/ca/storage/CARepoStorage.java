package se.swedenconnect.ca.headless.ca.storage;

import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;

import java.io.IOException;
import java.util.List;

/**
 * Description
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public interface CARepoStorage {

  /**
   * Store a certificate
   *
   * @param certificateBytes certificate bytes
   * @throws IOException error processing the data
   * @throws CertificateStorageException critical error attempting to write the certificate to the storage
   */
  void storeCertificate(byte[] certificateBytes) throws IOException, CertificateStorageException;

  /**
   * Revoke a certificate by updating the certificate revocation file
   *
   * <p>A certificate can only be revoked once unless the reason i certificateHold. In such case
   * the certificate can be revoked again with an updated reason code</p>
   *
   * @param revokedCertificate information about the certificate to be revoked
   * @throws CertificateRevocationException
   */
  void revokeCertificate(RevokedCertificate revokedCertificate) throws CertificateRevocationException;

  List<RevokedCertificate> getRevokedCertificates();

  boolean isCriticalStorageError();



}
