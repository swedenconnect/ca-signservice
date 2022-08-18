package se.swedenconnect.ca.headless.ca.storage.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON serializable storage data record used to store certificate data
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageRecord {

  /** Issue time in the form of yyyy-MM-dd'T'HH:mm:ss in the default timezone */
  private String it;

  /** Expiry time in the form of yyyy-MM-dd'T'HH:mm:ss in the default timezone */
  private String et;

  /** certificate serial number as hex string */
  private String serial;

  /** Subject primary id. This is absent if eId is present */
  private String id;

  /** Encrypted subject primary id */
  private String eId;

  /** The bytes of the certificate. This is absent if the eCert is present */
  private String cert;

  /** The encrypted bytes of the certificate */
  private String eCert;



}
