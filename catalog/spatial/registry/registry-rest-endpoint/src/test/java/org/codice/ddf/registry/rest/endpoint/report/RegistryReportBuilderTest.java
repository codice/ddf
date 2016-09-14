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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Arrays;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.transformer.RegistryTransformer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.impl.MetacardImpl;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class RegistryReportBuilderTest {
    private Parser parser;

    private ParserConfigurator configurator;

    private RegistryReportBuilder reportHelper;

    private RegistryTransformer registryTransformer;

    @Before
    public void setup() {
        parser = new XmlParser();
        registryTransformer = spy(new RegistryTransformer());
        registryTransformer.setParser(parser);
        registryTransformer.setRegistryMetacardType(new RegistryObjectMetacardType());
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

        reportHelper = new RegistryReportBuilder();
        reportHelper.setup();
    }

    @Test
    public void getErrorHtml() throws IOException {
        String html = reportHelper.getErrorHtml("New Error Message Here");
        assertThat(Jsoup.parse(html)
                .select(".ErrorMessage")
                .text(), is("New Error Message Here"));
    }

    @Test
    public void testSummaryReportWithSimplifiedRegistryPackage() throws Exception {
        MetacardImpl metacard = convertXmltoMetacard("/csw-simplified-registry-package.xml");
        String html = reportHelper.getSummaryHtmlFromMetacard(metacard);
        assertSummaryReportValues(Jsoup.parse(html));
    }

    @Test
    public void testSummaryReportWithSparseRegistryPackage() throws Exception {
        MetacardImpl metacard = convertXmltoMetacard("/csw-sparse-registry-package.xml");
        String html = reportHelper.getSummaryHtmlFromMetacard(metacard);
        assertSparseSummaryReportValues(Jsoup.parse(html));
    }

    @Test
    public void testOrganizationReportWithSimplifiedRegistryPackage() throws Exception {
        RegistryPackageType registryPackage = getRegistryPackageFromResource(
                "/csw-simplified-registry-package.xml");
        String html = reportHelper.getHtmlFromRegistryPackage(registryPackage,
                reportHelper.ORGANIZATIONS);
        assertOrganizationReportValues(Jsoup.parse(html));
    }

    @Test
    public void testFullReportWithSimplifiedRegistryPackage() throws Exception {
        RegistryPackageType registryPackage = getRegistryPackageFromResource(
                "/csw-simplified-registry-package.xml");
        String html = reportHelper.getHtmlFromRegistryPackage(registryPackage, reportHelper.REPORT);
        assertFullReportValues(Jsoup.parse(html));
    }

    private void assertSummaryReportValues(Document document) {

        String organizationName =
                document.select(".ValuePair:has(.key):contains(Organization Name)")
                        .select(".value")
                        .text();
        String phoneNumbers = document.select(".ValuePair:has(.key):contains(Phone Numbers)")
                .select(".value")
                .text();
        String emailAddresses = document.select(".ValuePair:has(.key):contains(Email Addresses)")
                .select(".value")
                .text();
        String address = document.select(".ValuePair:has(.key):contains(Street)")
                .select(".value")
                .text();
        String pointOfContact = document.select(".ValuePair:has(.key):contains(Point of Contact)")
                .select(".value")
                .text();

        assertThat(organizationName, is("Codice"));
        assertThat(phoneNumbers, is("(555) 555-5555 ext 1234"));
        assertThat(emailAddresses, is("emailaddress@something.com"));
        assertThat(address, is("1234 Some Street, Phoenix, AZ 85037, USA"));
        assertThat(pointOfContact,
                is("foo bar, +country (111) 111-1111 ext 1234, emailaddress@something.com"));
    }

    private void assertSparseSummaryReportValues(Document document) {
        String pointOfContact = document.select(".ValuePair:has(.key):contains(Point of Contact)")
                .select(".value")
                .text();

        assertThat(pointOfContact, is("no name, no telephone number, email that should not exist"));
    }

    private void assertOrganizationReportValues(Document document) {

        assertThat(document.select(".Address")
                .text(), is("1234 Some Street Phoenix, AZ 85037"));
        assertThat(document.select(".Phone")
                .text(), is("(555) 555-5555 ext. 1234"));
        assertThat(document.select(".Email")
                .text(), is("emailaddress@something.com"));
        assertThat(document.select(".Name")
                .text(), is("Codice"));
        assertThat(document.select(".Classifications:contains(classificationScheme)")
                .text(), is("classificationScheme"));

        String status = document.select(".ValuePair:has(.key):contains(Status)")
                .select(".value")
                .text();
        String parent = document.select(".ValuePair:has(.key):contains(parent)")
                .select(".value")
                .text();
        String lid = document.select(".ValuePair:has(.key):contains(Lid)")
                .select(".value")
                .text();

        assertThat(status, is("someStatus"));
        assertThat(parent, is("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(lid, is("someLid"));
    }

    private void assertFullReportValues(Document document) {

        //node info
        assertNodeInfo(document);
        //services
        assertServices(document);
        //organizations
        assertOrganizationReportValues(document);
        //contacts
        assertContacts(document);
        //collections
        assertCollections(document);
    }

    private void assertNodeInfo(Document document) {
        String nodeName = document.select(".NodeName:contains(Node Name)")
                .text();
        String inputDataSources = document.select(
                ".NodeValuePair:has(.NodeKey):contains(inputDataSources)")
                .select(".NodeValue")
                .text();
        String parameters = document.select(".NodeValuePair:has(.NodeKey):contains(parameters)")
                .select(".NodeValue")
                .text();
        String versionNumber = document.select(".NodeValuePair:has(.NodeKey):contains(VersionInfo)")
                .select(".NodeValue")
                .first()
                .text();
        String objectType = document.select(".NodeValuePair:has(.NodeKey):contains(objectType)")
                .select(".NodeValue")
                .text();

        assertThat(nodeName, is("Node Name"));
        assertThat(parameters, is("param1"));
        assertThat(versionNumber, is("2.10.x"));
        assertThat(inputDataSources, is("youtube, myCamera"));
        assertThat(objectType, is("urn:registry:federation:node"));

    }

    private void assertServices(Document document) {
        String versionInfo = document.select(
                ".ServiceValuePair:has(.ServiceKey):contains(VersionInfo)")
                .select(".ServiceValue")
                .text();
        String description = document.select(
                ".ServiceValuePair:has(.ServiceKey):contains(Description)")
                .select(".ServiceValue")
                .text();
        String id = document.select(".ServiceValuePair:has(.ServiceKey):contains(id)")
                .select(".ServiceValue")
                .text();
        String name = document.select(".ServiceValuePair:has(.ServiceKey):contains(Name)")
                .select(".ServiceValue")
                .text();
        String objectType = document.select(
                ".ServiceValuePair:has(.ServiceKey):contains(objectType)")
                .select(".ServiceValue")
                .text();
        String cswUrl = document.select(".ServiceValuePair:has(.ServiceKey):contains(cswUrl)")
                .select(".ServiceValue")
                .text();
        String bindingType = document.select(
                ".ServiceValuePair:has(.ServiceKey):contains(bindingType)")
                .select(".ServiceValue")
                .text();
        assertThat(versionInfo, is("2.10.x"));
        assertThat(description, is("Service Description"));
        assertThat(id, is("urn:service:id0"));
        assertThat(name, is("Service Name"));
        assertThat(objectType, is("urn:registry:federation:service"));
        assertThat(cswUrl, is("https://some/address/here"));
        assertThat(bindingType, is("Csw_Federated_Source"));
    }

    private void assertContacts(Document document) {
        assertThat(document.select(".ContactAddress")
                .text(), is("1234 Some Street Phoenix , AZ 85037"));
        assertThat(document.select(".ContactPhone")
                .text(), is("(111) 111-1111 ext. 1234"));
        assertThat(document.select(".ContactEmail")
                .text(), is("emailaddress@something.com"));
        assertThat(document.select(".ContactName")
                .text(), is("foo bar"));

        String id = document.select(".ContactValuePair:has(.ContactKey):contains(id)")
                .select(".ContactValue")
                .text();

        assertThat(id, is("urn:contact:id0"));
    }

    private void assertCollections(Document document) {
        String versionInfo = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(VersionInfo)")
                .select(".CollectionValue")
                .first()
                .text();
        String description = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(Description)")
                .select(".CollectionValue")
                .text();
        String isOpaque = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(isOpaque)")
                .select(".CollectionValue")
                .text();
        String parameters = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(parameters)")
                .select(".CollectionValue")
                .text();
        String id = document.select(".CollectionValuePair:has(.CollectionKey):contains(id)")
                .select(".CollectionValue")
                .text();
        String mimeType = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(mimeType)")
                .select(".CollectionValue")
                .text();
        String name = document.select(".CollectionValuePair:has(.CollectionKey):contains(name)")
                .select(".CollectionValue")
                .first()
                .text();
        String contentVersionInfo = document.select(
                ".CollectionValuePair:has(.CollectionKey):contains(contentVersionInfo)")
                .select(".CollectionValue")
                .first()
                .text();
        assertThat(versionInfo, is("2.10.x"));
        assertThat(description, is("Non Federation Node Extrinsic Object Description"));
        assertThat(isOpaque, is("false"));
        assertThat(parameters, is("param1"));
        assertThat(id, is("urn:service:params:id0"));
        assertThat(mimeType, is("application/octet-stream"));
        assertThat(name, is("Another Name"));
        assertThat(contentVersionInfo, is("versionName"));
    }

    private RegistryPackageType getRegistryPackageFromResource(String path) throws ParserException {
        RegistryPackageType registryPackage = null;
        JAXBElement<RegistryPackageType> jaxbRegistryObject = parser.unmarshal(configurator,
                JAXBElement.class,
                getClass().getResourceAsStream(path));

        if (jaxbRegistryObject != null) {
            registryPackage = jaxbRegistryObject.getValue();
        }

        return registryPackage;
    }

    private MetacardImpl convertXmltoMetacard(String path) throws Exception {
        return (MetacardImpl) registryTransformer.transform(getClass().getResourceAsStream(path));
    }
}