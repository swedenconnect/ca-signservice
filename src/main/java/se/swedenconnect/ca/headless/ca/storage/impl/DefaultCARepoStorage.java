package se.swedenconnect.ca.headless.ca.storage.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.util.encoders.Base64;
import se.swedenconnect.ca.engine.ca.attribute.CertAttributes;
import se.swedenconnect.ca.engine.revocation.CertificateRevocationException;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.headless.ca.storage.CARepoStorage;
import se.swedenconnect.ca.headless.ca.storage.CertificateStorageException;
import se.swedenconnect.ca.headless.ca.storage.StorageEncryption;
import se.swedenconnect.ca.headless.ca.storage.data.RevocationRecord;
import se.swedenconnect.ca.headless.ca.storage.data.StorageRecord;
import se.swedenconnect.ca.headless.utils.CertNameUtils;
import se.swedenconnect.ca.service.base.configuration.keys.BasicX509Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the default implementation of a CA repository storage service.
 *
 * <p>
 * The purpose of this service is to act as a storage diode to which it is possible to incrementally
 * add new certificates and optionally information about revocation, but where there is not way to
 * read out any issued certificates using this implementation.
 * </p>
 *
 * <p>
 * A major difference between this storage service and the general CARepository API is that storage of certificates
 * is separated from storage about revocation information. Revocation information is stored separately in a
 * revocation info yaml file, primarily intended for manual updates directly on the revocation file. There is
 * an optional function to also do revocation using this storage implementation, but this is only provided for
 * generality and possible future extended use of this class.
 * </p>
 *
 * <p>
 * Another difference is that there is no way to obtain information about stored certificates. This allows
 * effective incremental storage without the need to provide indexation and possibilities to search the database.
 * This design is intentional to allow for effective high volume storage.
 * </p>
 *
 * <p>
 * Certificate storage is done in daily certificate storage files tagged with its date in the file name.
 * </p>
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
@Slf4j
public class DefaultCARepoStorage implements CARepoStorage {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter CERT_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter REV_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** optional encryption handler to encrypt and decrypt data */
  private final StorageEncryption encryption;

  private final File storageDirectory;

  private final File revocationFile;

  /**
   * This is a second line defense preventing this CA from keep issuing certificates if there has been a critical error
   * writing issued certificates to the file storage. The primary defence is to throw the
   * {@link se.swedenconnect.ca.headless.ca.storage.CertificateStorageException} that should allow the
   * application to shut down gracefully. However, in case this is not properly implemented, this boolean
   * ensures that this storage module will re-throw the exception instead of allowing any more certificate to be
   * stored.
   */
  @Getter
  private boolean criticalStorageError = false;

  /** The attribute used to extract the id of the certificate subject */
  @Setter private ASN1ObjectIdentifier idAttribute = CertAttributes.SERIALNUMBER;

  /**
   * Constructor
   *
   * @param storageDirectory directory for storing data
   * @param encryption optional encrypter for encrypting data
   * @throws IOException error instantiating this storage service
   */
  public DefaultCARepoStorage(final @Nonnull File storageDirectory, final @Nonnull File revocationFile, final @Nullable StorageEncryption encryption)
    throws IOException {
    this.encryption = encryption;
    this.storageDirectory = storageDirectory;
    if (!storageDirectory.exists()) {
      throw new IOException("Storage directory " + storageDirectory.getAbsolutePath() + " does not exist");
    }
    log.info("Initiated certificate storage at: {}", storageDirectory);
    this.revocationFile = revocationFile;
    log.info("Revocation info file: {}", revocationFile);
    log.info("Initiated storage with {} revoked certificates", getRevocationData().size());
  }

  /** {@inheritDoc} */
  @Override
  public synchronized void storeCertificate(final @Nonnull byte[] certificateBytes)
    throws IOException, CertificateStorageException {
    Objects.requireNonNull(certificateBytes, "Certificate bytes must not be null");

    if (criticalStorageError) {
      // There has been a critical storage error on previous storage attempts. This must be fixed
      // and system must be restarted
      throw new CertificateStorageException(
        "Storage rejected due to previous storage failure. System must be restarted");
    }

    X509Certificate certificate;
    try {
      certificate = BasicX509Utils.getCertificate(certificateBytes);
    }
    catch (CertificateException ex) {
      throw new IOException(ex);
    }

    File storageFile = new File(storageDirectory, "certStore-" + LocalDateTime.now().format(DATE_FORMAT));
    byte[] storageRecordData = getStorageRecordData(certificate);

    // Append file
    try (FileOutputStream fos = new FileOutputStream(storageFile, true)) {
      fos.write(storageRecordData);
    }
    catch (IOException e) {
      // In case of file write error, this is considered an irrecoverable error as it would lead to a certificate being
      // issued without being stored.
      criticalStorageError = true;
      throw new CertificateStorageException(e);
    }

  }

  /** {@inheritDoc} */
  @Override
  public synchronized void revokeCertificate(RevokedCertificate revokedCertificate)
    throws CertificateRevocationException {

    if (revokedCertificate.getReason() > CRLReason.aACompromise) {
      throw new CertificateRevocationException("Illegal reason code");
    }

    List<RevocationRecord> revocationData = new ArrayList<>(getRevocationData());

    boolean modified = false;
    List<String> deleteList = new ArrayList<>();
    for (RevocationRecord record : revocationData) {
      if (record.getSerial().equalsIgnoreCase(revokedCertificate.getCertificateSerialNumber().toString(16))) {
        if (record.getReason() == CRLReason.certificateHold) {
          if (revokedCertificate.getReason() == CRLReason.removeFromCRL) {
            // remove this certificate from certificateHold status
            log.debug("Removing certificate from certificateHold");
            deleteList.add(record.getSerial());
            modified = true;
            break;
          }
          log.debug("Modifying revoked certificate from certificateHold to reason {}", revokedCertificate.getReason());
          record.setReason(revokedCertificate.getReason());
          modified = true;
          break;
        }
        else {
          if (revokedCertificate.getReason() < CRLReason.removeFromCRL) {
            log.debug("Revocation removal request denied since certificate has already been permanently revoked");
            throw new CertificateRevocationException(
              "Certificate is already revoked with reason other than certificate hold");
          }
          log.debug("Revocation request denied since certificate has already been revoked");
          throw new CertificateRevocationException(
            "Certificate is already revoked with reason other than certificate hold");
        }
      }
    }

    if (modified && !deleteList.isEmpty()) {
      // Delete all certificates on delete list
      revocationData = revocationData.stream()
        .filter(revocationRecord -> !deleteList.contains(revocationRecord.getSerial()))
        .collect(Collectors.toList());
    }

    if (!modified) {
      // No existing revocation record was modified. Add a new record.
      if (revokedCertificate.getReason() < 0) {
        throw new CertificateRevocationException("Revocation of new certificate must hav a non negative reason code");
      }
      if (revokedCertificate.getReason() == CRLReason.removeFromCRL) {
        throw new CertificateRevocationException("Removal request for a certificate that has not been revoked");
      }
      RevocationRecord newRevokedRecord = RevocationRecord.builder()
        .serial(revokedCertificate.getCertificateSerialNumber().toString(16))
        .time(getDateString(revokedCertificate.getRevocationTime(), REV_TIME_FORMAT))
        .reason(revokedCertificate.getReason())
        .build();
      revocationData.add(newRevokedRecord);
    }

    try {
      FileUtils.writeByteArrayToFile(revocationFile,
        OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(revocationData));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override public List<RevokedCertificate> getRevokedCertificates() {

    List<RevocationRecord> revocationData = getRevocationData();
    List<RevokedCertificate> revokedCertificates = new ArrayList<>();

    for (RevocationRecord record : revocationData) {
      try {
        revokedCertificates.add(getRevokedCertificateData(record));
      }
      catch (ParseException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return revokedCertificates;
  }

  private RevokedCertificate getRevokedCertificateData(RevocationRecord record) throws ParseException {
    BigInteger certSerial = new BigInteger(record.getSerial(), 16);
    LocalDateTime revocationDt = LocalDateTime.parse(record.getTime(), REV_TIME_FORMAT);
    Date revDate = Date.from(revocationDt.atZone(ZoneId.systemDefault()).toInstant());
    return new RevokedCertificate(certSerial, revDate, record.getReason());
  }

  private byte[] getStorageRecordData(X509Certificate certificate) throws IOException {

    Map<ASN1ObjectIdentifier, String> subjectAttributes = CertNameUtils.getSubjectAttributes(certificate);
    if (!subjectAttributes.containsKey(idAttribute)) {
      throw new IOException("Required subject ID attribute is not present in the certificate");
    }

    byte[] certBytes;
    try {
      certBytes = certificate.getEncoded();
    }
    catch (CertificateEncodingException e) {
      throw new IOException(e);
    }

    StorageRecord.StorageRecordBuilder recordBuilder = StorageRecord.builder();

    String idString = subjectAttributes.get(idAttribute);
    if (StringUtils.isBlank(idString)) {
      throw new IOException("Required subject ID attribute must not be empty");
    }
    if (encryption != null) {
      recordBuilder.eId(
        Base64.toBase64String(encryption.encryptData(idString.getBytes(StandardCharsets.UTF_8), false)));
      recordBuilder.eCert(Base64.toBase64String(encryption.encryptData(certBytes, true)));
    }
    else {
      recordBuilder.id(URLEncoder.encode(idString, StandardCharsets.UTF_8));
      recordBuilder.cert(Base64.toBase64String(certBytes));
    }

    StorageRecord record = recordBuilder
      .it(getDateString(certificate.getNotBefore(), CERT_TIME_FORMAT))
      .et(getDateString(certificate.getNotAfter(), CERT_TIME_FORMAT))
      .serial(certificate.getSerialNumber().toString(16))
      .build();

    return (OBJECT_MAPPER.writeValueAsString(record) + "\n").getBytes(StandardCharsets.UTF_8);
  }

  private List<RevocationRecord> getRevocationData() throws RuntimeException {
    if (!revocationFile.exists()) {
      return new ArrayList<>();
    }
    try {
      byte[] revDataBytes = FileUtils.readFileToByteArray(revocationFile);
      return OBJECT_MAPPER.readValue(revDataBytes,
        new TypeReference<>() {
        });
    }
    catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private String getDateString(Date date, DateTimeFormatter formatter) {
    LocalDateTime revocationDt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    return revocationDt.format(formatter);
  }

}
