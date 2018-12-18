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
package org.codice.ddf.persistence.internal;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.impl.SolrClientFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PersistentStoreImplTest {

  @Mock private SolrClientFactoryImpl solrClientFactory;

  @Mock private SolrClient solrClient;

  private PersistentStoreImpl persistentStore;

  @Captor private ArgumentCaptor<SolrParams> solrParamsArgumentCaptor;

  @Before
  public void setup() throws Exception {
    when(solrClientFactory.newClient(any())).thenReturn(solrClient);
    when(solrClient.isAvailable(anyLong(), any())).thenReturn(true);
    persistentStore = new PersistentStoreImpl(solrClientFactory);
  }

  @Test
  public void testAdd() throws Exception {
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    PersistentItem props = new PersistentItem();
    props.addProperty("property", "value");
    persistentStore.add("testcore", props);
    verify(solrClient).add(captor.capture(), any(Integer.class));
    List docs = captor.getValue();
    assertThat(docs.size(), equalTo(1));
  }

  @Test(expected = PersistenceException.class)
  public void testAddNoType() throws Exception {
    PersistentItem props = new PersistentItem();
    props.addProperty("property", "value");
    persistentStore.add(null, props);
  }

  @Test
  public void testAddEmptyItems() throws Exception {
    persistentStore.add("testcore", Collections.emptyList());
    verify(solrClient, never()).add(any(SolrInputDocument.class));
  }

  @Test
  public void testAddEmptyProperties() throws Exception {
    PersistentItem props = new PersistentItem();
    persistentStore.add("testcore", props);
    verify(solrClient, never()).add(any(SolrInputDocument.class));
  }

  @Test
  public void testGet() throws Exception {
    QueryResponse response = mock(QueryResponse.class);
    SolrDocumentList docList = getSolrDocuments(2);

    when(response.getResults()).thenReturn(docList);
    when(solrClient.query(any(), eq(METHOD.POST))).thenReturn(response);
    List<Map<String, Object>> items = persistentStore.get("testcore");
    assertThat(items.size(), equalTo(2));
    assertThat(items.get(0).get("id_txt"), equalTo("idvalue1"));
    assertThat(items.get(1).get("id_txt"), equalTo("idvalue2"));
  }

  @Test
  public void testGetWithStartIndexAndPageSize()
      throws PersistenceException, IOException, SolrServerException {
    QueryResponse response = mock(QueryResponse.class);

    when(response.getResults()).thenReturn(getSolrDocuments(1));
    when(solrClient.query(any(), eq(METHOD.POST))).thenReturn(response);
    List<Map<String, Object>> items = persistentStore.get("testcore", "", 0, 1);

    verify(solrClient).query(solrParamsArgumentCaptor.capture(), eq(METHOD.POST));

    final SolrParams solrParams = solrParamsArgumentCaptor.getValue();

    assertThat(items.size(), equalTo(1));
    assertThat(solrParams.get("start"), is("0"));
    assertThat(solrParams.get("rows"), is("1"));

    assertThat(items.get(0).get("id_txt"), equalTo("idvalue1"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithInvalidStartIndex() throws Exception {
    persistentStore.get("testcore", "", -1, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetWithInvalidPageSize() throws Exception {
    persistentStore.get("testcore", "", 0, 5000);
  }

  @Test(expected = PersistenceException.class)
  public void testGetInvalidQuery() throws Exception {
    List<Map<String, Object>> items = persistentStore.get("testcore", "property LIKE 'value'");
    assertThat(items.size(), equalTo(1));
    verify(solrClient, never()).query(any(), eq(SolrRequest.METHOD.POST));
  }

  private SolrDocumentList getSolrDocuments(int numDocuments) {
    final SolrDocumentList docList = new SolrDocumentList();

    for (int i = 0; i < numDocuments; i++) {
      SolrDocument solrDocument = new SolrDocument();
      solrDocument.addField("id_txt", String.format("idvalue%d", i + 1));
      docList.add(solrDocument);
    }

    return docList;
  }
}
