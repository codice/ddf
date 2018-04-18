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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.boon.Boon;
import org.boon.json.JsonFactory;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
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

  public static final String WORKSPACES_PATH = "/search/catalog/internal/workspaces";

  public static final String QUERY_TEMPLATES_PATH = "/search/catalog/internal/forms/query";

  public static final String RESULT_TEMPLATES_PATH = "/search/catalog/internal/forms/result";

  public static final DynamicUrl WORKSPACES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, WORKSPACES_PATH);

  public static final DynamicUrl QUERY_TEMPLATES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, QUERY_TEMPLATES_PATH);

  public static final DynamicUrl RESULT_TEMPLATES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, RESULT_TEMPLATES_PATH);

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
      getServiceManager().waitForHttpEndpoint(WORKSPACES_API_PATH.getUrl());
      getServiceManager().waitForHttpEndpoint(QUERY_TEMPLATES_API_PATH.getUrl());
      getServiceManager().waitForHttpEndpoint(RESULT_TEMPLATES_API_PATH.getUrl());
      getServiceManager().waitForAllBundles();

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void cleanUp() {
    clearCatalog();
  }

  private static String workspacesApi() {
    return WORKSPACES_API_PATH.getUrl();
  }

  private static String queryTemplatesApi() {
    return QUERY_TEMPLATES_API_PATH.getUrl();
  }

  private static String resultTemplatesApi() {
    return RESULT_TEMPLATES_API_PATH.getUrl();
  }

  private static Map parse(Response res) {
    return JsonFactory.create().readValue(res.getBody().asInputStream(), Map.class);
  }

  private static List<Map> parseList(Response res) {
    List<Object> list = Boon.fromJson(res.getBody().asString(), List.class);
    return list.stream().map(Map.class::cast).collect(Collectors.toList());
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
    expect(asGuest().body(stringify(workspace)), 404).post(workspacesApi());
  }

  @Test
  public void testGuestCanCreateWorkspacesForOthers() {
    Map<String, String> workspace =
        ImmutableMap.of("title", "my workspace", Core.METACARD_OWNER, "a@b.c");
    Response res = expect(asGuest().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testAdminCanCreateWorkspace() {
    Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testGuestCantViewUnsharedWorkspace() {
    Map<String, Object> workspace = Collections.emptyMap();
    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 404).get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareByGroup() {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 200).get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareByEmail() {
    Map<String, Object> workspace =
        ImmutableMap.of(
            SecurityAttributes.ACCESS_INDIVIDUALS, ImmutableList.of("random@localhost.local"));

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest(), 404).get(workspacesApi() + "/" + id);
    expect(asUser("random", "password"), 200).get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareAndUnshare() {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res =
        expect(asUser("random", "password").body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(
            asUser("random", "password")
                .body(stringify(ImmutableMap.of(Core.METACARD_OWNER, "random@localhost.local"))),
            200)
        .put(workspacesApi() + "/" + id);
  }

  @Test
  public void testWorkspaceSavedItems() {
    List<String> metacards = ImmutableList.of("item1", "item2");
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_METACARDS, metacards);

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

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

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

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

    Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_QUERIES), is(queries));
  }

  @Test
  public void testGetSystemTemplates() {
    Set<String> expectedQueryTemplateTitles =
        new HashSet<>(ImmutableSet.of("Contact Name", "Imagery Only"));

    Response httpRes1 = expect(asUser("srogers", "password1"), 200).get(queryTemplatesApi());
    Response httpRes2 = expect(asUser("srogers", "password1"), 200).get(resultTemplatesApi());

    assertTemplateDataStructures(JsonPath.from(httpRes1.getBody().asString()));

    List<Map> queryTemplates = parseList(httpRes1);
    List<Map> resultTemplates = parseList(httpRes2);

    assertThat(
        "Missing expected query templates: " + expectedQueryTemplateTitles.toString(),
        queryTemplates
            .stream()
            .map(m -> m.get("title"))
            .map(String.class::cast)
            .map(expectedQueryTemplateTitles::remove)
            .reduce(Boolean::logicalAnd)
            .orElse(false),
        is(true));

    List<?> unknownElements =
        Optional.of(resultTemplates.get(0))
            .map(m -> m.get("descriptors"))
            .map(List.class::cast)
            .orElseThrow(() -> new AssertionError("Result template data was malformed"));

    List<String> descriptors =
        unknownElements.stream().map(String.class::cast).collect(Collectors.toList());

    assertThat(resultTemplates, hasSize(1));
    assertThat(
        "Result template did not have expected descriptors in the data",
        descriptors,
        hasItems("title", "description", "created", "resource-download-url", "thumbnail"));
  }

  private static void assertTemplateDataStructures(JsonPath json) {
    assertThat(json.get("[0].title"), is("Imagery Only"));
    assertThat(json.get("[0].description"), is("Search across all image datatypes."));
    assertThat(json.get("[0].id"), isA(String.class));
    assertThat(json.get("[0].created"), isA(Long.class));

    assertThat(json.get("[0].filterTemplate.type"), is("AND"));

    assertThat(json.get("[0].filterTemplate.filters[0].type"), is("="));
    assertThat(json.get("[0].filterTemplate.filters[0].property"), is("datatype"));
    assertThat(json.get("[0].filterTemplate.filters[0].value"), is("Image"));
    assertThat(json.get("[0].filterTemplate.filters[0].templated"), is(false));

    assertThat(json.get("[0].filterTemplate.filters[1].type"), is("="));
    assertThat(json.get("[0].filterTemplate.filters[1].property"), is("title"));
    assertThat(json.get("[0].filterTemplate.filters[1].value"), is(nullValue()));
    assertThat(json.get("[0].filterTemplate.filters[1].templated"), is(true));
    assertThat(json.get("[0].filterTemplate.filters[1].defaultValue"), is(nullValue()));
    assertThat(json.get("[0].filterTemplate.filters[1].nodeId"), is("my-id-1"));
    assertThat(json.get("[0].filterTemplate.filters[1].isVisible"), is(true));
    assertThat(json.get("[0].filterTemplate.filters[1].isReadOnly"), is(false));

    assertThat(json.get("[0].filterTemplate.filters[2].type"), is("<="));
    assertThat(json.get("[0].filterTemplate.filters[2].property"), is("media.bit-rate"));
    assertThat(json.get("[0].filterTemplate.filters[2].value"), is(nullValue()));
    assertThat(json.get("[0].filterTemplate.filters[2].templated"), is(true));
    assertThat(json.get("[0].filterTemplate.filters[2].defaultValue"), is(nullValue()));
    assertThat(json.get("[0].filterTemplate.filters[2].nodeId"), is("my-id-2"));
    assertThat(json.get("[0].filterTemplate.filters[2].isVisible"), is(true));
    assertThat(json.get("[0].filterTemplate.filters[2].isReadOnly"), is(false));

    assertThat(json.get("[1].title"), is("Contact Name"));
    assertThat(json.get("[1].id"), isA(String.class));
    assertThat(json.get("[1].created"), isA(Long.class));

    assertThat(json.get("[1].filterTemplate.type"), is("OR"));
  }
}
