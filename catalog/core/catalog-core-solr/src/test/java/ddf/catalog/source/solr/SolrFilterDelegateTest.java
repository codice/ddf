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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;

public class SolrFilterDelegateTest {

  private static final String TOKENIZED_METADATA_FIELD =
      Metacard.METADATA + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED;

  private DynamicSchemaResolver mockResolver = mock(DynamicSchemaResolver.class);

  private SolrFilterDelegate toTest = new SolrFilterDelegate(mockResolver, Collections.EMPTY_MAP);

  @Test(expected = UnsupportedOperationException.class)
  public void intersectsWithNullWkt() {
    // given null WKT and a valid property name
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    // when the delegate intersects
    toTest.intersects("testProperty", null);
    // then the operation is unsupported
  }

  @Test(expected = UnsupportedOperationException.class)
  public void intersectsWithNullPropertyName() {
    // given null property name
    // when the delegate intersects
    toTest.intersects(null, "wkt");
    // then the operation is unsupported
  }

  @Test
  public void intersectsWithInvalidJtsWkt() {
    // given a geospatial property
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");

    // when the delegate intersects on WKT not handled by JTS
    SolrQuery query = toTest.intersects("testProperty", "invalid JTS wkt");

    // then return a valid Solr query using the given WKT
    assertThat(query.getQuery(), is("testProperty_geohash_index:\"Intersects(invalid JTS wkt)\""));
  }

  @Test
  public void intersectsWithValidJtsWkt() {
    // given a geospatial property
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");

    // when the delegate intersects on WKT not handled by JTS
    SolrQuery query = toTest.intersects("testProperty", "POINT(1 0)");

    // then return a valid Solr query using the given WKT
    assertThat(
        query.getQuery(),
        startsWith("testProperty_geohash_index:\"Intersects(BUFFER(POINT(1.0 0.0), "));
  }

  @Test
  public void selfIntersectingPolygon() {
    String wkt = "POLYGON((0 0, 10 0, 10 20, 5 -5, 0 20, 0 0))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(POLYGON ((5 -5, 0 0, 0 20, 10 20, 10 0, 5 -5)))\""));
  }

  @Test
  public void squarePolygon() {
    String wkt = "POLYGON ((0 10, 0 30, 20 30, 20 10, 0 10))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(POLYGON ((0 10, 0 30, 20 30, 20 10, 0 10)))\""));
  }

  @Test
  public void nonIntersectingPolygon() {
    String wkt = "POLYGON((5 -5, 0 0, 0 20, 10 20, 10 0, 5 -5))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(POLYGON ((5 -5, 0 0, 0 20, 10 20, 10 0, 5 -5)))\""));
  }

  @Test
  public void polygonWithHoleAndSelfIntersecting() {
    // in the case of a self-intersecting polygon with a hole the hole is lost in the conversion
    String wkt = "POLYGON ((0 0, 0 10, 13 3, 13 9, 0 0), (1 4, 1 7, 3 6, 3 4, 1 4))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(POLYGON ((0 0, 0 10, 13 9, 13 3, 0 0)))\""));
  }

  @Test
  public void multiPolygon() {
    String wkt =
        "MULTIPOLYGON (((30 20, 45 40, 10 40, 30 20)), ((15 5, 40 10, 10 20, 5 10, 15 5)))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(MULTIPOLYGON (((30 20, 45 40, 10 40, 30 20)), ((15 5, 40 10, 10 20, 5 10, 15 5))))\""));
  }

  @Test
  public void polygonWithHole() {
    String wkt = "POLYGON ((35 10, 45 45, 15 40, 10 20, 35 10), (20 30, 35 35, 30 20, 20 30))";
    stub(mockResolver.getField(
            "testProperty", AttributeFormat.GEOMETRY, false, Collections.EMPTY_MAP))
        .toReturn("testProperty_geohash_index");
    SolrQuery query = toTest.contains("testProperty", wkt);
    assertThat(
        query.getQuery(),
        startsWith(
            "testProperty_geohash_index:\"Contains(POLYGON ((35 10, 45 45, 15 40, 10 20, 35 10), (20 30, 35 35, 30 20, 20 30)))\""));
  }

  @Test
  public void reservedSpecialCharactersIsEqual() {
    // given a text property
    stub(mockResolver.getField("testProperty", AttributeFormat.STRING, true, Collections.EMPTY_MAP))
        .toReturn("testProperty_txt_index");

    // when searching for exact reserved characters
    SolrQuery equalQuery =
        toTest.propertyIsEqualTo("testProperty", "+ - && || ! ( ) { } [ ] ^ \" ~ :", true);

    // then return escaped special characters in the query
    assertThat(
        equalQuery.getQuery(),
        is(
            "testProperty_txt_index:\"\\+ \\- \\&& \\|| \\! \\( \\) \\{ \\} \\[ \\] \\^ \\\" \\~ \\:\""));
  }

  @Test
  public void reservedSpecialCharactersIsLike() {
    // given a tokenized text property
    stub(mockResolver.getField("testProperty", AttributeFormat.STRING, true, Collections.EMPTY_MAP))
        .toReturn("testProperty_txt");
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);
    stub(mockResolver.getCaseSensitiveField("testProperty_txt_tokenized", Collections.EMPTY_MAP))
        .toReturn("testProperty_txt_tokenized_tokenized");

    // when searching for like reserved characters
    SolrQuery likeQuery =
        toTest.propertyIsLike("testProperty", "+ - && || ! ( ) { } [ ] ^ \" ~ : \\*?", true);

    // then return escaped special characters in the query
    assertThat(
        likeQuery.getQuery(),
        is(
            "(testProperty_txt_tokenized_tokenized:(\\+ \\- \\&& \\|| \\! \\( \\) \\{ \\} \\[ \\] \\^ \\\" \\~ \\: \\*?))"));
  }

  /*
    DDF-314: COmmented out until the ANY_TEXT functionality is added back
    in - then these tests can be activated.

  @Test
  public void testPropertyIsEqualTo_AnyText_CaseSensitive() {
      String expectedQuery = "any_text:\"mySearchPhrase\"";
      String searchPhrase = "mySearchPhrase";
      boolean isCaseSensitive = true;
      SolrQuery equalToQuery = toTest.propertyIsEqualTo(Metacard.ANY_TEXT, searchPhrase,
              isCaseSensitive);
      assertThat(equalToQuery.getQuery(), is(expectedQuery));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPropertyIsEqualTo_AnyText_CaseInsensitive() {
      String searchPhrase = "mySearchPhrase";
      boolean isCaseSensitive = false;
      toTest.propertyIsEqualTo(Metacard.ANY_TEXT, searchPhrase, isCaseSensitive);
  }

  @Test
  public void testPropertyIsFuzzy_AnyText() {
      String expectedQuery = "+any_text:mysearchphrase~ ";
      String searchPhrase = "mySearchPhrase";
      SolrQuery fuzzyQuery = toTest.propertyIsFuzzy(Metacard.ANY_TEXT, searchPhrase);
      assertThat(fuzzyQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLike_AnyText_CaseInsensitive() {
      String expectedQuery = "any_text:\"mySearchPhrase\"";
      String searchPhrase = "mySearchPhrase";
      boolean isCaseSensitive = false;
      SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase,
              isCaseSensitive);
      assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLike_AnyText_CaseSensitive() {
      String expectedQuery = "any_text_has_case:\"mySearchPhrase\"";
      String searchPhrase = "mySearchPhrase";
      boolean isCaseSensitive = true;
      when(mockResolver.getCaseSensitiveField("any_text")).thenReturn(
              "any_text" + ddf.catalog.source.solr.SchemaFields.HAS_CASE);
      SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase,
              isCaseSensitive);
      assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }
  END OMIT per DDF-314*/

  @Test
  public void testPropertyIsEqualToEmpty() {
    stub(mockResolver.getField("title", AttributeFormat.STRING, true, Collections.EMPTY_MAP))
        .toReturn("title_txt");

    String searchPhrase = "";
    String expectedQuery = "-title_txt:[\"\" TO *]";
    boolean isCaseSensitive = true;

    SolrQuery isEqualTo = toTest.propertyIsEqualTo("title", searchPhrase, isCaseSensitive);

    assertThat(isEqualTo.getQuery(), is(expectedQuery));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPropertyIsEqualToNull() {
    stub(mockResolver.getField("title", AttributeFormat.STRING, true, Collections.EMPTY_MAP))
        .toReturn("title_txt");

    String searchPhrase = null;
    boolean isCaseSensitive = true;

    toTest.propertyIsEqualTo("title", searchPhrase, isCaseSensitive);
  }

  @Test
  public void testPropertyIsLikeWildcard() {
    stub(mockResolver.anyTextFields()).toReturn(Collections.singletonList("metadata_txt").stream());

    String searchPhrase = "*";
    String expectedQuery = "*:*";
    boolean isCaseSensitive = false;

    SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase, isCaseSensitive);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLikeTermAndWildcard() {
    stub(mockResolver.anyTextFields()).toReturn(Collections.singletonList("metadata_txt").stream());
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);

    String searchPhrase = "abc-123*";
    String expectedQuery = "(" + TOKENIZED_METADATA_FIELD + ":(abc\\-123*))";
    boolean isCaseSensitive = false;

    SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase, isCaseSensitive);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLikeEmpty() {
    stub(mockResolver.getField("title", AttributeFormat.STRING, false, Collections.EMPTY_MAP))
        .toReturn("title_txt");

    String searchPhrase = "";
    String expectedQuery = "-title_txt:[\"\" TO *]";
    boolean isCaseSensitive = false;

    SolrQuery isLikeQuery = toTest.propertyIsLike("title", searchPhrase, isCaseSensitive);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPropertyIsNull() {
    stub(mockResolver.getField("title", AttributeFormat.STRING, false, Collections.EMPTY_MAP))
        .toReturn("title_txt");

    String searchPhrase = null;
    boolean isCaseSensitive = false;

    toTest.propertyIsLike("title", searchPhrase, isCaseSensitive);
  }

  @Test
  public void testPropertyIsLikeWildcardNoTokens() {
    stub(mockResolver.anyTextFields()).toReturn(Collections.singletonList("metadata_txt").stream());
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);

    String searchPhrase = "title*";
    String expectedQuery = "(" + TOKENIZED_METADATA_FIELD + ":(title*))";
    boolean isCaseSensitive = false;

    SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase, isCaseSensitive);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLikeMultipleTermsWithWildcard() {
    stub(mockResolver.anyTextFields()).toReturn(Collections.singletonList("metadata_txt").stream());
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);

    String searchPhrase = "abc 123*";
    String expectedQuery = "(" + TOKENIZED_METADATA_FIELD + ":(abc 123*))";

    SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase, false);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsLikeCaseSensitiveWildcard() {
    stub(mockResolver.anyTextFields()).toReturn(Collections.singletonList("metadata_txt").stream());
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);
    stub(mockResolver.getCaseSensitiveField("metadata_txt_tokenized", Collections.EMPTY_MAP))
        .toReturn("metadata_txt_tokenized_has_case");

    String searchPhrase = "abc-123*";
    String expectedQuery =
        "(" + TOKENIZED_METADATA_FIELD + SchemaFields.HAS_CASE + ":(abc\\-123*))";

    SolrQuery isLikeQuery = toTest.propertyIsLike(Metacard.ANY_TEXT, searchPhrase, true);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testTemporalBefore() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:[ * TO 1995-11-24T23:59:56.765Z } ";
    SolrQuery temporalQuery = toTest.before(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testTemporalAfter() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:{ 1995-11-24T23:59:56.765Z TO * ] ";
    SolrQuery temporalQuery = toTest.after(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testDatePropertyGreaterThan() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:{ 1995-11-24T23:59:56.765Z TO * ] ";
    SolrQuery temporalQuery = toTest.propertyIsGreaterThan(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testDatePropertyGreaterThanOrEqualTo() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:[ 1995-11-24T23:59:56.765Z TO * ] ";
    SolrQuery temporalQuery =
        toTest.propertyIsGreaterThanOrEqualTo(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testDatePropertyLessThan() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:[ * TO 1995-11-24T23:59:56.765Z } ";
    SolrQuery temporalQuery = toTest.propertyIsLessThan(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testDatePropertyLessThanOrEqualTo() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery = " created_date:[ * TO 1995-11-24T23:59:56.765Z ] ";
    SolrQuery temporalQuery = toTest.propertyIsLessThanOrEqualTo(Metacard.CREATED, getCannedTime());
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testDatePropertyIsBetween() {
    stub(mockResolver.getField("created", AttributeFormat.DATE, false, Collections.EMPTY_MAP))
        .toReturn("created_date");

    String expectedQuery =
        " created_date:[ 1995-11-24T23:59:56.765Z TO 1995-11-27T04:59:56.765Z ] ";
    SolrQuery temporalQuery =
        toTest.propertyIsBetween(
            Metacard.CREATED, getCannedTime(), getCannedTime(1995, Calendar.NOVEMBER, 27, 4));
    assertThat(temporalQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testXpathExists() {
    String xpath = "//root/sub/@attribute";
    String expectedQuery = "{!xpath}xpath:\"" + xpath + "\"";
    SolrQuery xpathQuery = toTest.xpathExists(xpath);
    assertThat(xpathQuery.getFilterQueries()[0], is(expectedQuery));
  }

  @Test
  public void testXpathIsLike() {
    String xpath = "//root/sub/@attribute";
    String expectedQuery = "{!xpath}xpath:\"" + xpath + "[contains(lower-case(.), 'example')]\"";
    SolrQuery xpathQuery = toTest.xpathIsLike(xpath, "example", false);
    assertThat(xpathQuery.getFilterQueries()[0], is(expectedQuery));
  }

  @Test
  public void testXpathOR() {
    String xpath = "//root/sub/@attribute";
    String expected1Query = "{!xpath}xpath:\"" + xpath + "[contains(lower-case(.), 'example1')]\"";
    SolrQuery xpath1Query = toTest.xpathIsLike(xpath, "example1", false);
    assertThat(xpath1Query.getFilterQueries()[0], is(expected1Query));

    String expected2Query = "{!xpath}xpath:\"" + xpath + "[contains(lower-case(.), 'example2')]\"";
    SolrQuery xpath2Query = toTest.xpathIsLike(xpath, "example2", false);
    assertThat(xpath2Query.getFilterQueries()[0], is(expected2Query));

    SolrQuery combinedQuery = toTest.or(Arrays.asList(xpath1Query, xpath2Query));
    String combinedExpectedFilter =
        "{!xpath}xpath:\"("
            + xpath
            + "[contains(lower-case(.), 'example1')] or "
            + xpath
            + "[contains(lower-case(.), 'example2')])\"";
    String expectedIndex =
        "{!xpath}(xpath_index:\""
            + xpath
            + "[contains(lower-case(.), 'example1')]\") OR "
            + "(xpath_index:\""
            + xpath
            + "[contains(lower-case(.), 'example2')]\")";
    // TODO DDF-1882 support xpath pre-filtering
    //        assertThat(combinedQuery.getFilterQueries().length, is(2));
    assertThat(combinedQuery.getFilterQueries()[0], is(combinedExpectedFilter));
    //        assertThat(combinedQuery.getFilterQueries()[1], is(expectedIndex));
  }

  @Test
  public void testPropertyIsInProximityTo() {
    stub(mockResolver.getField("title", AttributeFormat.STRING, true, Collections.EMPTY_MAP))
        .toReturn("title_txt");
    stub(mockResolver.getSpecialIndexSuffix(AttributeFormat.STRING, Collections.EMPTY_MAP))
        .toReturn(SchemaFields.TOKENIZED);

    String expectedQuery = "(title_txt_tokenized:\"a proximity string\" ~2)";
    SolrQuery solrQuery = toTest.propertyIsInProximityTo(Core.TITLE, 2, "a proximity string");

    assertThat(solrQuery.getQuery(), is(expectedQuery));
  }

  @Test
  public void testPropertyIsDivisibleBy() {
    stub(mockResolver.getAnonymousField(Core.RESOURCE_SIZE))
        .toReturn(Collections.singletonList("resource-size_lng"));

    long divisibleBy = 2L;
    String expectedQuery = "_val_:\"{!frange l=0 u=0}mod(field(resource-size_lng,min),2)\"";

    SolrQuery isLikeQuery = toTest.propertyIsDivisibleBy(Core.RESOURCE_SIZE, divisibleBy);

    assertThat(isLikeQuery.getQuery(), is(expectedQuery));
  }

  private Date getCannedTime() {
    return getCannedTime(1995, Calendar.NOVEMBER, 24, 23);
  }

  private Date getCannedTime(int year, int month, int day, int hour) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    calendar.clear();
    calendar.set(year, month, day, hour, 59, 56);
    calendar.set(Calendar.MILLISECOND, 765);
    return calendar.getTime();
  }
}
