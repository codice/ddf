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
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.boon.json.JsonFactory;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.utils.LoggingUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCatalogSearchUi extends AbstractIntegrationTest {

  private static final String QUERY_CQL = "cql";

  private static final String QUERY_ENTERPRISE = "enterprise";

  private static final String WORKSPACE_METACARDS = "metacards";

  private static final String WORKSPACE_QUERIES = "queries";

  public static final String PATH = "/search/catalog/internal/workspaces";

  public static final DynamicUrl API_PATH = new DynamicUrl(SECURE_ROOT, HTTPS_PORT, PATH);

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
      getServiceManager().waitForHttpEndpoint(API_PATH.getUrl());
      getServiceManager().waitForAllBundles();

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void cleanUp() {
    clearCatalog();
  }

  private static String api() {
    return API_PATH.getUrl();
  }

  private static Map parse(Response res) {
    return JsonFactory.create().readValue(res.getBody().asInputStream(), Map.class);
  }

  private static String stringify(Object o) {
    return JsonFactory.create().writeValueAsString(o);
  }

  private static RequestSpecification asGuest() {
    return given().log().all().header("Content-Type", "application/json");
  }

  private static RequestSpecification asUser(String username, String password) {
    return given()
        .log()
        .all()
        .header("Content-Type", "application/json")
        .auth()
        .preemptive()
        .basic(username, password);
  }

  private static RequestSpecification asAdmin() {
    return given()
        .log()
        .all()
        .header("Content-Type", "application/json")
        .auth()
        .preemptive()
        .basic("admin", "admin");
  }

  private static ResponseSpecification expect(RequestSpecification req, int status) {
    return req.expect().log().all().statusCode(status).when();
  }

  @Test
  public void testGuestCantCreateWorkspace() throws Exception {
    Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
    expect(asGuest().body(stringify(workspace)), 404).post(api());
  }

  @Test
  public void testGuestCanCreateWorkspacesForOthers() {
    Map<String, String> workspace =
        ImmutableMap.of("title", "my workspace", Core.METACARD_OWNER, "a@b.c");
    Response res = expect(asGuest().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testAdminCanCreateWorkspace() {
    Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testGuestCantViewUnsharedWorkspace() {
    Map<String, Object> workspace = Collections.emptyMap();
    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 404).get(api() + "/" + id);
  }

  @Test
  public void testCanShareByGroup() {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 200).get(api() + "/" + id);
  }

  @Test
  public void testCanShareByEmail() {
    Map<String, Object> workspace =
        ImmutableMap.of(
            SecurityAttributes.ACCESS_INDIVIDUALS, ImmutableList.of("random@localhost.local"));

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 404).get(api() + "/" + id);
    expect(asUser("random", "password"), 200).get(api() + "/" + id);
  }

  @Test
  public void testCanShareAndUnshare() {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res = expect(asUser("random", "password").body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(
            asUser("random", "password")
                .body(stringify(ImmutableMap.of(Core.METACARD_OWNER, "random@localhost.local"))),
            200)
        .put(api() + "/" + id);
  }

  @Test
  public void testWorkspaceSavedItems() {
    List<String> metacards = ImmutableList.of("item1", "item2");
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_METACARDS, metacards);

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_METACARDS), is(metacards));
  }

  @Test
  public void testWorkspaceQueries() {
    Map<String, Object> query =
        ImmutableMap.<String, Object>builder()
            .put(Core.TITLE, "title")
            .put(QUERY_CQL, "query")
            .put(QUERY_ENTERPRISE, true)
            .build();

    List<Map<String, Object>> queries = ImmutableList.of(query);
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, queries);

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_QUERIES), is(queries));
  }

  @Test
  public void testWorkspaceQueriesWithSpecificSources() {
    Map<String, Object> query =
        ImmutableMap.<String, Object>builder()
            .put(Core.TITLE, "title")
            .put(QUERY_CQL, "query")
            .put("src", ImmutableList.of("source a", "source b"))
            .build();

    List<Map<String, Object>> queries = ImmutableList.of(query);
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, queries);

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(api());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_QUERIES), is(queries));
  }
}
