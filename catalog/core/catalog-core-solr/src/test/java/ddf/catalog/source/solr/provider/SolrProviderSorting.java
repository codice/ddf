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

import static com.google.common.truth.Truth.assertThat;
import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.numericalDescriptors;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrProviderTest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.SortByImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderSorting {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderSorting.class);

  private static final String DOUBLE_FIELD = "hertz";
  private static final double DOUBLE_FIELD_VALUE = 16065.435;

  private static final String FLOAT_FIELD = "inches";
  private static final float FLOAT_FIELD_VALUE = 4.435f;

  private static final String INT_FIELD = "count";
  private static final int INT_FIELD_VALUE = 4;

  private static final String LONG_FIELD = "milliseconds";
  private static final long LONG_FIELD_VALUE = 9876543293L;

  private static final String SHORT_FIELD = "daysOfTheWeek";
  private static final short SHORT_FIELD_VALUE = 1;

  private static final int FACTOR = 5;
  private static final int NUMBERIC_METACARD_COUNT = 5;

  private static SolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();
  }

  /** Test for a specific IRAD problem. */
  @Test
  public void testSortById() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    DateTime now = new DateTime();

    generateMockMetacards(list, now);

    create(list, provider);

    Filter filter =
        getFilterBuilder().attribute(Metacard.EFFECTIVE).before().date(now.plusMillis(1).toDate());

    QueryImpl query = new QueryImpl(filter);

    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(Core.ID, SortOrder.ASCENDING.name()));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    String currentId = "";

    for (Result r : sourceResponse.getResults()) {

      assertTrue(currentId.compareTo(r.getMetacard().getId()) < 0);
      currentId = r.getMetacard().getId();
    }
  }

  private void generateMockMetacards(List<Metacard> list, DateTime now) {
    for (int i = 0; i < 5; i++) {

      MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());

      m.setEffectiveDate(now.minus(5L * i).toDate());

      m.setTitle("Record " + i);

      list.add(m);
    }
  }

  @Test
  public void testTextualSort() throws Exception {
    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    DateTime now = new DateTime();

    for (int i = 65; i < 65 + 5; i++) {

      MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());

      m.setEffectiveDate(now.minus(5L * i).toDate());

      m.setTitle((char) i + " Record ");

      list.add(m);
    }

    create(list, provider);

    Filter filter;
    QueryImpl query;
    SourceResponse sourceResponse;

    // Sort all Textual ASCENDING

    filter =
        getFilterBuilder().attribute(Metacard.EFFECTIVE).before().date(now.plusMillis(1).toDate());

    query = new QueryImpl(filter);

    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(Core.TITLE, SortOrder.ASCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    int ascii = 65;
    for (int i = 0; i < list.size(); i++) {
      Result r = sourceResponse.getResults().get(i);
      assertEquals((char) (i + ascii) + " Record ", r.getMetacard().getTitle());
    }

    // Sort all Textual DESCENDING

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Core.TITLE, SortOrder.DESCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    int asciiE = 69;
    for (int i = (list.size() - 1); i >= 0; i--) {
      Result r = sourceResponse.getResults().get(i);
      assertEquals((char) (asciiE - i) + " Record ", r.getMetacard().getTitle());
    }
  }

  private void setupNumericMetacards() throws UnsupportedQueryException, IngestException {
    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    for (int i = 0; i < NUMBERIC_METACARD_COUNT; i++) {
      Set<AttributeDescriptor> descriptors =
          numericalDescriptors(DOUBLE_FIELD, FLOAT_FIELD, INT_FIELD, LONG_FIELD, SHORT_FIELD);

      MetacardTypeImpl mType = new MetacardTypeImpl("numberMetacardType", descriptors);

      MetacardImpl customMetacard1 = new MetacardImpl(mType);
      customMetacard1.setAttribute(Core.ID, "");
      customMetacard1.setAttribute(DOUBLE_FIELD, DOUBLE_FIELD_VALUE + FACTOR * i);
      customMetacard1.setAttribute(FLOAT_FIELD, FLOAT_FIELD_VALUE + FACTOR * i);
      customMetacard1.setAttribute(INT_FIELD, INT_FIELD_VALUE + FACTOR * i);
      customMetacard1.setAttribute(LONG_FIELD, LONG_FIELD_VALUE + FACTOR * i);
      customMetacard1.setAttribute(SHORT_FIELD, SHORT_FIELD_VALUE + FACTOR * i);

      list.add(customMetacard1);
    }

    create(list, provider);
  }

  @Test
  public void testNumericSortDoubleAscending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        Comparator.comparingDouble(
            result -> (double) result.getMetacard().getAttribute(DOUBLE_FIELD).getValue());
    verifyNumericSort(DOUBLE_FIELD, comparator, true);
  }

  @Test
  public void testNumericSortDoubleDescending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Double.compare(
                (double) rhs.getMetacard().getAttribute(DOUBLE_FIELD).getValue(),
                (double) lhs.getMetacard().getAttribute(DOUBLE_FIELD).getValue());
    verifyNumericSort(DOUBLE_FIELD, comparator, false);
  }

  @Test
  public void testNumericSortIntegerAscending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        Comparator.comparingInt(
            result -> (int) result.getMetacard().getAttribute(INT_FIELD).getValue());
    verifyNumericSort(INT_FIELD, comparator, true);
  }

  @Test
  public void testNumericSortIntegerDescending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Integer.compare(
                (int) rhs.getMetacard().getAttribute(INT_FIELD).getValue(),
                (int) lhs.getMetacard().getAttribute(INT_FIELD).getValue());
    verifyNumericSort(INT_FIELD, comparator, false);
  }

  @Test
  public void testNumericSortLongAscending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        Comparator.comparingLong(
            result -> (long) result.getMetacard().getAttribute(LONG_FIELD).getValue());
    verifyNumericSort(LONG_FIELD, comparator, true);
  }

  @Test
  public void testNumericSortLongDescending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Long.compare(
                (long) rhs.getMetacard().getAttribute(LONG_FIELD).getValue(),
                (long) lhs.getMetacard().getAttribute(LONG_FIELD).getValue());
    verifyNumericSort(LONG_FIELD, comparator, false);
  }

  @Test
  public void testNumericSortFloatAscending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Float.compare(
                (float) lhs.getMetacard().getAttribute(FLOAT_FIELD).getValue(),
                (float) rhs.getMetacard().getAttribute(FLOAT_FIELD).getValue());
    verifyNumericSort(FLOAT_FIELD, comparator, true);
  }

  @Test
  public void testNumericSortFloatDescending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Float.compare(
                (float) rhs.getMetacard().getAttribute(FLOAT_FIELD).getValue(),
                (float) lhs.getMetacard().getAttribute(FLOAT_FIELD).getValue());
    verifyNumericSort(FLOAT_FIELD, comparator, false);
  }

  @Test
  public void testNumericSortShortAscending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        Comparator.comparingInt(
            result -> (short) result.getMetacard().getAttribute(SHORT_FIELD).getValue());
    verifyNumericSort(SHORT_FIELD, comparator, true);
  }

  @Test
  public void testNumericSortShortDescending() throws UnsupportedQueryException, IngestException {
    Comparator<Result> comparator =
        (lhs, rhs) ->
            Short.compare(
                (short) rhs.getMetacard().getAttribute(SHORT_FIELD).getValue(),
                (short) lhs.getMetacard().getAttribute(SHORT_FIELD).getValue());
    verifyNumericSort(SHORT_FIELD, comparator, false);
  }

  private void verifyNumericSort(String attributeName, Comparator comparator, boolean isAscending)
      throws UnsupportedQueryException, IngestException {
    setupNumericMetacards();

    Filter filter = getFilterBuilder().attribute(Metacard.ANY_TEXT).like().text("*");
    QueryImpl query = new QueryImpl(filter);
    SourceResponse sourceResponse;

    String sortOrder = SortOrder.ASCENDING.name();
    if (!isAscending) {
      sortOrder = SortOrder.DESCENDING.name();
    }

    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(attributeName, sortOrder));
    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertThat(sourceResponse.getResults().size()).isEqualTo(NUMBERIC_METACARD_COUNT);
    assertThat(sourceResponse.getResults()).isOrdered(comparator);
  }

  @Test
  public void testSorting() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    DateTime now = new DateTime();

    generateMockMetacards(list, now);

    create(list, provider);

    Filter filter;
    QueryImpl query;
    SourceResponse sourceResponse;

    // Sort all TEMPORAL DESC

    filter =
        getFilterBuilder().attribute(Metacard.EFFECTIVE).before().date(now.plusMillis(1).toDate());

    query = new QueryImpl(filter);

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Metacard.EFFECTIVE, SortOrder.DESCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    for (int i = 0; i < list.size(); i++) {
      Result r = sourceResponse.getResults().get(i);
      assertEquals("Record " + i, r.getMetacard().getTitle());
    }

    // Sort all TEMPORAL ASC

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Metacard.EFFECTIVE, SortOrder.ASCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    int index = 0;
    for (int i = (list.size() - 1); i >= 0; i--) {
      Result r = sourceResponse.getResults().get(index);
      assertEquals("Record " + i, r.getMetacard().getTitle());
      index++;
    }

    // Sort all Relevancy score DESC

    filter =
        getFilterBuilder().attribute(Core.METADATA).like().text(Library.FLAGSTAFF_QUERY_PHRASE);

    query = new QueryImpl(filter);

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    double currentScore = Integer.MAX_VALUE;
    for (Result r : sourceResponse.getResults()) {
      assertThat(currentScore).isAtLeast(r.getRelevanceScore());
      currentScore = r.getRelevanceScore();
    }

    // Sort all Relevancy score DESC

    filter =
        getFilterBuilder().attribute(Core.METADATA).like().text(Library.FLAGSTAFF_QUERY_PHRASE);

    query = new QueryImpl(filter);

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Result.RELEVANCE, SortOrder.ASCENDING.name()));

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(list.size(), sourceResponse.getResults().size());

    currentScore = 0;
    for (Result r : sourceResponse.getResults()) {
      assertThat(currentScore).isAtMost(r.getRelevanceScore());
      currentScore = r.getRelevanceScore();
    }
  }

  @Test
  public void testSortingMultipleAttributes() throws Exception {
    deleteAll(provider);
    List<Metacard> list = new ArrayList<>();
    DateTime now = new DateTime();

    for (int i = 0; i < 5; i++) {
      MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());
      m.setEffectiveDate(now.minus(5L * i).toDate());
      m.setTitle("Record " + i);
      m.setLocation("POINT(0 0)");
      list.add(m);
    }

    create(list, provider);

    Filter filter;
    QueryImpl query;
    SourceResponse sourceResponse;

    filter =
        getFilterBuilder()
            .allOf(
                getFilterBuilder().attribute(Metacard.ANY_GEO).nearestTo().wkt("POINT(1 1)"),
                getFilterBuilder().attribute(Core.TITLE).like().text("Record"));

    query = new QueryImpl(filter);

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Result.DISTANCE, SortOrder.DESCENDING.name()));
    Map<String, Serializable> properties = new HashMap<>();
    SortBy relevanceSort =
        new ddf.catalog.filter.impl.SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING.name());
    SortBy titleSort =
        new ddf.catalog.filter.impl.SortByImpl(Core.TITLE, SortOrder.ASCENDING.name());
    SortBy[] additionalSorts = new SortBy[] {relevanceSort, titleSort};
    properties.put(ADDITIONAL_SORT_BYS, additionalSorts);
    sourceResponse = provider.query(new QueryRequestImpl(query, properties));

    assertEquals(list.size(), sourceResponse.getResults().size());

    int i = 0;
    for (Result r : sourceResponse.getResults()) {
      assertThat(r.getMetacard().getTitle()).isEqualTo("Record " + i);
      i++;
    }
  }

  @Test
  public void testSortingMultipleAttributesGeoAndRelevance() throws Exception {
    deleteAll(provider);
    List<Metacard> list = new ArrayList<>();
    DateTime now = new DateTime();

    for (int i = 0; i < 5; i++) {
      MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());
      m.setEffectiveDate(now.minus(5L * i).toDate());
      m.setTitle("Record " + i);
      m.setLocation("POINT(" + (6 - i) * -1 + " " + (6 - i) * -1 + ")");
      list.add(m);
    }

    create(list, provider);

    Filter filter;
    QueryImpl query;
    SourceResponse sourceResponse;

    filter =
        getFilterBuilder()
            .allOf(
                getFilterBuilder().attribute(Core.TITLE).like().text("Record"),
                getFilterBuilder().attribute(Metacard.ANY_GEO).nearestTo().wkt("POINT(1 1)"));

    query = new QueryImpl(filter);

    query.setSortBy(
        new ddf.catalog.filter.impl.SortByImpl(Result.RELEVANCE, SortOrder.DESCENDING.name()));
    Map<String, Serializable> properties = new HashMap<>();
    SortBy distanceSort =
        new ddf.catalog.filter.impl.SortByImpl(Result.DISTANCE, SortOrder.ASCENDING.name());
    SortBy[] additionalSorts = new SortBy[] {distanceSort};
    properties.put(ADDITIONAL_SORT_BYS, additionalSorts);
    sourceResponse = provider.query(new QueryRequestImpl(query, properties));

    assertEquals(list.size(), sourceResponse.getResults().size());

    int index = 0;
    for (int i = (list.size() - 1); i >= 0; i--) {
      Result r = sourceResponse.getResults().get(index);
      assertThat(r.getDistanceInMeters()).isNotEqualTo(r.getRelevanceScore());
      assertEquals("Record " + i, r.getMetacard().getTitle());
      index++;
    }
  }

  @Test
  public void testStartIndexWithSorting() throws Exception {
    deleteAll(provider);

    List<Metacard> metacards = new ArrayList<>();

    DateTime dt = new DateTime(1985, 1, 1, 1, 1, 1, 1, DateTimeZone.UTC);

    TreeSet<Date> calculatedDates = new TreeSet<>();

    for (int j = 0; j < 10; j++) {
      for (int i = 0; i < 100; i = i + 10) {

        MetacardImpl metacard = new MockMetacard(Library.getFlagstaffRecord());

        // ingest sporadically the effective dates so the default return
        // order won't be ordered
        Date calculatedDate = dt.plusDays(100 - i + 10 - j).toDate();
        calculatedDates.add(calculatedDate);
        metacard.setEffectiveDate(calculatedDate);
        metacards.add(metacard);
      }
    }

    // The TreeSet will sort them, the array will give me access to everyone
    // without an iterator
    Date[] dates = new Date[calculatedDates.size()];
    calculatedDates.toArray(dates);

    // CREATE
    CreateResponse response = create(metacards, provider);

    LOGGER.info("CREATED {} records.", response.getCreatedMetacards().size());

    FilterFactory filterFactory = new FilterFactoryImpl();

    // STARTINDEX=2, MAXSIZE=20
    int maxSize = 20;
    int startIndex = 2;
    SortByImpl sortBy =
        new SortByImpl(
            filterFactory.property(Metacard.EFFECTIVE),
            org.opengis.filter.sort.SortOrder.ASCENDING);
    QueryImpl query =
        query(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, startIndex, maxSize, sortBy);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(maxSize, sourceResponse.getResults().size());
    assertSorting(dates, startIndex, sourceResponse);

    // STARTINDEX=20, MAXSIZE=5
    // a match-all queryByProperty
    maxSize = 5;
    startIndex = 20;
    sortBy =
        new SortByImpl(
            filterFactory.property(Metacard.EFFECTIVE),
            org.opengis.filter.sort.SortOrder.ASCENDING);
    query = query(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, startIndex, maxSize, sortBy);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(maxSize, sourceResponse.getResults().size());
    assertSorting(dates, startIndex, sourceResponse);

    // STARTINDEX=80, MAXSIZE=20
    // a match-all queryByProperty
    maxSize = 20;
    startIndex = 80;
    sortBy =
        new SortByImpl(
            filterFactory.property(Metacard.EFFECTIVE),
            org.opengis.filter.sort.SortOrder.ASCENDING);
    query = query(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, startIndex, maxSize, sortBy);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(maxSize, sourceResponse.getResults().size());
    assertSorting(dates, startIndex, sourceResponse);

    // STARTINDEX=1, MAXSIZE=100
    // a match-all queryByProperty
    maxSize = 100;
    startIndex = 1;
    sortBy =
        new SortByImpl(
            filterFactory.property(Metacard.EFFECTIVE),
            org.opengis.filter.sort.SortOrder.ASCENDING);
    query = query(Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, startIndex, maxSize, sortBy);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(maxSize, sourceResponse.getResults().size());
    assertSorting(dates, startIndex, sourceResponse);
  }

  private QueryImpl query(
      String property, String value, int startIndex, int pageSize, SortBy sortBy) {
    return new QueryImpl(
        getFilterBuilder().attribute(property).is().equalTo().text(value),
        startIndex,
        pageSize,
        sortBy,
        true,
        0);
  }

  private void assertSorting(Date[] dates, int startIndex, SourceResponse sourceResponse) {
    for (int i = 0; i < sourceResponse.getResults().size(); i++) {

      Result r = sourceResponse.getResults().get(i);

      Date effectiveDate = r.getMetacard().getEffectiveDate();

      DateTime currentDate = new DateTime(effectiveDate.getTime());

      LOGGER.debug("Testing current index: {}", startIndex + i);

      assertEquals(
          new DateTime(dates[startIndex - 1 + i].getTime()).getDayOfYear(),
          currentDate.getDayOfYear());
    }
  }
}
