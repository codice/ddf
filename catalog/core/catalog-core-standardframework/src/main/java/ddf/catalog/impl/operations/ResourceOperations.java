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
package ddf.catalog.impl.operations;

import ddf.catalog.Constants;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.LiteralImpl;
import ddf.catalog.filter.impl.PropertyIsEqualToLiteral;
import ddf.catalog.filter.impl.PropertyNameImpl;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resourceretriever.LocalResourceRetriever;
import ddf.catalog.resourceretriever.RemoteResourceRetriever;
import ddf.catalog.resourceretriever.ResourceRetriever;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.DescribableImpl;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.codice.ddf.configuration.SystemInfo;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for resource delegate operations for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains six delegated resource methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI resource
 * operations.
 */
public class ResourceOperations extends DescribableImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceOperations.class);

  private static final String FAILED_BY_GET_RESOURCE_PLUGIN =
      "Error during Pre/PostResourcePlugin.";

  private static final String DEFAULT_RESOURCE_NOT_FOUND_MESSAGE = "Unknown resource request";

  // Inject properties
  private final FrameworkProperties frameworkProperties;

  private final QueryOperations queryOperations;

  private final OperationsSecuritySupport opsSecuritySupport;

  public ResourceOperations(
      FrameworkProperties frameworkProperties,
      QueryOperations queryOperations,
      OperationsSecuritySupport opsSecuritySupport) {
    this.frameworkProperties = frameworkProperties;
    this.queryOperations = queryOperations;
    this.opsSecuritySupport = opsSecuritySupport;

    setId(SystemInfo.getSiteName());
    setVersion(SystemInfo.getVersion());
    setOrganization(SystemInfo.getOrganization());
  }

  //
  // Delegate methods
  //
  public ResourceResponse getEnterpriseResource(ResourceRequest request, boolean fanoutEnabled)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    String methodName = "getEnterpriseResource";
    LOGGER.debug("ENTERING: {}", methodName);
    ResourceResponse resourceResponse = getResource(request, true, null, fanoutEnabled);
    LOGGER.debug("EXITING: {}", methodName);
    return resourceResponse;
  }

  public Map<String, Set<String>> getEnterpriseResourceOptions(
      String metacardId, boolean fanoutEnabled) throws ResourceNotFoundException {
    LOGGER.trace("ENTERING: getEnterpriseResourceOptions");
    Set<String> supportedOptions = Collections.emptySet();

    try {
      QueryRequest queryRequest =
          new QueryRequestImpl(createMetacardIdQuery(metacardId), true, null, null);
      QueryResponse queryResponse = queryOperations.query(queryRequest, null, false, fanoutEnabled);
      List<Result> results = queryResponse.getResults();

      if (!results.isEmpty()) {
        Metacard metacard = results.get(0).getMetacard();
        String sourceIdOfResult = metacard.getSourceId();

        if (sourceIdOfResult != null && sourceIdOfResult.equals(getId())) {
          // found entry on local source
          supportedOptions = getOptionsFromLocalProvider(metacard);
        } else if (sourceIdOfResult != null && !sourceIdOfResult.equals(getId())) {
          // found entry on federated source
          supportedOptions = getOptionsFromFederatedSource(metacard, sourceIdOfResult);
        }
      } else {
        String message = "Unable to find metacard " + metacardId + " on enterprise.";
        LOGGER.debug(message);
        LOGGER.trace("EXITING: getEnterpriseResourceOptions");
        throw new ResourceNotFoundException(message);
      }

    } catch (UnsupportedQueryException e) {
      LOGGER.debug("Error finding metacard {}", metacardId, e);
      LOGGER.trace("EXITING: getEnterpriseResourceOptions");
      throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query", e);
    } catch (FederationException e) {
      LOGGER.debug("Error federating query for metacard {}", metacardId, e);
      LOGGER.trace("EXITING: getEnterpriseResourceOptions");
      throw new ResourceNotFoundException("Error finding metacard due to Federation issue", e);
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Metacard couldn't be found {}", metacardId, e);
      LOGGER.trace("EXITING: getEnterpriseResourceOptions");
      throw new ResourceNotFoundException("Query returned null metacard", e);
    }

    LOGGER.trace("EXITING: getEnterpriseResourceOptions");
    return Collections.singletonMap(ResourceRequest.OPTION_ARGUMENT, supportedOptions);
  }

  public ResourceResponse getLocalResource(ResourceRequest request, boolean fanoutEnabled)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    String methodName = "getLocalResource";
    LOGGER.debug("ENTERING: {}", methodName);
    ResourceResponse resourceResponse;
    if (fanoutEnabled) {
      LOGGER.debug("getLocalResource call received, fanning it out to all sites.");
      resourceResponse = getEnterpriseResource(request, fanoutEnabled);
    } else {
      resourceResponse = getResource(request, false, getId(), fanoutEnabled);
    }
    LOGGER.debug("EXITING: {} ", methodName);
    return resourceResponse;
  }

  public Map<String, Set<String>> getLocalResourceOptions(String metacardId, boolean fanoutEnabled)
      throws ResourceNotFoundException {
    LOGGER.trace("ENTERING: getLocalResourceOptions");

    Map<String, Set<String>> optionsMap;
    try {
      QueryRequest queryRequest =
          new QueryRequestImpl(
              createMetacardIdQuery(metacardId), false, Collections.singletonList(getId()), null);
      QueryResponse queryResponse = queryOperations.query(queryRequest, null, false, fanoutEnabled);
      List<Result> results = queryResponse.getResults();

      if (!results.isEmpty()) {
        Metacard metacard = results.get(0).getMetacard();
        optionsMap =
            Collections.singletonMap(
                ResourceRequest.OPTION_ARGUMENT, getOptionsFromLocalProvider(metacard));
      } else {

        String message = "Could not find metacard " + metacardId + " on local source";
        ResourceNotFoundException resourceNotFoundException =
            new ResourceNotFoundException(message);
        LOGGER.trace("EXITING: getLocalResourceOptions");
        throw resourceNotFoundException;
      }
    } catch (UnsupportedQueryException e) {
      LOGGER.debug("Error finding metacard {}", metacardId, e);
      LOGGER.trace("EXITING: getLocalResourceOptions");
      throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query", e);
    } catch (FederationException e) {
      LOGGER.debug("Error federating query for metacard {}", metacardId, e);
      LOGGER.trace("EXITING: getLocalResourceOptions");
      throw new ResourceNotFoundException("Error finding metacard due to Federation issue", e);
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Metacard couldn't be found {}", metacardId, e);
      LOGGER.trace("EXITING: getLocalResourceOptions");
      throw new ResourceNotFoundException("Query returned null metacard", e);
    }

    LOGGER.trace("EXITING: getLocalResourceOptions");

    return optionsMap;
  }

  public ResourceResponse getResource(
      ResourceRequest request, String resourceSiteName, boolean fanoutEnabled)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    String methodName = "getResource";
    LOGGER.debug("ENTERING: {}", methodName);
    ResourceResponse resourceResponse;
    if (fanoutEnabled) {
      LOGGER.debug("getResource call received, fanning it out to all sites.");
      resourceResponse = getEnterpriseResource(request, true);
    } else {
      resourceResponse = getResource(request, false, resourceSiteName, false);
    }
    LOGGER.debug("EXITING: {}", methodName);
    return resourceResponse;
  }

  public Map<String, Set<String>> getResourceOptions(
      String metacardId, String sourceId, boolean fanoutEnabled) throws ResourceNotFoundException {
    LOGGER.trace("ENTERING: getResourceOptions");
    Map<String, Set<String>> optionsMap;
    try {
      LOGGER.debug("source id to get options from: {}", sourceId);
      QueryRequest queryRequest =
          new QueryRequestImpl(
              createMetacardIdQuery(metacardId),
              false,
              Collections.singletonList(sourceId == null ? this.getId() : sourceId),
              null);
      QueryResponse queryResponse = queryOperations.query(queryRequest, null, false, fanoutEnabled);
      List<Result> results = queryResponse.getResults();

      if (!results.isEmpty()) {
        Metacard metacard = results.get(0).getMetacard();
        // DDF-1763: Check if the source ID passed in is null, empty,
        // or the local provider.
        if (StringUtils.isEmpty(sourceId) || sourceId.equals(getId())) {
          optionsMap =
              Collections.singletonMap(
                  ResourceRequest.OPTION_ARGUMENT, getOptionsFromLocalProvider(metacard));
        } else {
          optionsMap =
              Collections.singletonMap(
                  ResourceRequest.OPTION_ARGUMENT,
                  getOptionsFromFederatedSource(metacard, sourceId));
        }
      } else {

        String message = "Could not find metacard " + metacardId + " on source " + sourceId;
        throw new ResourceNotFoundException(message);
      }
    } catch (UnsupportedQueryException e) {
      LOGGER.debug("Error finding metacard {}", metacardId, e);
      throw new ResourceNotFoundException("Error finding metacard due to Unsuppported Query", e);
    } catch (FederationException e) {
      LOGGER.debug("Error federating query for metacard {}", metacardId, e);
      throw new ResourceNotFoundException("Error finding metacard due to Federation issue", e);
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Metacard couldn't be found {}", metacardId, e);
      throw new ResourceNotFoundException("Query returned null metacard", e);
    } finally {
      LOGGER.trace("EXITING: getResourceOptions");
    }

    return optionsMap;
  }

  //
  //
  //
  @SuppressWarnings("javadoc")
  ResourceResponse getResource(
      ResourceRequest resourceRequest,
      boolean isEnterprise,
      String resourceSiteName,
      boolean fanoutEnabled)
      throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
    ResourceResponse resourceResponse = null;
    ResourceRequest resourceReq = resourceRequest;
    ResourceRetriever retriever = null;

    if (fanoutEnabled) {
      isEnterprise = true;
    }

    if (resourceSiteName == null && !isEnterprise) {
      throw new ResourceNotFoundException(
          "resourceSiteName cannot be null when obtaining resource.");
    }

    validateGetResourceRequest(resourceReq);
    try {
      resourceReq = preProcessPreAuthorizationPlugins(resourceReq);
      resourceReq = processPreResourcePolicyPlugins(resourceReq);
      resourceReq = processPreResourceAccessPlugins(resourceReq);
      resourceReq = processPreResourcePlugins(resourceReq);

      Map<String, Serializable> requestProperties = resourceReq.getProperties();
      LOGGER.debug("Attempting to get resource from siteName: {}", resourceSiteName);
      // At this point we pull out the properties and use them.
      Serializable sourceIdProperty = requestProperties.get(ResourceRequest.SOURCE_ID);
      final String namedSource =
          (sourceIdProperty != null) ? sourceIdProperty.toString() : resourceSiteName;

      Serializable enterpriseProperty = requestProperties.get(ResourceRequest.IS_ENTERPRISE);
      if (enterpriseProperty != null && Boolean.parseBoolean(enterpriseProperty.toString())) {
        isEnterprise = true;
      }

      // check if the resourceRequest has an ID only
      // If so, the metacard needs to be found and the Resource URI
      StringBuilder resolvedSourceIdHolder = new StringBuilder();

      ResourceInfo resourceInfo =
          getResourceInfo(
              resourceReq,
              namedSource,
              isEnterprise,
              resolvedSourceIdHolder,
              requestProperties,
              fanoutEnabled);
      if (resourceInfo == null) {
        throw new ResourceNotFoundException(
            "Resource could not be found for the given attribute value: "
                + resourceReq.getAttributeValue());
      }
      final URI responseURI = getResourceOrDerivedUri(resourceInfo, resourceReq);
      final Metacard metacard = resourceInfo.getMetacard();

      final String resolvedSourceId = resolvedSourceIdHolder.toString();
      LOGGER.debug("resolvedSourceId = {}", resolvedSourceId);
      LOGGER.debug("ID = {}", getId());

      // since resolvedSourceId specifies what source the product
      // metacard resides on, we can just
      // change resourceSiteName to be that value, and then the
      // following if-else statements will
      // handle retrieving the product on the correct source
      final String resourceSourceName = (isEnterprise) ? resolvedSourceId : namedSource;

      // retrieve product from specified federated site if not in cache
      if (!resourceSourceName.equals(getId())) {
        LOGGER.debug("Searching federatedSource {} for resource.", resourceSourceName);
        LOGGER.debug("metacard for product found on source: {}", resolvedSourceId);

        final List<FederatedSource> sources =
            frameworkProperties
                .getFederatedSources()
                .stream()
                .filter(e -> e.getId().equals(resourceSourceName))
                .collect(Collectors.toList());

        if (!sources.isEmpty()) {
          if (sources.size() != 1) {
            LOGGER.debug("Found multiple federatedSources for id: {}", resourceSourceName);
          }
          FederatedSource source = sources.get(0);
          LOGGER.debug("Adding federated site to federated query: {}", source.getId());
          LOGGER.debug("Retrieving product from remote source {}", source.getId());
          retriever = new RemoteResourceRetriever(source, responseURI, requestProperties);
        } else {
          LOGGER.debug("Could not find federatedSource: {}", resourceSourceName);
        }
      } else {
        LOGGER.debug("Retrieving product from local source {}", resourceSourceName);
        retriever =
            new LocalResourceRetriever(
                frameworkProperties.getResourceReaders(), responseURI, metacard, requestProperties);
      }

      try {
        resourceResponse =
            frameworkProperties.getDownloadManager().download(resourceRequest, metacard, retriever);
      } catch (DownloadException e) {
        LOGGER.info("Unable to download resource", e);
      }

      resourceResponse = putPropertiesInResponse(resourceRequest, resourceResponse);

      resourceResponse = validateFixGetResourceResponse(resourceResponse, resourceReq);

      resourceResponse = postProcessPreAuthorizationPlugins(resourceResponse, metacard);
      resourceResponse = processPostResourcePolicyPlugins(resourceResponse, metacard);
      resourceResponse = processPostResourceAccessPlugins(resourceResponse, metacard);
      resourceResponse = processPostResourcePlugins(resourceResponse);

      resourceResponse.getProperties().put(Constants.METACARD_PROPERTY, metacard);
    } catch (DataUsageLimitExceededException e) {
      LOGGER.info("RuntimeException caused by: ", e);
      throw e;
    } catch (RuntimeException e) {
      LOGGER.info("RuntimeException caused by: ", e);
      throw new ResourceNotFoundException("Unable to find resource");
    } catch (StopProcessingException e) {
      LOGGER.info("Resource not supported", e);
      throw new ResourceNotSupportedException(FAILED_BY_GET_RESOURCE_PLUGIN + e.getMessage());
    }

    return resourceResponse;
  }

  private static URI getResourceOrDerivedUri(ResourceInfo resourceInfo, ResourceRequest resourceReq)
      throws ResourceNotFoundException {
    // check if the resource request included a qualifier in which case we are trying to retrieve
    // a derived resource and not the resource
    final Serializable qualifier = resourceReq.getPropertyValue(ResourceRequest.QUALIFIER);

    if (qualifier == null) { // no qualifier means we are retrieving the resource
      return resourceInfo.getResourceUri();
    } // else we are retrieving a specific derived resource
    LOGGER.debug("Derived resource qualifier = {}", qualifier);
    final Attribute att = resourceInfo.getMetacard().getAttribute(Metacard.DERIVED_RESOURCE_URI);

    if (att != null) {
      final List<Serializable> values = att.getValues();

      if (values != null) {
        for (final Serializable v : values) {
          try {
            final URI uri = new URI(v.toString());

            if (qualifier.equals(getQualifier(uri))) {
              // override request URI property so that we use the derived resource uri from this
              // point on
              resourceReq.getProperties().put(Metacard.RESOURCE_URI, uri);
              return uri;
            }
          } catch (URISyntaxException e) {
            LOGGER.debug("Unable to create URI for: {}", v, e);
          }
        }
      }
    }
    throw new ResourceNotFoundException(
        "Derived resource '"
            + qualifier
            + "' could not be found for the given attribute value: "
            + resourceReq.getAttributeValue());
  }

  @Nullable
  private static String getQualifier(URI uri) {
    if (StringUtils.equals(uri.getScheme(), ContentItem.CONTENT_SCHEME)) {
      return uri.getFragment();
    }
    return URLEncodedUtils.parse(uri, StandardCharsets.UTF_8)
        .stream()
        .filter(pair -> ResourceRequest.QUALIFIER.equals(pair.getName()))
        .map(NameValuePair::getValue)
        .findFirst()
        .orElse(null); // default
  }

  private ResourceResponse putPropertiesInResponse(
      ResourceRequest resourceRequest, ResourceResponse resourceResponse) {
    if (resourceResponse != null) {
      // must add the request properties into response properties in case the source forgot to
      Map<String, Serializable> properties = new HashMap<>(resourceResponse.getProperties());
      resourceRequest.getProperties().forEach(properties::putIfAbsent);
      resourceResponse =
          new ResourceResponseImpl(
              resourceResponse.getRequest(), properties, resourceResponse.getResource());
    }
    return resourceResponse;
  }

  private ResourceResponse processPostResourcePlugins(ResourceResponse resourceResponse)
      throws StopProcessingException {
    for (PostResourcePlugin plugin : frameworkProperties.getPostResource()) {
      try {
        resourceResponse = plugin.process(resourceResponse);
      } catch (PluginExecutionException e) {
        LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.", e);
      }
    }
    return resourceResponse;
  }

  private ResourceResponse processPostResourceAccessPlugins(
      ResourceResponse resourceResponse, Metacard metacard) throws StopProcessingException {
    for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
      resourceResponse = plugin.processPostResource(resourceResponse, metacard);
    }
    return resourceResponse;
  }

  private ResourceResponse processPostResourcePolicyPlugins(
      ResourceResponse resourceResponse, Metacard metacard) throws StopProcessingException {
    HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
    for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
      PolicyResponse policyResponse = plugin.processPostResource(resourceResponse, metacard);
      opsSecuritySupport.buildPolicyMap(
          responsePolicyMap, policyResponse.operationPolicy().entrySet());
    }
    resourceResponse.getProperties().put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);
    return resourceResponse;
  }

  private ResourceRequest processPreResourcePlugins(ResourceRequest resourceReq)
      throws StopProcessingException {
    for (PreResourcePlugin plugin : frameworkProperties.getPreResource()) {
      try {
        ResourceRequest processed = plugin.process(resourceReq);
        if (processed != null) {
          resourceReq = processed;
        }
      } catch (PluginExecutionException e) {
        LOGGER.debug("Plugin processing failed. This is allowable. Skipping to next plugin.", e);
      }
    }
    return resourceReq;
  }

  private ResourceRequest processPreResourceAccessPlugins(ResourceRequest resourceReq)
      throws StopProcessingException {
    for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
      resourceReq = plugin.processPreResource(resourceReq);
    }
    return resourceReq;
  }

  private ResourceRequest processPreResourcePolicyPlugins(ResourceRequest resourceReq)
      throws StopProcessingException {
    HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
    for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
      PolicyResponse policyResponse = plugin.processPreResource(resourceReq);
      opsSecuritySupport.buildPolicyMap(
          requestPolicyMap, policyResponse.operationPolicy().entrySet());
    }
    resourceReq.getProperties().put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);
    return resourceReq;
  }

  private ResourceRequest preProcessPreAuthorizationPlugins(ResourceRequest resourceRequest)
      throws StopProcessingException {
    for (PreAuthorizationPlugin plugin : frameworkProperties.getPreAuthorizationPlugins()) {
      resourceRequest = plugin.processPreResource(resourceRequest);
    }
    return resourceRequest;
  }

  private ResourceResponse postProcessPreAuthorizationPlugins(
      ResourceResponse resourceResponse, Metacard metacard) throws StopProcessingException {
    for (PreAuthorizationPlugin plugin : frameworkProperties.getPreAuthorizationPlugins()) {
      resourceResponse = plugin.processPostResource(resourceResponse, metacard);
    }
    return resourceResponse;
  }

  /**
   * Retrieves a resource by URI.
   *
   * <p>The {@link ResourceRequest} can specify either the product's URI or ID. If the product ID is
   * specified, then the matching {@link Metacard} must first be retrieved and the product URI
   * extracted from this {@link Metacard}.
   *
   * @param resourceRequest
   * @param site
   * @param isEnterprise
   * @param federatedSite
   * @param requestProperties
   * @param fanoutEnabled
   * @return
   * @throws ResourceNotSupportedException
   * @throws ResourceNotFoundException
   */
  protected ResourceInfo getResourceInfo(
      ResourceRequest resourceRequest,
      String site,
      boolean isEnterprise,
      StringBuilder federatedSite,
      Map<String, Serializable> requestProperties,
      boolean fanoutEnabled)
      throws ResourceNotSupportedException, ResourceNotFoundException {

    ResourceInfo resourceInfo;
    Query query = null;
    URI resourceUri = null;
    String name = resourceRequest.getAttributeName();
    Object value = resourceRequest.getAttributeValue();

    validateResourceInfoRequest(name, value);

    try {
      if (ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(name)) {
        // because this is a get resource by product uri, we already
        // have the product uri to return
        LOGGER.debug("get resource by product uri");
        resourceUri = (URI) value;
        URI truncatedUri = resourceUri;
        if (StringUtils.isNotBlank(resourceUri.getFragment())) {
          resourceRequest
              .getProperties()
              .put(ContentItem.QUALIFIER_KEYWORD, resourceUri.getFragment());
          try {
            // Creating the truncated URL this way is important to preserve encoding!!
            String uriString = resourceUri.toString();
            truncatedUri = new URI(uriString.substring(0, uriString.lastIndexOf('#')));
          } catch (URISyntaxException e) {
            throw new ResourceNotFoundException(
                "Could not resolve URI by doing a URI based query: " + value);
          }
        }
        query = createPropertyStartsWithQuery(Metacard.RESOURCE_URI, truncatedUri.toString());
      } else if (ResourceRequest.GET_RESOURCE_BY_ID.equals(name)) {
        // since this is a get resource by id, we need to obtain the
        // product URI
        LOGGER.debug("get resource by id");
        String metacardId = (String) value;
        LOGGER.debug("metacardId = {},   site = {}", metacardId, site);
        query = createMetacardIdQuery(metacardId);
      }

      QueryRequest queryRequest =
          new QueryRequestImpl(
              anyTag(query, site, isEnterprise),
              isEnterprise,
              Collections.singletonList(site == null ? this.getId() : site),
              resourceRequest.getProperties());

      resourceInfo =
          getResourceInfo(
              queryRequest, resourceUri, requestProperties, federatedSite, fanoutEnabled);

    } catch (UnsupportedQueryException | FederationException e) {

      throw new ResourceNotFoundException(DEFAULT_RESOURCE_NOT_FOUND_MESSAGE, e);
    }

    if (resourceInfo.getResourceUri() == null) {
      throw new ResourceNotFoundException(DEFAULT_RESOURCE_NOT_FOUND_MESSAGE);
    }
    LOGGER.debug("Returning resourceURI: {}", resourceInfo.getResourceUri());
    return resourceInfo;
  }

  private ResourceInfo getResourceInfo(
      QueryRequest queryRequest,
      URI uri,
      Map<String, Serializable> requestProperties,
      StringBuilder federatedSite,
      boolean fanoutEnabled)
      throws ResourceNotFoundException, UnsupportedQueryException, FederationException {
    Metacard metacard;
    URI resourceUri = uri;
    QueryResponse queryResponse = queryOperations.query(queryRequest, null, true, fanoutEnabled);
    if (queryResponse.getResults().isEmpty()) {
      throw new ResourceNotFoundException(
          "Could not resolve source id for URI by doing a URI based query.");
    }
    metacard = queryResponse.getResults().get(0).getMetacard();
    if (uri != null && queryResponse.getResults().size() > 1) {
      for (Result result : queryResponse.getResults()) {
        if (uri.equals(result.getMetacard().getResourceURI())) {
          metacard = result.getMetacard();
          break;
        }
      }
    }
    if (resourceUri == null) {
      resourceUri = metacard.getResourceURI();
    }
    federatedSite.append(metacard.getSourceId());
    LOGGER.debug(
        "Trying to lookup resource URI {} for metacardId: {}", resourceUri, metacard.getId());

    if (!requestProperties.containsKey(Metacard.ID)) {
      requestProperties.put(Metacard.ID, metacard.getId());
    }
    if (!requestProperties.containsKey(Metacard.RESOURCE_URI)) {
      requestProperties.put(Metacard.RESOURCE_URI, metacard.getResourceURI());
    }

    return new ResourceInfo(metacard, resourceUri);
  }

  private void validateResourceInfoRequest(String name, Object value)
      throws ResourceNotSupportedException {
    if (!(ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI.equals(name) && value instanceof URI)
        && !(ResourceRequest.GET_RESOURCE_BY_ID.equals(name) && value instanceof String)) {
      throw new ResourceNotSupportedException(
          String.format(
              "The GetResourceRequest with attribute name '%s' and value of class '%s' is not supported by this instance of the CatalogFramework.",
              name, value.getClass()));
    }
  }

  private Query anyTag(Query query, String site, boolean isEnterprise) {
    if (isEnterprise || !getId().equals(site)) {
      return query;
    }

    Filter anyTag =
        frameworkProperties
            .getFilterBuilder()
            .attribute(Metacard.TAGS)
            .is()
            .like()
            .text(FilterDelegate.WILDCARD_CHAR);
    Filter filter = frameworkProperties.getFilterBuilder().allOf(anyTag, query);
    return new QueryImpl(
        filter,
        query.getStartIndex(),
        query.getPageSize(),
        query.getSortBy(),
        query.requestsTotalResultsCount(),
        query.getTimeoutMillis());
  }

  protected static class ResourceInfo {
    private final Metacard metacard;

    private final URI resourceUri;

    public ResourceInfo(Metacard metacard, URI uri) {
      this.metacard = metacard;
      this.resourceUri = uri;
    }

    public Metacard getMetacard() {
      return metacard;
    }

    public URI getResourceUri() {
      return resourceUri;
    }
  }

  /**
   * Get the supported options from the {@link ResourceReader} that matches the scheme in the
   * specified {@link Metacard}'s URI. Only look in the local provider for the specified {@link
   * Metacard}.
   *
   * @param metacard the {@link Metacard} to get the supported options for
   * @return the {@link Set} of supported options for the metacard
   * @deprecated
   */
  @Deprecated
  private Set<String> getOptionsFromLocalProvider(Metacard metacard) {
    LOGGER.trace("ENTERING: getOptionsFromLocalProvider");
    Set<String> supportedOptions = Collections.emptySet();
    URI resourceUri = metacard.getResourceURI();
    for (ResourceReader reader : frameworkProperties.getResourceReaders()) {
      LOGGER.debug("reader id: {}", reader.getId());
      Set<String> rrSupportedSchemes = reader.getSupportedSchemes();
      String metacardScheme = resourceUri.getScheme();
      if (metacardScheme != null && rrSupportedSchemes.contains(metacardScheme)) {
        supportedOptions = reader.getOptions(metacard);
      }
    }

    LOGGER.trace("EXITING: getOptionsFromLocalProvider");
    return supportedOptions;
  }

  /**
   * Get the supported options from the {@link ResourceReader} that matches the scheme in the
   * specified {@link Metacard}'s URI. Only look in the specified source for the {@link Metacard}.
   *
   * @param metacard the {@link Metacard} to get the supported options for
   * @param sourceId the ID of the federated source to look for the {@link Metacard}
   * @return the {@link Set} of supported options for the metacard
   * @throws ResourceNotFoundException if the {@link ddf.catalog.source.Source} cannot be found for
   *     the source ID
   * @deprecated
   */
  @Deprecated
  private Set<String> getOptionsFromFederatedSource(Metacard metacard, String sourceId)
      throws ResourceNotFoundException {
    LOGGER.trace("ENTERING: getOptionsFromFederatedSource");

    final List<FederatedSource> sources =
        frameworkProperties
            .getFederatedSources()
            .stream()
            .filter(e -> e.getId().equals(sourceId))
            .collect(Collectors.toList());

    if (!sources.isEmpty()) {
      if (sources.size() != 1) {
        LOGGER.debug("Multiple FederatedSources found for id: {}", sourceId);
      }
      LOGGER.trace("EXITING: getOptionsFromFederatedSource");

      return sources.get(0).getOptions(metacard);
    } else {
      String message = "Unable to find source corresponding to given site name: " + sourceId;
      LOGGER.trace("EXITING: getOptionsFromFederatedSource");

      throw new ResourceNotFoundException(message);
    }
  }

  protected Query createMetacardIdQuery(String metacardId) {
    return createPropertyIsEqualToQuery(Metacard.ID, metacardId);
  }

  protected Query createPropertyIsEqualToQuery(String propertyName, String literal) {
    return new QueryImpl(
        new PropertyIsEqualToLiteral(new PropertyNameImpl(propertyName), new LiteralImpl(literal)));
  }

  protected Query createPropertyStartsWithQuery(String propertyName, String literal) {
    return new QueryImpl(
        frameworkProperties
            .getFilterBuilder()
            .attribute(propertyName)
            .is()
            .like()
            .text(literal + FilterDelegate.WILDCARD_CHAR));
  }

  /**
   * Validates that the {@link ResourceResponse} has a {@link ddf.catalog.resource.Resource} in it
   * that was retrieved, and that the original {@link ResourceRequest} is included in the response.
   *
   * @param getResourceResponse the original {@link ResourceResponse} returned from the source
   * @param getResourceRequest the original {@link ResourceRequest} sent to the source
   * @return the updated {@link ResourceResponse}
   * @throws ResourceNotFoundException if the original {@link ResourceResponse} is null or the
   *     resource could not be found
   */
  protected ResourceResponse validateFixGetResourceResponse(
      ResourceResponse getResourceResponse, ResourceRequest getResourceRequest)
      throws ResourceNotFoundException {
    ResourceResponse resourceResponse = getResourceResponse;
    if (getResourceResponse != null) {
      if (getResourceResponse.getResource() == null) {
        throw new ResourceNotFoundException(
            "Resource was returned as null, meaning it could not be found.");
      }
      if (getResourceResponse.getRequest() == null) {
        resourceResponse =
            new ResourceResponseImpl(
                getResourceRequest,
                getResourceResponse.getProperties(),
                getResourceResponse.getResource());
      }
    } else {
      throw new ResourceNotFoundException("CatalogProvider returned null ResourceResponse Object.");
    }
    return resourceResponse;
  }

  /**
   * Validates that the {@link ResourceRequest} is non-null, a non-null attribute name (which
   * specifies if the retrieval is being done by product URI or ID), and a non-null attribute value.
   *
   * @param getResourceRequest the {@link ResourceRequest}
   * @throws ResourceNotSupportedException if the {@link ResourceRequest} is null, or has a null
   *     attribute value or name
   */
  protected void validateGetResourceRequest(ResourceRequest getResourceRequest)
      throws ResourceNotSupportedException {
    if (getResourceRequest == null) {
      throw new ResourceNotSupportedException(
          "GetResourceRequest was null, either passed in from endpoint, or as output from PreResourcePlugin");
    }
    Serializable value = getResourceRequest.getAttributeValue();
    if (value == null || getResourceRequest.getAttributeName() == null) {
      throw new ResourceNotSupportedException(
          "Cannot perform getResource with null attribute value or null attributeName, either passed in from endpoint, or as output from PreResourcePlugin");
    }
  }
}
