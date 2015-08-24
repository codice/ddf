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
 **/
package ddf.catalog.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.xml.sax.InputSource;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import ddf.common.test.BeforeExam;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {

    private static final String CSW_ENDPOINT = SERVICE_ROOT + "/csw";

    private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

    public static void deleteMetacard(String id) {
        LOGGER.info("Deleting metacard {}", id);
        delete(REST_PATH + id).then().assertThat().statusCode(200).log().all();
    }

    public static String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }

    public static String ingest(String data, String mimeType) {
        LOGGER.info("Ingesting data of type {}:\n{}", mimeType, data);
        return given().body(data).header("Content-Type", mimeType).expect().log().all()
                .statusCode(201).when().post(REST_PATH).getHeader("id");
    }

    @BeforeExam
    public void beforeExam() throws Exception {
        setLogLevels();
        waitForAllBundles();
        waitForCatalogProvider();
        waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
    }

    @Test
    public void testMetacardTransformersFromRest() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = REST_PATH + id;
        LOGGER.info("Getting response to {}", url);
        when().get(url).then().log().all().assertThat()
                .body(hasXPath("/metacard[@id='" + id + "']"));

        deleteMetacard(id);
    }

    @Test
    public void testOpenSearchQuery() throws IOException {
        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");
        String id3 = ingestXmlFromResource("/metacard3.xml");
        String id4 = ingestXmlFromResource("/metacard4.xml");

        // Test xml-format response for an all-query
        ValidatableResponse response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(String.format(METACARD_X_PATH, id1)))
                .body(hasXPath(String.format(METACARD_X_PATH, id2)))
                .body(hasXPath(String.format(METACARD_X_PATH, id3)))
                .body(hasXPath(String.format(METACARD_X_PATH, id4)));

        // Execute a text search against a value in an indexed field (metadata)
        response = executeOpenSearch("xml", "q=dunder*");
        response.body(hasXPath(String.format(METACARD_X_PATH, id3)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id1))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id2))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id4))));

        // Execute a text search against a value that isn't in any indexed fields
        response = executeOpenSearch("xml", "q=whatisthedealwithairlinefood");
        response.body("metacards.metacard.size()", equalTo(0));

        // Execute a geo search that should match a point card
        response = executeOpenSearch("xml", "lat=40.689", "lon=-74.045", "radius=250");
        response.body(hasXPath(String.format(METACARD_X_PATH, id1)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id2))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id3))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id4))));

        // Execute a geo search...this should match two cards, both polygons around the Space Needle
        response = executeOpenSearch("xml", "lat=47.62", "lon=-122.356", "radius=500");
        response.body(hasXPath(String.format(METACARD_X_PATH, id2)))
                .body(hasXPath(String.format(METACARD_X_PATH, id4)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id1))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id3))));

        deleteMetacard(id1);
        deleteMetacard(id2);
        deleteMetacard(id3);
        deleteMetacard(id4);
    }

    private Response ingestCswRecord() {
        return given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswIngest()).post(CSW_ENDPOINT);
    }

    private void deleteMetacard(Response response) throws IOException, XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String idPath = "//*[local-name()='identifier']/text()";
        InputSource xml = new InputSource(IOUtils.toInputStream(response.getBody().asString(),
                StandardCharsets.UTF_8.name()));
        String id = xPath.compile(idPath).evaluate(xml);
        deleteMetacard(id);
    }

    @Test
    public void testCswIngest() {
        Response response = ingestCswRecord();
        ValidatableResponse validatableResponse = response.then();

        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/InsertResult/BriefRecord/title",
                                is("Aliquam fermentum purus quis arcu")),
                        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswDeleteOneWithFilter() {
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswFilterDelete()).post(CSW_ENDPOINT).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteOneWithCQL() {
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDelete()).post(CSW_ENDPOINT).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteNone() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDeleteNone()).post(CSW_ENDPOINT).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCombinedCswIngestAndDelete() {
        // This record will be deleted with the <Delete> in the next transaction request.
        ingestCswRecord();

        // The record being inserted in this transaction request will be deleted at the end of the
        // test.
        Response response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCombinedCswInsertAndDelete()).post(CSW_ENDPOINT);
        ValidatableResponse validatableResponse = response.then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswDeleteMultiple() {
        ingestCswRecord();
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswFilterDelete()).post(CSW_ENDPOINT).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("2")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testFilterPlugin() {
        try {
            // Ingest the metacard
            String id1 = ingestXmlFromResource("/metacard1.xml");
            String xPath = String.format(METACARD_X_PATH, id1);

            // Test without filtering
            ValidatableResponse response = executeOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

            startFeature(true, "sample-filter");
            startFeature(true, "filter-plugin");

            // Configure the PDP
            PdpProperties pdpProperties = new PdpProperties();
            pdpProperties.put("matchAllMappings",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=point-of-contact");
            Configuration config = configAdmin
                    .getConfiguration("ddf.security.pdp.realm.SimpleAuthzRealm", null);
            Dictionary<String, ?> configProps = new Hashtable<>(pdpProperties);
            config.update(configProps);
            waitForAllBundles();

            // Test with filtering with out point-of-contact
            response = executeOpenSearch("xml", "q=*");
            response.body(not(hasXPath(xPath)));

            // Test filtering with point of contact
            configureRestForBasic();

            response = executeAdminOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

            configureRestForAnon();

            stopFeature(true, "sample-filter");
            stopFeature(true, "filter-plugin");

            deleteMetacard(id1);
        } catch (Exception e) {
            LOGGER.error("Couldn't start filter plugin");
        }
    }

    private void configureRestForBasic() throws IOException, InterruptedException {
        PolicyProperties policyProperties = new PolicyProperties();
        policyProperties.put("authenticationTypes",
                "/=SAML|basic,/admin=SAML|basic,/jolokia=SAML|basic,/system=SAML|basic,/solr=SAML|PKI|basic");
        policyProperties.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + SERVICE_ROOT
                        + "/sdk/SoapService");
        Configuration config = configAdmin.getConfiguration(PolicyProperties.FACTORY_PID, null);
        Dictionary<String, ?> configProps = new Hashtable<>(policyProperties);
        config.update(configProps);
        waitForAllBundles();
    }

    private void configureRestForAnon() throws IOException, InterruptedException {
        PolicyProperties policyProperties = new PolicyProperties();
        policyProperties.put("authenticationTypes",
                "/=SAML|anon,/admin=SAML|anon,/jolokia=SAML|anon,/system=SAML|anon,/solr=SAML|PKI|anon");
        policyProperties.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + SERVICE_ROOT
                        + "/sdk/SoapService");
        Configuration config = configAdmin.getConfiguration(PolicyProperties.FACTORY_PID, null);
        Dictionary<String, ?> configProps = new Hashtable<>(policyProperties);
        config.update(configProps);
        waitForAllBundles();
    }

    private ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH).append("?").append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url).then();
    }

    private ValidatableResponse executeAdminOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH).append("?").append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return given().auth().basic("admin", "admin").when().get(url).then();
    }

    protected String ingestXmlFromResource(String resourceName) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream(resourceName), writer);
        return ingest(writer.toString(), "text/xml");
    }

    public class PdpProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "security-pdp-authzrealm";

        public static final String FACTORY_PID = "ddf.security.pdp.realm.SimpleAuthzRealm";

        public PdpProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }

    }

    public class PolicyProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "security-policy-context";

        public static final String FACTORY_PID = "org.codice.ddf.security.policy.context.impl.PolicyManager";

        public PolicyProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }

    }
}
