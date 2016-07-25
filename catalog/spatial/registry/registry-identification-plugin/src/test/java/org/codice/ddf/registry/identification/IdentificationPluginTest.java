/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.identification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Charsets;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class IdentificationPluginTest {

    private IdentificationPlugin identificationPlugin;

    private Parser parser;

    private ParserConfigurator configurator;

    private MetacardImpl sampleData;

    @Before
    public void setUp() {
        parser = new XmlParser();
        identificationPlugin = new IdentificationPlugin();
        identificationPlugin.setMetacardMarshaller(new MetacardMarshaller(parser));
        setParser(parser);
        sampleData = new MetacardImpl();
        sampleData.setId("testNewMetacardId");
        sampleData.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "testNewRegistryId");
        sampleData.setAttribute(Metacard.MODIFIED, new Date().from(Instant.now()));
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
        CreateRequest result = identificationPlugin.process(new CreateRequestImpl(sampleData));
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
        CreateRequest result = identificationPlugin.process(new CreateRequestImpl(sampleData));
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
        CreateRequest result = identificationPlugin.process(new CreateRequestImpl(sampleData));
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
        CreateRequest result =
                identificationPlugin.process(new CreateRequestImpl(Collections.singletonList(
                        sampleData), props));
        assertThat(result.getMetacards()
                .get(0)
                .getMetadata(), equalTo(xml));
    }

    @Test(expected = StopProcessingException.class)
    public void testDuplicateChecking() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        identificationPlugin.process(new CreateResponseImpl(new CreateRequestImpl(sampleData),
                null,
                Collections.singletonList(sampleData)));
        identificationPlugin.process(new CreateRequestImpl(sampleData));
    }

    @Test
    public void testDuplicateCheckingAfterDelete() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);
        identificationPlugin.process(new CreateResponseImpl(new CreateRequestImpl(sampleData),
                null,
                Collections.singletonList(sampleData)));
        identificationPlugin.process(new DeleteResponseImpl(new DeleteRequestImpl(Collections.singletonList(
                "abc123"), RegistryObjectMetacardType.REGISTRY_ID, null),
                null,
                Collections.singletonList(sampleData)));
        identificationPlugin.process(new CreateRequestImpl(sampleData));
    }

    private String convert(String path) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(path);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        return buffer.lines()
                .collect(Collectors.joining("\n"));
    }

    @Test(expected = PluginExecutionException.class)
    public void testUpdateWithNullOperationTransaction() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);

        List<Map.Entry<Serializable, Metacard>> updatedEntries = new ArrayList<>();
        updatedEntries.add(new AbstractMap.SimpleEntry<>(sampleData.getId(), sampleData));

        UpdateRequest updateRequest = new UpdateRequestImpl(updatedEntries,
                Metacard.ID,
                new HashMap<>());
        identificationPlugin.process(updateRequest);
    }

    @Test
    public void testUpdateMetacardWithModifiedTimeSameAsCurrentMetacard() throws Exception {
        String xml = convert("/registry-both-extid.xml");
        sampleData.setAttribute(Metacard.METADATA, xml);

        OperationTransaction operationTransaction = new OperationTransactionImpl(null,
                Collections.singletonList(sampleData));
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(Constants.OPERATION_TRANSACTION_KEY, operationTransaction);
        List<Map.Entry<Serializable, Metacard>> updatedEntries = new ArrayList<>();

        Metacard updateMetacard = sampleData;
        updatedEntries.add(new AbstractMap.SimpleEntry<>(updateMetacard.getId(), updateMetacard));

        UpdateRequest updateRequest = new UpdateRequestImpl(updatedEntries,
                Metacard.ID,
                properties);
        UpdateRequest processedUpdateRequest = identificationPlugin.process(updateRequest);
        assertThat(processedUpdateRequest.getUpdates()
                .size(), is(1));
    }

    @Test
    public void testSetTransientAttributesOnUpdateMetacard() throws Exception {
        String xml = convert("/registry-no-extid.xml");
        MetacardImpl previousMetacard = new MetacardImpl();
        previousMetacard.setAttribute(Metacard.ID, "MetacardId");
        previousMetacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "MetacardId");
        previousMetacard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        previousMetacard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                "Published Locations");
        previousMetacard.setAttribute(RegistryObjectMetacardType.LAST_PUBLISHED,
                "Last Published Time");
        previousMetacard.setAttribute(Metacard.MODIFIED, new Date().from(Instant.now()));

        OperationTransaction operationTransaction = new OperationTransactionImpl(null,
                Collections.singletonList(previousMetacard));

        Map<String, Serializable> properties = new HashMap<>();
        properties.put(Constants.OPERATION_TRANSACTION_KEY, operationTransaction);

        List<Map.Entry<Serializable, Metacard>> updatedEntries = new ArrayList<>();

        MetacardImpl updateMetacard = new MetacardImpl();
        updateMetacard.setAttribute(Metacard.ID, "MetacardId");
        updateMetacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "MetacardId");
        updateMetacard.setAttribute(new AttributeImpl(Metacard.TAGS, RegistryConstants.REGISTRY_TAG));
        updateMetacard.setAttribute(Metacard.MODIFIED, new Date().from(Instant.now()));
        updateMetacard.setAttribute(Metacard.METADATA, xml);
        updatedEntries.add(new AbstractMap.SimpleEntry<>(updateMetacard.getId(), updateMetacard));

        UpdateRequest updateRequest = new UpdateRequestImpl(updatedEntries,
                Metacard.ID,
                properties);
        UpdateRequest processedUpdateRequest = identificationPlugin.process(updateRequest);
        Metacard processedMetacard = processedUpdateRequest.getUpdates()
                .get(0)
                .getValue();
        assertThat(processedMetacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS)
                .getValue(), is("Published Locations"));
        assertThat(processedMetacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED)
                .getValue(), is("Last Published Time"));
    }

    public void setParser(Parser parser) {

        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        EbrimConstants.OGC_FACTORY.getClass()
                                .getPackage()
                                .getName(),
                        EbrimConstants.GML_FACTORY.getClass()
                                .getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());

        this.parser = parser;
    }

}
