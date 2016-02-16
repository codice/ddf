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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.path.json.JsonPath;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFanout extends AbstractIntegrationTest {

    private static final String LOCAL_SOURCE_ID = "ddf.distribution";

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

    private void startCswSource() throws Exception {
        getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");
        CswSourceProperties cswProperties = new CswSourceProperties(CSW_SOURCE_ID);
        getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID, cswProperties);

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
            getServiceManager().stopManagedService(CswSourceProperties.FACTORY_PID);
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
            getServiceManager().stopManagedService(CswSourceProperties.FACTORY_PID);
        }
    }

    @Test
    public void testFanoutQueryWithUnknownSource() throws Exception {
        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=*&src=does.not.exist";

        when().get(queryUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .body(containsString("Unknown source"));
    }

    @Test
    public void testFanoutQueryWithoutFederatedSources() throws Exception {
        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=*&src=local";

        when().get(queryUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .body(containsString("SiteNames could not be resolved"));
    }
}
