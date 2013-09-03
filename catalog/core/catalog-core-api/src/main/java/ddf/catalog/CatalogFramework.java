/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.RemoteSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.Describable;

/**
 * The {@link CatalogFramework} functions as the routing mechanism between all
 * catalog components. It decouples clients from service implementations and
 * provides integration points for Catalog Plugins.
 * <p>
 * General, high-level flow:
 * <ul>
 * <li/>An endpoint will invoke the active {@link CatalogFramework}, typically via an OSGi
 * dependency injection framework such as Blueprint
 * <li/>For the {@link #query(QueryRequest) query},
 * {@link #create(CreateRequest) create}, {@link #delete(DeleteRequest) delete},
 * {@link #update(UpdateRequest) update} methods, the {@link CatalogFramework}
 * calls all "Pre" Catalog Plugins (either {@link PreQueryPlugin} or
 * {@link PreIngestPlugin}),
 * <li/>The active/requested {@link FederationStrategy} is invoked, which in turn calls:
 * <ul>
 * <li/>The active {@link CatalogProvider}
 * <li>all {@link ConnectedSource}s
 * <li>specified {@link FederatedSource}s
 * </ul>
 * <li/>All "Post" Catalog Plugins (either {@link PostQueryPlugin} or
 * {@link PostIngestPlugin}),
 * <li/>The appropriate {@link Response} is returned to the calling endpoint.
 * </p>
 * <p>
 * Also includes convenience methods endpoints can use to invoke {@link MetacardTransformer}s and {@link QueryResponseTransformer}s.
 * </p>
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author Ashraf Barakat, Lockheed Martin
 * @author Ian Barnett, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface CatalogFramework extends Describable {
	
	/**
	 * Creates {@link Metacard}s in the {@link CatalogProvider}.
	 * 
	 * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Before evaluation, call
     * {@link PreIngestPlugin#process(CreateRequest)} for each registered
     * {@link PreIngestPlugin} in order determined by the OSGi
     * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * <li/>Call {@link CatalogProvider#create(CreateRequest)} on the registered
     * {@link CatalogProvider}
     * <li/>Call {@link PostIngestPlugin#process(CreateResponse)} for
     * each registered {@link PostIngestPlugin} in order determined by the
     * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * </ol>
     * </p>
	 * 
	 * @param createRequest the {@link CreateRequest}
	 * @return {@link CreateResponse}
	 * @throws IngestException if an issue occurs during the update
	 * @throws SourceUnavailableException if the source being updated is unavailable
	 */
	public CreateResponse create(CreateRequest createRequest)
			throws IngestException, SourceUnavailableException;

	/**
	 * Deletes {@link Metacard}s with {@link Attribute}s matching a specified value. 
	 * 
	 * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Before evaluation, call
     * {@link PreIngestPlugin#process(DeleteRequest)} for each registered
     * {@link PreIngestPlugin} in order determined by the OSGi
     * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * <li/>Call {@link CatalogProvider#delete(DeleteRequest)} on the registered
     * {@link CatalogProvider}
     * <li/>Call {@link PostIngestPlugin#process(DeleteResponse)} for
     * each registered {@link PostIngestPlugin} in order determined by the
     * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * </ol>
     * </p>
	 * 
	 * @param deleteRequest the {@link DeleteRequest}
	 * @return {@link DeleteResponse}
	 * @throws IngestException if an issue occurs during the deletion
	 * @throws SourceUnavailableException if the source being updated is unavailable
	 */
	public DeleteResponse delete(DeleteRequest deleteRequest)
			throws IngestException, SourceUnavailableException;

	/**
	 * Evaluate a {@link ResourceRequest} against the local {@link CatalogProvider} and {@link RemoteSource}s.
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li/>Before evaluation, call
	 * {@link PreResourcePlugin#process(ResourceRequest)} for each registered
	 * {@link PreResourcePlugin} in order determined by the OSGi
	 * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * <li/>If not provided with a URI ( <code>
	 * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
	 * </code>) , retrieve the matching {@link Metacard} and get its Product
	 * URI.
	 * <li/>Locate the {@link ResourceReader} that supports the
	 * {@link URI#getScheme()} of the URI on the OSGi Registry.
	 * <li/>Call {@link ResourceReader#retrieveResource(URI, Map)}
	 * <li/>Call {@link PostResourcePlugin#process(ResourceResponse)} for
	 * each registered {@link PostResourcePlugin} in order determined by the
	 * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * </ol>
	 * </p>
	 * 
	 * @param request the {@link ResourceRequest}
	 * @return {@link ResourceResponse}
	 * @throws IOException
	 *             if there was a problem communicating with the system
	 *             containing the resource
	 * @throws ResourceNotFoundException
	 *             if the requested resource was not found
	 * @throws ResourceNotSupportedException
	 *             if the scheme used in the associated URI is not supported
	 *             by this {@link CatalogFramework}
	 */
	public ResourceResponse getEnterpriseResource(ResourceRequest request)
			throws IOException, ResourceNotFoundException,
			ResourceNotSupportedException;



	/**
	 * Search for a {@link Metacard} in the enterprise and retrieve arguments that can be used in the retrieval of its associated Resource
	 * 
	 * @param metacardId the ID of the {@link Metacard}
	 * @return Map of supported options with argument name for using those options
	 * @throws ResourceNotFoundException  if the {@link Metacard} is not found or there is no Resource associated with the {@link Metacard}
	 * @deprecated will be removed in the next release
	 */
	public Map<String, Set<String>> getEnterpriseResourceOptions(String metacardId) throws ResourceNotFoundException;

	/**
	 * Evaluate a {@link ResourceRequest} using available
	 * {@link ResourceReader}s (does not attempt to locate the resource via
	 * federation).
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li/>Before evaluation, call
	 * {@link PreResourcePlugin#process(ResourceRequest)} for each registered
	 * {@link PreResourcePlugin} in order determined by the OSGi
	 * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * <li/>If not provided with a URI ( <code>
	 * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
	 * </code>) , retrieve the matching {@link Metacard} and get its Product
	 * URI.
	 * <li/>Locate the {@link ResourceReader} that supports the
	 * {@link URI#getScheme()} of the URI on the OSGi Registry.
	 * <li/>Call {@link ResourceReader#retrieveResource(URI, Map)}
	 * <li/>Call {@link PostResourcePlugin#process(ResourceResponse)} for
	 * each registered {@link PostResourcePlugin} in order determined by the
	 * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * </ol>
	 * </p>
	 * 
	 * @param request the {@link ResourceRequest}
	 * @return {@link ResourceResponse}
	 * @throws IOException
	 *             if there was a problem communicating with the system
	 *             containing the resource
	 * @throws ResourceNotFoundException
	 *             if the requested resource was not found
	 * @throws ResourceNotSupportedException
	 *             if the scheme used in the associated URI is not supported
	 *             by this {@link CatalogFramework}
	 */
	public ResourceResponse getLocalResource(ResourceRequest request)
			throws IOException, ResourceNotFoundException,
			ResourceNotSupportedException;

	/**
	 * Search for a {@link Metacard} locally and retrieve arguments that can be used in the retrieval of its associated Resource.
	 * 
	 * @param metacardId the ID of the {@link Metacard}
	 * @return Map of supported options with argument name for using those options
	 * @throws ResourceNotFoundException if the {@link Metacard} is not found or there is no Resource associated with the {@link Metacard}
	 * @deprecated Will be removed in the next release.
	 */
	public Map<String, Set<String>> getLocalResourceOptions(String metacardId) throws ResourceNotFoundException;

	/**
	 * Evaluate a {@link ResourceRequest} using the specified site name.
	 * 
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li/>Before evaluation, call
	 * {@link PreResourcePlugin#process(ResourceRequest)} for each registered
	 * {@link PreResourcePlugin} in order determined by the OSGi
	 * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * <li/>If not provided with a URI ( <code>
	 * {@link ResourceRequest#GET_RESOURCE_BY_PRODUCT_URI}.equals({@link ResourceRequest#getAttributeName()})==false
	 * </code>) , retrieve the matching {@link Metacard} and get its Product
	 * URI.
	 * <li/>Locate the {@link ResourceReader} that supports the
	 * {@link URI#getScheme()} of the URI on the OSGi Registry.
	 * <li/>Call {@link ResourceReader#retrieveResource(URI, Map)}
	 * <li/>Call {@link PostResourcePlugin#process(ResourceResponse)} for
	 * each registered {@link PostResourcePlugin} in order determined by the
	 * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * </ol>
	 * </p>
	 * 
	 * @param request the {@link ResourceRequest}
	 * @param resourceSiteName name of the site/source to retrieve the resource from
	 * @return {@link ResourceResponse}
	 * @throws IOException
	 *             if there was a problem communicating with the system
	 *             containing the resource
	 * @throws ResourceNotFoundException
	 *             if the requested resource was not found
	 * @throws ResourceNotSupportedException
	 *             if the scheme used in the associated URI is not supported
	 *             by this {@link CatalogFramework}
	 */
	public ResourceResponse getResource(ResourceRequest request, String resourceSiteName)
			throws IOException, ResourceNotFoundException,
			ResourceNotSupportedException;

	/**
	 * Search for a {@link Metacard} on specified {@link Source} and retrieve arguments that can be used in the retrieval of its associated Resource
	 * 
	 * @param metacardId the ID of the metacard
	 * @param sourceId the ID of the source to retrieve the metacard from
	 * @return Map of supported options with argument name for using those options
	 * @throws ResourceNotFoundException  if the {@link Metacard} is not found or there is no Resource associated with the {@link Metacard}
	 * @deprecated Will be removed in the next release.
	 */
	public Map<String, Set<String>> getResourceOptions(String metacardId, String sourceId) throws ResourceNotFoundException;

	/**
	 * Return the set of source IDs known to the {@link CatalogFramework}. This set includes
	 * the local provider and any federated sources, but not connected sources as they are
	 * hidden from external clients.
	 * 
	 * @return Set of source IDs
	 */
	public Set<String> getSourceIds();

	/**
	 * Returns information for each {@link Source} that is endpoint-addressable
	 * in {@link CatalogFramework}, including its own {@link CatalogProvider}, based
	 * on the contents of the {@link SourceInfoRequest}. 
	 * 
	 * The {@link SourceInfoRequest} specifies either:
	 * <ol>
	 * <li>an enterprise source search, which includes the catalog provider and all federated
	 * and connected sources</li>
	 * <li>a list of requested source IDs, which are all federated sources</li>
	 * <li>the catalog provider only</li>
	 * </ol>
	 * 
	 * @param sourceInfoRequest the {@link SourceInfoRequest}
	 * @return {@link SourceInfoResponse}
	 * @throws SourceUnavailableException
	 *             if the source indicated in the {@link SourceInfoRequest} is
	 *             <code>null</code>, not found, or cannot provide the requested
	 *             information
	 */
	public SourceInfoResponse getSourceInfo(SourceInfoRequest sourceInfoRequest)
			throws SourceUnavailableException;

	/**
	 * Evaluates a {@link QueryRequest} using the default
	 * {@link FederationStrategy}
	 * <p>
	 * <b>Implementations of this method must:</b>
	 * <ol>
	 * <li/>Before evaluation, call {@link PreQueryPlugin#process(QueryRequest)}
	 * for each registered {@link PreQueryPlugin} in order determined by the
	 * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
	 * responses to each other.
	 * <li/>Call {@link CatalogProvider#query(QueryRequest)} on the registered
	 * {@link CatalogProvider}
	 * <li/>Call {@link ConnectedSource#query(QueryRequest)} on all registered
	 * {@link ConnectedSource}s
	 * <li/>If applicable, invoke the default {@link FederationStrategy} which
	 * will call {@link FederatedSource#query(QueryRequest)} on all registered
	 * {@link FederatedSource}s
	 * <li/>Before returning, call
	 * {@link PostQueryPlugin#process(QueryResponse)} for each registered
	 * {@link PostQueryPlugin} in order determined by the OSGi SERVICE_RANKING
	 * (Descending, highest first), "daisy chaining" their responses to each
	 * other.
	 * <li/>Return the last {@link QueryResponse} to the caller.
	 * </ol>
	 * </p>
	 * 
	 * @see #query(QueryRequest, FederationStrategy)
	 * @param query the {@link QueryRequest}
	 * @return {@link QueryResponse} the resulting response
	 * @throws UnsupportedQueryException if the provided query can not be interpreted by a required {@link Source}
	 * @throws SourceUnavailableException if a required {@link Source} is not available
	 * @throws FederationException if federation is requested but can not complete, usually due to the {@link FederationStrategy}
	 */
	public QueryResponse query(QueryRequest query)
			throws UnsupportedQueryException, SourceUnavailableException, FederationException;

	/**
	 * Evaluates and executes a {@link QueryRequest} using the
	 * {@link FederationStrategy} provided. <br/>
	 * <p>
	 * <b>Implementations of this method must implement all of the rules defined in {@link #query(QueryRequest)}, but use the specified {@link FederationStrategy}
	 * </b>
	 * </p>
	 * 
	 * @param queryRequest the {@link QueryRequest}
	 * @param strategy
	 *            the {@link FederationStrategy} to use
	 * @return {@link QueryResponse} the resulting response
	 * @throws SourceUnavailableException if a required {@link Source} is unavailable
	 * @throws UnsupportedQueryException
	 *             if the {@link Query} can not be evaluated by this
	 *             {@link CatalogFramework} or any of its {@link Source}s.
	 * @throws FederationException
	 *             if the {@link QueryRequest} includes
	 *             {@link FederatedSource}s and there is either a problem
	 *             connecting to a {@link FederatedSource} or a
	 *             {@link FederatedSource} cannot evaluate the {@link Query}
	 * 
	 */
	public QueryResponse query(QueryRequest queryRequest,
			FederationStrategy strategy) throws SourceUnavailableException, UnsupportedQueryException,
			FederationException;
	
	/**
	 * Transforms the provided {@link Metacard} into {@link BinaryContent}. The
	 * transformerId is used to uniquely identify the
	 * {@link MetacardTransformer} desired.
	 * 
	 * @param metacard
	 *            the {@link Metacard} to be transformed
	 * @param transformerId
	 *            the id of the {@link MetacardTransformer} desired, as
	 *            registered with the OSGi Service Registry.
	 * @param requestProperties
	 *            to be used by the the transformer, if applicable
	 * @return {@link BinaryContent} the transformed {@link Metacard}
	 * @throws CatalogTransformerException if there is a problem transforming the {@link Metacard}
	 */
	public BinaryContent transform(Metacard metacard, String transformerId,
			Map<String, Serializable> requestProperties)
			throws CatalogTransformerException;
	
	/**
	 * Transforms the provided {@link SourceResponse} (or {@link QueryResponse}) into {@link BinaryContent}.
	 * The transformerId is used to uniquely identify the {@link QueryResponseTransformer} desired.
	 * 
	 * @param response
	 *            the {@link SourceResponse} to be transformed
	 * @param transformerId
	 *            the id of the transformer
	 * @param requestProperties
	 *            to be used by the the transformer, if applicable
	 * @return {@link BinaryContent} the transformed {@link SourceResponse}
	 * @throws CatalogTransformerException  if there is a problem transforming the
	 *             {@link SourceResponse}
	 */
	public BinaryContent transform(SourceResponse response,
			String transformerId, Map<String, Serializable> requestProperties)
			throws CatalogTransformerException;
	
	/**
	 * Updates a list of Metacards. Metacards that are not in the Catalog will
	 * not be created. 
	 * 
	 * If a Metacard in the list to be updated does not have its
	 * ID attribute set, then the associated ID for that Metacard in the 
	 * {@link UpdateRequest} will be used.
	 * 
	 * <p>
     * <b>Implementations of this method must:</b>
     * <ol>
     * <li/>Before evaluation, call
     * {@link PreIngestPlugin#process(UpdateRequest)} for each registered
     * {@link PreIngestPlugin} in order determined by the OSGi
     * SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * <li/>Call {@link CatalogProvider#update(UpdateRequest)} on the registered
     * {@link CatalogProvider}
     * <li/>Call {@link PostIngestPlugin#process(UpdateResponse)} for
     * each registered {@link PostIngestPlugin} in order determined by the
     * OSGi SERVICE_RANKING (Descending, highest first), "daisy chaining" their
     * responses to each other.
     * </ol>
     * </p>
	 * 
	 * @param updateRequest the {@link UpdateRequest}
	 * @return {@link UpdateResponse}
	 * @throws IngestException if an issue occurs during the update
	 * @throws SourceUnavailableException if the source being updated is unavailable
	 */
	public UpdateResponse update(UpdateRequest updateRequest)
			throws IngestException, SourceUnavailableException;
	

}