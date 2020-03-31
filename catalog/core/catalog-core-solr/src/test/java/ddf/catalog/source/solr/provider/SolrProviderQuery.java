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

import static ddf.catalog.Constants.EXPERIMENTAL_FACET_RESULTS_KEY;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.ALL_RESULTS;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.ONE_HIT;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.numericalDescriptors;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.queryAndVerifyCount;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.quickQuery;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.impl.filter.GeoToolsFunctionFactory;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.FacetAttributeResult;
import ddf.catalog.operation.FacetValueCount;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.FacetedQueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrMetacardClientImpl;
import ddf.catalog.source.solr.SolrProviderTest;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.FunctionFactory;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrProviderQuery {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrProviderQuery.class);

  private static final String DEFAULT_TEST_ESCAPE = "\\";

  private static final String DEFAULT_TEST_SINGLE_WILDCARD = "?";

  private static final String DEFAULT_TEST_WILDCARD = "*";

  private static SolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = SolrProviderTest.getProvider();

    GeoTools.addFactoryIteratorProvider(
        new FactoryIteratorProvider() {
          @Override
          public <T> Iterator<T> iterator(Class<T> category) {
            if (FunctionFactory.class == category) {
              List<FunctionFactory> l = new LinkedList<>();
              l.add(new GeoToolsFunctionFactory());
              return (Iterator<T>) l.iterator();
            }
            return null;
          }
        });
    CommonFactoryFinder.reset();
  }

  @Test
  public void testQueryIsNull() throws Exception {
    SourceResponse response = provider.query(new QueryRequestImpl(null));
    assertEquals(0, response.getHits());

    response = provider.query(null);
    assertEquals(0, response.getHits());
  }

  @Test
  public void testQueryHasLuceneSpecialCharacters() throws Exception {
    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getTampaRecord()));
    create(list, provider);

    // if + is escaped, this query will be an implicit OR otherwise both both terms would be
    // required
    Filter txtFilter =
        getFilterBuilder()
            .attribute(Metacard.ANY_TEXT)
            .like()
            .text("+Flag?taff +" + Library.TAMPA_QUERY_PHRASE);

    SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(txtFilter)));

    assertEquals(1, response.getResults().size());
  }

  /**
   * Searching Solr with a field not known to the server should return 0 results and should not give
   * an error.
   */
  @Test
  public void testQueryMissingField() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    // TXT FORMAT
    Filter txtFilter = getFilterBuilder().attribute("missingField").like().text("*");

    SourceResponse response = provider.query(quickQuery(txtFilter));

    assertEquals(0, response.getResults().size());

    // DATE FORMAT
    Filter dateFilter = getFilterBuilder().attribute("missingField").before().date(new Date());

    response = provider.query(quickQuery(dateFilter));

    assertEquals(0, response.getResults().size());

    // GEO FORMAT
    Filter geoFilter =
        getFilterBuilder().attribute("missingField").intersecting().wkt("POINT ( 1 0 ) ");

    response = provider.query(quickQuery(geoFilter));

    assertEquals(0, response.getResults().size());

    // NUMERICAL FORMAT
    Filter numericalFilter =
        getFilterBuilder().attribute("missingField").greaterThanOrEqualTo().number(23L);

    response = provider.query(quickQuery(numericalFilter));

    assertEquals(0, response.getResults().size());

    // NUMERICAL FORMAT DOUBLE
    Filter doubleFilter =
        getFilterBuilder().attribute("missingField").greaterThanOrEqualTo().number(23.0);

    response = provider.query(quickQuery(doubleFilter));

    assertEquals(0, response.getResults().size());
  }

  @Test
  public void testQueryMissingSortField() throws IngestException, UnsupportedQueryException {

    deleteAll(provider);

    MockMetacard m = new MockMetacard(Library.getTampaRecord());
    m.setTitle("Tampa");

    List<Metacard> list = Arrays.asList(m, new MockMetacard(Library.getFlagstaffRecord()));

    create(list, provider);

    Filter txtFilter = getFilterBuilder().attribute("id").like().text("*");

    QueryImpl query = new QueryImpl(txtFilter);

    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl("unknownField", SortOrder.ASCENDING));

    SourceResponse response = provider.query(new QueryRequestImpl(query));

    assertEquals(2, response.getResults().size());
  }

  /**
   * Testing if the temporal search does not fail when no schema field can be found and/or there is
   * no data in the index
   */
  @Test
  public void testQueryMissingSortFieldTemporal()
      throws IngestException, UnsupportedQueryException {

    /*
     * I have tested this with an empty schema and without an empty schema - both pass, but
     * there is no regression test for the empty schema scenario TODO there should probably be
     * an automated test that creates a fresh cache, that would be a better test
     */

    deleteAll(provider);

    Filter txtFilter = getFilterBuilder().attribute("id").like().text("*");

    QueryImpl query = new QueryImpl(txtFilter);

    query.setSortBy(new ddf.catalog.filter.impl.SortByImpl(Result.TEMPORAL, SortOrder.ASCENDING));

    SourceResponse response = provider.query(new QueryRequestImpl(query));

    assertEquals(0, response.getResults().size());
  }

  /**
   * If parts of a query can be understood, the query should be executed whereas the part that has a
   * missing property should return 0 results.
   */
  @Test
  public void testTwoQueriesWithOneMissingField() throws Exception {

    deleteAll(provider);

    MockMetacard m = new MockMetacard(Library.getTampaRecord());
    m.setTitle("Tampa");

    List<Metacard> list = Arrays.asList(m, new MockMetacard(Library.getFlagstaffRecord()));

    create(list, provider);

    Filter filter =
        getFilterBuilder()
            .anyOf(
                getFilterBuilder().attribute(Metacard.TITLE).text("Tampa"),
                getFilterBuilder().attribute("missingField").text("someText"));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Tampa should be found", 1, sourceResponse.getResults().size());
  }

  @Test(expected = IngestException.class)
  public void testDeleteNullAttribute() throws IngestException {

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
            return null;
          }

          @Override
          public String getAttributeName() {
            return null;
          }
        });
  }

  @Test
  public void contextualLogicalAndPositiveCase() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.and(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.AIRPORT_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Flagstaff and Airport", ONE_HIT, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalAndNegativeCase() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.and(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.TAMPA_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Flagstaff and Tampa", 0, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalOrPositiveCase() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.or(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.TAMPA_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Flagstaff OR Tampa", 2, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalAndOrPositiveCase()
      throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.or(
            filterFactory.and(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    Library.AIRPORT_QUERY_PHRASE,
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false),
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    "AZ",
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false)),
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals(
        "Failed: (Airport AND AZ) or Flagstaff", ONE_HIT, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalComplex() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.and(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.AIRPORT_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.and(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    "AZ",
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false),
                filterFactory.or(
                    filterFactory.like(
                        filterFactory.property(Metacard.METADATA),
                        Library.FLAGSTAFF_QUERY_PHRASE,
                        DEFAULT_TEST_WILDCARD,
                        DEFAULT_TEST_SINGLE_WILDCARD,
                        DEFAULT_TEST_ESCAPE,
                        false),
                    filterFactory.like(
                        filterFactory.property(Metacard.METADATA),
                        Library.TAMPA_QUERY_PHRASE,
                        DEFAULT_TEST_WILDCARD,
                        DEFAULT_TEST_SINGLE_WILDCARD,
                        DEFAULT_TEST_ESCAPE,
                        false))));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals(
        "(Airport AND (AZ AND (Flagstaff OR TAMPA)))", ONE_HIT, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalNotPositiveCase() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.and(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.not(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    Library.TAMPA_QUERY_PHRASE,
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false)));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Did not find Flagstaff NOT Tampa", ONE_HIT, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalNotNegativeCase() throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.and(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.not(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    Library.AIRPORT_QUERY_PHRASE,
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false)));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Wrongly found Flagstaff NOT Airport", 0, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalSingleNotPositiveCase()
      throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.not(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.TAMPA_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Did not find Flagstaff", ONE_HIT, sourceResponse.getResults().size());
    assertTrue(
        sourceResponse
            .getResults()
            .get(0)
            .getMetacard()
            .getMetadata()
            .contains(Library.FLAGSTAFF_QUERY_PHRASE));
  }

  @Test
  public void contextualLogicalMultiNotPositiveCase()
      throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    LinkedList<Filter> filters = new LinkedList<>();
    filters.add(
        filterFactory.like(
            filterFactory.property(Metacard.METADATA),
            Library.FLAGSTAFF_QUERY_PHRASE,
            DEFAULT_TEST_WILDCARD,
            DEFAULT_TEST_SINGLE_WILDCARD,
            DEFAULT_TEST_ESCAPE,
            false));
    filters.add(
        filterFactory.not(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.TAMPA_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false)));
    filters.add(
        filterFactory.not(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                "Pennsylvania",
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false)));

    Filter filter = filterFactory.and(filters);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals("Did not find Flagstaff NOT Tampa", ONE_HIT, sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalNestedOrAndPositiveCase()
      throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.or(
            filterFactory.and(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    Library.AIRPORT_QUERY_PHRASE,
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false),
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    "AZ",
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false)),
            filterFactory.or(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    Library.FLAGSTAFF_QUERY_PHRASE,
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false),
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    "AZ",
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false)));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals(
        "Failed: ( Airport )  AND  ( AZ )  OR  ( Flagstaff )  OR  ( AZ ) ",
        ONE_HIT,
        sourceResponse.getResults().size());
  }

  @Test
  public void contextualLogicalOrThenNotPositiveCase()
      throws UnsupportedQueryException, IngestException {
    createContextualMetacards();
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.or(
            filterFactory.like(
                filterFactory.property(Metacard.METADATA),
                Library.FLAGSTAFF_QUERY_PHRASE,
                DEFAULT_TEST_WILDCARD,
                DEFAULT_TEST_SINGLE_WILDCARD,
                DEFAULT_TEST_ESCAPE,
                false),
            filterFactory.and(
                filterFactory.like(
                    filterFactory.property(Metacard.METADATA),
                    "AZ",
                    DEFAULT_TEST_WILDCARD,
                    DEFAULT_TEST_SINGLE_WILDCARD,
                    DEFAULT_TEST_ESCAPE,
                    false),
                filterFactory.not(
                    filterFactory.like(
                        filterFactory.property(Metacard.METADATA),
                        Library.TAMPA_QUERY_PHRASE,
                        DEFAULT_TEST_WILDCARD,
                        DEFAULT_TEST_SINGLE_WILDCARD,
                        DEFAULT_TEST_ESCAPE,
                        false))));

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertEquals(
        "Failed: ( Flagstaff )  OR  ( AZ )  NOT  (  ( Tampa )  )  ",
        ONE_HIT,
        sourceResponse.getResults().size());
  }

  private void createContextualMetacards() throws UnsupportedQueryException, IngestException {
    deleteAll(provider);

    MockMetacard m = new MockMetacard(Library.getTampaRecord());
    m.setTitle("Tampa");

    List<Metacard> list = Arrays.asList(new MockMetacard(Library.getFlagstaffRecord()), m);

    assertEquals(2, create(list, provider).getCreatedMetacards().size());
  }

  /** Testing attributes are properly indexed. */
  @Test
  public void testContextualXmlAttributes() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    String soughtWord = "self";

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

    list.add(metacard1);

    create(list, provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.METADATA).is().like().text(soughtWord), provider);
  }

  @Test
  public void testContextualXmlTagsNotIndexed() throws Exception {
    deleteAll(provider);

    create(Collections.singletonList(new MockMetacard(Library.getFlagstaffRecord())), provider);

    String xmlTag = "lastBuildDate";

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.METADATA).is().like().text(xmlTag), provider);
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.METADATA).is().like().text(xmlTag + "*"),
        provider);
  }

  /** Testing {@link Metacard#ANY_TEXT} */
  @Test
  public void testContextualAnyText() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    String soughtWord = "nitf";
    metacard1.setContentTypeName(soughtWord);

    list.add(metacard1);

    MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());

    list.add(metacard2);

    MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());

    list.add(metacard3);

    create(list, provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text(soughtWord), provider);
  }

  @Test
  public void testQueryExcludedAttributes() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    String soughtWord = "nitf";
    metacard1.setTitle(soughtWord);
    list.add(metacard1);

    MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
    list.add(metacard2);

    MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
    list.add(metacard3);

    create(list, provider);

    QueryImpl query =
        new QueryImpl(getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text(soughtWord));
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(
        SolrMetacardClientImpl.EXCLUDE_ATTRIBUTES,
        com.google.common.collect.Sets.newHashSet(Metacard.TITLE));
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query, properties));
    assertEquals(1, sourceResponse.getResults().size());
    assertThat(sourceResponse.getResults().get(0).getMetacard().getTitle(), is(nullValue()));
  }

  /** Testing Tokenization of the search phrase. */
  @Test
  public void testWhitespaceTokenizedFieldWithWildcardSearch() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    String title = "AB-12.yz_file";

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());

    metacard1.setTitle(title);

    String searchPhrase1 = "AB*12.yz_file";
    String searchPhrase2 = "AB-12*yz_file";
    String searchPhrase3 = "AB-12.yz*file";
    String searchPhrase4 = "AB-12.*_file";
    String searchPhrase5 = "Flagstaff Chamb*";
    String searchPhrase6 = "Flagstaff Cmmerce";

    list.add(metacard1);

    create(list, provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().like().text(searchPhrase1), provider);
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().like().text(searchPhrase2), provider);
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().like().text(searchPhrase3), provider);
    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().like().text(searchPhrase4), provider);
    // Matching Phrase with wildcard
    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase5),
        provider);
    // Non-Matching Phrase without wildcard
    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase6),
        provider);
  }

  /** Testing case sensitive index. */
  @Test
  public void testContextualCaseSensitiveSimple() throws Exception {

    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getTampaRecord()));

    create(list, provider);

    QueryImpl query;
    SourceResponse sourceResponse;

    // CONTEXTUAL QUERY - REGRESSION TEST OF SIMPLE TERMS

    // Find one
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText(Library.FLAGSTAFF_QUERY_PHRASE));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    // Find the other
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText(Library.TAMPA_QUERY_PHRASE));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    // Find nothing
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText("NO_SUCH_WORD"));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(0, sourceResponse.getResults().size());

    // Find both
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText(Library.AIRPORT_QUERY_PHRASE));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());

    // Phrase
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText("Airport TPA in FL"));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    // NEGATIVE CASES
    query =
        new QueryImpl(
            getFilterBuilder().attribute(Metacard.METADATA).is().like().caseSensitiveText("Tamp"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(0, sourceResponse.getResults().size());

    query =
        new QueryImpl(
            getFilterBuilder().attribute(Metacard.METADATA).is().like().caseSensitiveText("TAmpa"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(0, sourceResponse.getResults().size());

    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.METADATA)
                .is()
                .like()
                .caseSensitiveText("AIrport TPA in FL"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(0, sourceResponse.getResults().size());
  }

  @Test
  public void testContextualCaseSensitiveWildcardWithPunctuation() throws Exception {

    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getTampaRecord()));

    create(list, provider);

    QueryImpl query;
    SourceResponse sourceResponse;

    // WILDCARD CASE SENSITIVE CONTEXTUAL QUERY
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .caseSensitiveText("http://www.flagstaffchamber.com/arizona-cardinals*"));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .caseSensitiveText("http://*10160"));
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .caseSensitiveText("10160*"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    // NEGATIVE CASES
    query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.ANY_TEXT)
                .is()
                .like()
                .caseSensitiveText("HTTP://www.flagstaffchamber.com/arizona-cardinals*"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(0, sourceResponse.getResults().size());
  }

  @Test
  public void testContextualFuzzy() throws Exception {
    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getTampaRecord()));

    // CREATE
    create(list, provider);

    // CONTEXTUAL QUERY - FUZZY
    Filter filter =
        getFilterBuilder()
            .attribute(Metacard.METADATA)
            .like()
            .fuzzyText(Library.FLAGSTAFF_QUERY_PHRASE);
    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals(
        "Expected one hit for fuzzy term 'Flagstaff'", ONE_HIT, sourceResponse.getResults().size());

    // CONTEXTUAL QUERY - FUZZY PHRASE
    filter = getFilterBuilder().attribute(Metacard.METADATA).like().fuzzyText("Flagstaff Chamber");
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals(
        "Expected one hit for fuzzy term 'Flagstaff Chamber'",
        ONE_HIT,
        sourceResponse.getResults().size());

    // CONTEXTUAL QUERY - FUZZY PHRASE, multiple spaces
    filter =
        getFilterBuilder().attribute(Metacard.METADATA).like().fuzzyText("Flagstaff    Chamber");
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals(
        "Expected one hit for fuzzy term 'Flagstaff    Chamber'",
        ONE_HIT,
        sourceResponse.getResults().size());

    // CONTEXTUAL QUERY - FUZZY PHRASE, upper case with insertion
    filter = getFilterBuilder().attribute(Metacard.METADATA).like().fuzzyText("FLGD");
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals("Expected two hits for fuzzy term 'FLGD'", 2, sourceResponse.getResults().size());

    // CONTEXTUAL QUERY - FUZZY PHRASE, second word missing
    filter = getFilterBuilder().attribute(Metacard.METADATA).like().fuzzyText("Flagstaff Igloo");
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertEquals(
        "Expected zero hits for fuzzy term 'Flagstaff Igloo'",
        0,
        sourceResponse.getResults().size());

    // CONTEXTUAL QUERY - FUZZY - Possible POSITIVE CASE
    // Possible matches are:
    // Tampa record has word 'company'
    // Flagstaff has word 'camp'
    filter = getFilterBuilder().attribute(Metacard.METADATA).like().fuzzyText("comp");
    sourceResponse = provider.query(new QueryRequestImpl(new QueryImpl(filter)));
    assertThat(
        "Expected to find any hits for fuzzy 'comp'",
        sourceResponse.getResults().size(),
        is(greaterThanOrEqualTo(1)));

    // CONTEXTUAL QUERY - FUZZY - Bad fuzzy field
    filter = getFilterBuilder().attribute(Metacard.CREATED).like().fuzzyText(new Date().toString());
    try {
      provider.query(new QueryRequestImpl(new QueryImpl(filter)));
      fail("Should not be allowed to run a fuzzy on a date field.");
    } catch (UnsupportedQueryException e) {
      LOGGER.info("Properly received exception.");
    }
  }

  @Test
  public void testContextualWildcard() throws Exception {

    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getTampaRecord()));

    create(list, provider);

    QueryImpl query;
    SourceResponse sourceResponse;

    query =
        new QueryImpl(
            getFilterBuilder().attribute(Metacard.METADATA).is().like().text("Flag*ff Chamber"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(1, sourceResponse.getResults().size());

    // FIX FOR THIS IS IN https://issues.apache.org/jira/browse/SOLR-1604
    // Either roll this in yourself or wait for it to come in with Solr
    query =
        new QueryImpl(
            getFilterBuilder().attribute(Metacard.METADATA).is().like().text("Flag*ff Pulliam"));
    query.setStartIndex(1);
    provider.query(new QueryRequestImpl(query));
    // assertEquals(0, sourceResponse.getResults().size());

    query =
        new QueryImpl(getFilterBuilder().attribute(Metacard.METADATA).is().like().text("*rport"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());

    query =
        new QueryImpl(getFilterBuilder().attribute(Metacard.METADATA).is().like().text("*rpor*"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());

    query = new QueryImpl(getFilterBuilder().attribute(Metacard.METADATA).is().like().text("*"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());

    query =
        new QueryImpl(getFilterBuilder().attribute(Metacard.METADATA).is().like().text("airpo*t"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());

    query =
        new QueryImpl(getFilterBuilder().attribute(Metacard.METADATA).is().like().text("Airpo*t"));
    query.setStartIndex(1);
    sourceResponse = provider.query(new QueryRequestImpl(query));
    assertEquals(2, sourceResponse.getResults().size());
  }

  @Test
  public void testPropertyIsLikeWithQuotedPhrases() throws Exception {
    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle("Mary");

    list.add(metacard1);

    MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
    metacard2.setTitle("Mary had a little");

    list.add(metacard2);

    MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
    metacard3.setTitle("Mary had a little l!@#$%^&*()_mb");

    list.add(metacard3);

    create(list, provider);

    queryAndVerifyCount(
        2,
        getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"Mary had*\""),
        provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"*ad a little\""),
        provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"*little\""), provider);

    queryAndVerifyCount(
        2, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"had a\""), provider);

    queryAndVerifyCount(
        2,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"Mary had*\""),
        provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"*ad a little\""),
        provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"*little\""),
        provider);

    queryAndVerifyCount(
        2, getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"had a\""), provider);

    /* Negative cases */

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"*ad\""), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("\"a had\""), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"*ad\""), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text("\"a had\""), provider);
  }

  @Test
  public void testPropertyIsLike() throws Exception {
    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle("Mary");

    list.add(metacard1);

    MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
    metacard2.setTitle("Mary had a little");

    list.add(metacard2);

    MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
    metacard3.setTitle("Mary had a little l!@#$%^&*()_mb");

    list.add(metacard3);

    create(list, provider);

    queryAndVerifyCount(
        3, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("Mary"), provider);
    queryAndVerifyCount(
        2, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("little"), provider);
    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().like().text("gary"), provider);

    /* EMPTY ATTRIBUTE */

    queryAndVerifyCount(
        3, getFilterBuilder().attribute(Metacard.DESCRIPTION).is().equalTo().text(""), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(""), provider);
  }

  @Test
  public void testPropertyIsEqualTo() throws Exception {
    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    /* STRINGS */

    MockMetacard metacard1 = new MockMetacard(Library.getFlagstaffRecord());
    metacard1.setTitle("Mary");

    Date exactEffectiveDate = new DateTime().minusMinutes(1).toDate();
    metacard1.setEffectiveDate(exactEffectiveDate);

    list.add(metacard1);

    MockMetacard metacard2 = new MockMetacard(Library.getTampaRecord());
    metacard2.setTitle("Mary had a little");

    list.add(metacard2);

    MockMetacard metacard3 = new MockMetacard(Library.getShowLowRecord());
    metacard3.setTitle("Mary had a little l!@#$%^&*()_mb");

    list.add(metacard3);

    create(list, provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text("Mary"), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text("Mar"), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text("Mary had"), provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text("Mary had a little"),
        provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder()
            .attribute(Metacard.TITLE)
            .is()
            .equalTo()
            .text("Mary had a little l!@#$%^&*()_mb"),
        provider);

    /* ANY_TEXT */

    queryAndVerifyCount(
        0,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().equalTo().text("Mary had a "),
        provider);

    queryAndVerifyCount(
        1, getFilterBuilder().attribute(Metacard.ANY_TEXT).is().equalTo().text("Mary"), provider);

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.ANY_TEXT).is().equalTo().text("Mary had a little"),
        provider);

    /* DATES */

    queryAndVerifyCount(
        1,
        getFilterBuilder().attribute(Metacard.EFFECTIVE).is().equalTo().date(exactEffectiveDate),
        provider);

    /* EMPTY ATTRIBUTE */

    queryAndVerifyCount(
        3, getFilterBuilder().attribute(Metacard.DESCRIPTION).is().equalTo().text(""), provider);

    queryAndVerifyCount(
        0, getFilterBuilder().attribute(Metacard.TITLE).is().equalTo().text(""), provider);
  }

  @Test
  public void testPropertyIsInProximityToAnyText() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard = new MockMetacard(Library.getTampaRecord());
    metacard.setDescription("Mary had a little");

    MockMetacard metacard1 = new MockMetacard(Library.getShowLowRecord());
    metacard1.setTitle("Mary had a little ham");

    list.add(metacard);
    list.add(metacard1);

    create(list, provider);

    queryAndVerifyCount(
        1, getFilterBuilder().proximity(Metacard.ANY_TEXT, 3, "Mary ham"), provider);
    queryAndVerifyCount(
        0, getFilterBuilder().proximity(Metacard.ANY_TEXT, 2, "Mary ham"), provider);
    queryAndVerifyCount(
        2, getFilterBuilder().proximity(Metacard.ANY_TEXT, 2, "Mary little"), provider);
    queryAndVerifyCount(
        0, getFilterBuilder().proximity(Metacard.ANY_TEXT, 1, "Mary little"), provider);
  }

  @Test
  public void testPropertyIsInProximityTo() throws Exception {

    deleteAll(provider);

    List<Metacard> list = new ArrayList<>();

    MockMetacard metacard = new MockMetacard(Library.getTampaRecord());
    metacard.setTitle("Mary had a little");

    MockMetacard metacard1 = new MockMetacard(Library.getShowLowRecord());
    metacard1.setTitle("Mary had a little ham");

    list.add(metacard);
    list.add(metacard1);

    create(list, provider);

    queryAndVerifyCount(1, getFilterBuilder().proximity(Core.TITLE, 3, "Mary ham"), provider);
    queryAndVerifyCount(0, getFilterBuilder().proximity(Core.TITLE, 2, "Mary ham"), provider);
    queryAndVerifyCount(2, getFilterBuilder().proximity(Core.TITLE, 2, "Mary little"), provider);
    queryAndVerifyCount(0, getFilterBuilder().proximity(Core.TITLE, 1, "Mary little"), provider);
  }

  @Test
  public void testPropertyIsDivisibleBy() throws Exception {

    deleteAll(provider);

    String longField = "divisibleByCounter";

    Set<AttributeDescriptor> descriptors =
        new HashSet<>(
            Arrays.asList(
                new AttributeDescriptorImpl(
                    Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE),
                new AttributeDescriptorImpl(
                    longField, true, true, true, true, BasicTypes.LONG_TYPE)));

    MetacardTypeImpl mType = new MetacardTypeImpl("divisibleByMetacard", descriptors);

    MetacardImpl customMetacard1 = new MetacardImpl(mType);
    customMetacard1.setAttribute(longField, 6L);

    MetacardImpl customMetacard2 = new MetacardImpl(mType);
    customMetacard2.setAttribute(longField, 12L);

    create(Arrays.asList(customMetacard1, customMetacard2), provider);

    queryAndVerifyCount(
        2,
        getFilterBuilder()
            .function("divisibleBy")
            .attributeArg(longField)
            .numberArg(3L)
            .equalTo()
            .bool(true),
        provider);
    queryAndVerifyCount(
        0,
        getFilterBuilder()
            .function("divisibleBy")
            .attributeArg(longField)
            .numberArg(7L)
            .equalTo()
            .bool(true),
        provider);
  }

  @Test(expected = UnsupportedQueryException.class)
  public void testPropertyIsEqualToCaseSensitive() throws Exception {
    deleteAll(provider);

    FilterFactory filterFactory = new FilterFactoryImpl();
    Filter filter =
        filterFactory.equal(
            filterFactory.property(Metacard.TITLE), filterFactory.literal("Mary"), false);

    // Expect an exception
    queryAndVerifyCount(0, filter, provider);
  }

  /** Tests the offset aka start index (startIndex) functionality. */
  @Test
  public void testStartIndex() throws Exception {

    deleteAll(provider);

    List<Metacard> list =
        Arrays.asList(
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()),
            new MockMetacard(Library.getFlagstaffRecord()));

    // CREATE
    create(list, provider);

    // CONTEXTUAL QUERY
    QueryImpl query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(Metacard.TITLE)
                .is()
                .equalTo()
                .text(Library.FLAGSTAFF_QUERY_PHRASE));
    query.setStartIndex(1);
    query.setRequestsTotalResultsCount(true);

    int index;
    int maxSize = 9;
    int startIndex = 1;

    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);
    query.setRequestsTotalResultsCount(true);

    SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(9, sourceResponse.getResults().size());
    assertEquals(9L, sourceResponse.getHits());

    LinkedList<Result> allItems = new LinkedList<>();

    allItems.addAll(sourceResponse.getResults());

    // 1
    maxSize = 1;
    startIndex = 2;
    index = startIndex - 1;

    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);
    query.setRequestsTotalResultsCount(true);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(ONE_HIT, sourceResponse.getResults().size());
    assertEquals(9L, sourceResponse.getHits());

    for (Result r : sourceResponse.getResults()) {

      assertEquals(
          "Testing when startIndex = " + startIndex,
          allItems.get(index).getMetacard().getMetadata(),
          r.getMetacard().getMetadata());
      index++;
    }

    // 4
    maxSize = 1;
    startIndex = 4;
    index = startIndex - 1;
    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);
    query.setRequestsTotalResultsCount(false);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(ONE_HIT, sourceResponse.getResults().size());
    assertThat(sourceResponse.getHits(), anyOf(equalTo(-1L), equalTo(9L)));

    for (Result r : sourceResponse.getResults()) {

      assertEquals(
          "Testing when startIndex = " + startIndex,
          allItems.get(index).getMetacard().getMetadata(),
          r.getMetacard().getMetadata());
      index++;
    }

    // 5
    maxSize = 5;
    startIndex = 5;
    index = startIndex - 1;
    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(5, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {

      assertEquals(
          "Testing when startIndex = " + startIndex,
          allItems.get(index).getMetacard().getMetadata(),
          r.getMetacard().getMetadata());
      index++;
    }

    // 9
    maxSize = 9;
    startIndex = 9;
    index = startIndex - 1;
    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(ONE_HIT, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {

      assertEquals(
          "Testing when startIndex = " + startIndex,
          allItems.get(index).getMetacard().getMetadata(),
          r.getMetacard().getMetadata());
      index++;
    }

    // Max size is very large
    maxSize = 100;
    startIndex = 9;
    index = startIndex - 1;
    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);

    sourceResponse = provider.query(new QueryRequestImpl(query));

    assertEquals(ONE_HIT, sourceResponse.getResults().size());

    for (Result r : sourceResponse.getResults()) {

      assertEquals(allItems.get(index).getMetacard().getMetadata(), r.getMetacard().getMetadata());
      index++;
    }

    // bad start index
    maxSize = 2;
    startIndex = ALL_RESULTS;
    query.setPageSize(maxSize);
    query.setStartIndex(startIndex);

    try {
      provider.query(new QueryRequestImpl(query));
      Assert.fail("Expected an exception stating that the start index should be greater than 0. ");
    } catch (UnsupportedQueryException e) {
      assertTrue(e.getMessage().contains("greater than 0"));
    }
  }

  @Test
  public void testFacetedResponse() throws Exception {

    deleteAll(provider);

    List<Metacard> metacards = new ArrayList<>();

    for (int i = 0; i < 9; i++) {
      Metacard metacard = new MetacardImpl();
      metacard.setAttribute(new AttributeImpl(Metacard.DESCRIPTION, "Description " + i / 2));
      metacards.add(metacard);
    }

    create(metacards, provider);

    Filter filter =
        getFilterBuilder().attribute(Metacard.DESCRIPTION).is().like().fuzzyText("Description");

    Response response =
        provider.query(
            new FacetedQueryRequest(new QueryImpl(filter), ImmutableSet.of(Metacard.DESCRIPTION)));

    Serializable rawFacetResult = response.getPropertyValue(EXPERIMENTAL_FACET_RESULTS_KEY);

    assertThat(rawFacetResult, notNullValue());
    assertThat(rawFacetResult, instanceOf(List.class));

    List<FacetAttributeResult> facetResult = (List<FacetAttributeResult>) rawFacetResult;

    assertThat(facetResult.size(), is(1));

    FacetAttributeResult descriptionResult = facetResult.get(0);

    assertThat(descriptionResult.getAttributeName(), is(Metacard.DESCRIPTION));
    assertThat(descriptionResult.getFacetValues().size(), is(5));

    List<FacetValueCount> facetValueCounts = descriptionResult.getFacetValues();

    Map<String, Long> expectedResults =
        ImmutableMap.of(
            "Description 0", 2L,
            "Description 1", 2L,
            "Description 2", 2L,
            "Description 3", 2L,
            "Description 4", 1L);

    facetValueCounts.forEach(
        fvc -> {
          Long count = expectedResults.get(fvc.getValue());
          assertThat(count, notNullValue());
          assertThat(fvc.getCount(), is(count));
        });
  }

  @Test
  public void testNumericalFields() throws Exception {
    deleteAll(provider);

    /* SETUP */
    String doubleField = "hertz";
    double doubleFieldValue = 16065.435;

    String floatField = "inches";
    float floatFieldValue = 4.435f;

    String intField = "count";
    int intFieldValue = -4;

    String longField = "milliseconds";
    long longFieldValue = 9876543293L;

    String shortField = "daysOfTheWeek";
    short shortFieldValue = 1;

    Set<AttributeDescriptor> descriptors =
        numericalDescriptors(doubleField, floatField, intField, longField, shortField);

    MetacardTypeImpl mType = new MetacardTypeImpl("numberMetacardType", descriptors);

    MetacardImpl customMetacard1 = new MetacardImpl(mType);
    customMetacard1.setAttribute(Metacard.ID, "");
    customMetacard1.setAttribute(doubleField, doubleFieldValue);
    customMetacard1.setAttribute(floatField, floatFieldValue);
    customMetacard1.setAttribute(intField, intFieldValue);
    customMetacard1.setAttribute(longField, longFieldValue);
    customMetacard1.setAttribute(shortField, shortFieldValue);

    create(Collections.singletonList(customMetacard1), provider);

    // searching double field with int value
    greaterThanQueryAssertion(doubleField, 4, 1);

    // searching float field with double value
    greaterThanQueryAssertion(floatField, 4.0, 1);

    // searching long field with int value
    greaterThanQueryAssertion(longField, intFieldValue, 1);

    // searching int field with long value
    greaterThanQueryAssertion(intField, -6L, 1);

    // searching int field with long value
    greaterThanQueryAssertion(shortField, 0L, 1);

    equalToQueryAssertion(intField, intFieldValue, 1);
  }

  @Test()
  public void testNumericalOperations() throws Exception {

    deleteAll(provider);

    /* SETUP */
    String doubleField = "hertz";
    double doubleFieldValue = 16065.435;

    String floatField = "inches";
    float floatFieldValue = 4.435f;

    String intField = "count";
    int intFieldValue = 4;

    String longField = "milliseconds";
    long longFieldValue = 9876543293L;

    String shortField = "daysOfTheWeek";
    short shortFieldValue = 1;

    Set<AttributeDescriptor> descriptors =
        numericalDescriptors(doubleField, floatField, intField, longField, shortField);

    MetacardTypeImpl mType = new MetacardTypeImpl("anotherCustom", descriptors);

    MetacardImpl customMetacard1 = new MetacardImpl(mType);
    customMetacard1.setAttribute(Metacard.ID, "");
    customMetacard1.setAttribute(doubleField, doubleFieldValue);
    customMetacard1.setAttribute(floatField, floatFieldValue);
    customMetacard1.setAttribute(intField, intFieldValue);
    customMetacard1.setAttribute(longField, longFieldValue);
    customMetacard1.setAttribute(shortField, shortFieldValue);

    MetacardImpl customMetacard2 = new MetacardImpl(mType);
    customMetacard2.setAttribute(Metacard.ID, "");
    customMetacard2.setAttribute(doubleField, doubleFieldValue + 10.0);
    customMetacard2.setAttribute(floatField, (floatFieldValue + 10.0f));
    customMetacard2.setAttribute(intField, intFieldValue + 1);
    customMetacard2.setAttribute(longField, longFieldValue + 10L);
    customMetacard2.setAttribute(shortField, (shortFieldValue + 1));

    create(Arrays.asList(customMetacard1, customMetacard2), provider);

    // on exact DOUBLE
    greaterThanQueryAssertion(doubleField, doubleFieldValue, 1);
    greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue, 2);

    // beyond the DOUBLE
    greaterThanQueryAssertion(doubleField, doubleFieldValue - 0.00000001, 2);
    greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue - 0.00000001, 2);
    greaterThanQueryAssertion(doubleField, doubleFieldValue + 12.0, 0);
    greaterThanOrEqualToQueryAssertion(doubleField, doubleFieldValue + 12.0, 0);

    // on exact FLOAT
    greaterThanQueryAssertion(floatField, floatFieldValue, 1);
    greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue, 2);

    // beyond the FLOAT
    greaterThanQueryAssertion(floatField, floatFieldValue - 0.00001f, 2);
    greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue - 0.00001f, 2);
    greaterThanQueryAssertion(floatField, floatFieldValue + 12.0f, 0);
    greaterThanOrEqualToQueryAssertion(floatField, floatFieldValue + 12.0f, 0);

    // on exact LONG
    greaterThanQueryAssertion(longField, longFieldValue, 1);
    greaterThanOrEqualToQueryAssertion(longField, longFieldValue, 2);

    // beyond the LONG
    greaterThanQueryAssertion(longField, longFieldValue - 1L, 2);
    greaterThanOrEqualToQueryAssertion(longField, longFieldValue - 1L, 2);
    greaterThanQueryAssertion(longField, longFieldValue + 12L, 0);
    greaterThanOrEqualToQueryAssertion(longField, longFieldValue + 12L, 0);

    // on exact INT
    greaterThanQueryAssertion(intField, intFieldValue, 1);
    greaterThanOrEqualToQueryAssertion(intField, intFieldValue, 2);

    // beyond the INT
    greaterThanQueryAssertion(intField, intFieldValue - 1, 2);
    greaterThanOrEqualToQueryAssertion(intField, intFieldValue - 1, 2);
    greaterThanQueryAssertion(intField, intFieldValue + 2, 0);
    greaterThanOrEqualToQueryAssertion(intField, intFieldValue + 2, 0);

    // on exact SHORT
    greaterThanQueryAssertion(shortField, shortFieldValue, 1);
    greaterThanOrEqualToQueryAssertion(shortField, shortFieldValue, 2);

    // beyond the SHORT
    greaterThanQueryAssertion(shortField, (short) (shortFieldValue - 1), 2);
    greaterThanOrEqualToQueryAssertion(shortField, (short) (shortFieldValue - 1), 2);
    greaterThanQueryAssertion(shortField, (short) (shortFieldValue + 2), 0);
    greaterThanOrEqualToQueryAssertion(shortField, (short) (shortFieldValue + 2), 0);
  }

  private void greaterThanQueryAssertion(String fieldName, Serializable fieldValue, int count)
      throws UnsupportedQueryException {

    Filter filter = null;

    if (fieldValue instanceof Double) {
      filter = getFilterBuilder().attribute(fieldName).greaterThan().number((Double) fieldValue);
    } else if (fieldValue instanceof Integer) {
      filter = getFilterBuilder().attribute(fieldName).greaterThan().number((Integer) fieldValue);
    } else if (fieldValue instanceof Short) {
      filter = getFilterBuilder().attribute(fieldName).greaterThan().number((Short) fieldValue);
    } else if (fieldValue instanceof Long) {
      filter = getFilterBuilder().attribute(fieldName).greaterThan().number((Long) fieldValue);
    } else if (fieldValue instanceof Float) {
      filter = getFilterBuilder().attribute(fieldName).greaterThan().number((Float) fieldValue);
    }

    SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(response.getResults().size(), is(equalTo(count)));
  }

  private void equalToQueryAssertion(String fieldName, Serializable fieldValue, int count)
      throws UnsupportedQueryException {

    Filter filter = null;

    if (fieldValue instanceof Double) {
      filter = getFilterBuilder().attribute(fieldName).equalTo().number((Double) fieldValue);
    } else if (fieldValue instanceof Integer) {
      filter = getFilterBuilder().attribute(fieldName).equalTo().number((Integer) fieldValue);
    } else if (fieldValue instanceof Short) {
      filter = getFilterBuilder().attribute(fieldName).equalTo().number((Short) fieldValue);
    } else if (fieldValue instanceof Long) {
      filter = getFilterBuilder().attribute(fieldName).equalTo().number((Long) fieldValue);
    } else if (fieldValue instanceof Float) {
      filter = getFilterBuilder().attribute(fieldName).equalTo().number((Float) fieldValue);
    }

    SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(response.getResults().size(), is(equalTo(count)));
  }

  private void greaterThanOrEqualToQueryAssertion(
      String fieldName, Serializable fieldValue, int count) throws UnsupportedQueryException {

    Filter filter = null;

    if (fieldValue instanceof Double) {
      filter =
          getFilterBuilder()
              .attribute(fieldName)
              .greaterThanOrEqualTo()
              .number((Double) fieldValue);
    } else if (fieldValue instanceof Integer) {
      filter =
          getFilterBuilder()
              .attribute(fieldName)
              .greaterThanOrEqualTo()
              .number((Integer) fieldValue);
    } else if (fieldValue instanceof Short) {
      filter =
          getFilterBuilder().attribute(fieldName).greaterThanOrEqualTo().number((Short) fieldValue);
    } else if (fieldValue instanceof Long) {
      filter =
          getFilterBuilder().attribute(fieldName).greaterThanOrEqualTo().number((Long) fieldValue);
    } else if (fieldValue instanceof Float) {
      filter =
          getFilterBuilder().attribute(fieldName).greaterThanOrEqualTo().number((Float) fieldValue);
    }

    SourceResponse response = provider.query(new QueryRequestImpl(new QueryImpl(filter)));

    assertThat(response.getResults().size(), is(equalTo(count)));
  }
}
