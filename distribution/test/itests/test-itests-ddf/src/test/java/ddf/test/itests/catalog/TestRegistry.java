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

import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswQuery;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswRegistryStoreProperties;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.xml.sax.InputSource;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestRegistry extends AbstractIntegrationTest {

    private static final String CATALOG_REGISTRY = "registry-app";

    private static final String CATALOG_REGISTRY_CORE = "registry-core";

    private static final String REGISTRY_CATALOG_STORE_ID = "cswRegistryCatalogStore";

    private static final String ADMIN = "admin";

    private Set<String> destinations;

    private static final String CSW_REGISTRY_TYPE = "CSW Registry Store";

    public static final String FACTORY_PID = "Csw_Registry_Store";

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().startFeature(true, CATALOG_REGISTRY);
            getServiceManager().waitForAllBundles();
            getServiceManager().startFeature(true, CATALOG_REGISTRY_CORE);
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");
            getServiceManager().createManagedService(FACTORY_PID,
                    getCswRegistryStoreProperties(REGISTRY_CATALOG_STORE_ID,
                            CSW_PATH.getUrl(),
                            getServiceManager()));
            getCatalogBundle().waitForCatalogStore(String.format("%s (%s:%s) (%s)",
                    SystemInfo.getSiteName(),
                    SystemBaseUrl.getHost(),
                    SystemBaseUrl.getPort(),
                    CSW_REGISTRY_TYPE));
            destinations = new HashSet<>();
            destinations.add(REGISTRY_CATALOG_STORE_ID);
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }

    }

    @Test
    public void testCswRegistryIngest() throws Exception {
        createRegistryEntry("2014ca7f59ac46f495e32b4a67a51279",
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51279");
    }

    @Test
    public void testCswRegistryUpdate() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51285";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51285";
        String id = createRegistryEntry(mcardId, regID);

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(getCswRegistryUpdate(id, "New Node Name", "2018-02-26T17:16:34.996Z", regID))
                .header("Content-Type", "text/xml")
                .expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .post(CSW_PATH.getUrl());

        ValidatableResponse validatableResponse = response.then();

        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted",
                CoreMatchers.is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated",
                        CoreMatchers.is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                        CoreMatchers.is("0")));
    }

    @Test
    public void testCswRegistryUpdateFailure() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51280";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51280";
        String id = createRegistryEntry(mcardId, regID);
        given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(getCswRegistryUpdate(regID, "New Node Name", "2014-02-26T17:16:34.996Z", id))
                .log()
                .all()
                .header("Content-Type", "text/xml")
                .when()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(400);
    }

    @Test
    public void testCswRegistryDelete() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51281";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51281";
        createRegistryEntry(mcardId, regID);

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswRegistryDeleteRequest",
                        ImmutableMap.of("id", mcardId)))
                .header("Content-Type", "text/xml")
                .expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .post(CSW_PATH.getUrl());

        ValidatableResponse validatableResponse = response.then();

        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted",
                CoreMatchers.is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated",
                        CoreMatchers.is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                        CoreMatchers.is("1")));
    }

    // TODO: tbatie - 9/18/16 - REMOVE ME
    //
    //    public static String getCswRegistryDelete(String id) throws IOException {
    //        return "<csw:Transaction service=\"CSW\"\n"
    //                + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
    //                + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
    //                + "  <csw:Delete typeName=\"rim:RegistryPackage\" handle=\"something\">\n"
    //                + "    <csw:Constraint version=\"2.0.0\">\n" + "      <ogc:Filter>\n"
    //                + "        <ogc:PropertyIsEqualTo>\n"
    //                + "            <ogc:PropertyName>registry.local.remote-metacard-id</ogc:PropertyName>\n"
    //                + "            <ogc:Literal>" + id + "</ogc:Literal>\n"
    //                + "        </ogc:PropertyIsEqualTo>\n" + "      </ogc:Filter>\n"
    //                + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";
    //    }

    @Test
    public void testCswRegistryStoreCreate() throws Exception {

        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51277";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51277";
        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(mcardId, regID, regID);
                return null;
            });
        } catch (PrivilegedActionException e) {
            String message = "There was an error bringing up the Federation Admin Service.";
            LOGGER.error(message, e);
            throw new Exception(message, e);
        }
    }

    @Test
    public void testCswRegistryStoreUpdate() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51290";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51290";

        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(mcardId, regID, regID);

                FederationAdminService federationAdminServiceImpl = getServiceManager().getService(
                        FederationAdminService.class);
                federationAdminServiceImpl.updateRegistryEntry(getRegistryNode(mcardId,
                        regID,
                        regID,
                        "New Node Name",
                        "2016-02-26T17:16:34.996Z"), destinations);

                ValidatableResponse validatableResponse = getCswRegistryResponse("title",
                        "New Node Name");

                final String xPathRegistryName =
                        "string(//GetRecordsResponse/SearchResults/RegistryPackage/RegistryObjectList/ExtrinsicObject/Name/LocalizedString/@value)";
                validatableResponse.body(hasXPath(xPathRegistryName,
                        CoreMatchers.is("New Node Name")));
                return null;
            });
        } catch (PrivilegedActionException e) {
            String message = "There was an error bringing up the Federation Admin Service.";
            LOGGER.error(message, e);
            throw new Exception(message, e);
        }
    }

    @Test
    public void testCswRegistryStoreDelete() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51291";
        String mcardId = "2014ca7f59ac46f495e32b4a67a51291";
        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(mcardId, regID, regID);
                FederationAdminService federationAdminServiceImpl = getServiceManager().getService(
                        FederationAdminService.class);

                List<String> toBeDeletedIDs = new ArrayList<>();
                toBeDeletedIDs.add(regID);

                federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(toBeDeletedIDs,
                        destinations);

                ValidatableResponse validatableResponse = getCswRegistryResponse("registry-id",
                        regID);

                final String xPathNumRecords =
                        "//GetRecordsResponse/SearchResults/@numberOfRecordsMatched";
                validatableResponse.body(hasXPath(xPathNumRecords, CoreMatchers.is("0")));
                return null;
            });
        } catch (PrivilegedActionException e) {
            String message = "There was an error bringing up the Federation Admin Service.";
            LOGGER.error(message, e);
            throw new Exception(message, e);
        }

    }

    @Test
    public void testRestEndpoint() throws Exception {
        final String regId = "urn:uuid:2014ca7f59ac46f495e32b4a67a51292";
        final String mcardId = "2014ca7f59ac46f495e32b4a67a51292";
        createRegistryEntry(mcardId, regId);

        final String restUrl = SERVICE_ROOT.getUrl() + "/internal/registries/" + regId + "/report";

        ValidatableResponse response = when().get(restUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .contentType("text/html");

        final String xPathServices = "//html/body/h4";

        response.body(hasXPath(xPathServices, CoreMatchers.is("CSW Federation Method")),
                hasXPath(xPathServices + "[2]", CoreMatchers.is("Soap Federation Method")));
    }

    private String createRegistryEntry(String id, String regId) throws Exception {
        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(getCswRegistryInsert(id, regId))
                .header("Content-Type", "text/xml")
                .expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .post(CSW_PATH.getUrl());
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted",
                CoreMatchers.is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated",
                        CoreMatchers.is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                        CoreMatchers.is("0")));

        XPath xPath = XPathFactory.newInstance()
                .newXPath();
        String idPath = "//*[local-name()='identifier']/text()";
        InputSource xml = new InputSource(IOUtils.toInputStream(response.getBody()
                .asString(), StandardCharsets.UTF_8.name()));
        return xPath.compile(idPath)
                .evaluate(xml);
    }

    private void createRegistryStoreEntry(String id, String regId, String remoteRegId)
            throws Exception {
        FederationAdminService federationAdminServiceImpl = getServiceManager().getService(
                FederationAdminService.class);
        federationAdminServiceImpl.addRegistryEntry(getRegistryNode(id, regId, remoteRegId),
                destinations);

        ValidatableResponse validatableResponse =
                getCswRegistryResponse(RegistryObjectMetacardType.REGISTRY_ID, regId);

        final String xPathRegistryID =
                "string(//GetRecordsResponse/SearchResults/RegistryPackage/ExternalIdentifier/@registryObject)";
        validatableResponse.body(hasXPath(xPathRegistryID, CoreMatchers.is(regId)));
    }

    private ValidatableResponse getCswRegistryResponse(String attr, String value) {
        String regQuery = getCswQuery(attr,
                value,
                "application/xml",
                "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .contentType(ContentType.XML)
                .body(regQuery)
                .when()
                .post(CSW_PATH.getUrl());
        ValidatableResponse validatableResponse = response.then();

        return validatableResponse;
    }

    public static String getCswRegistryInsert(String id, String regId) throws IOException {
        return getCswInsertRequest("rim:RegistryPackage", getRegistryNode(id, regId, regId));
    }

    public static String getCswRegistryUpdate(String id, String nodeName, String date, String uuid)
            throws IOException {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
                + "    <csw:Update typeName=\"rim:RegistryPackage\">\n" + getRegistryNode(id,
                uuid,
                uuid,
                nodeName,
                date) + "\n" + "    </csw:Update>\n" + "</csw:Transaction>";
    }

    public static String getRegistryNode(String id, String regId, String remoteRegId)
            throws IOException {
        return getRegistryNode(id, regId, remoteRegId, "Node Name", "2016-01-26T17:16:34.996Z");
    }

    public static String getRegistryNode(String mcardId, String regId, String remoteReg,
            String nodeName, String date) throws IOException {
        return getFileContent("csw-rim-node.xml",
                ImmutableMap.of("mcardId",
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
}

