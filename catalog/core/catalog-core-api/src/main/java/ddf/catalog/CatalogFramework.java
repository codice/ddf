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
package ddf.catalog;

import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.Describable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * The {@link CatalogFramework} functions as the routing mechanism between all catalog components.
 * It decouples clients from service implementations and provides integration points for Catalog
 * Plugins.
 *
 * <p>General, high-level flow:
 *
 * <ul>
 *   <li/>An endpoint will invoke the active {@link CatalogFramework}, typically via an OSGi
 *       dependency injection framework such as Blueprint
 *   <li/>For the {@link #query(QueryRequest) query}, {@link #create(CreateRequest) create}, {@link
 *       #delete(DeleteRequest) delete}, {@link #update(UpdateRequest) update} methods, the {@link
 *       CatalogFramework} calls all "Pre" Catalog Plugins (either {@link
 *       ddf.catalog.plugin.PreQueryPlugin} or {@link ddf.catalog.plugin.PreIngestPlugin}),
 *   <li/>The active/requested {@link FederationStrategy} is invoked, which in turn calls:
 *       <ul>
 *         <li/>The active {@link ddf.catalog.source.CatalogProvider}
 *         <li>all {@link ddf.catalog.source.ConnectedSource}s
 *         <li>specified {@link ddf.catalog.source.FederatedSource}s
 *       </ul>
 *   <li/>All "Post" Catalog Plugins (either {@link ddf.catalog.plugin.PostQueryPlugin} or {@link
 *       ddf.catalog.plugin.PostIngestPlugin}),
 *   <li/>The appropriate {@link ddf.catalog.operation.Response} is returned to the calling
 *       endpoint.
 *       <p>Also includes convenience methods endpoints can use to invoke {@link
 *       ddf.catalog.transform.MetacardTransformer}s and {@link
 *       ddf.catalog.transform.QueryResponseTransformer}s.
 */
public interface CatalogFramework extends Describable {

  /**
   * <b> This code is experimental. While this interface is functional and tested, it may change or
   * be removed in a future version of the library. </b> Creates {@link Metacard}s in the {@link
   * ddf.catalog.source.CatalogProvider}.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(CreateRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#create(CreateRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(CreateResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PreCreateStoragePlugin#process(CreateStorageRequest)} for each
   *       registered {@link ddf.catalog.content.plugin.PreCreateStoragePlugin} in order determined
   *       by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses
   *       to each other.
   *   <li/>Call {@link ddf.catalog.content.StorageProvider#create(CreateStorageRequest)} on the
   *       registered {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PostCreateStoragePlugin#process(ddf.catalog.content.operation.CreateStorageResponse)}
   *       for each registered {@link ddf.catalog.content.plugin.PostCreateStoragePlugin} in order
   *       determined by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining"
   *       their responses to each other.
   * </ol>
   *
   * @param createRequest the {@link CreateStorageRequest}
   * @return {@link CreateResponse}
   * @throws IngestException if an issue occurs during the update
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  CreateResponse create(CreateStorageRequest createRequest)
      throws IngestException, SourceUnavailableException;

  /**
   * <b> This code is experimental. While this interface is functional and tested, it may change or
   * be removed in a future version of the library. </b> Creates {@link Metacard}s in the {@link
   * ddf.catalog.source.CatalogProvider}.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(CreateRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#create(CreateRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(CreateResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PreCreateStoragePlugin#process(CreateStorageRequest)} for each
   *       registered {@link ddf.catalog.content.plugin.PreCreateStoragePlugin} in order determined
   *       by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses
   *       to each other.
   *   <li/>Call {@link ddf.catalog.content.StorageProvider#create(CreateStorageRequest)} on the
   *       registered {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PostCreateStoragePlugin#process(ddf.catalog.content.operation.CreateStorageResponse)}
   *       for each registered {@link ddf.catalog.content.plugin.PostCreateStoragePlugin} in order
   *       determined by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining"
   *       their responses to each other.
   * </ol>
   *
   * @param createRequest the {@link CreateStorageRequest}
   * @param arguments this map gets passed to the underlying transformers
   * @return {@link CreateResponse}
   * @throws IngestException if an issue occurs during the update
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  CreateResponse create(
      CreateStorageRequest createRequest, Map<String, ? extends Serializable> arguments)
      throws IngestException, SourceUnavailableException;

  /**
   * Creates {@link Metacard}s in the {@link ddf.catalog.source.CatalogProvider}.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(CreateRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#create(CreateRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(CreateResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param createRequest the {@link CreateRequest}
   * @return {@link CreateResponse}
   * @throws IngestException if an issue occurs during the update
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  CreateResponse create(CreateRequest createRequest)
      throws IngestException, SourceUnavailableException;

  /**
   * Deletes {@link Metacard}s with {@link ddf.catalog.data.Attribute}s matching a specified value.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(DeleteRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#delete(DeleteRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(DeleteResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param deleteRequest the {@link DeleteRequest}
   * @return {@link DeleteResponse}
   * @throws IngestException if an issue occurs during the deletion
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  DeleteResponse delete(DeleteRequest deleteRequest)
      throws IngestException, SourceUnavailableException;

  /**
   * Evaluate a {@link ResourceRequest} against the local {@link ddf.catalog.source.CatalogProvider}
   * and {@link ddf.catalog.source.RemoteSource}s.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreResourcePlugin#process(ResourceRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreResourcePlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>If not provided with a java.net.URI ( <code>
   * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
   * </code>) , retrieve the matching {@link Metacard} and get its Product java.net.URI.
   *   <li/>Locate the {@link ddf.catalog.resource.ResourceReader} that supports the {@link
   *       java.net.URI#getScheme()} of the java.net.URI on the OSGi Registry.
   *   <li/>Call {@link ddf.catalog.resource.ResourceReader#retrieveResource(java.net.URI, Map)}
   *   <li/>Call {@link ddf.catalog.plugin.PostResourcePlugin#process(ResourceResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostResourcePlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param request the {@link ResourceRequest}
   * @return {@link ResourceResponse}
   * @throws IOException if there was a problem communicating with the system containing the
   *     resource
   * @throws ResourceNotFoundException if the requested resource was not found
   * @throws ResourceNotSupportedException if the scheme used in the associated java.net.URI is not
   *     supported by this {@link CatalogFramework}
   */
  ResourceResponse getEnterpriseResource(ResourceRequest request)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException;

  /**
   * Search for a {@link Metacard} in the enterprise and retrieve arguments that can be used in the
   * retrieval of its associated Resource
   *
   * @param metacardId the ID of the {@link Metacard}
   * @return Map of supported options with argument name for using those options
   * @throws ResourceNotFoundException if the {@link Metacard} is not found or there is no Resource
   *     associated with the {@link Metacard}
   * @deprecated will be removed in the next release
   */
  @Deprecated
  Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId)
      throws ResourceNotFoundException;

  /**
   * Evaluate a {@link ResourceRequest} using available {@link ddf.catalog.resource.ResourceReader}s
   * (does not attempt to locate the resource via federation).
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreResourcePlugin#process(ResourceRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreResourcePlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>If not provided with a java.net.URI ( <code>
   * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
   * </code>) , retrieve the matching {@link Metacard} and get its Product java.net.URI.
   *   <li/>Locate the {@link ddf.catalog.resource.ResourceReader} that supports the {@link
   *       java.net.URI#getScheme()} of the java.net.URI on the OSGi Registry.
   *   <li/>Call {@link ddf.catalog.resource.ResourceReader#retrieveResource(java.net.URI, Map)}
   *   <li/>Call {@link ddf.catalog.plugin.PostResourcePlugin#process(ResourceResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostResourcePlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param request the {@link ResourceRequest}
   * @return {@link ResourceResponse}
   * @throws IOException if there was a problem communicating with the system containing the
   *     resource
   * @throws ResourceNotFoundException if the requested resource was not found
   * @throws ResourceNotSupportedException if the scheme used in the associated java.net.URI is not
   *     supported by this {@link CatalogFramework}
   */
  ResourceResponse getLocalResource(ResourceRequest request)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException;

  /**
   * Search for a {@link Metacard} locally and retrieve arguments that can be used in the retrieval
   * of its associated Resource.
   *
   * @param metacardId the ID of the {@link Metacard}
   * @return Map of supported options with argument name for using those options
   * @throws ResourceNotFoundException if the {@link Metacard} is not found or there is no Resource
   *     associated with the {@link Metacard}
   * @deprecated Will be removed in the next release.
   */
  @Deprecated
  Map<String, Set<String>> getLocalResourceOptions(String metacardId)
      throws ResourceNotFoundException;

  /**
   * Evaluate a {@link ResourceRequest} using the specified site name.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreResourcePlugin#process(ResourceRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreResourcePlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>If not provided with a java.net.URI ( <code>
   * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
   * </code>) , retrieve the matching {@link Metacard} and get its Product java.net.URI.
   *   <li/>Locate the {@link ddf.catalog.resource.ResourceReader} that supports the {@link
   *       java.net.URI#getScheme()} of the java.net.URI on the OSGi Registry.
   *   <li/>Call {@link ddf.catalog.resource.ResourceReader#retrieveResource(java.net.URI, Map)}
   *   <li/>Call {@link ddf.catalog.plugin.PostResourcePlugin#process(ResourceResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostResourcePlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param request the {@link ResourceRequest}
   * @param resourceSiteName name of the site/source to retrieve the resource from
   * @return {@link ResourceResponse}
   * @throws IOException if there was a problem communicating with the system containing the
   *     resource
   * @throws ResourceNotFoundException if the requested resource was not found
   * @throws ResourceNotSupportedException if the scheme used in the associated java.net.URI is not
   *     supported by this {@link CatalogFramework}
   */
  ResourceResponse getResource(ResourceRequest request, String resourceSiteName)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException;

  /**
   * Search for a {@link Metacard} on specified {@link ddf.catalog.source.Source} and retrieve
   * arguments that can be used in the retrieval of its associated Resource
   *
   * @param metacardId the ID of the metacard
   * @param sourceId the ID of the source to retrieve the metacard from
   * @return Map of supported options with argument name for using those options
   * @throws ResourceNotFoundException if the {@link Metacard} is not found or there is no Resource
   *     associated with the {@link Metacard}
   * @deprecated Will be removed in the next release.
   */
  @Deprecated
  Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId)
      throws ResourceNotFoundException;

  /**
   * Return the set of source IDs known to the {@link CatalogFramework}. This set includes the local
   * provider and any federated sources, but not connected sources as they are hidden from external
   * clients.
   *
   * @return Set of source IDs
   */
  Set<String> getSourceIds();

  /**
   * Returns information for each {@link ddf.catalog.source.Source} that is endpoint-addressable in
   * {@link CatalogFramework}, including its own {@link ddf.catalog.source.CatalogProvider}, based
   * on the contents of the {@link SourceInfoRequest}.
   *
   * <p>The {@link SourceInfoRequest} specifies either:
   *
   * <ol>
   *   <li>an enterprise source search, which includes the catalog provider and all federated and
   *       connected sources
   *   <li>a list of requested source IDs, which are all federated sources
   *   <li>the catalog provider only
   * </ol>
   *
   * @param sourceInfoRequest the {@link SourceInfoRequest}
   * @return {@link SourceInfoResponse}
   * @throws SourceUnavailableException if the source indicated in the {@link SourceInfoRequest} is
   *     <code>null</code>, not found, or cannot provide the requested information
   */
  SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
      throws SourceUnavailableException;

  /**
   * Evaluates a {@link QueryRequest} using the default {@link FederationStrategy}
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link ddf.catalog.plugin.PreQueryPlugin#process(QueryRequest)}
   *       for each registered {@link ddf.catalog.plugin.PreQueryPlugin} in order determined by the
   *       OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to
   *       each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#query(QueryRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.source.ConnectedSource#query(QueryRequest)} on all registered
   *       {@link ddf.catalog.source.ConnectedSource}s
   *   <li/>If applicable, invoke the default {@link FederationStrategy} which will call {@link
   *       ddf.catalog.source.FederatedSource#query(QueryRequest)} on all registered {@link
   *       ddf.catalog.source.FederatedSource}s
   *   <li/>Before returning, call {@link ddf.catalog.plugin.PostQueryPlugin#process(QueryResponse)}
   *       for each registered {@link ddf.catalog.plugin.PostQueryPlugin} in order determined by the
   *       OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to
   *       each other.
   *   <li/>Return the last {@link QueryResponse} to the caller.
   * </ol>
   *
   * <p><b>Important:</b> Implementations are free to limit the number of results returned
   * regardless of the page size requested in the {@link QueryRequest}. For that reason, clients
   * should not assume that the number of results returned matches the page size requested and write
   * their paging code accordingly. If the {@link QueryResponse} does not need to be used, it is
   * highly recommended that clients use an iterable class such as {@link
   * ddf.catalog.util.impl.ResultIterable} to retrieve and process results. If partitioning of
   * results is required clients may use Guavas {@link
   * com.google.common.collect.Iterables#partition(Iterable, int)} method.
   *
   * @see #query(QueryRequest, FederationStrategy)
   * @param query the {@link QueryRequest}
   * @return {@link QueryResponse} the resulting response
   * @throws UnsupportedQueryException if the provided query can not be interpreted by a required
   *     {@link ddf.catalog.source.Source}
   * @throws SourceUnavailableException if a required {@link ddf.catalog.source.Source} is not
   *     available
   * @throws FederationException if federation is requested but can not complete, usually due to the
   *     {@link FederationStrategy}
   */
  QueryResponse query(QueryRequest query)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException;

  /**
   * Evaluates and executes a {@link QueryRequest} using the {@link FederationStrategy} provided.
   * <br>
   *
   * <p><b>Implementations of this method must implement all of the rules defined in {@link
   * #query(QueryRequest)}, but use the specified {@link FederationStrategy} </b>
   *
   * <p><b>Important:</b> Implementations are free to limit the number of results returned
   * regardless of the page size requested in the {@link QueryRequest}. For that reason, clients
   * should not assume that the number of results returned matches the page size requested and write
   * their paging code accordingly. If the {@link QueryResponse} does not need to be used, it is
   * highly recommended that clients use an iterable class such as {@link
   * ddf.catalog.util.impl.ResultIterable} to retrieve and process results. If partitioning of
   * results is required clients may use Guavas {@link
   * com.google.common.collect.Iterables#partition(Iterable, int)} method.
   *
   * @param queryRequest the {@link QueryRequest}
   * @param strategy the {@link FederationStrategy} to use
   * @return {@link QueryResponse} the resulting response
   * @throws SourceUnavailableException if a required {@link ddf.catalog.source.Source} is
   *     unavailable
   * @throws UnsupportedQueryException if the {@link ddf.catalog.operation.Query} can not be
   *     evaluated by this {@link CatalogFramework} or any of its {@link
   *     ddf.catalog.source.Source}s.
   * @throws FederationException if the {@link QueryRequest} includes {@link
   *     ddf.catalog.source.FederatedSource}s and there is either a problem connecting to a {@link
   *     ddf.catalog.source.FederatedSource} or a {@link ddf.catalog.source.FederatedSource} cannot
   *     evaluate the {@link ddf.catalog.operation.Query}
   */
  QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy)
      throws SourceUnavailableException, UnsupportedQueryException, FederationException;

  /**
   * Transforms the provided {@link Metacard} into {@link BinaryContent}. The transformerId is used
   * to uniquely identify the {@link ddf.catalog.transform.MetacardTransformer} desired.
   *
   * @param metacard the {@link Metacard} to be transformed
   * @param transformerId the id of the {@link ddf.catalog.transform.MetacardTransformer} desired,
   *     as registered with the OSGi Service Registry.
   * @param requestProperties to be used by the the transformer, if applicable
   * @return {@link BinaryContent} the transformed {@link Metacard}
   * @throws CatalogTransformerException if there is a problem transforming the {@link Metacard}
   */
  BinaryContent transform(
      Metacard metacard, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException;

  /**
   * Transforms the provided {@link SourceResponse} (or {@link QueryResponse}) into {@link
   * BinaryContent}. The transformerId is used to uniquely identify the {@link
   * ddf.catalog.transform.QueryResponseTransformer} desired.
   *
   * @param response the {@link SourceResponse} to be transformed
   * @param transformerId the id of the transformer
   * @param requestProperties to be used by the the transformer, if applicable
   * @return {@link BinaryContent} the transformed {@link SourceResponse}
   * @throws CatalogTransformerException if there is a problem transforming the {@link
   *     SourceResponse}
   */
  BinaryContent transform(
      SourceResponse response, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException;

  /**
   * <b> This code is experimental. While this interface is functional and tested, it may change or
   * be removed in a future version of the library. </b> Updates a list of Metacards. Metacards that
   * are not in the Catalog will not be created.
   *
   * <p>If a Metacard in the list to be updated does not have its ID attribute set, then the
   * associated ID for that Metacard in the {@link UpdateRequest} will be used.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(UpdateRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#update(UpdateRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(UpdateResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PreUpdateStoragePlugin#process(UpdateStorageRequest)} for each
   *       registered {@link ddf.catalog.content.plugin.PreUpdateStoragePlugin} in order determined
   *       by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses
   *       to each other.
   *   <li/>Call {@link ddf.catalog.content.StorageProvider#update(UpdateStorageRequest)} on the
   *       registered {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link
   *       ddf.catalog.content.plugin.PostUpdateStoragePlugin#process(ddf.catalog.content.operation.UpdateStorageResponse)}
   *       for each registered {@link ddf.catalog.content.plugin.PostUpdateStoragePlugin} in order
   *       determined by the OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining"
   *       their responses to each other.
   * </ol>
   *
   * @param updateRequest the {@link UpdateStorageRequest}
   * @param arguments this map gets passed to the underlying transformers
   * @return {@link UpdateResponse}
   * @throws IngestException if an issue occurs during the update
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  UpdateResponse update(
      UpdateStorageRequest updateRequest, Map<String, ? extends Serializable> arguments)
      throws IngestException, SourceUnavailableException;

  UpdateResponse update(UpdateStorageRequest updateRequest)
      throws IngestException, SourceUnavailableException;

  /**
   * Updates a list of Metacards. Metacards that are not in the Catalog will not be created.
   *
   * <p>If a Metacard in the list to be updated does not have its ID attribute set, then the
   * associated ID for that Metacard in the {@link UpdateRequest} will be used.
   *
   * <p><b>Implementations of this method must:</b>
   *
   * <ol>
   *   <li/>Before evaluation, call {@link
   *       ddf.catalog.plugin.PreIngestPlugin#process(UpdateRequest)} for each registered {@link
   *       ddf.catalog.plugin.PreIngestPlugin} in order determined by the OSGi SERVICE_RANKING
   *       (Descending, highest first), "daisy chaining" their responses to each other.
   *   <li/>Call {@link ddf.catalog.source.CatalogProvider#update(UpdateRequest)} on the registered
   *       {@link ddf.catalog.source.CatalogProvider}
   *   <li/>Call {@link ddf.catalog.plugin.PostIngestPlugin#process(UpdateResponse)} for each
   *       registered {@link ddf.catalog.plugin.PostIngestPlugin} in order determined by the OSGi
   *       SERVICE_RANKING (Descending, highest first), "daisy chaining" their responses to each
   *       other.
   * </ol>
   *
   * @param updateRequest the {@link UpdateRequest}
   * @return {@link UpdateResponse}
   * @throws IngestException if an issue occurs during the update
   * @throws SourceUnavailableException if the source being updated is unavailable
   */
  UpdateResponse update(UpdateRequest updateRequest)
      throws IngestException, SourceUnavailableException;
}
