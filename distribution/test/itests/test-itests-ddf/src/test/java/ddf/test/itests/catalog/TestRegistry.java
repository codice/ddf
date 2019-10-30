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
import static com.jayway.restassured.RestAssured.when;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.not;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static org.awaitility.Awaitility.await;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswRegistryStoreProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.xml.HasXPath.hasXPath;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.awaitility.Awaitility;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.XmlSearch;
import org.codice.ddf.itests.common.csw.mock.FederatedCswMockServer;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestRegistry extends AbstractIntegrationTest {

  public static final String FACTORY_PID = "Csw_Registry_Store";

  private static final String CATALOG_REGISTRY = "registry-app";

  private static final String CATALOG_REGISTRY_CORE = "registry-core";

  private static final String REGISTRY_CATALOG_STORE_ID = "cswRegistryCatalogStore";

  private static final String ADMIN = "admin";

  private static final String CSW_REGISTRY_TYPE = "CSW Registry Store";

  private static final DynamicPort CSW_STUB_SERVER_PORT = new DynamicPort(8);

  private static final String REMOTE_REGISTRY_ID = "urn:uuid:12121212121212121212121212121212";

  private static final String METACARD_ID = "12312312312312312312312312312312";

  private static final String REMOTE_METACARD_ID = "09876543210987654321098765432100";

  private static final long SLEEP_TIME = 2000;

  private static final Security SECURITY = Security.getInstance();

  private static String storeId;

  private static FederatedCswMockServer cswServer;

  private static String registryPid;

  private Set<String> destinations;

  public static String getCswRegistryInsert(String id, String regId) throws IOException {
    return getCswInsertRequest("rim:RegistryPackage", getRegistryNode(id, regId, regId));
  }

  public static String getCswRegistryUpdate(String id, String nodeName, String date, String uuid)
      throws IOException {
    return "<csw:Transaction\n"
        + "    service=\"CSW\"\n"
        + "    version=\"2.0.2\"\n"
        + "    verboseResponse=\"true\"\n"
        + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
        + "    <csw:Update typeName=\"rim:RegistryPackage\">\n"
        + getRegistryNode(id, uuid, uuid, nodeName, date)
        + "\n"
        + "    </csw:Update>\n"
        + "</csw:Transaction>";
  }

  public static String getRegistryNode(String id, String regId, String remoteRegId)
      throws IOException {
    return getRegistryNode(id, regId, remoteRegId, "Node Name", "2016-01-26T17:16:34.996Z");
  }

  public static String getRegistryNode(
      String mcardId, String regId, String remoteReg, String nodeName, String date)
      throws IOException {
    return getFileContent(
        "csw-rim-node.xml",
        ImmutableMap.of(
            "mcardId",
            mcardId,
            "nodeName",
            nodeName,
            "lastUpdated",
            date,
            "regId",
            regId,
            "remoteReg",
            remoteReg));
  }

  public static String getRegistryQueryResponse(
      String mcardId, String regId, String remoteReg, String nodeName, String date)
      throws IOException {
    return getFileContent(
        "default-csw-registry-query-response.xml",
        ImmutableMap.of(
            "mcardId",
            mcardId,
            "nodeName",
            nodeName,
            "lastUpdated",
            date,
            "regId",
            regId,
            "remoteReg",
            remoteReg));
  }

  public static String getRegistryInsertResponse(String mcardId, String mcardTitle)
      throws Exception {
    return getFileContent(
        "registry-csw-mock-insert-transaction-response.xml",
        ImmutableMap.of("mcardId", mcardId, "metacardTitle", mcardTitle));
  }

  public static String getCswQueryEmptyResponse() throws Exception {
    return "<?xml version='1.0' encoding='UTF-8'?>"
        + "<csw:GetRecordsResponse xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ows=\"http://www.opengis.net/ows\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" version=\"2.0.2\">\n"
        + "    <csw:SearchStatus timestamp=\"2017-01-17T09:34:28.597-07:00\"/>\n"
        + "    <csw:SearchResults numberOfRecordsMatched=\"0\" numberOfRecordsReturned=\"0\" nextRecord=\"0\" recordSchema=\"urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0\" elementSet=\"full\">\n"
        + "    </csw:SearchResults>\n"
        + "</csw:GetRecordsResponse>";
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      getServiceManager().startFeature(true, CATALOG_REGISTRY);
      getServiceManager().startFeature(true, CATALOG_REGISTRY_CORE);
      getServiceManager().waitForAllBundles();

      cswServer =
          new FederatedCswMockServer(
              "MockCswServer",
              "http://localhost:",
              Integer.parseInt(CSW_STUB_SERVER_PORT.getPort()));
      String defaultResponse =
          getRegistryQueryResponse(
              "11111111111111111111111111111111",
              REMOTE_REGISTRY_ID,
              REMOTE_REGISTRY_ID,
              "RemoteRegistry",
              "2018-02-26T17:16:34.996Z");
      cswServer.setupDefaultQueryResponseExpectation(defaultResponse);
      cswServer.start();
      waitForMockServer();
      registryPid =
          getServiceManager()
              .createManagedService(
                  FACTORY_PID,
                  getCswRegistryStoreProperties(
                      REGISTRY_CATALOG_STORE_ID,
                      "http://localhost:" + CSW_STUB_SERVER_PORT.getPort() + "/services/csw",
                      getServiceManager()))
              .getPid();
      storeId =
          String.format(
              "RemoteRegistry (localhost:%s) (%s)",
              CSW_STUB_SERVER_PORT.getPort(), CSW_REGISTRY_TYPE);

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @AfterExam
  public void afterExam() throws Exception {
    if (registryPid != null) {
      getServiceManager().stopManagedService(registryPid);
    }
    if (cswServer != null) {
      cswServer.stop();
    }
    getServiceManager().stopFeature(true, CATALOG_REGISTRY);
    getServiceManager().stopFeature(true, CATALOG_REGISTRY_CORE);
  }

  @Before
  public void setup() throws Exception {
    destinations = new HashSet<>();
    destinations.add(storeId);
    String defaultResponse =
        getRegistryQueryResponse(
            "11111111111111111111111111111111",
            REMOTE_REGISTRY_ID,
            REMOTE_REGISTRY_ID,
            "RemoteRegistry",
            "2018-02-26T17:16:34.996Z");
    cswServer.setupDefaultQueryResponseExpectation(defaultResponse);
    cswServer.reset();
    waitForMockServer();
  }

  @After
  public void tearDown() throws Exception {
    cswServer.stop();
    clearCatalog();
  }

  @Test
  public void testCswRegistryIngest() throws Exception {
    createRegistryEntry(
        "2014ca7f59ac46f495e32b4a67a51279", "urn:uuid:2014ca7f59ac46f495e32b4a67a51279");
  }

  @Test
  public void testCswRegistryUpdate() throws Exception {
    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51285";
    String mcardId = "2014ca7f59ac46f495e32b4a67a51285";
    String id = createRegistryEntry(mcardId, regID);

    Response response =
        given()
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .body(getCswRegistryUpdate(id, "New Node Name", "2018-02-26T17:16:34.996Z", regID))
            .header("Content-Type", "text/xml")
            .expect()
            .log()
            .ifValidationFails()
            .statusCode(200)
            .when()
            .post(CSW_PATH.getUrl());

    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", CoreMatchers.is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", CoreMatchers.is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", CoreMatchers.is("0")));
  }

  @Test
  public void testCswRegistryUpdateFailure() throws Exception {
    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51280";
    String mcardId = "2014ca7f59ac46f495e32b4a67a51280";
    String id = createRegistryEntry(mcardId, regID);
    given()
        .auth()
        .preemptive()
        .basic(ADMIN, ADMIN)
        .body(getCswRegistryUpdate(regID, "New Node Name", "2014-02-26T17:16:34.996Z", id))
        .log()
        .ifValidationFails()
        .header("Content-Type", "text/xml")
        .when()
        .post(CSW_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void testCswRegistryDelete() throws Exception {
    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51281";
    String mcardId = "2014ca7f59ac46f495e32b4a67a51281";
    createRegistryEntry(mcardId, regID);

    Response response =
        given()
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .body(
                getFileContent(
                    CSW_REQUEST_RESOURCE_PATH + "/CswRegistryDeleteRequest",
                    ImmutableMap.of("id", mcardId)))
            .header("Content-Type", "text/xml")
            .expect()
            .log()
            .ifValidationFails()
            .statusCode(200)
            .when()
            .post(CSW_PATH.getUrl());

    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", CoreMatchers.is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", CoreMatchers.is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", CoreMatchers.is("1")));
  }

  @Test
  public void testCswRegistryStoreCreate() throws Exception {

    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51277";

    cswServer.setupDefaultInsertTransactionResponseExpectation(
        getRegistryInsertResponse(REMOTE_METACARD_ID, "Node Name"));
    cswServer.reset();
    waitForMockServer();

    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL),
            withPostBodyContaining(METACARD_ID))
        .then(ok(), contentType("text/xml"), bytesContent(getCswQueryEmptyResponse().getBytes()));
    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(REMOTE_METACARD_ID))
        .then(
            ok(),
            contentType("text/xml"),
            bytesContent(
                getRegistryQueryResponse(
                        REMOTE_METACARD_ID,
                        regID,
                        REMOTE_REGISTRY_ID,
                        "NodeName",
                        "2016-01-26T17:16:34.996Z")
                    .getBytes()));

    waitForCatalogStoreVerify(
        () -> {
          String id = createRegistryStoreEntry(METACARD_ID, regID, regID);
          LOGGER.info("Created remote metacard with ID: {}", id);
          assertThat(id, is(regID));
          cswServer
              .verifyHttp()
              .times(
                  1,
                  withPostBodyContaining("Transaction"),
                  withPostBodyContaining("Insert"),
                  withPostBodyContaining(regID));
          return null;
        });
  }

  @Test
  public void testCswRegistryStoreCreateWithExistingRemoteEntry() throws Exception {

    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51277";

    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL),
            withPostBodyContaining(METACARD_ID))
        .then(
            ok(),
            contentType("text/xml"),
            bytesContent(
                getRegistryQueryResponse(
                        REMOTE_METACARD_ID,
                        regID,
                        REMOTE_REGISTRY_ID,
                        "NodeName",
                        "2016-01-26T17:16:34.996Z")
                    .getBytes()));

    waitForCatalogStoreVerify(
        () -> {
          String id = createRegistryStoreEntry(METACARD_ID, regID, regID);
          assertThat(id, is(regID));
          cswServer
              .verifyHttp()
              .times(
                  0,
                  withPostBodyContaining("Transaction"),
                  withPostBodyContaining("Ingest"),
                  withPostBodyContaining(METACARD_ID));

          return null;
        });
  }

  @Test
  public void testCswRegistryStoreUpdate() throws Exception {
    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51290";

    String remoteMetacardResponse =
        getRegistryQueryResponse(
            REMOTE_METACARD_ID, regID, REMOTE_REGISTRY_ID, "NodeName", "2016-01-26T17:16:34.996Z");
    String remoteUpdatedMetacardResponse =
        getRegistryQueryResponse(
            REMOTE_METACARD_ID,
            regID,
            REMOTE_REGISTRY_ID,
            "New Node Name",
            "2016-01-26T17:16:34.996Z");
    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL),
            withPostBodyContaining(METACARD_ID))
        .then(ok(), contentType("text/xml"), bytesContent(remoteMetacardResponse.getBytes()));
    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(RegistryConstants.REGISTRY_TAG),
            withPostBodyContaining(REMOTE_METACARD_ID))
        .then(ok(), contentType("text/xml"), bytesContent(remoteMetacardResponse.getBytes()));
    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            not(withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL)),
            withPostBodyContaining(REMOTE_METACARD_ID))
        .then(
            ok(), contentType("text/xml"), bytesContent(remoteUpdatedMetacardResponse.getBytes()));

    waitForCatalogStoreVerify(
        () -> {
          FederationAdminService federationAdminServiceImpl =
              getServiceManager().getService(FederationAdminService.class);
          federationAdminServiceImpl.updateRegistryEntry(
              getRegistryNode(
                  METACARD_ID, regID, regID, "New Node Name", "2016-02-26T17:16:34.996Z"),
              new HashSet(destinations));
          cswServer
              .verifyHttp()
              .times(1, withPostBodyContaining("Transaction"), withPostBodyContaining("Update"));
          cswServer.verifyHttp().atLeast(4);
          return null;
        });
  }

  @Test
  public void testCswRegistryStoreDelete() throws Exception {
    String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51291";

    String remoteMetacardResponse =
        getRegistryQueryResponse(
            REMOTE_METACARD_ID, regID, REMOTE_REGISTRY_ID, "NodeName", "2016-01-26T17:16:34.996Z");

    createRegistryEntry(METACARD_ID, regID);

    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL))
        .then(ok(), contentType("text/xml"), bytesContent(remoteMetacardResponse.getBytes()));
    cswServer
        .whenHttp()
        .match(
            post("/services/csw"),
            withPostBodyContaining("GetRecords"),
            not(withPostBodyContaining(RegistryConstants.REGISTRY_TAG_INTERNAL)),
            withPostBodyContaining(REMOTE_METACARD_ID))
        .then(ok(), contentType("text/xml"), bytesContent(remoteMetacardResponse.getBytes()));

    waitForCatalogStoreVerify(
        () -> {
          FederationAdminService federationAdminServiceImpl =
              getServiceManager().getService(FederationAdminService.class);

          List<String> toBeDeletedIDs = new ArrayList<>();
          toBeDeletedIDs.add(regID);

          federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(
              toBeDeletedIDs, new HashSet(destinations));
          cswServer
              .verifyHttp()
              .times(1, withPostBodyContaining("Transaction"), withPostBodyContaining("Delete"));
          cswServer.verifyHttp().atLeast(3);
          return null;
        });
  }

  @Test
  public void testRestEndpoint() throws Exception {
    final String regId = "urn:uuid:2014ca7f59ac46f495e32b4a67a51292";
    final String mcardId = "2014ca7f59ac46f495e32b4a67a51292";
    createRegistryEntry(mcardId, regId);

    final String restUrl = SERVICE_ROOT.getUrl() + "/internal/registries/" + regId + "/report";

    ValidatableResponse response =
        when().get(restUrl).then().log().all().assertThat().contentType("text/html");

    final String xPathServices = "//html/body/h4";

    response.body(
        hasXPath(xPathServices, CoreMatchers.is("CSW Federation Method")),
        hasXPath(xPathServices + "[2]", CoreMatchers.is("Soap Federation Method")));
  }

  private String createRegistryEntry(String id, String regId) throws Exception {
    Response response =
        given()
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .body(getCswRegistryInsert(id, regId))
            .header("Content-Type", "text/xml")
            .expect()
            .log()
            .ifValidationFails()
            .statusCode(200)
            .when()
            .post(CSW_PATH.getUrl());
    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", CoreMatchers.is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", CoreMatchers.is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", CoreMatchers.is("0")));

    String idPath = "//*[local-name()='identifier']/text()";
    String mcardId = XmlSearch.evaluate(idPath, response.getBody().asString());

    await("Waiting for metacard to be created")
        .atMost(10, TimeUnit.MINUTES)
        .pollDelay(SLEEP_TIME, TimeUnit.MILLISECONDS)
        .until(() -> areRegistryMetacardsPresent(regId));

    return mcardId;
  }

  private boolean areRegistryMetacardsPresent(String regId) throws PrivilegedActionException {
    return !SECURITY
        .runAsAdminWithException(
            () ->
                getServiceManager()
                    .getService(FederationAdminService.class)
                    .getRegistryMetacardsByRegistryIds(Collections.singletonList(regId)))
        .isEmpty();
  }

  private String createRegistryStoreEntry(String id, String regId, String remoteRegId)
      throws Exception {
    FederationAdminService federationAdminServiceImpl =
        getServiceManager().getService(FederationAdminService.class);
    return federationAdminServiceImpl.addRegistryEntry(
        getRegistryNode(id, regId, remoteRegId), new HashSet(destinations));
  }

  private void waitForMockServer() throws Exception {
    RetryPolicy retryPolicy =
        new RetryPolicy()
            .withMaxDuration(2, TimeUnit.MINUTES)
            .withDelay(SLEEP_TIME, TimeUnit.MILLISECONDS);
    Failsafe.with(retryPolicy).run(this::mockServerIsReady);
  }

  private void mockServerIsReady() {
    given()
        .auth()
        .preemptive()
        .basic(ADMIN, ADMIN)
        .body(
            "<csw:GetCapabilities service=\"CSW\" xmlns:csw=\"http://www.opengis.net/cat/csw\" xmlns:ows=\"http://www.opengis.net/ows\"/>")
        .header("Content-Type", "text/xml")
        .expect()
        .log()
        .ifValidationFails()
        .statusCode(200)
        .when()
        .post(CSW_PATH.getUrl());
  }

  private static void waitForCatalogStoreVerify(final Callable callable)
      throws PrivilegedActionException {
    SECURITY.runAsAdminWithException(
        () -> {
          Awaitility.given()
              .pollInterval(5, TimeUnit.SECONDS)
              .await()
              .atMost(5, TimeUnit.MINUTES)
              .untilAsserted(() -> callableAssertion(callable));
          return null;
        });
  }

  private static void callableAssertion(final Callable callable) {
    try {
      callable.call();
    } catch (Exception e) {
      throw new AssertionError(
          "There was an error interacting with the remote registry metacard.", e);
    }
  }
}
