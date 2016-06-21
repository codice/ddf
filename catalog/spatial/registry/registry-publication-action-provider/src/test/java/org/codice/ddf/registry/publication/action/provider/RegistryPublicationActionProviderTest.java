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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.publication.manager.RegistryPublicationManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import ddf.action.Action;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.source.Source;

@RunWith(MockitoJUnitRunner.class)
public class RegistryPublicationActionProviderTest {

    @Mock
    private ConfigurationAdmin configAdmin;

    @Mock
    private Configuration configuration;

    @Mock
    private Source source;

    @Mock
    private RegistryStore store;

    @Mock
    private RegistryPublicationManager publicationManager;

    private RegistryPublicationActionProvider publicationActionProvider;

    @Before
    public void setup() throws Exception {
        publicationActionProvider = new RegistryPublicationActionProvider();
        publicationActionProvider.setConfigAdmin(configAdmin);
        publicationActionProvider.setProviderId("catalog.source.operation.registry");
        publicationActionProvider.setRegistryStores(Collections.singletonList(store));
        publicationActionProvider.setRegistryPublicationManager(publicationManager);

        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                configuration});

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
    public void testCanHandleRegistryMetacardWithNullRegistryIdAttribute() throws Exception {
        assertThat(publicationActionProvider.canHandle(getRegistryMetacard(null)), is(false));
    }

    @Test
    public void testCanHandleRegistryMetacardWithBlankRegistryIdAttribute() throws Exception {
        assertThat(publicationActionProvider.canHandle(getRegistryMetacard("  ")), is(false));
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

        Map<String, List<String>> publications = new HashMap<>();
        publications.put("regId1", new ArrayList<>());
        doReturn(publications).when(publicationManager)
                .getPublications();

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

        Map<String, List<String>> publications = new HashMap<>();
        publications.put("regId1", Collections.singletonList("store1"));
        doReturn(publications).when(publicationManager)
                .getPublications();
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
        if (StringUtils.isNotEmpty(regId)) {
            mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, regId);
        }
        return mcard;
    }

}
