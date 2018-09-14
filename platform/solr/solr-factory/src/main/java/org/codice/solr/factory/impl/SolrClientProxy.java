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
package org.codice.solr.factory.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
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
 * Defines a proxy abstract class for Solr clients.
 *
 * <p><i>Note:</i> All methods proxying is handle as follow:
 *
 * <ul>
 *   <li>{@link #getBinder()}, {@link #toString()}, and {@link #close()} delegate directly to the
 *       Solr client returned from {@link #getProxiedClient}. It is therefore required to override
 *       these methods in order to intercept the delegation
 *   <li>{@link #clone} is not supported and throws {@link CloneNotSupportedException}
 *   <li>all other methods will be handled through the {@link #handle} method giving one place to
 *       intercept the delegation
 * </ul>
 */
public abstract class SolrClientProxy extends SolrClient {
  /**
   * Functional interface used to keep track of code to be delegate to a Solr client.
   *
   * @param <T> The type of object returned from the invoked code
   */
  @FunctionalInterface
  protected interface Code<T> {

    /**
     * Invokes the code.
     *
     * @param client the proxied client to invoke the code on
     * @return the result from the invocation of the code on the proxied client
     * @throws SolrServerException if a Solr server exception occurs
     * @throws IOException if an I/O exception occurs
     */
    public T invoke(SolrClient client) throws SolrServerException, IOException;
  }

  /**
   * Called to retrieve the client we are currently proxying to. The client returned is allowed to
   * change each time the method is called
   *
   * @return a solr client to proxy to
   */
  protected abstract SolrClient getProxiedClient();

  /**
   * Called to proxy all methods except for {@link #getBinder()}, {@link #clone()}, {@link
   * #toString()}, and {@link #close()} to the client.
   *
   * @param <T> the type for the returned value
   * @param code the code to handle
   * @return the returned value from the code
   * @throws SolrServerException if a solr server exception occurs
   * @throws IOException if an I/O exception occurs
   */
  protected <T> T handle(Code<T> code) throws SolrServerException, IOException {
    return code.invoke(getProxiedClient());
  }

  @Override
  public DocumentObjectBinder getBinder() {
    return getProxiedClient().getBinder();
  }

  @Override
  public String toString() {
    return getProxiedClient().toString();
  }

  @Override
  public void close() throws IOException {
    getProxiedClient().close();
  }

  @Override
  @SuppressWarnings("squid:S1182" /* Disabling support for cloning */)
  protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }

  @Override
  public UpdateResponse add(String collection, Collection<SolrInputDocument> docs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(collection, docs));
  }

  @Override
  public UpdateResponse add(Collection<SolrInputDocument> docs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(docs));
  }

  @Override
  public UpdateResponse add(
      String collection, Collection<SolrInputDocument> docs, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(collection, docs, commitWithinMs));
  }

  @Override
  public UpdateResponse add(Collection<SolrInputDocument> docs, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(docs, commitWithinMs));
  }

  @Override
  public UpdateResponse add(String collection, SolrInputDocument doc)
      throws SolrServerException, IOException {
    return handle(c -> c.add(collection, doc));
  }

  @Override
  public UpdateResponse add(SolrInputDocument doc) throws SolrServerException, IOException {
    return handle(c -> c.add(doc));
  }

  @Override
  public UpdateResponse add(String collection, SolrInputDocument doc, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(collection, doc, commitWithinMs));
  }

  @Override
  public UpdateResponse add(SolrInputDocument doc, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.add(doc, commitWithinMs));
  }

  @Override
  public UpdateResponse add(String collection, Iterator<SolrInputDocument> docIterator)
      throws SolrServerException, IOException {
    return handle(c -> c.add(collection, docIterator));
  }

  @Override
  public UpdateResponse add(Iterator<SolrInputDocument> docIterator)
      throws SolrServerException, IOException {
    return handle(c -> c.add(docIterator));
  }

  @Override
  public UpdateResponse addBean(String collection, Object obj)
      throws IOException, SolrServerException {
    return handle(c -> c.addBean(collection, obj));
  }

  @Override
  public UpdateResponse addBean(Object obj) throws IOException, SolrServerException {
    return handle(c -> c.addBean(obj));
  }

  @Override
  public UpdateResponse addBean(String collection, Object obj, int commitWithinMs)
      throws IOException, SolrServerException {
    return handle(c -> c.addBean(collection, obj, commitWithinMs));
  }

  @Override
  public UpdateResponse addBean(Object obj, int commitWithinMs)
      throws IOException, SolrServerException {
    return handle(c -> c.addBean(obj, commitWithinMs));
  }

  @Override
  public UpdateResponse addBeans(String collection, Collection<?> beans)
      throws SolrServerException, IOException {
    return handle(c -> c.addBeans(collection, beans));
  }

  @Override
  public UpdateResponse addBeans(Collection<?> beans) throws SolrServerException, IOException {
    return handle(c -> c.addBeans(beans));
  }

  @Override
  public UpdateResponse addBeans(String collection, Collection<?> beans, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.addBeans(collection, beans, commitWithinMs));
  }

  @Override
  public UpdateResponse addBeans(Collection<?> beans, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.addBeans(beans, commitWithinMs));
  }

  @Override
  public UpdateResponse addBeans(String collection, Iterator<?> beanIterator)
      throws SolrServerException, IOException {
    return handle(c -> c.addBeans(collection, beanIterator));
  }

  @Override
  public UpdateResponse addBeans(Iterator<?> beanIterator) throws SolrServerException, IOException {
    return handle(c -> c.addBeans(beanIterator));
  }

  @Override
  public UpdateResponse commit(String collection) throws SolrServerException, IOException {
    return handle(c -> c.commit(collection));
  }

  @Override
  public UpdateResponse commit() throws SolrServerException, IOException {
    return handle(SolrClient::commit);
  }

  @Override
  public UpdateResponse commit(String collection, boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException {
    return handle(c -> c.commit(collection, waitFlush, waitSearcher));
  }

  @Override
  public UpdateResponse commit(boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException {
    return handle(c -> c.commit(waitFlush, waitSearcher));
  }

  @Override
  public UpdateResponse commit(
      String collection, boolean waitFlush, boolean waitSearcher, boolean softCommit)
      throws SolrServerException, IOException {
    return handle(c -> c.commit(collection, waitFlush, waitSearcher, softCommit));
  }

  @Override
  public UpdateResponse commit(boolean waitFlush, boolean waitSearcher, boolean softCommit)
      throws SolrServerException, IOException {
    return handle(c -> c.commit(waitFlush, waitSearcher, softCommit));
  }

  @Override
  public UpdateResponse optimize(String collection) throws SolrServerException, IOException {
    return handle(c -> c.optimize(collection));
  }

  @Override
  public UpdateResponse optimize() throws SolrServerException, IOException {
    return handle(SolrClient::optimize);
  }

  @Override
  public UpdateResponse optimize(String collection, boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException {
    return handle(c -> c.optimize(collection, waitFlush, waitSearcher));
  }

  @Override
  public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher)
      throws SolrServerException, IOException {
    return handle(c -> c.optimize(waitFlush, waitSearcher));
  }

  @Override
  public UpdateResponse optimize(
      String collection, boolean waitFlush, boolean waitSearcher, int maxSegments)
      throws SolrServerException, IOException {
    return handle(c -> c.optimize(collection, waitFlush, waitSearcher, maxSegments));
  }

  @Override
  public UpdateResponse optimize(boolean waitFlush, boolean waitSearcher, int maxSegments)
      throws SolrServerException, IOException {
    return handle(c -> c.optimize(waitFlush, waitSearcher, maxSegments));
  }

  @Override
  public UpdateResponse rollback(String collection) throws SolrServerException, IOException {
    return handle(c -> c.rollback(collection));
  }

  @Override
  public UpdateResponse rollback() throws SolrServerException, IOException {
    return handle(SolrClient::rollback);
  }

  @Override
  public UpdateResponse deleteById(String collection, String id)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(collection, id));
  }

  @Override
  public UpdateResponse deleteById(String id) throws SolrServerException, IOException {
    return handle(c -> c.deleteById(id));
  }

  @Override
  public UpdateResponse deleteById(String collection, String id, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(collection, id, commitWithinMs));
  }

  @Override
  public UpdateResponse deleteById(String id, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(id, commitWithinMs));
  }

  @Override
  public UpdateResponse deleteById(String collection, List<String> ids)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(collection, ids));
  }

  @Override
  public UpdateResponse deleteById(List<String> ids) throws SolrServerException, IOException {
    return handle(c -> c.deleteById(ids));
  }

  @Override
  public UpdateResponse deleteById(String collection, List<String> ids, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(collection, ids, commitWithinMs));
  }

  @Override
  public UpdateResponse deleteById(List<String> ids, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteById(ids, commitWithinMs));
  }

  @Override
  public UpdateResponse deleteByQuery(String collection, String query)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteByQuery(collection, query));
  }

  @Override
  public UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
    return handle(c -> c.deleteByQuery(query));
  }

  @Override
  public UpdateResponse deleteByQuery(String collection, String query, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteByQuery(collection, query, commitWithinMs));
  }

  @Override
  public UpdateResponse deleteByQuery(String query, int commitWithinMs)
      throws SolrServerException, IOException {
    return handle(c -> c.deleteByQuery(query, commitWithinMs));
  }

  @Override
  public SolrPingResponse ping() throws SolrServerException, IOException {
    return handle(SolrClient::ping);
  }

  @Override
  public QueryResponse query(String collection, SolrParams params)
      throws SolrServerException, IOException {
    return handle(c -> c.query(collection, params));
  }

  @Override
  public QueryResponse query(SolrParams params) throws SolrServerException, IOException {
    return handle(c -> c.query(params));
  }

  @Override
  public QueryResponse query(String collection, SolrParams params, METHOD method)
      throws SolrServerException, IOException {
    return handle(c -> c.query(collection, params, method));
  }

  @Override
  public QueryResponse query(SolrParams params, METHOD method)
      throws SolrServerException, IOException {
    return handle(c -> c.query(params, method));
  }

  @Override
  public QueryResponse queryAndStreamResponse(
      String collection, SolrParams params, StreamingResponseCallback callback)
      throws SolrServerException, IOException {
    return handle(c -> c.queryAndStreamResponse(collection, params, callback));
  }

  @Override
  public QueryResponse queryAndStreamResponse(SolrParams params, StreamingResponseCallback callback)
      throws SolrServerException, IOException {
    return handle(c -> c.queryAndStreamResponse(params, callback));
  }

  @Override
  public SolrDocument getById(String collection, String id)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(collection, id));
  }

  @Override
  public SolrDocument getById(String id) throws SolrServerException, IOException {
    return handle(c -> c.getById(id));
  }

  @Override
  public SolrDocument getById(String collection, String id, SolrParams params)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(collection, id, params));
  }

  @Override
  public SolrDocument getById(String id, SolrParams params)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(id, params));
  }

  @Override
  public SolrDocumentList getById(String collection, Collection<String> ids)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(collection, ids));
  }

  @Override
  public SolrDocumentList getById(Collection<String> ids) throws SolrServerException, IOException {
    return handle(c -> c.getById(ids));
  }

  @Override
  public SolrDocumentList getById(String collection, Collection<String> ids, SolrParams params)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(collection, ids, params));
  }

  @Override
  public SolrDocumentList getById(Collection<String> ids, SolrParams params)
      throws SolrServerException, IOException {
    return handle(c -> c.getById(ids, params));
  }

  @Override
  public NamedList<Object> request(SolrRequest request, String collection)
      throws SolrServerException, IOException {
    return handle(c -> c.request(request, collection));
  }
}
