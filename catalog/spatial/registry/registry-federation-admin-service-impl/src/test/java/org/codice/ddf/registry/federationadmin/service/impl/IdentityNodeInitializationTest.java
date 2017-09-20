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

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExtrinsicObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.schemabindings.helper.InternationalStringTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.registry.transformer.RegistryTransformer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class IdentityNodeInitializationTest {

  private static final String TEST_SITE_NAME = "Slate Rock and Gravel Company";

  private static final String CHANGED_TEST_SITE_NAME = "Gravel and Slate Rock Company";

  private static final String TEST_VERSION = "FF 2.0";

  @Mock private FederationAdminServiceImpl federationAdminService;

  @Mock private ScheduledExecutorService executorService;

  private IdentityNodeInitialization identityNodeInitialization;

  private Metacard testMetacard;

  private RegistryTransformer registryTransformer;

  private XmlParser parser;

  private MetacardMarshaller metacardMarshaller;

  @Before
  public void setUp() throws Exception {
    parser = spy(new XmlParser());
    identityNodeInitialization = spy(new IdentityNodeInitialization());
    registryTransformer = spy(new RegistryTransformer());
    metacardMarshaller = spy(new MetacardMarshaller(parser));
    registryTransformer.setParser(parser);
    registryTransformer.setRegistryMetacardType(new RegistryObjectMetacardType());
    identityNodeInitialization.setRegistryTransformer(registryTransformer);
    identityNodeInitialization.setMetacardMarshaller(metacardMarshaller);
    identityNodeInitialization.setFederationAdminService(federationAdminService);
    identityNodeInitialization.setExecutorService(executorService);
    System.setProperty(SystemInfo.SITE_NAME, TEST_SITE_NAME);
    System.setProperty(SystemInfo.VERSION, TEST_VERSION);
    testMetacard = getTestMetacard();
    testMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, ""));
  }

  @Test
  public void initWithNoPreviousEntry() throws Exception {
    ArgumentCaptor<Metacard> captor = ArgumentCaptor.forClass(Metacard.class);
    when(federationAdminService.getLocalRegistryIdentityMetacard()).thenReturn(Optional.empty());
    identityNodeInitialization.init();
    verify(federationAdminService).addRegistryEntry(captor.capture());
    Metacard metacard = captor.getValue();
    assertThat(metacard.getAttribute(Core.MODIFIED), notNullValue());
    assertThat(metacard.getAttribute(Core.CREATED), notNullValue());
    assertThat(
        metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE).getValue(),
        equalTo(true));
    assertThat(
        metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE).getValue(),
        equalTo(true));
  }

  @Test
  public void initWithPreviousPrimaryEntry() throws Exception {
    testMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "registryId"));
    testMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, TEST_SITE_NAME));
    when(federationAdminService.getLocalRegistryIdentityMetacard())
        .thenReturn(Optional.of(testMetacard));
    identityNodeInitialization.init();
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test
  public void initWithPreviousEntryWithNameChange() throws Exception {
    System.setProperty(SystemInfo.SITE_NAME, CHANGED_TEST_SITE_NAME);
    RegistryPackageType registryPackageType = buildRegistryPackageType();

    testMetacard.setAttribute(
        new AttributeImpl(
            Metacard.METADATA, metacardMarshaller.getRegistryPackageAsXml(registryPackageType)));
    testMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, "registryId"));
    testMetacard.setAttribute(new AttributeImpl(Metacard.TITLE, TEST_SITE_NAME));
    when(federationAdminService.getLocalRegistryIdentityMetacard())
        .thenReturn(Optional.of(testMetacard));

    ArgumentCaptor<Metacard> updatedMetacard = ArgumentCaptor.forClass(Metacard.class);
    doNothing().when(federationAdminService).updateRegistryEntry(updatedMetacard.capture());

    identityNodeInitialization.init();

    assertThat(updatedMetacard.getValue().getTitle(), is(CHANGED_TEST_SITE_NAME));
    verify(federationAdminService, times(1)).updateRegistryEntry(any(Metacard.class));
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test
  public void initWithGetLocalRegistryNodeException() throws Exception {
    doThrow(FederationAdminException.class)
        .when(federationAdminService)
        .getLocalRegistryIdentityMetacard();
    identityNodeInitialization.init();
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void initWithIngestException() throws Exception {
    when(federationAdminService.getLocalRegistryIdentityMetacard()).thenReturn(Optional.empty());
    when(federationAdminService.addRegistryEntry(any(Metacard.class)))
        .thenThrow(FederationAdminException.class);
    identityNodeInitialization.init();
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void initWithRegistryTransformerException() throws Exception {
    when(federationAdminService.getLocalRegistryIdentityMetacard()).thenReturn(Optional.empty());
    doThrow(CatalogTransformerException.class)
        .when(registryTransformer)
        .transform(any(InputStream.class));
    identityNodeInitialization.init();
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void initWithParserException() throws Exception {
    when(federationAdminService.getLocalRegistryIdentityMetacard()).thenReturn(Optional.empty());
    doThrow(ParserException.class)
        .when(metacardMarshaller)
        .getRegistryPackageAsInputStream(any(RegistryPackageType.class));
    identityNodeInitialization.init();
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  @Test
  public void initWithIOException() throws Exception {
    when(federationAdminService.getLocalRegistryIdentityMetacard()).thenReturn(Optional.empty());
    doThrow(IOException.class).when(registryTransformer).transform(any(InputStream.class));
    identityNodeInitialization.init();
    verify(executorService).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
  }

  private Metacard getTestMetacard() {
    return new MetacardImpl(new RegistryObjectMetacardType());
  }

  private RegistryPackageType buildRegistryPackageType() {
    SlotTypeHelper slotTypeHelper = new SlotTypeHelper();
    InternationalStringTypeHelper internationalStringTypeHelper =
        new InternationalStringTypeHelper();
    String registryPackageId =
        RegistryConstants.GUID_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
    RegistryPackageType registryPackage = RIM_FACTORY.createRegistryPackageType();
    registryPackage.setId(registryPackageId);
    registryPackage.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

    ExtrinsicObjectType extrinsicObject = RIM_FACTORY.createExtrinsicObjectType();
    extrinsicObject.setObjectType(RegistryConstants.REGISTRY_NODE_OBJECT_TYPE);

    String extrinsicObjectId =
        RegistryConstants.GUID_PREFIX + UUID.randomUUID().toString().replaceAll("-", "");
    extrinsicObject.setId(extrinsicObjectId);
    extrinsicObject.setName(internationalStringTypeHelper.create(TEST_SITE_NAME));

    String home = SystemBaseUrl.getBaseUrl();
    extrinsicObject.setHome(home);

    registryPackage.setRegistryObjectList(RIM_FACTORY.createRegistryObjectListType());
    registryPackage
        .getRegistryObjectList()
        .getIdentifiable()
        .add(RIM_FACTORY.createIdentifiable(extrinsicObject));

    return registryPackage;
  }
}
