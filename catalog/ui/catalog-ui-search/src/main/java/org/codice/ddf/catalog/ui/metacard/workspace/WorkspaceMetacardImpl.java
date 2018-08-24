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
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.ui.security.AccessControlUtil;

public class WorkspaceMetacardImpl extends MetacardImpl {

  public static final MetacardType TYPE = new WorkspaceMetacardType();

  public WorkspaceMetacardImpl() {
    super(TYPE);
    setTags(Collections.singleton(WorkspaceConstants.WORKSPACE_TAG));
  }

  public WorkspaceMetacardImpl(String id) {
    this();
    setId(id);
  }

  public WorkspaceMetacardImpl(Metacard metacard) {
    super(metacard);
  }

  /** Wrap any metacard as a WorkspaceMetacardImpl. */
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

  public Metacard setOwner(String email) {
    setAttribute(Core.METACARD_OWNER, email);
    return this;
  }

  /** Check if a given metacard is a workspace metacard by checking the tags metacard attribute. */
  public static boolean isWorkspaceMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().contains(WorkspaceConstants.WORKSPACE_TAG);
  }

  public List<String> getMetacards() {
    return AccessControlUtil.getValuesOrEmpty(this, Associations.RELATED);
  }

  public void setMetacards(List<String> items) {
    setAttribute(Associations.RELATED, new ArrayList<>(items));
  }

  public List<String> getQueries() {
    return AccessControlUtil.getValuesOrEmpty(this, WorkspaceConstants.WORKSPACE_QUERIES);
  }

  public WorkspaceMetacardImpl setQueries(List<String> queries) {
    setAttribute(WorkspaceConstants.WORKSPACE_QUERIES, new ArrayList<>(queries));
    return this;
  }

  public List<String> getContent() {
    return AccessControlUtil.getValuesOrEmpty(this, WorkspaceConstants.WORKSPACE_LISTS);
  }

  public WorkspaceMetacardImpl setContent(List<String> lists) {
    setAttribute(WorkspaceConstants.WORKSPACE_LISTS, new ArrayList<>(lists));
    return this;
  }
}
