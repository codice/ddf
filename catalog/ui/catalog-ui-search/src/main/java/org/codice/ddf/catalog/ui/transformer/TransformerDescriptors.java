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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.osgi.framework.ServiceReference;

public class TransformerDescriptors {

  private List<ServiceReference> metacardTransformers;

  private List<ServiceReference> queryResponseTransformers;

  public TransformerDescriptors(
      List<ServiceReference> metacardTransformers,
      List<ServiceReference> queryResponseTransformers) {
    this.metacardTransformers = metacardTransformers;
    this.queryResponseTransformers = queryResponseTransformers;
  }

  public List<Map<String, String>> getMetacardTransformers() {
    return getTransformerDescriptors(metacardTransformers);
  }

  public List<Map<String, String>> getQueryResponseTransformers() {
    return getTransformerDescriptors(queryResponseTransformers);
  }

  public Map<String, String> getMetacardTransformer(String id) {
    return getTransformerDescriptor(metacardTransformers, id);
  }

  public Map<String, String> getQueryResponseTransformer(String id) {
    return getTransformerDescriptor(queryResponseTransformers, id);
  }

  private Map<String, String> getTransformerDescriptor(
      List<ServiceReference> serviceReferences, String id) {
    return serviceReferences
        .stream()
        .filter(serviceRef -> serviceRef.getProperty("id") != null)
        .filter(serviceRef -> id.endsWith(serviceRef.getProperty("id").toString()))
        .findFirst()
        .map(this::getTransformerDescriptor)
        .orElse(null);
  }

  private List<Map<String, String>> getTransformerDescriptors(List<ServiceReference> transformers) {
    return transformers
        .stream()
        .filter(serviceRef -> serviceRef.getProperty("id") != null)
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
