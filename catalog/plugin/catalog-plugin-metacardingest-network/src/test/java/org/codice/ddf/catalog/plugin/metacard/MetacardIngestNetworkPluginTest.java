/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.plugin.metacard;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.plugin.metacard.util.AttributeFactory;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.codice.ddf.catalog.plugin.metacard.util.MetacardServices;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;

/**
 * Ensure the rules and config options work correctly for any {@link MetacardIngestNetworkPlugin}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MetacardIngestNetworkPluginTest {

    private static final String CLIENT_INFO_KEY = "client-info";

    private static final String CRITERIA_KEY = "criteria-key";

    private static final String EXPECTED_VALUE = "criteria-value";

    private static final Map<String, Serializable> INFO_MAP = ImmutableMap.of();

    @Mock
    private CreateRequest mockCreateRequest;

    @Mock
    private Map<String, String> mockParsedAttributes;

    @Mock
    private KeyValueParser mockParser;

    @Mock
    private MetacardServices mockMetacardServices;

    @Mock
    private AttributeFactory mockAttributeFactory;

    @Mock
    private MetacardCondition mockMetacardCondition;

    private List<Metacard> metacards;

    private List<String> newAttributes;

    private MetacardIngestNetworkPlugin plugin;

    @Before
    public void setup() throws Exception {

        metacards = new ArrayList<>();
        newAttributes = new ArrayList<>();

        plugin = new MetacardIngestNetworkPlugin(mockParser,
                mockMetacardServices,
                mockAttributeFactory,
                mockMetacardCondition);

        when(mockCreateRequest.getMetacards()).thenReturn(metacards);
        when(mockParser.parsePairsToMap(newAttributes)).thenReturn(mockParsedAttributes);

        plugin.setNewAttributes(newAttributes);
    }

    @Test
    public void testClientInfoMapNull() throws Exception {
        ThreadContext.put(CLIENT_INFO_KEY, null);
        CreateRequest createRequest = plugin.processPreCreate(mockCreateRequest);
        verifyZeroInteractions(mockMetacardServices, mockMetacardCondition);
        assertThat(createRequest, is(mockCreateRequest));
    }

    @Test
    public void testClientInfoMapNotMap() throws Exception {
        ThreadContext.put(CLIENT_INFO_KEY, new Object());
        CreateRequest createRequest = plugin.processPreCreate(mockCreateRequest);
        verifyZeroInteractions(mockMetacardServices, mockMetacardCondition);
        assertThat(createRequest, is(mockCreateRequest));
    }

    @Test
    public void testApplySuceeds() throws Exception {
        ThreadContext.put(CLIENT_INFO_KEY, INFO_MAP);
        when(mockMetacardCondition.applies(INFO_MAP)).thenReturn(true);

        plugin.processPreCreate(mockCreateRequest);

        verify(mockMetacardServices).setAttributesIfAbsent(metacards,
                mockParsedAttributes,
                mockAttributeFactory);

        verifyNoMoreInteractions(mockMetacardServices);
    }

    @Test
    public void testApplyFails() throws Exception {
        ThreadContext.put(CLIENT_INFO_KEY, INFO_MAP);
        when(mockMetacardCondition.applies(INFO_MAP)).thenReturn(false);
        plugin.processPreCreate(mockCreateRequest);
        verifyZeroInteractions(mockMetacardServices);
    }

    @Test
    public void testGettersAndSetters() throws Exception {
        MetacardIngestNetworkPlugin networkPlugin = new MetacardIngestNetworkPlugin(mockParser,
                mockMetacardServices);

        networkPlugin.setCriteriaKey(CRITERIA_KEY);
        networkPlugin.setExpectedValue(EXPECTED_VALUE);
        networkPlugin.setNewAttributes(newAttributes);

        verify(mockParser, times(2)).parsePairsToMap(newAttributes);
        verifyNoMoreInteractions(mockParser);

        assertThat(networkPlugin.getCriteriaKey(), is(CRITERIA_KEY));
        assertThat(networkPlugin.getExpectedValue(), is(EXPECTED_VALUE));
        assertThat(networkPlugin.getNewAttributes(), is(newAttributes));
    }

    @Test
    public void testPassthroughMethods() throws Exception {
        ThreadContext.put(CLIENT_INFO_KEY, INFO_MAP);
        when(mockMetacardCondition.applies(INFO_MAP)).thenReturn(true);

        UpdateRequest updateRequest = mock(UpdateRequest.class);
        DeleteRequest deleteRequest = mock(DeleteRequest.class);
        QueryRequest queryRequest = mock(QueryRequest.class);
        ResourceRequest resourceRequest = mock(ResourceRequest.class);

        DeleteResponse deleteResponse = mock(DeleteResponse.class);
        QueryResponse queryResponse = mock(QueryResponse.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        assertThat(plugin.processPreUpdate(updateRequest, mock(Map.class)), is(updateRequest));
        assertThat(plugin.processPreDelete(deleteRequest), is(deleteRequest));
        assertThat(plugin.processPreQuery(queryRequest), is(queryRequest));
        assertThat(plugin.processPreResource(resourceRequest), is(resourceRequest));

        assertThat(plugin.processPostDelete(deleteResponse), is(deleteResponse));
        assertThat(plugin.processPostQuery(queryResponse), is(queryResponse));
        assertThat(plugin.processPostResource(resourceResponse, mock(Metacard.class)),
                is(resourceResponse));

        verifyZeroInteractions(mockMetacardCondition,
                mockMetacardServices,
                updateRequest,
                deleteRequest,
                queryRequest,
                resourceRequest,
                deleteResponse,
                queryResponse,
                resourceResponse);
    }
}
