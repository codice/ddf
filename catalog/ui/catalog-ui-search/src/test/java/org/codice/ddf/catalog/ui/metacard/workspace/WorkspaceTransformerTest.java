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

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.codice.ddf.catalog.ui.metacard.workspace.ListMetacardTypeImpl.LIST_BOOKMARKS;
import static org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes.WORKSPACE_LISTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.action.impl.ActionImpl;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.InputTransformer;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceTransformerTest {
  @Mock private CatalogFramework cf;

  @Mock private InputTransformer it;

  @Mock private EndpointUtil ut;

  @Mock private ActionRegistry actionRegistry;

  private WorkspaceTransformer wt;

  @Before
  public void setup() throws Exception {
    doReturn(new BinaryContentImpl(IOUtils.toInputStream("<xml></xml>")))
        .when(cf)
        .transform(any(Metacard.class), any(String.class), any(Map.class));

    doReturn(new QueryMetacardImpl("my query")).when(it).transform(any(InputStream.class));

    doReturn(emptyList()).when(actionRegistry).list(any());

    wt = new WorkspaceTransformer(cf, it, ut, actionRegistry);
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

  @Test
  public void testAddListActions() throws MalformedURLException {
    final WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    workspaceMetacard.setContent(asList("list1Xml", "list2Xml"));
    final Action action1 =
        new ActionImpl(
            "catalog.data.metacard.list.export",
            "Title1",
            "Description1",
            new URL("http://localhost:1"));
    final Action action2 =
        new ActionImpl(
            "catalog.data.metacard.xml", "Title2", "Description2", new URL("http://localhost:2"));
    final Action action3 =
        new ActionImpl(
            "catalog.data.metacard.list.transform",
            "Title3",
            "Description3",
            new URL("http://localhost:3"));
    doReturn(newArrayList(action1, action2, action3)).when(actionRegistry).list(anyMap());

    final List<Map<String, Object>> workspaceLists = new ArrayList<>();

    final Map<String, Object> list1AsMap = new HashMap<>();
    list1AsMap.put(LIST_BOOKMARKS, asList("bookmark1", "bookmark2"));
    workspaceLists.add(list1AsMap);

    final Map<String, Object> list2AsMap = new HashMap<>();
    list2AsMap.put(LIST_BOOKMARKS, asList("bookmark3", "bookmark4"));
    workspaceLists.add(list2AsMap);

    final Map<String, Object> workspaceAsMap = new HashMap<>();
    workspaceAsMap.put(WORKSPACE_LISTS, workspaceLists);

    // Stub convertDateEntries(Entry<String, Object>) to return the entry.
    doAnswer(invocationOnMock -> invocationOnMock.getArguments()[0])
        .when(ut)
        .convertDateEntries(any());

    wt.addListActions(workspaceMetacard, workspaceAsMap);

    final List<Map<String, Object>> list1Actions =
        (List<Map<String, Object>>) list1AsMap.get("actions");
    assertThat(list1Actions, containsInAnyOrder(action(action1), action(action3)));

    final List<Map<String, Object>> list2Actions =
        (List<Map<String, Object>>) list2AsMap.get("actions");
    assertThat(list2Actions, containsInAnyOrder(action(action1), action(action3)));
  }

  private static Matcher<Map<String, Object>> action(Action action) {
    return new ActionMatcher(action);
  }

  private static class ActionMatcher extends TypeSafeDiagnosingMatcher<Map<String, Object>> {
    private final Matcher<Map<String, Object>> matcher;

    private ActionMatcher(Action action) {
      this(action.getId(), action.getTitle(), action.getDescription(), action.getUrl());
    }

    private ActionMatcher(String id, String title, String description, URL url) {
      final Matcher<Map<? extends String, ?>> idMatcher = hasEntry("id", id);
      final Matcher<Map<? extends String, ?>> titleMatcher = hasEntry("title", title);
      final Matcher<Map<? extends String, ?>> descriptionMatcher =
          hasEntry("description", description);
      final Matcher<Map<? extends String, ?>> urlMatcher = hasEntry("url", url.toString());
      matcher = allOf(idMatcher, titleMatcher, descriptionMatcher, urlMatcher);
    }

    @Override
    protected boolean matchesSafely(Map<String, Object> map, Description description) {
      if (!matcher.matches(map)) {
        matcher.describeMismatch(map, description);
        return false;
      }

      return true;
    }

    @Override
    public void describeTo(Description description) {
      matcher.describeTo(description);
    }
  }
}
