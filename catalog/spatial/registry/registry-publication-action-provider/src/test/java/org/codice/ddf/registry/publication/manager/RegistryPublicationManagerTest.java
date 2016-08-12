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
package org.codice.ddf.registry.publication.manager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;

@RunWith(MockitoJUnitRunner.class)
public class RegistryPublicationManagerTest {

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private static final String DEFAULT_REGISTRY_ID = "defaultRegistryId";

    @Mock
    private FederationAdminService federationAdmin;

    @Mock
    private ScheduledExecutorService executorService;

    private RegistryPublicationManager publicationManager;

    @Before
    public void setup() throws Exception {
        publicationManager = new RegistryPublicationManager();
        publicationManager.setFederationAdminService(federationAdmin);
        publicationManager.setExecutorService(executorService);
    }

    @Test
    public void testInit() throws Exception {
        Metacard mcard1 = getRegistryMetacard("regId1");
        Metacard mcard2 = getRegistryMetacard("regId2");
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        mcard1.setAttribute(new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                locations));
        List<Metacard> metacardList = new ArrayList<>();
        metacardList.add(mcard1);
        metacardList.add(mcard2);
        when(federationAdmin.getRegistryMetacards()).thenReturn(metacardList);

        publicationManager.init();

        Map<String, List<String>> publications = publicationManager.getPublications();
        assertThat(publications.size(), is(2));
        assertThat(publications.get("regId1"), equalTo(locations));
        assertThat(publications.get("regId2")
                .size(), is(0));
    }

    @Test
    public void testInitFederationAdminException() throws Exception {
        when(federationAdmin.getRegistryMetacards())
                .thenThrow(new FederationAdminException("Test error"));
        publicationManager.init();
        verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
        publicationManager.destroy();
        verify(executorService).shutdown();
    }

    @Test
    public void testInitWithNullMetacard() throws Exception {
        Metacard mcard1 = getRegistryMetacard("regId1");
        Metacard mcard2 = getRegistryMetacard(null);
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        mcard1.setAttribute(new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                locations));
        List<Metacard> metacardList = new ArrayList<>();
        metacardList.add(mcard1);
        metacardList.add(mcard2);
        when(federationAdmin.getRegistryMetacards()).thenReturn(metacardList);

        publicationManager.init();

        Map<String, List<String>> publications = publicationManager.getPublications();
        assertThat(publications.size(), is(1));
        assertThat(publications.get("regId1"), equalTo(locations));
    }

    @Test
    public void testInitWithBlankMetacard() throws Exception {
        Metacard mcard1 = getRegistryMetacard("regId1");
        Metacard mcard2 = getRegistryMetacard("");
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        mcard1.setAttribute(new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                locations));
        List<Metacard> metacardList = new ArrayList<>();
        metacardList.add(mcard1);
        metacardList.add(mcard2);
        when(federationAdmin.getRegistryMetacards()).thenReturn(metacardList);

        publicationManager.init();

        Map<String, List<String>> publications = publicationManager.getPublications();
        assertThat(publications.size(), is(1));
        assertThat(publications.get("regId1"), equalTo(locations));
    }

    @Test
    public void testHandleEventNonRegistryMcard() throws Exception {
        Metacard mcard = new MetacardImpl();
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put(METACARD_PROPERTY, mcard);
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventNoMcard() throws Exception {

        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventNoRegistryId() throws Exception {
        Metacard mcard = getRegistryMetacard(null);
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put(METACARD_PROPERTY, mcard);
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventBlankRegistryId() throws Exception {
        Metacard mcard = getRegistryMetacard("");
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put(METACARD_PROPERTY, mcard);
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventCreate() throws Exception {
        Event event = getRegistryEvent(CREATED_TOPIC);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(1));
    }

    @Test
    public void testHandleEventCreateExistingPublications() throws Exception {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        Event event = getRegistryEvent(CREATED_TOPIC, locations);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(1));
        assertThat(publicationManager.getPublications()
                .get(DEFAULT_REGISTRY_ID)
                .size(), is(1));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPublicationsMapNotModifiable() throws Exception {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        Event event = getRegistryEvent(CREATED_TOPIC, locations);
        publicationManager.handleEvent(event);
        Map<String, List<String>> map = publicationManager.getPublications();
        map.put("SomeKey", Collections.singletonList("ShouldNotWork"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetPublicationsMapValueListNotModifiable() throws Exception {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        Event event = getRegistryEvent(CREATED_TOPIC, locations);
        publicationManager.handleEvent(event);
        Map<String, List<String>> map = publicationManager.getPublications();
        List<String> unmodifiable = map.get(DEFAULT_REGISTRY_ID);
        unmodifiable.add("ShouldNotWork");
    }

    @Test
    public void testHandleEventUpdate() throws Exception {
        Event event = getRegistryEvent(UPDATED_TOPIC);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(1));
    }

    @Test
    public void testHandleEventDelete() throws Exception {
        publicationManager.handleEvent(getRegistryEvent(CREATED_TOPIC));
        assertThat(publicationManager.getPublications()
                .size(), is(1));
        Event event = getRegistryEvent(DELETED_TOPIC);
        publicationManager.handleEvent(event);
        assertThat(publicationManager.getPublications()
                .size(), is(0));
    }

    private Metacard getRegistryMetacard(String regId) {
        MetacardImpl mcard = new MetacardImpl(new RegistryObjectMetacardType());
        mcard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        if (StringUtils.isNotEmpty(regId)) {
            mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, regId);
        }
        return mcard;
    }

    private Event getRegistryEvent(String topic) {
        return getRegistryEvent(topic, null);
    }

    private Event getRegistryEvent(String topic, ArrayList<String> locations) {
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Metacard mcard = getRegistryMetacard(DEFAULT_REGISTRY_ID);
        if (locations != null) {
            mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                    locations));
        }
        eventProperties.put(METACARD_PROPERTY, mcard);
        return new Event(topic, eventProperties);
    }
}