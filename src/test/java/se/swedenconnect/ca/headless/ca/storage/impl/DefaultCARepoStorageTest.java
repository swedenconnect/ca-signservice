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
package se.swedenconnect.ca.headless.ca.storage.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.util.encoders.Base64;
import org.cryptacular.util.CertUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.extern.slf4j.Slf4j;
import se.swedenconnect.ca.engine.ca.attribute.CertAttributes;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.headless.TestData;
import se.swedenconnect.ca.headless.ca.storage.CARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.CertificateStorageException;
import se.swedenconnect.ca.headless.ca.storage.StorageEncryption;
import se.swedenconnect.ca.headless.ca.storage.StoredCertificateIterator;
import se.swedenconnect.ca.headless.ca.storage.data.StorageRecord;
import se.swedenconnect.ca.headless.utils.CertNameUtils;

@Slf4j
public class DefaultCARepoStorageTest {

  private static File storageDataDir;
  private static StorageEncryption encrypt1;
  private static StorageEncryption encrypt2;
  private static StorageEncryption decrypter;
  private static Map<String, String> keyMap;

  @BeforeAll
  static void init() throws Exception {
    storageDataDir = new File(System.getProperty("user.dir"), "target/test-data/storage");
    encrypt1 = new DefaultStorageEncryption("s3cr3t".toCharArray(), "key01", "salt".getBytes());
    encrypt2 = new DefaultStorageEncryption("s3cr3t2".toCharArray(), "key02", "salt".getBytes());
    decrypter = new DefaultStorageEncryption("s3cr3t3".toCharArray(), "key03", "salt".getBytes());
    keyMap = new HashMap<>();
    keyMap.put("key01", "s3cr3t");
    keyMap.put("key02", "s3cr3t2");
    keyMap.put("key03", "s3cr3t3");
  }

  @Test
  void storeCertificate() throws Exception {

    CARepoStorage storageNoCrypt = new DefaultCARepoStorage(createDir("noCrypt"),
      new File(new File(storageDataDir, "noCrypt"), "revoked.json")
      , null);
    CARepoStorage storageCrypt = new DefaultCARepoStorage(createDir("crypt"),
      new File(new File(storageDataDir, "crypt"), "revoked.json")
      ,encrypt1);

    // Store certs
    storeCert("Storing cert 1 - no encryption", storageNoCrypt, TestData.CERT_1, null);
    storeCert("Storing cert 2 - no encryption", storageNoCrypt, TestData.CERT_2, null);
    storeCert("Storing cert 1 - encryption", storageCrypt, TestData.CERT_1, null);
    storeCert("Storing cert 2 - encryption", storageCrypt, TestData.CERT_2, null);

    // Revoke certs
    revokeCert("Revoking cert 1 - no crypt", storageNoCrypt, getSerial(TestData.CERT_1), new Date(), CRLReason.unspecified, null);
    revokeCert("Revoking cert 2 - no crypt", storageNoCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.unspecified, null);
    revokeCert("Revoking cert 2 - crypt", storageCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.certificateHold, null);

    // instantiate over existing storage dir
    storageNoCrypt = new DefaultCARepoStorage(new File(storageDataDir, "noCrypt"),
      new File(new File(storageDataDir, "noCrypt"), "revoked.json")
      ,null);
    storageCrypt = new DefaultCARepoStorage(new File(storageDataDir, "crypt"),
      new File(new File(storageDataDir, "crypt"), "revoked.json")
      , encrypt2);

    List<RevokedCertificate> revokedCertificatesNoCrypt = storageNoCrypt.getRevokedCertificates();
    assertEquals(2, revokedCertificatesNoCrypt.size());
    List<RevokedCertificate> revokedCertificatesCrypt = storageCrypt.getRevokedCertificates();
    assertEquals(1, revokedCertificatesCrypt.size());

    // Revoke already revoked
    // Removing from certificateHold should work
    revokeCert("Un-revoking cert 2 - crypt", storageCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.removeFromCRL, null);
    // Revoking it again with negative reason code should not work
    revokeCert("Revoking cert 2 again with removal request - crypt", storageCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.removeFromCRL, CertificateRevocationException.class);
    // Revoking it again with certificateHold reason should work
    revokeCert("Revoking cert 2 again with certificate Hold - crypt", storageCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.certificateHold, null);
    // Upgrading on hold should work
    revokeCert("Revoking cert 2 again with permanent reason - crypt", storageCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.keyCompromise, null);
    revokedCertificatesCrypt = storageCrypt.getRevokedCertificates();
    assertEquals(CRLReason.keyCompromise, revokedCertificatesCrypt.get(0).getReason());
    // Revoking already revoked should not work
    revokeCert("Revoking cert 2 - no crypt", storageNoCrypt, getSerial(TestData.CERT_2), new Date(), CRLReason.unspecified, CertificateRevocationException.class);

    //Test bad storage request
    storeCert("Bad storage request", storageCrypt, Base64.toBase64String("badCert".getBytes()), IOException.class);

    //Read info from storage
    File storageFile = getStorageFile("crypt");
    try(StoredCertificateIterator iterator = StoredCertificateIterator.getInstance(storageFile)){
      int cnt = 0;
      while (iterator.hasNext()){
        StorageRecord storageRecord = iterator.next();
        cnt++;
        log.info("Extracted certificate number {} from encrypted storage file", cnt);
        assertNull(storageRecord.getCert());
        assertNull(storageRecord.getId());
        String certId = new String(decrypter.decryptData(Base64.decode(storageRecord.getEId()), false, keyMap), StandardCharsets.UTF_8);
        log.info("Decrypted certId: {}", certId);
        byte[] certBytes = decrypter.decryptData(Base64.decode(storageRecord.getECert()), true, keyMap);
        X509Certificate certificate = CertUtil.decodeCertificate(certBytes);
        log.info("Decrypted certificate from record issued to: {}", certificate.getSubjectX500Principal());
        assertEquals(certId, CertNameUtils.getSubjectAttributes(certificate).get(CertAttributes.SERIALNUMBER));
      }
      assertEquals(2, cnt);
      log.info("Total number of certificates: {}", cnt);
    }

    //Read info from storage - not encrypted
    storageFile = getStorageFile("noCrypt");
    try(StoredCertificateIterator iterator = StoredCertificateIterator.getInstance(storageFile)){
      int cnt = 0;
      while (iterator.hasNext()){
        StorageRecord storageRecord = iterator.next();
        cnt++;
        log.info("Extracted certificate number {} from cleartext storage file", cnt);
        assertNull(storageRecord.getECert());
        assertNull(storageRecord.getEId());
        String certId = storageRecord.getId();
        log.info("Cleartext certId: {}", certId);
        X509Certificate certificate = CertUtil.decodeCertificate(Base64.decode(storageRecord.getCert()));
        log.info("Stored certificate found issued to: {}", certificate.getSubjectX500Principal());
        assertEquals(certId, CertNameUtils.getSubjectAttributes(certificate).get(CertAttributes.SERIALNUMBER));
      }
      assertEquals(2, cnt);
      log.info("Total number of certificates: {}", cnt);
    }

    // Finally force a storage failure by deleting the storage dir
    FileUtils.copyDirectory(new File(storageDataDir, "noCrypt"), new File(storageDataDir, "error"));
    DefaultCARepoStorage errorStorage = new DefaultCARepoStorage(
      new File(storageDataDir, "error"),
      new File(new File(storageDataDir, "error"), "revoked.json")
      , null);
    assertFalse(errorStorage.isCriticalStorageError());
    FileUtils.forceDelete(new File(storageDataDir, "error"));
    // With storage dir removed, we should get a critical error
    storeCert("Storing cert to deleted storage directory", errorStorage, TestData.CERT_1, CertificateStorageException.class);
    assertTrue(errorStorage.isCriticalStorageError());

    // Finally, ensure that storage to a storage service with previous critical error is rejected
    ReflectionTestUtils.setField(storageNoCrypt, "criticalStorageError", true);
    storeCert("Storing cert to storage with critical error", storageNoCrypt, TestData.CERT_1, CertificateStorageException.class);
  }

  private File getStorageFile(String dirName) {
    File storageDir = new File(storageDataDir, dirName);
    return new File(storageDir, "certStore-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
  }

  private BigInteger getSerial(String b64Cert) {
    X509Certificate certificate = CertUtil.decodeCertificate(TestData.getCertBytes(b64Cert));
    return certificate.getSerialNumber();
  }

  private void revokeCert(String message, CARepoStorage storage, BigInteger certSerial, Date revDate, int reason,
    Class<? extends Exception> exceptionClass) throws CertificateRevocationException {
    log.info("Revoke test - {}", message);
    if (exceptionClass != null) {
      Exception exception = assertThrows(exceptionClass, () -> storage.revokeCertificate(
        new RevokedCertificate(certSerial, revDate, reason)));
      log.info("Caught expected exception: {}", exception.toString());
      return;
    }

    log.info("Revoking certificate with serial number {}", certSerial.toString(16));
    storage.revokeCertificate(new RevokedCertificate(certSerial, revDate, reason));
  }

  private void storeCert(String message, CARepoStorage storage, String b64Cert,
    Class<? extends Exception> exceptionClass) throws Exception {
    log.info("Storage test - {}", message);
    byte[] certBytes = Base64.decode(b64Cert);

    if (exceptionClass != null) {
      Exception exception = assertThrows(exceptionClass, () -> storage.storeCertificate(certBytes));
      log.info("Caught expected exception: {}", exception.toString());
      return;
    }

    X509Certificate certificate = CertUtil.decodeCertificate(TestData.getCertBytes(b64Cert));
    log.info("Storing cert issued to {}", certificate.getSubjectX500Principal());
    storage.storeCertificate(certificate.getEncoded());
  }

  @Test
  void revokeCertificate() {
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