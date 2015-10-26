/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.test;

import static org.hamcrest.Matchers.containsString;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.common.test.BeforeExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFanout extends AbstractIntegrationTest {

    private static final String LOCAL_SOURCE_ID = "ddf.distribution";

    @BeforeExam
    public void beforeExam() throws Exception {
        setPortsAndUrls();
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
        getCatalogBundle().setFanout(true);
        getCatalogBundle().waitForCatalogProvider();
        getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");

        getServiceManager().waitForSourcesToBeAvailable(REST_PATH, LOCAL_SOURCE_ID);

        LOGGER.info("Source status: \n{}", get(REST_PATH + "sources").body().prettyPrint());
    }

    @Test
    public void testFanoutQueryWithUnknownSource() throws Exception {
        String queryUrl = OPENSEARCH_PATH + "?q=*&src=does.not.exist";

        when().get(queryUrl).then().log().all().assertThat().body(containsString("Unknown source"));
    }

    @Test
    public void testFanoutQueryWithoutFederatedSources() throws Exception {
        String queryUrl = OPENSEARCH_PATH + "?q=*&src=local";

        when().get(queryUrl).then().log().all().assertThat()
                .body(containsString("SiteNames could not be resolved"));
    }
}
