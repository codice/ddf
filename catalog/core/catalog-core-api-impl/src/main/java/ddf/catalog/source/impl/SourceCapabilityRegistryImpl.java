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
package ddf.catalog.source.impl;

import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCapabilityProvider;
import ddf.catalog.source.SourceCapabilityRegistry;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SourceCapabilityRegistryImpl implements SourceCapabilityRegistry {

  private List<SourceCapabilityProvider> sourceCapabilityProviders;

  public SourceCapabilityRegistryImpl(List<SourceCapabilityProvider> sourceCapabilityProviders) {
    this.sourceCapabilityProviders = sourceCapabilityProviders;
  }

  public SourceCapabilityRegistryImpl() {
    this.sourceCapabilityProviders = new LinkedList<>();
  }

  @Override
  public List<String> list(Source source) {
    return sourceCapabilityProviders
        .stream()
        .flatMap(
            sourceCapabilityProvider ->
                sourceCapabilityProvider.getSourceCapabilities(source).stream())
        .collect(Collectors.toList());
  }

  public void addCapabilityProvider(SourceCapabilityProvider sourceCapabilityProvider) {
    sourceCapabilityProviders.add(sourceCapabilityProvider);
  }

  public void removeCapabilityProvider(SourceCapabilityProvider sourceCapabilityProvider) {
    sourceCapabilityProviders.removeIf(
        capabilityProvider -> capabilityProvider.getId().equals(sourceCapabilityProvider.getId()));
  }
}
