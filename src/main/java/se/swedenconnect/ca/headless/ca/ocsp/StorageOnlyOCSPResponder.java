package se.swedenconnect.ca.headless.ca.ocsp;

import org.bouncycastle.asn1.ocsp.TBSRequest;
import org.bouncycastle.cert.ocsp.CertificateStatus;
import org.bouncycastle.cert.ocsp.RevokedStatus;
import se.swedenconnect.ca.engine.revocation.crl.RevokedCertificate;
import se.swedenconnect.ca.engine.revocation.ocsp.OCSPModel;
import se.swedenconnect.ca.engine.revocation.ocsp.OCSPStatusCheckingException;
import se.swedenconnect.ca.engine.revocation.ocsp.impl.AbstractOCSPResponder;
import se.swedenconnect.ca.headless.ca.StorageOnlyCARepository;
import se.swedenconnect.security.credential.PkiCredential;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This OCSP responder works of a CA repository that only stores certificates in a sequential list and
 * therefore does not allow retrieval of certificates by serial number.
 *
 * <p>
 *   This responder retrieves revoked certificates from an in memory list. This does onl work if the number
 *   of revoked certificate is manageable in size. This is the typical case for a sign service where
 *   revocation is extremely rare.
 * </p>
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class StorageOnlyOCSPResponder extends AbstractOCSPResponder {

  private final StorageOnlyCARepository caRepository;
  private Map<BigInteger, RevokedCertificate> revocationMap;

  public StorageOnlyOCSPResponder(final PkiCredential ocspIssuerCredential, final OCSPModel ocspModel, final StorageOnlyCARepository caRepository) throws NoSuchAlgorithmException {
    super(ocspIssuerCredential, ocspModel);
    this.caRepository = caRepository;
  }


  protected CertificateStatus getCertStatus(BigInteger certificateSerial) throws OCSPStatusCheckingException {
    List<RevokedCertificate> revokedCertificates = this.caRepository.getRevokedCertificates();
    Optional<RevokedCertificate> revokedCertificateOptional = revokedCertificates.stream()
      .filter(revokedCertificate -> revokedCertificate.getCertificateSerialNumber().equals(certificateSerial))
      .findFirst();

    if (revokedCertificateOptional.isPresent()){
      RevokedCertificate revokedCertificate = revokedCertificateOptional.get();
      int reason = revokedCertificate.getReason();
      return new RevokedStatus(revokedCertificate.getRevocationTime(), reason);
    }
    return CertificateStatus.GOOD;
  }

  protected void validateRequest(TBSRequest tbsRequest) throws OCSPStatusCheckingException {
    super.validateRequest(tbsRequest);
  }
}
