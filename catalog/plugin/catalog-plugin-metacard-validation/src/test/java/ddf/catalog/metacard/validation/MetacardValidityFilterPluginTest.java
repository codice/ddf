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
package ddf.catalog.metacard.validation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Validation;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class MetacardValidityFilterPluginTest {

  private static final String LOCAL_SOURCE_ID = "localSourceId";

  private static final List<String> ATTRIBUTE_MAPPING =
      Collections.singletonList("sample=test1,test2");

  private static final CatalogProvider LOCAL_PROVIDER = mockCatalogProvider(LOCAL_SOURCE_ID);

  private ValidationQueryDelegate errorsValidationQueryDelegate;

  private ValidationQueryDelegate warningsValidationQueryDelegate;

  private FilterAdapter filterAdapter;

  private FilterBuilder filterBuilder;

  private MetacardValidityFilterPlugin metacardValidityFilterPlugin;

  @Before
  public void setUp() {
    filterAdapter = new GeotoolsFilterAdapterImpl();
    filterBuilder = new GeotoolsFilterBuilder();

    errorsValidationQueryDelegate = new ValidationQueryDelegate(Validation.VALIDATION_ERRORS);
    warningsValidationQueryDelegate = new ValidationQueryDelegate(Validation.VALIDATION_WARNINGS);

    metacardValidityFilterPlugin =
        new MetacardValidityFilterPlugin(filterBuilder, Collections.singletonList(LOCAL_PROVIDER));
    metacardValidityFilterPlugin.setAttributeMap(ATTRIBUTE_MAPPING);
  }

  @Test
  public void testSetAttributeMapping() {
    Map<String, List<String>> assertMap = metacardValidityFilterPlugin.getAttributeMap();
    assertThat(assertMap.size(), is(1));
    assertThat(assertMap.containsKey("sample"), is(true));
    assertThat(assertMap.get("sample").contains("test1"), is(true));
    assertThat(assertMap.get("sample").contains("test2"), is(true));
  }

  @Test
  public void testResetAttributeMappingEmptyList() {
    metacardValidityFilterPlugin.setAttributeMap(Collections.emptyList());
    assertThat(metacardValidityFilterPlugin.getAttributeMap(), is(Collections.emptyMap()));
  }

  @Test
  public void testResetAttributeMappingEmptyString() {
    metacardValidityFilterPlugin.setAttributeMap(Collections.singletonList(""));
    assertThat(metacardValidityFilterPlugin.getAttributeMap(), is(Collections.emptyMap()));
  }

  @Test
  public void testValidMetacards() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getValidMetacard(), true, false);
    assertThat(response.itemPolicy().size(), is(0));
  }

  @Test
  public void testInvalidMetacards() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), true, false);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testNullMetacard() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, null, true, false);
    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testNullResults() throws Exception {
    PolicyResponse response = filterPluginResponseHelper(null, getValidMetacard(), true, false);

    assertThat(response.itemPolicy().isEmpty(), is(true));
  }

  @Test
  public void testFilterErrorsOnly() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), true, false);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterWarningsOnly() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response =
        filterPluginResponseHelper(result, getWarningsMetacard(), false, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterErrorsAndWarnings() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getWarningsMetacard(), true, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));

    response = filterPluginResponseHelper(result, getErrorsMetacard(), true, true);
    assertThat(response.itemPolicy().get("sample").contains("test1"), is(true));
  }

  @Test
  public void testFilterNone() throws Exception {
    Result result = mock(Result.class);

    PolicyResponse response = filterPluginResponseHelper(result, getErrorsMetacard(), false, false);
    assertThat(response.itemPolicy().size(), is(0));

    response = filterPluginResponseHelper(result, getWarningsMetacard(), false, false);
    assertThat(response.itemPolicy().size(), is(0));
  }

  @Test
  public void testNonLocalSource() throws Exception {
    QueryRequest queryRequest = mockQueryRequest(Collections.emptyMap());
    QueryRequest returnRequest =
        metacardValidityFilterPlugin.process(mock(Source.class), queryRequest);
    assertThat(queryRequest.equals(returnRequest), is(true));
  }

  @Test
  public void testCannotViewInvalidWithErrorsAndWarningsFiltered() throws Exception {
    metacardValidityFilterPlugin.setFilterErrors(true);
    metacardValidityFilterPlugin.setFilterWarnings(true);

    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(false));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(true));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(true));
  }

  @Test
  public void testCannotViewInvalidWithErrorsFiltered() throws Exception {
    metacardValidityFilterPlugin.setFilterErrors(true);
    metacardValidityFilterPlugin.setFilterWarnings(false);

    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(false));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(false));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(true));
  }

  @Test
  public void testCannotViewInvalidWithWarningsFiltered() throws Exception {
    metacardValidityFilterPlugin.setFilterErrors(false);
    metacardValidityFilterPlugin.setFilterWarnings(true);

    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(false));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(true));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(false));
  }

  @Test
  public void testCannotViewInvalidWithNoFiltered() throws Exception {
    metacardValidityFilterPlugin.setFilterErrors(false);
    metacardValidityFilterPlugin.setFilterWarnings(false);

    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(false));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(false));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(false));
  }

  @Test
  public void testCanViewInvalidData() throws Exception {
    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(true));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(false));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(false));
  }

  @Test
  public void testEmptySecurityMapAppliesFilters() throws Exception {
    metacardValidityFilterPlugin.setAttributeMap(Collections.emptyMap());

    QueryRequest queryRequest = mockQueryRequest(createSubjectRequestProperties(false));

    QueryRequest returnQueryRequest =
        metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), warningsValidationQueryDelegate),
        is(false));
    assertThat(
        filterAdapter.adapt(returnQueryRequest.getQuery(), errorsValidationQueryDelegate),
        is(true));
  }

  @Test(expected = StopProcessingException.class)
  public void noSubjectAvailable() throws Exception {
    metacardValidityFilterPlugin.setAttributeMap(ATTRIBUTE_MAPPING);

    QueryRequest queryRequest = mockQueryRequest(Collections.emptyMap());

    metacardValidityFilterPlugin.process(LOCAL_PROVIDER, queryRequest);
  }

  private Metacard getValidMetacard() {
    return mock(Metacard.class);
  }

  private Metacard getErrorsMetacard() {
    Attribute errorAttr = mock(Attribute.class);
    when(errorAttr.getName()).thenReturn(Validation.VALIDATION_ERRORS);
    when(errorAttr.getValues()).thenReturn(Collections.singletonList("sample-validator"));

    Metacard returnMetacard = mock(Metacard.class);
    when(returnMetacard.getAttribute(Validation.VALIDATION_ERRORS)).thenReturn(errorAttr);
    return returnMetacard;
  }

  private Metacard getWarningsMetacard() {
    Attribute errorAttr = mock(Attribute.class);
    when(errorAttr.getName()).thenReturn(Validation.VALIDATION_WARNINGS);
    when(errorAttr.getValues()).thenReturn(Collections.singletonList("sample-validator"));

    Metacard returnMetacard = mock(Metacard.class);
    when(returnMetacard.getAttribute(Validation.VALIDATION_WARNINGS)).thenReturn(errorAttr);
    return returnMetacard;
  }

  private QueryRequest mockQueryRequest(Map<String, Serializable> requestProperties) {
    QueryRequest queryRequest = mock(QueryRequest.class);

    Query query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));
    when(queryRequest.getQuery()).thenReturn(query);
    when(queryRequest.getProperties()).thenReturn(requestProperties);
    return queryRequest;
  }

  private static CatalogProvider mockCatalogProvider(String id) {
    CatalogProvider localSource = mock(CatalogProvider.class);
    when(localSource.getId()).thenReturn(id);
    return localSource;
  }

  private Map<String, Serializable> createSubjectRequestProperties(
      boolean subjectCanViewInvalidData) {
    Subject subject = mock(Subject.class);
    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(subjectCanViewInvalidData);

    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    return properties;
  }

  private PolicyResponse filterPluginResponseHelper(
      Result result, Metacard metacard, boolean filterErrors, boolean filterWarnings)
      throws Exception {
    metacardValidityFilterPlugin.setAttributeMap(ATTRIBUTE_MAPPING);
    metacardValidityFilterPlugin.setFilterErrors(filterErrors);
    metacardValidityFilterPlugin.setFilterWarnings(filterWarnings);

    if (result != null) {
      when(result.getMetacard()).thenReturn(metacard);
    }

    return metacardValidityFilterPlugin.processPostQuery(result, new HashMap<>());
  }
}
