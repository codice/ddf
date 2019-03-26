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

import static ddf.catalog.source.solr.SolrProviderTest.MASKED_ID;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.DEFAULT_TEST_ESCAPE;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.DEFAULT_TEST_SINGLE_WILDCARD;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.DEFAULT_TEST_WILDCARD;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Validation;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrProviderTest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.geotools.filter.FilterFactoryImpl;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class SolrProviderCreate {

  private static SolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();
  }

  @Test
  public void testCreatedDates() throws Exception {

    deleteAll(provider);

    /* ALL NULL */
    MockMetacard mockMetacard = new MockMetacard(Library.getFlagstaffRecord());
    mockMetacard.setEffectiveDate(null);
    mockMetacard.setExpirationDate(null);
    mockMetacard.setCreatedDate(null);
    mockMetacard.setModifiedDate(null);
    List<Metacard> list = Collections.singletonList(mockMetacard);

    CreateResponse createResponse = create(list, provider);

    assertEquals(1, createResponse.getCreatedMetacards().size());

    Metacard createdMetacard = createResponse.getCreatedMetacards().get(0);

    assertNotNull(createdMetacard.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, createdMetacard.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, createdMetacard.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, createdMetacard.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, createdMetacard.getContentTypeVersion());
    assertNotNull(createdMetacard.getMetadata());
    assertTrue(!createdMetacard.getMetadata().isEmpty());

    // DATES
    assertEquals(mockMetacard.getCreatedDate(), createdMetacard.getCreatedDate());
    assertThat(createdMetacard.getCreatedDate(), nullValue());

    assertEquals(mockMetacard.getModifiedDate(), createdMetacard.getModifiedDate());
    assertThat(createdMetacard.getModifiedDate(), nullValue());

    assertEquals(mockMetacard.getEffectiveDate(), createdMetacard.getEffectiveDate());
    assertThat(createdMetacard.getEffectiveDate(), nullValue());

    assertEquals(mockMetacard.getExpirationDate(), createdMetacard.getExpirationDate());
    assertThat(createdMetacard.getExpirationDate(), nullValue());

    assertTrue(Arrays.equals(mockMetacard.getThumbnail(), createdMetacard.getThumbnail()));
    assertEquals(mockMetacard.getLocation(), createdMetacard.getLocation());
    assertEquals(MASKED_ID, createdMetacard.getSourceId());
  }

  /** Tests that multivalued attributes are stored and returned */
  @Test
  public void testCreateMultivaluedAttribute() throws UnsupportedQueryException, IngestException {

    deleteAll(provider);

    FilterFactory filterFactory = new FilterFactoryImpl();

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    List<Serializable> a = new ArrayList<>();
    a.add("sample-validator");
    a.add("sample-validator2");
    AttributeImpl attribute = new AttributeImpl(Validation.VALIDATION_WARNINGS, a);
    metacard.setAttribute(attribute);
    create(metacard, provider);

    Filter filter =
        filterFactory.like(
            filterFactory.property(Core.TITLE),
            MockMetacard.DEFAULT_TITLE,
            DEFAULT_TEST_WILDCARD,
            DEFAULT_TEST_SINGLE_WILDCARD,
            DEFAULT_TEST_ESCAPE,
            false);

    QueryImpl query = new QueryImpl(filter);

    query.setStartIndex(1);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    List<Result> results = sourceResponse.getResults();
    Metacard mResult = results.get(0).getMetacard();
    assertThat(mResult.getAttribute(Validation.VALIDATION_WARNINGS).getValues().size(), is(2));
  }

  /** Testing that you cannot instantiate with a null Solr client. */
  @Test(expected = IllegalArgumentException.class)
  public void testSolrClientNull() {
    new SolrCatalogProvider(null, null, null);
  }

  /** Tests what happens when the whole request is null. */
  @Test(expected = IngestException.class)
  public void testCreateNull() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    provider.create(null);

    fail();
  }

  @Test
  public void testCreateNullList() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    CreateResponse response =
        provider.create(
            new CreateRequest() {

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
              public List<Metacard> getMetacards() {
                return null;
              }
            });

    assertThat(response.getCreatedMetacards().size(), is(0));
  }

  /**
   * Testing that if we create a record, it is truly ingested and we can retrieve all the fields we
   * intend to be retrievable.
   */
  @Test
  public void testCreateOperation() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    create(metacard, provider);

    FilterFactory filterFactory = new FilterFactoryImpl();

    // SIMPLE TITLE SEARCH
    Filter filter =
        filterFactory.like(
            filterFactory.property(Core.TITLE),
            MockMetacard.DEFAULT_TITLE,
            DEFAULT_TEST_WILDCARD,
            DEFAULT_TEST_SINGLE_WILDCARD,
            DEFAULT_TEST_ESCAPE,
            false);

    QueryImpl query = new QueryImpl(filter);

    query.setStartIndex(1);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    List<Result> results = sourceResponse.getResults();
    Metacard mResult = results.get(0).getMetacard();
    assertEquals(1, results.size());
    assertNotNull(mResult.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
    assertNotNull(mResult.getMetadata());
    assertThat(
        mResult.getMetadata(), containsString("<title>Flagstaff Chamber of Commerce</title>"));
    assertTrue(!mResult.getMetadata().isEmpty());
    assertFalse(mResult.getCreatedDate().after(new Date()));
    assertFalse(mResult.getModifiedDate().after(new Date()));
    assertEquals(metacard.getEffectiveDate(), mResult.getEffectiveDate());
    assertEquals(metacard.getExpirationDate(), mResult.getExpirationDate());
    assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
    assertEquals(metacard.getLocation(), mResult.getLocation());
    assertEquals(MASKED_ID, mResult.getSourceId());

    // --- Simple KEYWORD SEARCH
    filter =
        filterFactory.like(
            filterFactory.property(Core.METADATA),
            MockMetacard.DEFAULT_TITLE,
            DEFAULT_TEST_WILDCARD,
            DEFAULT_TEST_SINGLE_WILDCARD,
            DEFAULT_TEST_ESCAPE,
            false);

    query = new QueryImpl(filter);

    query.setStartIndex(1);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    results = sourceResponse.getResults();
    mResult = results.get(0).getMetacard();
    assertEquals(1, results.size());
    assertNotNull(mResult.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
    assertNotNull(mResult.getMetadata());
    assertTrue(!mResult.getMetadata().isEmpty());
    assertFalse(mResult.getCreatedDate().after(new Date()));
    assertFalse(mResult.getModifiedDate().after(new Date()));
    assertEquals(metacard.getEffectiveDate(), mResult.getEffectiveDate());
    assertEquals(metacard.getExpirationDate(), mResult.getExpirationDate());
    assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
    assertEquals(metacard.getLocation(), mResult.getLocation());
    assertEquals(MASKED_ID, mResult.getSourceId());
  }

  @Test(expected = IngestException.class)
  public void testCreateOperationWithSourceIdNoId()
      throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    metacard.setSourceId("ddfChild");

    Date oneDayAgo = new DateTime().minusDays(1).toDate();
    metacard.setCreatedDate(oneDayAgo);
    metacard.setExpirationDate(oneDayAgo);
    metacard.setEffectiveDate(oneDayAgo);
    metacard.setModifiedDate(oneDayAgo);

    create(metacard, provider);
  }

  @Test
  public void testCreateOperationWithSourceId() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

    String id = UUID.randomUUID().toString();
    metacard.setId(id);
    metacard.setSourceId("ddfChild");

    Date oneDayAgo = new DateTime().minusDays(1).toDate();
    metacard.setCreatedDate(oneDayAgo);
    metacard.setExpirationDate(oneDayAgo);
    metacard.setEffectiveDate(oneDayAgo);
    metacard.setModifiedDate(oneDayAgo);

    CreateResponse createResponse = create(metacard, provider);

    Metacard createdMetacard = createResponse.getCreatedMetacards().get(0);

    assertNotNull(createdMetacard.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, createdMetacard.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, createdMetacard.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, createdMetacard.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, createdMetacard.getContentTypeVersion());
    assertNotNull(createdMetacard.getMetadata());
    assertThat(
        createdMetacard.getMetadata(),
        containsString("<title>Flagstaff Chamber of Commerce</title>"));
    assertThat(createdMetacard.getMetadata().isEmpty(), is(not(true)));
    assertThat(createdMetacard.getCreatedDate(), is(oneDayAgo));
    assertThat(createdMetacard.getModifiedDate(), is(oneDayAgo));
    assertThat(createdMetacard.getEffectiveDate(), is(oneDayAgo));
    assertThat(createdMetacard.getExpirationDate(), is(oneDayAgo));
    assertTrue(Arrays.equals(metacard.getThumbnail(), createdMetacard.getThumbnail()));
    assertEquals(metacard.getLocation(), createdMetacard.getLocation());
    assertThat(createdMetacard.getSourceId(), is(metacard.getSourceId()));

    // --------------------

    FilterFactory filterFactory = new FilterFactoryImpl();

    // SIMPLE TITLE SEARCH
    Filter filter =
        filterFactory.like(
            filterFactory.property(Core.TITLE),
            MockMetacard.DEFAULT_TITLE,
            DEFAULT_TEST_WILDCARD,
            DEFAULT_TEST_SINGLE_WILDCARD,
            DEFAULT_TEST_ESCAPE,
            false);

    QueryImpl query = new QueryImpl(filter);

    query.setStartIndex(1);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    List<Result> results = sourceResponse.getResults();
    Metacard mResult = results.get(0).getMetacard();
    assertEquals(1, results.size());
    assertNotNull(mResult.getId());
    assertEquals(MockMetacard.DEFAULT_TITLE, mResult.getTitle());
    assertEquals(MockMetacard.DEFAULT_LOCATION, mResult.getLocation());
    assertEquals(MockMetacard.DEFAULT_TYPE, mResult.getContentTypeName());
    assertEquals(MockMetacard.DEFAULT_VERSION, mResult.getContentTypeVersion());
    assertNotNull(mResult.getMetadata());
    assertThat(
        mResult.getMetadata(), containsString("<title>Flagstaff Chamber of Commerce</title>"));
    assertThat(mResult.getMetadata().isEmpty(), is(not(true)));
    assertThat(mResult.getCreatedDate(), is(oneDayAgo));
    assertThat(mResult.getModifiedDate(), is(oneDayAgo));
    assertThat(mResult.getEffectiveDate(), is(oneDayAgo));
    assertThat(mResult.getExpirationDate(), is(oneDayAgo));
    assertTrue(Arrays.equals(metacard.getThumbnail(), mResult.getThumbnail()));
    assertEquals(metacard.getLocation(), mResult.getLocation());
    // assertThat(mResult.getSourceId(), is("ddf"));

  }

  @Test
  public void testCreatePendingNrtIndex() throws Exception {
    deleteAll(provider);
    ConfigurationStore.getInstance().setForceAutoCommit(false);

    try {
      MockMetacard metacard = new MockMetacard(Library.getFlagstaffRecord());

      CreateResponse response = create(metacard, provider);

      String createdId = response.getCreatedMetacards().get(0).getId();

      Filter titleFilter =
          getFilterBuilder().attribute(Core.TITLE).like().text(MockMetacard.DEFAULT_TITLE);

      Filter idFilter = getFilterBuilder().attribute(Core.ID).equalTo().text(createdId);

      SourceResponse titleResponse =
          provider.query(new QueryRequestImpl(new QueryImpl(titleFilter)));

      SourceResponse idResponse = provider.query(new QueryRequestImpl(new QueryImpl(idFilter)));

      assertThat(titleResponse.getResults().size(), is(0));
      assertThat(idResponse.getResults().size(), is(1));
    } finally {
      ConfigurationStore.getInstance().setForceAutoCommit(true);
    }
  }
}
