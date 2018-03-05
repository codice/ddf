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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.QueryMetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.InputTransformer;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class WorkspaceTransformerTest {

  private CatalogFramework cf;

  private InputTransformer it;

  private WorkspaceTransformer wt;

  private EndpointUtil ut;

  @Before
  public void setup() throws Exception {
    cf = Mockito.mock(CatalogFramework.class);

    doReturn(new BinaryContentImpl(IOUtils.toInputStream("<xml></xml>")))
        .when(cf)
        .transform(any(Metacard.class), any(String.class), any(Map.class));

    it = Mockito.mock(InputTransformer.class);

    doReturn(new QueryMetacardImpl("my query")).when(it).transform(any(InputStream.class));

    ut = Mockito.mock(EndpointUtil.class);

    wt = new WorkspaceTransformer(cf, it, ut);
  }

  // test metacard -> map

  @Test
  public void testMetacardToMapDirectMapping() {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
    workspace.setTitle("title");
    Map<String, Object> map = wt.transform(workspace);
    assertThat(map.get(Core.TITLE), is(workspace.getTitle()));
  }

  @Test
  public void testMetacardToMapFilteredKeys() {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
    Map<String, Object> map = wt.transform(workspace);
    assertThat(map.get(Core.METACARD_TAGS), nullValue());
  }

  @Test
  public void testMetacardToMapRemapKeys() {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
    workspace.setMetacards(ImmutableList.of("item1", "item2"));
    Map<String, Object> map = wt.transform(workspace);
    assertThat(map.get(WorkspaceAttributes.WORKSPACE_METACARDS), is(workspace.getMetacards()));
  }

  @Test
  public void testMetacardToMapRemapValues() {
    WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();
    workspace.setQueries(ImmutableList.of("<xml></xml>"));
    Map<String, Object> map = wt.transform(workspace);
    assertThat(
        map.get(WorkspaceAttributes.WORKSPACE_QUERIES),
        is(ImmutableList.of(ImmutableMap.of("title", "my query"))));
  }

  // test map -> metacard

  @Ignore // TODO (RCZ) - AttributeRegistry Mock?
  @Test
  public void testMapToMetacardDirectMapping() {
    Map<String, Object> map = ImmutableMap.of(Core.TITLE, "title");
    WorkspaceMetacardImpl workspace = (WorkspaceMetacardImpl) wt.transform(map);
    assertThat(workspace.getTitle(), is(map.get(Core.TITLE)));
  }

  @Ignore // TODO (RCZ) - AttributeRegistry Mock?
  @Test
  public void testMapToMetacardRemapKeys() {
    Map<String, Object> map =
        ImmutableMap.of(
            WorkspaceAttributes.WORKSPACE_METACARDS, ImmutableList.of("item1", "item2"));
    WorkspaceMetacardImpl workspace = (WorkspaceMetacardImpl) wt.transform(map);
    assertThat(workspace.getMetacards(), is(map.get(WorkspaceAttributes.WORKSPACE_METACARDS)));
  }

  @Ignore // TODO (RCZ) - AttributeRegistry Mock?
  @Test
  public void testMapToMetacardRemapValues() {
    Map<String, Object> map =
        ImmutableMap.of(
            WorkspaceAttributes.WORKSPACE_QUERIES,
            ImmutableList.of(ImmutableMap.of("title", "my query")));
    WorkspaceMetacardImpl workspace = (WorkspaceMetacardImpl) wt.transform(map);
    assertThat(workspace.getQueries(), is(ImmutableList.of("<xml></xml>")));
  }
}
