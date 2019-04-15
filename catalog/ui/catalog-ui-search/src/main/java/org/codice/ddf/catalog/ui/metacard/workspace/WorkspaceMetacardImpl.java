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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil;

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

  public static boolean isWorkspaceMetacard(Metacard metacard) {
    return metacard != null && metacard.getTags().contains(WorkspaceConstants.WORKSPACE_TAG);
  }

  public List<String> getMetacards() {
    return AccessControlUtil.getValuesOrEmpty(this, Associations.RELATED);
  }

  public void setMetacards(List<String> items) {
    setAttribute(WorkspaceConstants.WORKSPACE_QUERIES, new ArrayList<>(items));
  }

  public List<String> getQueries() {
    Attribute queryAttr = this.getAttribute(WorkspaceConstants.WORKSPACE_QUERIES);
    return queryAttr == null
        ? new ArrayList<>()
        : queryAttr
            .getValues()
            .stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .collect(Collectors.toList());
  }

  public void setQueries(List<String> queryIds) {
    setAttribute(WorkspaceConstants.WORKSPACE_QUERIES, new ArrayList<>(queryIds));
  }

  public void addQueryAssociation(String queryId) {
    List<String> queryIds = this.getQueries();
    queryIds.add(queryId);
    this.setQueries(queryIds);
  }

  public void removeQueryAssociation(String queryId) {
    List<String> queryIds = this.getQueries();
    queryIds.remove(queryId);
    this.setQueries(queryIds);
  }

  public List<String> getContent() {
    return AccessControlUtil.getValuesOrEmpty(this, WorkspaceConstants.WORKSPACE_LISTS);
  }

  public WorkspaceMetacardImpl setContent(List<String> lists) {
    setAttribute(WorkspaceConstants.WORKSPACE_LISTS, new ArrayList<>(lists));
    return this;
  }
}
