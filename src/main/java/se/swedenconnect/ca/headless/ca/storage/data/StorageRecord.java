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
package se.swedenconnect.ca.headless.ca.storage.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON serializable storage data record used to store certificate data.
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
