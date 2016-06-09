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
 **/
package org.codice.ddf.ui.searchui.query.model;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.operation.impl.QueryResponseImpl;

@RunWith(MockitoJUnitRunner.class)
public class SearchTest {
    private static final String REASON_INTERNAL = "Internal error";

    private static final String[] SOURCE_ID_VALUES =
            {"source_alpha", "source_bravo", "source_charlie", "source_delta"};

    private Search searchProvider;

    @Mock
    private SearchRequest mockSearchRequest;

    @Mock
    private QueryResponseImpl mockQueryResponse;

    @Before
    public void setup() throws Exception {
        Set<String> sourceIds = new HashSet<>(Arrays.asList(SOURCE_ID_VALUES));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("elapsed", 1000L);

        when(mockSearchRequest.getSourceIds()).thenReturn(sourceIds);
        when(mockQueryResponse.getResults()).thenReturn(Collections.emptyList());
        when(mockQueryResponse.getHits()).thenReturn(new Long(2));
        when(mockQueryResponse.getProcessingDetails()).thenReturn(Collections.emptySet());
        when(mockQueryResponse.getProperties()).thenReturn(properties);

        searchProvider = new Search(mockSearchRequest, new ActionRegistry() {
            @Override
            public <T> List<Action> list(T subject) {
                return null;
            }
        });
    }

    @Test
    public void testFailSource() throws Exception {
        for (String id : SOURCE_ID_VALUES) {
            searchProvider.failSource(id, new Exception());
        }

        Map<String, QueryStatus> statusMap = searchProvider.getQueryStatus();
        assertThat(statusMap, notNullValue());

        for (String id : SOURCE_ID_VALUES) {
            QueryStatus queryStatus = statusMap.get(id);
            assertThat(queryStatus.getReasons(), hasItem(REASON_INTERNAL));
        }
    }

    @Test
    public void testSuccessfulSource() throws Exception {
        for (String id : SOURCE_ID_VALUES) {
            searchProvider.update(id, mockQueryResponse);
        }

        Map<String, QueryStatus> statusMap = searchProvider.getQueryStatus();

        for (String id : SOURCE_ID_VALUES) {
            QueryStatus queryStatus = statusMap.get(id);
            assertTrue(queryStatus.getReasons()
                    .isEmpty());
        }
    }
}
