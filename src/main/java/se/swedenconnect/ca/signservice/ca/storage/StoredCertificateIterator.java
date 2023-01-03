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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

import se.swedenconnect.ca.signservice.ca.storage.data.StorageRecord;

/**
 * Iterator to read certificate records from a storage file.
 */
public class StoredCertificateIterator implements Iterator<StorageRecord>, Closeable {

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** The reader for file information */
  private final BufferedReader storageReader;

  /**
   * Get an instance of the stored certificate iterator.
   *
   * @param storageFile storage file
   * @return instance of {@link StoredCertificateIterator}
   */
  public static StoredCertificateIterator getInstance(final File storageFile) throws FileNotFoundException {
    return new StoredCertificateIterator(storageFile, null);
  }

  /**
   * Constructs an iterator for a specific storage file
   *
   * @param storageFile storage file where stored certificates are located
   * @param encryption optional encryption capability to decrypt encrypted data
   */
  private StoredCertificateIterator(final File storageFile, final StorageEncryption encryption)
      throws FileNotFoundException {
    this.storageReader = new BufferedReader(new FileReader(storageFile));
  }

  @Override
  public boolean hasNext() {
    try {
      return this.storageReader.ready();
    }
    catch (final IOException e) {
      return false;
    }
  }

  @Override
  public StorageRecord next() throws RuntimeException {
    try {
      final String recordStr = this.storageReader.readLine();
      return OBJECT_MAPPER.readValue(recordStr, StorageRecord.class);
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("This operation is not supported");
  }

  @Override
  public void close() throws IOException {
    this.storageReader.close();
  }
}
