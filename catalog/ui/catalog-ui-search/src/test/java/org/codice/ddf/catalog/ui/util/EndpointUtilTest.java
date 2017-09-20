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
package org.codice.ddf.catalog.ui.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.EqualityExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;

public class EndpointUtilTest {

  EndpointUtil endpointUtil;

  FilterBuilder filterBuilderMock;

  QueryResponse responseMock;

  Metacard metacardMock;

  Result resultMock;

  @Before
  public void setUp() throws Exception {

    List<MetacardType> metacardTypeList = new ArrayList<>();

    List<InjectableAttribute> injectableAttributeList = new ArrayList<>();

    // mocks
    MetacardType metacardTypeMock = mock(MetacardType.class);

    CatalogFramework catalogFrameworkMock = mock(CatalogFramework.class);

    InjectableAttribute injectableAttributeMock = mock(InjectableAttribute.class);

    AttributeRegistry attributeRegistryMock = mock(AttributeRegistry.class);

    Filter filterMock = mock(Filter.class);

    AttributeBuilder attributeBuilderMock = mock(AttributeBuilder.class);

    ConfigurationApplication configurationApplicationMock = mock(ConfigurationApplication.class);

    ContextualExpressionBuilder contextualExpressionBuilderMock =
        mock(ContextualExpressionBuilder.class);

    filterBuilderMock = mock(FilterBuilder.class);
    responseMock = mock(QueryResponse.class);
    metacardMock = mock(Metacard.class);
    resultMock = mock(Result.class);

    metacardTypeList.add(metacardTypeMock);
    injectableAttributeList.add(injectableAttributeMock);

    // when
    when(filterBuilderMock.attribute(any())).thenReturn(attributeBuilderMock);
    when(attributeBuilderMock.is()).thenReturn(attributeBuilderMock);
    when(attributeBuilderMock.like()).thenReturn(contextualExpressionBuilderMock);
    when(contextualExpressionBuilderMock.text(anyString())).thenReturn(filterMock);
    when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);
    when(configurationApplicationMock.getMaximumUploadSize()).thenReturn(1 << 20);

    when(resultMock.getMetacard()).thenReturn(metacardMock);
    when(metacardMock.getId()).thenReturn("MOCK METACARD");

    // constructor
    endpointUtil =
        new EndpointUtil(
            metacardTypeList,
            catalogFrameworkMock,
            filterBuilderMock,
            injectableAttributeList,
            attributeRegistryMock,
            configurationApplicationMock);
  }

  @Test
  public void testGetMetacardsByFilterExpectAll() throws Exception {

    String tagFilter = "";
    int expected = 100;

    List<Result> resultList = populateResultMockList(expected);
    List<Result> emptyList = populateResultMockList(0);

    // Internal implementation detail: ResultIterable.fetchNextResults may call resultList
    // a varying number times. emptyList is returned to express that all resources have been
    // exhausted
    when(responseMock.getResults()).thenReturn(resultList, resultList, emptyList);

    Map<String, Result> result = endpointUtil.getMetacardsByFilter(tagFilter);

    assertThat(result.keySet(), hasSize(expected));
  }

  @Test
  public void testGetMetacardsByIdListExpectAll() throws Exception {

    String attributeName = "attr";
    int expected = 100;

    Filter tagFilter = mock(Filter.class);
    List<Result> resultList = populateResultMockList(expected, attributeName);
    List<Result> emptyList = populateResultMockList(0);

    Collection<String> ids =
        resultList.stream().map(result -> result.getMetacard().getId()).collect(Collectors.toSet());

    when(responseMock.getResults()).thenReturn(resultList, resultList, emptyList);
    when(filterBuilderMock.attribute(attributeName).is().equalTo())
        .thenReturn(mock(EqualityExpressionBuilder.class));
    when(filterBuilderMock.anyOf(anyList())).thenReturn(mock(Or.class));

    Map<String, Result> result = endpointUtil.getMetacards(attributeName, ids, tagFilter);

    assertThat(result.keySet(), hasSize(expected));
  }

  private List<Result> populateResultMockList(int size) {
    return populateResultMockList(size, null);
  }

  // this method will return Metacards with an attribute if the attribute parameter is specified
  private List<Result> populateResultMockList(int size, String attribute) {

    List<Result> resultMockList = new ArrayList<>();

    for (int i = 0; i < size; i++) {
      resultMock = mock(Result.class);
      metacardMock = mock(Metacard.class);

      when(resultMock.getMetacard()).thenReturn(metacardMock);
      when(metacardMock.getId()).thenReturn("MOCK METACARD " + (i + 1));

      if (attribute != null) {
        when(metacardMock.getAttribute(attribute))
            .thenReturn(new AttributeImpl(attribute, new ArrayList<String>()));
      }
      resultMockList.add(resultMock);
    }

    return resultMockList;
  }
}
