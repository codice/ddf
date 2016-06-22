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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.codice.ddf.registry.api.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.FilterFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;

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


    @Mock
    private RegistryStore registryStore;

    @Before
    public void setUp() throws Exception {
        refreshRegistrySubscriptions = spy(new RefreshRegistrySubscriptions());
        refreshRegistrySubscriptions.setFederationAdminService(federationAdminService);
        refreshRegistrySubscriptions.setRegistryStores(Collections.emptyList());
    }

    @Test
    public void testRefreshRegistrySubscriptionsWhenPollableSourceIdsIAreEmpty() throws Exception {
        refreshRegistrySubscriptions.refreshRegistrySubscriptions();
        verify(federationAdminService, never()).getRegistryMetacards();
    }

    @Test
    public void testCreateRemoteEntries() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.emptyList());
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMetacard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService).addRegistryEntries(Collections.singletonList(remoteMetacard),
                null);
    }

    @Test
    public void testCreateRemoteEntriesSourceUnavailable() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.emptyList());
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMetacard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(false);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService, never()).addRegistryEntries(Collections.singletonList(
                remoteMetacard), null);
    }

    @Test
    public void testCreateRemoteEntriesPullNotAllowed() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.emptyList());
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMetacard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(false);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService, never()).addRegistryEntries(Collections.singletonList(
                remoteMetacard), null);
    }

    @Test
    public void testWriteRemoteUpdates() throws Exception {
        Metacard localMcard = getPopulatedTestRegistryMetacard();
        Metacard remoteMcard =
                getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 1000L);

        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMcard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService).updateRegistryEntry(remoteMcard);
    }

    @Test
    public void testNoUpdatesOnLocal() throws Exception {
        MetacardImpl localMcard = getPopulatedTestRegistryMetacard();
        localMcard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
        Metacard remoteMcard =
                getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 1000L);

        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMcard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test
    public void testNoUpdatesOnOlder() throws Exception {
        MetacardImpl localMcard = getPopulatedTestRegistryMetacard();
        Metacard remoteMcard =
                getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, -5000L);

        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(Collections.singletonList(
                remoteMcard));

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test
    public void testMultipleRemoteSameEntry() throws Exception {
        MetacardImpl remoteMcard1 = getPopulatedTestRegistryMetacard();
        MetacardImpl remoteMcard2 = getPopulatedTestRegistryMetacard();
        List<Metacard> mcards = new ArrayList<>();
        mcards.add(remoteMcard1);
        mcards.add(remoteMcard2);

        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.emptyList());
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(mcards);

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService).addRegistryEntries(Collections.singletonList(remoteMcard1),
                null);
    }

    @Test
    public void testMultipleRemoteNewerEntry() throws Exception {
        MetacardImpl remoteMcard1 = getPopulatedTestRegistryMetacard();
        MetacardImpl remoteMcard2 =
                getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 5000L);
        List<Metacard> mcards = new ArrayList<>();
        mcards.add(remoteMcard1);
        mcards.add(remoteMcard2);

        when(federationAdminService.getRegistryMetacards()).thenReturn(Collections.emptyList());
        when(federationAdminService.getRegistryMetacards(any(Set.class))).thenReturn(mcards);

        refreshRegistrySubscriptions.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistrySubscriptions.refreshRegistrySubscriptions();

        verify(federationAdminService).addRegistryEntries(Collections.singletonList(remoteMcard2),
                null);
    }

    private MetacardImpl getPopulatedTestRegistryMetacard() {
        return getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 0);
    }

    private MetacardImpl getPopulatedTestRegistryMetacard(String id, long dateOffset) {
        MetacardImpl registryMetacard = new MetacardImpl(new RegistryObjectMetacardType());
        registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                id));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED,
                new Date(new Date().getTime() + dateOffset)));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.ID, TEST_METACARD_ID));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, TEST_XML_STRING));
        return registryMetacard;
    }
}
