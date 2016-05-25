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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.CatalogFramework;
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
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

@RunWith(MockitoJUnitRunner.class)
public class RefreshRegistrySubscriptionsTest {

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private static final String TEST_SITE_NAME = "Slate Rock and Gravel Company";

    private static final String TEST_VERSION = "FF 2.0";

    private static final String TEST_METACARD_ID = "MetacardId";

    private static final String TEST_ID = "TestId";

    private static final String TEST_XML_STRING = "SomeValidStringVersionOfXml";

    @Mock
    private FederationAdminServiceImpl federationAdminService;

    private RefreshRegistrySubscriptions refreshRegistrySubscriptions;

    private Metacard testMetacard;

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
    private FilterBuilder filterBuilder;

    @Mock
    private AttributeBuilder attributeBuilder;

    @Mock
    private ExpressionBuilder expressionBuilder;

    @Mock
    private Subject subject;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private ServiceReference serviceReference;

    @Mock
    private RegistryStore registryStore;

    @Before
    public void setUp() throws Exception {
        when(parser.configureParser(anyList(), any(ClassLoader.class))).thenReturn(configurator);
        when(filterBuilder.attribute(any(String.class))).thenReturn(attributeBuilder);
        when(attributeBuilder.is()).thenReturn(expressionBuilder);
        when(expressionBuilder.bool(any(Boolean.class))).thenReturn(getTestFilter());
        refreshRegistrySubscriptions = spy(new RefreshRegistrySubscriptions());
        federationAdminService.setRegistryTransformer(registryTransformer);
        federationAdminService.setCatalogFramework(catalogFramework);
        federationAdminService.setParser(parser);
        federationAdminService.setFilterBuilder(filterBuilder);
        refreshRegistrySubscriptions.setFederationAdminService(federationAdminService);
        System.setProperty(SystemInfo.SITE_NAME, TEST_SITE_NAME);
        System.setProperty(SystemInfo.VERSION, TEST_VERSION);
        testMetacard = getPopulatedTestRegistryMetacard();

    }

    @Test
    public void testRefreshRegistrySubscriptionsWhenPollableSourceIdsIAreEmpty() throws Exception {
        refreshRegistrySubscriptions.refreshRegistrySubscriptions();
        verify(refreshRegistrySubscriptions).refreshRegistrySubscriptions();
    }

    @Test
    public void testCreateRemoteEntries() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = testMetacard;
        Metacard thirdMetacard = getPopulatedTestRegistryMetacard();
        thirdMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                firstMetacard,
                secondMetacard);
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        localMetacards.add(secondMetacard);
        doReturn(localMetacards).when(federationAdminService)
                .getRegistryMetacards();
        QueryRequest request1 = getTestQueryRequest();
        QueryResponse response1 = getPopulatedTestQueryResponse(request1, thirdMetacard);
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response, response1);
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
        refreshRegistrySubscriptions.refreshRegistrySubscriptions();
    }

    @Test(expected = FederationAdminException.class)
    public void testcreateRemoteEntriesSourceUnavailableOnQuery() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = testMetacard;
        Metacard thirdMetacard = getPopulatedTestRegistryMetacard();
        thirdMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        localMetacards.add(secondMetacard);
        doReturn(localMetacards).when(federationAdminService)
                .getRegistryMetacards();
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(federationAdminService.getRegistryMetacards()).thenThrow(SourceUnavailableException.class);
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
        refreshRegistrySubscriptions.refreshRegistrySubscriptions();
    }

    @Test
    public void testcreateRemoteEntriesSourceUnavailableOnCreate() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = getPopulatedTestRegistryMetacard();
        secondMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        List<Metacard> localMetacards2 = new ArrayList<>();
        localMetacards2.add(secondMetacard);
        QueryRequest request = getTestQueryRequest();
        QueryResponse response1 = getPopulatedTestQueryResponse(request, firstMetacard);
        when(federationAdminService.getRegistryMetacards()).thenReturn(localMetacards,
                localMetacards2);
        QueryRequest request1 = getTestQueryRequest();
        QueryResponse response2 = getPopulatedTestQueryResponse(request1, secondMetacard);
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response1, response2);
        when(catalogFramework.create(any(CreateRequest.class))).thenThrow(SourceUnavailableException.class);
        when(security.getSystemSubject()).thenReturn(subject);
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
    }

    @Test
    public void testWriteRemoteUpdates() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = getPopulatedTestRegistryMetacard();
        Metacard diffDateMetacard = getPopulatedTestRegistryMetacard();
        firstMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        secondMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "2"));
        //Assign diffDateMetacard sameId as firstMetacard but with a different modified date
        diffDateMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        //Date set 1000ms in the future to avoid timing issues
        diffDateMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED,
                new Date(new Date().getTime() - 1000)));
        //Add only first two metacarsds to localMetacards, in order to compare them against response containing diffDateMetacard
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        List<Metacard> localMetacards2 = new ArrayList<>();
        localMetacards2.add(diffDateMetacard);
        when(federationAdminService.getRegistryMetacards()).thenReturn(localMetacards,
                localMetacards2);
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request, diffDateMetacard);
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(catalogFramework.update(any(UpdateRequest.class))).thenReturn(null);
        //bindRegistryStore calls the method being tested: writeRemoteUpdates()
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
    }

    @Test
    public void testWriteRemoteUpdatesSourceUnavailable() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = getPopulatedTestRegistryMetacard();
        Metacard diffDateMetacard = getPopulatedTestRegistryMetacard();
        firstMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        secondMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "2"));
        //Assign diffDateMetacard sameId as firstMetacard but with a different modified date
        diffDateMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID + "1"));
        //Date set 1000ms in the future to avoid timing issues
        diffDateMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED,
                new Date(new Date().getTime() + 1000)));
        //Add only first two metacarsds to localMetacards, in order to compare them against response containing diffDateMetacard
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        localMetacards.add(secondMetacard);
        doReturn(localMetacards).when(federationAdminService)
                .getRegistryMetacards();
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request, diffDateMetacard);
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        when(catalogFramework.update(any(UpdateRequest.class))).thenThrow(SourceUnavailableException.class);
        //bindRegistryStore calls the method being tested: writeRemoteUpdates()
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
    }

    @Test
    public void testBindRegistryStoreNullServiceReference() {
        refreshRegistrySubscriptions.bindRegistryStore(null);
    }

    @Test
    public void testUnbindRegistryStore()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(RegistryObjectMetacardType.REGISTRY_ID);
        QueryRequest request = getTestQueryRequest();
        Metacard metacard = testMetacard;
        QueryResponse response = getPopulatedTestQueryResponse(request, metacard);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
        refreshRegistrySubscriptions.unbindRegistryStore(serviceReference);
    }

    @Test
    public void testRefreshRegistrySubscriptions() throws Exception {
        Metacard firstMetacard = testMetacard;
        Metacard secondMetacard = testMetacard;
        QueryRequest request = getTestQueryRequest();
        QueryResponse response = getPopulatedTestQueryResponse(request,
                firstMetacard,
                secondMetacard);
        List<Metacard> localMetacards = new ArrayList<>();
        localMetacards.add(firstMetacard);
        localMetacards.add(secondMetacard);
        doReturn(localMetacards).when(federationAdminService)
                .getRegistryMetacards();
        when(refreshRegistrySubscriptions.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.getService(serviceReference)).thenReturn(registryStore);
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
        refreshRegistrySubscriptions.bindRegistryStore(serviceReference);
        refreshRegistrySubscriptions.refreshRegistrySubscriptions();
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

    private Metacard getPopulatedTestRegistryMetacard() {
        Metacard registryMetacard = getTestMetacard();
        registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                RegistryObjectMetacardType.REGISTRY_ID));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, new Date()));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.ID, TEST_METACARD_ID));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, TEST_XML_STRING));
        return registryMetacard;
    }
}
