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

import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.CSW_TRANSACTIONAL_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswTransactionalSourceProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.boon.core.value.CharSequenceValue;
import org.boon.json.JsonFactory;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.csw.mock.FederatedCswMockServer;
import org.codice.ddf.itests.common.utils.LoggingUtils;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.SecurityAttributes;
import ddf.catalog.data.types.Core;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCatalogSearchUi extends AbstractIntegrationTest {

    public static final String WORKSPACE_PATH = "/search/catalog/internal/workspaces";

    private static final String ASSOCIATIONS_PATH = "/search/catalog/internal/associations";

    private static final String PUT_ASSOCIATION_STRING =
            "[{\"parent\":{\"id\":\"%s\"},\"child\":{\"id\":\"%s\"},\"relationship\":\"%s\",\"relation\":\"%s\"}]\"";

    private static final String RELATED = "related";

    private static final String JSON_MIME_TYPE = "application/json";

    private static final String CONTENT_TYPE_HEADER = "Content-Type";

    private static final String QUERY_CQL = "cql";

    private static final String ADMIN = "admin";

    private static final String QUERY = "query";

    private static final String QUERY_ENTERPRISE = "enterprise";

    private static final String WORKSPACE_METACARDS = "metacards";

    private static final String WORKSPACE_QUERIES = "queries";

    private static final String REMOTE_METACARD_ID = "6000da314ffe4465a5ce042f31b9f4ac";

    public static final DynamicUrl ASSOCIATIONS_URL = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            ASSOCIATIONS_PATH);

    public static final DynamicUrl WORKSPACE_URL = new DynamicUrl(SECURE_ROOT,
            HTTPS_PORT,
            WORKSPACE_PATH);

    private static final DynamicPort CSW_STUB_SERVER_PORT = new DynamicPort(6);

    public static final DynamicUrl CSW_STUB_SERVER_PATH = new DynamicUrl(INSECURE_ROOT,
            CSW_STUB_SERVER_PORT,
            "/services/csw");

    private static final String CSW_STUB_SOURCE_ID = "cswStubServer";

    private static FederatedCswMockServer cswServer;

    private static final int CSW_SOURCE_POLL_INTERVAL = 10;

    private static final String POLL_INTERVAL = "pollInterval";

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            waitForSystemReady();
            cswServer = new FederatedCswMockServer(CSW_STUB_SOURCE_ID,
                    INSECURE_ROOT,
                    Integer.parseInt(CSW_STUB_SERVER_PORT.getPort()));
            cswServer.start();

            Map<String, Object> cswStubServerProperties = getCswTransactionalSourceProperties(
                    CSW_STUB_SOURCE_ID,
                    CSW_STUB_SERVER_PATH.getUrl(),
                    getServiceManager());
            cswStubServerProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
            getServiceManager().createManagedService(CSW_TRANSACTIONAL_SOURCE_FACTORY_PID,
                    cswStubServerProperties);

            getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");
            getServiceManager().waitForHttpEndpoint(WORKSPACE_URL.getUrl());

            getCatalogBundle().waitForFederatedSource(CSW_STUB_SOURCE_ID);
            getServiceManager().waitForSourcesToBeAvailable(REST_PATH.getUrl(), CSW_STUB_SOURCE_ID);

            LOGGER.info("Source status: \n{}", get(REST_PATH.getUrl() + "sources").body()
                    .prettyPrint());
        } catch (Exception e) {
            LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
        }
    }

    @After
    public void tearDown() {
        clearCatalog();
    }

    @Test
    public void testGuestCantCreateWorkspace() throws Exception {
        Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
        expect(asGuest().body(stringify(workspace)), 404).post(workspaceApi());
    }

    @Test
    public void testGuestCanCreateWorkspacesForOthers() {
        Map<String, String> workspace = ImmutableMap.of("title",
                "my workspace",
                Core.METACARD_OWNER,
                "a@b.c");
        Response res = expect(asGuest().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
    }

    @Test
    public void testAdminCanCreateWorkspace() {
        Map<String, String> workspace = ImmutableMap.of("title", "my workspace");
        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
    }

    @Test
    public void testGuestCantViewUnsharedWorkspace() {
        Map<String, Object> workspace = Collections.emptyMap();
        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
        expect(asGuest(), 404).get(workspaceApi() + "/" + id);
    }

    @Test
    public void testCanShareByGroup() {
        Map<String, Object> workspace = ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS,
                ImmutableList.of("guest"));

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
        expect(asGuest(), 200).get(workspaceApi() + "/" + id);
    }

    @Test
    public void testCanShareByEmail() {
        Map<String, Object> workspace = ImmutableMap.of(SecurityAttributes.ACCESS_INDIVIDUALS,
                ImmutableList.of("random@localhost.local"));

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());

        expect(asGuest(), 404).get(workspaceApi() + "/" + id);
        expect(asUser("random", "password"), 200).get(workspaceApi() + "/" + id);
    }

    @Test
    public void testCanShareAndUnshare() {
        Map<String, Object> workspace = ImmutableMap.of(SecurityAttributes.ACCESS_GROUPS,
                ImmutableList.of("guest"));

        Response res = expect(asUser("random", "password").body(stringify(workspace)), 201).post(
                workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get("id");
        assertThat(id, notNullValue());

        expect(asUser("random", "password").body(stringify(ImmutableMap.of(Core.METACARD_OWNER,
                "random@localhost.local"))), 200).put(workspaceApi() + "/" + id);
    }

    @Test
    public void testWorkspaceSavedItems() {
        List<String> metacards = ImmutableList.of("item1", "item2");
        Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_METACARDS, metacards);

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
        assertThat(body.get(WORKSPACE_METACARDS), is(metacards));
    }

    @Test
    public void testWorkspaceQueries() {
        Map<String, Object> query = ImmutableMap.<String, Object>builder()
                .put("title", "title")
                .put(QUERY_CQL, QUERY)
                .put(QUERY_ENTERPRISE, true)
                .build();

        List<Map<String, Object>> queries = ImmutableList.of(query);
        Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, queries);

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
        assertThat(body.get(WORKSPACE_QUERIES), is(queries));
    }

    @Test
    public void testWorkspaceQueriesWithSpecificSources() {
        Map<String, Object> query = ImmutableMap.<String, Object>builder()
                .put("title", "title")
                .put(QUERY_CQL, QUERY)
                .put("src", ImmutableList.of("source a", "source b"))
                .build();

        List<Map<String, Object>> queries = ImmutableList.of(query);
        Map<String, Object> workspace = ImmutableMap.of(WORKSPACE_QUERIES, queries);

        Response res = expect(asAdmin().body(stringify(workspace)), 201).post(workspaceApi());

        Map body = parseMap(res);
        String id = (String) body.get(Core.ID);
        assertThat(id, notNullValue());
        assertThat(body.get(WORKSPACE_QUERIES), is(queries));
    }

    @Test
    public void testLocalAssociation() {
        setEmptyCswQueryResponse();

        String metacardId1 = ingest(getFileContent("metacard1.xml"), MediaType.APPLICATION_XML);
        String metacardId2 = ingest(getFileContent("metacard2.xml"), MediaType.APPLICATION_XML);
        String associationJson = String.format(PUT_ASSOCIATION_STRING,
                metacardId1,
                metacardId2,
                RELATED,
                AssociationsAttributes.RELATED);

        Response putResponse = expect(asAdmin().body(associationJson), 200).put(
                associationsApi() + "/" + metacardId1);

        List<Object> associationsPutResponses = parseArray(AssociationsPutResponse.class,
                putResponse);
        AssociationsPutResponse associationsPutResponse =
                (AssociationsPutResponse) associationsPutResponses.get(0);
        String parentId = ((CharSequenceValue) associationsPutResponse.getParent()
                .get(Core.ID)).stringValue();
        assertThat(parentId, is(metacardId1));
        String childId = ((CharSequenceValue) associationsPutResponse.getChild()
                .get(Core.ID)).stringValue();
        assertThat(childId, is(metacardId2));
        assertThat(associationsPutResponse.getRelationship(), is(RELATED));
        assertThat(associationsPutResponse.getRelation(), is(AssociationsAttributes.RELATED));

        Response getResponse = expect(asAdmin(), 200).get(associationsApi() + "/" + metacardId1);
        List<Object> associationGetResponses = parseArray(AssociationsPutResponse.class,
                getResponse);
        AssociationsGetResponse associationGetResponse =
                (AssociationsGetResponse) associationGetResponses.get(0);
        parentId = ((CharSequenceValue) associationGetResponse.getParent()
                .get(Core.ID)).stringValue();
        assertThat(parentId, is(metacardId1));
        childId = ((CharSequenceValue) associationGetResponse.getChild()
                .get(Core.ID)).stringValue();
        assertThat(childId, is(metacardId2));
    }


    /* Remote Associations will not work until the ContentResourceReader is updated https://codice.atlassian.net/browse/DDF-2957 */
    @Test
    @Ignore
    public void testRemoteAssociation() {
        setMetacardCswQueryResponse();

        String metacardId1 = ingest(getFileContent("metacard1.xml"), MediaType.APPLICATION_XML);

        String associationJson = String.format(PUT_ASSOCIATION_STRING,
                metacardId1,
                REMOTE_METACARD_ID,
                RELATED,
                AssociationsAttributes.RELATED);

        Response putResponse = expect(asAdmin().body(associationJson), 200).put(
                associationsApi() + "/" + metacardId1);

        List<Object> associationsPutResponses = parseArray(AssociationsPutResponse.class,
                putResponse);
        AssociationsPutResponse associationsPutResponse =
                (AssociationsPutResponse) associationsPutResponses.get(0);
        String parentId = ((CharSequenceValue) associationsPutResponse.getParent()
                .get(Core.ID)).stringValue();
        assertThat(parentId, is(metacardId1));
        String childId = ((CharSequenceValue) associationsPutResponse.getChild()
                .get(Core.ID)).stringValue();
        assertThat(childId, is(REMOTE_METACARD_ID));
        assertThat(associationsPutResponse.getRelationship(), is(RELATED));
        assertThat(associationsPutResponse.getRelation(), is(AssociationsAttributes.RELATED));

        Response getResponse = expect(asAdmin(), 200).get(associationsApi() + "/" + metacardId1);
        List<Object> associationGetResponses = parseArray(AssociationsPutResponse.class,
                getResponse);
        AssociationsGetResponse associationGetResponse =
                (AssociationsGetResponse) associationGetResponses.get(0);
        parentId = ((CharSequenceValue) associationGetResponse.getParent()
                .get(Core.ID)).stringValue();
        assertThat(parentId, is(metacardId1));
        childId = ((CharSequenceValue) associationGetResponse.getChild()
                .get(Core.ID)).stringValue();
        assertThat(childId, is(REMOTE_METACARD_ID));
    }

    private static String workspaceApi() {
        return WORKSPACE_URL.getUrl();
    }

    private static String associationsApi() {
        return ASSOCIATIONS_URL.getUrl();
    }

    private static Map parseMap(Response res) {
        return JsonFactory.create()
                .readValue(res.getBody()
                        .asInputStream(), Map.class);
    }

    private static void setEmptyCswQueryResponse() {
        byte[] bytes = getFileContent("default-csw-mock-query-empty-response.xml").getBytes();

        cswServer.whenHttp()
                .match(post("/services/csw"), withPostBodyContaining("GetRecords"))
                .then(ok(), contentType("text/xml"), bytesContent(bytes));
    }

    private static void setMetacardCswQueryResponse() {
        byte[] bytes = getFileContent("default-csw-mock-query-response-urn-metacard.xml",
                ImmutableMap.of("id", REMOTE_METACARD_ID, "source-id", CSW_STUB_SOURCE_ID)).getBytes();

        cswServer.whenHttp()
                .match(post("/services/csw"), withPostBodyContaining("GetRecords"))
                .then(ok(), contentType("text/xml"), bytesContent(bytes));
    }

    @SuppressWarnings("unchecked")
    private static List<Object> parseArray(Class clazz, Response res) {
        return JsonFactory.create()
                .parser()
                .parseList(clazz,
                        res.getBody()
                                .asInputStream());
    }

    private static String stringify(Object o) {
        return JsonFactory.create()
                .writeValueAsString(o);
    }

    private static RequestSpecification asGuest() {
        return given().log()
                .all()
                .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE);
    }

    private static RequestSpecification asUser(String username, String password) {
        return given().log()
                .all()
                .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
                .auth()
                .preemptive()
                .basic(username, password);
    }

    private static RequestSpecification asAdmin() {
        return given().log()
                .all()
                .header(CONTENT_TYPE_HEADER, JSON_MIME_TYPE)
                .auth()
                .preemptive()
                .basic(ADMIN, ADMIN);
    }

    private static ResponseSpecification expect(RequestSpecification req, int status) {
        return req.expect()
                .log()
                .all()
                .statusCode(status)
                .when();
    }

    private class AssociationsGetResponse {
        private Map<String, Object> parent;

        private Map<String, Object> child;

        public AssociationsGetResponse(Map<String, Object> parent, Map<String, Object> child) {
            this.parent = parent;
            this.child = child;
        }

        public Map<String, Object> getParent() {
            return parent;
        }

        public Map<String, Object> getChild() {
            return child;
        }
    }

    private class AssociationsPutResponse extends AssociationsGetResponse {
        private String relationship;

        private String relation;

        public AssociationsPutResponse(Map<String, Object> parent, Map<String, Object> child,
                String relationship, String relation) {
            super(parent, child);
            this.relationship = relationship;
            this.relation = relation;
        }

        public String getRelationship() {
            return relationship;
        }

        public String getRelation() {
            return relation;
        }

    }
}
