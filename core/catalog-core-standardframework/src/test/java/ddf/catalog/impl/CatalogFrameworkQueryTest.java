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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.geotools.filter.FilterFactoryImpl;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.FilterFactory;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.CachedSource;
import ddf.catalog.util.impl.SourcePoller;

public class CatalogFrameworkQueryTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkQueryTest.class);

    private CatalogFrameworkImpl framework;

    @Before
    public void initFramework() {
        MockMemoryProvider provider = new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF",
                new HashSet<ContentType>(), true, new Date());

        // Mock register the provider in the container
        // Mock the source poller
        SourcePoller mockPoller = mock(SourcePoller.class);
        CachedSource source = mock(CachedSource.class);
        when(source.isAvailable()).thenReturn(Boolean.TRUE);        
        when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(source);
        ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<PostIngestPlugin>();
        framework = new CatalogFrameworkImpl(Collections.singletonList((CatalogProvider) provider),
                null, new ArrayList<PreIngestPlugin>(), postIngestPlugins,
                new ArrayList<PreQueryPlugin>(), new ArrayList<PostQueryPlugin>(),
                new ArrayList<PreResourcePlugin>(), new ArrayList<PostResourcePlugin>(),
                new ArrayList<ConnectedSource>(), new ArrayList<FederatedSource>(),
                new ArrayList<ResourceReader>(), new MockFederationStrategy(),
                mock(QueryResponsePostProcessor.class), null,
                mockPoller, null, null, null);
        framework.bind(provider);
    }

    @Test
    public void testAfterQuery() {
        Calendar afterCal = Calendar.getInstance();
        Calendar card1Exp = Calendar.getInstance();
        card1Exp.add(Calendar.YEAR, 1);
        Calendar card2Exp = Calendar.getInstance();
        card2Exp.add(Calendar.YEAR, 3);

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setId(null);
        newCard1.setExpirationDate(card1Exp.getTime());
        metacards.add(newCard1);
        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setId(null);
        newCard2.setExpirationDate(card2Exp.getTime());
        metacards.add(newCard2);
        String mcId1 = null;
        String mcId2 = null;

        CreateResponse createResponse = null;
        try {
            createResponse = framework.create(new CreateRequestImpl(metacards, null));
        } catch (IngestException e1) {
            fail();
        } catch (SourceUnavailableException e1) {
            fail();
        }
        assertEquals(createResponse.getCreatedMetacards().size(), metacards.size());
        for (Metacard curCard : createResponse.getCreatedMetacards()) {
            if (curCard.getExpirationDate().equals(card1Exp.getTime())) {
                mcId1 = curCard.getId();
            } else {
                mcId2 = curCard.getId();
            }
            assertNotNull(curCard.getId());
        }

        FilterFactory filterFactory = new FilterFactoryImpl();
        Instant afterInstant = new DefaultInstant(new DefaultPosition(afterCal.getTime()));
        QueryImpl query = new QueryImpl(filterFactory.after(
                filterFactory.property(Metacard.EXPIRATION), filterFactory.literal(afterInstant)));
        QueryRequest queryReq = new QueryRequestImpl(query, false);

        try {
            QueryResponse response = framework.query(queryReq);
            LOGGER.info("Response:{}", response);
            assertEquals("Expecting return 2 results.", 2, response.getHits());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure!!!", e);
            fail();
        } catch (FederationException e) {
            fail();
        }
        afterInstant = new DefaultInstant(new DefaultPosition(card1Exp.getTime()));
        query = new QueryImpl(filterFactory.after(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(afterInstant)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("After filter should return 1 result", 1, response.getHits());
            assertEquals("After filter should return metacard[" + mcId2 + "]", mcId2, response
                    .getResults().get(0).getMetacard().getId());
        } catch (UnsupportedQueryException e) {
            fail();
        } catch (FederationException e) {
            fail();
        }

        afterInstant = new DefaultInstant(new DefaultPosition(card2Exp.getTime()));
        query = new QueryImpl(filterFactory.after(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(afterInstant)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("After filter should return 0 results.", 0, response.getHits());
        } catch (UnsupportedQueryException e) {
            fail();
        } catch (FederationException e) {
            fail();
        }
    }

    @Test
    public void testBeforeQuery() {
        Calendar beforeCal = Calendar.getInstance();
        beforeCal.add(Calendar.YEAR, 4);
        Calendar card1Exp = Calendar.getInstance();
        card1Exp.add(Calendar.YEAR, 1);
        Calendar card2Exp = Calendar.getInstance();
        card2Exp.add(Calendar.YEAR, 3);

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setId(null);
        newCard1.setExpirationDate(card1Exp.getTime());
        metacards.add(newCard1);
        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setId(null);
        newCard2.setExpirationDate(card2Exp.getTime());
        metacards.add(newCard2);
        String mcId1 = null;
        String mcId2 = null;

        CreateResponse createResponse = null;
        try {
            createResponse = framework.create(new CreateRequestImpl(metacards, null));
        } catch (IngestException e1) {
            fail();
        } catch (SourceUnavailableException e1) {
            fail();
        }
        assertEquals(createResponse.getCreatedMetacards().size(), metacards.size());
        for (Metacard curCard : createResponse.getCreatedMetacards()) {
            if (curCard.getExpirationDate().equals(card1Exp.getTime())) {
                mcId1 = curCard.getId();
            } else {
                mcId2 = curCard.getId();
            }
            assertNotNull(curCard.getId());
        }

        FilterFactory filterFactory = new FilterFactoryImpl();
        Instant beforeInstant = new DefaultInstant(new DefaultPosition(beforeCal.getTime()));
        QueryImpl query = new QueryImpl(filterFactory.before(
                filterFactory.property(Metacard.EXPIRATION), filterFactory.literal(beforeInstant)));
        QueryRequest queryReq = new QueryRequestImpl(query, false);

        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Expecting return 2 results.", 2, response.getHits());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }
        beforeInstant = new DefaultInstant(new DefaultPosition(card2Exp.getTime()));
        query = new QueryImpl(filterFactory.before(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(beforeInstant)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Before filter should return 1 result", 1, response.getHits());
            assertEquals("Before filter should return metacard[" + mcId1 + "]", mcId1, response
                    .getResults().get(0).getMetacard().getId());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }

        beforeInstant = new DefaultInstant(new DefaultPosition(card1Exp.getTime()));
        query = new QueryImpl(filterFactory.before(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(beforeInstant)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Before filter should return 0 results.", 0, response.getHits());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }
    }

    @Test
    public void testBeginsQuery() {

        Calendar beginsStart = Calendar.getInstance();
        Calendar card1Exp = Calendar.getInstance();
        if (beginsStart.equals(card1Exp)) {
            card1Exp.add(Calendar.MILLISECOND, 1);
        }
        Calendar card2Exp = Calendar.getInstance();
        card2Exp.add(Calendar.YEAR, 3);
        Calendar beginsEnd = Calendar.getInstance();
        beginsEnd.add(Calendar.YEAR, 4);

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setId(null);
        newCard1.setExpirationDate(card1Exp.getTime());
        metacards.add(newCard1);
        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setId(null);
        newCard2.setExpirationDate(card2Exp.getTime());
        metacards.add(newCard2);
        String mcId1 = null;
        String mcId2 = null;

        CreateResponse createResponse = null;
        try {
            createResponse = framework.create(new CreateRequestImpl(metacards, null));
        } catch (IngestException e1) {
            LOGGER.error("Failure", e1);
            fail();
        } catch (SourceUnavailableException e1) {
            LOGGER.error("Failure", e1);
            fail();
        }
        assertEquals(createResponse.getCreatedMetacards().size(), metacards.size());
        for (Metacard curCard : createResponse.getCreatedMetacards()) {
            if (curCard.getExpirationDate().equals(card1Exp.getTime())) {
                mcId1 = curCard.getId();
            } else {
                mcId2 = curCard.getId();
            }
            assertNotNull(curCard.getId());
        }

        FilterFactory filterFactory = new FilterFactoryImpl();
        Period beginsPeriod = new DefaultPeriod(new DefaultInstant(new DefaultPosition(
                beginsStart.getTime())), new DefaultInstant(
                new DefaultPosition(beginsEnd.getTime())));
        QueryImpl query = new QueryImpl(filterFactory.begins(
                filterFactory.property(Metacard.EXPIRATION), filterFactory.literal(beginsPeriod)));
        QueryRequest queryReq = new QueryRequestImpl(query, false);

        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Expecting return 0 results.", 0, response.getHits());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }

        beginsPeriod = new DefaultPeriod(
                new DefaultInstant(new DefaultPosition(card1Exp.getTime())), new DefaultInstant(
                        new DefaultPosition(beginsEnd.getTime())));
        query = new QueryImpl(filterFactory.begins(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(beginsPeriod)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Begins filter should return 1 result", 1, response.getHits());
            assertEquals("Begins filter should return metacard[" + mcId1 + "]", mcId1, response
                    .getResults().get(0).getMetacard().getId());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }

        beginsPeriod = new DefaultPeriod(
                new DefaultInstant(new DefaultPosition(card2Exp.getTime())), new DefaultInstant(
                        new DefaultPosition(beginsEnd.getTime())));
        query = new QueryImpl(filterFactory.begins(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(beginsPeriod)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Begins filter should return 1 result", 1, response.getHits());
            assertEquals("Begins filter should return metacard[" + mcId2 + "]", mcId2, response
                    .getResults().get(0).getMetacard().getId());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }
    }

    @Test
    public void testDuringQuery() {

        List<Metacard> metacards = new ArrayList<Metacard>();

        MetacardImpl newCard1 = new MetacardImpl();
        newCard1.setId(null);
        Calendar duringStart = Calendar.getInstance();
        Calendar card1Exp = Calendar.getInstance();
        card1Exp.add(Calendar.YEAR, 1);
        Calendar duringEnd1 = Calendar.getInstance();
        duringEnd1.add(Calendar.YEAR, 2);
        Calendar card2Exp = Calendar.getInstance();
        card2Exp.add(Calendar.YEAR, 3);
        Calendar duringEnd2 = Calendar.getInstance();
        duringEnd2.add(Calendar.YEAR, 4);

        newCard1.setExpirationDate(card1Exp.getTime());
        metacards.add(newCard1);
        MetacardImpl newCard2 = new MetacardImpl();
        newCard2.setId(null);
        newCard2.setExpirationDate(card2Exp.getTime());
        metacards.add(newCard2);
        String mcId1 = null;
        String mcId2 = null;

        CreateResponse createResponse = null;
        try {
            createResponse = framework.create(new CreateRequestImpl(metacards, null));
        } catch (IngestException e1) {
            LOGGER.error("Failure", e1);
            fail();
        } catch (SourceUnavailableException e1) {
            LOGGER.error("Failure", e1);
            fail();
        }
        assertEquals(createResponse.getCreatedMetacards().size(), metacards.size());
        for (Metacard curCard : createResponse.getCreatedMetacards()) {
            if (curCard.getExpirationDate().equals(card1Exp.getTime())) {
                mcId1 = curCard.getId();
            } else {
                mcId2 = curCard.getId();
            }
            assertNotNull(curCard.getId());
        }

        FilterFactory filterFactory = new FilterFactoryImpl();
        Period duringPeriod = new DefaultPeriod(new DefaultInstant(new DefaultPosition(
                duringStart.getTime())), new DefaultInstant(new DefaultPosition(
                duringEnd1.getTime())));
        QueryImpl query = new QueryImpl(filterFactory.during(
                filterFactory.property(Metacard.EXPIRATION), filterFactory.literal(duringPeriod)));
        QueryRequest queryReq = new QueryRequestImpl(query, false);

        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("Expecting return 1 result.", 1, response.getHits());
            assertEquals("During filter should return metacard[" + mcId1 + "]", mcId1, response
                    .getResults().get(0).getMetacard().getId());

        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }

        duringPeriod = new DefaultPeriod(
                new DefaultInstant(new DefaultPosition(card1Exp.getTime())), new DefaultInstant(
                        new DefaultPosition(duringEnd2.getTime())));
        query = new QueryImpl(filterFactory.during(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(duringPeriod)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("During filter should return 1 result", 1, response.getHits());
            assertEquals("During filter should return metacard[" + mcId2 + "]", mcId2, response
                    .getResults().get(0).getMetacard().getId());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }

        duringPeriod = new DefaultPeriod(new DefaultInstant(new DefaultPosition(
                duringStart.getTime())), new DefaultInstant(new DefaultPosition(
                duringEnd2.getTime())));
        query = new QueryImpl(filterFactory.during(filterFactory.property(Metacard.EXPIRATION),
                filterFactory.literal(duringPeriod)));
        queryReq = new QueryRequestImpl(query, false);
        try {
            QueryResponse response = framework.query(queryReq);
            assertEquals("During filter should return 2 result", 2, response.getHits());
        } catch (UnsupportedQueryException e) {
            LOGGER.error("Failure", e);
            fail();
        } catch (FederationException e) {
            LOGGER.error("Failure", e);
            fail();
        }
    }

}
