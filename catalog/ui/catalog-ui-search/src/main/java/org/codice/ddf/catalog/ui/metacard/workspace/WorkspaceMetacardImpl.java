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
package org.codice.ddf.catalog.ui.metacard.workspace;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Associations;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.codice.ddf.catalog.ui.security.ShareableMetacardImpl;

public class WorkspaceMetacardImpl extends ShareableMetacardImpl {

  public static final MetacardType TYPE = new WorkspaceMetacardType();

  public WorkspaceMetacardImpl() {
    super(TYPE);
    Set<String> setOfTags = new HashSet<>();
    setTags(Collections.singleton(WorkspaceAttributes.WORKSPACE_TAG));
  }

  public WorkspaceMetacardImpl(String id) {
    this();
    setId(id);
  }

  public WorkspaceMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  /**
   * Wrap any metacard as a WorkspaceMetacardImpl.
   *
   * @param metacard
   * @return
   */
  public static WorkspaceMetacardImpl from(Metacard metacard) {
    return new WorkspaceMetacardImpl(metacard);
  }

  public static WorkspaceMetacardImpl from(Map<String, Serializable> attributes) {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

    attributes
        .entrySet()
        .stream()
        .forEach(entry -> workspace.setAttribute(entry.getKey(), entry.getValue()));

    return workspace;
  }

  /**
   * Check if a given metacard is a workspace metacard by checking the tags metacard attribute.
   *
   * @param metacard
   * @return
   */
  public static boolean isWorkspaceMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().contains(WorkspaceAttributes.WORKSPACE_TAG);
  }

  public List<String> getMetacards() {
    return getValuesOrEmpty(Associations.RELATED);
  }

  public void setMetacards(List<String> items) {
    setAttribute(Associations.RELATED, new ArrayList<>(items));
  }

  public List<String> getQueries() {
    return getValuesOrEmpty(WorkspaceAttributes.WORKSPACE_QUERIES);
  }

  public WorkspaceMetacardImpl setQueries(List<String> queries) {
    setAttribute(WorkspaceAttributes.WORKSPACE_QUERIES, new ArrayList<>(queries));
    return this;
  }

  public List<String> getContent() {
    return getValuesOrEmpty(WorkspaceAttributes.WORKSPACE_LISTS);
  }

  public WorkspaceMetacardImpl setContent(List<String> lists) {
    setAttribute(WorkspaceAttributes.WORKSPACE_LISTS, new ArrayList<>(lists));
    return this;
  }
}
