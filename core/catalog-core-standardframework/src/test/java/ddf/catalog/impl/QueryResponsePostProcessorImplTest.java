/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;

@RunWith(MockitoJUnitRunner.class)
public class QueryResponsePostProcessorImplTest {

    private static final String URL1 = "https://localhost/abc";

    private static final String URL2 = "https://localhost/def";

    @Mock
    private ActionProvider resourceActionProvider;

    @Mock
    private QueryResponse queryResponse;

    private List<Result> results;

    private Metacard[] metacards = {mock(Metacard.class), mock(Metacard.class)};

    private Action[] resourceActions = {mock(Action.class), mock(Action.class)};

    private URI[] uris = new URI[2];

    private URL[] urls = new URL[2];

    @Before
    public void setUp() throws Exception {
        uris[0] = new URI("content:abc");
        urls[0] = new URL(URL1);

        uris[1] = new URI("content:def");
        urls[1] = new URL(URL2);

        results = new ArrayList<Result>();
        results.add(mock(Result.class));
        results.add(mock(Result.class));
    }

    @Test
    public void testProcessRequest() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                resourceActionProvider);

        when(queryResponse.getResults()).thenReturn(results);

        setUpExpectationsForResult(0);
        setUpExpectationsForResult(1);

        queryResponsePostProcessor.processResponse(queryResponse);

        verifyMetacardAttribute(metacards[0], "resource-download-url", URL1);
        verifyMetacardAttribute(metacards[1], "resource-download-url", URL2);
    }

    @Test
    public void testProcessRequestWhenResourceActionProviderIsNull() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                null);
        queryResponsePostProcessor.processResponse(queryResponse);

        verifyZeroInteractions(queryResponse);
    }

    @Test
    public void testProcessRequestWithEmptyResult() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                resourceActionProvider);
        List<Result> emptyResult = new ArrayList<Result>();

        when(queryResponse.getResults()).thenReturn(emptyResult);

        queryResponsePostProcessor.processResponse(queryResponse);

        verifyZeroInteractions(metacards[0], metacards[1]);
    }

    @Test
    public void testProcessRequestWhenResourceUriIsNull() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                resourceActionProvider);

        when(queryResponse.getResults()).thenReturn(results);

        when(results.get(0).getMetacard()).thenReturn(metacards[0]);
        when(metacards[0].getResourceURI()).thenReturn(null);

        setUpExpectationsForResult(1);

        queryResponsePostProcessor.processResponse(queryResponse);

        verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
        verifyMetacardAttribute(metacards[1], "resource-download-url", URL2);
    }

    @Test
    public void testProcessRequestWhenActionNotFoundForMetacard() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                resourceActionProvider);

        when(queryResponse.getResults()).thenReturn(results);

        when(results.get(0).getMetacard()).thenReturn(metacards[0]);
        when(metacards[0].getResourceURI()).thenReturn(uris[0]);
        when(resourceActionProvider.getAction(metacards[0])).thenReturn(null);

        setUpExpectationsForResult(1);

        queryResponsePostProcessor.processResponse(queryResponse);

        verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
        verifyMetacardAttribute(metacards[1], "resource-download-url", URL2);
    }

    @Test
    public void testProcessRequestWhenActionReturnsNullUrl() {
        QueryResponsePostProcessorImpl queryResponsePostProcessor = new QueryResponsePostProcessorImpl(
                resourceActionProvider);

        when(queryResponse.getResults()).thenReturn(results);

        when(results.get(0).getMetacard()).thenReturn(metacards[0]);
        when(metacards[0].getResourceURI()).thenReturn(uris[0]);
        when(resourceActionProvider.getAction(metacards[0])).thenReturn(resourceActions[0]);
        when(resourceActions[0].getUrl()).thenReturn(null);

        setUpExpectationsForResult(1);

        queryResponsePostProcessor.processResponse(queryResponse);

        verify(metacards[0], never()).setAttribute(any(AttributeImpl.class));
        verifyMetacardAttribute(metacards[1], "resource-download-url", URL2);
    }

    private void setUpExpectationsForResult(int resultNumber) {
        when(results.get(resultNumber).getMetacard()).thenReturn(metacards[resultNumber]);
        when(metacards[resultNumber].getResourceURI()).thenReturn(uris[resultNumber]);
        when(resourceActionProvider.getAction(metacards[resultNumber])).thenReturn(
                resourceActions[resultNumber]);
        when(resourceActions[resultNumber].getUrl()).thenReturn(urls[resultNumber]);
    }

    private void verifyMetacardAttribute(Metacard metacard, String attributeName,
            String expectedValue) {
        ArgumentCaptor<AttributeImpl> attribute1 = ArgumentCaptor.forClass(AttributeImpl.class);
        verify(metacard).setAttribute(attribute1.capture());
        assertThat(attribute1.getValue().getName(), is(attributeName));
        assertThat(attribute1.getValue().getValues().size(), is(1));
        assertThat(attribute1.getValue().getValues(), hasItems((Serializable) expectedValue));
    }
}
