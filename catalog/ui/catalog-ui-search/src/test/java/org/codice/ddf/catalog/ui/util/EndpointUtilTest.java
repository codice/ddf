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

import static ddf.catalog.data.AttributeType.AttributeFormat.STRING;
import static ddf.catalog.data.types.Security.ACCESS_ADMINISTRATORS;
import static ddf.catalog.data.types.Security.ACCESS_GROUPS;
import static ddf.catalog.data.types.Security.ACCESS_GROUPS_READ;
import static ddf.catalog.data.types.Security.ACCESS_INDIVIDUALS;
import static ddf.catalog.data.types.Security.ACCESS_INDIVIDUALS_READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.EqualityExpressionBuilder;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.query.cql.CqlResult;
import org.codice.ddf.catalog.ui.transformer.TransformerDescriptors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;

public class EndpointUtilTest {

  private EndpointUtil endpointUtil;

  private FilterBuilder filterBuilderMock;

  private FilterAdapter filterAdapterMock;

  private ActionRegistry actionRegistryMock;

  private QueryResponse responseMock;

  private Metacard metacardMock;

  private Result resultMock;

  private AttributeType attributeTypeMock;

  private MetacardType metacardTypeMock;

  private AttributeRegistry attributeRegistryMock;

  CatalogFramework catalogFrameworkMock;

  // represents 2018-09-05T14:03:17.000Z
  private static final long DATE_EPOCH = 1536156197000L;

  @Before
  public void setUp() throws Exception {

    List<MetacardType> metacardTypeList = new ArrayList<>();

    List<InjectableAttribute> injectableAttributeList = new ArrayList<>();

    // mocks
    metacardTypeMock = mock(MetacardType.class);

    catalogFrameworkMock = mock(CatalogFramework.class);

    InjectableAttribute injectableAttributeMock = mock(InjectableAttribute.class);

    attributeRegistryMock = mock(AttributeRegistry.class);

    Filter filterMock = mock(Filter.class);

    AttributeBuilder attributeBuilderMock = mock(AttributeBuilder.class);

    ConfigurationApplication configurationApplicationMock = mock(ConfigurationApplication.class);

    ContextualExpressionBuilder contextualExpressionBuilderMock =
        mock(ContextualExpressionBuilder.class);

    filterBuilderMock = mock(FilterBuilder.class);
    filterAdapterMock = mock(FilterAdapter.class);
    actionRegistryMock = mock(ActionRegistry.class);
    responseMock = mock(QueryResponse.class);
    metacardMock = mock(Metacard.class);
    resultMock = mock(Result.class);
    attributeTypeMock = mock(AttributeType.class);

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
            filterAdapterMock,
            actionRegistryMock,
            injectableAttributeList,
            attributeRegistryMock,
            configurationApplicationMock);

    endpointUtil.setDescriptors(
        new TransformerDescriptors(Collections.emptyList(), Collections.emptyList()));
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

    Map<String, Result> result = endpointUtil.getMetacardsByTag(tagFilter);

    assertThat(result.keySet(), hasSize(expected));
  }

  @Test
  public void testGetMetacardTypeMapFiltered() {
    AttributeDescriptor mockAttrDescriptor = mock(AttributeDescriptor.class);
    when(mockAttrDescriptor.getName()).thenReturn("first");
    when(mockAttrDescriptor.getType()).thenReturn(attributeTypeMock);
    when(mockAttrDescriptor.isMultiValued()).thenReturn(false);

    when(metacardTypeMock.getName()).thenReturn("mockType");
    when(attributeTypeMock.getAttributeFormat()).thenReturn(STRING);
    when(attributeRegistryMock.lookup(any())).thenReturn(Optional.ofNullable(mockAttrDescriptor));

    endpointUtil.setWhiteListedMetacardTypes(ImmutableList.of("otherType"));
    Map<String, Object> metacardTypes = endpointUtil.getMetacardTypeMap();

    assertThat(metacardTypes, anEmptyMap());
    assertThat(metacardTypes.containsKey("mockType"), is(false));
  }

  @Test
  public void testGetMetacardTypeMapNonFiltered() {
    AttributeDescriptor mockAttrDescriptor = mock(AttributeDescriptor.class);
    when(mockAttrDescriptor.getName()).thenReturn("first");
    when(mockAttrDescriptor.getType()).thenReturn(attributeTypeMock);
    when(mockAttrDescriptor.isMultiValued()).thenReturn(false);

    when(metacardTypeMock.getName()).thenReturn("mockType");
    when(attributeTypeMock.getAttributeFormat()).thenReturn(STRING);
    when(attributeRegistryMock.lookup(any())).thenReturn(Optional.ofNullable(mockAttrDescriptor));

    Map<String, Object> metacardTypes = endpointUtil.getMetacardTypeMap();
    assertThat(metacardTypes, hasKey("mockType"));
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
    when(filterBuilderMock.allOf(any(Filter.class), any(Filter.class))).thenReturn(mock(And.class));

    Map<String, Result> result =
        endpointUtil.getMetacardsWithTagByAttributes(attributeName, ids, tagFilter);

    assertThat(result.keySet(), hasSize(expected));
  }

  @Test
  public void testGetMetacardsByTagWithLikeAttributes() throws Exception {

    String ownerEmail = "doNoReply@xyz.com";
    ArgumentCaptor<List> capturedFilterList = ArgumentCaptor.forClass(List.class);

    Map<String, List<String>> attributeMapMock = new HashMap<>();
    attributeMapMock.put(Core.METACARD_OWNER, Collections.singletonList(ownerEmail));
    attributeMapMock.put(ACCESS_ADMINISTRATORS, Collections.singletonList(ownerEmail));
    attributeMapMock.put(ACCESS_GROUPS_READ, Collections.singletonList("guest"));
    attributeMapMock.put(ACCESS_GROUPS, Collections.singletonList("admin"));
    attributeMapMock.put(ACCESS_INDIVIDUALS, Collections.singletonList(ownerEmail));
    attributeMapMock.put(ACCESS_INDIVIDUALS_READ, Collections.singletonList(ownerEmail));

    Filter tagFilter = mock(Filter.class);
    List<Result> emptyList = populateResultMockList(0);

    when(responseMock.getResults()).thenReturn(emptyList);
    for (String attributeName : attributeMapMock.keySet()) {
      when(filterBuilderMock.attribute(attributeName).is().equalTo())
          .thenReturn(mock(EqualityExpressionBuilder.class));
    }
    when(filterBuilderMock.anyOf(anyList())).thenReturn(mock(Or.class));
    when(filterBuilderMock.allOf(any(Filter.class), any(Filter.class))).thenReturn(mock(And.class));

    Map<String, Collection<String>> attributeMap = new HashMap<>();

    // Only match on owner email
    attributeMap.put(Core.METACARD_OWNER, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(1));

    // Add individual access email match
    attributeMap.put(ACCESS_INDIVIDUALS, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(2)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(2));

    // Add individual read only email match
    attributeMap.put(ACCESS_INDIVIDUALS_READ, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(3)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(3));

    // Admistrator match
    attributeMap.clear();
    attributeMap.put(ACCESS_ADMINISTRATORS, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(4)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(1));

    // Group read only match
    attributeMap.clear();
    attributeMap.put(ACCESS_GROUPS_READ, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(5)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(1));

    // Group read and write match
    attributeMap.put(ACCESS_GROUPS, Collections.singletonList(ownerEmail));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(6)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(2));

    // Multiple Group match
    attributeMap.clear();
    attributeMap.put(ACCESS_GROUPS_READ, new ArrayList<>(Arrays.asList("guest", "admin")));
    attributeMap.put(ACCESS_GROUPS, new ArrayList<>(Arrays.asList("guest", "admin")));
    endpointUtil.getMetacardsWithTagByLikeAttributes(attributeMap, tagFilter);
    verify(filterBuilderMock, times(9)).anyOf(capturedFilterList.capture());
    assertThat((List<Filter>) capturedFilterList.getValue(), hasSize(2));
  }

  private List<Result> populateResultMockList(int size) {
    return populateResultMockList(size, (String) null);
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
            .thenReturn(AttributeImpl.fromMultipleValues(attribute, new ArrayList<>()));
      }
      resultMockList.add(resultMock);
    }

    return resultMockList;
  }

  @Test
  public void testParseDateEmptyString() {
    Serializable serializable = endpointUtil.parseDate("");
    assertThat(serializable, nullValue());
  }

  @Test
  public void testHitCountOnlyQuery() throws Exception {
    long hitCount = 12L;
    when(responseMock.getResults()).thenReturn(Collections.emptyList());
    when(responseMock.getHits()).thenReturn(hitCount);
    when(catalogFrameworkMock.query(any(QueryRequestImpl.class))).thenReturn(responseMock);

    CqlQueryResponse cqlQueryResponse = endpointUtil.executeCqlQuery(generateCqlRequest(0));
    List<CqlResult> results = cqlQueryResponse.getResults();
    assertThat(results, hasSize(0));
    assertThat(cqlQueryResponse.getQueryResponse().getHits(), is(hitCount));
  }

  @Test
  public void testCopyAttributes() {

    AttributeDescriptor firstAttributeDescriptor = mock(AttributeDescriptor.class);
    when(firstAttributeDescriptor.getName()).thenReturn("first");

    AttributeDescriptor secondAttributeDescriptor = mock(AttributeDescriptor.class);
    when(secondAttributeDescriptor.getName()).thenReturn("second");

    MetacardType metacardType = mock(MetacardType.class);
    when(metacardType.getAttributeDescriptors())
        .thenReturn(
            new HashSet<>(Arrays.asList(firstAttributeDescriptor, secondAttributeDescriptor)));

    String firstValue = "a";
    String secondValue = "b";

    Metacard sourceMetacard = new MetacardImpl();
    sourceMetacard.setAttribute(new AttributeImpl(firstAttributeDescriptor.getName(), firstValue));
    sourceMetacard.setAttribute(
        new AttributeImpl(secondAttributeDescriptor.getName(), secondValue));

    Metacard destinationMetacard = new MetacardImpl();

    endpointUtil.copyAttributes(sourceMetacard, metacardType, destinationMetacard);

    assertThat(
        Collections.singletonList(firstValue),
        is(destinationMetacard.getAttribute(firstAttributeDescriptor.getName()).getValues()));
    assertThat(
        Collections.singletonList(secondValue),
        is(destinationMetacard.getAttribute(secondAttributeDescriptor.getName()).getValues()));
  }

  private CqlRequest generateCqlRequest(int count) {
    CqlRequest cqlRequest = new CqlRequest();
    cqlRequest.setCount(count);
    cqlRequest.setCql("anyText ILIKE '*'");

    return cqlRequest;
  }

  @Test
  public void testParseDateEpochIsoZ() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate("2018-09-05T14:03:17.000Z");
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateEpochIso0000() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate("2018-09-05T14:03:17.000+00:00");
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateEpochIsoNoMilli() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate("2018-09-05T14:03:17Z");
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateEpochIsoDifferentTimeZone() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate("2018-09-05T10:03:17.000-04:00");
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateEpochLong() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate(DATE_EPOCH);
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateEpochString() {
    Instant date = new Date(DATE_EPOCH).toInstant();

    Instant dateConverted = endpointUtil.parseDate(Long.toString(DATE_EPOCH));
    assertThat(dateConverted, is(date));
  }

  @Test
  public void testParseDateWhiteSpaceString() {
    Instant dateConverted = endpointUtil.parseDate("  ");
    assertThat(dateConverted, nullValue());
  }

  @Test
  public void testGetJsonUsesIsoDateTimeFormat() {
    Date date = new Date(DATE_EPOCH);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    String dateIso = sdf.format(date);
    Map<String, Object> map = new HashMap<>();
    map.put("date", date);
    String json = endpointUtil.getJson(map);
    assertThat(json, is("{\"date\":\"" + dateIso + "\"}"));
  }
}
