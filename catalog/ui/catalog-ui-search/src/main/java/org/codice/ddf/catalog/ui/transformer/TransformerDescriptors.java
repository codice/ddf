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

  private List<Map<String, String>> metacardTransformers;

  private List<Map<String, String>> queryResponseTransformers;

  public TransformerDescriptors(
      List<ServiceReference> metacardTransformers,
      List<ServiceReference> queryResponseTransformers) {
    this.metacardTransformers = getTransformerDescriptors(metacardTransformers);
    this.queryResponseTransformers = getTransformerDescriptors(queryResponseTransformers);
  }

  public List<Map<String, String>> getMetacardTransformers() {
    return metacardTransformers;
  }

  public List<Map<String, String>> getQueryResponseTransformers() {
    return queryResponseTransformers;
  }

  public static Map<String, String> getTransformerDescriptor(
      List<Map<String, String>> descriptors, String id) {
    return descriptors
        .stream()
        .filter(descriptor -> descriptor.get("id") != null)
        .filter(descriptor -> id.endsWith(descriptor.get("id")))
        .findFirst()
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
