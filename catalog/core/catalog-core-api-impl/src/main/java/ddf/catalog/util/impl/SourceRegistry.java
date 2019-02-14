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
package ddf.catalog.util.impl;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.collect.ImmutableList;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;

/** This class maintains lists of all of the currently-registered {@link Source}s. */
public class SourceRegistry {

  private volatile List<ConnectedSource> connectedSources = Collections.emptyList();

  private volatile List<FederatedSource> federatedSources = Collections.emptyList();

  private volatile List<CatalogProvider> catalogProviders = Collections.emptyList();

  private volatile List<CatalogStore> catalogStores = Collections.emptyList();

  /** @throws NullPointerException if {@code connectedSources} is {@code null} */
  public void setConnectedSources(final List<ConnectedSource> connectedSources) {
    notNull(connectedSources);
    this.connectedSources = connectedSources;
  }

  /** @throws NullPointerException if {@code federatedSources} is {@code null} */
  public void setFederatedSources(final List<FederatedSource> federatedSources) {
    notNull(federatedSources);
    this.federatedSources = federatedSources;
  }

  /** @throws NullPointerException if {@code catalogProviders} is {@code null} */
  public void setCatalogProviders(final List<CatalogProvider> catalogProviders) {
    notNull(catalogProviders);
    this.catalogProviders = catalogProviders;
  }

  /** @throws NullPointerException if {@code catalogStores} is {@code null} */
  public void setCatalogStores(final List<CatalogStore> catalogStores) {
    notNull(catalogStores);
    this.catalogStores = catalogStores;
  }

  /**
   * This method assumes that {@link Source}s are only registered as one type of {@link Source} so
   * that this method returns a {@link Collection} of non-{@code null} unique current {@link
   * Source}s.
   */
  public Collection<Source> getCurrentSources() {
    return Stream.of(connectedSources, federatedSources, catalogProviders, catalogStores)
        .flatMap(Collection::stream)
        .map(Validate::notNull)
        .collect(collectingAndThen(toList(), ImmutableList::copyOf));
  }
}
