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

import java.nio.charset.StandardCharsets;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.xml.sax.InputSource;

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
        String id = createRegistryEntry("urn:uuid:2014ca7f59ac46f495e32b4a67a51280");

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryUpdate()
                        .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                                "urn:uuid:2014ca7f59ac46f495e32b4a67a51280")
                        .replace("Node Name", "New Node Name")
                        .replaceAll("someUUID", id))
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
    public void testCswRegistryDelete() throws Exception {
        createRegistryEntry("urn:uuid:2014ca7f59ac46f495e32b4a67a51281");

        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryDelete()
                        .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                                "urn:uuid:2014ca7f59ac46f495e32b4a67a51281"))
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
        createRegistryEntry("urn:uuid:2014ca7f59ac46f495e32b4a67a51282");

        given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryInsert()
                        .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                                "urn:uuid:2014ca7f59ac46f495e32b4a67a51282"))
                .header("Content-Type", "text/xml")
                .expect()
                .log()
                .all()
                .statusCode(400)
                .when()
                .post(CSW_PATH.getUrl());
    }

    @Ignore
    @Test
    public void testCswRegistryStoreCreate() throws Exception {
        //Stub test, waiting for additional features/service before implementing
        //Use federation admin to create a registry entry
        //on a 'remote' (aka loopback) catalog store
    }

    @Ignore
    @Test
    public void testCswRegistryStoreUpdate() throws Exception {
        //Stub test, waiting for additional features/service before implementing
        //Use federation admin to update a registry entry
        //on a 'remote' (aka loopback) catalog store
    }

    @Ignore
    @Test
    public void testCswRegistryStoreDelete() throws Exception {
        //Stub test, waiting for additional features/service before implementing
        //Use federation admin to update a registry entry
        //on a 'remote' (aka loopback) catalog store
    }

    private String createRegistryEntry(String id) throws Exception {
        Response response = given().auth()
                .preemptive()
                .basic(ADMIN, ADMIN)
                .body(Library.getCswRegistryInsert()
                        .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276", id))
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
}

