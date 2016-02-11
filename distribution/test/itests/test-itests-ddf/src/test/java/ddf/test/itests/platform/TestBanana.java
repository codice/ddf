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

package ddf.test.itests.platform;

import static com.jayway.restassured.RestAssured.given;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.response.Response;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestBanana extends AbstractIntegrationTest {

    private static final DynamicUrl BANANA_URL = new DynamicUrl(DynamicUrl.SECURE_ROOT,
            HTTPS_PORT,
            "/solr/banana");

    @BeforeExam
    public void beforeTest() throws Exception {
        basePort = getBasePort();
        getServiceManager().waitForAllBundles();
        // Start the services needed for testing.
        // We need to start the Search UI to test that it redirects properly
        getServiceManager().startFeature(true, "banana-provisioner");
    }

    @Test
    public void testDeployment() throws Exception {

        Response res = given().auth()
                .preemptive()
                .basic("admin", "admin")
                .expect()
                .statusCode(200)
                .when()
                .get(BANANA_URL.getUrl());
    }
}
