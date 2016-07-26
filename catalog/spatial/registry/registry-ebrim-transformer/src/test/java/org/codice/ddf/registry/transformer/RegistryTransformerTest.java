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
 **/
package org.codice.ddf.registry.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.common.metacard.RegistryUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

@RunWith(JUnit4.class)
public class RegistryTransformerTest {

    private RegistryTransformer registryTransformer;

    private Parser parser;

    private void assertRegistryMetacard(Metacard meta) {
        assertThat(meta.getContentTypeName(),
                startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME));
    }

    @Before
    public void setUp() {
        registryTransformer = new RegistryTransformer();
        parser = new XmlParser();
        registryTransformer.setParser(parser);
        System.setProperty(RegistryConstants.REGISTRY_ID_PROPERTY, "identityRegistryId");

    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadInputStream() throws Exception {
        InputStream is = Mockito.mock(InputStream.class);
        doThrow(new IOException()).when(is)
                .read(any());
        registryTransformer.transform(is);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testParserReturnNull() throws Exception {
        Parser mockParser = Mockito.mock(Parser.class);
        when((mockParser).unmarshal(any(ParserConfigurator.class),
                any(Class.class),
                any(InputStream.class))).thenReturn(null);
        registryTransformer.setParser(mockParser);

        convert("/csw-basic-info.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testJaxbElementNullValue() throws Exception {
        JAXBElement<RegistryObjectType> mockRegistryObjectType = Mockito.mock(JAXBElement.class);
        when((mockRegistryObjectType).getValue()).thenReturn(null);

        Parser mockParser = Mockito.mock(Parser.class);
        when((mockParser).unmarshal(any(ParserConfigurator.class),
                any(Class.class),
                any(InputStream.class))).thenReturn(mockRegistryObjectType);

        registryTransformer.setParser(mockParser);
        convert("/csw-basic-info.xml");
    }

    @Test
    public void testBasicTransformWithoutId() throws Exception {
        assertRegistryMetacard(convert("/csw-rim-node.xml"));
    }

    @Test
    public void testBasicTransformWithId() throws Exception {
        InputStream is = getClass().getResourceAsStream("/csw-rim-node.xml");
        Metacard meta = registryTransformer.transform(is, "my-id");
        assertRegistryMetacard(meta);
    }

    @Test
    public void testBasicInfo() throws Exception {
        MetacardImpl meta = convert("/csw-basic-info.xml");
        assertRegistryMetacard(meta);

        assertThat(RegistryUtility.getStringAttribute(meta,
                RegistryObjectMetacardType.REGISTRY_ID,
                null), is("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(meta.getTitle(), is("my service"));
        assertThat(meta.getDescription(), is("something"));
        assertThat(meta.getContentTypeVersion(), is("0.0.0"));
    }

    @Test
    public void testOrgInfo() throws Exception {
        MetacardImpl metacard = convert("/csw-org-info.xml");
        assertRegistryMetacard(metacard);

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_NAME,
                null), is("Codice"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_ADDRESS),
                hasItem("1234 Some Street, Phoenix, AZ 85037, USA"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER),
                hasItem("(555) 555-5555 extension 1234"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_EMAIL),
                hasItem("emailaddress@something.com"));
    }

    @Test
    public void testNullMetacardAttributeValues() throws Exception {
        MetacardImpl metacard = convert("/csw-null-metacard-attributes.xml");
        assertRegistryMetacard(metacard);

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_NAME,
                null), is("Codice"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_ADDRESS),
                hasItem("1234 Some Street, AZ 85037, USA"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER),
                hasItem("555-5555 extension 1234"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER), hasItem("123-4567"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_EMAIL),
                hasItem("emailaddress@something.com"));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoRegistryObjetId() throws Exception {
        convert("/bad-id.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoServiceId() throws Exception {
        convert("/bad-id-wrapped-service.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoPersonId() throws Exception {
        convert("/bad-id-wrapped-person.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoOrgId() throws Exception {
        convert("/bad-id-wrapped-org.xml");
    }

    @Test(expected = CatalogTransformerException.class)
    public void testParserException() throws Exception {
        convert("/bad-xml.xml");
    }

    @Test
    public void testCustomSlotSaved() throws Exception {
        // Just test that an unknown slot gets saved to the metadata field and not discarded.
        assertThat(convert("/custom-slot-service.xml").getMetadata()
                .contains("unknowSlotName"), is(true));
    }

    @Test
    public void testServiceWithMinimumBinding() throws Exception {
        MetacardImpl metacard = convert("/valid-federation-min-service.xml");
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.SERVICE_BINDING_TYPES,
                null), is("csw"));
    }

    @Test
    public void testServiceWithMultipleBindings() throws Exception {
        MetacardImpl metacard = convert("/valid-federation-multiple-service.xml");
        List<String> serviceBindingTypes = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.SERVICE_BINDING_TYPES);
        assertThat(serviceBindingTypes.size(), is(2));
        assertThat(serviceBindingTypes, hasItem("csw"));
        assertThat(serviceBindingTypes, hasItem("soap"));
    }

    @Test
    public void testMinimumValidService() throws Exception {
        convert("/empty-service.xml");
    }

    @Test
    public void testMetacardToXml() throws Exception {
        String in = IOUtils.toString(getClass().getResourceAsStream("/csw-rim-node.xml"));
        Metacard metacard = registryTransformer.transform(IOUtils.toInputStream(in));
        String out = IOUtils.toString(registryTransformer.transform(metacard, null)
                .getInputStream());
        assertThat(in, is(out));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testMetacardToXmlBadTag() throws Exception {
        String in = IOUtils.toString(getClass().getResourceAsStream("/csw-rim-node.xml"));
        Metacard metacard = registryTransformer.transform(IOUtils.toInputStream(in));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS, "JustSomeMadeUpStuff"));
        String out = IOUtils.toString(registryTransformer.transform(metacard, null)
                .getInputStream());
        assertThat(in, is(out));
    }

    @Test
    public void testLastUpdated() throws Exception {
        MetacardImpl metacard = convert("/csw-last-updated.xml");
        String utc = metacard.getModifiedDate()
                .toInstant()
                .toString();
        assertThat(utc, is("2016-01-26T17:16:34.996Z"));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadGeometryConversion() throws Exception {
        convert("/bad-geo-conversion.xml");
    }

    @Test
    public void testPersonNoExtension() throws Exception {
        MetacardImpl metacard = convert("/csw-person-info.xml");

        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.POINT_OF_CONTACT, null),
                is("Vito Andolini, (999) 555-2368, godfather@mafia.com"));
    }

    @Test
    public void testServiceBindingWithMultipleTypes() throws Exception {
        MetacardImpl metacard = convert("/binding-service-multiple-types.xml");

        List<String> serviceBindingsList = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.SERVICE_BINDING_TYPES);
        assertThat(serviceBindingsList.size(), is(2));
        assertThat(serviceBindingsList, hasItem("csw"));
        assertThat(serviceBindingsList, hasItem("fakeBindingType"));
    }

    @Test
    public void testFullRegistryPackage() throws Exception {
        MetacardImpl metacard = convert("/csw-full-registry-package.xml");

        Date date = Date.from(ZonedDateTime.parse("2015-11-01T06:15:30-07:00")
                .toInstant());
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.LIVE_DATE,
                null), is(date.toString()));

        date = Date.from(ZonedDateTime.parse("2015-11-01T13:15:30Z")
                .toInstant());
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.DATA_START_DATE,
                null), is(date.toString()));

        date = Date.from(ZonedDateTime.parse("2015-12-01T23:01:40Z")
                .toInstant());
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.DATA_END_DATE,
                null), is(date.toString()));

        date = Date.from(ZonedDateTime.parse("2016-01-26T17:16:34.996Z")
                .toInstant());
        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.MODIFIED, null),
                is(date.toString()));

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.LINKS,
                null), is("https://some/link/to/my/repo"));

        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.GEOGRAPHY, null),
                is("POINT (112.267472 33.467944)"));
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.REGION,
                null), is("USA"));

        List<String> attributeValuesList = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.DATA_SOURCES);
        assertThat(attributeValuesList.size(), is(2));
        assertThat(attributeValuesList, hasItem("youtube"));
        assertThat(attributeValuesList, hasItem("myCamera"));

        attributeValuesList = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.DATA_TYPES);
        assertThat(attributeValuesList.size(), is(2));
        assertThat(attributeValuesList, hasItem("video"));
        assertThat(attributeValuesList, hasItem("sensor"));

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.SECURITY_LEVEL,
                null), is("role=guest"));
        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.TITLE, null),
                is("Node Name"));
        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.DESCRIPTION, null),
                is("A little something describing this node in less than 1024 characters"));
        assertThat(RegistryUtility.getStringAttribute(metacard,
                Metacard.CONTENT_TYPE_VERSION,
                null), is("2.9.x"));

        attributeValuesList = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.SERVICE_BINDING_TYPES);
        assertThat(attributeValuesList.size(), is(2));
        assertThat(attributeValuesList, hasItem("Csw_Federated_Source"));
        assertThat(attributeValuesList, hasItem("soap13"));

        attributeValuesList = RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.SERVICE_BINDINGS);
        assertThat(attributeValuesList.size(), is(2));
        assertThat(attributeValuesList, hasItem("REST"));
        assertThat(attributeValuesList, hasItem("SOAP"));

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_NAME,
                null), is("Codice"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_ADDRESS),
                hasItem("1234 Some Street, Phoenix, AZ 85037, USA"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER),
                hasItem("(555) 555-5555 extension 1234"));
        assertThat(RegistryUtility.getListOfStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_EMAIL),
                hasItem("emailaddress@something.com"));
        assertThat(RegistryUtility.getStringAttribute(metacard, Metacard.POINT_OF_CONTACT, null),
                is("john doe, (111) 111-1111 extension 1234, emailaddress@something.com"));
    }

    @Test
    public void testMultipleOrgsOneEmpty() throws Exception {
        MetacardImpl metacard = convert("/multiple-org-one-empty-wrapped.xml");
        assertRegistryMetacard(metacard);

        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_NAME,
                null), is(nullValue()));
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_ADDRESS,
                null), is(nullValue()));
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_PHONE_NUMBER,
                null), is(nullValue()));
        assertThat(RegistryUtility.getStringAttribute(metacard,
                RegistryObjectMetacardType.ORGANIZATION_EMAIL,
                null), is(nullValue()));
    }

    @Test
    public void testMultiplePersonsOneEmpty() throws Exception {
        MetacardImpl metacard = convert("/multiple-person-one-empty-wrapped.xml");
        assertRegistryMetacard(metacard);

        assertThat(metacard.getPointOfContact(),
                is("no name, no telephone number, no email address"));
    }

    private MetacardImpl convert(String path) throws Exception {
        return (MetacardImpl) registryTransformer.transform(getClass().getResourceAsStream(path));
    }
}
