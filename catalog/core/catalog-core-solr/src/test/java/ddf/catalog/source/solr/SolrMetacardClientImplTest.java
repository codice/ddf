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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.codice.solr.client.solrj.SolrClient;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;

public class SolrMetacardClientImplTest {
  private static final String DDF_METACARD_TYPE = "ddf.metacardType";

  private FilterBuilder builder;
  private TestSolrMetacardClientImpl clientImpl;
  private SolrClient client;
  private FilterAdapter catalogFilterAdapter;
  private SolrFilterDelegateFactory solrFilterDelegateFactory;
  private DynamicSchemaResolver dynamicSchemaResolver;
  private SolrQuery solrQuery;
  private QueryResponse queryResponse;

  class TestSolrMetacardClientImpl extends SolrMetacardClientImpl {
    public TestSolrMetacardClientImpl(
        SolrClient client,
        FilterAdapter catalogFilterAdapter,
        SolrFilterDelegateFactory solrFilterDelegateFactory,
        DynamicSchemaResolver dynamicSchemaResolver) {
      super(client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);
    }
  }

  @Before
  public void setup() throws UnsupportedQueryException, IOException, SolrServerException {
    client = mock(SolrClient.class);
    catalogFilterAdapter = mock(FilterAdapter.class);
    solrFilterDelegateFactory = mock(SolrFilterDelegateFactory.class);
    dynamicSchemaResolver = mock(DynamicSchemaResolver.class);
    builder = new GeotoolsFilterBuilder();
    solrQuery = mock(SolrQuery.class);
    queryResponse = mock(QueryResponse.class);
    clientImpl =
        new TestSolrMetacardClientImpl(
            client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);

    when(solrFilterDelegateFactory.newInstance(dynamicSchemaResolver, Collections.EMPTY_MAP))
        .thenReturn(mock(SolrFilterDelegate.class));
    when(solrFilterDelegateFactory.newInstance(
            dynamicSchemaResolver, Collections.singletonMap("spellcheck", new Boolean("true"))))
        .thenReturn(mock(SolrFilterDelegate.class));

    when(catalogFilterAdapter.adapt(any(), any())).thenReturn(solrQuery);
    when(client.query(solrQuery, SolrRequest.METHOD.POST)).thenReturn(queryResponse);
  }

  @Test
  public void testQueryOneResults() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));

    List<String> names = Collections.singletonList("title");
    List<String> values = Collections.singletonList("normal");

    Map<String, String> attributes = createAttributes(names, values);

    when(queryResponse.getResults()).thenReturn(createSolrDocumentList(attributes));
    mockDynamicSchemsolverCalls(createAttributeDescriptor(names), attributes);

    List<Result> results = clientImpl.query(request).getResults();
    assertThat(results.size(), is(1));
    assertThat(results.get(0).getMetacard().getAttribute("title").getValue(), is("normal"));
  }

  @Test
  public void testQueryMultipleResults() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));

    List<String> names = Arrays.asList("title", "title2");
    List<String> values = Arrays.asList("normal", "normal2");

    Map<String, String> attributes = createAttributes(names, values);

    when(queryResponse.getResults()).thenReturn(createSolrDocumentList(attributes));
    mockDynamicSchemsolverCalls(createAttributeDescriptor(names), attributes);

    List<Result> results = clientImpl.query(request).getResults();
    assertThat(results.size(), is(2));
    assertThat(results.get(0).getMetacard().getAttribute("title2").getValue(), is("normal2"));
    assertThat(results.get(1).getMetacard().getAttribute("title").getValue(), is("normal"));
  }

  @Test
  public void testQueryZeroResults() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));

    List<String> names = Collections.emptyList();
    List<String> values = Collections.emptyList();

    Map<String, String> attributes = createAttributes(names, values);

    when(queryResponse.getResults()).thenReturn(createSolrDocumentList(attributes));
    mockDynamicSchemsolverCalls(createAttributeDescriptor(names), attributes);

    List<Result> results = clientImpl.query(request).getResults();
    assertThat(results.size(), is(0));
  }

  @Test
  public void testQueryNullResults() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    when(queryResponse.getResults()).thenReturn(null);
    List<Result> results = clientImpl.query(request).getResults();
    assertThat(results, is(Collections.EMPTY_LIST));
  }

  @Test
  public void testQuerySpellCheckOn() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request.getProperties().put("spellcheck", new Boolean("true"));
    List<String> names = Collections.singletonList("title");
    List<String> values = Collections.singletonList("normal");
    SpellCheckResponse spellCheckResponse = mock(SpellCheckResponse.class);

    List<Collation> collations = new ArrayList<>();
    Collation collation = new Collation();
    collation.setCollationQueryString("real");
    collation.setNumberOfHits(2);
    collations.add(collation);

    Map<String, String> attributes = createAttributes(names, values);
    mockDynamicSchemsolverCalls(createAttributeDescriptor(names), attributes);
    when(queryResponse.getSpellCheckResponse()).thenReturn(spellCheckResponse);
    when(queryResponse.getSpellCheckResponse().getCollatedResults()).thenReturn(collations);
    when(queryResponse.getResults()).thenReturn(createSolrDocumentList(attributes));

    List<Result> results = clientImpl.query(request).getResults();
    assertThat(results.size(), is(1));
    verify(queryResponse, times(2)).getResults();
  }

  private void mockDynamicSchemsolverCalls(
      Set<AttributeDescriptor> descriptors, Map<String, String> attributes)
      throws MetacardCreationException {
    MetacardType metacardType = new MetacardTypeImpl(DDF_METACARD_TYPE, descriptors);
    when(dynamicSchemaResolver.getMetacardType(any())).thenReturn(metacardType);

    for (String name : attributes.keySet()) {
      when(dynamicSchemaResolver.resolveFieldName(name)).thenReturn(name);
      List<Serializable> value = Collections.singletonList(attributes.get(name));
      when(dynamicSchemaResolver.getDocValues(name, (Collection) value)).thenReturn(value);
    }
  }

  private QueryRequest createQuery(Filter query) {
    return new QueryRequestImpl(
        new QueryImpl(
            query, 1, 1, new SortByImpl("someAttribute", SortOrder.DESCENDING), false, 1000));
  }

  private Set<AttributeDescriptor> createAttributeDescriptor(List<String> names) {
    Set<AttributeDescriptor> descriptors = new HashSet<>();
    for (String name : names) {
      descriptors.add(
          new AttributeDescriptorImpl(name, true, true, true, true, BasicTypes.STRING_TYPE));
    }
    return descriptors;
  }

  private Map<String, String> createAttributes(List<String> names, List<String> values) {
    Map<String, String> attributes = new HashMap<>();
    for (int i = 0; i < names.size(); i++) {
      attributes.put(names.get(i), values.get(i));
    }
    return attributes;
  }

  private SolrDocumentList createSolrDocumentList(Map<String, String> attributes) {
    SolrDocumentList solrDocumentList = new SolrDocumentList();
    List<String> names = new ArrayList<>(attributes.keySet());
    for (String name : names) {
      solrDocumentList.add(createSolrDocument(name, attributes.get(name)));
    }
    return solrDocumentList;
  }

  private SolrDocument createSolrDocument(String name, String value) {
    SolrDocument solrDocument = new SolrDocument();
    solrDocument.addField(name, value);
    return solrDocument;
  }
}
