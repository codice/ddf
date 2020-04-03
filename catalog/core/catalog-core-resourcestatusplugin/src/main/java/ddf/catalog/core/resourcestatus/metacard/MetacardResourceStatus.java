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
package ddf.catalog.core.resourcestatus.metacard;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.data.ReliableResource;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.codice.ddf.configuration.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link PostQueryPlugin} that checks the {@link ddf.catalog.cache.impl.ResourceCache} for
 * existence of each {@link Metacard}'s related {@link ddf.catalog.resource.Resource} and adds an
 * {@link ddf.catalog.data.Attribute} to each {@link Metacard} in the {@link QueryResponse}.
 */
public class MetacardResourceStatus implements PostQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardResourceStatus.class);

  private static final String INTERNAL_LOCAL_RESOURCE = "internal.local-resource";

  private static final String LOCAL_CONTENT_SCHEME = "content:";

  private ResourceCacheInterface cache;

  public MetacardResourceStatus(ResourceCacheInterface cache) {
    this.cache = cache;
  }

  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    List<Result> results = input.getResults();

    results.stream()
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .forEach(this::addResourceLocalAttribute);

    return input;
  }

  private void addResourceLocalAttribute(Metacard metacard) {
    boolean isResourceLocal = false;
    if (!hasResourceUri(metacard)) {
      isResourceLocal = false;
    } else {
      isResourceLocal = isResourceLocal(metacard);
    }
    metacard.setAttribute(new AttributeImpl(INTERNAL_LOCAL_RESOURCE, isResourceLocal));
  }

  private boolean isResourceLocal(Metacard metacard) {
    return (doesSourceIdMatchLocalSiteName(metacard) && isResourceUriLocal(metacard))
        || isResourceCached(metacard, new ResourceRequestById(metacard.getId()));
  }

  private boolean isResourceCached(Metacard metacard, ResourceRequest resourceRequest) {
    String key = getCacheKey(metacard, resourceRequest);
    ReliableResource cachedResource = (ReliableResource) cache.getValid(key, metacard);
    return cachedResource != null;
  }

  private boolean hasResourceUri(Metacard metacard) {
    Optional<String> resourceUri =
        Optional.ofNullable(metacard.getResourceURI()).map(uri -> uri.toString());
    return resourceUri.isPresent();
  }

  private boolean isResourceUriLocal(Metacard metacard) {
    return hasResourceUri(metacard)
        && metacard.getResourceURI().toString().startsWith(LOCAL_CONTENT_SCHEME);
  }

  private boolean doesSourceIdMatchLocalSiteName(Metacard metacard) {
    Optional<String> sourceId = Optional.ofNullable(metacard.getSourceId());

    if (sourceId.isPresent()) {
      String sourceIdLowerCase = sourceId.get().toLowerCase(Locale.getDefault());
      return sourceIdLowerCase.equals(getLocalSiteName());
    } else {
      LOGGER.debug(
          "Unable to determine if the source id in metacard {} matches the local site name because the metacard did not contain a source id attribute.",
          metacard.getId());
      return false;
    }
  }

  private String getCacheKey(Metacard metacard, ResourceRequest resourceRequest) {
    CacheKey cacheKey = new CacheKey(metacard, resourceRequest);
    return cacheKey.generateKey();
  }

  String getLocalSiteName() {
    return SystemInfo.getSiteName().toLowerCase();
  }
}
