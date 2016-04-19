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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.boon.json.JsonFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalogSearchUi extends AbstractIntegrationTest {

    private static final DynamicUrl API_PATH = new DynamicUrl(SERVICE_ROOT,
            "/search/catalog/workspaces");

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps("catalog-app", "solr-app");
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().startFeature(true, "search-ui");
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/search/catalog/workspaces");
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    private static String api() {
        return API_PATH.getUrl();
    }

    private static Map parse(Response res) {
        return JsonFactory.create()
                .readValue(res.getBody()
                        .asInputStream(), Map.class);
    }

    private static String stringify(Object o) {
        return JsonFactory.create()
                .writeValueAsString(o);
    }

    private static void delete(String id) {
        given().log()
                .all()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .delete(api() + "/" + id);
    }

    @Test
    public void testWorkspaceEndpointAsNonAdmin() throws Exception {
        given().log()
                .all()
                .header("Content-Type", "application/json")
                .body(stringify(ImmutableMap.of("title", "my workspace")))
                .expect()
                .log()
                .all()
                .statusCode(401)
                .when()
                .post(api());
    }

    /*
    @Test
    public void testWorkspaceEndpointWithRolesAsGuest() throws Exception {
        List<String> roles = Arrays.asList("guest");

        Response res = given().log()
                .all()
                .header("Content-Type", "application/json")
                .body(stringify(ImmutableMap.of("roles", roles)))
                .expect()
                .log()
                .all()
                .statusCode(201)
                .when()
                .post(api());

        Map body = parse(res);
        assertNotNull(body.get("id"));
        assertEquals((List) body.get("roles"), roles);
        delete((String) body.get("id"));
    }*/

    @Test
    public void testWorkspaceEndpointWithRolesAsNonAdmin() throws Exception {
        given().log()
                .all()
                .header("Content-Type", "application/json")
                .body(stringify(ImmutableMap.of("roles", Arrays.asList("admin"))))
                .expect()
                .log()
                .all()
                .statusCode(401)
                .when()
                .post(api());
    }

    @Test
    public void testWorkspaceEndpointWithRolesAsAdmin() throws Exception {
        List<String> roles = Arrays.asList("admin");

        Response res = given().log()
                .all()
                .auth()
                .preemptive()
                .basic("admin", "admin")
                .header("Content-Type", "application/json")
                .body(stringify(ImmutableMap.of("roles", roles)))
                .expect()
                .log()
                .all()
                .statusCode(201)
                .when()
                .post(api());

        Map body = parse(res);
        assertNotNull(body.get("id"));
        assertEquals((List) body.get("roles"), roles);
        delete((String) body.get("id"));
    }

}
