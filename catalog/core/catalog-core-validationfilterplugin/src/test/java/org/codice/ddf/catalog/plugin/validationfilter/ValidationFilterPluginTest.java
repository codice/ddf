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
import static org.junit.Assert.assertEquals;
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
import ddf.catalog.plugin.PluginExecutionException;
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
import org.geotools.filter.AndImpl;
import org.geotools.filter.IsNullImpl;
import org.geotools.filter.LikeFilterImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

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

    CatalogProvider catProvider1 = mock(CatalogProvider.class);
    when(catProvider1.getId()).thenReturn("cat1");

    source = mock(CatalogProvider.class);
    when(source.getId()).thenReturn("cat1");

    subject = mock(Subject.class);
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);

    filterAdapter = new GeotoolsFilterAdapterImpl();
    filterBuilder = new GeotoolsFilterBuilder();

    testValidationWarningQueryDelegate = new ValidationQueryDelegate(Validation.VALIDATION_WARNINGS);
    testValidationErrorQueryDelegate = new ValidationQueryDelegate(Validation.VALIDATION_ERRORS);

    plugin = new ValidationFilterPlugin(filterAdapter, filterBuilder);
  }

  @Test
  public void testEmptyAttributeMapping() {
    plugin.setAttributeMap(new ArrayList<>());
    assertEquals(plugin.getAttributeMap().isEmpty(), true);
  }

  @Test
  public void testAttributeMapping() {
    List<String> attributeMapping = new ArrayList<>();
    attributeMapping.add("invalid-state=datamanager,admin");
    plugin.setAttributeMap(attributeMapping);
    Map<String, List<String>> attributeMap = plugin.getAttributeMap();

    assertEquals(true, attributeMap.containsKey("invalid-state"));
    assertEquals(2, attributeMap.get("invalid-state").size());
    assertEquals(true, attributeMap.get("invalid-state").contains("datamanager"));
    assertEquals(true, attributeMap.get("invalid-state").contains("admin"));
  }

  @Test
  public void testAttributeMap() {
    Map<String, List<String>> attributeMapping = new HashMap<>();
    List<String> attributes = new ArrayList<>();
    attributes.add("datamanager");
    attributes.add("admin");
    attributeMapping.put("invalid-state", attributes);

    plugin.setAttributeMap(attributeMapping);
    Map<String, List<String>> attributeMap = plugin.getAttributeMap();

    assertEquals(true, attributeMap.containsKey("invalid-state"));
    assertEquals(2, attributeMap.get("invalid-state").size());
    assertEquals(true, attributeMap.get("invalid-state").contains("datamanager"));
    assertEquals(true, attributeMap.get("invalid-state").contains("admin"));
  }

  @Test
  public void testNotShowErrorWithPermission()
      throws PluginExecutionException, StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);

    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(false);
    plugin.setShowWarnings(false);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationWarningQueryDelegate), is(true));
  }

  @Test
  public void testNotShowErrorWithNoPermission()
      throws PluginExecutionException, StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(!canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(false);
    plugin.setShowWarnings(false);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
  }

  @Test
  public void testShowErrorWithNoPermission()
      throws PluginExecutionException, StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(!canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
  }

  @Test
  public void testShowErrorWithPermission()
      throws PluginExecutionException, StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(filterBuilder.attribute(Metacard.ANY_TEXT).is().equalTo().text("sample"));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);
    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(false));
  }

  @Test
  public void testNotShowErrorExisted()
      throws PluginExecutionException, StopProcessingException, UnsupportedQueryException {
    QueryImpl query =
        new QueryImpl(
            filterBuilder.allOf(
                filterBuilder.attribute(Validation.VALIDATION_WARNINGS).is().empty(),
                filterBuilder.attribute(Validation.VALIDATION_ERRORS).is().empty()));

    when(subject.isPermitted(any(KeyValueCollectionPermission.class)))
        .thenReturn(canViewInvalidData);

    QueryRequest queryRequest = new QueryRequestImpl(query, false, null, properties);

    plugin.setShowErrors(true);
    plugin.setShowWarnings(true);

    QueryRequest returnQuery = plugin.process(source, queryRequest);

    assertThat(filterAdapter.adapt(returnQuery.getQuery(), testValidationErrorQueryDelegate), is(true));
    assertEquals(true, plugin.isShowErrors());
    assertEquals(true, plugin.isShowWarnings());
  }

  @Test(expected = StopProcessingException.class)
  public void testInvalidSubject() throws PluginExecutionException, StopProcessingException {
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
