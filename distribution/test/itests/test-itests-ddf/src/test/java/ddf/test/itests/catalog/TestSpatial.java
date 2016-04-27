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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.FrameworkUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.ValidatableResponse;
import com.jayway.restassured.specification.RequestSpecification;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.Library;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSpatial extends AbstractIntegrationTest {

    private static final String CSW_RESOURCE_ROOT = "/TestSpatial/";

    private static final String CSW_QUERY_RESOURCES = CSW_RESOURCE_ROOT + "csw/request/query/";

    private static final DynamicUrl CSW_ENDPOINT_URL = new DynamicUrl(SERVICE_ROOT, "/csw");

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
                            new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE,
                                    CSW_METACARD)})
                    .build();

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();

            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();

            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");

            loadResourceQueries(CSW_QUERY_RESOURCES, savedCswQueries);
        } catch (Exception e) {
            LOGGER.error("Failed to start required apps:", e);
            fail("Failed to start required apps: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        if (metacardIds != null) {
            for (String metacardId : metacardIds.values()) {
                TestCatalog.deleteMetacard(metacardId);
            }
            metacardIds.clear();
        }
    }

    @Test
    public void testCswAfterDateQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswAfterDateQuery");
    }

    @Test
    public void testCswBeforeDateQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswBeforeDateQuery");
    }

    @Test
    public void testCswContainingWktLineStringQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswContainingWktLineStringQuery");
    }

    @Test
    public void testCswContainingWktPolygonQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswContainingWktPolygonQuery");
    }

    @Test
    public void testCswDuringDatesQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswDuringDatesQuery");
    }

    @Test
    public void testCswEqualToTextQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswEqualToTextQuery");
    }

    @Test
    public void testCswIntersectingWktLineStringQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswIntersectingWktLineStringQuery");
    }

    @Test
    public void testCswIntersectingWktPolygonQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswIntersectingWktPolygonQuery");
    }

    @Test
    public void testCswLikeTextQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswLikeTextQuery");
    }

    @Test
    public void testCswNearestToWktLineStringQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswNearestToWktLineStringQuery");
    }

    @Test
    public void testCswNearestToWktPolygonQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswNearestToWktPolygonQuery");
    }

    @Test
    public void testCswOverLappingDatesQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswOverLappingDatesQuery");
    }

    @Test
    public void testCswWithinBufferWktLineStringQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswWithinBufferWktLineStringQuery");
    }

    @Test
    public void testCswWithinBufferWktPolygonQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswWithinBufferWktPolygonQuery");
    }

    @Test
    public void testCswWithinWktPolygonQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswWithinWktPolygonQuery");
    }

    @Test
    public void testCswCompoundAfterDateAndIntersectingWktPolygon()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswCompoundAfterDateAndIntersectingWktPolygon");
    }

    @Test
    public void testCswCompoundBeforeDateAndLikeText()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswCompoundBeforeDateAndLikeText");
    }

    @Test
    public void testCswCompoundLikeTextAndIntersectingWktLineString()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswCompoundLikeTextAndIntersectingWktLineString");
    }

    @Test
    public void testCswLogicalOperatorContextualNotQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswLogicalOperatorContextualNotQuery");
    }

    @Test
    public void testCswLogicalOperatorContextualOrQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswLogicalOperatorContextualOrQuery");
    }

    @Test
    public void testCswXPathExpressionQueryWithAttributeSelector()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswXPathExpressionQueryWithAttributeSelector");
    }

    @Test
    public void testCswXPathExpressionQuery()
            throws XPathException, ParserConfigurationException, SAXException, IOException {

        performQueryAndValidateExpectedResults("CswXPathExpressionQuery");
    }

    /**
     * Ingests data, performs and validates the query returns the correct results.
     *
     * @param queryType - The query that is performed
     * @throws XPathException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void performQueryAndValidateExpectedResults(String queryType)
            throws XPathException, ParserConfigurationException, SAXException, IOException {
        ingestMetacards(metacardIds);

        String cswQuery = savedCswQueries.get(queryType);

        String cswResponse = sendCswQuery(cswQuery);

        hasExpectedResults(cswResponse, cswExpectedResults.get(queryType));
    }

    /**
     * Loads the resource queries into memory.
     */
    public static Map<String, String> loadResourceQueries(String resourcesPath,
            Map<String, String> savedQueries) {

        //gets a list of resources available within the resource bundle
        Enumeration<URL> queryResourcePaths = FrameworkUtil.getBundle(TestSpatial.class)
                .getBundleContext()
                .getBundle()
                .findEntries(resourcesPath, "*", false);

        while (queryResourcePaths.hasMoreElements()) {
            String queryResourcePath = queryResourcePaths.nextElement()
                    .getPath();
            if (!queryResourcePath.endsWith("/")) {
                String queryName = queryResourcePath.substring(
                        queryResourcePath.lastIndexOf("/") + 1);
                savedQueries.put(removeFileExtension(queryName),
                        Library.getFileContent(queryResourcePath));
            }
        }
        return savedQueries;
    }

    public static String ingestCswRecord(String cswRecord) {

        String transactionRequest = Library.getCswInsert("csw:Record", cswRecord);

        ValidatableResponse response = given().log()
                .all()
                .body(transactionRequest)
                .header("Content-Type", MediaType.APPLICATION_XML)
                .when()
                .post(CSW_ENDPOINT_URL.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(equalTo(HttpStatus.SC_OK));

        return response.extract()
                .body()
                .xmlPath()
                .get("Transaction.InsertResult.BriefRecord.identifier")
                .toString();
    }

    public static Map<String, String> ingestMetacards(Map<String, String> metacardsIds) {
        //ingest csw
        String cswRecordId = ingestCswRecord(Library.getFileContent(
                CSW_RESOURCE_ROOT + "csw/record/CswRecord.xml"));
        metacardsIds.put(CSW_METACARD, cswRecordId);

        //ingest xml
        String plainXmlNearId = TestCatalog.ingest(Library.getFileContent(
                CSW_RESOURCE_ROOT + "xml/PlainXmlNear.xml"), MediaType.TEXT_XML);
        String plainXmlFarId = TestCatalog.ingest(Library.getFileContent(
                CSW_RESOURCE_ROOT + "xml/PlainXmlFar.xml"), MediaType.TEXT_XML);
        metacardsIds.put(PLAINXML_NEAR_METACARD, plainXmlNearId);
        metacardsIds.put(PLAINXML_FAR_METACARD, plainXmlFarId);

        //ingest json
        String geoJsonNearId = TestCatalog.ingestGeoJson(Library.getFileContent(
                CSW_RESOURCE_ROOT + "json/GeoJsonNear.json"));
        String geoJsonFarId = TestCatalog.ingestGeoJson(Library.getFileContent(
                CSW_RESOURCE_ROOT + "json/GeoJsonFar.json"));
        metacardsIds.put(GEOJSON_NEAR_METACARD, geoJsonNearId);
        metacardsIds.put(GEOJSON_FAR_METACARD, geoJsonFarId);

        return metacardsIds;
    }

    private static String removeFileExtension(String file) {
        return file.contains(".") ? file.substring(0, file.lastIndexOf(".")) : file;
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
     * @throws XPathException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private void hasExpectedResults(String queryResult, ExpectedResultPair[] expectedValues)
            throws XPathException, ParserConfigurationException, SAXException, IOException {
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
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws XPathExpressionException
     */
    private boolean hasExpectedMetacardsReturned(String queryResult,
            ExpectedResultPair[] expectedValues)
            throws IOException, SAXException, ParserConfigurationException,
            XPathExpressionException {
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
     * @throws XPathException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    private boolean hasExpectedResultCount(String queryResult, ExpectedResultPair expectedValue)
            throws XPathException, IOException, SAXException, ParserConfigurationException {

        XPathExpression cswXPathExpression = getXPath().compile("//@numberOfRecordsMatched");

        String originalCount = cswXPathExpression.evaluate(getDoc(queryResult));

        return originalCount.equals(expectedValue.value);
    }

    private static XPath getXPath() {
        return XPathFactory.newInstance()
                .newXPath();
    }

    private static Document getDoc(String input)
            throws ParserConfigurationException, IOException, SAXException {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(input.getBytes()));
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