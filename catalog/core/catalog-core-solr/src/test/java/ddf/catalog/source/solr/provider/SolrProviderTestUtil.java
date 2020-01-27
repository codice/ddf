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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.BaseSolrCatalogProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderTestUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderTestUtil.class);

  public static final int ONE_HIT = 1;

  public static final int ALL_RESULTS = -1;

  public static final String DEFAULT_TEST_ESCAPE = "\\";

  public static final String DEFAULT_TEST_SINGLE_WILDCARD = "?";

  public static final String DEFAULT_TEST_WILDCARD = "*";

  private static SolrFilterBuilder filterBuilder = new SolrFilterBuilder();

  private static final int TEST_METHOD_NAME_INDEX = 3;

  private static final int ONE_SECOND = 1;

  private static final int TIME_STEP_10SECONDS = 10 * ONE_SECOND;

  private static final int A_LITTLE_WHILE = TIME_STEP_10SECONDS;

  public static void deleteAll(BaseSolrCatalogProvider provider)
      throws IngestException, UnsupportedQueryException {
    deleteAll(TEST_METHOD_NAME_INDEX, provider);
  }

  public static void deleteAll(int methodNameIndex, BaseSolrCatalogProvider provider)
      throws IngestException, UnsupportedQueryException {
    messageBreak(Thread.currentThread().getStackTrace()[methodNameIndex].getMethodName() + "()");

    QueryImpl query;
    SourceResponse sourceResponse;
    query = new QueryImpl(filterBuilder.attribute(Metacard.ID).is().like().text("*"));
    query.setPageSize(ALL_RESULTS);
    sourceResponse = provider.query(new QueryRequestImpl(query));

    List<String> ids = new ArrayList<>();
    for (Result r : sourceResponse.getResults()) {
      ids.add(r.getMetacard().getId());
    }

    LOGGER.info("Records found for deletion: {}", ids);

    provider.delete(new DeleteRequestImpl(ids.toArray(new String[0])));

    LOGGER.info("Deletion complete. -----------");
  }

  public static QueryRequest quickQuery(Filter filter) {
    return new QueryRequestImpl(new QueryImpl(filter));
  }

  public static void queryAndVerifyCount(int count, Filter filter, BaseSolrCatalogProvider provider)
      throws UnsupportedQueryException {
    Query query = new QueryImpl(filter);
    QueryRequest request = new QueryRequestImpl(query);
    SourceResponse response = provider.query(request);

    assertEquals(count, response.getResults().size());
  }

  public static DeleteResponse delete(String identifier, BaseSolrCatalogProvider provider)
      throws IngestException {
    return delete(new String[] {identifier}, provider);
  }

  public static DeleteResponse delete(String[] identifier, BaseSolrCatalogProvider provider)
      throws IngestException {
    return provider.delete(new DeleteRequestImpl(identifier));
  }

  public static UpdateResponse update(
      String id, Metacard metacard, BaseSolrCatalogProvider provider) throws IngestException {
    String[] ids = {id};
    return update(ids, Collections.singletonList(metacard), provider);
  }

  public static UpdateResponse update(
      String[] ids, List<Metacard> list, BaseSolrCatalogProvider provider) throws IngestException {
    return provider.update(new UpdateRequestImpl(ids, list));
  }

  public static CreateResponse create(Metacard metacard, BaseSolrCatalogProvider provider)
      throws IngestException {
    return create(Collections.singletonList(metacard), provider);
  }

  public static CreateResponse create(List<Metacard> metacards, BaseSolrCatalogProvider provider)
      throws IngestException {
    return createIn(metacards, provider);
  }

  public static SolrFilterBuilder getFilterBuilder() {
    return filterBuilder;
  }

  public static void addMetacardWithModifiedDate(DateTime now, BaseSolrCatalogProvider provider)
      throws IngestException {
    List<Metacard> list = new ArrayList<>();
    MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());
    m.setEffectiveDate(dateNow(now));
    list.add(m);
    create(list, provider);
  }

  public static List<Result> getResultsForFilteredQuery(
      Filter filter, BaseSolrCatalogProvider provider) throws UnsupportedQueryException {
    QueryImpl query = new QueryImpl(filter);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));
    return sourceResponse.getResults();
  }

  public static Date dateAfterNow(DateTime now) {
    return now.plusSeconds(A_LITTLE_WHILE).toDate();
  }

  public static Date dateBeforeNow(DateTime now) {
    return now.minusSeconds(A_LITTLE_WHILE).toDate();
  }

  public static Date dateNow(DateTime now) {
    return now.toDate();
  }

  public static Set<AttributeDescriptor> numericalDescriptors(
      String doubleField, String floatField, String intField, String longField, String shortField) {
    Set<AttributeDescriptor> descriptors = new HashSet<>();
    descriptors.add(
        new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(doubleField, true, true, true, false, BasicTypes.DOUBLE_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(floatField, true, true, true, false, BasicTypes.FLOAT_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(intField, true, true, true, false, BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(longField, true, true, true, false, BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(shortField, true, true, true, false, BasicTypes.SHORT_TYPE));
    return descriptors;
  }

  private static CreateResponse createIn(
      List<Metacard> metacards, BaseSolrCatalogProvider solrProvider) throws IngestException {
    return solrProvider.create(new CreateRequestImpl(metacards));
  }

  private static void messageBreak(String string) {
    String stars = StringUtils.repeat("*", string.length() + 2);
    LOGGER.info(stars);
    LOGGER.info("* {}", string);
    LOGGER.info(stars);
  }
}
