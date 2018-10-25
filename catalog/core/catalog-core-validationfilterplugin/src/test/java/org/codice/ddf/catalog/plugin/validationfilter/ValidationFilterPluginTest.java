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
package org.codice.ddf.catalog.plugin.validationfilter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Validation;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class ValidationFilterPluginTest {
  private ValidationFilterPlugin plugin;

  private FilterAdapter filterAdapter;

  private FilterBuilder filterBuilder;

  private Source source;

  private Subject subject;

  private Map<String, Serializable> properties = new HashMap<>();

  private ValidationQueryDelegate testValidationWarningQueryDelegate;

  private ValidationQueryDelegate testValidationErrorQueryDelegate;

  private boolean canViewInvalidData = true;

  @Before
  public void setup() {

    source = mock(CatalogProvider.class);
    when(source.getId()).thenReturn("cat1");

    subject = mock(Subject.class);
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);

    filterAdapter = new GeotoolsFilterAdapterImpl();
    filterBuilder = new GeotoolsFilterBuilder();

    testValidationWarningQueryDelegate =
        new ValidationQueryDelegate(Validation.VALIDATION_WARNINGS);
    testValidationErrorQueryDelegate = new ValidationQueryDelegate(Validation.VALIDATION_ERRORS);

    plugin = new ValidationFilterPlugin(filterBuilder);

    List<String> attributeMapping = new ArrayList<>();
    attributeMapping.add("invalid-state=data-manager,system-admin");
    plugin.setAttributeMap(attributeMapping);
  }

  @Test
  public void testEmptyAttributeMapping() {
    plugin.setAttributeMap(new ArrayList<>());
    assertThat(plugin.getAttributeMap().isEmpty(), is(true));
  }

  @Test
  public void testAttributeMapping() {
    Map<String, List<String>> attributeMap = plugin.getAttributeMap();

    assertThat(attributeMap.containsKey("invalid-state"), is(true));
    assertThat(attributeMap.get("invalid-state").size(), is(2));
    assertThat(attributeMap.get("invalid-state").contains("data-manager"), is(true));
    assertThat(attributeMap.get("invalid-state").contains("system-admin"), is(true));
  }

  @Test
  public void testAttributeMap() {
    Map<String, List<String>> attributeMapping = new HashMap<>();
    List<String> attributes = new ArrayList<>();
    attributes.add("data-manager");
    attributes.add("system-admin");
    attributeMapping.put("invalid-state", attributes);

    plugin.setAttributeMap(attributeMapping);
    Map<String, List<String>> attributeMap = plugin.getAttributeMap();

    assertThat(attributeMap.containsKey("invalid-state"), is(true));
    assertThat(attributeMap.get("invalid-state").size(), is(2));
    assertThat(attributeMap.get("invalid-state").contains("data-manager"), is(true));
    assertThat(attributeMap.get("invalid-state").contains("system-admin"), is(true));
  }

  @Test
  public void testEmptyAttributeMap() throws StopProcessingException, UnsupportedQueryException {
    Map<String, List<String>> attributeMapping = new HashMap<>();

    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);

    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setAttributeMap(attributeMapping);
    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
  }

  @Test
  public void testHideErrorWarningWithPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);

    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(false);
    plugin.setShowWarnings(false);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
    assertThat(plugin.isShowErrors(), is(false));
    assertThat(plugin.isShowWarnings(), is(false));
  }

  @Test
  public void testHideErrorWarningWithNoPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(!canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(false);
    plugin.setShowWarnings(false);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
  }

  @Test
  public void testShowErrorWarningWithNoPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(!canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
  }

  @Test
  public void testShowErrorWarningWithPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(false));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(false));
  }

  @Test
  public void testShowErrorHideWarningWithPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(false);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(false));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
  }

  @Test
  public void testHideErrorShowWarningWithPermission()
      throws StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(false);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(
        filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(false));
  }

  @Test(expected = StopProcessingException.class)
  public void testInvalidSubject() throws StopProcessingException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);

    properties.clear();
    properties.put(SecurityConstants.SECURITY_SUBJECT, new ArrayList());

    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.process(source, queryRequest);
  }
}
