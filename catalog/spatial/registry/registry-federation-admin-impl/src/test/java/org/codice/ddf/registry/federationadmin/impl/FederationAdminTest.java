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
package org.codice.ddf.registry.federationadmin.impl;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.action.MultiActionProvider;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.endpoint.CatalogEndpoint;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.Source;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminService;
import org.codice.ddf.registry.federationadmin.service.internal.RegistrySourceConfiguration;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.schemabindings.converter.type.RegistryPackageTypeConverter;
import org.codice.ddf.registry.schemabindings.converter.web.RegistryPackageWebConverter;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.registry.transformer.RegistryTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.event.Event;

@RunWith(MockitoJUnitRunner.class)
public class FederationAdminTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  @Mock private FederationAdminService federationAdminService;

  @Mock private RegistryTransformer registryTransformer;

  @Mock private AdminHelper helper;

  @Mock private CatalogFramework catalogFramework;

  @Mock private BundleContext context;

  @Mock private QueryResponse queryResponse;

  @Mock private CatalogStore store;

  @Mock private RegistrySourceConfiguration sourceConfiguration;

  @Mock private MultiActionProvider multiActionProvider;

  private Parser parser;

  private ParserConfigurator configurator;

  private FederationAdmin federationAdmin;

  private Map<String, CatalogStore> catalogStoreMap = new HashMap<>();

  private MetacardImpl mcard;

  private RegistryPackageTypeConverter typeConverter = new RegistryPackageTypeConverter();

  private RegistryPackageWebConverter mapConverter = new RegistryPackageWebConverter();

  private static final String LOCAL_NODE_KEY = "nodes";

  @Before
  public void setup() throws Exception {
    parser = new XmlParser();
    configurator =
        parser.configureParser(
            Arrays.asList(
                RegistryObjectType.class.getPackage().getName(),
                EbrimConstants.OGC_FACTORY.getClass().getPackage().getName(),
                EbrimConstants.GML_FACTORY.getClass().getPackage().getName()),
            this.getClass().getClassLoader());
    federationAdmin =
        new FederationAdmin(helper) {
          @Override
          public BundleContext getContext() {
            return context;
          }
        };
    federationAdmin.setFederationAdminService(federationAdminService);
    federationAdmin.setRegistryTransformer(registryTransformer);
    federationAdmin.setMetacardMarshaller(new MetacardMarshaller(parser));
    federationAdmin.setSlotHelper(new SlotTypeHelper());
    federationAdmin.setRegistryMapConverter(new RegistryPackageWebConverter());
    federationAdmin.setRegistryTypeConverter(new RegistryPackageTypeConverter());
    federationAdmin.setSourceConfigRefresh(sourceConfiguration);

    mcard = new MetacardImpl(new RegistryObjectMetacardType());
    mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "myId");
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, new ArrayList<>());
    mcard.setId("someUUID");

    when(queryResponse.getResults()).thenReturn(Collections.singletonList(new ResultImpl(mcard)));
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(queryResponse);
    catalogStoreMap.put("myDest", store);
    setupDefaultFilterProps();
  }

  @Test
  public void testCreateLocalEntry() throws Exception {
    String metacardId = "metacardId";
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);

    Metacard metacard = getTestMetacard();
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(federationAdminService.addRegistryEntry(any(Metacard.class))).thenReturn(metacardId);

    String createdMetacardId = federationAdmin.createLocalEntry(registryMap);

    assertThat(createdMetacardId, is(equalTo(metacardId)));
    verify(federationAdminService).addRegistryEntry(metacard);
  }

  @Test
  public void testCreateLocalEntryMissingAttributes() throws Exception {
    String metacardId = "metacardId";
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
    registryMap.remove("id");
    registryMap.remove("home");
    registryMap.remove("objectType");
    Metacard metacard = getTestMetacard();
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(federationAdminService.addRegistryEntry(any(Metacard.class))).thenReturn(metacardId);

    federationAdmin.createLocalEntry(registryMap);
    ArgumentCaptor<InputStream> captor = ArgumentCaptor.forClass(InputStream.class);
    verify(registryTransformer).transform(captor.capture());
    String ebrim = IOUtils.toString(captor.getValue());
    IOUtils.closeQuietly(captor.getValue());
    assertXpathEvaluatesTo(
        SystemBaseUrl.EXTERNAL.getBaseUrl(), "/*[local-name() = 'RegistryPackage']/@home", ebrim);
    assertXpathEvaluatesTo(
        RegistryConstants.REGISTRY_NODE_OBJECT_TYPE,
        "/*[local-name() = 'RegistryPackage']/@objectType",
        ebrim);
    assertXpathExists("/*[local-name() = 'RegistryPackage']/@id", ebrim);
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryWithEmptyMap() throws Exception {
    Map<String, Object> registryMap = new HashMap<>();
    federationAdmin.createLocalEntry(registryMap);

    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryWithBadMap() throws Exception {
    Map<String, Object> registryMap = new HashMap<>();
    registryMap.put("BadKey", "BadValue");

    federationAdmin.createLocalEntry(registryMap);
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryWithFederationAdminException() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);

    Metacard metacard = getTestMetacard();
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(federationAdminService.addRegistryEntry(any(Metacard.class)))
        .thenThrow(FederationAdminException.class);

    federationAdmin.createLocalEntry(registryMap);

    verify(federationAdminService).addRegistryEntry(metacard);
  }

  @Test
  public void testCreateLocalEntryString() throws Exception {
    String encodeThisString = "aPretendXmlRegistryPackage";
    String metacardId = "createdMetacardId";
    String base64EncodedString = Base64.getEncoder().encodeToString(encodeThisString.getBytes());

    Metacard metacard = getTestMetacard();

    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(federationAdminService.addRegistryEntry(metacard)).thenReturn(metacardId);

    String createdMetacardId = federationAdmin.createLocalEntry(base64EncodedString);

    assertThat(createdMetacardId, is(equalTo(metacardId)));
    verify(registryTransformer).transform(any(InputStream.class));
    verify(federationAdminService).addRegistryEntry(metacard);
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryStringWithBlankString() throws Exception {
    String base64EncodedString = "";

    federationAdmin.createLocalEntry(base64EncodedString);

    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryStringWithTransformerException() throws Exception {
    String encodeThisString = "aPretendXmlRegistryPackage";
    String base64EncodedString = Base64.getEncoder().encodeToString(encodeThisString.getBytes());

    when(registryTransformer.transform(any(InputStream.class)))
        .thenThrow(CatalogTransformerException.class);

    federationAdmin.createLocalEntry(base64EncodedString);

    verify(registryTransformer).transform(any(InputStream.class));
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testCreateLocalEntryStringWithDecodeError() throws Exception {
    // This is has an illegal base64 character
    String base64EncodedString = "[B@6499375d";

    federationAdmin.createLocalEntry(base64EncodedString);

    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).addRegistryEntry(any(Metacard.class));
  }

  @Test
  public void testUpdateLocalEntry() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-registry-package-smaller.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
    String existingMetacardId = "someUpdateMetacardId";

    Metacard existingMetacard = getTestMetacard();
    existingMetacard.setAttribute(new AttributeImpl(Metacard.ID, existingMetacardId));
    List<Metacard> existingMetacards = new ArrayList<>();
    existingMetacards.add(existingMetacard);
    Metacard updateMetacard = getTestMetacard();

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(
            Collections.singletonList(registryObject.getId())))
        .thenReturn(existingMetacards);
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(updateMetacard);

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService)
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(registryObject.getId()));
    verify(registryTransformer).transform(any(InputStream.class));
    verify(federationAdminService).updateRegistryEntry(updateMetacard);
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateLocalEntryWithEmptyMap() throws Exception {
    Map<String, Object> registryMap = new HashMap<>();

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService, never())
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(any(String.class)));
    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateLocalEntryWithBadMap() throws Exception {
    Map<String, Object> registryMap = new HashMap<>();
    registryMap.put("BadKey", "BadValue");

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService, never())
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(any(String.class)));
    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateLocalEntryWithEmptyExistingList() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
    List<Metacard> existingMetacards = new ArrayList<>();

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(
            Collections.singletonList(registryObject.getId())))
        .thenReturn(existingMetacards);

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService, never())
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(registryObject.getId()));
    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateLocalEntryWithMultipleExistingMetacards() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
    List<Metacard> existingMetacards = new ArrayList<>();
    existingMetacards.add(getTestMetacard());
    existingMetacards.add(getTestMetacard());

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(
            Collections.singletonList(registryObject.getId())))
        .thenReturn(existingMetacards);

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService, never())
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(registryObject.getId()));
    verify(registryTransformer, never()).transform(any(InputStream.class));
    verify(federationAdminService, never()).updateRegistryEntry(any(Metacard.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateLocalEntryWithFederationAdminServiceException() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-full-registry-package.xml");
    Map<String, Object> registryMap = getMapFromRegistryObject(registryObject);
    String existingMetacardId = "someUpdateMetacardId";

    Metacard existingMetacard = getTestMetacard();
    existingMetacard.setAttribute(new AttributeImpl(Metacard.ID, existingMetacardId));
    List<Metacard> existingMetacards = new ArrayList<>();
    existingMetacards.add(existingMetacard);
    Metacard updateMetacard = getTestMetacard();

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(
            Collections.singletonList(registryObject.getId())))
        .thenReturn(existingMetacards);
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(updateMetacard);
    doThrow(FederationAdminException.class)
        .when(federationAdminService)
        .updateRegistryEntry(updateMetacard);

    federationAdmin.updateLocalEntry(registryMap);

    verify(federationAdminService)
        .getLocalRegistryMetacardsByRegistryIds(Collections.singletonList(registryObject.getId()));
    verify(registryTransformer).transform(any(InputStream.class));
    verify(federationAdminService).updateRegistryEntry(updateMetacard);
  }

  @Test
  public void testDeleteLocalEntry() throws Exception {
    String firstRegistryId = "firstRegistryId";
    String secondRegistryId = "secondRegistryId";
    List<String> ids = new ArrayList<>();
    ids.add(firstRegistryId);
    ids.add(secondRegistryId);

    String firstMetacardId = "firstMetacardId";
    String secondMetacardId = "secondMetacardId";

    Metacard firstMetacard = getTestMetacard();
    firstMetacard.setAttribute(new AttributeImpl(Metacard.ID, firstMetacardId));
    Metacard secondMetacard = getTestMetacard();
    secondMetacard.setAttribute(new AttributeImpl(Metacard.ID, secondMetacardId));

    List<Metacard> matchingMetacards = new ArrayList<>();
    matchingMetacards.add(firstMetacard);
    matchingMetacards.add(secondMetacard);

    List<String> metacardIds = new ArrayList<>();
    metacardIds.addAll(
        matchingMetacards.stream().map(Metacard::getId).collect(Collectors.toList()));

    when(federationAdminService.getRegistryMetacardsByRegistryIds(ids, true))
        .thenReturn(matchingMetacards);

    federationAdmin.deleteLocalEntry(ids);

    verify(federationAdminService).getRegistryMetacardsByRegistryIds(ids, true);
    verify(federationAdminService).deleteRegistryEntriesByMetacardIds(metacardIds);
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteLocalEntryWithEmptyList() throws Exception {
    List<String> ids = new ArrayList<>();

    federationAdmin.deleteLocalEntry(ids);

    verify(federationAdminService, never()).getLocalRegistryMetacardsByRegistryIds(anyList());
    verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(anyList());
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteLocalEntryWithExceptionGettingLocalMetacards() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add("whatever");

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids))
        .thenThrow(FederationAdminException.class);

    federationAdmin.deleteLocalEntry(ids);

    verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(ids);
    verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(anyList());
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteLocalEntryWithNonMatchingLists() throws Exception {
    String firstRegistryId = "firstRegistryId";
    String secondRegistryId = "secondRegistryId";
    List<String> ids = new ArrayList<>();
    ids.add(firstRegistryId);
    ids.add(secondRegistryId);

    String firstMetacardId = "firstMetacardId";

    Metacard firstMetacard = getTestMetacard();
    firstMetacard.setAttribute(new AttributeImpl(Metacard.ID, firstMetacardId));

    List<Metacard> matchingMetacards = new ArrayList<>();
    matchingMetacards.add(firstMetacard);

    List<String> metacardIds = new ArrayList<>();
    metacardIds.addAll(
        matchingMetacards.stream().map(Metacard::getId).collect(Collectors.toList()));

    when(federationAdminService.getLocalRegistryMetacardsByRegistryIds(ids))
        .thenReturn(matchingMetacards);

    federationAdmin.deleteLocalEntry(ids);

    verify(federationAdminService).getLocalRegistryMetacardsByRegistryIds(ids);
    verify(federationAdminService, never()).deleteRegistryEntriesByMetacardIds(metacardIds);
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteLocalEntryWithExceptionDeletingEntries() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add("firstId");

    doThrow(FederationAdminException.class)
        .when(federationAdminService)
        .deleteRegistryEntriesByRegistryIds(ids);
    federationAdmin.deleteLocalEntry(ids);

    verify(federationAdminService).deleteRegistryEntriesByRegistryIds(ids);
  }

  @Test
  public void testGetLocalNodes() throws Exception {
    RegistryPackageType registryObject =
        getRegistryObjectFromResource("/csw-registry-package-smaller.xml");
    Map<String, Object> registryObjectMap =
        new RegistryPackageWebConverter().convert(registryObject);
    List<RegistryPackageType> registryPackages = new ArrayList<>();
    registryPackages.add((RegistryPackageType) registryObject);

    when(federationAdminService.getLocalRegistryObjects()).thenReturn(registryPackages);
    Map<String, Object> localNodes = federationAdmin.getLocalNodes();

    Map<String, Object> localNode =
        ((List<Map<String, Object>>) localNodes.get(LOCAL_NODE_KEY)).get(0);
    verify(federationAdminService).getLocalRegistryObjects();
    assertThat(localNode, is(equalTo(registryObjectMap)));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalNodesWithFederationAdminException() throws Exception {
    when(federationAdminService.getLocalRegistryObjects())
        .thenThrow(FederationAdminException.class);

    federationAdmin.getLocalNodes();

    verify(federationAdminService).getLocalRegistryObjects();
  }

  @Test
  public void testAllRegistryInfoNoMetatypes() throws Exception {
    List<Service> metatypes = new ArrayList<>();
    when(helper.getMetatypes()).thenReturn(metatypes);
    assertThat(federationAdmin.allRegistryInfo().size(), is(0));
  }

  @Test
  public void testAllRegistryInfo() throws Exception {
    List<Service> metatypes = new ArrayList<>();
    List<Configuration> configurations = new ArrayList<>();
    Dictionary<String, Object> props = new Hashtable<>();
    Dictionary<String, Object> propsDisabled = new Hashtable<>();

    metatypes.add(new ServiceImpl());

    props.put("key1", "value1");
    propsDisabled.put("key2", "value2");

    Configuration config = mock(Configuration.class);
    configurations.add(config);
    when(config.getPid()).thenReturn("myPid");
    when(config.getFactoryPid()).thenReturn("myFpid");
    when(config.getProperties()).thenReturn(props);

    Configuration configDisabled = mock(Configuration.class);
    configurations.add(configDisabled);
    when(configDisabled.getPid()).thenReturn("myPid_disabled");
    when(configDisabled.getFactoryPid()).thenReturn("myFpid_disabled");
    when(configDisabled.getProperties()).thenReturn(propsDisabled);

    when(helper.getMetatypes()).thenReturn(metatypes);
    when(helper.getConfigurations(any(Service.class))).thenReturn(configurations);
    when(helper.getName(any(Configuration.class))).thenReturn("name");
    when(helper.getBundleName(any(Configuration.class))).thenReturn("bundleName");
    when(helper.getBundleId(any(Configuration.class))).thenReturn(1234L);

    List<Service> updatedMetatypes = federationAdmin.allRegistryInfo();
    assertThat(updatedMetatypes.size(), is(1));
    ArrayList<Map<String, Object>> configs =
        (ArrayList<Map<String, Object>>) updatedMetatypes.get(0).get("configurations");
    assertThat(configs.size(), is(2));
    Map<String, Object> activeConfig = configs.get(0);
    Map<String, Object> disabledConfig = configs.get(1);
    assertThat(activeConfig.get("name"), equalTo("name"));
    assertThat(activeConfig.get("id"), equalTo("myPid"));
    assertThat(activeConfig.get("fpid"), equalTo("myFpid"));
    assertThat(activeConfig.get("enabled"), equalTo(true));
    assertThat(
        ((Map<String, Object>) activeConfig.get("properties")).get("key1"), equalTo("value1"));

    assertThat(disabledConfig.get("name"), equalTo("myPid_disabled"));
    assertThat(disabledConfig.get("id"), equalTo("myPid_disabled"));
    assertThat(disabledConfig.get("fpid"), equalTo("myFpid_disabled"));
    assertThat(disabledConfig.get("enabled"), equalTo(false));
    assertThat(
        ((Map<String, Object>) disabledConfig.get("properties")).get("key2"), equalTo("value2"));
  }

  @Test
  public void testAllRegistryMetacards() throws Exception {
    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);
    ArrayList<String> tags = new ArrayList<>();
    tags.add(RegistryConstants.REGISTRY_TAG);
    mcard.setAttribute(Metacard.TAGS, tags);
    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "location1");
    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacards().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertThat(mcardMap.get("TransientValues"), notNullValue());
    Map<String, Object> transValues = (Map<String, Object>) mcardMap.get("TransientValues");
    assertThat(
        ((List) transValues.get(RegistryObjectMetacardType.PUBLISHED_LOCATIONS)).get(0),
        equalTo("location1"));
  }

  @Test
  public void testRegistryStatusNotConfiguredService() throws Exception {
    Source source = mock(Source.class);
    when(helper.getRegistrySources()).thenReturn(Collections.singletonList(source));
    assertThat(federationAdmin.registryStatus("servicePid"), is(false));
  }

  @Test
  public void testRegistryStatusNoMatchingConfig() throws Exception {
    RegistryStore source = mock(RegistryStore.class);
    Configuration config = mock(Configuration.class);
    Dictionary<String, Object> props = new Hashtable<>();
    props.put("service.pid", "servicePid2");
    when(config.getProperties()).thenReturn(props);
    when(source.isAvailable()).thenReturn(true);
    when(helper.getRegistrySources()).thenReturn(Collections.singletonList(source));
    when(helper.getConfiguration(any(ConfiguredService.class))).thenReturn(config);
    assertThat(federationAdmin.registryStatus("servicePid"), is(false));
  }

  @Test
  public void testRegistryStatusNoConfig() throws Exception {
    RegistryStore source = mock(RegistryStore.class);
    when(source.isAvailable()).thenReturn(true);
    when(helper.getRegistrySources()).thenReturn(Collections.singletonList(source));
    when(helper.getConfiguration(any(ConfiguredService.class))).thenReturn(null);
    assertThat(federationAdmin.registryStatus("servicePid"), is(false));
  }

  @Test
  public void testRegistryStatus() throws Exception {
    RegistryStore source = mock(RegistryStore.class);
    Configuration config = mock(Configuration.class);
    Dictionary<String, Object> props = new Hashtable<>();
    props.put("service.pid", "servicePid");
    when(config.getProperties()).thenReturn(props);
    when(source.isAvailable()).thenReturn(true);
    when(helper.getRegistrySources()).thenReturn(Collections.singletonList(source));
    when(helper.getConfiguration(any(ConfiguredService.class))).thenReturn(config);
    assertThat(federationAdmin.registryStatus("servicePid"), is(true));
  }

  @Test
  public void testInit() throws Exception {

    copyJsonFileToKarafDir();

    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);

    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "location1");
    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    federationAdmin.init();
    Map<String, Object> nodes = federationAdmin.getLocalNodes();
    Map<String, Object> customSlots = (Map<String, Object>) nodes.get("customSlots");
    assertThat(customSlots.size(), is(6));
  }

  @Test
  public void testInitNoFile() throws Exception {
    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);

    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
    mcard.setAttribute(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "location1");
    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    federationAdmin.init();
    Map<String, Object> nodes = federationAdmin.getLocalNodes();
    Map<String, Object> customSlots = (Map<String, Object>) nodes.get("customSlots");
    assertThat(customSlots, nullValue());
  }

  @Test
  public void testBindEndpointNullReference() throws Exception {
    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);

    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");

    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    federationAdmin.bindEndpoint(null);
    Map<String, Object> autoValues =
        (Map<String, Object>) federationAdmin.getLocalNodes().get("autoPopulateValues");
    assertThat(autoValues.size(), is(1));
    Collection bindingValues = (Collection) autoValues.get("ServiceBinding");
    assertThat(bindingValues.size(), is(0));
  }

  @Test
  public void testBindEndpoint() throws Exception {
    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);

    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");

    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    ServiceReference reference = mock(ServiceReference.class);
    CatalogEndpoint endpoint = mock(CatalogEndpoint.class);
    Map<String, String> props = new HashMap<>();
    props.put(CatalogEndpoint.ID_KEY, "myId");
    when(endpoint.getEndpointProperties()).thenReturn(props);
    when(context.getService(reference)).thenReturn(endpoint);
    federationAdmin.bindEndpoint(reference);
    Map<String, Object> autoValues =
        (Map<String, Object>) federationAdmin.getLocalNodes().get("autoPopulateValues");
    assertThat(autoValues.size(), is(1));
    Collection bindingValues = (Collection) autoValues.get("ServiceBinding");
    assertThat(bindingValues.size(), is(1));
    Map<String, String> bindings = (Map<String, String>) bindingValues.iterator().next();
    assertThat(bindings.get(CatalogEndpoint.ID_KEY), equalTo("myId"));
  }

  @Test
  public void testUnbindEndpoint() throws Exception {
    List<RegistryPackageType> regObjects =
        Collections.singletonList(
            (RegistryPackageType) getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    when(federationAdminService.getRegistryObjects()).thenReturn(regObjects);

    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");

    when(federationAdminService.getRegistryMetacards())
        .thenReturn(Collections.singletonList(mcard));
    ServiceReference reference = mock(ServiceReference.class);
    CatalogEndpoint endpoint = mock(CatalogEndpoint.class);
    Map<String, String> props = new HashMap<>();
    props.put(CatalogEndpoint.ID_KEY, "myId");
    when(endpoint.getEndpointProperties()).thenReturn(props);
    when(context.getService(reference)).thenReturn(endpoint);
    federationAdmin.bindEndpoint(reference);

    federationAdmin.unbindEndpoint(reference);
    Map<String, Object> autoValues =
        (Map<String, Object>) federationAdmin.getLocalNodes().get("autoPopulateValues");
    assertThat(autoValues.size(), is(1));
    Collection bindingValues = (Collection) autoValues.get("ServiceBinding");
    assertThat(bindingValues.size(), is(0));
  }

  @Test
  public void testRegenerateSources() throws Exception {
    federationAdmin.regenerateRegistrySources(Collections.singletonList("regId"));
    verify(sourceConfiguration).regenerateOneSource("regId");
  }

  @Test
  public void testHandleEventCreate() throws Exception {
    performCreateEvent();
  }

  @Test
  public void testHandleEventUpdate() throws Exception {
    performCreateEvent();
    Dictionary<String, Object> eventProperties = new Hashtable<>();
    mcard.setTitle("UpdatedTitle");
    eventProperties.put("ddf.catalog.event.metacard", mcard);
    Event event = new Event("ddf/catalog/event/UPDATED", eventProperties);
    federationAdmin.handleEvent(event);

    setupDefaultFilterProps();

    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_NAME), is("UpdatedTitle"));
  }

  @Test
  public void testHandleEventDelete() throws Exception {
    performCreateEvent();
    Dictionary<String, Object> eventProperties = new Hashtable<>();
    eventProperties.put("ddf.catalog.event.metacard", mcard);
    Event event = new Event("ddf/catalog/event/DELETED", eventProperties);
    federationAdmin.handleEvent(event);

    setupDefaultFilterProps();

    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(0));
  }

  @Test
  public void testAllRegistryMetacardsSummary() throws Exception {
    Date timestamp = setupSummary();
    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertSummary(mcardMap, timestamp);
  }

  @Test
  public void testAllRegistryMetacardsSummaryWithActionProvider() throws Exception {
    Action action = mock(Action.class);
    when(action.getId()).thenReturn("catalog.data.metacard.registry");
    when(action.getUrl()).thenReturn(new URL("https://host/path"));
    when(multiActionProvider.canHandle(any())).thenReturn(true);
    when(multiActionProvider.getActions(any())).thenReturn(Collections.singletonList(action));
    federationAdmin.setRegistryActionProvider(multiActionProvider);
    Date timestamp = setupSummary();
    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertSummary(mcardMap, timestamp);
    assertThat(mcardMap.get("reportAction"), is("https://host/path"));
  }

  @Test
  public void testAllRegistryMetacardsSummaryWithActionProviderNoAction() throws Exception {
    when(multiActionProvider.canHandle(any())).thenReturn(true);
    when(multiActionProvider.getActions(any())).thenReturn(Collections.emptyList());
    federationAdmin.setRegistryActionProvider(multiActionProvider);
    Date timestamp = setupSummary();
    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertSummary(mcardMap, timestamp);
  }

  @Test
  public void testRegistryMetacard() throws Exception {
    when(federationAdminService.getRegistryObjectByRegistryId(any()))
        .thenReturn(getRegistryObjectFromResource("/csw-full-registry-package.xml"));
    ArrayList<String> tags = new ArrayList<>();
    tags.add(RegistryConstants.REGISTRY_TAG);
    mcard.setAttribute(Metacard.TAGS, tags);
    mcard.setAttribute(
        RegistryObjectMetacardType.REGISTRY_ID, "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");

    when(federationAdminService.getRegistryMetacardsByRegistryIds(any()))
        .thenReturn(Collections.singletonList(mcard));
    List<Map<String, Object>> result =
        (List<Map<String, Object>>)
            federationAdmin
                .registryMetacard("urn:uuid:2014ca7f59ac46f495e32b4a67a51276")
                .get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertThat(mcardMap.get("id"), is("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegistryMetacardInvalidRegistryId() throws Exception {
    federationAdmin.registryMetacard("invalidRegId");
  }

  @Test
  public void testRegistryMetacardRegistryIdNotFound() throws Exception {
    when(federationAdminService.getRegistryObjectByRegistryId(any()))
        .thenThrow(new FederationAdminException("Not found"));

    when(federationAdminService.getRegistryMetacardsByRegistryIds(any()))
        .thenReturn(Collections.emptyList());
    List<Map<String, Object>> result =
        (List<Map<String, Object>>)
            federationAdmin
                .registryMetacard("urn:uuid:2014ca7f59ac46f495e32b4a67a51276")
                .get("nodes");
    assertThat(result.size(), is(0));
  }

  private void setupDefaultFilterProps() throws IOException {
    Map<String, Object> filterProps = new HashMap<>();
    filterProps.put(FederationAdmin.SUMMARY_FILTERED, new String[0]);
    when(helper.getFilterProperties()).thenReturn(filterProps);
  }

  private void performCreateEvent() throws Exception {
    List<Map<String, Object>> result =
        (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(0));
    Date timestamp = setupSummary(false);
    Dictionary<String, Object> eventProperties = new Hashtable<>();
    eventProperties.put("ddf.catalog.event.metacard", mcard);
    Event event = new Event("ddf/catalog/event/CREATED", eventProperties);
    federationAdmin.handleEvent(event);

    setupDefaultFilterProps();

    result = (List<Map<String, Object>>) federationAdmin.allRegistryMetacardsSummary().get("nodes");
    assertThat(result.size(), is(1));
    Map<String, Object> mcardMap = result.get(0);
    assertSummary(mcardMap, timestamp);
  }

  private Date setupSummary() throws Exception {
    return setupSummary(true);
  }

  private Date setupSummary(boolean setupAdminService) throws Exception {
    Date now = new Date();
    ArrayList<String> tags = new ArrayList<>();
    tags.add(RegistryConstants.REGISTRY_TAG);
    mcard.setAttribute(Metacard.TAGS, tags);
    mcard.setTitle("TestTitle");
    mcard.setCreatedDate(now);
    mcard.setModifiedDate(now);
    mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true);
    mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
    if (setupAdminService) {
      when(federationAdminService.getRegistryMetacards())
          .thenReturn(Collections.singletonList(mcard));
    }
    return now;
  }

  private void assertSummary(Map<String, Object> mcardMap, Date timestamp) {
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_METACARD_ID), is("someUUID"));
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_REGISTRY_ID), is("myId"));
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_NAME), is("TestTitle"));
    assertThat(mcardMap.get(Metacard.CREATED), is(timestamp));
    assertThat(mcardMap.get(Metacard.MODIFIED), is(timestamp));
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_IDENTITY_NODE), is(true));
    assertThat(mcardMap.get(FederationAdmin.SUMMARY_LOCAL_NODE), is(true));
  }

  private void copyJsonFileToKarafDir() throws Exception {
    File etc = folder.getRoot();
    File registry = folder.newFolder("registry");
    File jsonFile = new File(registry, "registry-custom-slots.json");
    FileOutputStream outputStream = new FileOutputStream(jsonFile);
    InputStream inputStream =
        this.getClass().getResourceAsStream("/etc/registry/registry-custom-slots.json");
    IOUtils.copy(inputStream, outputStream);
    inputStream.close();
    outputStream.close();
    System.setProperty("karaf.etc", etc.getCanonicalPath());
  }

  private RegistryPackageType getRegistryObjectFromResource(String path) throws ParserException {
    RegistryObjectType registryObject = null;
    JAXBElement<RegistryObjectType> jaxbRegistryObject =
        parser.unmarshal(configurator, JAXBElement.class, getClass().getResourceAsStream(path));

    if (jaxbRegistryObject != null) {
      registryObject = jaxbRegistryObject.getValue();
    }

    return (RegistryPackageType) registryObject;
  }

  private Map<String, Object> getMapFromRegistryObject(RegistryPackageType registryObject) {
    return mapConverter.convert(registryObject);
  }

  private Metacard getTestMetacard() {
    return new MetacardImpl(new RegistryObjectMetacardType());
  }

  private static class ServiceImpl extends HashMap<String, Object> implements Service {}
}
