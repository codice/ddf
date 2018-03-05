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
package org.codice.ddf.catalog.ui.query.monitor.api;

import ddf.catalog.data.impl.QueryMetacardImpl;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;

public interface WorkspaceService {

  /**
   * Get a collection of workspace metacards.
   *
   * @return collection of workspace metacards (non-null)
   */
  List<WorkspaceMetacardImpl> getWorkspaceMetacards();

  /**
   * Get a list of query metacards for a workspace metacard.
   *
   * @param workspaceMetacard must be non-null
   * @return list of query metacards
   */
  List<QueryMetacardImpl> getQueryMetacards(WorkspaceMetacardImpl workspaceMetacard);

  /**
   * Get a list of workspace metacards for a set of workspace identifiers.
   *
   * @param workspaceIds must be non-null
   * @return list of workspace metacards (non-null)
   */
  List<WorkspaceMetacardImpl> getWorkspaceMetacards(Set<String> workspaceIds);

  /**
   * Uses {@link #getWorkspaceMetacards()} and {@link #getQueryMetacards(WorkspaceMetacardImpl)} to
   * get a map of workspace metacard to a list of query metacards.
   *
   * @return map of workspace metacards to a list of query metacards
   */
  Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> getQueryMetacards();

  /**
   * Uses {@link #getWorkspaceMetacards(Set)} to get a single workspace metacard.
   *
   * @param workspaceId workspace identifier
   * @return workspace metacard
   */
  @SuppressWarnings("unused")
  WorkspaceMetacardImpl getWorkspaceMetacard(String workspaceId);
}
