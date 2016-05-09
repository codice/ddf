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
package org.codice.ddf.registry.identification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.plugin.StopProcessingException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class IdentificationPluginTest {

    private IdentificationPlugin idp;

    private Parser parser;

    private ParserConfigurator configurator;

    private MetacardImpl sampleData;

    @Before
    public void setUp() {
        parser = new XmlParser();
        idp = new IdentificationPlugin();
        idp.setParser(parser);
        setParser(parser);
        sampleData = new MetacardImpl();
        sampleData.setId("testNewMetacardId");
        sampleData.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "testNewRegistryId");
        Set<String> tags = new HashSet<>();
        tags.add("registry");
        sampleData.setTags(tags);
    }

    //test ext IDs are not set (origin & local)
    @Test
    public void testBothExtIdMissing() throws Exception {
        //unmarshal metacard.metadata and confirm both origin and local ext id are set to metacard.getId()

        String xml = convert("/registry-no-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        CreateRequest result = idp.process(new CreateRequestImpl(sampleData));
        Metacard testMetacard = result.getMetacards()
                .get(0);

        String metadata = testMetacard.getMetadata();
        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                configurator,
                JAXBElement.class,
                inputStream);

        RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();
        List<ExternalIdentifierType> extIdList = registryObjectType.getExternalIdentifier();

        for (ExternalIdentifierType singleExtId : extIdList) {
            if (singleExtId.getId()
                    .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                assertThat(singleExtId.getValue(), is("testNewMetacardId"));
            } else {
                assertThat(singleExtId.getId(), is(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID));
                assertThat(singleExtId.getValue(), is("testNewMetacardId"));
            }
        }
    }

    //test ext IDs are not set to other items
    @Test
    public void testOtherExtIds() throws Exception {
        //unmarshal metacard.metadata and confirm both origin and local ext id are set to metacard.getId()

        String xml = convert("/registry-extra-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        CreateRequest result = idp.process(new CreateRequestImpl(sampleData));
        Metacard testMetacard = result.getMetacards()
                .get(0);

        String metadata = testMetacard.getMetadata();
        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                configurator,
                JAXBElement.class,
                inputStream);

        RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();
        List<ExternalIdentifierType> extIdList = registryObjectType.getExternalIdentifier();

        for (ExternalIdentifierType singleExtId : extIdList) {
            if (singleExtId.getId()
                    .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                assertThat(singleExtId.getValue(), is("testNewMetacardId"));
            } else if (singleExtId.getId()
                    .equals(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID)) {
                assertThat(singleExtId.getValue(), is("testNewMetacardId"));
            }
        }
    }

    //test both ids are already set
    @Test
    public void testIdsAlreadySet() throws Exception {
        //unmarshal metacard.metadata and confirm only local ext id are set to metacard.getId()

        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        CreateRequest result = idp.process(new CreateRequestImpl(sampleData));
        Metacard testMetacard = result.getMetacards()
                .get(0);

        String metadata = testMetacard.getMetadata();
        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                configurator,
                JAXBElement.class,
                inputStream);

        RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();
        List<ExternalIdentifierType> extIdList = registryObjectType.getExternalIdentifier();

        for (ExternalIdentifierType singleExtId : extIdList) {
            if (singleExtId.getId()
                    .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                assertThat(singleExtId.getValue(), is("testNewMetacardId"));
            } else {
                assertThat(singleExtId.getId(), is(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID));
                assertThat(singleExtId.getValue(), is("registryPresetOriginValue"));
            }
        }

    }

    @Test
    public void testRemoteRequest() throws Exception {
        String xml = convert("/registry-no-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        Map<String, Serializable> props = new HashMap<>();
        props.put(Constants.LOCAL_DESTINATION_KEY, false);
        CreateRequest result = idp.process(new CreateRequestImpl(Collections.singletonList(
                sampleData), props));
        assertThat(result.getMetacards()
                .get(0)
                .getMetadata(), equalTo(xml));
    }

    @Test(expected = StopProcessingException.class)
    public void testDuplicateChecking() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        idp.process(new CreateResponseImpl(new CreateRequestImpl(sampleData),
                null,
                Collections.singletonList(sampleData)));
        idp.process(new CreateRequestImpl(sampleData));
    }

    @Test
    public void testDuplicateCheckingAfterDelete() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        idp.process(new CreateResponseImpl(new CreateRequestImpl(sampleData),
                null,
                Collections.singletonList(sampleData)));
        idp.process(new DeleteResponseImpl(new DeleteRequestImpl(Collections.singletonList("abc123"),
                RegistryObjectMetacardType.REGISTRY_ID,
                null), null, Collections.singletonList(sampleData)));
        idp.process(new CreateRequestImpl(sampleData));
    }

    @Test
    public void testUnusedMethods() throws Exception {
        UpdateRequest updateRequest = new UpdateRequestImpl("abc123", sampleData);
        assertThat(updateRequest, equalTo(idp.process(updateRequest)));
        DeleteRequest deleteRequest = new DeleteRequestImpl("abc123");
        assertThat(deleteRequest, equalTo(idp.process(deleteRequest)));
        UpdateResponse updateResponse = new UpdateResponseImpl(updateRequest, null, null);
        assertThat(updateResponse, equalTo(idp.process(updateResponse)));
    }

    private String convert(String path) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(path);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        return buffer.lines()
                .collect(Collectors.joining("\n"));
    }

    public void setParser(Parser parser) {

        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());

        this.parser = parser;
    }

}
