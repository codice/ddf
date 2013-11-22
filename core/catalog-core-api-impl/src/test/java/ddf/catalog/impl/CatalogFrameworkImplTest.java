/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.impl.CatalogFrameworkImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoRequestSources;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.CachedSource;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;

public class CatalogFrameworkImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkImplTest.class);

    // Test proper use-cases

    // Start testing Describable

    // End testing Describable

    // Start testing MetacardReader

    @Rule
    public MethodRule watchman = new TestWatchman() {
        public void starting(FrameworkMethod method) {
            LOGGER.debug("***************************  STARTING: {}  **************************\n"
                    + method.getName());
        }

        public void finished(FrameworkMethod method) {
            LOGGER.debug("***************************  END: {}  **************************\n"
                    + method.getName());
        }
    };

    @BeforeClass
    public static void init() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.INFO);
    }

    // Start testing MetacardWriter

    /**
     * Tests that the framework properly passes a create request to the local provider.
     */
    @Test
    public void testCreate() throws Exception {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        postIngestPlugins.add(eventAdmin);
        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        CreateResponse response = framework.create(new CreateRequestImpl(metacards, null));
        assertEquals(response.getCreatedMetacards().size(), provider.size());
        for (Metacard curCard : response.getCreatedMetacards()) {
            assertNotNull(curCard.getId());
        }

        // make sure that the event was posted correctly
        assertTrue(eventAdmin.wasEventPosted());
        Metacard[] array = {};
        array = response.getCreatedMetacards().toArray(array);
        assertTrue(eventAdmin.wasEventPosted());
        assertEquals(eventAdmin.getLastEvent(), array[array.length - 1]);

    }

    /**
     * Tests that the framework properly passes an update request to the local provider.
     */
    @Test
    public void testUpdate() throws Exception {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        postIngestPlugins.add(eventAdmin);
        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        // create the entry manually in the provider
        CreateResponse response = provider.create(new CreateRequestImpl(metacards, null));

        Metacard insertedCard = response.getCreatedMetacards().get(0);
        List<Entry<Serializable, Metacard>> updatedEntries = new ArrayList<Entry<Serializable, Metacard>>();
        updatedEntries.add(new SimpleEntry<Serializable, Metacard>(insertedCard.getId(),
                insertedCard));
        UpdateRequest request = new UpdateRequestImpl(updatedEntries, Metacard.ID, null);
        // send update to framework
        List<Update> returnedCards = framework.update(request).getUpdatedMetacards();
        for (Update curCard : returnedCards) {
            assertNotNull(curCard.getNewMetacard().getId());
        }

        // make sure that the event was posted correctly
        assertTrue(eventAdmin.wasEventPosted());
        assertEquals(eventAdmin.getLastEvent(), returnedCards.get(returnedCards.size() - 1)
                .getOldMetacard());

    }

    /**
     * Tests that the framework properly passes a delete request to the local provider.
     */
    @Test
    public void testDelete() throws Exception {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        postIngestPlugins.add(eventAdmin);
        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        // create the entry manually in the provider
        Metacard insertedCard = provider.create(new CreateRequestImpl(metacards, null))
                .getCreatedMetacards().iterator().next();

        String[] ids = new String[1];
        ids[0] = insertedCard.getId();

        // send delete to framework
        List<Metacard> returnedCards = framework.delete(new DeleteRequestImpl(ids))
                .getDeletedMetacards();
        assertEquals(ids.length, returnedCards.size());
        // make sure that the event was posted correctly
        Metacard[] array = {};
        array = returnedCards.toArray(array);
        assertTrue(eventAdmin.wasEventPosted());
        assertEquals(eventAdmin.getLastEvent(), array[array.length - 1]);

    }

    /**
     * Tests that the framework properly passes an update by identifier request to the local
     * provider.
     */
    @Test
    public void testUpdateByIdentifier() throws Exception {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        postIngestPlugins.add(eventAdmin);
        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);

        List<Metacard> metacards = new ArrayList<Metacard>();
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        newCard.setResourceURI(new URI("DDF:///12345"));
        metacards.add(newCard);

        // create the entry manually in the provider
        List<Metacard> insertedCards = provider.create(new CreateRequestImpl(metacards))
                .getCreatedMetacards();

        ArrayList<URI> list = new ArrayList<URI>();

        list.add(new URI("DDF:///12345"));

        UpdateRequest request = new UpdateRequestImpl((URI[]) list.toArray(new URI[list.size()]),
                insertedCards);

        // send update to framework
        UpdateResponse updateResponse = framework.update(request);
        List<Update> returnedCards = updateResponse.getUpdatedMetacards();
        assertNotNull(returnedCards);
        assertEquals(list.size(), returnedCards.size());
        assertTrue(provider.hasReceivedUpdateByIdentifier());

        // make sure that the event was posted correctly
        assertTrue(eventAdmin.wasEventPosted());
        assertEquals(eventAdmin.getLastEvent(), returnedCards.get(returnedCards.size() - 1)
                .getOldMetacard());
    }

    /**
     * Tests that the framework properly passes a delete by identifier request to the local
     * provider.
     */
    @Ignore
    @Test
    public void testDeleteByIdentifier() {
        // TODO create
    }

    // End testing MetacardWriter

    // Start testing CatalogFramework

    @Ignore
    @Test
    public void testFederateRead() {
        // TODO create
    }

    @Ignore
    @Test
    public void testFederateReadWithFrameworkName() {
        // TODO create
    }

    /*
     * Test for "ResourceResponse returns null ResourceRequest in the PostResourcePlugin"
     * 
     * The error this test case addresses is as follows: The PostResourcePlugin receives a
     * ResourceResponse with a null ResourceRequest.
     */
    @Test
    public void testGetResource_WhenNonNullResourceRequest_ExpectPostResourcePluginToReceiveResourceResponseWithNonNullResourceRequest()
        throws Exception {

        // Setup
        /*
         * Prepare to capture the ResourceResponse argument passed into
         * PostResourcePlugin.process(). We will verify that it contains a non-null ResourceRequest
         * in the verification section of this test.
         */
        ArgumentCaptor<ResourceResponse> argument = ArgumentCaptor.forClass(ResourceResponse.class);

        Resource mockResource = mock(Resource.class);

        ResourceRequest mockResourceRequest = mock(ResourceRequest.class);
        when(mockResourceRequest.getAttributeValue()).thenReturn(new URI("myURI"));
        when(mockResourceRequest.getAttributeName()).thenReturn(new String("myName"));

        ResourceResponse mockResourceResponse = mock(ResourceResponse.class);
        when(mockResourceResponse.getRequest()).thenReturn(mockResourceRequest);
        when(mockResourceResponse.getResource()).thenReturn(mockResource);

        PostResourcePlugin mockPostResourcePlugin = mock(PostResourcePlugin.class);
        /*
         * We verify (see verification section of test) that PostResourcePlugin.process() receives a
         * ResourceResponse with a non-null ResourceRequest. We assume that it works correctly and
         * returns a ResourceResponse with a non-null ResourceRequest, so we return our
         * mockResouceResponse that contains a non-null ResourceRequest.
         */
        when(mockPostResourcePlugin.process(isA(ResourceResponse.class))).thenReturn(
                mockResourceResponse);

        List<PostResourcePlugin> mockPostResourcePlugins = new ArrayList<PostResourcePlugin>();
        mockPostResourcePlugins.add(mockPostResourcePlugin);

        CatalogFramework catalogFrameworkUnderTest = new CatalogFrameworkImpl(null,
                (CatalogProvider) null, new ArrayList<PreIngestPlugin>(),
                new ArrayList<PostIngestPlugin>(), new ArrayList<PreQueryPlugin>(),
                new ArrayList<PostQueryPlugin>(), new ArrayList<PreResourcePlugin>(),
                mockPostResourcePlugins, new ArrayList<ConnectedSource>(),
                new ArrayList<FederatedSource>(), new ArrayList<ResourceReader>(), null, null, null) {
            @Override
            protected URI getResourceURI(ResourceRequest resourceRequest, String site,
                    boolean isEnterprise, StringBuilder federatedSite,
                    Map<String, Serializable> requestProperties) {
                URI uri = null;

                try {
                    uri = new URI("myURI");
                } catch (URISyntaxException e) {
                }

                return uri;
            };

            @Override
            protected ResourceResponse getResourceUsingResourceReader(URI resourceUri,
                    Map<String, Serializable> properties) {
                Resource mockResource = mock(Resource.class);
                ResourceResponse resourceResponse = new ResourceResponseImpl(mockResource);
                LOGGER.debug("resourceResponse.getResource() returned: "
                        + resourceResponse.getResource());
                LOGGER.debug("resourceResponse.getRequest() expected returned value: null;  actual returned value: "
                        + resourceResponse.getRequest());
                assertNull(resourceResponse.getRequest());
                // Returns a ResourceResponse with a null ResourceRequest.
                return resourceResponse;
            }
        };

        String sourceId = "myId";
        ((CatalogFrameworkImpl) catalogFrameworkUnderTest).setId(sourceId);

        String resourceSiteName = "myId";

        // Execute
        LOGGER.debug("Testing CatalogFramework.getResource(ResourceRequest, String)...");
        ResourceResponse resourceResponse = catalogFrameworkUnderTest.getResource(
                mockResourceRequest, resourceSiteName);
        LOGGER.debug("resourceResponse: " + resourceResponse);

        // Verify
        /*
         * Verify that when PostResoucePlugin.process() is called, the ResourceResponse argument
         * contains a non-null ResourceRequest.
         */
        verify(mockPostResourcePlugin).process(argument.capture());
        assertNotNull(
                "PostResourcePlugin received a ResourceResponse with a null ResourceRequest.",
                argument.getValue().getRequest());

        /*
         * We really don't need to assert this since we return our mockResourceResponse from
         * PostResourcePlugin.process()
         */
        // assertNotNull("ResourceResponse.getResource() returned a ResourceResponse with a null ResourceRequest.",
        // resourceResponse.getRequest());
    }

    @Test(expected = FederationException.class)
    public void testPreQuery_StopExecution() throws UnsupportedQueryException, FederationException {

        SourcePoller poller = mock(SourcePoller.class);
        when(poller.getCachedSource(isA(Source.class))).thenReturn(null);

        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        BundleContext context = null;

        FederationStrategy federationStrategy = mock(FederationStrategy.class);

        QueryRequest request = mock(QueryRequest.class);

        when(request.getQuery()).thenReturn(mock(Query.class));

        PreQueryPlugin stopQueryPlugin = new PreQueryPlugin() {

            @Override
            public QueryRequest process(QueryRequest input) throws PluginExecutionException,
                StopProcessingException {
                throw new StopProcessingException("Testing that the framework will stop the query.");
            }
        };

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), context,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                Arrays.asList(stopQueryPlugin), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), federationStrategy, null, poller);

        framework.bind(provider);
        framework.query(request);
    }

    @Test(expected = FederationException.class)
    public void testPostQuery_StopExecution() throws UnsupportedQueryException, FederationException {

        SourcePoller poller = mock(SourcePoller.class);

        when(poller.getCachedSource(isA(Source.class))).thenReturn(null);


        BundleContext context = null;

        FilterFactory filterFactory = new FilterFactoryImpl();

        Filter filter = filterFactory.like(filterFactory.property(Metacard.METADATA), "goodyear",
                "*", "?", "/", false);

        QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));

        SourceResponseImpl sourceResponse = new SourceResponseImpl(request, new ArrayList<Result>());

        QueryResponseImpl queryResponse = new QueryResponseImpl(sourceResponse, "anyId");

        CatalogProvider provider = mock(CatalogProvider.class);

        when(provider.query(isA(QueryRequest.class))).thenReturn(sourceResponse);

        FederationStrategy federationStrategy = mock(FederationStrategy.class);

        when(federationStrategy.federate(isA(List.class), isA(QueryRequest.class))).thenReturn(
                queryResponse);

        PostQueryPlugin stopQueryPlugin = new PostQueryPlugin() {

            @Override
            public QueryResponse process(QueryResponse input) throws PluginExecutionException,
                StopProcessingException {
                throw new StopProcessingException("Testing that the framework will stop the query.");
            }

        };

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), context,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), Arrays.asList(stopQueryPlugin),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), federationStrategy, null, poller);

        framework.bind(provider);
        framework.query(request);
    }

    @Ignore
    @Test
    public void testFederateQueryWithFrameworkName() {
        // TODO create
    }

    @Ignore
    @Test
    public void testQueryTransform() {
        // TODO create
    }

    @Ignore
    @Test
    public void testMetacardTransform() {
        // TODO create
    }

    @Test
    public void testGetSites() {
        // Catalog Provider
        CatalogProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);

        // Set<Source> expectedSourceSet = new HashSet<Source>();
        // add framework as a site
        // add local provider
        // expectedSourceSet.add(provider);
        // add all of the federated sites
        // expectedSourceSet.addAll(federatedSources);

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);


        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), federatedSources,
                new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);
        framework.setId("ddf");

        Set<String> ids = new HashSet<String>();
        for (FederatedSource source : federatedSources) {
            ids.add(source.getId());
        }
        ids.add(framework.getId());

        SourceInfoRequest request = new SourceInfoRequestSources(true, ids);

        SourceInfoResponse response = null;
        try {
            response = framework.getSourceInfo(request);
        } catch (SourceUnavailableException e) {
            fail();
        }
        Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();

        List<String> siteNames = new ArrayList<String>();
        for (SourceDescriptor descriptor : sourceDescriptors) {
            LOGGER.debug("Descriptor id: " + descriptor.getSourceId());
            siteNames.add(descriptor.getSourceId());
        }

        // add a plus one for now to simulate that the framework is ad
        // assertTrue( sourceDescriptor.containsAll( federatedSources ) );
        // assertTrue( sourceDescriptor.containsAll( expectedSourceSet ) );
        assertEquals(ids.size(), sourceDescriptors.size());

        String[] expectedOrdering = {"A", "B", "C", framework.getId()};

        assertArrayEquals(expectedOrdering, siteNames.toArray(new String[siteNames.size()]));

    }

    @Test
    public void testGetFederatedSources() {
        List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);

        // Mock register the federated sources in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(null, (CatalogProvider) null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), federatedSources,
                new ArrayList<ResourceReader>(), null, null, mockPoller);

        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
        SourceInfoResponse response = null;
        try {
            response = framework.getSourceInfo(request);
        } catch (SourceUnavailableException e) {
            fail();
        }
        Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
        for (SourceDescriptor descriptor : sourceDescriptors) {
            LOGGER.debug("Descriptor id: " + descriptor.getSourceId());
        }

        // The "+1" is to account for the CatalogFramework source descriptor.
        // Even if no local catalog provider is configured, the catalog framework's
        // site info is included in the SourceDescriptos list.
        assertEquals(federatedSources.size() + 1, sourceDescriptors.size());
    }

    @Test
    public void testGetUnavailableFederatedSources() {
        List<FederatedSource> federatedSources = createDefaultFederatedSourceList(false);

        // Mock register the federated sources in the container
        SourcePollerRunner runner = new SourcePollerRunner();
        SourcePoller poller = new SourcePoller(runner);
        for (FederatedSource source : federatedSources) {
            runner.bind(source);
        }

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(null, (CatalogProvider) null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), federatedSources,
                new ArrayList<ResourceReader>(), null, null, poller);

        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
        SourceInfoResponse response = null;
        try {
            response = framework.getSourceInfo(request);
        } catch (SourceUnavailableException e) {
            fail();
        }
        Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
        for (SourceDescriptor descriptor : sourceDescriptors) {
            LOGGER.debug("Descriptor id: " + descriptor.getSourceId());
            if (StringUtils.isNotBlank(descriptor.getId())) {
                assertFalse(descriptor.isAvailable());
                // No contentTypes should be listed if the source is unavailable
                assertTrue(descriptor.getContentTypes().isEmpty());
            }
        }

        // The "+1" is to account for the CatalogFramework source descriptor.
        // Even if no local catalog provider is configured, the catalog
        // framework's
        // site info is included in the SourceDescriptos list.
        assertEquals(federatedSources.size() + 1, sourceDescriptors.size());
    }

    @Test
    public void testGetFederatedSourcesDuplicates() {
        List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);
        // Duplicate Site
        FederatedSource siteC2 = new MockSource("C", "Site C2", "v1.0", "DDF", null, true,
                new Date());
        federatedSources.add(siteC2);

        // Expected Sites
        List<FederatedSource> expectedSources = createDefaultFederatedSourceList(true);

        // Mock register the federated sources in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);


        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(null, (CatalogProvider) null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), expectedSources, new ArrayList<ResourceReader>(),
                null, null, mockPoller);

        // Returned Sites
        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);

        SourceInfoResponse response = null;
        try {
            response = framework.getSourceInfo(request);
        } catch (SourceUnavailableException e) {
            LOGGER.debug("SourceUnavilable", e);
            fail();
        }
        Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
        // should contain ONLY the original federated sites and the catalog framework's
        // site info (even though it has no local catalog provider configured) - hence,
        // the "+1"
        assertEquals(expectedSources.size() + 1, sourceDescriptors.size());

    }

    @Test
    public void testGetAllSiteNames() {
        String frameworkName = "DDF";
        CatalogProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);

        // Expected Set of Names
        Set<String> expectedNameSet = new HashSet<String>();
        expectedNameSet.add(frameworkName);
        for (FederatedSource curSite : federatedSources) {
            expectedNameSet.add(curSite.getId());
        }

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList(provider), null, new ArrayList<PreIngestPlugin>(),
                new ArrayList<PostIngestPlugin>(), new ArrayList<PreQueryPlugin>(),
                new ArrayList<PostQueryPlugin>(), new ArrayList<PreResourcePlugin>(),
                new ArrayList<PostResourcePlugin>(), new ArrayList<ConnectedSource>(),
                federatedSources, new ArrayList<ResourceReader>(), null, null, mockPoller);
        framework.bind(provider);
        framework.setId(frameworkName);

        // Returned Set of Names
        // Returned Sites
        SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
        SourceInfoResponse response = null;
        try {
            response = framework.getSourceInfo(request);
        } catch (SourceUnavailableException e) {
            LOGGER.debug("SourceUnavilable", e);
            fail();
        }
        assert (response != null);
        Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
        // should contain ONLY the original federated sites
        assertEquals(expectedNameSet.size(), sourceDescriptors.size());
        Set<String> returnedSourceIds = new HashSet<String>();

        for (SourceDescriptor sd : sourceDescriptors) {
            returnedSourceIds.add(sd.getSourceId());
        }

        for (String id : returnedSourceIds) {
            LOGGER.debug("returned sourceId: " + id);
        }
        assertTrue(expectedNameSet.equals(returnedSourceIds));

    }

    // End testing CatalogFramework

    // Test negative use-cases (expected errors)

    /**
     * Tests that the framework properly throws a catalog exception when the local provider is not
     * available for create.
     * 
     * @throws SourceUnavailableException
     */
    @Test(expected = SourceUnavailableException.class)
    public void testProviderUnavailableCreate() throws SourceUnavailableException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), false, null);
        CatalogFramework framework = createDummyCatalogFramework(provider, eventAdmin, false);
        List<Metacard> metacards = new ArrayList<Metacard>();
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        CreateRequest create = new CreateRequestImpl(metacards);

        // expected to throw exception due to catalog provider being unavailable
        try {
            framework.create(create);
        } catch (IngestException e) {
            fail();
        }

    }

    /**
     * Tests that the framework properly throws a catalog exception when the local provider is not
     * available for update by id.
     * 
     * @throws IngestException
     * @throws SourceUnavailableException
     */
    @Test(expected = SourceUnavailableException.class)
    public void testProviderUnavailableUpdateByID() throws SourceUnavailableException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), false, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, false);
        List<Metacard> metacards = new ArrayList<Metacard>();
        List<URI> uris = new ArrayList<URI>();
        // expected to throw exception due to catalog provider being unavailable
        try {
            MetacardImpl newCard = new MetacardImpl();
            newCard.setId(null);
            newCard.setResourceURI(new URI("uri:///1234"));
            metacards.add(newCard);
            uris.add(new URI("uri:///1234"));

            UpdateRequest update = new UpdateRequestImpl(
                    (URI[]) uris.toArray(new URI[uris.size()]), metacards);

            framework.update(update);
        } catch (URISyntaxException e) {
            fail();
        } catch (IngestException e) {
            fail();
        }
    }

    /**
     * Tests that the framework properly throws a catalog exception when the local provider is not
     * available for update by identifier.
     * 
     * @throws IngestException
     * @throws SourceUnavailableException
     */
    @Test(expected = SourceUnavailableException.class)
    public void testProviderUnavailableUpdateByIdentifier() throws SourceUnavailableException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), false, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, false);
        List<Metacard> metacards = new ArrayList<Metacard>();
        List<URI> uris = new ArrayList<URI>();

        // expected to throw exception due to catalog provider being unavailable
        try {
            MetacardImpl newCard = new MetacardImpl();
            newCard.setId(null);
            newCard.setResourceURI(new URI("uri:///1234"));
            metacards.add(newCard);
            uris.add(new URI("uri:////1234"));

            UpdateRequest update = new UpdateRequestImpl(
                    (URI[]) uris.toArray(new URI[uris.size()]), metacards);

            framework.update(update);
        } catch (URISyntaxException e) {
            fail();
        } catch (IngestException e) {
            fail();
        }
    }

    /**
     * Tests that the framework properly throws a catalog exception when the local provider is not
     * available for delete by id.
     * 
     * @throws IngestException
     * @throws SourceUnavailableException
     */
    @Test(expected = SourceUnavailableException.class)
    public void testProviderUnavailableDeleteByID() throws SourceUnavailableException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), false, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, false);
        List<String> ids = new ArrayList<String>();
        ids.add("1234");

        DeleteRequest request = new DeleteRequestImpl(
                (String[]) ids.toArray(new String[ids.size()]));

        // expected to throw exception due to catalog provider being unavailable
        try {
            framework.delete(request);
        } catch (IngestException e) {
            fail();
        }

    }

    /**
     * Tests that the framework properly throws a catalog exception when the local provider is not
     * available for delete by identifier.
     * 
     * @throws IngestException
     * @throws SourceUnavailableException
     */
    @Test(expected = SourceUnavailableException.class)
    public void testProviderUnavailableDeleteByIdentifier() throws SourceUnavailableException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), false, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, false);
        List<URI> uris = new ArrayList<URI>();
        try {
            uris.add(new URI("id://1234"));
            DeleteRequest request = new DeleteRequestImpl(
                    (URI[]) uris.toArray(new URI[uris.size()]));

            // expected to throw exception due to catalog provider being
            // unavailable
            framework.delete(request);
        } catch (URISyntaxException e) {
            fail();
        } catch (IngestException e) {
            fail();
        }

    }

    /**
     * Tests that the framework properly throws a catalog exception when there are no sites
     * (federated or local) that are available to perform the query.
     * 
     * @throws SourceUnavailableException
     * 
     */
    @Ignore
    @Test(expected = SourceUnavailableException.class)
    public void testNoSitesAvailableFederatedQuery() throws SourceUnavailableException {
        CatalogFramework framework = this.createDummyCatalogFramework(null, null, false);

        QueryRequest request = new QueryRequestImpl(null);

        try {
            framework.query(request);
        } catch (UnsupportedQueryException e) {
            // we don't even care what the query was
        } catch (FederationException e) {
            fail();
        }
    }

    /**
     * Tests that the framework properly throws a catalog exception when the query being passed in
     * is null.
     * 
     * @throws UnsupportedQueryException
     */
    @Test(expected = UnsupportedQueryException.class)
    public void testNullQuery() throws UnsupportedQueryException {
        boolean isAvailable = false;
        CatalogProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), isAvailable, new Date());

        CatalogFramework framework = this.createDummyCatalogFramework(provider, null, true);

        try {
            framework.query(null);
        } catch (FederationException e) {
            fail();
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    /**
     * Tests that the framework properly throws a catalog exception when the federated query being
     * passed in is null.
     * 
     * @throws UnsupportedQueryException
     */
    @Test(expected = UnsupportedQueryException.class)
    public void testNullFederatedQuery() throws UnsupportedQueryException {
        boolean isAvailable = false;
        CatalogProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), isAvailable, new Date());
        createDefaultFederatedSourceList(isAvailable);

        CatalogFramework framework = this.createDummyCatalogFramework(provider, null, true);

        try {
            framework.query(null, null);
        } catch (FederationException e) {
            fail();
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    // @Test( expected = CatalogException.class )
    // public void testNullIdsRead() throws CatalogException
    // {
    // MockEventProcessor eventAdmin = new MockEventProcessor();
    // MockMemoryProvider provider = new MockMemoryProvider( "Provider",
    // "Provider", "v1.0", "DDF",
    // new HashSet<MetacardType>(), true, new Date() );
    // CatalogFramework framework = this.createDummyCatalogFramework(provider,
    // eventAdmin);
    //
    // // call framework with null for the read ids list
    // // framework.read( null, null );
    // }

    @Test(expected = IngestException.class)
    public void testNullEntriesCreate() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);

        // call framework with null request
        try {
            framework.create(null);
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Test(expected = IngestException.class)
    public void testNullEntriesUpdate() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);

        // call framework with null request
        try {
            framework.update(null);
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Test(expected = IngestException.class)
    public void testNullIdsDelete() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);

        // call framework with null request
        try {
            framework.delete(null);
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Test(expected = IngestException.class)
    public void testProviderRuntimeExceptionOnCreate() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        // use exception provider instead of memory
        MockExceptionProvider provider = new MockExceptionProvider("Provider", "Provider", "v1.0",
                "DDF", new HashSet<ContentType>(), true, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        List<Metacard> metacards = new ArrayList<Metacard>();
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);
        metacards.add(newCard);

        CreateRequest create = new CreateRequestImpl((Metacard) null);
        try {
            framework.create(create);
        } catch (SourceUnavailableException e) {
            fail();
        }

    }

    // @Test( expected = CatalogException.class )
    // public void testProviderRuntimeExceptionOnRead() throws CatalogException
    // {
    // MockEventProcessor eventAdmin = new MockEventProcessor();
    // MockExceptionProvider provider = new MockExceptionProvider( "Provider",
    // "Provider", "v1.0", "DDF",
    // new HashSet<MetacardType>(), true, new Date() );
    // CatalogFramework framework = this.createDummyCatalogFramework(provider,
    // eventAdmin);
    //
    // MetacardImpl newCard = new MetacardImpl( );
    // newCard.setId( null );
    //
    // List<String> ids = new ArrayList<String>();
    // ids.add( newCard.getId() );
    //
    // // send read to framework
    // // framework.read( null, ids );
    //
    // }

    @Test(expected = IngestException.class)
    public void testProviderRuntimeExceptionOnUpdateByID() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        // use exception provider instead of memory
        MockExceptionProvider provider = new MockExceptionProvider("Provider", "Provider", "v1.0",
                "DDF", new HashSet<ContentType>(), true, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        List<Entry<Object, Metacard>> metacards = new ArrayList<Entry<Object, Metacard>>();
        HashMap<Object, Metacard> map = new HashMap<Object, Metacard>();

        // expected to throw exception due to catalog provider being unavailable
        try {
            MetacardImpl newCard = new MetacardImpl();
            newCard.setId(null);
            newCard.setResourceURI(new URI("uri:///1234"));
            map.put(Metacard.ID, newCard);
            metacards.addAll(map.entrySet());

            UpdateRequest update = new UpdateRequestImpl(null, Metacard.ID, null);
            framework.update(update);
        } catch (URISyntaxException e) {
            fail();
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Test(expected = IngestException.class)
    public void testProviderRuntimeExceptionOnUpdateByIdentifier() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        // use exception provider instead of memory
        MockExceptionProvider provider = new MockExceptionProvider("Provider", "Provider", "v1.0",
                "DDF", new HashSet<ContentType>(), true, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        List<Entry<Object, Metacard>> metacards = new ArrayList<Entry<Object, Metacard>>();
        HashMap<Object, Metacard> map = new HashMap<Object, Metacard>();

        try {
            MetacardImpl newCard = new MetacardImpl();
            newCard.setId(null);
            newCard.setResourceURI(new URI("uri:///1234"));
            map.put(Metacard.ID, newCard);
            metacards.addAll(map.entrySet());

            UpdateRequest update = new UpdateRequestImpl(null, Metacard.RESOURCE_URI, null);
            framework.update(update);
        } catch (URISyntaxException e) {
            fail();
        } catch (SourceUnavailableException e) {
            fail();
        }

    }

    @Test(expected = IngestException.class)
    public void testProviderRuntimeExceptionOnDeleteByID() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        // use exception provider instead of memory
        MockExceptionProvider provider = new MockExceptionProvider("Provider", "Provider", "v1.0",
                "DDF", new HashSet<ContentType>(), true, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        List<String> ids = new ArrayList<String>();
        ids.add("1234");

        DeleteRequest request = new DeleteRequestImpl(
                (String[]) ids.toArray(new String[ids.size()]));

        // expected to throw exception due to catalog provider
        try {
            framework.delete(request);
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Test(expected = IngestException.class)
    public void testProviderRuntimeExceptionOnDeleteByIdentifier() throws IngestException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        // use exception provider instead of memory
        MockExceptionProvider provider = new MockExceptionProvider("Provider", "Provider", "v1.0",
                "DDF", new HashSet<ContentType>(), true, null);
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        // List<MetacardType> identifiers = new ArrayList<MetacardType>();
        // identifiers.add( new MetacardTypeImpl( "id", "1234" ) );
        ArrayList<URI> uris = new ArrayList<URI>();

        DeleteRequest request = new DeleteRequestImpl((URI[]) uris.toArray(new URI[uris.size()]));
        // expected to throw exception due to catalog provider being unavailable
        try {
            framework.delete(request);
        } catch (SourceUnavailableException e) {
            fail();
        }
    }

    @Ignore
    @Test(expected = CatalogTransformerException.class)
    public void testMetacardTransformWithBadShortname() throws CatalogTransformerException {
        MockEventProcessor eventAdmin = new MockEventProcessor();
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());
        // TODO pass in bundle context
        CatalogFramework framework = this.createDummyCatalogFramework(provider, eventAdmin, true);
        MetacardImpl newCard = new MetacardImpl();
        newCard.setId(null);

        framework.transform(newCard, "NONE", new HashMap<String, Serializable>());

    }

    /**
     * Tests that you can get a resource's (product) options. Covers the case where the source ID
     * specified is actually the local catalog provider's site name (so this reduces down to a
     * getResourceOptions for local provider); and the case where a federated source is specified.
     * 
     * Test for DDF-1763.
     * 
     * @throws Exception
     */
    @Test
    public void testGetResourceOptions() throws Exception {
        String localProviderName = "ddf";
        String federatedSite1Name = "fed-site-1";
        String metacardId = "123";

        // The resource's URI
        URI metacardUri = new URI(
                "http:///27+Nov+12+12%3A30%3A04?MyPhotograph%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc%2Fimages%2FActionable.jpg%0AMyAttachment%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc#abc.xyz.dao.URLResourceOptionDataAccessObject");

        Set<String> supportedOptions = new HashSet<String>();
        supportedOptions.add("MyPhotograph");
        supportedOptions.add("MyAttachment");

        // Catalog Provider
        CatalogProvider provider = mock(CatalogProvider.class);
        when(provider.getId()).thenReturn(localProviderName);
        when(provider.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
        when(provider.isAvailable()).thenReturn(true);

        // Federated Source 1
        FederatedSource federatedSource1 = mock(FederatedSource.class);
        when(federatedSource1.getId()).thenReturn(federatedSite1Name);
        when(federatedSource1.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
        when(federatedSource1.isAvailable()).thenReturn(true);
        when(federatedSource1.getOptions(isA(Metacard.class))).thenReturn(supportedOptions);

        List<FederatedSource> federatedSources = new ArrayList<FederatedSource>();
        federatedSources.add(federatedSource1);

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        Metacard metacard = mock(Metacard.class);
        when(metacard.getId()).thenReturn(metacardId);
        when(metacard.getResourceURI()).thenReturn(metacardUri);
        Result result = mock(Result.class);
        when(result.getMetacard()).thenReturn(metacard);
        List<Result> results = new ArrayList<Result>();
        results.add(result);

        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(results);
        FederationStrategy strategy = mock(FederationStrategy.class);
        when(strategy.federate(isA(federatedSources.getClass()), isA(QueryRequest.class)))
                .thenReturn(queryResponse);

        ResourceReader resourceReader = mock(ResourceReader.class);
        Set<String> supportedSchemes = new HashSet<String>();
        supportedSchemes.add("http");
        when(resourceReader.getSupportedSchemes()).thenReturn(supportedSchemes);
        when(resourceReader.getOptions(isA(Metacard.class))).thenReturn(supportedOptions);
        List<ResourceReader> resourceReaders = new ArrayList<ResourceReader>();
        resourceReaders.add(resourceReader);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), federatedSources, resourceReaders, strategy,
                null, mockPoller);
        framework.bind(provider);
        framework.setId("ddf");

        Set<String> ids = new HashSet<String>();
        for (FederatedSource source : federatedSources) {
            ids.add(source.getId());
        }
        ids.add(framework.getId());

        // site name = local provider
        Map<String, Set<String>> optionsMap = framework.getResourceOptions(metacardId,
                localProviderName);
        LOGGER.debug("localProvider optionsMap = " + optionsMap);
        assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

        // site name = federated site's name
        optionsMap = framework.getResourceOptions(metacardId, federatedSite1Name);
        LOGGER.debug("federatedSource optionsMap = " + optionsMap);
        assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

        // site name = null (should default to local provider)
        optionsMap = framework.getResourceOptions(metacardId, null);
        LOGGER.debug("localProvider optionsMap = " + optionsMap);
        assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

        // site name = empty string (should default to local provider)
        optionsMap = framework.getResourceOptions(metacardId, "");
        LOGGER.debug("localProvider optionsMap = " + optionsMap);
        assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));
    }

    /**
     * Tests that multiple ResourceReaders with the same scheme will be invoked if the first one did
     * not return a Response.
     * 
     * @throws Exception
     */
    @Test
    public void testGetResourceToTestSecondResourceReaderWithSameSchemeGetsCalledIfFirstDoesNotReturnAnything()
        throws Exception {
        String localProviderName = "ddf";
        String metacardId = "123";
        final String EXPECTED = "result from mockResourceResponse2";
        final String DDF = "ddf";

        // The resource's URI
        URI metacardUri = new URI(
                "http:///27+Nov+12+12%3A30%3A04?MyPhotograph%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc%2Fimages%2FActionable.jpg%0AMyAttachment%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc#abc.xyz.dao.URLResourceOptionDataAccessObject");

        // Mock a Catalog Provider
        CatalogProvider provider = mock(CatalogProvider.class);
        when(provider.getId()).thenReturn(localProviderName);
        when(provider.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
        when(provider.isAvailable()).thenReturn(true);

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

        // Create two ResourceReaders. The first should not return anything
        // and the second should.
        ResourceReader resourceReader1 = mock(ResourceReader.class);
        ResourceReader resourceReader2 = mock(ResourceReader.class);

        // Set the supported Schemes so that both ResourceReaders use
        // the same scheme ("DAD")
        Set<String> supportedSchemes = new HashSet<String>();
        supportedSchemes.add("DAD");

        when(resourceReader1.getSupportedSchemes()).thenReturn(supportedSchemes);
        when(resourceReader2.getSupportedSchemes()).thenReturn(supportedSchemes);

        List<ResourceReader> resourceReaders = new ArrayList<ResourceReader>();
        resourceReaders.add(resourceReader1);
        resourceReaders.add(resourceReader2);

        // Set up the requests and responses. The first ResourceReader will return null
        // and the second one will retrieve a value, showing that if more than one
        // ResourceReader with the same scheme are used, they will be called until a
        // response is returned
        ResourceRequest mockResourceRequest = mock(ResourceRequest.class);
        URI myURI = new URI("DAD", "host", "/path", "fragment");
        when(mockResourceRequest.getAttributeValue()).thenReturn(myURI);
        when(mockResourceRequest.getAttributeName()).thenReturn(
                new String(ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI));

        Result result = mock(Result.class);
        Metacard metacard = mock(Metacard.class);
        when(metacard.getResourceURI()).thenReturn(myURI);
        when(result.getMetacard()).thenReturn(metacard);
        List<Result> results = new ArrayList<Result>();
        results.add(result);

        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getResults()).thenReturn(results);

        List<Source> federatedSources = new ArrayList<Source>();

        FederationStrategy strategy = mock(FederationStrategy.class);
        when(strategy.federate(isA(federatedSources.getClass()), isA(QueryRequest.class)))
                .thenReturn(queryResponse);

        ResourceResponse mockResourceResponse1 = mock(ResourceResponse.class);
        when(mockResourceResponse1.getRequest()).thenReturn(mockResourceRequest);
        when(mockResourceResponse1.getResource()).thenReturn(null);
        when(resourceReader1.retrieveResource(any(URI.class), anyMap())).thenReturn(null);

        Resource mockResource = mock(Resource.class);
        when(mockResource.getName()).thenReturn(EXPECTED);
        ResourceResponse mockResourceResponse2 = mock(ResourceResponse.class);
        when(mockResourceResponse2.getResource()).thenReturn(mockResource);
        when(resourceReader2.retrieveResource(any(URI.class), anyMap())).thenReturn(
                mockResourceResponse2);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList((CatalogProvider) provider), null,
                new ArrayList<PreIngestPlugin>(), new ArrayList<PostIngestPlugin>(),
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), null, resourceReaders, strategy, null, mockPoller);
        framework.bind(provider);
        framework.setId(DDF);

        ResourceResponse response = framework.getResource(mockResourceRequest, false, DDF);

        // Verify that the Response is as expected
        org.junit.Assert.assertEquals(EXPECTED, response.getResource().getName());

        // Verify that resourceReader1 was called 1 time
        // This line is equivalent to verify(resourceReader1,
        // times(1)).retrieveResource(any(URI.class), anyMap());
        verify(resourceReader1).retrieveResource(any(URI.class), anyMap());

    }

    /**************************** utility methods ******************************/

    private List<FederatedSource> createDefaultFederatedSourceList(boolean isAvailable) {
        FederatedSource siteA = new MockSource("A", "Site A", "v1.0", "DDF", null, isAvailable,
                new Date());
        FederatedSource siteB = new MockSource("B", "Site B", "v1.0", "DDF", null, isAvailable,
                new Date());
        FederatedSource siteC = new MockSource("C", "Site C", "v1.0", "DDF", null, isAvailable,
                new Date());
        ArrayList<FederatedSource> federatedSources = new ArrayList<FederatedSource>();
        federatedSources.add(siteC);
        federatedSources.add(siteB);
        federatedSources.add(siteA);

        return federatedSources;
    }

    private CatalogFramework createDummyCatalogFramework(CatalogProvider provider,
            MockEventProcessor admin, boolean sourceAvailability) {
        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        CachedSource mockSource = mock(CachedSource.class);
        when(mockSource.isAvailable()).thenReturn(sourceAvailability);
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(mockSource);

        CatalogFrameworkImpl framework = new CatalogFrameworkImpl(
                Collections.singletonList(provider), null, new ArrayList<PreIngestPlugin>(),
                new ArrayList<PostIngestPlugin>(), new ArrayList<PreQueryPlugin>(),
                new ArrayList<PostQueryPlugin>(), new ArrayList<PreResourcePlugin>(),
                new ArrayList<PostResourcePlugin>(), new ArrayList<ConnectedSource>(),
                new ArrayList<FederatedSource>(), new ArrayList<ResourceReader>(), null, null,
                mockPoller);
        framework.bind(provider);

        return framework;
    }

}
