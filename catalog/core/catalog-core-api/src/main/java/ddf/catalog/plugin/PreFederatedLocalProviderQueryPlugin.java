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
package ddf.catalog.plugin;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceCache;
import java.util.List;

/**
 * The {@link PreFederatedLocalProviderQueryPlugin} is an abstract class implementing the {@link
 * PreFederatedQueryPlugin}. A {@link PreFederatedQueryPlugin} that applies only for local {@link
 * Source}s should extend the {@link PreFederatedLocalProviderQueryPlugin}. This abstract class
 * provides the {@link #isLocalSource} method to check if a given {@link Source} is a local {@link
 * Source}.
 */
@Deprecated
public abstract class PreFederatedLocalProviderQueryPlugin implements PreFederatedQueryPlugin {

  protected final List<CatalogProvider> catalogProviders;

  public PreFederatedLocalProviderQueryPlugin(List<CatalogProvider> catalogProviders) {
    this.catalogProviders = catalogProviders;
  }

  private boolean isCacheSource(Source source) {
    return source instanceof SourceCache;
  }

  private boolean isCatalogProvider(Source source) {
    return source instanceof CatalogProvider
        && catalogProviders.stream().map(CatalogProvider::getId).anyMatch(source.getId()::equals);
  }

  /** Given a source, determine if it is a registered catalog provider or a cache. */
  protected boolean isLocalSource(Source source) {
    return isCacheSource(source) || isCatalogProvider(source);
  }

  @Override
  public abstract QueryRequest process(Source source, QueryRequest input)
      throws StopProcessingException;
}
