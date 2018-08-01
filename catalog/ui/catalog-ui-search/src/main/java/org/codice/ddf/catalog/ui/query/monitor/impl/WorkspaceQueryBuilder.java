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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import ddf.catalog.filter.FilterBuilder;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.opengis.filter.Filter;

public class WorkspaceQueryBuilder {
  private final FilterBuilder filterBuilder;

  private final FilterService filterService;

  @SuppressWarnings("WeakerAccess" /* constructor must be public for blueprint access */)
  public WorkspaceQueryBuilder(FilterBuilder filterBuilder, FilterService filterService) {
    this.filterBuilder = filterBuilder;
    this.filterService = filterService;
  }

  @SuppressWarnings("WeakerAccess" /* this method could be package-private, but codacy complains */)
  public Filter createFilter(Set<String> workspaceIds) {
    return createAndFilter(createWorkspaceTagFilter(), createOrWorkspaceIdsFilter(workspaceIds));
  }

  private Filter createOrWorkspaceIdsFilter(Set<String> workspaceIds) {
    return createOrFilter(createWorkspaceIdFilters(workspaceIds));
  }

  private List<Filter> createWorkspaceIdFilters(Set<String> workspaceIds) {
    return workspaceIds
        .stream()
        .map(filterService::buildMetacardIdFilter)
        .collect(Collectors.toList());
  }

  private Filter createOrFilter(List<Filter> filters) {
    return filterBuilder.anyOf(filters);
  }

  private Filter createAndFilter(Filter... filters) {
    return filterBuilder.allOf(filters);
  }

  private Filter createWorkspaceTagFilter() {
    return filterService.buildWorkspaceTagFilter();
  }
}
