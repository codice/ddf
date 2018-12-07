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
package org.codice.solr.query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.util.NamedList;
import org.codice.solr.client.solrj.SolrClient;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;

@RunWith(MockitoJUnitRunner.class)
public class SolrQueryFilterVisitorTest {

  @Mock private SolrClient solrClient;

  @Mock private org.apache.solr.client.solrj.SolrClient solrjClient;

  @Mock private NamedList namedList;

  private SolrQueryFilterVisitor solrVisitor;

  @Before
  public void setup() throws Exception {
    when(solrClient.getClient()).thenReturn(solrjClient);
    when(solrjClient.request(any(), nullable(String.class))).thenReturn(namedList);
    solrVisitor = new SolrQueryFilterVisitor(solrClient, "alerts");
  }

  @Test
  public void testEqualTo() throws Exception {
    Filter filter = ECQL.toFilter("property = 'test'");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery(), equalTo("property_txt:\"test\""));
  }

  @Test
  public void testEqualToID() throws Exception {
    Filter filter = ECQL.toFilter("'id' = 'test'");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery(), equalTo("id_txt:\"test\""));
  }

  @Test
  public void testGreaterThan() throws Exception {
    Filter filter = ECQL.toFilter("property > 9");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery().trim(), equalTo("property_txt:{ 9 TO * ]"));
  }

  @Test
  public void testGreaterThanOrEqualTo() throws Exception {
    Filter filter = ECQL.toFilter("property >= 9");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery().trim(), equalTo("property_txt:[ 9 TO * ]"));
  }

  @Test
  public void testLessThan() throws Exception {
    Filter filter = ECQL.toFilter("property < 9");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery().trim(), equalTo("property_txt:[ * TO 9 }"));
  }

  @Test
  public void testLessThanOrEqualTo() throws Exception {
    Filter filter = ECQL.toFilter("property <= 9");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery().trim(), equalTo("property_txt:[ * TO 9 ]"));
  }

  @Test
  public void testAnd() throws Exception {
    Filter filter = ECQL.toFilter("property = 'val' AND otherProp = 'val2'");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(
        solrQuery.getQuery().trim(),
        equalTo("( property_txt:\"val\" AND otherProp_txt:\"val2\" )"));
  }

  @Test
  public void testOr() throws Exception {
    Filter filter = ECQL.toFilter("property = 'val' OR otherProp = 'val2'");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(
        solrQuery.getQuery().trim(), equalTo("( property_txt:\"val\" OR otherProp_txt:\"val2\" )"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testOrNoChildren() throws Exception {
    Or or = mock(Or.class);
    when(or.getChildren()).thenReturn(Collections.emptyList());
    solrVisitor.visit(or, null);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testOrWithInvalidQuery() throws Exception {
    Filter filter = ECQL.toFilter("property = 'val' OR otherProp LIKE 'val2'");
    filter.accept(solrVisitor, null);
  }

  @Test
  public void testInSingleParam() throws Exception {
    // this is effectively an or with a single param which is allowed
    Filter filter = ECQL.toFilter("property IN ('val')");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery.getQuery().trim(), equalTo("( property_txt:\"val\" )"));
  }

  @Test
  public void testUnsupportedQuery() throws Exception {
    Filter filter = ECQL.toFilter("property LIKE 'val*'");
    SolrQuery solrQuery = (SolrQuery) filter.accept(solrVisitor, null);
    assertThat(solrQuery, equalTo(null));
  }

  @Test
  public void testGetMappedPropertyNameCache() {
    SchemaFieldResolver mockResolver = mock(SchemaFieldResolver.class);
    SchemaField schema = new SchemaField("testField_int", "tint");
    schema.setSuffix("_int");
    when(mockResolver.getSchemaField("testField", true)).thenReturn(schema);
    solrVisitor = new SolrQueryFilterVisitor("alerts", mockResolver);

    String propertyName = solrVisitor.getMappedPropertyName("testField");
    assertThat(propertyName, is("testField_int"));

    propertyName = solrVisitor.getMappedPropertyName("testField");
    assertThat(propertyName, is("testField_int"));
    verify(mockResolver, times(1)).getSchemaField("testField", true);
  }

  @Test
  public void testGetMappedPropertyNameNullNotCached() {
    SchemaFieldResolver mockResolver = mock(SchemaFieldResolver.class);

    // returning null simulates no entry in SOLR to query schema for "testField"
    when(mockResolver.getSchemaField("testField2", true)).thenReturn(null);
    when(mockResolver.getFieldSuffix(AttributeFormat.STRING)).thenReturn("_txt");
    solrVisitor = new SolrQueryFilterVisitor("alerts", mockResolver);

    String propertyName = solrVisitor.getMappedPropertyName("testField2");
    assertThat(propertyName, is("testField2_txt"));

    // simulates an entry in SOLR to query schema for "testField"
    SchemaField schema = new SchemaField("testField2_int", "tint");
    schema.setSuffix("_int");

    when(mockResolver.getSchemaField("testField2", true)).thenReturn(schema);
    propertyName = solrVisitor.getMappedPropertyName("testField2");
    assertThat(propertyName, is("testField2_int"));

    verify(mockResolver, times(2)).getSchemaField("testField2", true);
  }
}
