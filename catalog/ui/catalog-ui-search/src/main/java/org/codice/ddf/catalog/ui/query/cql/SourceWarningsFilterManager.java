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
package org.codice.ddf.catalog.ui.query.cql;

import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.security.SourceWarningsFilter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * A {@code SourceWarningsFilterManager} has a {@link List} of {@link SourceWarningsFilter}s which
 * it uses to {@linkplain #getFilteredWarningsFrom(ProcessingDetails) extract user-friendly warnings
 * from} the {@link ProcessingDetails} of a {@link QueryResponse} so that this application can
 * display those {@code warnings} to the user.
 */
public class SourceWarningsFilterManager {

  private List<SourceWarningsFilter> filters;

  public SourceWarningsFilterManager(List<SourceWarningsFilter> filters) {
    if (filters == null) {
      throw new IllegalArgumentException(
          "the constructor of SourceWarningsFilterManager received a null argument");
    }
    this.filters = filters;
  }

  public Set<String> getFilteredWarningsFrom(ProcessingDetails processingDetails) {
    SourceWarningsFilter compatibleFilter = null;

    if (canInspect(processingDetails)) {
      compatibleFilter = getCompatibleFilterFor(processingDetails);
    }

    return compatibleFilter == null
        ? Collections.emptySet()
        : compatibleFilter.filter(processingDetails);
  }

  private boolean canInspect(ProcessingDetails processingDetails) {
    return processingDetails != null
        && processingDetails.getSourceId() != null
        && filters != null
        && !filters.isEmpty();
  }

  @Nullable
  private SourceWarningsFilter getCompatibleFilterFor(ProcessingDetails processingDetails) {
    return filters
        .stream()
        .filter(Objects::nonNull)
        .filter(filter -> Objects.equals(filter.getId(), processingDetails.getSourceId()))
        .findFirst()
        .orElse(null);
  }
}
