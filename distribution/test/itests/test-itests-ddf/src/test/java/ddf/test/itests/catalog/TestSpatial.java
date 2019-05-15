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
package ddf.test.itests.catalog;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static ddf.catalog.Constants.DEFAULT_PAGE_SIZE;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestCswRecord;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestGeoJson;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.specification.RequestSpecification;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.server.StubServer;
import ddf.catalog.data.types.Location;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.XmlSearch;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule.ConditionalIgnore;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.FrameworkUtil;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSpatial extends AbstractIntegrationTest {

  private static final String CSW_RESPONSE_COUNTRY_CODE = "GBR";

  private static final String CSW_RESOURCE_ROOT = "/TestSpatial/";

  private static final String CSW_QUERY_RESOURCES = CSW_RESOURCE_ROOT + "csw/request/query/";

  private static final String CSW_PAGING_METACARD =
      CSW_RESOURCE_ROOT + "csw/record/CswPagingRecord.xml";

  private static final String CSW_METACARD = "CswRecord.xml";

  private static final String GEOJSON_NEAR_METACARD = "GeoJson near";

  private static final String GEOJSON_FAR_METACARD = "GeoJson far";

  private static final String PLAINXML_NEAR_METACARD = "PlainXml near";

  private static final String PLAINXML_FAR_METACARD = "PlainXml far";

  private static final String TEXT_XML_UTF_8 = "text/xml;charset=UTF-8";

  private static final String WFS_11_SYMBOLIC_NAME = "spatial-wfs-v1_1_0-source";

  private static final String WFS_11_FACTORY_PID = "Wfs_v110_Federated_Source";

  private static final String WFS_11_SOURCE_ID = "WFS 1.1 Source";

  private static final String WFS_11_CONTEXT = "/mockWfs/11";

  private String restitoStubServerPath;

  private StubServer server;

  private static Map<String, String> savedCswQueries = new HashMap<>();

  private static Map<String, String> metacardIds = new HashMap<>();

  private final ImmutableMap<String, ExpectedResultPair[]> cswExpectedResults =
      ImmutableMap.<String, ExpectedResultPair[]>builder()
          .put(
              "CswAfterDateQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswBeforeDateQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_FAR_METACARD)
              })
          .put(
              "CswContainingWktLineStringQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)
              })
          .put(
              "CswContainingWktPolygonQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_FAR_METACARD)
              })
          .put(
              "CswDuringDatesQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)
              })
          .put(
              "CswEqualToTextQuery",
              new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
          .put(
              "CswIntersectingWktLineStringQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswIntersectingWktPolygonQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswLikeTextQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswNearestToWktLineStringQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswNearestToWktPolygonQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD),
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswOverLappingDatesQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswWithinBufferWktLineStringQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswWithinBufferWktPolygonQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswWithinWktPolygonQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswCompoundLikeTextAndIntersectingWktLineString",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswCompoundAfterDateAndIntersectingWktPolygon",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_NEAR_METACARD)
              })
          .put(
              "CswCompoundBeforeDateAndLikeText",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_FAR_METACARD)
              })
          .put(
              "CswCompoundNotBeforeDateAndLikeText",
              new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
          .put(
              "CswLogicalOperatorContextualNotQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD)
              })
          .put(
              "CswLogicalOperatorContextualOrQuery",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_NEAR_METACARD),
                new ExpectedResultPair(ResultType.TITLE, GEOJSON_FAR_METACARD)
              })
          .put(
              "CswXPathExpressionQueryWithAttributeSelector",
              new ExpectedResultPair[] {
                new ExpectedResultPair(ResultType.TITLE, PLAINXML_FAR_METACARD)
              })
          .put(
              "CswXPathExpressionQuery",
              new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
          .put(
              "CswFuzzyTextQuery",
              new ExpectedResultPair[] {new ExpectedResultPair(ResultType.TITLE, CSW_METACARD)})
          .put(
              "CswCompoundNot",
              new ExpectedResultPair[] {new ExpectedResultPair(ResultType.COUNT, "0")})
          .build();

  @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

  /** Loads the resource queries into memory. */
  public static Map<String, String> loadResourceQueries(
      String resourcesPath, Map<String, String> savedQueries) {

    // gets a list of resources available within the resource bundle
    Enumeration<URL> queryResourcePaths =
        FrameworkUtil.getBundle(AbstractIntegrationTest.class)
            .getBundleContext()
            .getBundle()
            .findEntries(resourcesPath, "*", false);

    while (queryResourcePaths.hasMoreElements()) {
      String queryResourcePath = queryResourcePaths.nextElement().getPath();
      if (!queryResourcePath.endsWith("/")) {
        String queryName = queryResourcePath.substring(queryResourcePath.lastIndexOf("/") + 1);
        savedQueries.put(removeFileExtension(queryName), getFileContent(queryResourcePath));
      }
    }
    return savedQueries;
  }

  private static String removeFileExtension(String file) {
    return file.contains(".") ? file.substring(0, file.lastIndexOf(".")) : file;
  }

  public static Map<String, String> ingestMetacards(Map<String, String> metacardsIds) {
    // ingest csw
    String cswRecordId =
        ingestCswRecord(getFileContent(CSW_RESOURCE_ROOT + "csw/record/CswRecord.xml"));
    metacardsIds.put(CSW_METACARD, cswRecordId);

    // ingest xml
    String plainXmlNearId =
        ingest(getFileContent(CSW_RESOURCE_ROOT + "xml/PlainXmlNear.xml"), MediaType.TEXT_XML);
    String plainXmlFarId =
        ingest(getFileContent(CSW_RESOURCE_ROOT + "xml/PlainXmlFar.xml"), MediaType.TEXT_XML);
    metacardsIds.put(PLAINXML_NEAR_METACARD, plainXmlNearId);
    metacardsIds.put(PLAINXML_FAR_METACARD, plainXmlFarId);

    // ingest json
    String geoJsonNearId =
        ingestGeoJson(getFileContent(CSW_RESOURCE_ROOT + "json/GeoJsonNear.json"));
    String geoJsonFarId = ingestGeoJson(getFileContent(CSW_RESOURCE_ROOT + "json/GeoJsonFar.json"));
    metacardsIds.put(GEOJSON_NEAR_METACARD, geoJsonNearId);
    metacardsIds.put(GEOJSON_FAR_METACARD, geoJsonFarId);

    return metacardsIds;
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
      getSecurityPolicy().configureRestForGuest();
      waitForSystemReady();
      setupMockServer();

      getCatalogBundle().waitForFederatedSource(WFS_11_SOURCE_ID);
      getServiceManager().waitForSourcesToBeAvailable(REST_PATH.getUrl(), WFS_11_SOURCE_ID);

      loadResourceQueries(CSW_QUERY_RESOURCES, savedCswQueries);
      getServiceManager().startFeature(true, "spatial-wps");
      getServiceManager().startFeature(true, "sample-process");

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed to start required apps: ");
    }
  }

  @Before
  public void setup() throws Exception {
    clearCatalogAndWait();
  }

  private void setupMockServer() throws IOException {
    DynamicPort restitoStubServerPort =
        new DynamicPort("org.codice.ddf.system.restito_stub_server_port", 6);
    restitoStubServerPath = DynamicUrl.INSECURE_ROOT + restitoStubServerPort.getPort();
    server = new StubServer(Integer.parseInt(restitoStubServerPort.getPort())).run();
    setupWfs11(restitoStubServerPort.getPort());
  }

  private void setupWfs11(String mockServerPort) throws IOException {
    FederatedSourceProperties wfs11SourceProperties =
        new FederatedSourceProperties(
            WFS_11_SOURCE_ID, WFS_11_CONTEXT, WFS_11_SYMBOLIC_NAME, WFS_11_FACTORY_PID, "wfsUrl");

    String wfs11GetCapabilities =
        getFileContent("/TestSpatial/xml/WFS_11_GetCapabilities.xml")
            .replaceAll("\\$\\{PORT}", mockServerPort);
    String wfs11sfRoadsFeatureType =
        getFileContent("/TestSpatial/xml/WFS_11_sfRoadsFeatureType.xsd");
    String sfRoad1 = getFileContent("/TestSpatial/xml/sfRoad1.xml");
    String sfRoad2 = getFileContent("/TestSpatial/xml/sfRoad2.xml");
    String sfRoad3 = getFileContent("/TestSpatial/xml/sfRoad3.xml");

    setupWfs11GetCapabilities(wfs11GetCapabilities);
    setupWfs11DescribeFeature(wfs11sfRoadsFeatureType);
    // wildcard
    setupWfs11Query(wfsResponse(sfRoad1, sfRoad2, sfRoad3), withPostBodyContaining("Literal>*"));

    // keyword
    setupWfs11Query(wfsResponse(sfRoad1), withPostBodyContaining(">roads.1"));

    // boolean
    setupWfs11Query(wfsResponse(sfRoad2, sfRoad3), withPostBodyContaining("And>"));

    // geometry
    setupWfs11Query(wfsResponse(sfRoad1), withPostBodyContaining("coordinates>"));

    // ID Search
    whenHttp(server)
        .match(post(WFS_11_CONTEXT), withPostBodyContaining("FeatureId"))
        .then(Action.success(), Action.stringContent(wfsResponse(sfRoad1)));

    getServiceManager().createManagedService(WFS_11_FACTORY_PID, wfs11SourceProperties);
  }

  private void setupWfs11GetCapabilities(String getCapabilities) {
    whenHttp(server)
        .match(
            get(WFS_11_CONTEXT),
            parameter("service", "WFS"),
            parameter("version", "1.1.0"),
            parameter("request", "GetCapabilities"))
        .then(Action.success(), Action.stringContent(getCapabilities));
  }

  private void setupWfs11DescribeFeature(String featureDescription) {
    whenHttp(server)
        .match(
            get(WFS_11_CONTEXT),
            parameter("service", "WFS"),
            parameter("version", "1.1.0"),
            parameter("request", "DescribeFeatureType"))
        .then(Action.success(), Action.stringContent(featureDescription));
  }

  private void setupWfs11Query(String response, Condition... conditions) {
    List<Condition> queryConditions = new ArrayList<>();
    queryConditions.add(post(WFS_11_CONTEXT));
    queryConditions.add(withPostBodyContaining("GetFeature"));
    queryConditions.add(withPostBodyContaining("sf:roads"));
    queryConditions.addAll(Arrays.asList(conditions));

    whenHttp(server)
        .match(queryConditions.toArray(new Condition[0]))
        .then(Action.success(), Action.stringContent(response));
  }

  @After
  public void tearDown() throws Exception {
    clearCatalog();
  }

  @Test
  public void testCswPagingQuery() throws Exception {
    // Set to internal paging size
    int pageSize = 500;
    Set<String> ingestIds = ingestPagingRecords(pageSize + 11);
    int ingestCount = ingestIds.size();
    assertThat("Ingest count not equal to expected", ingestCount, is(pageSize + 11));

    ImmutableList<Integer> maxSizes =
        ImmutableList.of(
            DEFAULT_PAGE_SIZE - 5,
            DEFAULT_PAGE_SIZE,
            pageSize - 5,
            pageSize,
            pageSize + 7,
            ingestCount,
            ingestCount + 1);

    for (Integer maxSize : maxSizes) {
      String query = getPagingMaxRecordsQuery(maxSize);
      String cswResponse = sendCswQuery(query);

      int expectedResults = (maxSize <= ingestCount) ? maxSize : ingestCount;

      assertTrue(
          "The responses contained a different number of matches; expected " + ingestCount,
          hasExpectedMatchCount(
              cswResponse, new ExpectedResultPair(ResultType.COUNT, ingestCount + "")));

      assertTrue(
          "The responses contained a different result count; expected " + expectedResults,
          hasExpectedResultCount(
              cswResponse, new ExpectedResultPair(ResultType.COUNT, expectedResults + "")));
    }
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
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-3032
  public void testGeoCoderPlugin() throws Exception {
    getServiceManager().startFeature(true, "webservice-gazetteer");
    String id = ingestCswRecord(getFileContent(CSW_RECORD_RESOURCE_PATH + "/CswRecord2"));

    String queryUrl = REST_PATH.getUrl() + "sources/ddf.distribution/" + id + "?format=xml";

    when()
        .get(queryUrl)
        .then()
        .and()
        .assertThat()
        .body(
            hasXPath(
                "/metacard/string[@name='" + Location.COUNTRY_CODE + "']/value/text()",
                equalTo(CSW_RESPONSE_COUNTRY_CODE)));
  }

  @Test
  public void testWpsGetCapabilities() throws Exception {
    given()
        .get(SERVICE_ROOT.getUrl() + "/wps?service=WPS&request=GetCapabilities")
        .then()
        .assertThat()
        .body(
            hasXPath(
                "count(/*[local-name()='Capabilities']/*[local-name()='Contents']/*[local-name()='ProcessSummary'])",
                Matchers.is("5")),
            hasXPath(
                "/*[local-name()='Capabilities']/*[local-name()='Contents']/*[local-name()='ProcessSummary']/*[local-name()='Identifier' and text()='geojson']"));
  }

  @Test
  public void testWpsDescribeProcess() throws Exception {
    given()
        .get(SERVICE_ROOT.getUrl() + "/wps?service=WPS&request=DescribeProcess&identifier=geojson")
        .then()
        .assertThat()
        .body(
            hasXPath(
                "count(/*[local-name()='ProcessOfferings']/*[local-name()='ProcessOffering'])",
                Matchers.is("1")),
            hasXPath(
                "/*[local-name()='ProcessOfferings']/*[local-name()='ProcessOffering']/*[local-name()='Process']/*[local-name()='Identifier' and text()='geojson']"));
  }

  @Test
  public void testWpsExecute() throws Exception {
    ingestMetacards(metacardIds);
    final String requestXml =
        getFileContent(
            "ExecuteRequest.xml",
            ImmutableMap.of(
                "id", "csw:Record", "metacardId", metacardIds.get(PLAINXML_NEAR_METACARD)),
            AbstractIntegrationTest.class);
    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(requestXml)
        .post(SERVICE_ROOT.getUrl() + "/wps?service=WPS&request=Execute")
        .then()
        .assertThat()
        .body(
            hasXPath(
                "count(/*[local-name()='Result']/*[local-name()='Output'])", Matchers.is("1")));
  }

  @Test
  public void testWfs11Wildcard() {
    ingestMetacards(metacardIds);
    String queryUrl = OPENSEARCH_PATH + "?q=*&format=xml&src=" + WFS_11_SOURCE_ID;
    String responseXml = given().request().get(queryUrl).andReturn().body().asString();
    assertMetacards(responseXml, 3, WFS_11_SOURCE_ID, "roads");
  }

  @Test
  public void testWfs11Keyword() {
    ingestMetacards(metacardIds);
    String queryUrl = OPENSEARCH_PATH + "?q=roads.1&format=xml&src=" + WFS_11_SOURCE_ID;
    String responseXml = given().request().get(queryUrl).andReturn().body().asString();
    assertMetacards(responseXml, 1, WFS_11_SOURCE_ID, "roads");
  }

  @Test
  public void testWfs11Boolean() {
    ingestMetacards(metacardIds);
    String queryUrl = OPENSEARCH_PATH + "?q=roads.2 AND roads.3&format=xml&src=" + WFS_11_SOURCE_ID;
    String responseXml = given().request().get(queryUrl).andReturn().body().asString();
    assertMetacards(responseXml, 2, WFS_11_SOURCE_ID, "roads");
  }

  @Test
  public void testWfs11Id() {
    ingestMetacards(metacardIds);
    String queryUrl = REST_PATH.getUrl() + "sources/" + WFS_11_SOURCE_ID + "/roads.1";
    String responseXml = given().request().get(queryUrl).andReturn().body().asString();
    assertMetacard(responseXml, WFS_11_SOURCE_ID, "roads");
  }

  @Test
  public void testWfs11Geo() {
    ingestMetacards(metacardIds);
    String queryUrl =
        OPENSEARCH_PATH + "?lat=90&lon=90&radius=1000&format=xml&src=" + WFS_11_SOURCE_ID;
    String responseXml = given().request().get(queryUrl).andReturn().body().asString();
    assertMetacards(responseXml, 1, WFS_11_SOURCE_ID, "roads");
  }

  private String wfsResponse(String... featureMembers) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        .append("<wfs:FeatureCollection\n")
        .append("  xmlns:sf=\"http://www.openplans.org/spearfish\"\n")
        .append("  xmlns:wfs=\"http://www.opengis.net/wfs\"\n")
        .append("  xmlns:gml=\"http://www.opengis.net/gml\"\n")
        .append("  numberOfFeatures=\"")
        .append(featureMembers.length)
        .append("\"\n")
        .append("  timeStamp=\"2018-05-11T00:11:44.120Z\">\n")
        .append("  <gml:featureMembers>\n");

    for (String featureMember : featureMembers) {
      sb.append(featureMember).append("\n");
    }

    sb.append("</gml:featureMembers>\n").append("</wfs:FeatureCollection>");

    return sb.toString();
  }

  private void assertMetacard(String responseXml, String expectedSource, String expectedType) {
    XmlPath xmlPath = new XmlPath(responseXml);
    int numMetacards = xmlPath.get("metacard.size()");
    assertThat(numMetacards, equalTo(1));
    assertThat(xmlPath.get("metacard.type"), equalTo(expectedType));
    assertThat(xmlPath.get("metacard.source"), equalTo(expectedSource));
  }

  private void assertMetacards(
      String responseXml, int expectedNumMetacards, String expectedSource, String expectedType) {
    XmlPath xmlPath = new XmlPath(responseXml);
    int numMetacards = xmlPath.get("metacards.metacard.size()");
    assertThat(numMetacards, equalTo(expectedNumMetacards));

    for (int i = 0; i < numMetacards; i++) {
      assertThat(xmlPath.get("metacards.metacard[" + i + "].type"), equalTo(expectedType));
      assertThat(xmlPath.get("metacards.metacard[" + i + "].source"), equalTo(expectedSource));
    }
  }

  /**
   * Ingests data, performs and validates the query returns the correct results.
   *
   * @param queryType - The query that is performed
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

    return queryRequest
        .when()
        .post(CSW_PATH.getUrl())
        .then()
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
   * @param queryResult - The result obtained from sending the query
   * @param expectedValues - The values expected within the results
   */
  private void hasExpectedResults(String queryResult, ExpectedResultPair[] expectedValues)
      throws Exception {
    if (expectedValues[0].type == ResultType.COUNT) {
      assertTrue(
          "The responses contained a different count",
          hasExpectedResultCount(queryResult, expectedValues[0]));
    } else if (expectedValues[0].type == ResultType.TITLE) {
      // assertion done within the method
      hasExpectedMetacardsReturned(queryResult, expectedValues);
    } else {
      assertTrue("The expected values are an invalid type", false);
    }
  }

  /**
   * Validates that the query returned the expected metacards.
   *
   * @param queryResult - The result obtained from sending the query
   * @param expectedValues - The values expected within the result
   */
  private boolean hasExpectedMetacardsReturned(
      String queryResult, ExpectedResultPair[] expectedValues) throws Exception {
    boolean testPassed = false;

    for (int i = 0; i < expectedValues.length; i++) {
      assertTrue(
          "Metacard: " + expectedValues[i].value + " not found in result.",
          testPassed = queryResult.contains(metacardIds.get(expectedValues[i].value)));
    }
    return testPassed;
  }

  /**
   * Validates that the query matched the expected result count.
   *
   * @param queryResult - The result obtained from sending the query
   * @param expectedValue - The values expected within the result
   * @return true if the {@code numberOfRecordsMatched} matches the expected value
   * @throws Exception if an error occurs parsing the XML response
   */
  private boolean hasExpectedMatchCount(String queryResult, ExpectedResultPair expectedValue)
      throws Exception {

    String originalCount = XmlSearch.evaluate("//@numberOfRecordsMatched", queryResult);

    return originalCount.equals(expectedValue.value);
  }

  /**
   * Validates that the query returned the expected result count.
   *
   * @param queryResult - The result obtained from sending the query
   * @param expectedValue - The values expected within the result
   * @return true if the {@code numberOfRecordsReturned} matches the expected value
   * @throws Exception if an error occurs parsing the XML response
   */
  private boolean hasExpectedResultCount(String queryResult, ExpectedResultPair expectedValue)
      throws Exception {

    String originalCount = XmlSearch.evaluate("//@numberOfRecordsReturned", queryResult);

    return originalCount.equals(expectedValue.value);
  }

  private String getPagingMaxRecordsQuery(int maxRecords) {
    String rawCswQuery = savedCswQueries.get("CswPagingTestLikeQuery");
    StrSubstitutor strSubstitutor =
        new StrSubstitutor(ImmutableMap.of("maxRecords", "" + maxRecords));

    strSubstitutor.setVariablePrefix(RESOURCE_VARIABLE_DELIMETER);
    strSubstitutor.setVariableSuffix(RESOURCE_VARIABLE_DELIMETER);
    return strSubstitutor.replace(rawCswQuery);
  }

  private Set<String> ingestPagingRecords(int number) {
    String rawCswRecord = getFileContent(CSW_PAGING_METACARD);

    Set<String> pagingIds = new HashSet<>();
    for (int i = 1; i <= number; i++) {
      String identifier = UUID.randomUUID().toString().replaceAll("-", "");
      String cswPagingRecord = substitutePagingParams(rawCswRecord, i, identifier);

      String id = ingestCswRecord(cswPagingRecord);

      pagingIds.add(id);
      metacardIds.put(id, id);
    }

    return pagingIds;
  }

  private String substitutePagingParams(String rawCswRecord, int testNum, String identifier) {
    StrSubstitutor strSubstitutor =
        new StrSubstitutor(ImmutableMap.of("identifier", identifier, "testNum", "" + testNum));

    strSubstitutor.setVariablePrefix(RESOURCE_VARIABLE_DELIMETER);
    strSubstitutor.setVariableSuffix(RESOURCE_VARIABLE_DELIMETER);
    return strSubstitutor.replace(rawCswRecord);
  }

  public enum ResultType {
    TITLE,
    COUNT
  }

  public class ExpectedResultPair {

    String value;

    ResultType type;

    public ExpectedResultPair(ResultType type, String value) {
      this.value = value;
      this.type = type;
    }
  }

  public class FederatedSourceProperties extends HashMap<String, Object> {

    public FederatedSourceProperties(
        String sourceId,
        String context,
        String symbolicName,
        String factoryPid,
        String urlPropName) {
      this.putAll(getServiceManager().getMetatypeDefaults(symbolicName, factoryPid));

      this.put("id", sourceId);
      this.put(urlPropName, restitoStubServerPath + context);
      this.put("pollInterval", 1);
    }
  }
}
