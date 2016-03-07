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
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.registry.api.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.source.IngestException;
import ddf.common.test.BeforeExam;
import ddf.security.SecurityConstants;
import ddf.security.common.util.Security;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.Library;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestRegistry extends AbstractIntegrationTest {

    private static final String CATALOG_REGISTRY = "catalog-registry";

    private static final String REGISTRY_CATALOG_STORE_ID = "cswRegistryCatalogStore";

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().startFeature(true, CATALOG_REGISTRY);
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");
            CswRegistryStoreProperties cswRegistryStoreProperties = new CswRegistryStoreProperties(
                    REGISTRY_CATALOG_STORE_ID);
            getServiceManager().createManagedService(cswRegistryStoreProperties.FACTORY_PID,
                    cswRegistryStoreProperties);
            getCatalogBundle().waitForCatalogStore(REGISTRY_CATALOG_STORE_ID);

            MetacardImpl metacard = new MetacardImpl(new RegistryObjectMetacardType());
            metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
            metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                    "urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
            metacard.setMetadata(Library.getRegistryNode());
            metacard.setId("origId");
            Map<String, Serializable> properties = new HashMap<>();
            properties.put(SecurityConstants.SECURITY_SUBJECT, Security.getSystemSubject());

            CreateRequest createRequest = new CreateRequestImpl(Collections.singletonList(metacard),
                    properties);
            getCatalogBundle().getCatalogFramework()
                    .create(createRequest);

        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Test
    public void testCswRegistryIngest() throws Exception {

        given().body(Library.getCswRegistryInsert()
                .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                        "urn:uuid:2014ca7f59ac46f495e32b4a67a51279"))
                .header("Content-Type", "text/xml")
                .expect()
                .log()
                .all()
                .statusCode(200)
                .when()
                .post(CSW_PATH.getUrl())
                .getHeader("id");

    }

    @Test
    public void testCswRegistryCreate() throws Exception {
        ArrayList<Metacard> metacards = new ArrayList<>();
        Map<String, Serializable> properties = new HashMap<>();
        Set<String> destinations = new HashSet<>();

        MetacardImpl metacard = new MetacardImpl(new RegistryObjectMetacardType());
        metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51277"));
        metacard.setMetadata(Library.getRegistryNode()
                .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                        "urn:uuid:2014ca7f59ac46f495e32b4a67a51277"));
        metacards.add(metacard);

        properties.put(SecurityConstants.SECURITY_SUBJECT, Security.getSystemSubject());

        destinations.add(REGISTRY_CATALOG_STORE_ID);

        CreateRequest createRequest = new CreateRequestImpl(metacards, properties, destinations);
        getCatalogBundle().getCatalogFramework()
                .create(createRequest);
    }

    @Test(expected = IngestException.class)
    public void testCswRegistryCreateDup() throws Exception {
        ArrayList<Metacard> metacards = new ArrayList<>();
        Map<String, Serializable> properties = new HashMap<>();

        MetacardImpl metacard = new MetacardImpl(new RegistryObjectMetacardType());
        metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        metacard.setMetadata(Library.getRegistryNode());
        metacards.add(metacard);

        properties.put(SecurityConstants.SECURITY_SUBJECT, Security.getSystemSubject());

        CreateRequest createRequest = new CreateRequestImpl(metacards, properties);
        getCatalogBundle().getCatalogFramework()
                .create(createRequest);

    }

    @Test
    public void testCswRegistryUpdate() throws Exception {

        Map<String, Serializable> properties = new HashMap<>();
        Set<String> destinations = new HashSet<>();

        MetacardImpl metacard = new MetacardImpl(new RegistryObjectMetacardType());
        metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        metacard.setMetadata(Library.getRegistryNode());
        metacard.setId("origId");

        properties.put(SecurityConstants.SECURITY_SUBJECT, Security.getSystemSubject());

        //        CreateRequest createRequest = new CreateRequestImpl(Collections.singletonList(metacard),
        //                properties);
        //        getCatalogBundle().getCatalogFramework()
        //                .create(createRequest);

        metacard.setMetadata(Library.getRegistryNode()
                .replace("Node Name", "New Node Name"));

        Map<String, Metacard> updateMap = new HashMap<>();
        updateMap.put("urn:uuid:2014ca7f59ac46f495e32b4a67a51276", metacard);

        destinations.add(REGISTRY_CATALOG_STORE_ID);

        UpdateRequest updateRequest = new UpdateRequestImpl(new ArrayList(updateMap.entrySet()),
                RegistryObjectMetacardType.REGISTRY_ID,
                properties,
                destinations);

        UpdateResponse updateResponse = getCatalogBundle().getCatalogFramework()
                .update(updateRequest);

        assertThat(updateResponse.getUpdatedMetacards()
                .size(), is(1));
        assertThat(updateResponse.getUpdatedMetacards()
                .get(0)
                .getNewMetacard()
                .getMetadata()
                .contains("New Node Name"), is(true));

    }

    @Test
    public void testCswRegistryDelete() throws Exception {

        Map<String, Serializable> properties = new HashMap<>();
        Set<String> destinations = new HashSet<>();

        MetacardImpl metacard = new MetacardImpl(new RegistryObjectMetacardType());
        metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51278"));

        metacard.setMetadata(Library.getRegistryNode()
                .replaceAll("urn:uuid:2014ca7f59ac46f495e32b4a67a51276",
                        "urn:uuid:2014ca7f59ac46f495e32b4a67a51278"));

        properties.put(SecurityConstants.SECURITY_SUBJECT, Security.getSystemSubject());

        CreateRequest createRequest = new CreateRequestImpl(Collections.singletonList(metacard),
                properties);
        CreateResponse createResponse = getCatalogBundle().getCatalogFramework()
                .create(createRequest);
        String id = createResponse.getCreatedMetacards()
                .get(0)
                .getId();

        destinations.add(REGISTRY_CATALOG_STORE_ID);
        DeleteRequest deleteRequest = new DeleteRequestImpl(Collections.singletonList(id),
                Metacard.ID,
                properties,
                destinations);

        DeleteResponse deleteResponse = getCatalogBundle().getCatalogFramework()
                .delete(deleteRequest);

        assertThat(deleteResponse.getDeletedMetacards()
                .size(), is(1));
        assertThat(deleteResponse.getDeletedMetacards()
                .get(0)
                .getMetadata()
                .contains("2014ca7f59ac46f495e32b4a67a51278"), is(true));
    }
}

