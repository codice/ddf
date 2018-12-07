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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import static java.util.Collections.singletonList;
import static org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl.EmbeddedListMetacardsHandler.ACTIONS_KEY;
import static org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl.EmbeddedListMetacardsHandler.LIST_ACTION_PREFIX;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Metacard;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EmbeddedListMetacardsHandlerTest {

  private EmbeddedListMetacardsHandler embeddedListMetacardsHandler;

  @Mock private WorkspaceTransformer workspaceTransformer;

  @Mock private ActionRegistry actionRegistry;

  @Mock private Metacard workspaceMetacard;

  @Captor private ArgumentCaptor<Map<String, Object>> jsonArgumentCaptor;

  @Before
  public void setup() throws MalformedURLException {
    embeddedListMetacardsHandler = new EmbeddedListMetacardsHandler(actionRegistry);
    final Action action = getMockAction();
    doReturn(singletonList(action))
        .when(actionRegistry)
        .list(
            argThat(
                Matchers.<Map<String, Metacard>>allOf(
                    hasEntry("workspace", workspaceMetacard),
                    hasEntry(is("list"), isA(Metacard.class)))));
  }

  @Test
  public void removeExternalListAttributes() {
    final Map<String, Object> metacardMap = new HashMap<>();
    metacardMap.put("actions", "actionAttributes");
    metacardMap.put("other", "otherAttributes");

    embeddedListMetacardsHandler.jsonValueToMetacardValue(
        workspaceTransformer, singletonList(metacardMap));

    verify(workspaceTransformer)
        .transformIntoMetacard(jsonArgumentCaptor.capture(), any(Metacard.class));
    final List<Map<String, Object>> metacardMaps = jsonArgumentCaptor.getAllValues();

    assertThat(metacardMaps, hasSize(1));

    final Map<String, Object> transformedMetacardMap = metacardMaps.get(0);
    assertThat(transformedMetacardMap, hasEntry("other", "otherAttributes"));
    assertThat(transformedMetacardMap, not(hasKey("actions")));
  }

  @Test
  public void addListActions() {
    final Map<String, Object> metacardMap = new HashMap<>();
    metacardMap.put("other", "otherAttributes");

    when(workspaceTransformer.xmlToMetacard(anyString())).thenReturn(mock(Metacard.class));
    when(workspaceTransformer.transform(any(Metacard.class), any(Metacard.class)))
        .thenReturn(metacardMap);

    final Optional<List> listMetacardsOptional =
        embeddedListMetacardsHandler.metacardValueToJsonValue(
            workspaceTransformer, singletonList(""), workspaceMetacard);

    assertTrue(
        "The handler did not return any list metacard JSON maps.",
        listMetacardsOptional.isPresent());

    final List<Object> listMetacards = listMetacardsOptional.get();
    assertThat(listMetacards, hasSize(1));

    final Map listMetacardMap = (Map) listMetacards.get(0);
    verifyActionList(listMetacardMap);
  }

  private void verifyActionList(Map<String, Object> listMetacardMap) {
    assertThat(listMetacardMap, hasKey(ACTIONS_KEY));
    List<Map<String, Object>> listMetacardActions = (List) listMetacardMap.get(ACTIONS_KEY);
    assertThat(listMetacardActions, hasSize(1));
    final Map<String, Object> actionMap = listMetacardActions.get(0);
    assertThat(actionMap, hasEntry("id", LIST_ACTION_PREFIX + ".test"));
    assertThat(actionMap, hasEntry("title", "title"));
    assertThat(actionMap, hasEntry("description", "description"));

    try {
      assertThat(actionMap, hasEntry("url", new URL("http://test.com")));
    } catch (MalformedURLException e) {
      fail(e.getMessage());
    }
  }

  private Action getMockAction() throws MalformedURLException {
    final Action action = mock(Action.class);
    doReturn(LIST_ACTION_PREFIX + ".test").when(action).getId();
    doReturn("title").when(action).getTitle();
    doReturn("description").when(action).getDescription();

    URL url = new URL("http://test.com");
    doReturn(url).when(action).getUrl();
    return action;
  }
}
