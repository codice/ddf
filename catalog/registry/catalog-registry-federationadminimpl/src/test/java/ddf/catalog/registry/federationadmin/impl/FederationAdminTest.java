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
package ddf.catalog.registry.federationadmin.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.federationadmin.converter.RegistryPackageWebConverter;
import ddf.catalog.registry.federationadmin.service.FederationAdminException;
import ddf.catalog.registry.federationadmin.service.FederationAdminService;
import ddf.catalog.registry.transformer.RegistryTransformer;
import ddf.catalog.transform.CatalogTransformerException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

@RunWith(MockitoJUnitRunner.class)
public class FederationAdminTest {
    @Mock
    private FederationAdminService federationAdminService;

    private FederationAdmin federationAdmin;

    @Mock
    private Parser mockParser;

    @Mock
    private ParserConfigurator mockConfigurator;

    private Parser parser;

    private ParserConfigurator configurator;

    @Mock
    private RegistryTransformer registryTransformer;

    private static final String LOCAL_NODE_KEY = "localNodes";

    @Before
    public void setUp() {
        parser = new XmlParser();
        configurator = parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                        .getName(),
                net.opengis.ogc.ObjectFactory.class.getPackage()
                        .getName(),
                net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                        .getName()),
                this.getClass()
                        .getClassLoader());

        when(mockParser.configureParser(anyList(), any(ClassLoader.class))).thenReturn(
                mockConfigurator);
        federationAdmin = new FederationAdmin();
        federationAdmin.setFederationAdminService(federationAdminService);
        federationAdmin.setRegistryTransformer(registryTransformer);
        federationAdmin.setParser(mockParser);
    }

    @Test
    public void testCreateLocalEntry() throws Exception {
        String metacardId = "metacardId";
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);

        Metacard metacard = getTestMetacard();
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(federationAdminService.addRegistryEntry(any(Metacard.class))).thenReturn(metacardId);

        String createdMetacardId = federationAdmin.createLocalEntry(registryMap);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(federationAdminService).addRegistryEntry(metacard);
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryWithEmptyMap() throws Exception {
        Map<String, Object> registryMap = new HashMap<>();
        federationAdmin.createLocalEntry(registryMap);

        verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryWithBadMap() throws Exception {
        Map<String, Object> registryMap = new HashMap<>();
        registryMap.put("BadKey", "BadValue");

        federationAdmin.createLocalEntry(registryMap);
        verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryWithFederationAdminException() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);

        Metacard metacard = getTestMetacard();
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(federationAdminService.addRegistryEntry(any(Metacard.class))).thenThrow(
                FederationAdminException.class);

        federationAdmin.createLocalEntry(registryMap);

        verify(federationAdminService).addRegistryEntry(metacard);
    }

    @Test
    public void testCreateLocalEntryString() throws Exception {
        String encodeThisString = "aPretendXmlRegistryPackage";
        String metacardId = "createdMetacardId";
        String base64EncodedString = Base64.getEncoder()
                .encodeToString(encodeThisString.getBytes());

        Metacard metacard = getTestMetacard();

        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(federationAdminService.addRegistryEntry(metacard)).thenReturn(metacardId);

        String createdMetacardId = federationAdmin.createLocalEntry(base64EncodedString);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(registryTransformer).transform(any(InputStream.class));
        verify(federationAdminService).addRegistryEntry(metacard);
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryStringWithBlankString() throws Exception {
        String base64EncodedString = "";

        federationAdmin.createLocalEntry(base64EncodedString);

        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryStringWithTransformerException() throws Exception {
        String encodeThisString = "aPretendXmlRegistryPackage";
        String base64EncodedString = Base64.getEncoder()
                .encodeToString(encodeThisString.getBytes());

        when(registryTransformer.transform(any(InputStream.class))).thenThrow(
                CatalogTransformerException.class);

        federationAdmin.createLocalEntry(base64EncodedString);

        verify(registryTransformer).transform(any(InputStream.class));
        verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testCreateLocalEntryStringWithDecodeError() throws Exception {
        // This is has an illegal base64 character
        String base64EncodedString = "[B@6499375d";

        federationAdmin.createLocalEntry(base64EncodedString);

        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test
    public void testUpdateLocalEntry() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
        String existingMetacardId = "someUpdateMetacardId";

        Metacard existingMetacard = getTestMetacard();
        existingMetacard.setAttribute(new AttributeImpl(Metacard.ID, existingMetacardId));
        List<Metacard> existingMetacards = new ArrayList<>();
        existingMetacards.add(existingMetacard);
        Metacard updateMetacard = getTestMetacard();

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()))).thenReturn(existingMetacards);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(updateMetacard);

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()));
        verify(registryTransformer).transform(any(InputStream.class));
        verify(federationAdminService).updateRegistryEntry(updateMetacard);
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateLocalEntryWithEmptyMap() throws Exception {
        Map<String, Object> registryMap = new HashMap<>();

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService,
                never()).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(any(String.class)));
        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateLocalEntryWithBadMap() throws Exception {
        Map<String, Object> registryMap = new HashMap<>();
        registryMap.put("BadKey", "BadValue");

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService,
                never()).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(any(String.class)));
        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateLocalEntryWithEmptyExistingList() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
        List<Metacard> existingMetacards = new ArrayList<>();

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()))).thenReturn(existingMetacards);

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService,
                never()).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()));
        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateLocalEntryWithMultipleExistingMetacards() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
        List<Metacard> existingMetacards = new ArrayList<>();
        existingMetacards.add(getTestMetacard());
        existingMetacards.add(getTestMetacard());

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()))).thenReturn(existingMetacards);

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService,
                never()).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()));
        verify(registryTransformer, never()).transform(any(InputStream.class));
        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateLocalEntryWithFederationAdminServiceException() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-full-registry-package.xml");
        Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
        String existingMetacardId = "someUpdateMetacardId";

        Metacard existingMetacard = getTestMetacard();
        existingMetacard.setAttribute(new AttributeImpl(Metacard.ID, existingMetacardId));
        List<Metacard> existingMetacards = new ArrayList<>();
        existingMetacards.add(existingMetacard);
        Metacard updateMetacard = getTestMetacard();

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()))).thenReturn(existingMetacards);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(updateMetacard);
        doThrow(FederationAdminException.class).when(federationAdminService)
                .updateRegistryEntry(updateMetacard);

        federationAdmin.updateLocalEntry(registryMap);

        verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(
                registryObject.getId()));
        verify(registryTransformer).transform(any(InputStream.class));
        verify(federationAdminService).updateRegistryEntry(updateMetacard);
    }

    @Test
    public void testDeleteLocalEntry() throws Exception {
        String firstRegistryId = "firstRegistryId";
        String secondRegistryId = "secondRegistryId";
        List<String> ids = new ArrayList<>();
        ids.add(firstRegistryId);
        ids.add(secondRegistryId);

        String firstMetacardId = "firstMetacardId";
        String secondMetacardId = "secondMetacardId";

        Metacard firstMetacard = getTestMetacard();
        firstMetacard.setAttribute(new AttributeImpl(Metacard.ID, firstMetacardId));
        Metacard secondMetacard = getTestMetacard();
        secondMetacard.setAttribute(new AttributeImpl(Metacard.ID, secondMetacardId));

        List<Metacard> matchingMetacards = new ArrayList<>();
        matchingMetacards.add(firstMetacard);
        matchingMetacards.add(secondMetacard);

        List<String> metacardIds = new ArrayList<>();
        metacardIds.addAll(matchingMetacards.stream()
                .map(Metacard::getId)
                .collect(Collectors.toList()));

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids)).thenReturn(
                matchingMetacards);

        federationAdmin.deleteLocalEntry(ids);

        verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(ids);
        verify(federationAdminService).deleteRegistryEntriesByMetacardIds(metacardIds);
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteLocalEntryWithEmptyList() throws Exception {
        List<String> ids = new ArrayList<>();

        federationAdmin.deleteLocalEntry(ids);

        verify(federationAdminService, never()).getLocalRegistryMetacardsByRegistryIds(anyList());
        verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(anyList());
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteLocalEntryWithExceptionGettingLocalMetacards() throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("whatever");

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids)).thenThrow(
                FederationAdminException.class);

        federationAdmin.deleteLocalEntry(ids);

        verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(ids);
        verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(anyList());
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteLocalEntryWithNonMatchingLists() throws Exception {
        String firstRegistryId = "firstRegistryId";
        String secondRegistryId = "secondRegistryId";
        List<String> ids = new ArrayList<>();
        ids.add(firstRegistryId);
        ids.add(secondRegistryId);

        String firstMetacardId = "firstMetacardId";

        Metacard firstMetacard = getTestMetacard();
        firstMetacard.setAttribute(new AttributeImpl(Metacard.ID, firstMetacardId));

        List<Metacard> matchingMetacards = new ArrayList<>();
        matchingMetacards.add(firstMetacard);

        List<String> metacardIds = new ArrayList<>();
        metacardIds.addAll(matchingMetacards.stream()
                .map(Metacard::getId)
                .collect(Collectors.toList()));

        when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids)).thenReturn(
                matchingMetacards);

        federationAdmin.deleteLocalEntry(ids);

        verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(ids);
        verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(metacardIds);
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteLocalEntryWithExceptionDeletingEntries() throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("firstId");

        doThrow(FederationAdminException.class).when(federationAdminService)
                .deleteRegistryEntriesByRegistryIds(ids);
        federationAdmin.deleteLocalEntry(ids);

        verify(federationAdminService).deleteRegistryEntriesByRegistryIds(ids);
    }

    @Test
    public void testGetLocalNodes() throws Exception {
        RegistryObjectType registryObject = getRegistryObjectFromResource(
                "/csw-registry-package-smaller.xml");
        Map<String, Object> registryObjectMap = RegistryPackageWebConverter.getRegistryObjectWebMap(
                registryObject);
        List<RegistryPackageType> registryPackages = new ArrayList<>();
        registryPackages.add((RegistryPackageType) registryObject);

        when(federationAdminService.getLocalRegistryObjects()).thenReturn(registryPackages);
        Map<String, Object> localNodes = federationAdmin.getLocalNodes();

        Map<String, Object> localNode =
                ((List<Map<String, Object>>) localNodes.get(LOCAL_NODE_KEY)).get(0);
        verify(federationAdminService).getLocalRegistryObjects();
        assertThat(localNode, is(equalTo(registryObjectMap)));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalNodesWithFederationAdminException() throws Exception {
        when(federationAdminService.getLocalRegistryObjects()).thenThrow(FederationAdminException.class);

        federationAdmin.getLocalNodes();

        verify(federationAdminService).getLocalRegistryObjects();
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

    private Map<String, Object> getMapFromRegistryObject(RegistryObjectType registryObject) {
        return RegistryPackageWebConverter.getRegistryObjectWebMap(registryObject);
    }

    private RegistryPackageType getRegistryObjectFromMap(Map<String, Object> registryMap) {
        return RegistryPackageWebConverter.getRegistryPackageFromWebMap(registryMap);
    }

    private Metacard getTestMetacard() {
        return new MetacardImpl(new RegistryObjectMetacardType());
    }
}