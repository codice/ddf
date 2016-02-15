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

import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.Library;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestRegistry extends AbstractIntegrationTest {

    private static final String CATALOG_REGISTRY = "catalog-registry";

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(DEFAULT_REQUIRED_APPS);
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Test
    public void testCswRegistryIngest() throws Exception {
        getServiceManager().startFeature(true, CATALOG_REGISTRY);
        given().body(Library.getCswRegistryInsert()).header("Content-Type", "text/xml").expect()
                .log().all().statusCode(200).when().post(CSW_PATH.getUrl()).getHeader("id");

    }

}
