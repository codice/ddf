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
package org.codice.ddf.registry.federationadmin.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.transformer.RegistryTransformer;
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
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

@RunWith(MockitoJUnitRunner.class)
public class FederationAdminServiceImplTest {

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

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

        verify(fasi).addLocalEntry(metacard);
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

        verify(fasi).addLocalEntry(addThisMetacard);
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

        verify(fasi).addLocalEntry(addThisMetacard);
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

        verify(fasi, never()).addLocalEntry(addThisMetacard);
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

        verify(fasi, never()).addLocalEntry(addThisMetacard);
    }

    @Test
    public void initWithQueryException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(SourceUnavailableException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.init();
        verify(fasi, never()).addLocalEntry(any(Metacard.class));
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
        verify(fasi).addLocalEntry(any(Metacard.class));
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
        verify(fasi, never()).addLocalEntry(any(Metacard.class));
    }

    @Test
    public void testGetRegistryMetacard() throws Exception {
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

    @Test(expected = IngestException.class)
    public void testAddLocalEntryCreateException() throws Exception {
        Metacard metacard = getTestMetacard();

        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(IngestException.class).when(catalogFramework)
                .create(any(CreateRequest.class));

        fasi.addLocalEntry(metacard);
        verify(fasi).addLocalEntry(any(Metacard.class));
    }

    @Test(expected = UnsupportedQueryException.class)
    public void testGetRegistryMetacardsUnsupportedQueryException() throws Exception {
        when(security.getSystemSubject()).thenReturn(subject);
        doThrow(UnsupportedQueryException.class).when(catalogFramework)
                .query(any(QueryRequest.class));

        fasi.getRegistryMetacards();
        verify(fasi).getRegistryMetacards();
    }

    private Metacard getTestMetacard() {
        Metacard metacard = new MetacardImpl(new RegistryObjectMetacardType());

        return metacard;
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

}
