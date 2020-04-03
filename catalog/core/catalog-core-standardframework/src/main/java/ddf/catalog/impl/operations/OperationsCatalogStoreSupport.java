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
package ddf.catalog.impl.operations;

import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.source.CatalogStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for working with {@code CatalogStore}s for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains methods specific to catalog stores for the CFI and its support classes. No
 * operations/support methods should be added to this class except in support of CFI, specific to
 * catalog stores.
 */
public class OperationsCatalogStoreSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceOperations.class);

  // Inject properties
  private final FrameworkProperties frameworkProperties;

  private final SourceOperations sourceOperations;

  public OperationsCatalogStoreSupport(
      FrameworkProperties frameworkProperties, SourceOperations sourceOperations) {
    this.frameworkProperties = frameworkProperties;
    this.sourceOperations = sourceOperations;
  }

  List<CatalogStore> getCatalogStoresForRequest(
      Request request, Set<ProcessingDetails> exceptions) {
    if (!isCatalogStoreRequest(request)) {
      return Collections.emptyList();
    }

    List<CatalogStore> results = new ArrayList<>(request.getStoreIds().size());
    for (String destination : request.getStoreIds()) {
      final List<CatalogStore> sources =
          frameworkProperties.getCatalogStores().stream()
              .filter(e -> e.getId().equals(destination))
              .collect(Collectors.toList());

      if (sources.isEmpty()
          && (sourceOperations.getCatalog() == null
              || !destination.equals(sourceOperations.getCatalog().getId()))) {
        exceptions.add(new ProcessingDetailsImpl(destination, null, "CatalogStore does not exist"));
      } else if (!sources.isEmpty()) {
        if (sources.size() != 1) {
          LOGGER.debug("Multiple CatalogStores for id: {}", destination);
        }
        results.add(sources.get(0));
      }
    }
    return results;
  }

  boolean isCatalogStoreRequest(Request request) {
    return request != null
        && CollectionUtils.isNotEmpty(request.getStoreIds())
        && (request.getStoreIds().size() > 1
            || sourceOperations.getCatalog() == null
            || !request.getStoreIds().contains(sourceOperations.getCatalog().getId()));
  }
}
