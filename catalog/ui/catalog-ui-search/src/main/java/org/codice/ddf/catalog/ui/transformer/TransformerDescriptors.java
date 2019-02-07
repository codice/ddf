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
package org.codice.ddf.catalog.ui.transformer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import org.osgi.framework.ServiceReference;

/**
 * Provides descriptors for all metacard and query response transformers. This class provides
 * transformer blacklisting as well as display names.
 */
public class TransformerDescriptors {

  private final List<ServiceReference> metacardTransformers;

  private final List<ServiceReference> queryResponseTransformers;

  private Set<String> blackListedMetacardTransformerIds = Collections.emptySet();

  private Set<String> blackListedQueryResponseTransformerIds = ImmutableSet.of("zipCompression");

  public TransformerDescriptors(
      List<ServiceReference> metacardTransformers,
      List<ServiceReference> queryResponseTransformers) {
    this.metacardTransformers = metacardTransformers;
    this.queryResponseTransformers = queryResponseTransformers;
  }

  @Nullable
  public List<Map<String, String>> getMetacardTransformers() {
    return getTransformerDescriptors(metacardTransformers, blackListedMetacardTransformerIds);
  }

  public List<Map<String, String>> getQueryResponseTransformers() {
    return getTransformerDescriptors(
        queryResponseTransformers, blackListedQueryResponseTransformerIds);
  }

  @Nullable
  public Map<String, String> getMetacardTransformer(String id) {
    return getTransformerDescriptor(metacardTransformers, blackListedMetacardTransformerIds, id);
  }

  @Nullable
  public Map<String, String> getQueryResponseTransformer(String id) {
    return getTransformerDescriptor(
        queryResponseTransformers, blackListedQueryResponseTransformerIds, id);
  }

  public Set<String> getBlackListedMetacardTransformerIds() {
    return blackListedMetacardTransformerIds;
  }

  public Set<String> getBlackListedQueryResponseTransformerIds() {
    return blackListedQueryResponseTransformerIds;
  }

  public void setBlackListedMetacardTransformerIds(Set<String> blackListedMetacardTransformerIds) {
    this.blackListedMetacardTransformerIds = blackListedMetacardTransformerIds;
  }

  public void setBlackListedQueryResponseTransformerIds(
      Set<String> blackListedQueryResponseTransformerIds) {
    this.blackListedQueryResponseTransformerIds = blackListedQueryResponseTransformerIds;
  }

  @Nullable
  private Map<String, String> getTransformerDescriptor(
      List<ServiceReference> serviceReferences, Set<String> blacklist, String id) {
    for (ServiceReference serviceRef : serviceReferences) {
      Object idProperty = serviceRef.getProperty("id");

      if (idProperty != null) {
        String serviceId = idProperty.toString();

        if (!blacklist.contains(serviceId) && id.endsWith(serviceId)) {
          return getTransformerDescriptor(serviceRef);
        }
      }
    }

    return null;
  }

  private List<Map<String, String>> getTransformerDescriptors(
      List<ServiceReference> transformers, Set<String> blacklist) {
    return transformers
        .stream()
        .filter(serviceRef -> serviceRef.getProperty("id") != null)
        .filter(serviceRef -> !blacklist.contains(serviceRef.getProperty("id").toString()))
        .map(this::getTransformerDescriptor)
        .collect(Collectors.toList());
  }

  private Map<String, String> getTransformerDescriptor(ServiceReference serviceRef) {
    return new ImmutableMap.Builder<String, String>()
        .put("id", serviceRef.getProperty("id").toString())
        .put("displayName", getDisplayName(serviceRef))
        .build();
  }

  private String getDisplayName(ServiceReference serviceRef) {
    Object displayName = serviceRef.getProperty("displayName");

    if (displayName == null) {
      return serviceRef.getProperty("id").toString();
    }

    return displayName.toString();
  }
}
