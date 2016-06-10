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
package org.codice.ddf.registry.report.viewer;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryReportViewerTest {

    private Parser parser;

    private ParserConfigurator configurator;

    private FederationAdminService federationAdminService = mock(FederationAdminService.class);

    private RegistryReportViewer reportViewer;

    @Before
    public void setup() {
        parser = new XmlParser();

        configurator = parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                EbrimConstants.OGC_FACTORY.getClass()
                        .getPackage()
                        .getName(),
                EbrimConstants.GML_FACTORY.getClass()
                        .getPackage()
                        .getName()),
                this.getClass()
                        .getClassLoader());

        reportViewer = new RegistryReportViewer();
        reportViewer.setFederationAdminService(federationAdminService);
    }

    @Test
    public void testWithBlankMetacard() {
        RegistryReportViewer reportViewer = new RegistryReportViewer();
        Response response = reportViewer.viewRegInfoHtml(null, null);
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void testWithFederationAdminException() throws FederationAdminException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anyList())).thenThrow(new FederationAdminException());
        Response response = reportViewer.viewRegInfoHtml("registryId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void testRegistryPackageNotFound() throws FederationAdminException {
        when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                anyList())).thenReturn(null);
        Response response = reportViewer.viewRegInfoHtml("registryId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void testPersonsWithoutNames() throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-sparse-registry-package.xml");
        if (registryObject instanceof RegistryPackageType) {
            when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                    anyList())).thenReturn((RegistryPackageType) registryObject);

            assertThatPersonsMapIsCorrect(reportViewer.buildRegistryMap((RegistryPackageType) registryObject));

            Response response = reportViewer.viewRegInfoHtml("registryId",
                    Arrays.asList("sourceIds"));
            assertThat(response.getStatusInfo()
                    .getStatusCode(), is(Response.Status.OK.getStatusCode()));
        }

    }

    @Test
    public void testWithSimplifiedRegistryPackage()
            throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-simplified-registry-package.xml");
        if (registryObject instanceof RegistryPackageType) {
            when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                    anyList())).thenReturn((RegistryPackageType) registryObject);
        }
        assertThatRegistryMapHasExpectedValues(reportViewer.buildRegistryMap((RegistryPackageType) registryObject));

        Response response = reportViewer.viewRegInfoHtml("registryId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));

    }

    @Test
    public void testWithBareRegistryPackage() throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-bare-registry-package.xml");
        if (registryObject instanceof RegistryPackageType) {
            when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                    anyList())).thenReturn((RegistryPackageType) registryObject);
        }
        RegistryReportViewer reportViewer = new RegistryReportViewer();
        reportViewer.setFederationAdminService(federationAdminService);

        assertThatRegistryMapisEmpty(reportViewer.buildRegistryMap((RegistryPackageType) registryObject));

        Response response = reportViewer.viewRegInfoHtml("registryId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testWithSparseRegistryPackage() throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-sparse-registry-package.xml");
        if (registryObject instanceof RegistryPackageType) {
            when(federationAdminService.getRegistryObjectByRegistryId(anyString(),
                    anyList())).thenReturn((RegistryPackageType) registryObject);
        }
        RegistryReportViewer reportViewer = new RegistryReportViewer();
        reportViewer.setFederationAdminService(federationAdminService);

        assertThatRegistryMapDoesNotHaveCertainValues(reportViewer.buildRegistryMap((RegistryPackageType) registryObject));

        Response response = reportViewer.viewRegInfoHtml("registryId", Arrays.asList("sourceIds"));
        assertThat(response.getStatusInfo()
                .getStatusCode(), is(Response.Status.OK.getStatusCode()));
    }

    private void assertThatPersonsMapIsCorrect(Map<String, Object> registryMap) {
        Map<String, Object> personContactInfo =
                (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) registryMap.get(
                        "Contacts")).get("foo")).get("ContactInfo");
        assertThat((ArrayList<String>) personContactInfo.get("Phone Numbers"), hasItem("111-1111"));
        assertThat(((Map<String, Object>) registryMap.get("Contacts")).size(), is(1));
    }

    private void assertThatRegistryMapHasExpectedValues(Map<String, Object> registryMap) {
        assertThat(registryMap.get("Name"), is("Node Name"));

        assertThat(((Map<String, Object>) registryMap.get("General")).get("inputDataSources"),
                is("youtube, myCamera"));

        assertThat(((Map<String, Object>) ((Map<String, Object>) registryMap.get("Collections")).get(
                "Another Name")).get("parameters"), is("param1"));

        assertThat(((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) registryMap.get(
                "Services")).get("Service")).get("Bindings")).get("Csw_Federated_Source")).get(
                "cswUrl"), is("https://some/address/here"));

        Map<String, Object> orgContactInfo =
                (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) registryMap.get(
                        "Organizations")).get("Codice")).get("ContactInfo");
        assertThat((ArrayList<String>) orgContactInfo.get("Addresses"),
                hasItem("1234 Some Street Phoenix AZ 85037 USA"));
        assertThat((ArrayList<String>) orgContactInfo.get("Phone Numbers"),
                hasItem("555-555-5555 ext. 1234"));
        assertThat((ArrayList<String>) orgContactInfo.get("Email Addresses"),
                hasItem("emailaddress@something.com"));

        Map<String, Object> personContactInfo =
                (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) registryMap.get(
                        "Contacts")).get("foo joe bar")).get("ContactInfo");
        assertThat((ArrayList<String>) personContactInfo.get("Addresses"),
                hasItem("1234 Some Street Phoenix AZ 85037 USA"));
        assertThat((ArrayList<String>) personContactInfo.get("Phone Numbers"),
                hasItem("country-111-111-1111 ext. 1234 (cell phone)"));
        assertThat((ArrayList<String>) personContactInfo.get("Email Addresses"),
                hasItem("emailaddress@something.com"));
    }

    private void assertThatRegistryMapisEmpty(Map<String, Object> registryMap) {
        assertThat(registryMap.get("Name"), is(""));
        assertThat(((Map<String, Object>) registryMap.get("General")).size(), is(0));
        assertThat(((Map<String, Object>) registryMap.get("Collections")).size(), is(0));
        assertThat(((Map<String, Object>) registryMap.get("Services")).size(), is(0));
        assertThat(((Map<String, Object>) registryMap.get("Organizations")).size(), is(0));
        assertThat(((Map<String, Object>) registryMap.get("Contacts")).size(), is(0));
    }

    private void assertThatRegistryMapDoesNotHaveCertainValues(Map<String, Object> registryMap) {
        Map<String, Object> personContactInfo =
                (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) registryMap.get(
                        "Contacts")).get("foo")).get("ContactInfo");
        assertThat((ArrayList<String>) personContactInfo.get("Addresses"),
                hasItem("1234 Some Street"));
        assertThat((ArrayList<String>) personContactInfo.get("Phone Numbers"), hasItem("111-1111"));

    }

    private RegistryObjectType getRegistryObjectFromResource(String path) throws ParserException {
        RegistryObjectType registryObject = null;
        JAXBElement<RegistryObjectType> jaxbRegistryObject = parser.unmarshal(configurator,
                JAXBElement.class,
                getClass().getResourceAsStream(path));

        if (jaxbRegistryObject != null) {
            registryObject = jaxbRegistryObject.getValue();
        }

        return registryObject;
    }

}
