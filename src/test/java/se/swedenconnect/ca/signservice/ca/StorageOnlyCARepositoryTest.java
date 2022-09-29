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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.cryptacular.util.CertUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.signservice.TestData;
import se.swedenconnect.ca.signservice.ca.storage.impl.DefaultCARepoStorage;
import se.swedenconnect.ca.signservice.ca.storage.impl.DefaultStorageEncryption;

/**
 * Tests for the main CA repository implementation.
 */
class StorageOnlyCARepositoryTest {

  private static File storageDataDir;
  private static X509Certificate cert1;
  private static X509Certificate cert2;

  @BeforeAll
  static void init(){
    storageDataDir = new File(System.getProperty("user.dir"), "target/test-data/storage");
    cert1 = CertUtil.decodeCertificate(TestData.getCertBytes(TestData.CERT_1));
    cert2 = CertUtil.decodeCertificate(TestData.getCertBytes(TestData.CERT_2));
  }

  @Test
  void addCertificate() throws Exception {

    File repoDir = createDir("repo-1");
    File crlFile = new File(repoDir, "crl");

    StorageOnlyCARepository caRepository = new StorageOnlyCARepository(new DefaultCARepoStorage(repoDir,
      new File(repoDir, "revoked.json"),
      new DefaultStorageEncryption(
      "s3cr3t".toCharArray(), "01", "salt".getBytes()
    )), crlFile);

    caRepository.addCertificate(new JcaX509CertificateHolder(cert1));
    caRepository.addCertificate(new JcaX509CertificateHolder(cert2));
    Instant revocationTime = Instant.now();

    caRepository.revokeCertificate(cert1.getSerialNumber(), CRLReason.unspecified, Date.from(revocationTime));

    List<RevokedCertificate> revokedCertificates = caRepository.getRevokedCertificates();

    assertEquals(1, revokedCertificates.size());
    assertEquals(cert1.getSerialNumber(), revokedCertificates.get(0).getCertificateSerialNumber());
    LocalDateTime revLdt = LocalDateTime.ofInstant(revocationTime, ZoneId.systemDefault());
    String dateStr = revLdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    LocalDateTime parsedRevLdt = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    Date paredRevDate = Date.from(parsedRevLdt.atZone(ZoneId.systemDefault()).toInstant());
    assertEquals(paredRevDate, revokedCertificates.get(0).getRevocationTime());
  }

  private File createDir(String name) throws Exception {
    File dir = new File(storageDataDir, name);
    if (dir.exists()) {
      FileUtils.forceDelete(dir);
    }
    dir.mkdirs();
    return dir;
  }


}