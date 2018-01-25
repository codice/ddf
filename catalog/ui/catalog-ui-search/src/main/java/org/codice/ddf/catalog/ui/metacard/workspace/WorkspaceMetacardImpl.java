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

import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkspaceMetacardImpl extends MetacardImpl {

  public static final MetacardType TYPE = new WorkspaceMetacardType();

  public WorkspaceMetacardImpl() {
    super(TYPE);
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
   * Get a copy of a worksapce metacard.
   *
   * @param metacard
   * @return
   */
  public static WorkspaceMetacardImpl clone(Metacard metacard) {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

    metacard
        .getMetacardType()
        .getAttributeDescriptors()
        .stream()
        .forEach(descriptor -> workspace.setAttribute(metacard.getAttribute(descriptor.getName())));

    return workspace;
  }

  /**
   * Check if a given metacard is a workspace metacard by checking the tags metacard attribute.
   *
   * @param metacard
   * @return
   */
  public static boolean isWorkspaceMetacard(Metacard metacard) {
    if (metacard != null) {
      return metacard
          .getTags()
          .stream()
          .filter(WorkspaceAttributes.WORKSPACE_TAG::equals)
          .findFirst()
          .isPresent();
    }

    return false;
  }

  /**
   * Compute the symmetric difference between the sharing permissions of two workspaces.
   *
   * @param m - metacard to diff against
   * @return
   */
  public Set<String> diffSharing(Metacard m) {
    if (isWorkspaceMetacard(m)) {
      return Sets.symmetricDifference(getSharing(), from(m).getSharing());
    }
    return Collections.emptySet();
  }

  public List<String> getMetacards() {
    return getValues(Associations.RELATED);
  }

  public void setMetacards(List<String> items) {
    setAttribute(Associations.RELATED, new ArrayList<>(items));
  }

  private List<String> getValues(String attribute) {
    Attribute attr = getAttribute(attribute);

    if (attr != null) {
      return attr.getValues().stream().map(String::valueOf).collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  public List<String> getQueries() {
    return getValues(WorkspaceAttributes.WORKSPACE_QUERIES);
  }

  public WorkspaceMetacardImpl setQueries(List<String> queries) {
    setAttribute(WorkspaceAttributes.WORKSPACE_QUERIES, new ArrayList<>(queries));
    return this;
  }

  public List<String> getContent() {
    return getValues(WorkspaceAttributes.WORKSPACE_LISTS);
  }

  public WorkspaceMetacardImpl setContent(List<String> lists) {
    setAttribute(WorkspaceAttributes.WORKSPACE_LISTS, new ArrayList<>(lists));
    return this;
  }

  public String getOwner() {
    List<String> values = getValues(Core.METACARD_OWNER);

    if (!values.isEmpty()) {
      return values.get(0);
    }

    return null;
  }

  public WorkspaceMetacardImpl setOwner(String email) {
    setAttribute(Core.METACARD_OWNER, email);
    return this;
  }

  public Set<String> getSharing() {
    return new HashSet<>(getValues(WorkspaceAttributes.WORKSPACE_SHARING));
  }

  public WorkspaceMetacardImpl setSharing(Set<String> sharing) {
    setAttribute(WorkspaceAttributes.WORKSPACE_SHARING, new ArrayList<>(sharing));
    return this;
  }
}
