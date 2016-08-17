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

import java.util.Collections;
import java.util.Date;

import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;

@RunWith(MockitoJUnitRunner.class)
public class RefreshRegistryEntriesTest {

    private static final String TEST_METACARD_ID = "MetacardId";

    private static final String TEST_ID = "TestId";

    private static final String TEST_XML_STRING = "SomeValidStringVersionOfXml";

    @Mock
    private FederationAdminServiceImpl federationAdminService;

    private RefreshRegistryEntries refreshRegistryEntries;

    private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    @Mock
    private RegistryStore registryStore;

    @Before
    public void setUp() throws Exception {
        refreshRegistryEntries = spy(new RefreshRegistryEntries());
        refreshRegistryEntries.setFederationAdminService(federationAdminService);
        refreshRegistryEntries.setRegistryStores(Collections.emptyList());
        refreshRegistryEntries.setFilterBuilder(filterBuilder);
    }

    @Test
    public void testCreateRemoteEntries() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMetacard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);
        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService).addRegistryEntries(Collections.singletonList(remoteMetacard),
                null);
    }

    @Test
    public void testCreateRemoteEntriesSourceUnavailable() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMetacard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(false);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService, never()).addRegistryEntries(Collections.singletonList(
                remoteMetacard), null);
    }

    @Test
    public void testCreateRemoteEntriesPullNotAllowed() throws Exception {
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard();
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.emptyList());
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMetacard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(false);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService, never()).addRegistryEntries(Collections.singletonList(
                remoteMetacard), null);
    }

    @Test
    public void testSubscriptionEntityRemovalNoRemoteEntries() throws Exception {
        Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMetacard));

        SourceResponse response = new SourceResponseImpl(null, Collections.emptyList());
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.getRegistryId()).thenReturn("remoteRegId");
        when(registryStore.isAvailable()).thenReturn(true);
        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService).deleteRegistryEntriesByMetacardIds(Collections.singletonList(
                localMetacard.getId()));
    }

    @Test
    public void testSubscriptionEntityRemoval() throws Exception {
        Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMetacard));
        Metacard remoteMetacard = getPopulatedTestRegistryMetacard("mcardId2",
                "testRegId2",
                0,
                true);
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMetacard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.getRegistryId()).thenReturn("remoteRegId");
        when(registryStore.isAvailable()).thenReturn(true);
        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService).deleteRegistryEntriesByMetacardIds(Collections.singletonList(
                localMetacard.getId()));
    }

    @Test
    public void testSubscriptionEntityRemovalFailedQuery() throws Exception {
        Metacard localMetacard = getPopulatedTestRegistryMetacard("mcardId", "testRegId", 0, true);
        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMetacard));

        when(registryStore.query(any(QueryRequest.class))).thenThrow(new UnsupportedQueryException(
                "query error"));

        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.getRegistryId()).thenReturn("remoteRegId");
        when(registryStore.isAvailable()).thenReturn(true);
        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService,
                never()).deleteRegistryEntriesByMetacardIds(Collections.singletonList(localMetacard.getId()));
    }

    @Test
    public void testWriteRemoteUpdates() throws Exception {
        Metacard localMcard = getPopulatedTestRegistryMetacard(TEST_METACARD_ID,
                RegistryObjectMetacardType.REGISTRY_ID,
                0,
                true);
        Metacard remoteMcard = getPopulatedTestRegistryMetacard("remoteMcardId",
                RegistryObjectMetacardType.REGISTRY_ID,
                1000L);

        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMcard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService).updateRegistryEntry(remoteMcard);
    }

    @Test
    public void testNoUpdatesOnLocal() throws Exception {
        MetacardImpl localMcard = getPopulatedTestRegistryMetacard(TEST_METACARD_ID,
                RegistryObjectMetacardType.REGISTRY_ID,
                0,
                true);
        localMcard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
        Metacard remoteMcard = getPopulatedTestRegistryMetacard("remoteMcardId",
                RegistryObjectMetacardType.REGISTRY_ID,
                1000L);

        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMcard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    @Test
    public void testNoUpdatesOnOlder() throws Exception {
        MetacardImpl localMcard = getPopulatedTestRegistryMetacard(TEST_METACARD_ID,
                RegistryObjectMetacardType.REGISTRY_ID,
                0,
                true);
        Metacard remoteMcard = getPopulatedTestRegistryMetacard("remoteMcardId",
                RegistryObjectMetacardType.REGISTRY_ID,
                -5000L);

        when(federationAdminService.getInternalRegistryMetacards()).thenReturn(Collections.singletonList(
                localMcard));
        SourceResponse response = new SourceResponseImpl(null,
                Collections.singletonList(new ResultImpl(remoteMcard)));
        when(registryStore.query(any(QueryRequest.class))).thenReturn(response);

        refreshRegistryEntries.setRegistryStores(Collections.singletonList(registryStore));
        when(registryStore.isPullAllowed()).thenReturn(true);
        when(registryStore.getId()).thenReturn(TEST_ID);
        when(registryStore.isAvailable()).thenReturn(true);

        refreshRegistryEntries.refreshRegistryEntries();

        verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
    }

    private MetacardImpl getPopulatedTestRegistryMetacard() {
        return getPopulatedTestRegistryMetacard(RegistryObjectMetacardType.REGISTRY_ID, 0);
    }

    private MetacardImpl getPopulatedTestRegistryMetacard(String id, long dateOffset) {
        return getPopulatedTestRegistryMetacard(TEST_METACARD_ID, id, dateOffset, false);
    }

    private MetacardImpl getPopulatedTestRegistryMetacard(String id, String regId,
            long dateOffset) {
        return getPopulatedTestRegistryMetacard(id, regId, dateOffset, false);
    }

    private MetacardImpl getPopulatedTestRegistryMetacard(String id, String regId, long dateOffset,
            boolean internal) {
        return getPopulatedTestRegistryMetacard(id, regId, dateOffset, internal, "remoteMcardId");
    }

    private MetacardImpl getPopulatedTestRegistryMetacard(String id, String regId, long dateOffset,
            boolean internal, String remoteMcardId) {
        MetacardImpl registryMetacard = new MetacardImpl(new RegistryObjectMetacardType());
        registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID,
                regId));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED,
                new Date(new Date().getTime() + dateOffset)));
        if (internal) {
            registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_METACARD_ID,
                    remoteMcardId));
            registryMetacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REMOTE_REGISTRY_ID,
                    "remoteRegId"));
            registryMetacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                    Collections.singletonList(RegistryConstants.REGISTRY_TAG_INTERNAL)));
        } else {
            registryMetacard.setAttribute(new AttributeImpl(Metacard.TAGS,
                    Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
        }
        registryMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        registryMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, TEST_XML_STRING));
        return registryMetacard;
    }

}
