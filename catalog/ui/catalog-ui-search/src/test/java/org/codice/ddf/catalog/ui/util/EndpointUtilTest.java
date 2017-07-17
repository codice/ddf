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
package org.codice.ddf.catalog.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

public class EndpointUtilTest {

    EndpointUtil endpointUtil;

    List<MetacardType> metacardTypeList;

    MetacardType metacardTypeMock;

    CatalogFramework catalogFrameworkMock;

    FilterBuilder filterBuilderMock;

    List<InjectableAttribute> injectableAttributeList;

    InjectableAttribute injectableAttributeMock;

    AttributeRegistry attributeRegistryMock;

    Filter filterMock;

    AttributeBuilder attributeBuilderMock;

    ContextualExpressionBuilder contextualExpressionBuilderMock;

    QueryResponse responseMock;

    Metacard metacardMock;

    Result resultMock;

    @Before
    public void setUp() throws Exception {

        // mocks
        metacardTypeList = new ArrayList<>();
        metacardTypeMock = mock(MetacardType.class);
        metacardTypeList.add(metacardTypeMock);

        catalogFrameworkMock = mock(CatalogFramework.class);

        filterBuilderMock = mock(FilterBuilder.class);

        injectableAttributeList = new ArrayList<>();
        injectableAttributeMock = mock(InjectableAttribute.class);
        injectableAttributeList.add(injectableAttributeMock);

        attributeRegistryMock = mock(AttributeRegistry.class);
        filterMock = mock(Filter.class);
        attributeBuilderMock = mock(AttributeBuilder.class);
        contextualExpressionBuilderMock = mock(ContextualExpressionBuilder.class);

        responseMock = mock(QueryResponse.class);
        metacardMock = mock(Metacard.class);
        resultMock = mock(Result.class);

        // when
        when(filterBuilderMock.attribute(any())).thenReturn(attributeBuilderMock);
        when(attributeBuilderMock.is()).thenReturn(attributeBuilderMock);
        when(attributeBuilderMock.like()).thenReturn(contextualExpressionBuilderMock);
        when(contextualExpressionBuilderMock.text(anyString())).thenReturn(filterMock);
        when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);

        when(resultMock.getMetacard()).thenReturn(metacardMock);
        when(metacardMock.getId()).thenReturn("MOCK METACARD");

        // constructor
        endpointUtil = new EndpointUtil(metacardTypeList,
                catalogFrameworkMock,
                filterBuilderMock,
                injectableAttributeList,
                attributeRegistryMock);

    }

    @Test
    public void testGetMetacardsByFilterExpectAll()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {

        int numResults = 100;

        // TODO: 7/17/17 Look into why tagFilter is irrelevant (might need for other test)
        String tagFilter = "";

        List<Result> resultList = populateResultMockList(numResults);
        List<Result> emptyList = populateResultMockList(0);

        when(responseMock.getResults()).thenReturn(resultList, resultList, emptyList);

        Map<String, Result> expected = endpointUtil.getMetacardsByFilter(tagFilter);

        assertThat("Should return " + numResults + " results, but returned " + expected.size(),
                expected.size() == numResults);

        verify(responseMock, times(4)).getResults();
    }

    private List<Result> populateResultMockList(int size) {

        List<Result> resultMockList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            resultMock = mock(Result.class);
            metacardMock = mock(Metacard.class);

            when(resultMock.getMetacard()).thenReturn(metacardMock);
            when(metacardMock.getId()).thenReturn("MOCK METACARD " + (i + 1));

            resultMockList.add(resultMock);
        }

        return resultMockList;
    }
}