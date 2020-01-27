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
package ddf.catalog.solr.collection.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.solr.collection.SolrCollectionProvider;
import ddf.catalog.util.impl.DescribableImpl;

public abstract class AbstractSolrCollectionProvider extends DescribableImpl
    implements SolrCollectionProvider {

  protected abstract String getCollectionName();

  protected abstract boolean matches(Metacard metacard);

  @Override
  public String getCollection(Metacard metacard) {
    if (metacard == null) {
      return null;
    }

    if (matches(metacard)) {
      return getCollectionName();
    }

    return null;
  }
}
