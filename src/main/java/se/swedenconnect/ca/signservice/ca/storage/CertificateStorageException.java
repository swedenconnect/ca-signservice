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
package se.swedenconnect.ca.signservice.ca.storage;

/**
 * Exception indicating that there was a critical error attempting to store certificate data in the repository.
 */
public class CertificateStorageException extends Exception {

  private static final long serialVersionUID = 8431429002240594156L;

  public CertificateStorageException() {
  }

  public CertificateStorageException(final String message) {
    super(message);
  }

  public CertificateStorageException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public CertificateStorageException(final Throwable cause) {
    super(cause);
  }

}
