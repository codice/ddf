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

package org.codice.ddf.catalog.admin.poller;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.shiro.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.metatype.AttributeDefinition;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.opensearch.OpenSearchSource;

public class AdminPollerTest {

    public static final String CONFIG_PID = "properPid";

    public static final String EXCEPTION_PID = "throwsAnException";

    public static final String FPID = "OpenSearchSource";

    public static MockedAdminPoller poller;

    private static final String REGISTRY_ID = "registry-id";

    private static final String PUBLISHED_LOCATIONS = "published-locations";

    @Mock
    private CatalogFramework catalogFramework;

    @Mock
    private CatalogStore catalogStore1;

    @Mock
    private CatalogStore catalogStore2;

    @Mock
    private CatalogStore catalogStore3;

    private FilterBuilder filterBuilder;

    private Map<String, CatalogStore> catalogStoreMap;

    private ArrayList<String> publishedPlaces;

    private ArrayList<String> destinations;

    private List<Result> results;

    private Metacard metacard1;

    @Before
    public void setup() {
        catalogFramework = mock(CatalogFramework.class);
        catalogStoreMap = new HashMap<>();

        catalogStore1 = mock(CatalogStore.class);
        catalogStore2 = mock(CatalogStore.class);
        catalogStore3 = mock(CatalogStore.class);
        catalogStoreMap.put("destination1", catalogStore1);
        catalogStoreMap.put("destination2", catalogStore2);
        catalogStoreMap.put("destination3", catalogStore3);

        publishedPlaces = new ArrayList<>();
        publishedPlaces.add("destination1");
        publishedPlaces.add("destination3");

        destinations = new ArrayList<>();
        destinations.add("destination1");
        destinations.add("destination2");

        metacard1 = new MetacardImpl();
        metacard1.setAttribute(new AttributeImpl(PUBLISHED_LOCATIONS, publishedPlaces));
        metacard1.setAttribute(new AttributeImpl(REGISTRY_ID, "registry1"));
        results = new ArrayList<>();
        results.add(new ResultImpl(metacard1));

        filterBuilder = new GeotoolsFilterBuilder();

        poller = new AdminPollerTest().new MockedAdminPoller(null,
                catalogFramework,
                filterBuilder,
                catalogStoreMap);
    }

    @Test
    public void testAllSourceInfo() {
        List<Map<String, Object>> sources = poller.allSourceInfo();
        assertThat(sources, notNullValue());
        assertThat(sources.size(), is(2));

        assertThat(sources.get(0), not(hasKey("configurations")));
        assertThat(sources.get(1), hasKey("configurations"));

        // Assert that the password value was changed from "secret" to "password".
        String password =
                (String) ((Map<String, Object>) ((List<Map<String, Object>>) sources.get(1)
                        .get("configurations")).get(0)
                        .get("properties")).get("password");
        assertThat(password, is(equalTo("password")));
    }

    @Test
    public void testSourceStatus() {
        assertThat(poller.sourceStatus(CONFIG_PID), is(true));
        assertThat(poller.sourceStatus(EXCEPTION_PID), is(false));
        assertThat(poller.sourceStatus("FAKE SOURCE"), is(false));
    }

    @Test
    public void testSuccessfulPublish()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {
        when(catalogFramework.query(any())).thenReturn(new QueryResponseImpl(new QueryRequestImpl(
                null), results, 1));
        when(catalogStore1.create(any())).thenReturn(new CreateResponseImpl(new CreateRequestImpl(
                new MetacardImpl()),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));
        when(catalogStore2.create(any())).thenReturn(new CreateResponseImpl(new CreateRequestImpl(
                new MetacardImpl()),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));
        when(catalogStore3.delete(any())).thenReturn(new DeleteResponseImpl(new DeleteRequestImpl(""),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));

        List<Serializable> newPublishedPlaces = poller.updatePublications("mySource", destinations);
        assertThat(newPublishedPlaces, hasItems("destination1", "destination2"));
        newPublishedPlaces.remove("destination1");
        newPublishedPlaces.remove("destination2");
        assertThat(newPublishedPlaces, empty());
    }

    @Test
    public void testUnsuccessfulCreatePublish()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {
        when(catalogFramework.query(any())).thenReturn(new QueryResponseImpl(new QueryRequestImpl(
                null), results, 1));
        when(catalogStore1.create(any())).thenReturn(new CreateResponseImpl(new CreateRequestImpl(
                new MetacardImpl()),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));
        when(catalogStore2.create(any())).thenThrow(new IngestException());
        when(catalogStore2.query(any())).thenReturn(new SourceResponseImpl(new QueryRequestImpl(null),
                new ArrayList<Result>()));
        when(catalogStore3.delete(any())).thenReturn(new DeleteResponseImpl(new DeleteRequestImpl(""),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));

        List<Serializable> newPublishedPlaces = poller.updatePublications("mySource", destinations);
        assertThat(newPublishedPlaces, hasItems("destination1"));
        newPublishedPlaces.remove("destination1");
        assertThat(newPublishedPlaces, empty());
    }

    @Test
    public void testUnsuccessfulDeletePublish()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException,
            IngestException {
        when(catalogFramework.query(any())).thenReturn(new QueryResponseImpl(new QueryRequestImpl(
                null), results, 1));
        when(catalogStore1.create(any())).thenReturn(new CreateResponseImpl(new CreateRequestImpl(
                new MetacardImpl()),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));
        when(catalogStore2.create(any())).thenReturn(new CreateResponseImpl(new CreateRequestImpl(
                new MetacardImpl()),
                new HashMap<String, Serializable>(),
                Arrays.asList(metacard1)));
        when(catalogStore3.delete(any())).thenThrow(new IngestException());
        when(catalogStore3.query(any())).thenReturn(new SourceResponseImpl(new QueryRequestImpl(null),
                Arrays.asList(new ResultImpl(metacard1))));

        List<Serializable> newPublishedPlaces = poller.updatePublications("mySource", destinations);
        assertThat(newPublishedPlaces, hasItems("destination1", "destination2", "destination3"));
    }

    private class MockedAdminPoller extends AdminPollerServiceBean {
        public MockedAdminPoller(ConfigurationAdmin configAdmin, CatalogFramework catalogFramework,
                FilterBuilder filterBuilder, Map<String, CatalogStore> catalogStoreMap) {
            super(configAdmin, catalogFramework, filterBuilder, catalogStoreMap);
        }

        @Override
        protected AdminSourceHelper getHelper() {
            AdminSourceHelper helper = mock(AdminSourceHelper.class);
            try {
                // Mock out the configuration
                Configuration config = mock(Configuration.class);
                when(config.getPid()).thenReturn(CONFIG_PID);
                when(config.getFactoryPid()).thenReturn(FPID);
                Dictionary<String, Object> dict = new Hashtable<>();
                dict.put("service.pid", CONFIG_PID);
                dict.put("service.factoryPid", FPID);
                // Add a password property with the value of "secret".
                dict.put("password", "secret");
                when(config.getProperties()).thenReturn(dict);
                when(helper.getConfigurations(anyMap())).thenReturn(CollectionUtils.asList(config),
                        null);

                // Mock out the sources
                OpenSearchSource source = mock(OpenSearchSource.class);
                when(source.isAvailable()).thenReturn(true);

                OpenSearchSource badSource = mock(OpenSearchSource.class);
                when(badSource.isAvailable()).thenThrow(new RuntimeException());

                //CONFIG_PID, EXCEPTION_PID, FAKE_SOURCE
                when(helper.getConfiguration(any(ConfiguredService.class))).thenReturn(config,
                        config,
                        config);
                when(helper.getSources()).thenReturn(CollectionUtils.asList((Source) source,
                        badSource));

                // Mock out the metatypes
                Map<String, Object> metatype = new HashMap<>();
                // Add a password property to the metatype.
                List<Map<String, Object>> metatypeProperties = new ArrayList<>();
                Map<String, Object> metatypeProperty = new HashMap<>();
                metatypeProperty.put("id", "password");
                metatypeProperty.put("type", AttributeDefinition.PASSWORD);
                metatypeProperties.add(metatypeProperty);
                metatype.put("id", "OpenSearchSource");
                metatype.put("metatype", metatypeProperties);

                Map<String, Object> noConfigMetaType = new HashMap<>();
                noConfigMetaType.put("id", "No Configurations");
                noConfigMetaType.put("metatype", new ArrayList<Map<String, Object>>());

                when(helper.getMetatypes()).thenReturn(CollectionUtils.asList(metatype,
                        noConfigMetaType));
            } catch (Exception e) {

            }

            return helper;
        }
    }
}
