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
package org.codice.ddf.registry.rest.endpoint.report;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryReportMapBuilderTest {
    private Parser parser;

    private ParserConfigurator configurator;

    private RegistryReportMapBuilder reportHelper;

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

        reportHelper = new RegistryReportMapBuilder();
    }

    @Test
    public void testWithSimplifiedRegistryPackage()
            throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-simplified-registry-package.xml");
        assertThatRegistryMapHasExpectedValues(reportHelper.buildRegistryMap((RegistryPackageType) registryObject));
    }

    @Test
    public void testWithBareRegistryPackage() throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-bare-registry-package.xml");
        assertThatRegistryMapisEmpty(reportHelper.buildRegistryMap((RegistryPackageType) registryObject));
    }

    @Test
    public void testWithSparseRegistryPackage() throws ParserException, FederationAdminException {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-sparse-registry-package.xml");
        assertThatRegistryMapDoesNotHaveCertainValues(reportHelper.buildRegistryMap((RegistryPackageType) registryObject));
    }

    private void assertThatRegistryMapHasExpectedValues(Map<String, Object> registryMap) {
        assertThat(registryMap.get("Name"), is("Node Name"));

        Map<String, Object> general = getMapFromMap(registryMap, "General");
        assertThat(general.get("Version Info"), is("2.10.x"));
        assertThat(general.get("Description"), is("Extrinsic Object Description"));
        assertThat(general.get("inputDataSources"), is("youtube, myCamera"));

        // Collections
        Map<String, Object> collections = getMapFromMap(registryMap, "Collections");
        Map<String, Object> anotherName = getMapFromMap(collections, "Another Name");
        assertThat(anotherName.get("parameters"), is("param1"));

        // Services
        Map<String, Object> services = getMapFromMap(registryMap, "Services");
        //        Map<String, Object> serviceName = getMapFromMap(services, "Service Name");
        Map<String, Object> service = getMapFromMap(services, "Service Name");
        Map<String, Object> bindings = getMapFromMap(service, "Bindings");
        Map<String, Object> properties = getMapFromMap(service, "Properties");
        Map<String, Object> cswFederatedSource = getMapFromMap(bindings, "Csw_Federated_Source");
        assertThat(cswFederatedSource.get("cswUrl"), is("https://some/address/here"));
        assertThat(cswFederatedSource.get("Service"),
                is("urn:uuid:service:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(cswFederatedSource.get("Access Url"), is("some:access:URI:any:URI"));
        assertThat(cswFederatedSource.get("Target binding"),
                is("some:target:binding:reference:URI"));
        assertThat(properties.get("Version Info"), is("2.10.x"));
        assertThat(properties.get("Description"), is("Service Description"));
        assertThat(properties.get("Name"), is("Service Name"));

        // Organizations
        Map<String, Object> organizations = getMapFromMap(registryMap, "Organizations");
        Map<String, Object> codice = getMapFromMap(organizations, "Codice");
        Map<String, Object> contactInfo = getMapFromMap(codice, "ContactInfo");
        assertThat(getStringListFromMap(contactInfo, "Addresses"),
                hasItem("1234 Some Street Phoenix AZ 85037 USA"));
        assertThat(getStringListFromMap(contactInfo, "Phone Numbers"),
                hasItem("555-555-5555 ext. 1234"));
        assertThat(getStringListFromMap(contactInfo, "Email Addresses"),
                hasItem("emailaddress@something.com"));

        // Contacts
        Map<String, Object> contacts = getMapFromMap(registryMap, "Contacts");
        Map<String, Object> fooJoeBar = getMapFromMap(contacts, "foo joe bar");
        contactInfo = getMapFromMap(fooJoeBar, "ContactInfo");
        assertThat(getStringListFromMap(contactInfo, "Addresses"),
                hasItem("1234 Some Street Phoenix AZ 85037 USA"));
        assertThat(getStringListFromMap(contactInfo, "Phone Numbers"),
                hasItem("country-111-111-1111 ext. 1234 (cell phone)"));
        assertThat(getStringListFromMap(contactInfo, "Email Addresses"),
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
        Map<String, Object> contacts = getMapFromMap(registryMap, "Contacts");
        Map<String, Object> foo = getMapFromMap(contacts, "foo");
        Map<String, Object> contactInfo = getMapFromMap(foo, "ContactInfo");

        assertThat(getStringListFromMap(contactInfo, "Addresses"), hasItem("1234 Some Street"));
        assertThat(getStringListFromMap(contactInfo, "Phone Numbers"), hasItem("111-1111"));
    }

    private Map<String, Object> getMapFromMap(Map<String, Object> map, String key) {
        Map<String, Object> innerMap = Collections.emptyMap();

        if (map.get(key) instanceof Map) {
            innerMap = (Map<String, Object>) map.get(key);
        }

        assertThat(innerMap, not(is(equalTo(Collections.emptyMap()))));
        return innerMap;
    }

    private List<String> getStringListFromMap(Map<String, Object> map, String key) {
        List<String> list = Collections.emptyList();

        if (map.get(key) instanceof List) {
            list = (List<String>) map.get(key);
        }

        assertThat(list, not(empty()));
        return list;
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
