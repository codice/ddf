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

import static ddf.catalog.Constants.QUERY_HIGHLIGHT_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
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
import ddf.catalog.operation.Highlight;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResultAttributeHighlight;
import ddf.catalog.operation.ResultHighlight;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.HighlightImpl;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse;
import org.apache.solr.client.solrj.response.SpellCheckResponse.Collation;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

    @Override
    protected SolrQuery getRealTimeQuery(SolrQuery originalQuery, Collection<String> ids) {
      return originalQuery;
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
    when(solrFilterDelegateFactory.newInstance(
            dynamicSchemaResolver,
            Collections.singletonMap(SolrMetacardClientImpl.DO_REALTIME_GET, new Boolean("true"))))
        .thenReturn(mock(SolrFilterDelegate.class));

    when(catalogFilterAdapter.adapt(any(), any()))
        .thenAnswer(
            new Answer() {
              public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Object filterDelegate = args[1];
                if (filterDelegate instanceof RealTimeGetDelegate) {
                  return false;
                } else {
                  return solrQuery;
                }
              }
            });
    when(client.query(solrQuery, SolrRequest.METHOD.POST)).thenReturn(queryResponse);
  }

  @Test
  public void testDeleteByIds() throws SolrServerException, IOException {
    when(client.deleteById(anyList())).thenReturn(new UpdateResponse());
    List<String> terms = Arrays.asList("1234");
    clientImpl.deleteByIds(Metacard.ID, terms, false);
  }

  @Test
  public void testDeleteEmpty() throws SolrServerException, IOException {
    when(client.deleteById(anyList())).thenReturn(new UpdateResponse());
    clientImpl.deleteByIds(Metacard.ID, null, false);
  }

  @Test
  public void testDeleteSingle() throws SolrServerException, IOException {
    when(client.deleteByQuery(anyString())).thenReturn(new UpdateResponse());
    List<String> terms = Arrays.asList("title");
    clientImpl.deleteByIds(Metacard.TITLE, terms, false);
  }

  @Test
  public void testDeleteLargeSet() throws SolrServerException, IOException {
    when(client.deleteByQuery(anyString())).thenReturn(new UpdateResponse());
    List<String> terms = new ArrayList<>(SolrCatalogProviderImpl.MAX_BOOLEAN_CLAUSES + 1);
    for (int i = 0; i < SolrCatalogProviderImpl.MAX_BOOLEAN_CLAUSES + 1; i++) {
      terms.add("title");
    }
    clientImpl.deleteByIds(Metacard.TITLE, terms, false);
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
  public void testRealTimeQuery() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request.getProperties().put(SolrMetacardClientImpl.DO_REALTIME_GET, true);
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

  @Test
  public void testPartialResults() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));

    List<String> names = Collections.emptyList();
    List<String> values = Collections.emptyList();

    Map<String, String> attributes = createAttributes(names, values);

    when(queryResponse.getResults()).thenReturn(createSolrDocumentList(attributes));
    NamedList responseHeader = mock(NamedList.class);
    when(queryResponse.getResponseHeader()).thenReturn(responseHeader);
    when(responseHeader.get("partialResults")).thenReturn(Boolean.TRUE);
    mockDynamicSchemsolverCalls(createAttributeDescriptor(names), attributes);

    SourceResponse response = clientImpl.query(request);
    assertThat(response.getPropertyValue("partial-results"), is(Boolean.TRUE));
  }

  @Test
  public void testHighlightOn() throws Exception {
    System.setProperty(ResultHighlighter.HIGHLIGHT_ENABLE_PROPERTY, "true");

    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));

    List<String> docNames = Arrays.asList("id_txt", "title_txt", "description_txt");
    List<String> attrNames = Arrays.asList("id", "title", "description");
    List<String> values =
        Arrays.asList(
            "123", "normal", "<em>normal</em> normal description with two normal <em>matches</em>");

    Map<String, String> attributes = createAttributes(docNames, values);
    Map<String, Map<String, String>> documents = new HashMap<>();
    documents.put("123", attributes);

    Map<String, Map<String, List<String>>> resultsHighlightData = new HashMap<>();
    Map<String, List<String>> recordHighlights = new HashMap<>();
    recordHighlights.put("title_txt", Collections.singletonList("<em>normal</em>"));
    recordHighlights.put(
        "description_txt_tokenized",
        Collections.singletonList(
            "<em><em>normal</em></em> <em>normal</em> description with two <em>normal</em> <em>matches</em>"));
    resultsHighlightData.put("123", recordHighlights);

    when(queryResponse.getHighlighting()).thenReturn(resultsHighlightData);
    when(queryResponse.getResults()).thenReturn(createSolrDocuments(documents));
    mockDynamicSchemsolverCalls(createAttributeDescriptor(attrNames), attributes);

    SourceResponse response = clientImpl.query(request);

    System.setProperty(ResultHighlighter.HIGHLIGHT_ENABLE_PROPERTY, "false");

    List<Result> results = response.getResults();
    assertThat(results.size(), is(1));
    assertThat(results.get(0).getMetacard().getAttribute("title").getValue(), is("normal"));
    List<ResultHighlight> highlights =
        (List<ResultHighlight>) response.getPropertyValue(QUERY_HIGHLIGHT_KEY);
    assertThat(highlights, notNullValue());
    assertThat(highlights.size(), is(1));

    List<ResultAttributeHighlight> highlightedAttributes =
        highlights.get(0).getAttributeHighlights();

    List<String> attributeNames =
        highlightedAttributes.stream()
            .map(ResultAttributeHighlight::getAttributeName)
            .collect(Collectors.toList());
    assertThat(attributeNames, containsInAnyOrder("title", "description"));

    Optional<List<Highlight>> descriptionHighlights =
        highlightedAttributes.stream()
            .filter(attr -> attr.getAttributeName().equals("description"))
            .findFirst()
            .map(ResultAttributeHighlight::getHighlights);
    assertThat(descriptionHighlights.isPresent(), is(true));

    assertThat(descriptionHighlights.get().size(), is(3));
    verifyHighlight(descriptionHighlights.get(), new HighlightImpl(4, 10, 0));
    verifyHighlight(descriptionHighlights.get(), new HighlightImpl(16, 22, 0));
    verifyHighlight(descriptionHighlights.get(), new HighlightImpl(44, 50, 0));
  }

  @Test
  public void testQueryWithoutFieldRestrictionsKeepsDefaultFieldList() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(realQuery.getFields(), is(nullValue()));
  }

  @Test
  public void testIncludedFieldsRestrictsFieldList() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(
            SolrMetacardClientImpl.INCLUDED_FIELDS,
            new ArrayList<>(Arrays.asList("title", "custom")));
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder(
            "id_txt", "title_txt", "custom_txt", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test
  public void testIncludedFieldsPreservesRelevanceScore() throws Exception {
    QueryImpl query =
        new QueryImpl(
            builder.attribute("anyText").is().like().text("normal"),
            1,
            1,
            new SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING),
            false,
            1000);
    QueryRequest request = new QueryRequestImpl(query);
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder("id_txt", "title_txt", "score", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test
  public void testExcludedFieldsRemovesFromFieldList() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(
            SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("author", "id")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields();

    clientImpl.query(request);

    // author and its derived fields are removed; id is required so it cannot be excluded
    assertThat(fieldList(realQuery), containsInAnyOrder(knownFieldsExcept("author_txt")));
  }

  @Test
  public void testExcludedThumbnailRemovesBinaryField() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("thumbnail")));
    SolrQuery realQuery = prepareRealQuery(request);
    when(dynamicSchemaResolver.getAnonymousField("thumbnail"))
        .thenReturn(Collections.singletonList("thumbnail_bin"));
    mockKnownFields();

    clientImpl.query(request);

    assertThat(fieldList(realQuery), containsInAnyOrder(knownFieldsExcept("thumbnail_bin")));
  }

  @Test
  public void testExcludedFieldsPreservesRelevanceScore() throws Exception {
    QueryImpl query =
        new QueryImpl(
            builder.attribute("anyText").is().like().text("normal"),
            1,
            1,
            new SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING),
            false,
            1000);
    QueryRequest request = new QueryRequestImpl(query);
    request
        .getProperties()
        .put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("author")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields();

    clientImpl.query(request);

    List<String> expected = new ArrayList<>(Arrays.asList(knownFieldsExcept("author_txt")));
    expected.add("score");
    assertThat(fieldList(realQuery), containsInAnyOrder(expected.toArray(new String[0])));
  }

  @Test
  public void testIncludedFieldsWildcardPassesThrough() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("custom.*")));
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder(
            "id_txt", "title_txt", "custom.*", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test
  public void testExcludedFieldsWildcardIsExpanded() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("ext.xyz.*")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields("ext.xyz.a_txt", "ext.xyz.a_txt_sort", "ext.other_txt");

    clientImpl.query(request);

    List<String> expected = new ArrayList<>(Arrays.asList(knownFieldsExcept()));
    expected.add("ext.other_txt");
    assertThat(fieldList(realQuery), containsInAnyOrder(expected.toArray(new String[0])));
  }

  @Test
  public void testExcludedFieldsWildcardCollidingWithRequiredIsExpanded() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("*_txt")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields();

    clientImpl.query(request);

    // *_txt matches id_txt, title_txt and author_txt, but the required id and title are kept
    assertThat(fieldList(realQuery), containsInAnyOrder(knownFieldsExcept("author_txt")));
  }

  @Test
  public void testFieldListRestrictionDisabledByFeatureFlag() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    SolrQuery realQuery = prepareRealQuery(request);
    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_ENABLED, "false");

    clientImpl.query(request);

    assertThat(realQuery.getFields(), is(nullValue()));
  }

  @Test
  public void testConfiguredRequiredFieldsCannotBeExcluded() throws Exception {
    System.setProperty(
        SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_REQUIRED_FIELDS, "owner, metadata.*");
    clientImpl =
        new TestSolrMetacardClientImpl(
            client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);

    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(
            SolrMetacardClientImpl.EXCLUDED_FIELDS,
            new ArrayList<>(Arrays.asList("author", "owner", "metadata.*")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields("owner_txt", "metadata.foo_txt");

    clientImpl.query(request);

    // author_txt excluded; owner_txt and metadata.* are required so they cannot be excluded
    List<String> expected = new ArrayList<>(Arrays.asList(knownFieldsExcept("author_txt")));
    expected.add("owner_txt");
    expected.add("metadata.foo_txt");
    assertThat(fieldList(realQuery), containsInAnyOrder(expected.toArray(new String[0])));

    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_REQUIRED_FIELDS, "");
  }

  @Test
  public void testConfiguredRequiredWildcardOnlyAddsBaseFields() throws Exception {
    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_REQUIRED_FIELDS, "security.*");
    clientImpl =
        new TestSolrMetacardClientImpl(
            client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);

    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields(
        "security.access-groups_txt",
        "security.access-groups_txt_tokenized",
        "security.access-groups_txt_phonetics");

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder(
            "id_txt",
            "title_txt",
            "security.access-groups_txt",
            SchemaFields.METACARD_TYPE_FIELD_NAME));

    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_REQUIRED_FIELDS, "");
  }

  @Test
  public void testEmptyFieldListPropertyIsIgnored() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request.getProperties().put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<String>());
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(realQuery.getFields(), is(nullValue()));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testBothFieldListPropertiesThrows() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("author")));
    prepareRealQuery(request);

    clientImpl.query(request);
  }

  @Test
  public void testInvalidFieldListPropertyTypeIsIgnored() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request.getProperties().put(SolrMetacardClientImpl.INCLUDED_FIELDS, "title");
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(realQuery.getFields(), is(nullValue()));
  }

  @Test
  public void testObjectArrayFieldListIsAccepted() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new Object[] {"title", "custom"});
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder(
            "id_txt", "title_txt", "custom_txt", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test
  public void testIncludedFieldsReadFromUncheckedProperties() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    HashMap<String, Serializable> unchecked = new HashMap<>();
    unchecked.put(
        SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title", "custom")));
    request.getProperties().put(SolrMetacardClientImpl.UNCHECKED_PROPERTIES, unchecked);
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder(
            "id_txt", "title_txt", "custom_txt", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test
  public void testExcludedFieldsReadFromUncheckedProperties() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    HashMap<String, Serializable> unchecked = new HashMap<>();
    unchecked.put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("author")));
    request.getProperties().put(SolrMetacardClientImpl.UNCHECKED_PROPERTIES, unchecked);
    SolrQuery realQuery = prepareRealQuery(request);
    mockKnownFields();

    clientImpl.query(request);

    assertThat(fieldList(realQuery), containsInAnyOrder(knownFieldsExcept("author_txt")));
  }

  @Test
  public void testTopLevelFieldListTakesPrecedenceOverUncheckedProperties() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    HashMap<String, Serializable> unchecked = new HashMap<>();
    unchecked.put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("custom")));
    request.getProperties().put(SolrMetacardClientImpl.UNCHECKED_PROPERTIES, unchecked);
    SolrQuery realQuery = prepareRealQuery(request);

    clientImpl.query(request);

    assertThat(
        fieldList(realQuery),
        containsInAnyOrder("id_txt", "title_txt", SchemaFields.METACARD_TYPE_FIELD_NAME));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testBothFieldListPropertiesInUncheckedPropertiesThrows() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    HashMap<String, Serializable> unchecked = new HashMap<>();
    unchecked.put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    unchecked.put(SolrMetacardClientImpl.EXCLUDED_FIELDS, new ArrayList<>(Arrays.asList("author")));
    request.getProperties().put(SolrMetacardClientImpl.UNCHECKED_PROPERTIES, unchecked);
    prepareRealQuery(request);

    clientImpl.query(request);
  }

  private static final List<String> KNOWN_FIELDS =
      Arrays.asList(
          "id_txt",
          "id_txt_sort",
          "title_txt",
          "title_txt_sort",
          "title_txt_tokenized",
          "author_txt",
          "author_txt_sort",
          "author_txt_tokenized",
          "thumbnail_bin",
          "metadata_xml",
          SchemaFields.METACARD_TYPE_FIELD_NAME,
          SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME);

  /**
   * Fields expected in an exclusion field list: the base attribute fields among {@link
   * #KNOWN_FIELDS}. Derived index-only fields and the serialized metacard type object are omitted.
   */
  private static final List<String> BASE_FIELDS =
      Arrays.asList(
          "id_txt",
          "title_txt",
          "author_txt",
          "thumbnail_bin",
          "metadata_xml",
          SchemaFields.METACARD_TYPE_FIELD_NAME);

  private void mockKnownFields(String... extraFields) {
    Set<String> fields = new HashSet<>(KNOWN_FIELDS);
    fields.addAll(Arrays.asList(extraFields));
    when(dynamicSchemaResolver.getFieldNames()).thenReturn(fields);
    when(dynamicSchemaResolver.getAnonymousField("thumbnail"))
        .thenReturn(Collections.singletonList("thumbnail_bin"));
    when(dynamicSchemaResolver.getAnonymousField("metadata"))
        .thenReturn(Collections.singletonList("metadata_xml"));
    when(dynamicSchemaResolver.resolveFieldName(anyString()))
        .thenAnswer(
            invocation -> {
              String field = invocation.getArgument(0, String.class);
              int index = field.lastIndexOf('_');
              return index == -1 ? field : field.substring(0, index);
            });
  }

  /** @return the base attribute fields expected in an exclusion field list, minus the given ones */
  private static String[] knownFieldsExcept(String... excluded) {
    List<String> excludedList = Arrays.asList(excluded);
    return BASE_FIELDS.stream()
        .filter(field -> !excludedList.contains(field))
        .toArray(String[]::new);
  }

  private static List<String> fieldList(SolrQuery query) {
    String fields = query.getFields();
    return fields == null ? Collections.emptyList() : Arrays.asList(fields.split(","));
  }

  private SolrQuery prepareRealQuery(QueryRequest request) throws Exception {
    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_ENABLED, "true");
    SolrQuery realQuery = new SolrQuery();
    when(solrFilterDelegateFactory.newInstance(dynamicSchemaResolver, request.getProperties()))
        .thenReturn(mock(SolrFilterDelegate.class));
    when(catalogFilterAdapter.adapt(any(), any()))
        .thenAnswer(
            invocation -> {
              if (invocation.getArguments()[1] instanceof RealTimeGetDelegate) {
                return false;
              }
              return realQuery;
            });
    when(client.query(realQuery, SolrRequest.METHOD.POST)).thenReturn(queryResponse);
    when(queryResponse.getResults()).thenReturn(new SolrDocumentList());
    when(dynamicSchemaResolver.getAnonymousField(anyString()))
        .thenAnswer(
            invocation ->
                Collections.singletonList(invocation.getArgument(0, String.class) + "_txt"));
    when(dynamicSchemaResolver.getSortKey(anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0, String.class));
    return realQuery;
  }

  private void verifyHighlight(List<Highlight> results, Highlight mustContain) {
    boolean found = false;
    for (Highlight highlight : results) {
      if (highlight.getBeginIndex() == mustContain.getBeginIndex()
          && highlight.getEndIndex() == mustContain.getEndIndex()
          && highlight.getValueIndex() == mustContain.getValueIndex()) {
        found = true;
        break;
      }
    }
    assertThat(found, is(true));
  }

  @Test
  public void testUncachedMetacardTypesFetchedOncePerType() throws Exception {
    SolrDocumentList results =
        resultDocs(typeDoc("1", "a#1", null), typeDoc("2", "a#1", null), typeDoc("3", "b#2", null));
    QueryRequest request = prepareTypeResolutionQuery(results);
    byte[] aBytes = new byte[] {1};
    byte[] bBytes = new byte[] {2};
    SolrDocumentList typeDocs =
        resultDocs(typeDoc("1", "a#1", aBytes), typeDoc("3", "b#2", bBytes));
    when(client.getById(anyCollection(), any(SolrParams.class))).thenReturn(typeDocs);

    clientImpl.query(request);

    ArgumentCaptor<Collection<String>> ids = ArgumentCaptor.forClass(Collection.class);
    ArgumentCaptor<SolrParams> params = ArgumentCaptor.forClass(SolrParams.class);
    verify(client, times(1)).getById(ids.capture(), params.capture());
    assertThat(ids.getValue(), containsInAnyOrder("1", "3"));
    assertThat(
        Arrays.asList(params.getValue().getParams(CommonParams.FL)),
        hasItem(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME));
    verify(dynamicSchemaResolver).cacheMetacardType("a#1", aBytes);
    verify(dynamicSchemaResolver).cacheMetacardType("b#2", bBytes);
  }

  @Test
  public void testCachedMetacardTypeNotFetched() throws Exception {
    QueryRequest request = prepareTypeResolutionQuery(resultDocs(typeDoc("1", "a#1", null)));
    when(dynamicSchemaResolver.isMetacardTypeCached("a#1")).thenReturn(true);

    clientImpl.query(request);

    verify(client, never()).getById(anyCollection(), any(SolrParams.class));
  }

  @Test
  public void testMetacardTypeReturnedWithDocNotFetched() throws Exception {
    QueryRequest request =
        prepareTypeResolutionQuery(resultDocs(typeDoc("1", "a#1", new byte[] {1})));

    clientImpl.query(request);

    verify(client, never()).getById(anyCollection(), any(SolrParams.class));
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testUnresolvableMetacardTypeFailsQuery() throws Exception {
    QueryRequest request = prepareTypeResolutionQuery(resultDocs(typeDoc("1", "a#1", null)));
    when(client.getById(anyCollection(), any(SolrParams.class))).thenReturn(new SolrDocumentList());
    when(dynamicSchemaResolver.getMetacardType(any()))
        .thenThrow(new MetacardCreationException("not cached"));

    clientImpl.query(request);
  }

  @Test
  public void testFieldListRestrictedResultsMarkedPartial() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    prepareRealQuery(request);
    MetacardType type = prepareResultDocs(typeDoc("1", "a#1", null), typeDoc("2", "a#1", null));
    when(dynamicSchemaResolver.isMetacardTypeCached("a#1")).thenReturn(true);

    SourceResponse response = clientImpl.query(request);

    assertThat(response.getResults().size(), is(2));
    MetacardType firstType = response.getResults().get(0).getMetacard().getMetacardType();
    for (Result result : response.getResults()) {
      Metacard metacard = result.getMetacard();
      assertThat(
          metacard.getAttribute(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE).getValue(),
          is(true));
      assertThat(
          metacard
              .getMetacardType()
              .getAttributeDescriptor(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE),
          notNullValue());
      assertThat(metacard.getMetacardType().getName(), is(type.getName()));
      // the extended type is built once and shared by results of the same type
      assertThat(metacard.getMetacardType() == firstType, is(true));
    }
  }

  @Test
  public void testUnrestrictedResultsNotMarkedPartial() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    prepareRealQuery(request);
    MetacardType type = prepareResultDocs(typeDoc("1", "a#1", new byte[] {1}));

    SourceResponse response = clientImpl.query(request);

    Metacard metacard = response.getResults().get(0).getMetacard();
    assertThat(metacard.getAttribute(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE), nullValue());
    assertThat(metacard.getMetacardType() == type, is(true));
  }

  @Test
  public void testResultsNotMarkedPartialWhenFeatureDisabled() throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    request
        .getProperties()
        .put(SolrMetacardClientImpl.INCLUDED_FIELDS, new ArrayList<>(Arrays.asList("title")));
    prepareRealQuery(request);
    System.setProperty(SolrMetacardClientImpl.SOLR_QUERY_FIELDLIST_ENABLED, "false");
    prepareResultDocs(typeDoc("1", "a#1", new byte[] {1}));

    SourceResponse response = clientImpl.query(request);

    Metacard metacard = response.getResults().get(0).getMetacard();
    assertThat(metacard.getAttribute(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE), nullValue());
  }

  /** Makes the mocked Solr query return the given docs, all resolved to one metacard type. */
  private MetacardType prepareResultDocs(SolrDocument... docs) throws Exception {
    when(queryResponse.getResults()).thenReturn(resultDocs(docs));
    MetacardType type =
        new MetacardTypeImpl(
            DDF_METACARD_TYPE,
            Collections.singleton(
                new AttributeDescriptorImpl(
                    Metacard.TITLE, true, true, false, false, BasicTypes.STRING_TYPE)));
    when(dynamicSchemaResolver.getMetacardType(any())).thenReturn(type);
    when(dynamicSchemaResolver.isPrivateField(anyString())).thenReturn(true);
    return type;
  }

  private QueryRequest prepareTypeResolutionQuery(SolrDocumentList results) throws Exception {
    QueryRequest request = createQuery(builder.attribute("anyText").is().like().text("normal"));
    when(queryResponse.getResults()).thenReturn(results);
    when(dynamicSchemaResolver.getMetacardType(any()))
        .thenReturn(new MetacardTypeImpl(DDF_METACARD_TYPE, new HashSet<>()));
    when(dynamicSchemaResolver.isPrivateField(anyString())).thenReturn(true);
    return request;
  }

  private static SolrDocument typeDoc(String id, String typeKey, byte[] typeBytes) {
    SolrDocument doc = new SolrDocument();
    doc.addField(Metacard.ID + SchemaFields.TEXT_SUFFIX, id);
    doc.addField(SchemaFields.METACARD_TYPE_FIELD_NAME, typeKey);
    if (typeBytes != null) {
      doc.addField(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME, typeBytes);
    }
    return doc;
  }

  private static SolrDocumentList resultDocs(SolrDocument... docs) {
    SolrDocumentList list = new SolrDocumentList();
    list.addAll(Arrays.asList(docs));
    list.setNumFound(docs.length);
    return list;
  }

  private void mockDynamicSchemsolverCalls(
      Set<AttributeDescriptor> descriptors, Map<String, String> attributes)
      throws MetacardCreationException {
    MetacardType metacardType = new MetacardTypeImpl(DDF_METACARD_TYPE, descriptors);
    when(dynamicSchemaResolver.getMetacardType(any())).thenReturn(metacardType);
    when(dynamicSchemaResolver.resolveFieldName(any())).thenCallRealMethod();

    for (String name : attributes.keySet()) {
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

  private SolrDocumentList createSolrDocuments(Map<String, Map<String, String>> docs) {
    SolrDocumentList solrDocumentList = new SolrDocumentList();
    List<String> names = new ArrayList<>(docs.keySet());
    for (String name : names) {
      solrDocumentList.add(createSolrDocument(docs.get(name)));
    }
    return solrDocumentList;
  }

  private SolrDocument createSolrDocument(Map<String, String> attributes) {
    SolrDocument solrDocument = new SolrDocument();
    for (Map.Entry<String, String> attr : attributes.entrySet()) {
      solrDocument.addField(attr.getKey(), attr.getValue());
    }
    return solrDocument;
  }
}
