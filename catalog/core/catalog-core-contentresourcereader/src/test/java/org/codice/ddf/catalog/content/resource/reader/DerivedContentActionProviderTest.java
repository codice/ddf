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
package org.codice.ddf.catalog.content.resource.reader;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DerivedContentActionProviderTest {

  public static final String EXAMPLE_URL = "https://example.com/other";

  private static DerivedContentActionProvider actionProvider;

  private static ActionProvider mockResourceActionProvider = mock(ActionProvider.class);

  private static final String CONTENT_ID = UUID.randomUUID().toString();

  private static Metacard metacard = mock(Metacard.class);

  private static Attribute attribute = mock(Attribute.class);

  private static URI actionUri;

  private static URI derivedResourceUri;

  private static final String QUALIFIER_VALUE = "value";

  @BeforeClass
  public static void init() throws URISyntaxException {
    actionUri =
        new URI("https://example.com/download?transform=resource&qualifier=" + QUALIFIER_VALUE);
    derivedResourceUri = new URI(ContentItem.CONTENT_SCHEME, CONTENT_ID, QUALIFIER_VALUE);
    actionProvider = new DerivedContentActionProvider(mockResourceActionProvider);
  }

  @Before
  public void setUp() {
    when(metacard.getId()).thenReturn(CONTENT_ID);
    when(metacard.getAttribute(Core.DERIVED_RESOURCE_URI)).thenReturn(attribute);
    when(attribute.getValues()).thenReturn(Arrays.asList(derivedResourceUri.toString()));
  }

  @Test
  public void testGetActions() throws Exception {
    ActionImpl expectedAction =
        new ActionImpl("expected", "expected", "expected", actionUri.toURL());
    when(mockResourceActionProvider.getAction(any(Metacard.class))).thenReturn(expectedAction);
    List<Action> actions = actionProvider.getActions(metacard);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0), notNullValue());
    assertThat(actions.get(0).getUrl(), notNullValue());
    assertThat(
        actions.get(0).getUrl().getQuery(),
        containsString(ContentItem.QUALIFIER_KEYWORD + "=" + QUALIFIER_VALUE));
  }

  @Test
  public void testGetActionsNonContentUriDefault() throws Exception {
    ActionImpl expectedAction =
        new ActionImpl("expected", "expected", "expected", actionUri.toURL());
    when(mockResourceActionProvider.getAction(any(Metacard.class))).thenReturn(expectedAction);
    when(attribute.getValues()).thenReturn(Arrays.asList(EXAMPLE_URL));
    List<Action> actions = actionProvider.getActions(metacard);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0), notNullValue());
    assertThat(actions.get(0).getUrl(), notNullValue());
    assertThat(actions.get(0).getUrl(), is(new URL(EXAMPLE_URL)));
  }

  @Test
  public void testGetActionsNonContentUriWithQualifier() throws Exception {
    ActionImpl expectedAction =
        new ActionImpl("expected", "expected", "expected", actionUri.toURL());
    when(mockResourceActionProvider.getAction(any(Metacard.class))).thenReturn(expectedAction);
    when(attribute.getValues()).thenReturn(Arrays.asList(actionUri));
    List<Action> actions = actionProvider.getActions(metacard);
    assertThat(actions, hasSize(1));
    assertThat(actions.get(0), notNullValue());
    assertThat(actions.get(0).getUrl(), notNullValue());
    assertThat(actions.get(0).getUrl().toString(), is(actionUri.toString()));
    assertThat(actions.get(0).getTitle(), is("View " + QUALIFIER_VALUE));
  }

  @Test
  public void testCanHandle() throws Exception {
    assertThat(actionProvider.canHandle(metacard), is(true));
  }

  @Test
  public void testCanHandleMetacardWithNoDerivedResources() throws Exception {
    when(metacard.getAttribute(Core.DERIVED_RESOURCE_URI)).thenReturn(null);
    assertThat(actionProvider.canHandle(metacard), is(false));
  }

  @Test
  public void testCanHandleMetacardWithEmptyDerivedResources() throws Exception {
    when(attribute.getValues()).thenReturn(Collections.emptyList());
    assertThat(actionProvider.canHandle(metacard), is(false));
  }

  @Test
  public void testCanHandleNonMetacard() throws Exception {
    assertThat(actionProvider.canHandle("bad"), is(false));
  }

  @Test
  public void testCanHandleNull() throws Exception {
    assertThat(actionProvider.canHandle(null), is(false));
  }
}
