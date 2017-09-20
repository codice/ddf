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
package org.codice.ddf.catalog.resource.cache;

import ddf.catalog.data.Metacard;
import org.codice.ddf.catalog.resource.cache.impl.ResourceCacheService;

/** MBean interface describing the operations used to manage the product cache. */
public interface ResourceCacheServiceMBean {

  public static final String OBJECT_NAME =
      ResourceCacheService.class.getName() + ":service=resource-cache-service";

  public static final Class<ResourceCacheServiceMBean> MBEAN_CLASS =
      ResourceCacheServiceMBean.class;

  /**
   * Determines if the resource cache is enabled.
   *
   * @return {@code true} if the resource cache is enabled, {@code false} otherwise
   */
  boolean isCacheEnabled();

  /**
   * Determines if the resource associated with the metacard is in the resource cache
   *
   * @param metacard to use to find a matching resource
   * @return {@code true} if the resource is in the cache, {@code false} otherwise.
   */
  boolean contains(Metacard metacard);

  /**
   * Determines if the resource associated with the metacard is in the resource cache
   *
   * @param metacardId to use to find a matching resource
   * @return {@code true} if the resource is in the cache, {@code false} otherwise.
   */
  boolean containsById(String metacardId);
}
