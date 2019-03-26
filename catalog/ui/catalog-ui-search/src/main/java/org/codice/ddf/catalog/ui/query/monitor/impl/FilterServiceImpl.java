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

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import java.util.Date;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.opengis.filter.Filter;

public class FilterServiceImpl implements FilterService {

  private final FilterBuilder filterBuilder;

  public FilterServiceImpl(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @Override
  public Filter buildWorkspaceTagFilter() {
    return filterBuilder
        .attribute(Core.METACARD_TAGS)
        .is()
        .like()
        .text(WorkspaceConstants.WORKSPACE_TAG);
  }

  @Override
  public Filter buildMetacardIdFilter(String id) {
    return filterBuilder.attribute(Core.ID).is().equalTo().text(id);
  }

  @Override
  public Filter getModifiedDateFilter(Date lastCheckDate) {
    return filterBuilder.attribute(Core.MODIFIED).after().date(lastCheckDate);
  }

  @Override
  public String toString() {
    return "FilterServiceImpl{" + "filterBuilder=" + filterBuilder + '}';
  }
}
