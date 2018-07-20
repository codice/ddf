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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.impl.SolrClientFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PersistentStoreImplTest {

  @Mock private SolrClientFactoryImpl solrClientFactory;

  @Mock private SolrClient solrClient;

  private PersistentStoreImpl persistentStore;

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
    verify(solrClient).add(captor.capture());
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
    verify(solrClient, never()).add(any(SolrInputDocument.class));
  }

  @Test
  public void testGet() throws Exception {
    QueryResponse response = mock(QueryResponse.class);
    SolrDocumentList docList = getSolrDocuments();

    when(response.getResults()).thenReturn(docList);
    when(solrClient.query(any(), eq(METHOD.POST))).thenReturn(response);
    List<Map<String, Object>> items = persistentStore.get("testcore");
    assertThat(items.size(), equalTo(2));
    assertThat(items.get(0).get("id_txt"), equalTo("idvalue1"));
    assertThat(items.get(1).get("id_txt"), equalTo("idvalue2"));
  }

  @Test
  public void testGetWithStartIndexAndPageSize() throws PersistenceException {
    final List<Map<String, Object>> items =
        persistentStore.get("testcore", "property LIKE 'value'", 0, 1);

    assertThat(items.size(), equalTo(1));
  }

  @Test(expected = PersistenceException.class)
  public void testGetInvalidQuery() throws Exception {
    List<Map<String, Object>> items = persistentStore.get("testcore", "property LIKE 'value'");
    assertThat(items.size(), equalTo(1));
    verify(solrClient, never()).query(any(), eq(SolrRequest.METHOD.POST));
  }

  private SolrDocumentList getSolrDocuments() {
    SolrDocumentList docList = new SolrDocumentList();

    SolrDocument docOne = new SolrDocument();
    docOne.addField("id_txt", "idvalue1");
    docList.add(docOne);

    SolrDocument docTwo = new SolrDocument();
    docTwo.addField("id_txt", "idvalue2");
    docList.add(docTwo);
    return docList;
  }
}
