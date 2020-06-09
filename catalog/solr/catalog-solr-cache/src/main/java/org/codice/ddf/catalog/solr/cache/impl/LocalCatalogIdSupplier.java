/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.catalog.solr.cache.impl;

import ddf.catalog.CatalogFramework;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

public class LocalCatalogIdSupplier implements Supplier<String> {

  private final CatalogFramework catalogFramework;

  public LocalCatalogIdSupplier(CatalogFramework catalogFramework) {

    Validate.notNull(catalogFramework, "Valid CatalogFramework required.");

    this.catalogFramework = catalogFramework;
    CacheStrategy.setLocalSourceIdSupplier(this);
  }

  @Override
  public String get() {
    return catalogFramework.getId();
  }
}
