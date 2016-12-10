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

import static org.codice.ddf.itests.common.csw.CswTestCommons.CSW_FEDERATED_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswSourceProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpStatus;
<<<<<<< HEAD
=======
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
>>>>>>> master
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.path.json.JsonPath;

<<<<<<< HEAD
import ddf.common.test.BeforeExam;
import ddf.common.test.catalog.CatalogIngest;
import ddf.test.itests.AbstractIntegrationTest;

=======
>>>>>>> master
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFanout extends AbstractIntegrationTest {

    private static final String LOCAL_SOURCE_ID = "ddf.distribution";

    // Using default resource tag as the one to blacklist
    private static final List<String> TAG_BLACKLIST = Collections.singletonList("resource");

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().setFanout(true);
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT.getUrl() + "/catalog/query?_wadl");

            LOGGER.info("Source status: \n{}", get(REST_PATH.getUrl() + "sources").body()
                    .prettyPrint());
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Before
    public void setup() throws IOException {
        // Start with empty blacklist
        getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
        // Start with fanout enabled
        getCatalogBundle().setFanout(true);
    }

    private void startCswSource() throws Exception {
        getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");
        getServiceManager().createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID,
                getCswSourceProperties(CSW_SOURCE_ID, CSW_PATH.getUrl(), getServiceManager()));
        getCatalogBundle().waitForFederatedSource(CSW_SOURCE_ID);
    }

    @Test
    public void fanoutQueryReturnsOnlyOneSource() throws Exception {
        startCswSource();
        try {
            String queryUrl = REST_PATH.getUrl() + "sources";

            String jsonBody = given().when()
                    .get(queryUrl)
                    .getBody()
                    .asString();
            JsonPath json = JsonPath.from(jsonBody);

            assertThat(json.getInt("size()"), equalTo(1));
            assertThat(json.getList("id", String.class), hasItem(LOCAL_SOURCE_ID));
        } finally {
            getServiceManager().stopManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID);
        }
    }

    @Test
    public void nonFanoutQueryReturnsMultipleSources() throws Exception {
        startCswSource();
        try {
            getCatalogBundle().setFanout(false);
            getCatalogBundle().waitForCatalogProvider();

            String queryUrl = REST_PATH.getUrl() + "sources";

            String jsonBody = given().when()
                    .get(queryUrl)
                    .getBody()
                    .asString();
            JsonPath json = JsonPath.from(jsonBody);

            assertThat(json.getInt("size()"), equalTo(2));
            assertThat(json.getList("id", String.class), hasItem(LOCAL_SOURCE_ID));
            assertThat(json.getList("id", String.class), hasItem(CSW_SOURCE_ID));
        } finally {
            getServiceManager().stopManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID);
        }
    }

    @Test
    public void testCswIngestWithFanoutEnabledAndEmptyBlacklist() throws Exception {
<<<<<<< HEAD
        String id = CatalogIngest.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl(),
                HttpStatus.SC_CREATED);
        CatalogIngest.deleteMetacard(id, REST_PATH.getUrl(), HttpStatus.SC_OK);
=======
        String id = CatalogTestCommons.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);
        CatalogTestCommons.deleteMetacard(id, HttpStatus.SC_OK);
>>>>>>> master
    }

    @Test
    public void testCswIngestFailsWithFanoutEnabledAndBlacklistSet() throws Exception {
        getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
<<<<<<< HEAD
        CatalogIngest.ingest("Some data to ingest. This should fail.",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl(),
=======
        CatalogTestCommons.ingest("Some data to ingest. This should fail.",
                MediaType.TEXT_PLAIN,
>>>>>>> master
                HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    public void testCswUpdateWorksWithFanoutEnabledAndEmptyBlacklist() throws IOException {
        getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
<<<<<<< HEAD
        String id = CatalogIngest.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl(),
                HttpStatus.SC_CREATED);
        CatalogIngest.update("Some data to update",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl() + id,
                HttpStatus.SC_OK);
        CatalogIngest.deleteMetacard(id, REST_PATH.getUrl(), HttpStatus.SC_OK);
=======
        String id = CatalogTestCommons.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                HttpStatus.SC_CREATED);
        CatalogTestCommons.update(id,
                "Some data to update",
                MediaType.TEXT_PLAIN,
                HttpStatus.SC_OK);
        CatalogTestCommons.deleteMetacard(id, HttpStatus.SC_OK);
>>>>>>> master
    }

    @Test
    public void testCswUpdateFailsWithFanoutEnabledAndBlacklistSet() throws IOException {
        getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
<<<<<<< HEAD
        String id = CatalogIngest.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl(),
=======
        String id = CatalogTestCommons.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
>>>>>>> master
                HttpStatus.SC_CREATED);

        // Set blacklist so update will fail
        getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
<<<<<<< HEAD
        CatalogIngest.update("Some data to update",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl() + id,
=======
        CatalogTestCommons.update(id,
                "Some data to update",
                MediaType.TEXT_PLAIN,
>>>>>>> master
                HttpStatus.SC_BAD_REQUEST);

        // Set blacklist to empty list so the delete will succeed
        getCatalogBundle().setFanoutTagBlacklist(new ArrayList<>());
<<<<<<< HEAD
        CatalogIngest.deleteMetacard(id, REST_PATH.getUrl(), HttpStatus.SC_OK);
=======
        CatalogTestCommons.deleteMetacard(id, HttpStatus.SC_OK);
>>>>>>> master
    }

    @Test
    public void testCswDeleteFailsWithFanoutEnabledAndBlacklistSet() throws IOException {
        // The case where delete works with fanout on and empty blacklist is tested as clean up in the other tests.
        getCatalogBundle().setFanoutTagBlacklist(Collections.emptyList());
<<<<<<< HEAD
        String id = CatalogIngest.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
                REST_PATH.getUrl(),
=======
        String id = CatalogTestCommons.ingest("Some data to ingest",
                MediaType.TEXT_PLAIN,
>>>>>>> master
                HttpStatus.SC_CREATED);

        // Set blacklist so update will fail
        getCatalogBundle().setFanoutTagBlacklist(TAG_BLACKLIST);
<<<<<<< HEAD
        CatalogIngest.deleteMetacard(id, REST_PATH.getUrl(), HttpStatus.SC_BAD_REQUEST);

        // Set blacklist to empty list so the delete will succeed
        getCatalogBundle().setFanoutTagBlacklist(new ArrayList<>());
        CatalogIngest.deleteMetacard(id, REST_PATH.getUrl(), HttpStatus.SC_OK);
=======
        CatalogTestCommons.deleteMetacard(id, HttpStatus.SC_BAD_REQUEST);

        // Set blacklist to empty list so the delete will succeed
        getCatalogBundle().setFanoutTagBlacklist(new ArrayList<>());
        CatalogTestCommons.deleteMetacard(id, HttpStatus.SC_OK);
>>>>>>> master
    }
}