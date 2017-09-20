/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.registry.federationadmin.service.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

@RunWith(MockitoJUnitRunner.class)
public class RegistryPublicationServiceImplTest {

  private FederationAdminService federationAdminService;

  private RegistryPublicationServiceImpl registryPublicationService;

  private MetacardImpl metacard;

  @Mock private BundleContext bundleContext;

  @Mock private ServiceReference serviceReference;

  @Mock private RegistryStore registryStore;

  private static final String REGISTRY_STORE_REGISTRY_ID = "registryStoreRegistryId";

  private static final String REGISTRY_STORE_ID = "registryStoreId";

  @Before
  public void setup() {
    federationAdminService = mock(FederationAdminService.class);
    registryPublicationService = Mockito.spy(new RegistryPublicationServiceImpl());
    registryPublicationService.setFederationAdminService(federationAdminService);

    metacard = new MetacardImpl(new RegistryObjectMetacardType());
    metacard.setId("mcardId");
    metacard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));

    when(registryPublicationService.getBundleContext()).thenReturn(bundleContext);
    when(bundleContext.getService(serviceReference)).thenReturn(registryStore);

    doReturn(REGISTRY_STORE_REGISTRY_ID).when(registryStore).getRegistryId();
    doReturn(REGISTRY_STORE_ID).when(registryStore).getId();
    registryPublicationService.bindRegistryStore(serviceReference);
  }

  @Test(expected = FederationAdminException.class)
  public void testPublishInvalidRegistryId() throws Exception {
    when(federationAdminService.getRegistryMetacardsByRegistryIds(any(List.class)))
        .thenReturn(Collections.emptyList());
    registryPublicationService.publish(null, REGISTRY_STORE_REGISTRY_ID);
  }

  @Test
  public void testPublishAlreadyPublished() throws Exception {
    String publishThisRegistryId = "publishThisRegistryId";
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);

    Attribute publishedLocationsAttribute =
        new AttributeImpl(
            RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
            Collections.singletonList(REGISTRY_STORE_REGISTRY_ID));
    metacard.setAttribute(publishedLocationsAttribute);
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));

    when(federationAdminService.getRegistryMetacardsByRegistryIds(any(List.class)))
        .thenReturn(Collections.singletonList(metacard));

    registryPublicationService.publish(publishThisRegistryId, REGISTRY_STORE_REGISTRY_ID);
    verify(federationAdminService, never())
        .addRegistryEntry(metacard, Collections.singleton(REGISTRY_STORE_REGISTRY_ID));
    verify(federationAdminService, never()).updateRegistryEntry(metacard);

    Attribute publicationsAfter =
        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
    assertThat(publicationsAfter, is(equalTo(publishedLocationsAttribute)));

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate, is(equalTo(before)));
  }

  @Test
  public void testPublishAddAnotherSource() throws Exception {
    String someAlreadyPublishedRegistryId = "someAlreadyPublishedRegistryId";
    String publishThisRegistryId = "publishThisRegistryId";

    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);

    Attribute publishedLocationsAttribute =
        new AttributeImpl(
            RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
            Collections.singletonList(someAlreadyPublishedRegistryId));
    metacard.setAttribute(publishedLocationsAttribute);
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));

    when(federationAdminService.getRegistryMetacardsByRegistryIds(any(List.class)))
        .thenReturn(Collections.singletonList(metacard));

    registryPublicationService.publish(publishThisRegistryId, REGISTRY_STORE_REGISTRY_ID);
    verify(federationAdminService)
        .addRegistryEntry(metacard, Collections.singleton(REGISTRY_STORE_ID));
    verify(federationAdminService).updateRegistryEntry(metacard);

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate.after(before), is(equalTo(true)));

    Attribute publicationsAfter =
        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
    List<Serializable> publications = publicationsAfter.getValues();
    assertThat(publications, hasItem(REGISTRY_STORE_REGISTRY_ID));
  }

  @Test
  public void testPublish() throws Exception {
    doReturn(Collections.singletonList(metacard))
        .when(federationAdminService)
        .getRegistryMetacardsByRegistryIds(any(List.class));
    String publishThisRegistryId = "registryId";
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));

    registryPublicationService.publish(publishThisRegistryId, REGISTRY_STORE_REGISTRY_ID);
    verify(federationAdminService)
        .addRegistryEntry(metacard, Collections.singleton(REGISTRY_STORE_ID));
    verify(federationAdminService).updateRegistryEntry(metacard);

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate.after(before), is(equalTo(true)));

    Attribute publicationsAfter =
        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
    List<Serializable> publications = publicationsAfter.getValues();
    assertThat(publications, hasItem(REGISTRY_STORE_REGISTRY_ID));
  }

  @Test
  public void testUnpublish() throws Exception {
    String unPublishThisRegistryId = "unPublishThisRegistryId";
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);

    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));
    metacard.setAttribute(
        new AttributeImpl(
            RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
            Collections.singletonList(REGISTRY_STORE_REGISTRY_ID)));
    when(federationAdminService.getRegistryMetacardsByRegistryIds(any(List.class)))
        .thenReturn(Collections.singletonList(metacard));
    registryPublicationService.unpublish(unPublishThisRegistryId, REGISTRY_STORE_REGISTRY_ID);
    verify(federationAdminService)
        .deleteRegistryEntriesByRegistryIds(
            Collections.singletonList(unPublishThisRegistryId),
            Collections.singleton(REGISTRY_STORE_ID));
    verify(federationAdminService).updateRegistryEntry(metacard);

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate.after(before), is(equalTo(true)));

    Attribute publicationsAfter =
        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
    assertThat(publicationsAfter.getValue(), is("No_Publications"));
  }

  @Test
  public void testUnpublishNoCurrentlyPublished() throws Exception {
    metacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, Collections.emptyList()));
    when(federationAdminService.getRegistryMetacardsByRegistryIds(any(List.class)))
        .thenReturn(Collections.singletonList(metacard));
    registryPublicationService.unpublish("regId", "sourceId");
    verify(federationAdminService, never())
        .deleteRegistryEntriesByRegistryIds(
            Collections.singletonList("regId"), Collections.singleton("sourceId"));
    verify(federationAdminService, never()).updateRegistryEntry(metacard);
  }

  @Test
  public void testUpdateNoPublications() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, new ArrayList<>());
    registryPublicationService.update(metacard);
    verify(federationAdminService, never()).updateRegistryEntry(metacard);
  }

  @Test
  public void testUpdateEmptyPublications() throws Exception {
    MetacardImpl metacard = new MetacardImpl();
    registryPublicationService.update(metacard);
    verify(federationAdminService, never()).updateRegistryEntry(metacard);
  }

  @Test
  public void testUpdate() throws Exception {
    String registryId1 = "registryId1";
    String registryId2 = "registryId2";
    String registryId3 = "registryId3";
    String storeId1 = "store1";
    String storeId3 = "store3";

    // Purposely leave out mock for registry store 2
    // so a store id won't be found for registryId2
    // Not a likely case but should be handled
    RegistryStore store1 = mock(RegistryStore.class);
    when(store1.getId()).thenReturn(storeId1);
    when(store1.getRegistryId()).thenReturn(registryId1);
    RegistryStore store3 = mock(RegistryStore.class);
    when(store3.getId()).thenReturn(storeId3);
    when(store3.getRegistryId()).thenReturn(registryId3);

    when(bundleContext.getService(serviceReference)).thenReturn(store1);
    registryPublicationService.bindRegistryStore(serviceReference);
    when(bundleContext.getService(serviceReference)).thenReturn(store3);
    registryPublicationService.bindRegistryStore(serviceReference);

    List<Serializable> publishedLocations = ImmutableList.of(registryId1, registryId2, registryId3);
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, publishedLocations));
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));

    registryPublicationService.update(metacard);
    // update will only be callled with storeIds 1 and 3, 2 didn't have a storeId match for
    // registryId2
    verify(federationAdminService, times(1))
        .updateRegistryEntry(eq(metacard), (Set<String>) argThat(contains(storeId1, storeId3)));
    verify(federationAdminService, times(1)).updateRegistryEntry(metacard);

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate.after(before), is(equalTo(true)));
  }

  @Test
  public void testUpdateException() throws Exception {
    doThrow(new FederationAdminException("Test Error"))
        .when(federationAdminService)
        .updateRegistryEntry(any(Metacard.class), any(Set.class));
    MetacardImpl mcard = new MetacardImpl();
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, REGISTRY_STORE_REGISTRY_ID);

    registryPublicationService.update(mcard);
    verify(federationAdminService)
        .addRegistryEntry(mcard, Collections.singleton(REGISTRY_STORE_ID));
  }

  @Test(expected = FederationAdminException.class)
  public void testNoRegistryStores() throws Exception {
    doReturn(Collections.singletonList(metacard))
        .when(federationAdminService)
        .getRegistryMetacardsByRegistryIds(any(List.class));
    registryPublicationService.unbindRegistryStore(serviceReference);
    String publishThisRegistryId = "publishThisRegistryId";
    Date now = new Date();
    Date before = new Date(now.getTime() - 100000);
    MetacardImpl metacard = new MetacardImpl();

    Attribute publishedLocationsAttribute =
        new AttributeImpl(
            RegistryObjectMetacardType.PUBLISHED_LOCATIONS,
            Collections.singletonList(REGISTRY_STORE_REGISTRY_ID));
    metacard.setAttribute(publishedLocationsAttribute);
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, before));

    registryPublicationService.publish(publishThisRegistryId, REGISTRY_STORE_REGISTRY_ID);
    verify(federationAdminService, never()).updateRegistryEntry(metacard);

    Attribute publicationsAfter =
        metacard.getAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS);
    assertThat(publicationsAfter, is(equalTo(publishedLocationsAttribute)));

    Attribute lastPublished = metacard.getAttribute(RegistryObjectMetacardType.LAST_PUBLISHED);
    assertThat(lastPublished, notNullValue());

    Date lastPublishedDate = (Date) lastPublished.getValue();
    assertThat(lastPublishedDate, is(equalTo(before)));
  }

  @Test
  public void testBindRegistryStore() {
    registryPublicationService.bindRegistryStore(serviceReference);
  }

  @Test
  public void testBindRegistryStoreNullServiceReference() {
    registryPublicationService.bindRegistryStore(null);
  }

  @Test
  public void testUnBindRegistryStore() {
    registryPublicationService.unbindRegistryStore(serviceReference);
  }

  @Test
  public void testUnBindRegistryStoreNullServiceReference() {
    registryPublicationService.unbindRegistryStore(null);
  }
}
