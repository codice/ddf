/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.catalog;

import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestCswRecord;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestMetacards;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertTrue;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.XmlSearch;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule.ConditionalIgnore;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.itests.common.utils.LoggingUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.specification.RequestSpecification;

import ddf.catalog.data.types.Location;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSpatial extends AbstractIntegrationTest {

    private static final String CSW_RESPONSE_COUNTRY_CODE = "GBR";

    private static final String CSW_RESOURCE_ROOT = "/TestSpatial/";

    private static final String CSW_QUERY_RESOURCES = CSW_RESOURCE_ROOT + "csw/request/query/";

    private static final String CSW_METACARD = "CswRecord.xml";

    private static final String GEOJSON_NEAR_METACARD = "GeoJson near";

    private static final String GEOJSON_FAR_METACARD = "GeoJson far";

    private static final String PLAINXML_NEAR_METACARD = "PlainXml near";

    private static final String PLAINXML_FAR_METACARD = "PlainXml far";

    private static final String TEXT_XML_UTF_8 = "text/xml;charset=UTF-8";

    private static Map<String, String> savedCswQueries = new HashMap<>();

    private static Map<String, String> metacardIds = new HashMap<>();

    private final ImmutableMap<String, ExpectedResultPair[]> cswExpectedResults =
            ImmutableMap.<String, ExpectedResultPair[]>builder().put("CswAfterDateQuery",
                    new ExpectedResultPair[] {
                            new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)})
                    .put("CswBeforeDateQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_FAR_METACARD)})
                    .put("CswContainingWktLineStringQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)})
                    .put("CswContainingWktPolygonQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_FAR_METACARD)})
                    .put("CswDuringDatesQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)})
                    .put("CswEqualToTextQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
                    .put("CswIntersectingWktLineStringQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD)})
                    .put("CswIntersectingWktPolygonQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswLikeTextQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswNearestToWktLineStringQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD)})
                    .put("CswNearestToWktPolygonQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD),
                                    new ExpectedResultPair(ResultType.TITLE,
                                            PLAINXML_NEAR_METACARD)})
                    .put("CswOverLappingDatesQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswWithinBufferWktLineStringQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD)})
                    .put("CswWithinBufferWktPolygonQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswWithinWktPolygonQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswCompoundLikeTextAndIntersectingWktLineString",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD)})
                    .put("CswCompoundAfterDateAndIntersectingWktPolygon",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    GEOJSON_NEAR_METACARD)})
                    .put("CswCompoundBeforeDateAndLikeText",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_FAR_METACARD)})
                    .put("CswCompoundNotBeforeDateAndLikeText",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
                    .put("CswLogicalOperatorContextualNotQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD)})
                    .put("CswLogicalOperatorContextualOrQuery",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_NEAR_METACARD),
                                    new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)})
                    .put("CswXPathExpressionQueryWithAttributeSelector",
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    PLAINXML_FAR_METACARD)})
                    .put("CswXPathExpressionQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
                    .put("CswFuzzyTextQuery",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
                    .put("CswCompoundNot",
                            new ExpectedResultPair[] {
                                    new ExpectedResultPair(ResultType.COUNT, "0")})
                    .build();

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    /**
     * Loads the resource queries into memory.
     */
    public static Map<String, String> loadResourceQueries(String resourcesPath,
            Map<String, String> savedQueries) {

        //gets a list of resources available within the resource bundle
        Enumeration<URL> queryResourcePaths = FrameworkUtil.getBundle(AbstractIntegrationTest.class)
                .getBundleContext()
                .getBundle()
                .findEntries(resourcesPath, "*", false);

        while (queryResourcePaths.hasMoreElements()) {
            String queryResourcePath = queryResourcePaths.nextElement()
                    .getPath();
            if (!queryResourcePath.endsWith("/")) {
                String queryName = queryResourcePath.substring(
                        queryResourcePath.lastIndexOf("/") + 1);
                savedQueries.put(removeFileExtension(queryName), getFileContent(queryResourcePath));
            }
        }
        return savedQueries;
    }

    private static String removeFileExtension(String file) {
        return file.contains(".") ? file.substring(0, file.lastIndexOf(".")) : file;
    }

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            waitForSystemReady();
            loadResourceQueries(CSW_QUERY_RESOURCES, savedCswQueries);
        } catch (Exception e) {
            LoggingUtils.failWithThrowableStacktrace(e, "Failed to start required apps: ");
        }
    }

    @After
    public void tearDown() throws Exception {
        clearCatalog();
    }

    @Test
    public void testCswAfterDateQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswAfterDateQuery");
    }

    @Test
    public void testCswBeforeDateQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswBeforeDateQuery");
    }

    @Test
    public void testCswContainingWktLineStringQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswContainingWktLineStringQuery");
    }

    @Test
    public void testCswContainingWktPolygonQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswContainingWktPolygonQuery");
    }

    @Test
    public void testCswDuringDatesQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswDuringDatesQuery");
    }

    @Test
    public void testCswEqualToTextQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswEqualToTextQuery");
    }

    @Test
    public void testCswIntersectingWktLineStringQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswIntersectingWktLineStringQuery");
    }

    @Test
    public void testCswIntersectingWktPolygonQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswIntersectingWktPolygonQuery");
    }

    @Test
    public void testCswLikeTextQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswLikeTextQuery");
    }

    @Test
    public void testCswNearestToWktLineStringQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswNearestToWktLineStringQuery");
    }

    @Test
    public void testCswNearestToWktPolygonQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswNearestToWktPolygonQuery");
    }

    @Test
    public void testCswOverLappingDatesQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswOverLappingDatesQuery");
    }

    @Test
    public void testCswWithinBufferWktLineStringQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswWithinBufferWktLineStringQuery");
    }

    @Test
    public void testCswWithinBufferWktPolygonQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswWithinBufferWktPolygonQuery");
    }

    @Test
    public void testCswWithinWktPolygonQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswWithinWktPolygonQuery");
    }

    @Test
    public void testCswCompoundAfterDateAndIntersectingWktPolygon() throws Exception {

        performQueryAndValidateExpectedResults("CswCompoundAfterDateAndIntersectingWktPolygon");
    }

    @Test
    public void testCswCompoundBeforeDateAndLikeText() throws Exception {

        performQueryAndValidateExpectedResults("CswCompoundBeforeDateAndLikeText");
    }

    @Test
    public void testCswCompoundNotBeforeDateAndLikeText() throws Exception {

        performQueryAndValidateExpectedResults("CswCompoundNotBeforeDateAndLikeText");
    }

    @Test
    public void testCswCompoundLikeTextAndIntersectingWktLineString() throws Exception {

        performQueryAndValidateExpectedResults("CswCompoundLikeTextAndIntersectingWktLineString");
    }

    @Test
    public void testCswLogicalOperatorContextualNotQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswLogicalOperatorContextualNotQuery");
    }

    @Test
    public void testCswLogicalOperatorContextualOrQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswLogicalOperatorContextualOrQuery");
    }

    @Test
    public void testCswXPathExpressionQueryWithAttributeSelector() throws Exception {

        performQueryAndValidateExpectedResults("CswXPathExpressionQueryWithAttributeSelector");
    }

    @Test
    public void testCswXPathExpressionQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswXPathExpressionQuery");
    }

    @Test
    public void testCswCompoundNot() throws Exception {

        performQueryAndValidateExpectedResults("CswCompoundNot");
    }

    @Test
    public void testCswFuzzyTextQuery() throws Exception {

        performQueryAndValidateExpectedResults("CswFuzzyTextQuery");
    }

    @Test
    @ConditionalIgnore(condition = SkipUnstableTest.class) //DDF-3032
    public void testGeoCoderPlugin() throws Exception {
        getServiceManager().startFeature(true, "webservice-gazetteer");
        String id = ingestCswRecord(getFileContent(CSW_RECORD_RESOURCE_PATH + "/CswRecord2"));

        String queryUrl = REST_PATH.getUrl() + "sources/ddf.distribution/" + id + "?format=xml";

        when().get(queryUrl)
                .then()
                .log()
                .all()
                .and()
                .assertThat()
                .body(hasXPath(
                        "/metacard/string[@name='" + Location.COUNTRY_CODE + "']/value/text()",
                        equalTo(CSW_RESPONSE_COUNTRY_CODE)));
    }

    /**
     * Ingests data, performs and validates the query returns the correct results.
     *
     * @param queryType - The query that is performed
     * @throws Exception
     */
    private void performQueryAndValidateExpectedResults(String queryType) throws Exception {
        ingestMetacards(metacardIds);

        String cswQuery = savedCswQueries.get(queryType);

        String cswResponse = sendCswQuery(cswQuery);

        hasExpectedResults(cswResponse, cswExpectedResults.get(queryType));
    }

    private String sendCswQuery(String query) {
        RequestSpecification queryRequest = given().body(query);

        queryRequest = queryRequest.header("Content-Type", TEXT_XML_UTF_8);

        return queryRequest.when()
                .log()
                .all()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(equalTo(HttpStatus.SC_OK))
                .extract()
                .response()
                .getBody()
                .asString();
    }

    /**
     * Validates that the results from the query are correct.
     *
     * @param queryResult    - The result obtained from sending the query
     * @param expectedValues - The values expected within the results
     * @throws Exception
     */
    private void hasExpectedResults(String queryResult, ExpectedResultPair[] expectedValues)
            throws Exception {
        if (expectedValues[0].type == ResultType.COUNT) {
            assertTrue("The responses contained a different count",
                    hasExpectedResultCount(queryResult, expectedValues[0]));
        } else if (expectedValues[0].type == ResultType.TITLE) {
            //assertion done within the method
            hasExpectedMetacardsReturned(queryResult, expectedValues);
        } else {
            assertTrue("The expected values are an invalid type", false);
        }
    }

    /**
     * Validates that the query returned the expected metacards.
     *
     * @param queryResult    - The result obtained from sending the query
     * @param expectedValues - The values expected within the result
     * @return
     * @throws Exception
     */
    private boolean hasExpectedMetacardsReturned(String queryResult,
            ExpectedResultPair[] expectedValues) throws Exception {
        boolean testPassed = false;

        for (int i = 0; i < expectedValues.length; i++) {
            assertTrue("Metacard: " + expectedValues[i].value + " not found in result.",
                    testPassed = queryResult.contains(metacardIds.get(expectedValues[i].value)));
        }
        return testPassed;
    }

    /**
     * Validates that the query returned the expected result count.
     *
     * @param queryResult   - The result obtained from sending the query
     * @param expectedValue - The values expected within the result
     * @return
     * @throws Exception
     */
    private boolean hasExpectedResultCount(String queryResult, ExpectedResultPair expectedValue)
            throws Exception {

        String originalCount = XmlSearch.evaluate("//@numberOfRecordsMatched", queryResult);

        return originalCount.equals(expectedValue.value);
    }

    public enum ResultType {
        TITLE, COUNT
    }

    public class ExpectedResultPair {
        String value;

        ResultType type;

        public ExpectedResultPair(ResultType type, String value) {
            this.value = value;
            this.type = type;
        }
    }
}