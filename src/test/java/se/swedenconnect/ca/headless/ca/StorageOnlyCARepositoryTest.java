package se.swedenconnect.ca.headless.ca;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.cryptacular.util.CertUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.headless.TestData;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultCARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.impl.DefaultStorageEncryption;

import java.io.File;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the main CA repository implementation
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
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