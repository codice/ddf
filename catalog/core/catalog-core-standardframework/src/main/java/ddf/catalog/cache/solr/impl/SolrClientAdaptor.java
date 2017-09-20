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
package ddf.catalog.cache.solr.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClient;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adaptor interface between {@link SolrClient} and the methods needed by {@link SolrCache}. */
class SolrClientAdaptor {

  private Function<SolrClient, CacheSolrMetacardClient> metacardClientSupplierFunction;

  private Function<SolrClient, InitializedSolrClientAdaptor> clientAdaptorSupplierFunction;

  /**
   * Interface implemented by the different states the {@link SolrClientAdaptor} can be in. All
   * operations on the {@link SolrClientAdaptor} will be delegate to the current state
   * implementation.
   */
  interface State {
    /**
     * Commits Solr transactions.
     *
     * @see SolrClient#commit()
     */
    void commit() throws SolrServerException, IOException;

    /**
     * Closes the Solr connection.
     *
     * @see SolrClient#close()
     */
    void close() throws IOException;

    /**
     * Deletes Solr documents that match the query provided.
     *
     * @see SolrClient#deleteByQuery(String)
     */
    UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException;
  }

  private final String coreName;

  private final FilterAdapter filterAdapter;

  private final SolrClientFactory solrClientFactory;

  private final SolrFilterDelegateFactory solrFilterDelegateFactory;

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrClientAdaptor.class);

  private Future<SolrClient> solrClientFuture;

  private volatile SolrMetacardClient client = NoOpSolrMetacardClient.getInstance();

  private volatile State state = UninitializedSolrClientAdaptor.getInstance();

  SolrClientAdaptor(
      String coreName,
      FilterAdapter filterAdapter,
      SolrClientFactory solrClientFactory,
      SolrFilterDelegateFactory solrFilterDelegateFactory) {
    this.metacardClientSupplierFunction =
        (solrClient) ->
            new CacheSolrMetacardClient(solrClient, filterAdapter, solrFilterDelegateFactory);
    this.clientAdaptorSupplierFunction =
        (solrClient) -> new InitializedSolrClientAdaptor(solrClient);
    this.coreName = coreName;
    this.filterAdapter = filterAdapter;
    this.solrClientFactory = solrClientFactory;
    this.solrFilterDelegateFactory = solrFilterDelegateFactory;
  }

  public void init() {
    RetryPolicy retryPolicy = new RetryPolicy();

    Failsafe.with(retryPolicy)
        .onRetry(
            (exception) ->
                LOGGER.debug("Failed to get Solr client for SolrCache. Retrying...", exception))
        .onSuccess(
            (result, context) ->
                LOGGER.debug(
                    "SolrCache successfully initialized after {} retries", context.getExecutions()))
        .run(this::getSolrClient);
  }

  /** Gets a reference to the {@link SolrMetacardClient} associated with the {@link SolrClient}. */
  SolrMetacardClient getSolrMetacardClient() {
    return client;
  }

  /**
   * Commits Solr transactions.
   *
   * @see SolrClient#commit()
   */
  void commit() throws SolrServerException, IOException {
    state.commit();
  }

  /**
   * Closes the Solr connection.
   *
   * @see SolrClient#close()
   */
  void close() throws IOException {
    state.close();
  }

  /**
   * Deletes Solr documents that match the query provided.
   *
   * @see SolrClient#deleteByQuery(String)
   */
  UpdateResponse deleteByQuery(String query) throws SolrServerException, IOException {
    return state.deleteByQuery(query);
  }

  private void getSolrClient() throws InterruptedException, ExecutionException, TimeoutException {

    if (solrClientFuture == null) {
      solrClientFuture = solrClientFactory.newClient(coreName);
    }

    SolrClient solrClient = solrClientFuture.get(5, SECONDS);

    if (solrClient == null) {
      solrClientFuture = null;
      throw new IllegalStateException();
    }

    this.state = clientAdaptorSupplierFunction.apply(solrClient);

    this.client = metacardClientSupplierFunction.apply(solrClient);
  }

  //For unit testing purposes.
  State getState() {
    return state;
  }

  //For unit testing purposes.
  void setMetacardClientSupplierFunction(
      Function<SolrClient, CacheSolrMetacardClient> metacardClientSupplierFunction) {
    this.metacardClientSupplierFunction = metacardClientSupplierFunction;
  }

  //For unit testing purposes.
  void setClientAdaptorSupplierFunction(
      Function<SolrClient, InitializedSolrClientAdaptor> clientAdaptorSupplierFunction) {
    this.clientAdaptorSupplierFunction = clientAdaptorSupplierFunction;
  }
}
