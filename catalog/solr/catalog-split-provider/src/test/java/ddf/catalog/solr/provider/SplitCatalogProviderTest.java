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
package ddf.catalog.solr.provider;

import static ddf.catalog.solr.provider.SplitCatalogProvider.COLLECTION_HINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.solr.collection.SolrCollectionProvider;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SolrCatalogProviderImpl;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opengis.filter.Filter;

public class SplitCatalogProviderTest {

  private FilterBuilder filterBuilder;

  private SolrCatalogProviderImpl catalogProvider;

  private SplitCatalogProvider splitProvider;

  private FilterAdapter filterAdapter;

  private SolrClient solrClientMock;

  private SolrClientFactory cloudClientMock;

  private SolrCollectionProvider collectionProvider;

  @Before
  public void setUp() {
    cloudClientMock = mock(SolrClientFactory.class);
    solrClientMock = mock(SolrClient.class);
    filterAdapter = mock(FilterAdapter.class);
    filterBuilder = new GeotoolsFilterBuilder();

    when(cloudClientMock.newClient(any(), any())).thenReturn(solrClientMock);

    collectionProvider = mock(SolrCollectionProvider.class);
    when(collectionProvider.getCollection(any())).thenReturn("default");

    splitProvider =
        new SplitCatalogProvider(
            cloudClientMock,
            filterAdapter,
            mock(SolrFilterDelegateFactory.class),
            mock(DynamicSchemaResolver.class),
            Collections.singletonList(collectionProvider));
    splitProvider.setGetHandlerWorkaround(false);
    splitProvider.setCollectionAlias("catalog");

    catalogProvider = mock(SolrCatalogProviderImpl.class);
  }

  @Test
  public void testCreate() throws Exception {
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);
    CreateResponse response = createRecord(splitProvider);
    assertThat(response.getCreatedMetacards().size(), equalTo(2));
  }

  @Test
  public void testQuery() throws Exception {
    splitProvider.catalogProviders.put("catalog", catalogProvider);
    when(filterAdapter.adapt(any(), any())).thenReturn(false);
    Filter filter = filterBuilder.attribute("anyText").is().like().text("*");
    List<Metacard> records = getTestRecords();

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));
    when(catalogProvider.query(request))
        .thenReturn(
            new SourceResponseImpl(
                request,
                records.stream().map(this::getScoredResult).collect(Collectors.toList()),
                Long.valueOf(records.size())));

    SourceResponse response = splitProvider.query(request);
    assertThat(response.getHits(), equalTo(2L));
  }

  @Test
  public void testQueryWorkAround() throws Exception {
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);
    splitProvider.setGetHandlerWorkaround(true);
    when(filterAdapter.adapt(any(), any())).thenReturn(true);
    Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("1234");
    List<Metacard> records = getTestRecords();

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));
    request.getProperties().put(SolrMetacardClientImpl.DO_REALTIME_GET, true);
    when(catalogProvider.query(request))
        .thenReturn(
            new SourceResponseImpl(
                request,
                records.stream().map(this::getScoredResult).collect(Collectors.toList()),
                Long.valueOf(records.size())));

    SourceResponse response = splitProvider.query(request);
    assertThat(response.getHits(), equalTo(2L));
  }

  @Test
  public void testQueryWithHint() throws Exception {
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);
    when(filterAdapter.adapt(any(), any())).thenReturn(true);
    Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("1234");
    List<Metacard> records = getTestRecords();

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));
    request.getProperties().put(COLLECTION_HINT, "catalog_default");
    request.getProperties().put(SolrMetacardClientImpl.DO_REALTIME_GET, true);
    when(catalogProvider.query(request))
        .thenReturn(
            new SourceResponseImpl(
                request,
                records.stream().map(this::getScoredResult).collect(Collectors.toList()),
                Long.valueOf(records.size())));

    SourceResponse response = splitProvider.query(request);
    assertThat(response.getHits(), equalTo(2L));
  }

  @Test
  public void testQueryWithHintNotRealTime() throws Exception {
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);
    when(filterAdapter.adapt(any(), any())).thenReturn(false);
    Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("1234");
    List<Metacard> records = getTestRecords();

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));
    request.getProperties().put(COLLECTION_HINT, "catalog_default");
    when(catalogProvider.query(request))
        .thenReturn(
            new SourceResponseImpl(
                request,
                records.stream().map(this::getScoredResult).collect(Collectors.toList()),
                Long.valueOf(records.size())));

    SourceResponse response = splitProvider.query(request);
    assertThat(response.getHits(), equalTo(2L));
  }

  @Test
  public void testRealTimeQuery() throws Exception {
    splitProvider.catalogProviders.put("catalog", catalogProvider);
    when(filterAdapter.adapt(any(), any())).thenReturn(true);

    Filter filter = filterBuilder.attribute(Metacard.ID).is().like().text("1234");
    List<Metacard> records = getTestRecords();

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));
    request.getProperties().put(SolrMetacardClientImpl.DO_REALTIME_GET, true);
    when(catalogProvider.query(any()))
        .thenReturn(
            new SourceResponseImpl(
                request,
                records.stream().map(this::getScoredResult).collect(Collectors.toList()),
                Long.valueOf(records.size())));

    SourceResponse response = splitProvider.query(request);
    assertThat(response.getHits(), equalTo(2L));
  }

  @Test
  public void testUpdate() throws Exception {
    splitProvider.catalogProviders.put("catalog", catalogProvider);
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);

    List<Metacard> records = getTestRecords();

    UpdateRequest request =
        new UpdateRequestImpl(
            records.stream().map(Metacard::getId).toArray(String[]::new), records);

    when(catalogProvider.update(any()))
        .thenReturn(new UpdateResponseImpl(request, request.getProperties(), records, records));
    UpdateResponse response = splitProvider.update(request);
    assertThat(response.getUpdatedMetacards().size(), equalTo(2));
  }

  @Test
  public void testDelete() throws Exception {
    splitProvider.catalogProviders.put("catalog", catalogProvider);
    splitProvider.catalogProviders.put("catalog_default", catalogProvider);

    List<Metacard> records = getTestRecords();
    DeleteRequest deleteRequest =
        new DeleteRequestImpl(records.stream().map(Metacard::getId).toArray(String[]::new));

    when(catalogProvider.delete(any()))
        .thenReturn(new DeleteResponseImpl(deleteRequest, Collections.emptyMap(), records));

    splitProvider.delete(deleteRequest);
  }

  @Test
  public void testIsAvailableFalse() throws SolrServerException, IOException {
    SolrPingResponse pingResponse = mock(SolrPingResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(pingResponse.getResponse().get("status")).thenReturn("FALSE");
    when(solrClientMock.ping()).thenReturn(pingResponse);
    boolean avail = splitProvider.isAvailable();
    assertThat(avail, is(false));
  }

  @Test
  public void testIsAvailableTrue() throws SolrServerException, IOException {
    SolrPingResponse pingResponse = mock(SolrPingResponse.class, Mockito.RETURNS_DEEP_STUBS);
    when(pingResponse.getResponse().get("status")).thenReturn("OK");
    when(solrClientMock.ping()).thenReturn(pingResponse);
    boolean avail = splitProvider.isAvailable();
    assertThat(avail, is(true));
  }

  @Test
  public void testForceAutoCommit() {
    splitProvider.setForceAutoCommit(true);
    assertThat(ConfigurationStore.getInstance().isForceAutoCommit(), is(true));
  }

  @Test
  public void testRefresh() {
    final String alias = "mycatalog";
    Map<String, Object> props = new HashMap<>();
    props.put(SplitCatalogProvider.GET_HANDLER_WORKAROUND_PROP, Boolean.TRUE);
    props.put(SplitCatalogProvider.COLLECTION_ALIAS_PROP, alias);
    splitProvider.refresh(props);

    assertThat(splitProvider.isGetHandlerWorkaround(), is(true));
    assertThat(splitProvider.getCollectionAlias(), is(alias));
  }

  private CreateResponse createRecord(SplitCatalogProvider provider) throws Exception {
    List<Metacard> list =
        Arrays.asList(
            MockMetacard.createMetacard(MockMetacard.getFlagstaffRecord()),
            MockMetacard.createMetacard(MockMetacard.getTampaRecord()));

    CreateRequest request = new CreateRequestImpl(list);
    when(catalogProvider.create(any()))
        .thenReturn(new CreateResponseImpl(request, request.getProperties(), list));

    return provider.create(request);
  }

  private List<Metacard> getTestRecords() {
    Metacard metacard1 = MockMetacard.createMetacard(MockMetacard.getFlagstaffRecord());
    metacard1.setAttribute(new AttributeImpl(Core.ID, "1"));
    Metacard metacard2 = MockMetacard.createMetacard(MockMetacard.getTampaRecord());
    metacard2.setAttribute(new AttributeImpl(Core.ID, "2"));
    return Arrays.asList(metacard1, metacard2);
  }

  private Result getScoredResult(Metacard metacard) {
    return new ResultImpl(metacard);
  }

  @Test
  public void testShutdown() {
    splitProvider.shutdown();
  }

  @Test
  public void testMaskId() {
    splitProvider.maskId("id");
  }
}
