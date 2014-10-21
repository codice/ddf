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
package org.codice.ddf.ui.searchui.query.controller;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test cases for {@link org.codice.ddf.ui.searchui.query.controller.NotificationController}
 */
public class SearchControllerTest {
    private SearchController searchController;

    // NOTE: The ServerSession ID == The ClientSession ID
    private static final String MOCK_SESSION_ID = "1234-5678-9012-3456";

    private ServerSession mockServerSession;

    private ServerMessage mockServerMessage;

    private HashMap<String, Object> testEventProperties;

    private static final Date TIMESTAMP = new Date();

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchControllerTest.class);

    @Before
    public void setUp() throws Exception {
        searchController = new SearchController(createFramework());

        mockServerSession = mock(ServerSession.class);

        mockServerMessage = mock(ServerMessage.class);

        when(mockServerSession.getId()).thenReturn(MOCK_SESSION_ID);
    }

    @Test
    public void testMetacardTypeValues() {

        final String ID = "id";
        Set<String> srcIds = new HashSet<String>();
        srcIds.add(ID);

        BayeuxServer bayeuxServer = mock(BayeuxServer.class);
        ServerChannel channel = mock(ServerChannel.class);
        ArgumentCaptor<ServerMessage.Mutable> reply =
                ArgumentCaptor.forClass(ServerMessage.Mutable.class);

        when(bayeuxServer.getChannel(any(String.class))).thenReturn(channel);

        SearchRequest request = new SearchRequest(srcIds, mock(Query.class), ID);

        searchController.setBayeuxServer(bayeuxServer);
        searchController.executeQuery(request, mockServerSession, null);

        verify(channel, timeout(1000).only()).publish(any(ServerSession.class),
                reply.capture(), anyString());
        assertThat(reply.getValue(), is(not(nullValue())));
        assertThat(reply.getValue().get("types"), is(not(nullValue())));
        assertThat(reply.getValue().get("types"), instanceOf(Map.class));

        Map<String, Object> types = (Map) reply.getValue().get("types");

        assertThat(types.get("ddf.metacard"), is(not(nullValue())));
        assertThat(types.get("ddf.metacard"), instanceOf(Map.class));

        Map<String, String> typeInfo = (Map) types.get("ddf.metacard");

        assertThat(typeInfo.get("effective"), is("DATE"));
        assertThat(typeInfo.get("modified"), is("DATE"));
        assertThat(typeInfo.get("created"), is("DATE"));
        assertThat(typeInfo.get("expiration"), is("DATE"));
        assertThat(typeInfo.get("id"), is("STRING"));
        assertThat(typeInfo.get("title"), is("STRING"));
        assertThat(typeInfo.get("metadata-content-type"), is("STRING"));
        assertThat(typeInfo.get("metadata-content-type-version"), is("STRING"));
        assertThat(typeInfo.get("metadata-target-namespace"), is("STRING"));
        assertThat(typeInfo.get("resource-uri"), is("STRING"));
        assertThat(typeInfo.get("resource-size"), is("STRING"));
        assertThat(typeInfo.get("metadata"), is("XML"));
        assertThat(typeInfo.get("location"), is("GEOMETRY"));

    }

    private CatalogFramework createFramework() {
        final long COUNT = 2;

        CatalogFramework framework = mock(CatalogFramework.class);
        QueryResponse response = mock(QueryResponse.class);
        List<Result> results = new ArrayList<Result>();

        for(int i = 0; i < COUNT; i++) {
            Result result = mock(Result.class);

            MetacardImpl metacard = new MetacardImpl();
            metacard.setId("Metacard_" + i);
            metacard.setTitle("Metacard " + i);
            metacard.setLocation("POINT(" + i + " " + i + ")");
            metacard.setType(BasicTypes.BASIC_METACARD);
            metacard.setCreatedDate(TIMESTAMP);
            metacard.setEffectiveDate(TIMESTAMP);
            metacard.setExpirationDate(TIMESTAMP);
            metacard.setModifiedDate(TIMESTAMP);
            metacard.setContentTypeName("TEST");
            metacard.setContentTypeVersion("1.0");
            metacard.setTargetNamespace(URI.create(getClass().getPackage().getName()));

            when(result.getDistanceInMeters()).thenReturn(100.0 * i);
            when(result.getRelevanceScore()).thenReturn(100.0 * (COUNT - i) / COUNT);
            when(result.getMetacard()).thenReturn(metacard);

            results.add(result);
        }

        when(response.getHits()).thenReturn(COUNT);
        when(response.getResults()).thenReturn(results);

        try {
            when(framework.query(any(QueryRequest.class))).thenReturn(response);
        } catch (UnsupportedQueryException e) {
            LOGGER.debug("Error querying framework", e);
        } catch (SourceUnavailableException e) {
            LOGGER.debug("Error querying framework", e);
        } catch (FederationException e) {
            LOGGER.debug("Error querying framework", e);
        }
        return framework;
    }

}
