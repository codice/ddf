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

import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.nio.charset.StandardCharsets;
import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.xml.sax.InputSource;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.Library;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestRegistry extends AbstractIntegrationTest {

    private static final String CATALOG_REGISTRY = "registry-app";

    private static final String CATALOG_REGISTRY_CORE = "registry-core";

    private static final String REGISTRY_CATALOG_STORE_ID = "cswRegistryCatalogStore";

    private static final String ADMIN = "admin";

    private Set<String> destinations;

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
            CswRegistryStoreProperties cswRegistryStoreProperties = new CswRegistryStoreProperties(
                    REGISTRY_CATALOG_STORE_ID);
            getServiceManager().createManagedService(cswRegistryStoreProperties.FACTORY_PID,
                    cswRegistryStoreProperties);
            getCatalogBundle().waitForCatalogStore(REGISTRY_CATALOG_STORE_ID);
            destinations = new HashSet<>();
            destinations.add(REGISTRY_CATALOG_STORE_ID);
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }

    }

    @Test
    public void testCswRegistryIngest() throws Exception {
        createRegistryEntry("urn:uuid:2014ca7f59ac46f495e32b4a67a51279");
    }

    @Test
    public void testCswRegistryUpdate() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51285";
        String id = createRegistryEntry(regID);

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryUpdate(regID,
                        "New Node Name",
                        "2018-02-26T17:16:34.996Z",
                        id))
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
        String id = createRegistryEntry(regID);
        given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryUpdate(regID,
                        "New Node Name",
                        "2014-02-26T17:16:34.996Z",
                        id))
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
        createRegistryEntry(regID);

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryDelete(regID))
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

    @Test
    public void testCswRegistryCreateDup() throws Exception {
        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51282";
        createRegistryEntry(regID);

        given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryInsert(regID))
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
    public void testCswRegistryStoreCreate() throws Exception {

        String regID = "urn:uuid:2014ca7f59ac46f495e32b4a67a51277";
        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(regID);
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

        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(regID);
                FederationAdminService federationAdminServiceImpl = getServiceManager().getService(
                        FederationAdminService.class);
                federationAdminServiceImpl.updateRegistryEntry(Library.getRegistryNode(regID,
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
        try {
            Security.runAsAdminWithException(() -> {

                createRegistryStoreEntry(regID);
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
        createRegistryEntry(regId);

        final String restUrl = SERVICE_ROOT.getUrl() + "/registries/" + regId + "/report";

        ValidatableResponse response = when().get(restUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .contentType("text/html");

        final String xPathServices = "//html/body/h4";

        response.body(hasXPath(xPathServices, CoreMatchers.is("Csw_Federated_Source")),
                hasXPath(xPathServices + "[2]", CoreMatchers.is("soap13")));
    }

    private String createRegistryEntry(String id) throws Exception {
        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryInsert(id))
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

    private void createRegistryStoreEntry(String id) throws Exception {
        FederationAdminService federationAdminServiceImpl = getServiceManager().getService(
                FederationAdminService.class);
        federationAdminServiceImpl.addRegistryEntry(Library.getRegistryNode(id), destinations);

        ValidatableResponse validatableResponse = getCswRegistryResponse("registry-id", id);

        final String xPathRegistryID =
                "string(//GetRecordsResponse/SearchResults/RegistryPackage/ExternalIdentifier/@registryObject)";
        validatableResponse.body(hasXPath(xPathRegistryID, CoreMatchers.is(id)));
    }

    private ValidatableResponse getCswRegistryResponse(String attr, String value) {
        String regQuery = Library.getCswQuery(attr,
                value,
                "application/xml",
                "urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0");
        LOGGER.error("##### {}", regQuery);
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
}

