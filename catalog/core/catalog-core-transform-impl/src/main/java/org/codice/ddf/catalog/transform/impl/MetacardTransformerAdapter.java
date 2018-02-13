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
package org.codice.ddf.catalog.transform.impl;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;

public class MetacardTransformerAdapter extends AbstractTransformerAdapter
    implements MultiMetacardTransformer {

  private MetacardTransformer metacardTransformer;

  public MetacardTransformerAdapter(
      MetacardTransformer metacardTransformer, Map<String, Object> properties) {
    super(properties);
    this.metacardTransformer = metacardTransformer;
  }

  @Override
  public List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException {
    if (metacards.isEmpty()) {
      throw new CatalogTransformerException("transform must be called with at least one metacard");
    }

    Map<String, Serializable> args =
        arguments != null ? new HashMap<>(arguments) : Collections.emptyMap();

    List<BinaryContent> results = new LinkedList<>();

    for (Metacard metacard : metacards) {
      results.add(metacardTransformer.transform(metacard, args));
    }

    return results;
  }

  public MetacardTransformer getMetacardTransformer() {
    return metacardTransformer;
  }
}
