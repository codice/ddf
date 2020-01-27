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
package ddf.catalog.source.solr;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.operation.IndexQueryResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

/** Interface that defines the different metacard operations performed on Solr. */
public interface SolrMetacardClient {

  /**
   * Converts a {@link QueryRequest} into a Solr query and returns the result as a {@link
   * IndexQueryResponse}.
   *
   * @param request query request to execute against Solr
   * @return converted Solr response
   * @throws UnsupportedQueryException if the query is not supported
   */
  IndexQueryResponse queryIndex(QueryRequest request) throws UnsupportedQueryException;

  /**
   * Performs an cache query by ID against Solr. Uses the /get handler to retrieve data that may not
   * yet have been indexed.
   *
   * @param request query request to execute against Solr
   * @return converted Solr response
   * @throws UnsupportedQueryException if the query is not supported
   */
  IndexQueryResponse queryIndexCache(QueryRequest request) throws UnsupportedQueryException;

  /**
   * Converts a {@link QueryRequest} into a Solr query and returns the result as a {@link
   * SourceResponse}.
   *
   * @param request query request to execute against Solr
   * @return converted Solr response
   * @throws UnsupportedQueryException if the query is not supported
   */
  SourceResponse query(QueryRequest request) throws UnsupportedQueryException;

  /**
   * Runs a Solr query and converts the result as a list of {@link Metacard} objects.
   *
   * @param queryString Solr query string
   * @return list of {@link Metacard} objects created from the Solr result
   * @throws UnsupportedQueryException if the query is not supported, e.g., invalid query string
   */
  List<Metacard> query(String queryString) throws UnsupportedQueryException;

  List<Metacard> getIds(Set<String> ids) throws UnsupportedQueryException;

  /** @return set of supported content types. */
  Set<ContentType> getContentTypes();

  /**
   * Adds a list of {@link Metacard} objects to Solr.
   *
   * @param metacards list of {@link Metacard} objects to add
   * @param forceAutoCommit force an auto-commit after the addition
   * @return list of documents added
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   * @throws MetacardCreationException if a {@link Metacard} could not be created
   */
  @Nullable
  List<SolrInputDocument> add(@Nullable List<Metacard> metacards, boolean forceAutoCommit)
      throws IOException, SolrServerException, MetacardCreationException;

  /**
   * Deletes Solr documents by ID.
   *
   * @param fieldName field name that contains the ID
   * @param identifiers list of identifiers to delete
   * @param forceCommit force an auto-commit after the deletion
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   */
  void deleteByIds(String fieldName, List<? extends Serializable> identifiers, boolean forceCommit)
      throws IOException, SolrServerException;

  /**
   * Deletes all the Solr documents that match a specific query.
   *
   * @param query Solr query string
   * @throws IOException if there is a communication error with the server
   * @throws SolrServerException if there is an error on the server
   */
  void deleteByQuery(String query) throws IOException, SolrServerException;

  /**
   * Returns whether or not the metacard should be committed in Near Real Time
   *
   * @param metacard
   * @return True if metacard is a type that requires Near Real Time commit
   */
  boolean isNrtType(Metacard metacard);

  /**
   * Commits a set of documents to Solr
   *
   * @param docs - Documents to commit to Solr
   * @param forceAutoCommit - Whether or not to force a commit
   * @param isNrtCommit - Whether or not a document contains NRT data
   */
  void commit(List<SolrInputDocument> docs, boolean forceAutoCommit, boolean isNrtCommit)
      throws IOException, SolrServerException;

  /**
   * Returns a list of SolrDocuments
   *
   * @param ids - Document IDs to retrieve
   * @return - List of Solr docs for provided IDs
   */
  List<SolrDocument> getSolrDocs(Set<String> ids) throws UnsupportedQueryException;
}
