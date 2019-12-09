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
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.junit.After;
import org.junit.Ignore;
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

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  public static final String QUERIES_PATH = "/search/catalog/internal/queries";

  public static final String WORKSPACES_PATH = "/search/catalog/internal/workspaces";

  public static final String QUERY_TEMPLATES_PATH = "/search/catalog/internal/forms/query";

  public static final String RESULT_TEMPLATES_PATH = "/search/catalog/internal/forms/result";

  public static final String TRANSFORMERS_PATH = "/search/catalog/internal/transformers";

  public static final DynamicUrl QUERIES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, QUERIES_PATH);

  public static final DynamicUrl WORKSPACES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, WORKSPACES_PATH);

  public static final DynamicUrl QUERY_TEMPLATES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, QUERY_TEMPLATES_PATH);

  public static final DynamicUrl RESULT_TEMPLATES_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, RESULT_TEMPLATES_PATH);

  public static final DynamicUrl TRANSFORMERS_API_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, TRANSFORMERS_PATH);

  private static final Map<String, String> METACARD_TRANSFORMER_DESCRIPTORS =
      ImmutableMap.<String, String>builder()
          .put("metadata", "Metadata XML")
          .put("rtf", "RTF")
          .put("overlay.thumbnail", "Overlay Thumbnail")
          .put("xml", "OGC GML")
          .put("preview", "Preview")
          .put("resource", "Binary Resource")
          .put("geojson", "GeoJSON")
          .put("html", "Preview HTML")
          .put("thumbnail", "Thumbnail")
          .put("propertyjson", "Property JSON")
          .put("kml", "KML")
          .put("gmd:MD_Metadata", "GMD Metadata")
          .put("csw:Record", "CSW Record XML")
          .build();

  private static final Map<String, String> QUERY_RESPONSE_TRANSFORMER_DESCRIPTORS =
      ImmutableMap.<String, String>builder()
          .put("html", "Preview HTML")
          .put("rtf", "RTF")
          .put("xml", "OGC GML")
          .put("csv", "CSV")
          .put("geojson", "GeoJSON")
          .put("atom", "Atom")
          .put("kml", "KML")
          .put("csw:Record", "CSW Record XML")
          .build();

  private static Map<String, Object> originalPolicyManagerProps = null;

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      getServiceManager().waitForHttpEndpoint(WORKSPACES_API_PATH.getUrl());
      getServiceManager().waitForHttpEndpoint(QUERY_TEMPLATES_API_PATH.getUrl());
      getServiceManager().waitForHttpEndpoint(RESULT_TEMPLATES_API_PATH.getUrl());
      getServiceManager().waitForAllBundles();

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @AfterExam
  public void afterExam() throws Exception {
    if (originalPolicyManagerProps != null) {
      getSecurityPolicy().updateWebContextPolicy(originalPolicyManagerProps);
    }
  }

  @After
  public void cleanUp() {
    clearCatalog();
  }

  private static String queriesApi() {
    return QUERIES_API_PATH.getUrl();
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

  private static String transformersApi() {
    return TRANSFORMERS_API_PATH.getUrl();
  }

  private static Map parse(Response res) {
    return GSON.fromJson(res.getBody().asString(), MAP_STRING_TO_OBJECT_TYPE);
  }

  private static List<Map> parseList(Response res) {
    List<Map> list =
        GSON.fromJson(res.getBody().asString(), new TypeToken<List<Map>>() {}.getType());
    return list.stream().map(Map.class::cast).collect(Collectors.toList());
  }

  private static String stringify(Object o) {
    return GSON.toJson(o);
  }

  private RequestSpecification asGuest() throws Exception {
    Map<String, Object> policyManagerProps = getSecurityPolicy().configureRestForGuest();
    getSecurityPolicy().waitForGuestAuthReady(SERVICE_ROOT.getUrl());
    if (originalPolicyManagerProps == null) {
      originalPolicyManagerProps = policyManagerProps;
    }
    getSecurityPolicy().configureRestForGuest();
    return given()
        .log()
        .ifValidationFails()
        .header("Content-Type", "application/json")
        .header("X-Requested-With", "XMLHttpRequest");
  }

  private RequestSpecification asUser(String username, String password) throws Exception {
    Map<String, Object> policyManagerProps = getSecurityPolicy().configureRestForBasic();
    getSecurityPolicy().waitForBasicAuthReady(SERVICE_ROOT.getUrl());
    if (originalPolicyManagerProps == null) {
      originalPolicyManagerProps = policyManagerProps;
    }
    return given()
        .log()
        .ifValidationFails()
        .header("Content-Type", "application/json")
        .auth()
        .preemptive()
        .basic(username, password)
        .header("X-Requested-With", "XMLHttpRequest");
  }

  private RequestSpecification asAdmin() throws Exception {
    Map<String, Object> policyManagerProps = getSecurityPolicy().configureRestForBasic();
    getSecurityPolicy().waitForBasicAuthReady(SERVICE_ROOT.getUrl());
    if (originalPolicyManagerProps == null) {
      originalPolicyManagerProps = policyManagerProps;
    }
    return given()
        .log()
        .ifValidationFails()
        .header("Content-Type", "application/json")
        .auth()
        .preemptive()
        .basic("admin", "admin")
        .header("X-Requested-With", "XMLHttpRequest");
  }

  private static ResponseSpecification expect(RequestSpecification req, int status) {
    return req.expect().log().all().statusCode(status).when();
  }

  @Test
  public void testGuestCantCreateWorkspace() throws Exception {
    Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
    expect(asGuest().header("Origin", workspacesApi()).body(stringify(workspace)), 404)
        .post(workspacesApi());
  }

  @Test
  public void testGuestCanCreateWorkspacesForOthers() throws Exception {
    Map<String, String> workspace =
        ImmutableMap.of("title", "my workspace", Core.METACARD_OWNER, "a@b.c");
    Response res =
        expect(asGuest().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testAdminCanCreateWorkspace() throws Exception {
    Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
  }

  @Test
  public void testGuestCantViewUnsharedWorkspace() throws Exception {
    Map<String, Object> workspace = Collections.emptyMap();
    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest().header("Origin", workspacesApi()), 404).get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareByGroup() throws Exception {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest().header("Origin", workspacesApi()), 200).get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareByEmail() throws Exception {
    Map<String, Object> workspace =
        ImmutableMap.of(
            SecurityAttributes.ACCESS_INDIVIDUALS, ImmutableList.of("random@localhost.local"));

    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(asGuest().header("Origin", workspacesApi()), 404).get(workspacesApi() + "/" + id);
    expect(asUser("random", "password").header("Origin", workspacesApi()), 200)
        .get(workspacesApi() + "/" + id);
  }

  @Test
  public void testCanShareAndUnshare() throws Exception {
    Map<String, Object> workspace =
        ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS, ImmutableList.of("guest"));

    Response res =
        expect(
                asUser("random", "password")
                    .header("Origin", workspacesApi())
                    .body(stringify(workspace)),
                201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);

    expect(
            asUser("random", "password")
                .header("Origin", workspacesApi())
                .body(stringify(ImmutableMap.of(Core.METACARD_OWNER, "random@localhost.local"))),
            200)
        .put(workspacesApi() + "/" + id);
  }

  @Test
  public void testWorkspaceSavedItems() throws Exception {
    List<String> metacards = ImmutableList.of("item1", "item2");
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_METACARDS, metacards);

    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_METACARDS), is(metacards));
  }

  @Test
  public void testWorkspaceQueries() throws Exception {
    List<Map<String, String>> queries =
        Arrays.asList(ImmutableMap.of("id", "queryId1"), ImmutableMap.of("id", "queryId2"));
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, queries);

    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_QUERIES), is(queries));
  }

  @Test
  public void testQueriesWithSpecificSources() throws Exception {
    List<String> sources = ImmutableList.of("source a", "source b");

    Map<String, Object> query =
        ImmutableMap.<String, Object>builder()
            .put(Core.TITLE, "title")
            .put(QUERY_CQL, "query")
            .put("src", sources)
            .build();

    Response res =
        expect(asAdmin().header("Origin", queriesApi()).body(stringify(query)), 201)
            .post(queriesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get("src"), is(sources));
  }

  @Test
  public void testCreateWorkspaceWithQueries() throws Exception {
    Map<String, Object> query = ImmutableMap.of("id", "queryId");
    Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, ImmutableList.of(query));

    Response res =
        expect(asAdmin().header("Origin", workspacesApi()).body(stringify(workspace)), 201)
            .post(workspacesApi());

    Map body = parse(res);
    String id = (String) body.get("id");
    assertNotNull(id);
    assertThat(body.get(WORKSPACE_QUERIES), is(ImmutableList.of(query)));
  }

  @SuppressWarnings("squid:S1607" /* Feature is off by default */)
  @Ignore
  @Test
  public void testGetSystemTemplates() throws Exception {
    Set<String> expectedQueryTemplateTitles =
        new HashSet<>(ImmutableSet.of("Contact Name", "Imagery Only"));

    Response httpRes1 =
        expect(asUser("srogers", "password1").header("Origin", queryTemplatesApi()), 200)
            .get(queryTemplatesApi());
    Response httpRes2 =
        expect(asUser("srogers", "password1").header("Origin", resultTemplatesApi()), 200)
            .get(resultTemplatesApi());

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

  @Test
  public void testGetMetacardTransformerDescriptors() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 200)
            .get(transformersApi() + "/metacard");

    List<Map> body = parseList(res);

    for (Entry<String, String> entry : METACARD_TRANSFORMER_DESCRIPTORS.entrySet()) {
      String id = entry.getKey();
      String displayName = entry.getValue();

      assertTransformerDescriptorsContains(body, id, displayName);
    }
  }

  @Test
  public void testGetQueryResponseTransformerDescriptors() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 200)
            .get(transformersApi() + "/query");

    List<Map> body = parseList(res);

    for (Entry<String, String> entry : QUERY_RESPONSE_TRANSFORMER_DESCRIPTORS.entrySet()) {
      String id = entry.getKey();
      String displayName = entry.getValue();

      assertTransformerDescriptorsContains(body, id, displayName);
    }
  }

  @Test
  public void testGetMetacardTransformerDescriptorById() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 200)
            .get(transformersApi() + "/metacard/html");

    Map<String, String> body = (Map<String, String>) parse(res);

    assertTransformerDescriptor(body, "html", "Preview HTML");
  }

  @Test
  public void testGetQueryResponseTransformerDescriptorById() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 200)
            .get(transformersApi() + "/query/atom");

    Map<String, String> body = (Map<String, String>) parse(res);

    assertTransformerDescriptor(body, "atom", "Atom");
  }

  @Test
  public void testGetMetacardTransformerDescriptorNotFound() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 404)
            .get(transformersApi() + "/metacard/foo");

    Map<String, String> body = (Map<String, String>) parse(res);

    assertThat(body, hasEntry("message", "Transformer not found"));
  }

  @Test
  public void testGetQueryResponseTransformerDescriptorNotFound() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 404)
            .get(transformersApi() + "/query/bar");

    Map<String, String> body = (Map<String, String>) parse(res);

    assertThat(body, hasEntry("message", "Transformer not found"));
  }

  @Test
  public void testGetTransformerDescriptorTypeNotFound() throws Exception {
    Response res =
        expect(asUser("random", "password").header("Origin", transformersApi()), 404)
            .get(transformersApi() + "/foo/bar");

    Map<String, String> body = (Map<String, String>) parse(res);

    assertThat(body, hasEntry("message", "Transformer type not found"));
  }

  private static void assertTransformerDescriptorsContains(
      List<Map> descriptors, String id, String displayName) {
    assertThat(descriptors, hasItem(hasEntry("id", id)));
    assertThat(descriptors, hasItem(hasEntry("displayName", displayName)));
  }

  private static void assertTransformerDescriptor(
      Map<String, String> descriptor, String id, String displayName) {
    assertThat(descriptor, hasEntry("id", id));
    assertThat(descriptor, hasEntry("displayName", displayName));
  }

  private static void assertTemplateDataStructures(JsonPath json) {
    assertThat(json.get("[1].title"), is("Imagery Only"));
    assertThat(json.get("[1].description"), is("Search across all image datatypes."));
    assertThat(json.get("[1].id"), isA(String.class));
    assertThat(json.get("[1].created"), isA(Long.class));

    assertThat(json.get("[1].filterTemplate.type"), is("AND"));

    assertThat(json.get("[1].filterTemplate.filters[0].type"), is("="));
    assertThat(json.get("[1].filterTemplate.filters[0].property"), is("datatype"));
    assertThat(json.get("[1].filterTemplate.filters[0].value"), is("Image"));

    assertThat(json.get("[1].filterTemplate.filters[1].type"), is("="));
    assertThat(json.get("[1].filterTemplate.filters[1].property"), is("title"));
    assertThat(json.get("[1].filterTemplate.filters[1].value"), is(nullValue()));

    assertThat(
        json.get("[1].filterTemplate.filters[1].templateProperties.defaultValue"), is(nullValue()));
    assertThat(json.get("[1].filterTemplate.filters[1].templateProperties.nodeId"), is("my-id-1"));
    assertThat(json.get("[1].filterTemplate.filters[1].templateProperties.isVisible"), is(true));
    assertThat(json.get("[1].filterTemplate.filters[1].templateProperties.isReadOnly"), is(false));

    assertThat(json.get("[1].filterTemplate.filters[2].type"), is("<="));
    assertThat(json.get("[1].filterTemplate.filters[2].property"), is("media.bit-rate"));
    assertThat(json.get("[1].filterTemplate.filters[2].value"), is(nullValue()));

    assertThat(
        json.get("[1].filterTemplate.filters[2].templateProperties.defaultValue"), is(nullValue()));
    assertThat(json.get("[1].filterTemplate.filters[2].templateProperties.nodeId"), is("my-id-2"));
    assertThat(json.get("[1].filterTemplate.filters[2].templateProperties.isVisible"), is(true));
    assertThat(json.get("[1].filterTemplate.filters[2].templateProperties.isReadOnly"), is(false));

    assertThat(json.get("[0].title"), is("Contact Name"));
    assertThat(json.get("[0].id"), isA(String.class));
    assertThat(json.get("[0].created"), isA(Long.class));

    assertThat(json.get("[0].filterTemplate.type"), is("OR"));
  }
}
