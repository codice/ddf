/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.solr.client.solrj;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.solr.client.solrj.FastStreamingDocsCallback;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.StreamingResponseCallback;
import org.apache.solr.client.solrj.beans.DocumentObjectBinder;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;

/**
 * Provides a pure interface to the Solr client allowing us to add new functionality.
 *
 * <p><i>Note:</i> Such a client is returned via the {@link
 * org.codice.solr.factory.SolrClientFactory#newClient(String)} method where it might not yet be
 * available (see {@link SolrClient#isAvailable}). Even after having reported to be available, a
 * client might suddenly become unavailable. All methods will throw {@link
 * org.codice.solr.factory.impl.UnavailableSolrClient} exceptions anytime the client is unavailable
 * and the client will attempt to reestablish the connection in the background.
 *
 * <p><i>Note:</i> This is a precursor to having the whole Solr client abstracted into interfaces to
 * decouple us from the Solr implementation.
 */
@SuppressWarnings("PMD.ExcessiveClassLength" /* Duplicating SolrJ SolrClient interface */)
public interface SolrClient extends Closeable {
  /**
   * Retrieve the corresponding SolrJ's client.
   *
   * @return the corresponding SolrJ's client
   */
  public org.apache.solr.client.solrj.SolrClient getClient();

  /**
   * Gets the core associated with this Solr client
   *
   * @return the core associated with this Solr client
   */
  public String getCore();

  /**
   * Checks whether the Solr server this client is connected to and the corresponding core is
   * available or not. This method is different from an active ping as it might be basing its answer
   * on cached information.
   *
   * <p><i>Note:</i> Returning <code>true</code> doesn't guarantee the next call to any methods will
   * succeed as the server or the core might become unavailable at any time without notice.
   *
   * @return <code>false</code> if the Solr server this client is connected or the corresponding
   *     core is not available; <code>true</code> otherwise
   */
  public boolean isAvailable();

  /**
   * Waits if necessary for the Solr server this client is connected to and the corresponding core
   * to become available. This method is different from an active ping as it might be basing its
   * answer on cached information.
   *
   * <p><i>Note:</i> Nothing will happen and the method will return right away if the client was
   * already closed.
   *
   * <p><i>Note:</i> Returning <code>true</code> doesn't guarantee the next call to any methods will
   * succeed as the server or the core might become unavailable at any time without notice.
   *
   * @param timeout the maximum time to wait
   * @param unit the time unit of the timeout argument
   * @return <code>false</code> if the Solr server this client is connected or the corresponding
   *     core is not available; <code>true</code> otherwise
   * @throws InterruptedException if the current thread was interrupted while waiting
   * @throws IllegalArgumentException if <code>unit</code> is <code>null</code>
   */
  public boolean isAvailable(long timeout, TimeUnit unit) throws InterruptedException;

  /**
   * This method is the same as {@link #isAvailable()} but allows a caller to provide a listener
   * that will be called back with a boolean flag representing the new availability for this Solr
   * client whenever it changes in the future. The listener will also be called back right away with
   * the current availability of the client. This method is different from an active ping as it
   * might be basing its initial answer on cached information.
   *
   * <p><i>Note:</i> Nothing will happen and the method will return right away if the client was
   * already closed.
   *
   * <p><i>Note:</i> Returning <code>true</code> doesn't guarantee the next call to any methods will
   * succeed as the server or the core might become unavailable at any time without notice.
   *
   * @param listener a listener to notify whenever the Solr client's availability changes
   * @return <code>false</code> if the Solr server this client is connected or the corresponding
   *     core is not available; <code>true</code> otherwise
   * @throws IllegalArgumentException if <code>listener</code> is <code>null</code>
   */
  public boolean isAvailable(Listener listener);

  /**
   * Registers an initializer to be called when this Solr client finishes initializing and becomes
   * available. The initializer is called right away if this client is already available. The
   * initializer will never be called twice.
   *
   * <p><i>Note:</i> Nothing will happen and the method will return right away if the client was
   * already closed.
   *
   * <p><i>Note:</i> The client might no longer be available by the time the initializer is called
   * as the server or the core might become unavailable at any time without notice. It is, however,
   * guaranteed that the client was available at least once.
   *
   * @param initializer an initializer to notify when the Solr client first become available
   * @throws IllegalArgumentException if <code>initializer</code> is <code>null</code>
   */
  public void whenAvailable(Initializer initializer);

  /**
   * Adds a collection of documents
   *
   * @param collection the Solr collection to add documents to
   * @param docs the collection of documents
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(String collection, Collection<SolrInputDocument> docs)
      throws SolrServerException, IOException;

  /**
   * Adds a collection of documents
   *
   * @param docs the collection of documents
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(Collection<SolrInputDocument> docs)
      throws SolrServerException, IOException;

  /**
   * Adds a collection of documents, specifying max time before they become committed
   *
   * @param collection the Solr collection to add documents to
   * @param docs the collection of documents
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(
      String collection, Collection<SolrInputDocument> docs, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds a collection of documents, specifying max time before they become committed
   *
   * @param docs the collection of documents
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds a single document
   *
   * @param collection the Solr collection to add the document to
   * @param doc the input document
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(String collection, SolrInputDocument doc)
      throws SolrServerException, IOException;

  /**
   * Adds a single document
   *
   * @param doc the input document
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException;

  /**
   * Adds a single document specifying max time before it becomes committed
   *
   * @param collection the Solr collection to add the document to
   * @param doc the input document
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds a single document specifying max time before it becomes committed
   *
   * @param doc the input document
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(SolrInputDocument doc, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds the documents supplied by the given iterator.
   *
   * @param collection the Solr collection to add the documents to
   * @param docIterator the iterator which returns SolrInputDocument instances
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator)
      throws SolrServerException, IOException;

  /**
   * Adds the documents supplied by the given iterator.
   *
   * @param docIterator the iterator which returns SolrInputDocument instances
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse add(Iterator<SolrInputDocument> docIterator)
      throws SolrServerException, IOException;

  /**
   * Adds a single bean
   *
   * <p>The bean is converted to a {@link SolrInputDocument} by the client's document object binder
   *
   * @param collection to Solr collection to add documents to
   * @param obj the input bean
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBean(String collection, Object obj)
      throws IOException, SolrServerException;

  /**
   * Adds a single bean
   *
   * <p>The bean is converted to a {@link SolrInputDocument} by the client's document object binder
   *
   * @param obj the input bean
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBean(Object obj) throws IOException, SolrServerException;

  /**
   * Adds a single bean specifying max time before it becomes committed
   *
   * <p>The bean is converted to a {@link SolrInputDocument} by the client's document object binder
   *
   * @param collection to Solr collection to add documents to
   * @param obj the input bean
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBean(String collection, Object obj, int commitWithinMs)
      throws IOException, SolrServerException;

  /**
   * Adds a single bean specifying max time before it becomes committed
   *
   * <p>The bean is converted to a {@link SolrInputDocument} by the client's document object binder
   *
   * @param obj the input bean
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBean(Object obj, int commitWithinMs)
      throws IOException, SolrServerException;

  /**
   * Adds a collection of beans
   *
   * <p>The beans are converted to {@link SolrInputDocument}s by the client's document object binder
   *
   * @param collection the Solr collection to add documents to
   * @param beans the collection of beans
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(String collection, Collection<?> beans)
      throws SolrServerException, IOException;

  /**
   * Adds a collection of beans
   *
   * <p>The beans are converted to {@link SolrInputDocument}s by the client's document object binder
   *
   * @param beans the collection of beans
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException;

  /**
   * Adds a collection of beans specifying max time before they become committed
   *
   * <p>The beans are converted to {@link SolrInputDocument}s by the client's document object binder
   *
   * @param collection the Solr collection to add documents to
   * @param beans the collection of beans
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(String collection, Collection<?> beans, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds a collection of beans specifying max time before they become committed
   *
   * <p>The beans are converted to {@link SolrInputDocument}s by the client's document object binder
   *
   * @param beans the collection of beans
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Adds the beans supplied by the given iterator.
   *
   * @param collection the Solr collection to add the documents to
   * @param beanIterator the iterator which returns Beans
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(String collection, Iterator<?> beanIterator)
      throws SolrServerException, IOException;

  /**
   * Adds the beans supplied by the given iterator.
   *
   * @param beanIterator the iterator which returns Beans
   * @return an {@link UpdateResponse} from the server
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse addBeans(Iterator<?> beanIterator) throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * <p>waitFlush=true and waitSearcher=true to be inline with the defaults for plain HTTP access
   *
   * @param collection the Solr collection to send the commit to
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit(String collection) throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * <p>waitFlush=true and waitSearcher=true to be inline with the defaults for plain HTTP access
   *
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit() throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * @param collection the Solr collection to send the commit to
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit(boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * @param collection the Solr collection to send the commit to
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @param softCommit makes index changes visible while neither fsync-ing index files nor writing a
   *     new index descriptor
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit(
      String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit commit, causing pending documents to be committed for indexing
   *
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @param softCommit makes index changes visible while neither fsync-ing index files nor writing a
   *     new index descriptor
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>waitFlush=true and waitSearcher=true to be inline with the defaults for plain HTTP access
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @param collection the Solr collection to send the optimize to
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize(String collection) throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>waitFlush=true and waitSearcher=true to be inline with the defaults for plain HTTP access
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize() throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @param collection the Solr collection to send the optimize to
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @param collection the Solr collection to send the optimize to
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @param maxSegments optimizes down to at most this number of segments
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize(
      String collection, boolean waitFlush, boolean waitSearcher, int maxSegments)
      throws SolrServerException, IOException;

  /**
   * Performs an explicit optimize, causing a merge of all segments to one.
   *
   * <p>Note: In most cases it is not required to do explicit optimize
   *
   * @param waitFlush block until index changes are flushed to disk
   * @param waitSearcher block until a new searcher is opened and registered as the main query
   *     searcher, making the changes visible
   * @param maxSegments optimizes down to at most this number of segments
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments)
      throws SolrServerException, IOException;

  /**
   * Performs a rollback of all non-committed documents pending.
   *
   * <p>Note that this is not a true rollback as in databases. Content you have previously added may
   * have been committed due to autoCommit, buffer full, other client performing a commit etc.
   *
   * @param collection the Solr collection to send the rollback to
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse rollback(String collection) throws SolrServerException, IOException;

  /**
   * Performs a rollback of all non-committed documents pending.
   *
   * <p>Note that this is not a true rollback as in databases. Content you have previously added may
   * have been committed due to autoCommit, buffer full, other client performing a commit etc.
   *
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse rollback() throws SolrServerException, IOException;

  /**
   * Deletes a single document by unique ID
   *
   * @param collection the Solr collection to delete the document from
   * @param id the ID of the document to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String collection, String id)
      throws SolrServerException, IOException;

  /**
   * Deletes a single document by unique ID
   *
   * @param id the ID of the document to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String id) throws SolrServerException, IOException;

  /**
   * Deletes a single document by unique ID, specifying max time before commit
   *
   * @param collection the Solr collection to delete the document from
   * @param id the ID of the document to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String collection, String id, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Deletes a single document by unique ID, specifying max time before commit
   *
   * @param id the ID of the document to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String id, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Deletes a list of documents by unique ID
   *
   * @param collection the Solr collection to delete the documents from
   * @param ids the list of document IDs to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String collection, List<String> ids)
      throws SolrServerException, IOException;

  /**
   * Deletes a list of documents by unique ID
   *
   * @param ids the list of document IDs to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException;

  /**
   * Deletes a list of documents by unique ID, specifying max time before commit
   *
   * @param collection the Solr collection to delete the documents from
   * @param ids the list of document IDs to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Deletes a list of documents by unique ID, specifying max time before commit
   *
   * @param ids the list of document IDs to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteById(List<String> ids, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Deletes documents from the index based on a query
   *
   * @param collection the Solr collection to delete the documents from
   * @param query the query expressing what documents to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteByQuery(String collection, String query)
      throws SolrServerException, IOException;

  /**
   * Deletes documents from the index based on a query
   *
   * @param query the query expressing what documents to delete
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException;

  /**
   * Deletes documents from the index based on a query, specifying max time before commit
   *
   * @param collection the Solr collection to delete the documents from
   * @param query the query expressing what documents to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Deletes documents from the index based on a query, specifying max time before commit
   *
   * @param query the query expressing what documents to delete
   * @param commitWithinMs max time (in ms) before a commit will happen
   * @return an {@link UpdateResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public UpdateResponse deleteByQuery(String query, int commitWithinMs)
      throws SolrServerException, IOException;

  /**
   * Issues a ping request to check if the collection's replicas are alive
   *
   * @param collection the Solr collection to ping
   * @return a {@link SolrPingResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrPingResponse ping(String collection) throws SolrServerException, IOException;

  /**
   * Issues a ping request to check if the server is alive
   *
   * @return a {@link SolrPingResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrPingResponse ping() throws SolrServerException, IOException;

  /**
   * Performs a query to the Solr server
   *
   * @param collection the Solr collection to query
   * @param params an object holding all key/value parameters to send along the request
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse query(String collection, SolrParams params)
      throws SolrServerException, IOException;

  /**
   * Performs a query to the Solr server
   *
   * @param params an object holding all key/value parameters to send along the request
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse query(SolrParams params) throws SolrServerException, IOException;

  /**
   * Performs a query to the Solr server
   *
   * @param collection the Solr collection to query
   * @param params an object holding all key/value parameters to send along the request
   * @param method specifies the HTTP method to use for the request, such as GET or POST
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse query(String collection, SolrParams params, SolrRequest.METHOD method)
      throws SolrServerException, IOException;

  /**
   * Performs a query to the Solr server
   *
   * @param params an object holding all key/value parameters to send along the request
   * @param method specifies the HTTP method to use for the request, such as GET or POST
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse query(SolrParams params, SolrRequest.METHOD method)
      throws SolrServerException, IOException;

  /**
   * Query solr, and stream the results. Unlike the standard query, this will send events for each
   * Document rather then add them to the QueryResponse.
   *
   * <p>Although this function returns a 'QueryResponse' it should be used with care since it
   * excludes anything that was passed to callback. Also note that future version may pass even more
   * info to the callback and may not return the results in the QueryResponse.
   *
   * @param collection the Solr collection to query
   * @param params an object holding all key/value parameters to send along the request
   * @param callback the callback to stream results to
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse queryAndStreamResponse(
      String collection, SolrParams params, StreamingResponseCallback callback)
      throws SolrServerException, IOException;

  /**
   * @param collection the Solr collection to query
   * @param params an object holding all key/value parameters to send along the request
   * @param callback the callback to stream results to
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse queryAndStreamResponse(
      String collection, SolrParams params, FastStreamingDocsCallback callback)
      throws SolrServerException, IOException;

  /**
   * Query solr, and stream the results. Unlike the standard query, this will send events for each
   * Document rather then add them to the QueryResponse.
   *
   * <p>Although this function returns a 'QueryResponse' it should be used with care since it
   * excludes anything that was passed to callback. Also note that future version may pass even more
   * info to the callback and may not return the results in the QueryResponse.
   *
   * @param params an object holding all key/value parameters to send along the request
   * @param callback the callback to stream results to
   * @return a {@link QueryResponse} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback)
      throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocument associated with the given identifier.
   *
   * @param collection the Solr collection to query
   * @param id the id
   * @return retrieved SolrDocument, or null if no document is found.
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocument getById(String collection, String id) throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocument associated with the given identifier.
   *
   * @param id the id
   * @return retrieved SolrDocument, or null if no document is found.
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocument getById(String id) throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocument associated with the given identifier and uses the SolrParams to
   * execute the request.
   *
   * @param collection the Solr collection to query
   * @param id the id
   * @param params additional parameters to add to the query
   * @return retrieved SolrDocument, or null if no document is found.
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocument getById(String collection, String id, SolrParams params)
      throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocument associated with the given identifier and uses the SolrParams to
   * execute the request.
   *
   * @param id the id
   * @param params additional parameters to add to the query
   * @return retrieved SolrDocument, or null if no document is found.
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocument getById(String id, SolrParams params) throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocuments associated with the given identifiers.
   *
   * <p>If a document was not found, it will not be added to the SolrDocumentList.
   *
   * @param collection the Solr collection to query
   * @param ids the ids
   * @return a SolrDocumentList, or null if no documents were found
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocumentList getById(String collection, Collection<String> ids)
      throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocuments associated with the given identifiers.
   *
   * <p>If a document was not found, it will not be added to the SolrDocumentList.
   *
   * @param ids the ids
   * @return a SolrDocumentList, or null if no documents were found
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocumentList getById(Collection<String> ids) throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocuments associated with the given identifiers and uses the SolrParams to
   * execute the request.
   *
   * <p>If a document was not found, it will not be added to the SolrDocumentList.
   *
   * @param collection the Solr collection to query
   * @param ids the ids
   * @param params additional parameters to add to the query
   * @return a SolrDocumentList, or null if no documents were found
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params)
      throws SolrServerException, IOException;

  /**
   * Retrieves the SolrDocuments associated with the given identifiers and uses the SolrParams to
   * execute the request.
   *
   * <p>If a document was not found, it will not be added to the SolrDocumentList.
   *
   * @param ids the ids
   * @param params additional parameters to add to the query
   * @return a SolrDocumentList, or null if no documents were found
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public SolrDocumentList getById(Collection<String> ids, SolrParams params)
      throws SolrServerException, IOException;

  /**
   * Execute a request against a Solr server for a given collection
   *
   * @param request the request to execute
   * @param collection the collection to execute the request against
   * @return a {@link NamedList} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   */
  public NamedList<Object> request(SolrRequest request, String collection)
      throws SolrServerException, IOException;

  /**
   * Execute a request against a Solr server
   *
   * @param request the request to execute
   * @return a {@link NamedList} containing the response from the server
   * @throws IOException If there is a low-level I/O error.
   * @throws SolrServerException if there is an error on the server
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   */
  public NamedList<Object> request(SolrRequest request) throws SolrServerException, IOException;

  /**
   * Get the {@link DocumentObjectBinder} for this client.
   *
   * @return a DocumentObjectBinder
   * @throws UnavailableSolrException if the Solr server or the core is unavailable
   * @see SolrClient#addBean
   * @see SolrClient#addBeans
   */
  public DocumentObjectBinder getBinder();

  /**
   * Represents a listener that can be registered with the client to be notified of the availability
   * changed of a client. Implementing {@link #hashCode()} and {@link #equals(Object)} will
   * guarantee uniqueness of the listener object when registered.
   */
  @FunctionalInterface
  public interface Listener {
    /**
     * Notifies this listener of the availability change of this client.
     *
     * <p><i>Note:</i> Providing <code>true</code> doesn't guarantee the next call to any methods on
     * the client will succeed as the server or the core might become unavailable at any time
     * without notice.
     *
     * @param client the solr client which availability changed
     * @param available <code>true</code> if the client became available; <code>false</code> if it
     *     became unavailable
     */
    void changed(SolrClient client, boolean available);
  }

  /**
   * Represents a listener that can be registered with the client to be notified the first time the
   * client finished initializing and became available. Or if registered after such event, it would
   * be notified whenever the client becomes available again unless it is already available at which
   * point it would be notified right away.
   */
  @FunctionalInterface
  public interface Initializer {

    /**
     * Notifies this initializer that the client finished initializing and became available.
     *
     * <p><i>Note:</i> Calling this method doesn't guarantee the next call to any methods on the
     * client will succeed as the server or the core might become unavailable at any time without
     * notice. This method will only be called once.
     *
     * @param client the solr client which finished initialization
     */
    void initialized(SolrClient client);
  }
}
