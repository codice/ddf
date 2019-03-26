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
package ddf.catalog.plugin.resourcesize.metacard;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.data.ReliableResource;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetacardResourceSizePlugin implements PostQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardResourceSizePlugin.class);

  private ResourceCacheInterface cache;

  public MetacardResourceSizePlugin(ResourceCacheInterface cache) {
    this.cache = cache;
  }

  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {

    List<Result> results = input.getResults();
    for (Result result : results) {
      Metacard metacard = result.getMetacard();
      if (metacard != null) {
        // Can only search cache based on Metacard - no way to generate ResourceRequest with
        // any properties for use in generating the CacheKey
        final ResourceRequest resourceRequest = new ResourceRequestById(metacard.getId());
        CacheKey cacheKey;
        String key = null;
        ReliableResource cachedResource = null;

        try {
          cacheKey = new CacheKey(metacard, resourceRequest);
          ClassLoader tccl = Thread.currentThread().getContextClassLoader();
          key = cacheKey.generateKey();
          try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            cachedResource = (ReliableResource) cache.getValid(key, metacard);
          } finally {
            Thread.currentThread().setContextClassLoader(tccl);
          }
        } catch (IllegalArgumentException e) {
          LOGGER.debug("Unable to retrieve cached resource for metacard id = {}", metacard.getId());
        }

        if (cachedResource != null) {
          long resourceSize = cachedResource.getSize();
          if (resourceSize > 0 && cachedResource.hasProduct()) {
            LOGGER.debug(
                "Setting resourceSize = {} for metacard ID = {}", resourceSize, metacard.getId());
            Attribute resourceSizeAttribute =
                new AttributeImpl(Core.RESOURCE_SIZE, String.valueOf(resourceSize));
            metacard.setAttribute(resourceSizeAttribute);
          } else {
            LOGGER.debug("resourceSize <= 0 for metacard ID = {}", metacard.getId());
          }
        } else {
          LOGGER.debug("No cached resource for cache key = {}", key);
        }
      }
    }

    return input;
  }
}
