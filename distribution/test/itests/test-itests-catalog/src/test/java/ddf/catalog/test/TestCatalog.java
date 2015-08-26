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
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import static ddf.catalog.test.SecurityPolicyConfigurator.configureRestForAnonymous;
import static ddf.catalog.test.SecurityPolicyConfigurator.configureRestForBasic;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

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

    @Test
    public void testCswIngest() {
        ValidatableResponse response = given().header("Content-Type", "application/xml")
                .body(Library.getCswIngest()).post(CSW_ENDPOINT).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/title",
                        is("Aliquam fermentum purus quis arcu")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));
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
            configureRestForBasic(getAdminConfig(), getServiceManager(), SERVICE_ROOT);

            response = executeAdminOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

            configureRestForAnonymous(getAdminConfig(), getServiceManager(), SERVICE_ROOT);

            stopFeature(true, "sample-filter");
            stopFeature(true, "filter-plugin");

            deleteMetacard(id1);
        } catch (Exception e) {
            LOGGER.error("Couldn't start filter plugin");
        }
    }

    @Test
    public void testContentDirectoryMonitor() throws Exception {
        startFeature(true, "content-core-directorymonitor");
        final String TMP_PREFIX = "tcdm_";
        Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
        tmpDir.toFile().deleteOnExit();
        Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
        tmpFile.toFile().deleteOnExit();
        Files.copy(this.getClass().getClassLoader().getResourceAsStream("metacard5.xml"), tmpFile,
                StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> cdmProperties = new HashMap<>();
        cdmProperties.putAll(getMetatypeDefaults("content-core-directorymonitor",
                "ddf.content.core.directorymonitor.ContentDirectoryMonitor"));
        cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/"); // Must end with /
        cdmProperties.put("directive", "STORE_AND_PROCESS");
        createManagedService("ddf.content.core.directorymonitor.ContentDirectoryMonitor",
                cdmProperties);

        long startTime = System.nanoTime();
        ValidatableResponse response = null;
        do {
            response = executeOpenSearch("xml", "q=*SysAdmin*");
            if (response.extract().xmlPath().getList("metacards.metacard").size() == 1) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        } while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) < TimeUnit.MINUTES
                .toMillis(1));
        response.body("metcards.metacard.size()", equalTo(1));
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
}
