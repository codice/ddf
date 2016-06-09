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
package org.codice.ddf.registry.publication.action.provider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;

import ddf.action.Action;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.source.Source;

public class RegistryPublicationActionProviderTest {

    private static final String METACARD_PROPERTY = "ddf.catalog.event.metacard";

    private static final String CREATED_TOPIC = "ddf/catalog/event/CREATED";

    private static final String UPDATED_TOPIC = "ddf/catalog/event/UPDATED";

    private static final String DELETED_TOPIC = "ddf/catalog/event/DELETED";

    private ConfigurationAdmin configAdmin;

    private Configuration configuration;

    private Source source;

    private FederationAdminService federationAdmin;

    private RegistryStore store;

    private RegistryPublicationActionProvider publicationActionProvider;

    @Before
    public void setup() throws Exception {
        configAdmin = mock(ConfigurationAdmin.class);
        federationAdmin = mock(FederationAdminService.class);
        configuration = mock(Configuration.class);
        source = mock(Source.class);
        store = mock(RegistryStore.class);

        publicationActionProvider = new RegistryPublicationActionProvider();
        publicationActionProvider.setConfigAdmin(configAdmin);
        publicationActionProvider.setFederationAdminService(federationAdmin);
        publicationActionProvider.setProviderId("catalog.source.operation.registry");
        publicationActionProvider.setRegistryStores(Collections.singletonList(store));

        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});

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

        publicationActionProvider.init();

        Map<String, List<String>> publications = publicationActionProvider.getPublications();
        assertThat(publications.size(), is(2));
        assertThat(publications.get("regId1"), equalTo(locations));
        assertThat(publications.get("regId2")
                .size(), is(0));
    }

    @Test
    public void testHandleEventNonRegistryMcard() throws Exception {
        Metacard mcard = new MetacardImpl();
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put(METACARD_PROPERTY, mcard);
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventNoMcard() throws Exception {

        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Event event = new Event(CREATED_TOPIC, eventProperties);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(0));
    }

    @Test
    public void testHandleEventCreate() throws Exception {
        Event event = getRegistryEvent(CREATED_TOPIC);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(1));
    }

    @Test
    public void testHandleEventCreateExistingPublications() throws Exception {
        ArrayList<String> locations = new ArrayList<>();
        locations.add("location1");
        Event event = getRegistryEvent(CREATED_TOPIC, locations);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(1));
        assertThat(publicationActionProvider.getPublications()
                .get("regId1")
                .size(), is(1));
    }

    @Test
    public void testHandleEventUpdate() throws Exception {
        Event event = getRegistryEvent(UPDATED_TOPIC);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(1));
    }

    @Test
    public void testHandleEventDelete() throws Exception {
        publicationActionProvider.handleEvent(getRegistryEvent(CREATED_TOPIC));
        assertThat(publicationActionProvider.getPublications()
                .size(), is(1));
        Event event = getRegistryEvent(DELETED_TOPIC);
        publicationActionProvider.handleEvent(event);
        assertThat(publicationActionProvider.getPublications()
                .size(), is(0));
    }

    @Test
    public void testCanHandleNull() throws Exception {
        assertThat(publicationActionProvider.canHandle(null), is(false));
    }

    @Test
    public void testCanHandleNonRegistryMetacard() throws Exception {
        assertThat(publicationActionProvider.canHandle(new MetacardImpl()), is(false));
    }

    @Test
    public void testCanHandleRegistryMetacard() throws Exception {
        assertThat(publicationActionProvider.canHandle(getRegistryMetacard("regId1")), is(true));
    }

    @Test
    public void testCanHandleNonRegistrySource() throws Exception {
        when(source.getId()).thenReturn("someId");
        Dictionary<String, Object> properties = new Hashtable<>();
        when(configuration.getProperties()).thenReturn(properties);
        assertThat(publicationActionProvider.canHandle(source), is(false));
    }

    @Test
    public void testCanHandleRegistrySource() throws Exception {
        when(source.getId()).thenReturn("regId1");
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(RegistryObjectMetacardType.REGISTRY_ID, "regId1");
        when(configuration.getProperties()).thenReturn(properties);
        assertThat(publicationActionProvider.canHandle(source), is(true));
    }

    @Test
    public void testCanHandleNonRegistryConfig() throws Exception {
        Dictionary<String, Object> properties = new Hashtable<>();
        when(configuration.getProperties()).thenReturn(properties);
        assertThat(publicationActionProvider.canHandle(configuration), is(false));
    }

    @Test
    public void testCanHandleRegistryConfig() throws Exception {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(RegistryObjectMetacardType.REGISTRY_ID, "regId1");
        when(configuration.getProperties()).thenReturn(properties);
        assertThat(publicationActionProvider.canHandle(configuration), is(true));
    }

    @Test
    public void testGetActionMcardBadSubject() throws Exception {
        List<Action> actions = publicationActionProvider.getActions(new MetacardImpl());
        assertThat(actions.size(), is(0));
    }

    @Test
    public void testGetActionMcardForSameRegistry() throws Exception {
        when(store.isPushAllowed()).thenReturn(true);
        when(store.getId()).thenReturn("store1");
        when(store.getRegistryId()).thenReturn("regId1");
        List<Action> actions = publicationActionProvider.getActions(getRegistryMetacard("regId1"));
        assertThat(actions.size(), is(0));
    }

    @Test
    public void testGetActionMcardForNoPushRegistry() throws Exception {
        when(store.isPushAllowed()).thenReturn(false);
        when(store.getId()).thenReturn("store1");
        when(store.getRegistryId()).thenReturn("regId2");
        List<Action> actions = publicationActionProvider.getActions(getRegistryMetacard("regId1"));
        assertThat(actions.size(), is(0));
    }

    @Test
    public void testGetActionMcardPublish() throws Exception {
        when(store.isPushAllowed()).thenReturn(true);
        when(store.getId()).thenReturn("store1");
        when(store.getRegistryId()).thenReturn("regId2");
        publicationActionProvider.getPublications()
                .put("regId1", new ArrayList<>());
        List<Action> actions = publicationActionProvider.getActions(getRegistryMetacard("regId1"));
        assertThat(actions.size(), is(1));
        assertThat(actions.get(0)
                .getId(), equalTo("catalog.source.operation.registry.publish.HTTP_POST"));
        assertThat(actions.get(0)
                        .getUrl()
                        .toString(),
                equalTo(SystemBaseUrl.constructUrl("registries/regId1/publication/store1", true)));
    }

    @Test
    public void testGetActionMcardUnpublish() throws Exception {
        when(store.isPushAllowed()).thenReturn(true);
        when(store.getId()).thenReturn("store1");
        when(store.getRegistryId()).thenReturn("regId2");
        publicationActionProvider.getPublications()
                .put("regId1", Collections.singletonList("store1"));
        List<Action> actions = publicationActionProvider.getActions(getRegistryMetacard("regId1"));
        assertThat(actions.size(), is(1));
        assertThat(actions.get(0)
                .getId(), equalTo("catalog.source.operation.registry.unpublish.HTTP_DELETE"));
        assertThat(actions.get(0)
                        .getUrl()
                        .toString(),
                equalTo(SystemBaseUrl.constructUrl("registries/regId1/publication/store1", true)));
    }

    private Metacard getRegistryMetacard(String regId) {
        MetacardImpl mcard = new MetacardImpl(new RegistryObjectMetacardType());
        mcard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, regId);
        return mcard;
    }

    private Event getRegistryEvent(String topic) {
        return getRegistryEvent(topic, null);
    }

    private Event getRegistryEvent(String topic, ArrayList<String> locations) {
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Metacard mcard = getRegistryMetacard("regId1");
        if (locations != null) {
            mcard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
                    locations));
        }
        eventProperties.put(METACARD_PROPERTY, mcard);
        return new Event(topic, eventProperties);
    }

}
