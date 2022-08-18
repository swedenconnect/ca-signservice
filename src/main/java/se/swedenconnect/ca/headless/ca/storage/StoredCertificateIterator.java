package se.swedenconnect.ca.headless.ca.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import se.swedenconnect.ca.headless.ca.storage.data.StorageRecord;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator to read certificate records from a storage file
 *
 * @author Martin Lindström (martin@idsec.se)
 * @author Stefan Santesson (stefan@idsec.se)
 */
public class StoredCertificateIterator implements Iterator<StorageRecord>, Closeable {

  private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** The reader for file information */
  private final BufferedReader storageReader;

  /**
   * Get an instance of the stored certificate iterator
   * @param storageFile storage file
   * @return instance of {@link StoredCertificateIterator}
   */
  public static StoredCertificateIterator getInstance(File storageFile) throws FileNotFoundException {
    return new StoredCertificateIterator(storageFile, null);
  }

  /**
   * Constructs an iterator for a specific storage file
   *
   * @param storageFile storage file where stored certificates are located
   * @param encryption optional encryption capability to decrypt encrypted data
   */
  private StoredCertificateIterator(File storageFile, StorageEncryption encryption) throws FileNotFoundException {
    this.storageReader = new BufferedReader(new FileReader(storageFile));
  }

  /**
   * Returns {@code true} if the iteration has more elements.
   * (In other words, returns {@code true} if {@link #next} would
   * return an element rather than throwing an exception.)
   *
   * @return {@code true} if the iteration has more elements
   */
  @Override public boolean hasNext() {
    try {
      return storageReader.ready();
    }
    catch (IOException e) {
      return false;
    }
  }

  /**
   * Returns the next element in the iteration.
   *
   * @return the next element in the iteration
   * @throws NoSuchElementException if the iteration has no more elements
   */
  @Override public StorageRecord next() throws RuntimeException {
    try {
      String recordStr = storageReader.readLine();
      return OBJECT_MAPPER.readValue(recordStr, StorageRecord.class);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Removes from the underlying collection the last element returned
   * by this iterator (optional operation).  This method can be called
   * only once per call to {@link #next}.
   * <p>
   * The behavior of an iterator is unspecified if the underlying collection
   * is modified while the iteration is in progress in any way other than by
   * calling this method, unless an overriding class has specified a
   * concurrent modification policy.
   * <p>
   * The behavior of an iterator is unspecified if this method is called
   * after a call to the {@link #forEachRemaining forEachRemaining} method.
   *
   * @throws UnsupportedOperationException if the {@code remove}
   * operation is not supported by this iterator
   * @throws IllegalStateException if the {@code next} method has not
   * yet been called, or the {@code remove} method has already
   * been called after the last call to the {@code next}
   * method
   * @implSpec The default implementation throws an instance of
   * {@link UnsupportedOperationException} and performs no other action.
   */
  @Override public void remove() throws UnsupportedOperationException{
    throw new UnsupportedOperationException("This operation is not supported");
  }

  /**
   * Closes this stream and releases any system resources associated
   * with it. If the stream is already closed then invoking this
   * method has no effect.
   *
   * <p> As noted in {@link AutoCloseable#close()}, cases where the
   * close may fail require careful attention. It is strongly advised
   * to relinquish the underlying resources and to internally
   * <em>mark</em> the {@code Closeable} as closed, prior to throwing
   * the {@code IOException}.
   *
   * @throws IOException if an I/O error occurs
   */
  @Override public void close() throws IOException {
    storageReader.close();
  }
}
