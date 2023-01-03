/*
 * Copyright (c) 2022-2023.  Agency for Digital Government (DIGG)
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
package se.swedenconnect.ca.signservice.ca.storage;

import java.io.IOException;
import java.util.List;

import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;

/**
 * A CA storage repository.
 */
public interface CARepoStorage {

  /**
   * Store a certificate.
   *
   * @param certificateBytes certificate bytes
   * @throws IOException error processing the data
   * @throws CertificateStorageException critical error attempting to write the certificate to the storage
   */
  void storeCertificate(final byte[] certificateBytes) throws IOException, CertificateStorageException;

  /**
   * Revoke a certificate by updating the certificate revocation file.
   *
   * <p>
   * A certificate can only be revoked once unless the reason i certificateHold. In such case the certificate can be
   * revoked again with an updated reason code
   * </p>
   *
   * @param revokedCertificate information about the certificate to be revoked
   * @throws CertificateRevocationException
   */
  void revokeCertificate(final RevokedCertificate revokedCertificate) throws CertificateRevocationException;

  List<RevokedCertificate> getRevokedCertificates();

  boolean isCriticalStorageError();

}
