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
package ddf.catalog.registry.federationadmin.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.federationadmin.service.FederationAdminException;
import ddf.catalog.registry.transformer.RegistryTransformer;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

@RunWith(MockitoJUnitRunner.class)
public class FederationAdminServiceImplTest {

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private static final String TEST_SITE_NAME = "Slate Rock and Gravel Company";

    private static final String TEST_VERSION = "FF 2.0";

    private FederationAdminServiceImpl fasi;

    @Mock
    private ParserConfigurator configurator;

    @Mock
    private CatalogFramework catalogFramework;

    @Mock
    private Parser parser;

    @Mock
    private RegistryTransformer registryTransformer;

    @Mock
    private Security security;

    @Mock
    FilterBuilder filterBuilder;

    @Mock
    AttributeBuilder attributeBuilder;

    @Mock
    ExpressionBuilder expressionBuilder;

    @Mock
    Subject subject;

    @Before
    public void setUp() throws Exception {
        when(parser.configureParser(anyList(), any(ClassLoader.class))).thenReturn(configurator);
        when(filterBuilder.attribute(any(String.class))).thenReturn(attributeBuilder);
        when(attributeBuilder.is()).thenReturn(expressionBuilder);
        when(expressionBuilder.bool(any(Boolean.class))).thenReturn(getTestFilter());

        fasi = spy(new FederationAdminServiceImpl(security));
        fasi.setRegistryTransformer(registryTransformer);
        fasi.setCatalogFramework(catalogFramework);
        fasi.setParser(parser);
        fasi.setFilterBuilder(filterBuilder);
        System.setProperty(SystemInfo.SITE_NAME, TEST_SITE_NAME);
        System.setProperty(SystemInfo.VERSION, TEST_VERSION);

    }

    @Test
    public void initWithNoPreviousEntry() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request);
        Metacard metacard = getTestMetacard();

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);

        fasi.init();

        verify(fasi).addRegistryEntry(metacard);
    }

    @Test
    public void initWithPreviousNonPrimaryEntry() throws Exception {
        Metacard addThisMetacard = getTestMetacard();
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request);

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(addThisMetacard);

        fasi.init();

        verify(fasi).addRegistryEntry(addThisMetacard);
    }

    @Test
    public void initWithPreviousWithEmptyMetacard() throws Exception {
        Metacard addThisMetacard = getTestMetacard();
        QueryRequest request = getTestQueryRequest();
        Result result = new ResultImpl();
        QueryResponse response = getTestQueryResponse(request, Collections.singletonList(result));

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(addThisMetacard);

        fasi.init();

        verify(fasi).addRegistryEntry(addThisMetacard);
    }

    @Test
    public void initWithPreviousPrimaryEntry() throws Exception {
        Metacard addThisMetacard = getTestMetacard();
        Metacard findThisMetacard = getTestMetacard();
        Attribute regNodeStatus =
                new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
        findThisMetacard.setAttribute(regNodeStatus);
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request, findThisMetacard);

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(addThisMetacard);

        fasi.init();

        verify(fasi, never()).addRegistryEntry(addThisMetacard);
    }

    @Test
    public void initWithDuplicatePreviousPrimaryEntry() throws Exception {
        Metacard addThisMetacard = getTestMetacard();
        Metacard findThisMetacard = getTestMetacard();
        Attribute regNodeStatus =
                new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
        findThisMetacard.setAttribute(regNodeStatus);
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                findThisMetacard,
                findThisMetacard);

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(addThisMetacard);

        fasi.init();

        verify(fasi, never()).addRegistryEntry(addThisMetacard);
    }

    @Test
    public void initWithQueryException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(SourceUnavailableException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.init();
        verify(fasi, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test
    public void initWithIngestException() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(security.getSystemSubject()).thenReturn(subject);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(getTestMetacard());
        doThrow(IngestException.class).when(catalogFramework)
                .create(any(CreateRequest.class));

        fasi.init();
        verify(fasi).addRegistryEntry(any(Metacard.class));
    }

    @Test
    public void initWithMarshalException() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request);
        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        doThrow(ParserException.class).when(parser)
                .marshal(any(ParserConfigurator.class), any(Object.class), any(OutputStream.class));

        fasi.init();
        verify(fasi, never()).addRegistryEntry(any(Metacard.class));
    }

    @Test
    public void testAddRegistryEntry() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard createdMetacard = metacard;
        createdMetacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        Subject systemSubject = security.getSystemSubject();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
        CreateRequest request = new CreateRequestImpl(Collections.singletonList(metacard),
                properties,
                destinations);
        CreateResponse response = new CreateResponseImpl(request,
                null,
                Collections.singletonList(createdMetacard));

        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

        String createdMetacardId = fasi.addRegistryEntry(metacard, destinations);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(catalogFramework).create(any(CreateRequest.class));
    }

    @Test
    public void testAddRegistryEntryWithNullDestinations() throws Exception {
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard createdMetacard = metacard;
        createdMetacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));

        Set<String> destinations = null;

        Subject systemSubject = security.getSystemSubject();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
        CreateRequest request = new CreateRequestImpl(Collections.singletonList(metacard),
                properties,
                destinations);
        CreateResponse response = new CreateResponseImpl(request,
                null,
                Collections.singletonList(createdMetacard));

        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

        String createdMetacardId = fasi.addRegistryEntry(metacard, destinations);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(catalogFramework).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryWithNullMetacard() throws Exception {
        String destination = "someDestination";
        Metacard metacard = null;

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.addRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryWithInvalidMetacardNoRegistryId() throws Exception {
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.addRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryWithInvalidMetacardNoRegistryTag() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.addRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryWithIngestException() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.create(any(CreateRequest.class))).thenThrow(IngestException.class);

        fasi.addRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryWithSourceUnavailableException() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.create(any(CreateRequest.class))).thenThrow(SourceUnavailableException.class);

        fasi.addRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test
    public void testAddRegistryEntryMetacard() throws Exception {
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard createdMetacard = metacard;
        createdMetacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));

        Subject systemSubject = security.getSystemSubject();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
        CreateRequest request = new CreateRequestImpl(Collections.singletonList(metacard),
                properties,
                null);
        CreateResponse response = new CreateResponseImpl(request,
                null,
                Collections.singletonList(createdMetacard));

        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

        String createdMetacardId = fasi.addRegistryEntry(metacard);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(catalogFramework).create(any(CreateRequest.class));
    }

    @Test
    public void testAddRegistryEntryStringWithDestinations() throws Exception {
        String xml = "someValidStringVersionOfXml";
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard createdMetacard = metacard;
        createdMetacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        Subject systemSubject = security.getSystemSubject();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
        CreateRequest request = new CreateRequestImpl(Collections.singletonList(metacard),
                properties,
                destinations);
        CreateResponse response = new CreateResponseImpl(request,
                null,
                Collections.singletonList(createdMetacard));

        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

        String createdMetacardId = fasi.addRegistryEntry(xml, destinations);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework).create(any(CreateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testAddRegistryEntryStringWithTransformerException() throws Exception {
        String xml = "someValidStringVersionOfXml";
        String registryId = "registryId";
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(registryTransformer.transform(any(InputStream.class))).thenThrow(
                CatalogTransformerException.class);

        fasi.addRegistryEntry(xml, destinations);

        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework, never()).create(any(CreateRequest.class));
    }

    @Test
    public void testAddRegistryEntryString() throws Exception {
        String xml = "someValidStringVersionOfXml";
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));

        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard createdMetacard = metacard;
        createdMetacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));

        Subject systemSubject = security.getSystemSubject();
        Map<String, Serializable> properties = new HashMap<>();
        properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
        CreateRequest request = new CreateRequestImpl(Collections.singletonList(metacard),
                properties,
                null);
        CreateResponse response = new CreateResponseImpl(request,
                null,
                Collections.singletonList(createdMetacard));

        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

        String createdMetacardId = fasi.addRegistryEntry(xml);

        assertThat(createdMetacardId, is(equalTo(metacardId)));
        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework).create(any(CreateRequest.class));
    }

    @Test
    public void testUpdateRegistryEntry() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test
    public void testUpdateRegistryEntryWithNullDestinations() throws Exception {
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = null;

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryWithNoMetacardId() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework, never()).query(any(QueryRequest.class));
        verify(catalogFramework, never()).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryWithNoExistingMetacard() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest());

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework, never()).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryWithMultipleMetacardMatches() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard,
                getTestMetacard());

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework, never()).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryWithIngestException() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(catalogFramework.update(any(UpdateRequest.class))).thenThrow(IngestException.class);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryWithSourceUnavailableException() throws Exception {
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(catalogFramework.update(any(UpdateRequest.class))).thenThrow(SourceUnavailableException.class);

        fasi.updateRegistryEntry(metacard, destinations);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test
    public void testUpdateRegistryEntryMetacard() throws Exception {
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(metacard);

        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test
    public void testUpdateRegistryEntryStringWithDestinations() throws Exception {
        String xml = "someValidStringXml";
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(xml, destinations);

        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testUpdateRegistryEntryStringWithTransformerException() throws Exception {
        String xml = "someValidStringXml";
        String registryId = "registryId";
        String destination = "someDestination";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(registryTransformer.transform(any(InputStream.class))).thenThrow(
                CatalogTransformerException.class);

        fasi.updateRegistryEntry(xml, destinations);

        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework, never()).query(any(QueryRequest.class));
        verify(catalogFramework, never()).update(any(UpdateRequest.class));
    }

    @Test
    public void testUpdateRegistryEntryString() throws Exception {
        String xml = "someValidStringXml";
        String registryId = "registryId";
        String metacardId = "someMetacardId";
        Metacard metacard = getTestMetacard();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                registryId));
        metacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));

        Metacard existingMetacard = getTestMetacard();

        QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(),
                existingMetacard);

        when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        fasi.updateRegistryEntry(xml);

        verify(registryTransformer).transform(any(InputStream.class));
        verify(catalogFramework).query(any(QueryRequest.class));
        verify(catalogFramework).update(any(UpdateRequest.class));
    }

    @Test
    public void testDeleteRegistryEntriesByRegistryIds() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.deleteRegistryEntriesByRegistryIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByRegistryIdsWithEmptyList() throws Exception {
        List<String> ids = new ArrayList<>();

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.deleteRegistryEntriesByRegistryIds(ids, destinations);

        verify(catalogFramework, never()).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByRegistryIdsWithSourceUnavailableException()
            throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(SourceUnavailableException.class);
        fasi.deleteRegistryEntriesByRegistryIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByRegistryIdsWithIngestException() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
        fasi.deleteRegistryEntriesByRegistryIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test
    public void testDeleteRegistryEntriesByRegistryIdsNoDestinations() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        fasi.deleteRegistryEntriesByRegistryIds(ids);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test
    public void testDeleteRegistryEntriesByMetacardIds() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.deleteRegistryEntriesByMetacardIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByMetacardIdsWithEmptyList() throws Exception {
        List<String> ids = new ArrayList<>();

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        fasi.deleteRegistryEntriesByMetacardIds(ids, destinations);

        verify(catalogFramework, never()).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByMetacardIdsWithSourceUnavailableException()
            throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(SourceUnavailableException.class);
        fasi.deleteRegistryEntriesByMetacardIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testDeleteRegistryEntriesByMetacardIdsWithIngestException() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        String destination = "destination";
        Set<String> destinations = new HashSet<>();
        destinations.add(destination);

        when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
        fasi.deleteRegistryEntriesByMetacardIds(ids, destinations);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test
    public void testDeleteRegistryEntriesByMetacardIdsNoDestinations() throws Exception {
        String firstId = "firstRegistyId";
        List<String> ids = new ArrayList<>();
        ids.add(firstId);

        fasi.deleteRegistryEntriesByRegistryIds(ids);

        verify(catalogFramework).delete(any(DeleteRequest.class));
    }

    @Test
    public void testGetRegistryMetacards() throws Exception {
        Metacard addThisMetacard = getTestMetacard();
        Metacard findThisMetacard = getTestMetacard();
        Attribute regNodeStatus =
                new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
        findThisMetacard.setAttribute(regNodeStatus);
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                findThisMetacard,
                getTestMetacard());

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(registryTransformer.transform(any(InputStream.class))).thenReturn(addThisMetacard);

        List<Metacard> metacards = fasi.getRegistryMetacards();

        verify(fasi).getRegistryMetacards();
        assertThat(metacards, hasSize(2));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsWithUnsupportedQueryException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(UnsupportedQueryException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsWithSourceUnavailableException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(SourceUnavailableException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsWithFederationException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(FederationException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test
    public void testGetLocalRegistryMetacards() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                getTestMetacard(),
                getTestMetacard());

        when(security.getSystemSubject()).thenReturn(subject);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        List<Metacard> metacards = fasi.getLocalRegistryMetacards();

        verify(fasi).getLocalRegistryMetacards();
        verify(catalogFramework).query(any(QueryRequest.class));
        assertThat(metacards, hasSize(2));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsWithUnsupportedQueryException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(UnsupportedQueryException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getLocalRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsWithSourceUnavailableException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(SourceUnavailableException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getLocalRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsWithFederationException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(FederationException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getLocalRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    @Test
    public void testGetRegistryMetacardsByRegistryIds() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                getTestMetacard(),
                getTestMetacard(),
                getTestMetacard());
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        List<Metacard> metacards = fasi.getRegistryMetacardsByRegistryIds(ids);

        assertThat(metacards, hasSize(3));

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsByRegistryIdsWithEmptyList() throws Exception {
        List<String> ids = new ArrayList<>();

        fasi.getRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework, never()).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsByRegistryIdsWithSourceUnavailableException()
            throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(SourceUnavailableException.class);

        fasi.getRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsByRegistryIdsWithUnsupportedQueryException()
            throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(UnsupportedQueryException.class);

        fasi.getRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetRegistryMetacardsByRegistryIdsWithFederationException() throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(FederationException.class);

        fasi.getRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test
    public void testGetLocalRegistryMetacardsByRegistryIds() throws Exception {
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                getTestMetacard(),
                getTestMetacard(),
                getTestMetacard());
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);

        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        List<Metacard> metacards = fasi.getLocalRegistryMetacardsByRegistryIds(ids);

        assertThat(metacards, hasSize(3));

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsByRegistryIdsWithEmptyList() throws Exception {
        List<String> ids = new ArrayList<>();

        fasi.getLocalRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework, never()).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsByRegistryIdsWithSourceUnavailableException()
            throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(SourceUnavailableException.class);

        fasi.getLocalRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsByRegistryIdsWithUnsupportedQueryException()
            throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(UnsupportedQueryException.class);

        fasi.getLocalRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryMetacardsByRegistryIdsWithFederationException()
            throws Exception {
        List<String> ids = new ArrayList<>();
        ids.add("someMadeUpId");
        ids.add("anotherOne");
        ids.add("oneMore");

        when(catalogFramework.query(any(QueryRequest.class))).thenThrow(FederationException.class);

        fasi.getLocalRegistryMetacardsByRegistryIds(ids);

        verify(catalogFramework).query(any(QueryRequest.class));
    }

    @Test
    public void testGetLocalRegistryObjects() throws Exception {
        Metacard localMetacardOne = getTestMetacard();
        localMetacardOne.setAttribute(new AttributeImpl(Metacard.METADATA, "xmlString"));

        Metacard localMetacardTwo = getTestMetacard();
        localMetacardTwo.setAttribute(new AttributeImpl(Metacard.METADATA, "anotherXmlString"));

        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(localMetacardOne);
        localMetacards.add(localMetacardTwo);

        doReturn(localMetacards).when(fasi)
                .getLocalRegistryMetacards();

        JAXBElement<RegistryPackageType> jaxbRegistryPackage = RIM_FACTORY.createRegistryPackage(
                getTestRegistryPackage());
        when(parser.unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class))).thenReturn(jaxbRegistryPackage);

        List<RegistryPackageType> packages = fasi.getLocalRegistryObjects();

        assertThat(packages, hasSize(2));

        verify(fasi).getLocalRegistryMetacards();
        verify(parser, times(2)).unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryObjectsWithEmptyMetadata() throws Exception {
        Metacard localMetacardOne = getTestMetacard();
        localMetacardOne.setAttribute(new AttributeImpl(Metacard.METADATA, ""));

        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(localMetacardOne);

        doReturn(localMetacards).when(fasi)
                .getLocalRegistryMetacards();

        fasi.getLocalRegistryObjects();

        verify(fasi).getLocalRegistryMetacards();
        verify(parser, never()).unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryObjectsWithQueryException() throws Exception {
        doThrow(FederationAdminException.class).when(fasi)
                .getLocalRegistryMetacards();

        fasi.getLocalRegistryObjects();

        verify(fasi).getLocalRegistryMetacards();
        verify(parser, never()).unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class));
    }

    @Test(expected = FederationAdminException.class)
    public void testGetLocalRegistryObjectsWithParserException() throws Exception {
        Metacard localMetacardOne = getTestMetacard();
        localMetacardOne.setAttribute(new AttributeImpl(Metacard.METADATA, "someXmlMetadata"));

        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(localMetacardOne);

        doReturn(localMetacards).when(fasi)
                .getLocalRegistryMetacards();
        when(parser.unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class))).thenThrow(ParserException.class);

        fasi.getLocalRegistryObjects();

        verify(fasi).getLocalRegistryMetacards();
        verify(parser).unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class));
    }

    @Test
    public void testGetLocalRegistryObjectsWithNoRegistryEntries() throws Exception {
        List<Metacard> localMetacards = new ArrayList<>();

        doReturn(localMetacards).when(fasi)
                .getLocalRegistryMetacards();

        List<RegistryPackageType> packages = fasi.getLocalRegistryObjects();

        assertThat(packages, empty());

        verify(fasi).getLocalRegistryMetacards();
        verify(parser, never()).unmarshal(any(ParserConfigurator.class),
                eq(JAXBElement.class),
                any(InputStream.class));
    }

    private Metacard getTestMetacard() {
        return new MetacardImpl(new RegistryObjectMetacardType());
    }

    private Filter getTestFilter() {
        return FILTER_FACTORY.and(FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.CONTENT_TYPE),
                RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME),
                FILTER_FACTORY.like(FILTER_FACTORY.property(Metacard.TAGS),
                        RegistryConstants.REGISTRY_TAG));

    }

    private QueryRequest getTestQueryRequest() {
        Filter filter = getTestFilter();
        SortBy sortBy = FILTER_FACTORY.sort(Metacard.CREATED, SortOrder.ASCENDING);

        Query query = new QueryImpl(filter);
        ((QueryImpl) query).setSortBy(sortBy);
        QueryRequest request = new QueryRequestImpl(query);
        request.getProperties()
                .put(SecurityConstants.SECURITY_SUBJECT, subject);

        return request;
    }

    private QueryResponse getPopulatedTestQueryResponse(QueryRequest request,
            Metacard... metacards) {
        List<Result> results = new ArrayList<>();
        for (Metacard metacard : metacards) {
            results.add(new ResultImpl(metacard));
        }

        return getTestQueryResponse(request, results);
    }

    private QueryResponse getTestQueryResponse(QueryRequest request, List<Result> results) {
        return new QueryResponseImpl(request, results, results.size());
    }

    private RegistryPackageType getTestRegistryPackage() {
        return RIM_FACTORY.createRegistryPackageType();
    }

}
