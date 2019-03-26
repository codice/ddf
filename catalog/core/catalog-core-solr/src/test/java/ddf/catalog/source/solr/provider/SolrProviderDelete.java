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
package ddf.catalog.source.solr.provider;

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.delete;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import ddf.catalog.source.solr.SolrProviderTest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;

public class SolrProviderDelete {

  private static SolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();
  }
  /** Testing that if records are properly deleted. */
  @Test
  public void testDeleteOperation() throws IngestException, UnsupportedQueryException {

    // Single Deletion

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    CreateResponse createResponse = create(metacard, provider);

    DeleteResponse deleteResponse =
        delete(createResponse.getCreatedMetacards().get(0).getId(), provider);

    Metacard deletedMetacard = deleteResponse.getDeletedMetacards().get(0);

    verifyDeletedRecord(metacard, createResponse, deleteResponse, deletedMetacard);
  }

  @Test
  public void testDeleteList() throws IngestException, UnsupportedQueryException {
    int metacardCount = 20;
    addAndDeleteMetacards(metacardCount);
  }

  @Test
  public void testDeleteLargeList() throws IngestException, UnsupportedQueryException {
    int metacardCount = 2000;
    addAndDeleteMetacards(metacardCount);
  }

  @Test
  public void testDeleteLargeGetByIdList() throws IngestException, UnsupportedQueryException {
    addAndDeleteMetacards(SolrMetacardClientImpl.GET_BY_ID_LIMIT);
  }

  private void addAndDeleteMetacards(int metacardCount)
      throws IngestException, UnsupportedQueryException {
    deleteAll(provider);

    List<Metacard> metacards = new ArrayList<>();
    for (int i = 0; i < metacardCount; i++) {
      metacards.add(new MockMetacard(Library.getFlagstaffRecord()));
    }

    CreateResponse createResponse = create(metacards, provider);
    assertThat(createResponse.getCreatedMetacards().size(), is(metacards.size()));

    List<String> ids = new ArrayList<>();
    for (Metacard mc : createResponse.getCreatedMetacards()) {
      ids.add(mc.getId());
    }

    DeleteResponse deleteResponse = delete(ids.toArray(new String[metacardCount]), provider);
    List<Metacard> deletedMetacards = deleteResponse.getDeletedMetacards();
    assertThat(deletedMetacards.size(), is(metacards.size()));

    for (int i = 0; i < metacardCount; i++) {
      assertThat(deletedMetacards.get(i).getId(), isIn(ids));
    }
  }

  /** Tests what happens when the whole request is null. */
  @Test(expected = IngestException.class)
  public void testDeleteNull() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    provider.delete(null);

    fail();
  }

  /** Tests the provider will allow you to delete nothing. */
  @Test
  public void testDeleteNothing() throws IngestException, UnsupportedQueryException {

    // Single Deletion

    deleteAll(provider);

    DeleteResponse deleteResponse = delete("no_such_record", provider);

    assertThat(deleteResponse.getDeletedMetacards().size(), equalTo(0));
  }

  /** Testing if another attribute can be used to delete records other than {@link Core#ID} */
  @Test
  public void testDeleteAlternativeAttribute() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    CreateResponse createResponse = create(metacard, provider);

    DeleteResponse deleteResponse =
        provider.delete(
            new DeleteRequest() {

              @Override
              public boolean hasProperties() {
                return false;
              }

              @Override
              public Serializable getPropertyValue(String name) {
                return null;
              }

              @Override
              public Set<String> getPropertyNames() {
                return null;
              }

              @Override
              public Map<String, Serializable> getProperties() {
                return null;
              }

              @Override
              public boolean containsPropertyName(String name) {
                return false;
              }

              @Override
              public List<? extends Serializable> getAttributeValues() {
                return Collections.singletonList(MockMetacard.DEFAULT_TITLE);
              }

              @Override
              public String getAttributeName() {
                return Core.TITLE;
              }
            });

    Metacard deletedMetacard = deleteResponse.getDeletedMetacards().get(0);

    verifyDeletedRecord(metacard, createResponse, deleteResponse, deletedMetacard);

    // verify it is really not in SOLR

    Filter filter =
        getFilterBuilder().attribute(Core.TITLE).like().text(MockMetacard.DEFAULT_TITLE);

    QueryImpl query = new QueryImpl(filter);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    List<Result> results = sourceResponse.getResults();
    assertEquals(0, results.size());
  }

  @Test
  public void testDeleteNoList() throws IngestException {

    /* EMPTY */
    DeleteRequestImpl deleteRequest = new DeleteRequestImpl(new String[0]);

    DeleteResponse results = provider.delete(deleteRequest);

    assertNotNull(results.getDeletedMetacards());

    assertEquals(0, results.getDeletedMetacards().size());

    assertEquals(deleteRequest, results.getRequest());

    /* EMPTY */
    DeleteRequestImpl emptyDeleteRequest =
        new DeleteRequestImpl(new ArrayList<>(), DeleteRequest.DELETE_BY_ID, null);

    results = provider.delete(emptyDeleteRequest);

    assertNotNull(results.getDeletedMetacards());

    assertEquals(0, results.getDeletedMetacards().size());

    assertEquals(emptyDeleteRequest, results.getRequest());

    /* NULL */
    DeleteRequest nullDeleteRequest = new DeleteRequestImpl(null, DeleteRequest.DELETE_BY_ID, null);

    results = provider.delete(nullDeleteRequest);

    assertNotNull(results.getDeletedMetacards());

    assertEquals(0, results.getDeletedMetacards().size());

    assertEquals(nullDeleteRequest, results.getRequest());
  }

  @Test
  public void testDeletePendingNrtIndex() throws Exception {
    deleteAll(provider);
    ConfigurationStore.getInstance().setForceAutoCommit(false);

    try {
      MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

      CreateResponse createResponse = create(metacard, provider);

      DeleteResponse deleteResponse =
          delete(createResponse.getCreatedMetacards().get(0).getId(), provider);

      Metacard deletedMetacard = deleteResponse.getDeletedMetacards().get(0);

      verifyDeletedRecord(metacard, createResponse, deleteResponse, deletedMetacard);
    } finally {
      ConfigurationStore.getInstance().setForceAutoCommit(true);
    }
  }

  private void verifyDeletedRecord(
      MockMetacard metacard,
      CreateResponse createResponse,
      DeleteResponse deleteResponse,
      Metacard deletedMetacard) {
    assertEquals(1, deleteResponse.getDeletedMetacards().size());
    assertEquals(createResponse.getCreatedMetacards().get(0).getId(), deletedMetacard.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, deletedMetacard.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, deletedMetacard.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, deletedMetacard.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, deletedMetacard.getContentTypeVersion());
    assertNotNull(deletedMetacard.getMetadata());
    assertTrue(!deletedMetacard.getMetadata().isEmpty());
    assertFalse(((Date) deletedMetacard.getAttribute(Core.CREATED).getValue()).after(new Date()));
    assertFalse(((Date) deletedMetacard.getAttribute(Core.MODIFIED).getValue()).after(new Date()));
    assertEquals(metacard.getEffectiveDate(), deletedMetacard.getEffectiveDate());
    assertEquals(metacard.getExpirationDate(), deletedMetacard.getExpirationDate());
    assertTrue(Arrays.equals(metacard.getThumbnail(), deletedMetacard.getThumbnail()));
    assertEquals(metacard.getLocation(), deletedMetacard.getLocation());
  }
}
