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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.temporal.object.DefaultPeriodDuration;
import org.joda.time.DateTime;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderTemporal extends SolrProviderTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderTemporal.class);

  private static final long MINUTES_IN_MILLISECONDS = 60000;

  @Test
  public void testTemporalDuring() throws Exception {

    deleteAll();

    Metacard metacard = new MockMetacard(Library.getFlagstaffRecord());
    List<Metacard> list = Collections.singletonList(metacard);

    // CREATE
    create(list);

    // TEMPORAL QUERY - DURING FILTER (Period) - AKA ABSOLUTE
    FilterFactory filterFactory = new FilterFactoryImpl();

    int minutes = 3;

    DateTime startDT = new DateTime().plusMinutes(ALL_RESULTS * minutes);

    DateTime endDT = new DateTime();

    QueryImpl query =
        new QueryImpl(
            filterBuilder
                .attribute(Metacard.MODIFIED)
                .is()
                .during()
                .dates(startDT.toDate(), endDT.toDate()));
    query.setStartIndex(1);
    query.setRequestsTotalResultsCount(true);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(1, sourceResponse.getResults().size());

    assertContainsTerm(sourceResponse);

    // TEMPORAL QUERY - DURING FILTER (Duration) - AKA RELATIVE
    DefaultPeriodDuration duration = new DefaultPeriodDuration(minutes * MINUTES_IN_MILLISECONDS);

    Filter filter =
        filterFactory.during(
            filterFactory.property(Metacard.MODIFIED), filterFactory.literal(duration));

    query = new QueryImpl(filter);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(1, sourceResponse.getResults().size());

    assertContainsTerm(sourceResponse);

    provider.isAvailable();
  }

  private void assertContainsTerm(SourceResponse sourceResponse) {
    for (Result content : sourceResponse.getResults()) {
      String term = Library.FLAGSTAFF_QUERY_PHRASE;

      LOGGER.debug("RESULT returned: {}", content);
      String metadata = content.getMetacard().getMetadata();
      assertTrue("Testing if contents has term [" + term + "]", metadata.contains(term));
    }
  }

  @Test()
  public void testTemporalBefore() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.attribute(Metacard.MODIFIED).before().date(dateAfterNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.attribute(Metacard.MODIFIED).before().date(dateBeforeNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testTemporalAfter() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.attribute(Metacard.MODIFIED).after().date(dateBeforeNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.attribute(Metacard.MODIFIED).after().date(dateAfterNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testDateGreaterThan() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.dateGreaterThan(Metacard.MODIFIED, dateBeforeNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.dateGreaterThan(Metacard.MODIFIED, dateAfterNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testDateGreaterThanOrEqualTo() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.dateGreaterThanOrEqual(Metacard.MODIFIED, dateBeforeNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.dateGreaterThanOrEqual(Metacard.MODIFIED, dateAfterNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testDateLessThan() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.dateLessThan(Metacard.MODIFIED, dateAfterNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.dateLessThan(Metacard.MODIFIED, dateNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testDateLessThanOrEqualTo() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter = filterBuilder.dateLessThanOrEqual(Metacard.MODIFIED, dateAfterNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASE
    filter = filterBuilder.dateLessThanOrEqual(Metacard.MODIFIED, dateBeforeNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  @Test()
  public void testDateDuring() throws Exception {

    deleteAll();

    DateTime now = new DateTime();
    addMetacardWithModifiedDate(now);

    // POSITIVE CASE
    Filter filter =
        filterBuilder.dateIsDuring(Metacard.MODIFIED, dateBeforeNow(now), dateAfterNow(now));
    List<Result> results = getResultsForFilteredQuery(filter);
    assertEquals(1, results.size());

    // NEGATIVE CASES
    filter =
        filterBuilder.dateIsDuring(
            Metacard.MODIFIED, getCannedTime(1980, Calendar.JANUARY, 1, 3), dateBeforeNow(now));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());

    filter =
        filterBuilder.dateIsDuring(
            Metacard.MODIFIED, dateAfterNow(now), getCannedTime(2035, Calendar.JULY, 23, 46));
    results = getResultsForFilteredQuery(filter);
    assertEquals(0, results.size());
  }

  private Date getCannedTime(int year, int month, int day, int hour) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.clear();
    calendar.set(year, month, day, hour, 59, 56);
    calendar.set(Calendar.MILLISECOND, 765);
    return calendar.getTime();
  }
}
