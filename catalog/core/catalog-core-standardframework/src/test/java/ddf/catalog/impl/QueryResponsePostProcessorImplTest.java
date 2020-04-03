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
package ddf.catalog.impl;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.MultiActionProvider;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryResponsePostProcessorImplTest {

  private static final String URL1 = "https://localhost/abc";

  private static final String URL2 = "https://localhost/def";

  private static final String DERIVED_URL1 = "https://localhost/abc?options=qualifier";

  private static final String DERIVED_URL2 = "https://localhost/def?options=other";

  @Mock private ActionProvider resourceActionProvider;

  @Mock private MultiActionProvider derivedActionProvider;

  @Mock private QueryResponse queryResponse;

  private Attribute[] attrs = {mock(Attribute.class), mock(Attribute.class)};

  private List<Result> results;

  private Metacard[] metacards = {mock(Metacard.class), mock(Metacard.class)};

  private Action[] resourceActions = {mock(Action.class), mock(Action.class)};

  private Action[] derivedResourceActions = {mock(Action.class), mock(Action.class)};

  private URI[] uris = new URI[2];

  private URL[] urls = new URL[2];

  private URI[] derivedUris = new URI[2];

  private URL[] derivedUrls = new URL[2];

  private QueryResponsePostProcessor queryResponsePostProcessor;

  @Before
  public void setUp() throws Exception {
    uris[0] = new URI("content:abc");
    urls[0] = new URL(URL1);

    uris[1] = new URI("content:def");
    urls[1] = new URL(URL2);

    derivedUris[0] = new URI("content:abc#qualifier");
    derivedUrls[0] = new URL(DERIVED_URL1);

    derivedUris[1] = new URI("content:def#other");
    derivedUrls[1] = new URL(DERIVED_URL2);

    results = new ArrayList<>();
    results.add(mock(Result.class));
    results.add(mock(Result.class));

    queryResponsePostProcessor =
        new QueryResponsePostProcessor(resourceActionProvider, derivedActionProvider);
  }

  @Test
  public void testProcessRequest() {

    when(queryResponse.getResults()).thenReturn(results);

    setUpExpectationsForResult(0);
    setUpExpectationsForResult(1);

    queryResponsePostProcessor.processResponse(queryResponse);

    verifyMetacardAttribute(metacards[0], Metacard.RESOURCE_DOWNLOAD_URL, URL1);
    verifyMetacardAttribute(metacards[1], Metacard.RESOURCE_DOWNLOAD_URL, URL2);

    verifyMetacardAttribute(metacards[0], Metacard.DERIVED_RESOURCE_DOWNLOAD_URL, DERIVED_URL1);
    verifyMetacardAttribute(metacards[1], Metacard.DERIVED_RESOURCE_DOWNLOAD_URL, DERIVED_URL2);
  }

  @Test
  public void testProcessRequestWhenResourceActionProviderIsNull() {
    QueryResponsePostProcessor queryResponsePostProcessor =
        new QueryResponsePostProcessor(null, null);
    queryResponsePostProcessor.processResponse(queryResponse);

    verifyZeroInteractions(queryResponse);
  }

  @Test
  public void testProcessRequestWithEmptyResult() {
    List<Result> emptyResult = new ArrayList<Result>();

    when(queryResponse.getResults()).thenReturn(emptyResult);

    queryResponsePostProcessor.processResponse(queryResponse);

    verifyZeroInteractions(metacards[0], metacards[1]);
  }

  @Test
  public void testProcessRequestWhenResourceUriIsNull() {
    when(queryResponse.getResults()).thenReturn(results);

    when(results.get(0).getMetacard()).thenReturn(metacards[0]);
    when(metacards[0].getResourceURI()).thenReturn(null);

    setUpExpectationsForResult(1);

    queryResponsePostProcessor.processResponse(queryResponse);

    verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
    verifyMetacardAttribute(metacards[1], Metacard.RESOURCE_DOWNLOAD_URL, URL2);
  }

  @Test
  public void testProcessRequestWhenActionNotFoundForMetacard() {
    when(queryResponse.getResults()).thenReturn(results);

    when(results.get(0).getMetacard()).thenReturn(metacards[0]);
    when(metacards[0].getResourceURI()).thenReturn(uris[0]);
    when(resourceActionProvider.getAction(metacards[0])).thenReturn(null);

    setUpExpectationsForResult(1);

    queryResponsePostProcessor.processResponse(queryResponse);

    verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
    verifyMetacardAttribute(metacards[1], Metacard.RESOURCE_DOWNLOAD_URL, URL2);
  }

  @Test
  public void testProcessRequestWhenActionReturnsNullUrl() {
    when(queryResponse.getResults()).thenReturn(results);

    when(results.get(0).getMetacard()).thenReturn(metacards[0]);
    when(metacards[0].getResourceURI()).thenReturn(uris[0]);
    when(resourceActionProvider.getAction(metacards[0])).thenReturn(resourceActions[0]);
    when(resourceActions[0].getUrl()).thenReturn(null);

    setUpExpectationsForResult(1);

    queryResponsePostProcessor.processResponse(queryResponse);

    verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
    verifyMetacardAttribute(metacards[1], Metacard.RESOURCE_DOWNLOAD_URL, URL2);
  }

  private void setUpExpectationsForResult(int resultNumber) {
    when(results.get(resultNumber).getMetacard()).thenReturn(metacards[resultNumber]);
    when(metacards[resultNumber].getResourceURI()).thenReturn(uris[resultNumber]);
    when(resourceActionProvider.getAction(metacards[resultNumber]))
        .thenReturn(resourceActions[resultNumber]);
    when(resourceActions[resultNumber].getUrl()).thenReturn(urls[resultNumber]);

    when(metacards[resultNumber].getAttribute(Metacard.DERIVED_RESOURCE_URI))
        .thenReturn(attrs[resultNumber]);
    when(attrs[resultNumber].getValues()).thenReturn(Arrays.asList(derivedUris[resultNumber]));
    when(derivedActionProvider.getActions(metacards[resultNumber]))
        .thenReturn(Arrays.asList(derivedResourceActions[resultNumber]));
    when(derivedResourceActions[resultNumber].getUrl()).thenReturn(derivedUrls[resultNumber]);
  }

  private void verifyMetacardAttribute(
      Metacard metacard, String attributeName, String expectedValue) {
    ArgumentCaptor<AttributeImpl> attribute1 = ArgumentCaptor.forClass(AttributeImpl.class);
    verify(metacard, times(2)).setAttribute(attribute1.capture());
    AttributeImpl actualAttribute =
        attribute1.getAllValues().stream()
            .filter(attribute -> attributeName.equals(attribute.getName()))
            .collect(Collectors.toList())
            .get(0);
    assertThat(actualAttribute.getName(), is(attributeName));
    assertThat(actualAttribute.getValues().size(), is(1));
    assertThat(actualAttribute.getValues(), hasItems((Serializable) expectedValue));
  }
}
