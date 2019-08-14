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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.query.data.metacard.QueryMetacardTypeImpl;

public class AssociatedQueryMetacardsHandler {

  private final CatalogFramework catalogFramework;

  private static final Set<String> QUERY_ATTRIBUTE_NAMES =
      QueryMetacardTypeImpl.getQueryAttributeNames();

  public AssociatedQueryMetacardsHandler(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void create(List<String> existingQueryIds, List<QueryMetacardImpl> updatedQueryMetacards)
      throws IngestException, SourceUnavailableException {
    List<Metacard> createMetacards =
        updatedQueryMetacards
            .stream()
            .filter(Objects::nonNull)
            .filter(query -> !existingQueryIds.contains(query.getId()))
            .collect(Collectors.toList());
    if (!createMetacards.isEmpty()) {
      catalogFramework.create(new CreateRequestImpl(createMetacards));
    }
  }

  public void delete(List<String> existingQueryIds, List<String> updatedQueryIds)
      throws IngestException, SourceUnavailableException {
    String[] deleteIds =
        existingQueryIds
            .stream()
            .filter(Objects::nonNull)
            .filter(queryId -> !updatedQueryIds.contains(queryId))
            .toArray(String[]::new);
    if (deleteIds.length > 0) {
      catalogFramework.delete(new DeleteRequestImpl(deleteIds));
    }
  }

  public void update(
      List<String> existingQueryIds,
      List<QueryMetacardImpl> existingQueryMetacards,
      List<QueryMetacardImpl> updatedQueryMetacards)
      throws IngestException, SourceUnavailableException {
    Map<String, QueryMetacardImpl> existingQueryMap =
        existingQueryMetacards
            .stream()
            .collect(Collectors.toMap(QueryMetacardImpl::getId, query -> query));
    List<Metacard> updateMetacards =
        updatedQueryMetacards
            .stream()
            .filter(query -> existingQueryIds.contains(query.getId()))
            .filter(query -> hasChanges(existingQueryMap.get(query.getId()), query))
            .collect(Collectors.toList());
    if (!updateMetacards.isEmpty()) {
      String[] updateMetacardIds =
          updateMetacards.stream().map(Metacard::getId).toArray(String[]::new);
      catalogFramework.update(new UpdateRequestImpl(updateMetacardIds, updateMetacards));
    }
  }

  private boolean hasChanges(Metacard existing, Metacard updated) {
    Optional<String> difference =
        QUERY_ATTRIBUTE_NAMES
            .stream()
            .filter(
                attributeName ->
                    !Objects.equals(
                        existing.getAttribute(attributeName), updated.getAttribute(attributeName)))
            .findFirst();
    return difference.isPresent();
  }
}
