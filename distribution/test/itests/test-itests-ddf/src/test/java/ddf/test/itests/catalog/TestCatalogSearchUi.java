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
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static ddf.test.itests.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.boon.json.JsonFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

import ddf.common.test.BeforeExam;
import ddf.security.permission.CollectionPermission;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalogSearchUi extends AbstractIntegrationTest {

    public static final String PATH = "/search/catalog/internal/workspaces";

    public static final DynamicUrl API_PATH = new DynamicUrl(SECURE_ROOT, HTTPS_PORT, PATH);

    private List<String> ids = new ArrayList<>();

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps("catalog-app", "solr-app");
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().startFeature(true, "search-ui");
            getServiceManager().waitForHttpEndpoint(API_PATH.getUrl());
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @After
    public void cleanUp() {
        ids.stream()
                .forEach(TestCatalogSearchUi::delete);
        ids.clear();
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

    private static RequestSpecification asGuest() {
        return given().log()
                .all()
                .header("Content-Type", "application/json");
    }

    private static RequestSpecification asAdmin() {
        return given().log()
                .all()
                .header("Content-Type", "application/json")
                .auth()
                .preemptive()
                .basic("admin", "admin");
    }

    private static ResponseSpecification expect(RequestSpecification req, int status) {
        return req.expect()
                .log()
                .all()
                .statusCode(status)
                .when();
    }

    private static Map<String, String> makePermission(String attribute, String action,
            String value) {
        return ImmutableMap.of("attribute", attribute, "action", action, "value", value);
    }

    private static void delete(String id) {
        asAdmin().expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .delete(api() + "/" + id);
    }

    @Test
    public void testGuestCantCreateWorkspace() throws Exception {
        Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
        expect(asGuest().body(stringify(workspace)), 404).post(api());
    }

    @Test
    public void testAdminCanCreateWorkspace() {
        Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

        Map body = parse(res);
        String id = (String) body.get("id");
        assertNotNull(id);

        ids.add(id); // for cleanUp
    }

    @Test
    public void testGuestCantViewUnsharedWorkspace() {
        Map<String, Object> workspace = Collections.emptyMap();
        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

        Map body = parse(res);
        String id = (String) body.get("id");
        assertNotNull(id);

        expect(asGuest(), 404).get(api() + "/" + id);

        ids.add(id); // for cleanUp
    }

    @Test
    public void testCanShareViewWithGuestByRole() {
        Map<String, String> guestPermission = makePermission("role",
                CollectionPermission.READ_ACTION,
                "guest");

        Map<String, Object> workspace = ImmutableMap.of("metacard.sharing",
                ImmutableList.of(guestPermission));

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

        Map body = parse(res);
        String id = (String) body.get("id");
        assertNotNull(id);

        expect(asGuest(), 200).get(api() + "/" + id);

        ids.add(id); // for cleanUp
    }

    @Test
    public void testCanShareEditWithGuestByRole() {

        Map<String, String> guestPermission = makePermission("role",
                CollectionPermission.UPDATE_ACTION,
                "guest");

        Map<String, Object> workspace = ImmutableMap.of("metacard.sharing",
                ImmutableList.of(guestPermission));

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

        Map body = parse(res);
        String id = (String) body.get("id");
        assertNotNull(id);

        String title = "from guest";

        Map update = ImmutableMap.builder()
                .putAll(body)
                .put("title", title)
                .build();

        res = expect(asGuest().body(stringify(update)), 200).put(api() + "/" + id);
        body = parse(res);

        assertThat(body.get("id"), is(id));
        assertThat(body.get("title"), is(title));

        ids.add(id); // for cleanUp
    }
}
