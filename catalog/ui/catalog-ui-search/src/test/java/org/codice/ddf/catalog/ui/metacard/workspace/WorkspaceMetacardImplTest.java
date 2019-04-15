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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlUtil;
import org.junit.Before;
import org.junit.Test;

public class WorkspaceMetacardImplTest {

  private WorkspaceMetacardImpl workspace;

  @Before
  public void setUp() {
    workspace = new WorkspaceMetacardImpl();
  }

  @Test
  public void testWorkspaceMetacardFrom() {
    WorkspaceMetacardImpl wrapped = WorkspaceMetacardImpl.from(new MetacardImpl(workspace));
    wrapped.setId("0");
    assertThat(wrapped.getId(), is("0"));
    assertThat(workspace.getId(), is("0"));
  }

  @Test
  public void testGetWorkspaceQueries() {
    WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    workspaceMetacard.setQueries(Arrays.asList("queryId1", "queryId2"));

    List<String> queries = workspaceMetacard.getQueries();

    assertThat(queries, hasSize(2));
    assertThat(queries.get(0), is("queryId1"));
    assertThat(queries.get(1), is("queryId2"));
  }

  @Test
  public void testGetEmptyWorkspaceQueries() {
    WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    List<String> queries = workspaceMetacard.getQueries();

    assertThat(queries, hasSize(0));
  }

  @Test
  public void testAddWorkspaceQuery() {
    WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    workspaceMetacard.addQueryAssociation("queryId");

    List<String> queries = workspaceMetacard.getQueries();

    assertThat(queries, hasSize(1));
    assertThat(queries, contains("queryId"));
  }

  @Test
  public void testRemoveWorkspaceQuery() {
    WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    workspaceMetacard.addQueryAssociation("queryId1");
    workspaceMetacard.addQueryAssociation("queryId2");

    workspaceMetacard.removeQueryAssociation("queryId1");

    List<String> queries = workspaceMetacard.getQueries();

    assertThat(queries, hasSize(1));
    assertThat(queries, contains("queryId2"));
  }

  @Test
  public void testGetWorkspaceMetacardFromMap() {
    final ImmutableMap<String, Serializable> metacardAttributes =
        new ImmutableMap.Builder<String, Serializable>()
            .put(Core.ID, "workspaceId")
            .put(Core.METACARD_TAGS, "workspace")
            .put(WorkspaceConstants.WORKSPACE_QUERIES, "queryId")
            .put(Core.TITLE, "title")
            .put(Core.METACARD_OWNER, "admin")
            .build();

    WorkspaceMetacardImpl workspaceMetacard = WorkspaceMetacardImpl.from(metacardAttributes);

    assertAttributeValueIs(workspaceMetacard, Core.ID, "workspaceId");
    assertAttributeValueIs(workspaceMetacard, Core.METACARD_TAGS, "workspace");
    assertAttributeValueIs(workspaceMetacard, WorkspaceConstants.WORKSPACE_QUERIES, "queryId");
    assertAttributeValueIs(workspaceMetacard, Core.TITLE, "title");
    assertAttributeValueIs(workspaceMetacard, Core.METACARD_OWNER, "admin");
  }

  @Test
  public void testIsWorkspaceMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTags(ImmutableSet.of("workspace"));

    boolean isWorkspace = WorkspaceMetacardImpl.isWorkspaceMetacard(metacard);

    assertThat(isWorkspace, is(true));
  }

  @Test
  public void testIsNotWorkspaceMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setTags(ImmutableSet.of("hello", "world"));

    boolean isWorkspace = WorkspaceMetacardImpl.isWorkspaceMetacard(metacard);

    assertThat(isWorkspace, is(false));
  }

  @Test
  public void testIsNullWorkspaceMetacard() {
    boolean isWorkspace = WorkspaceMetacardImpl.isWorkspaceMetacard(null);

    assertThat(isWorkspace, is(false));
  }

  @Test
  public void testOwner() {
    String owner = "owner@localhost";
    workspace = (WorkspaceMetacardImpl) AccessControlUtil.setOwner(workspace, owner);
    assertThat(AccessControlUtil.getOwner(workspace), is(owner));
  }

  private void assertAttributeValueIs(Metacard metacard, String attrKey, Serializable value) {
    Attribute attr = metacard.getAttribute(attrKey);
    assertThat(attr.getValue(), is(value));
  }
}
