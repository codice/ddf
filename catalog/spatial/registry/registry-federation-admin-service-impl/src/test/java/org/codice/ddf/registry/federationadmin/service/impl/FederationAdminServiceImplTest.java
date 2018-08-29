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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import javax.xml.bind.JAXBElement;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.internal.FederationAdminException;
import org.codice.ddf.registry.schemabindings.EbrimConstants;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.transformer.RegistryTransformer;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FederationAdminServiceImplTest {

  private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

  private static final String TEST_SITE_NAME = "Slate Rock and Gravel Company";

  private static final String TEST_VERSION = "FF 2.0";

  private static final String TEST_DESTINATION = "Destination";

  private static final String TEST_METACARD_ID = "MetacardId";

  private static final String TEST_XML_STRING = "SomeValidStringVersionOfXml";

  private FederationAdminServiceImpl federationAdminServiceImpl;

  private Metacard testMetacard;

  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  @Mock private ParserConfigurator configurator;

  @Mock private CatalogFramework catalogFramework;

  @Mock private Parser parser;

  @Mock private RegistryTransformer registryTransformer;

  @Mock private Security security;

  @Mock private Subject subject;

  @Mock private DeleteResponse deleteResponse;

  @Mock private UpdateResponse updateResponse;

  @Mock private CreateResponse createResponse;

  @Before
  public void setUp() throws Exception {
    when(parser.configureParser(anyList(), any(ClassLoader.class))).thenReturn(configurator);
    federationAdminServiceImpl = Mockito.spy(new FederationAdminServiceImpl(security));
    federationAdminServiceImpl.setRegistryTransformer(registryTransformer);
    federationAdminServiceImpl.setCatalogFramework(catalogFramework);
    federationAdminServiceImpl.setMetacardMarshaller(new MetacardMarshaller(parser));
    federationAdminServiceImpl.setFilterBuilder(filterBuilder);
    System.setProperty(SystemInfo.SITE_NAME, TEST_SITE_NAME);
    System.setProperty(SystemInfo.VERSION, TEST_VERSION);
    testMetacard = getPopulatedTestRegistryMetacard();
    when(deleteResponse.getProcessingErrors()).thenReturn(new HashSet<ProcessingDetails>());
    when(createResponse.getProcessingErrors()).thenReturn(new HashSet<ProcessingDetails>());
    when(updateResponse.getProcessingErrors()).thenReturn(new HashSet<ProcessingDetails>());
    when(catalogFramework.update(any(UpdateRequest.class))).thenReturn(updateResponse);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenReturn(deleteResponse);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(createResponse);
    when(security.runWithSubjectOrElevate(any(Callable.class)))
        .thenAnswer(
            invocation -> {
              try {
                return ((Callable) invocation.getArguments()[0]).call();
              } catch (Exception e) {
                throw new InvocationTargetException(e.getCause());
              }
            });
  }

  @Test
  public void testGetRegistryObjectByMetacardId() throws Exception {
    Metacard metacard = testMetacard;
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, metacard);
    RegistryPackageType expectedRegistryPackage = getTestRegistryPackage();
    JAXBElement<RegistryPackageType> jaxbRegistryPackage =
        EbrimConstants.RIM_FACTORY.createRegistryPackage(expectedRegistryPackage);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    when(parser.unmarshal(
            any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class)))
        .thenReturn(jaxbRegistryPackage);
    RegistryPackageType registryPackage =
        federationAdminServiceImpl.getRegistryObjectByRegistryId(TEST_METACARD_ID, null);
    assertThat(registryPackage, is(sameInstance(expectedRegistryPackage)));
  }

  @Test(expected = FederationAdminException.class)
  public void getRegistryObjectByRegistryIdEmptyString() throws FederationAdminException {
    assertThat(federationAdminServiceImpl.getRegistryObjectByRegistryId(""), is(nullValue()));
  }

  @Test(expected = FederationAdminException.class)
  public void getRegistryObjectByRegistryIdThatDoesNotExist() throws Exception {
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request);
    Metacard metacard = getTestMetacard();
    when(security.getSystemSubject()).thenReturn(subject);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    federationAdminServiceImpl.getRegistryObjectByRegistryId("Not a metacard");
  }

  @Test(expected = FederationAdminException.class)
  public void getRegistryObjectByRegistryIdDuplicateMetacards() throws Exception {
    Metacard metacard = testMetacard;
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, metacard, metacard);
    when(security.getSystemSubject()).thenReturn(subject);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.getRegistryObjectByRegistryId(TEST_METACARD_ID);
  }

  @Test
  public void testAddRegistryEntry() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = testMetacard;
    Metacard createdMetacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    Subject systemSubject = security.getSystemSubject();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
    CreateRequest request =
        new CreateRequestImpl(Collections.singletonList(metacard), properties, destinations);
    CreateResponse response =
        new CreateResponseImpl(request, null, Collections.singletonList(createdMetacard));
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);
    String createdMetacardId = federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    assertThat(createdMetacardId, is(equalTo(RegistryObjectMetacardType.REGISTRY_ID)));
    verify(catalogFramework).create(any(CreateRequest.class));
  }

  @Test
  public void testAddRegistryEntryWithNullDestinations() throws Exception {
    String registryId = RegistryObjectMetacardType.REGISTRY_ID;
    Metacard metacard = testMetacard;
    Metacard createdMetacard = testMetacard;
    Set<String> destinations = null;
    Subject systemSubject = security.getSystemSubject();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
    CreateRequest request =
        new CreateRequestImpl(Collections.singletonList(metacard), properties, destinations);
    CreateResponse response =
        new CreateResponseImpl(request, null, Collections.singletonList(createdMetacard));
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);
    String createdMetacardId = federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    assertThat(createdMetacardId, is(equalTo(registryId)));
    verify(catalogFramework).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryWithNullMetacard() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = null;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryWithInvalidMetacardNoRegistryId() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = getTestMetacard();
    String s = null;
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_ID, s));
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryWithInvalidMetacardNoRegistryTag() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = getTestMetacard();
    metacard.setAttribute(new AttributeImpl(Metacard.TAGS, Collections.singletonList(null)));
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryWithIngestException() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.create(any(CreateRequest.class))).thenThrow(IngestException.class);
    federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryWithSourceUnavailableException() throws Exception {
    String destination = TEST_DESTINATION;
    Metacard metacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.create(any(CreateRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.addRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test
  public void testAddRegistryEntryMetacard() throws Exception {
    Metacard metacard = testMetacard;
    Metacard createdMetacard = testMetacard;
    Subject systemSubject = security.getSystemSubject();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
    CreateRequest request =
        new CreateRequestImpl(Collections.singletonList(metacard), properties, null);
    CreateResponse response =
        new CreateResponseImpl(request, null, Collections.singletonList(createdMetacard));
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);
    String createdMetacardId = federationAdminServiceImpl.addRegistryEntry(metacard);
    assertThat(createdMetacardId, is(equalTo(RegistryObjectMetacardType.REGISTRY_ID)));
    verify(catalogFramework).create(any(CreateRequest.class));
  }

  @Test
  public void testAddRegistryEntryStringWithDestinations() throws Exception {
    Metacard metacard = testMetacard;
    Metacard createdMetacard = testMetacard;

    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);

    Subject systemSubject = security.getSystemSubject();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
    CreateRequest request =
        new CreateRequestImpl(Collections.singletonList(metacard), properties, destinations);
    CreateResponse response =
        new CreateResponseImpl(request, null, Collections.singletonList(createdMetacard));

    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);

    String createdMetacardId =
        federationAdminServiceImpl.addRegistryEntry(TEST_XML_STRING, destinations);

    assertThat(createdMetacardId, is(equalTo(RegistryObjectMetacardType.REGISTRY_ID)));
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework).create(any(CreateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testAddRegistryEntryStringWithTransformerException() throws Exception {
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    when(registryTransformer.transform(any(InputStream.class)))
        .thenThrow(CatalogTransformerException.class);
    federationAdminServiceImpl.addRegistryEntry(TEST_XML_STRING, destinations);
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework, never()).create(any(CreateRequest.class));
  }

  @Test
  public void testAddRegistryEntryString() throws Exception {
    Metacard metacard = testMetacard;
    metacard.setAttribute(
        new AttributeImpl(
            Metacard.TAGS, Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
    Metacard createdMetacard = testMetacard;
    Subject systemSubject = security.getSystemSubject();
    Map<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, systemSubject);
    CreateRequest request =
        new CreateRequestImpl(Collections.singletonList(metacard), properties, null);
    CreateResponse response =
        new CreateResponseImpl(request, null, Collections.singletonList(createdMetacard));
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(catalogFramework.create(any(CreateRequest.class))).thenReturn(response);
    String createdMetacardId = federationAdminServiceImpl.addRegistryEntry(TEST_XML_STRING);
    assertThat(createdMetacardId, is(equalTo(RegistryObjectMetacardType.REGISTRY_ID)));
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework).create(any(CreateRequest.class));
  }

  @Test
  public void testUpdateRegistryEntry() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntrySourceUnavailale() throws Exception {
    Metacard metacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test
  public void testUpdateRegistryEntryWithNullDestinations() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = testMetacard;
    Set<String> destinations = null;
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test
  public void testGetBundleContextNullContext() {
    assertThat(federationAdminServiceImpl.getBundleContext(), is(nullValue()));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryWithNoRegistryId() throws Exception {
    Metacard metacard = getTestMetacard();
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework, never()).query(any(QueryRequest.class));
    verify(catalogFramework, never()).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryWithNoExistingMetacard() throws Exception {
    Metacard metacard = testMetacard;
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework, never()).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryWithMultipleMetacardMatches() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = getTestMetacard();
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response =
        getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard, getTestMetacard());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework, never()).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryWithIngestException() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = getTestMetacard();
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    when(catalogFramework.update(any(UpdateRequest.class))).thenThrow(IngestException.class);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryWithSourceUnavailableException() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = getTestMetacard();
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    when(catalogFramework.update(any(UpdateRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateRegistryEntryMetacard() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = testMetacard;
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard);
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateRegistryEntryStringWithDestinations() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = getTestMetacard();
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(TEST_XML_STRING, destinations);
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testUpdateRegistryEntryStringWithTransformerException() throws Exception {
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    when(registryTransformer.transform(any(InputStream.class)))
        .thenThrow(CatalogTransformerException.class);
    federationAdminServiceImpl.updateRegistryEntry(TEST_XML_STRING, destinations);
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework, never()).query(any(QueryRequest.class));
    verify(catalogFramework, never()).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateRegistryEntryString() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = getTestMetacard();
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(registryTransformer.transform(any(InputStream.class))).thenReturn(metacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(TEST_XML_STRING);
    verify(registryTransformer).transform(any(InputStream.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test
  public void testUpdateRegistryWithTransientAttributes() throws Exception {
    Metacard metacard = testMetacard;
    Metacard existingMetacard = testMetacard;
    metacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true));
    metacard.setAttribute(new AttributeImpl(RegistryObjectMetacardType.REGISTRY_LOCAL_NODE, true));
    metacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, "publishedHere"));
    existingMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, false));
    existingMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.LAST_PUBLISHED, java.time.Instant.now()));
    List<String> values = new ArrayList<>();
    values.add("firstValue");
    values.add("secondValue");
    existingMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.PUBLISHED_LOCATIONS, (Serializable) values));
    Set<String> destinations = new HashSet<>();
    destinations.add(TEST_DESTINATION);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), existingMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.updateRegistryEntry(metacard, destinations);
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(catalogFramework).update(any(UpdateRequest.class));
  }

  @Test
  public void testDeleteRegistryEntriesByRegistryIds() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    QueryResponse response = getPopulatedTestQueryResponse(getTestQueryRequest(), testMetacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByRegistryIdsWithEmptyList() throws Exception {
    List<String> ids = new ArrayList<>();
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(ids, destinations);
    verify(catalogFramework, never()).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByRegistryIdsWithSourceUnavailableException()
      throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.delete(any(DeleteRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByRegistryIdsWithIngestException() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
    federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test
  public void testDeleteRegistryEntriesByRegistryIdsNoDestinations() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    federationAdminServiceImpl.deleteRegistryEntriesByRegistryIds(ids);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test
  public void testDeleteRegistryEntriesByMetacardIds() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.deleteRegistryEntriesByMetacardIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByMetacardIdsWithEmptyList() throws Exception {
    List<String> ids = new ArrayList<>();
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    federationAdminServiceImpl.deleteRegistryEntriesByMetacardIds(ids, destinations);
    verify(catalogFramework, never()).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByMetacardIdsWithSourceUnavailableException()
      throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.delete(any(DeleteRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.deleteRegistryEntriesByMetacardIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testDeleteRegistryEntriesByMetacardIdsWithIngestException() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    when(catalogFramework.delete(any(DeleteRequest.class))).thenThrow(IngestException.class);
    federationAdminServiceImpl.deleteRegistryEntriesByMetacardIds(ids, destinations);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test
  public void testDeleteRegistryEntriesByMetacardIdsNoDestinations() throws Exception {
    String firstId = RegistryObjectMetacardType.REGISTRY_ID;
    List<String> ids = new ArrayList<>();
    ids.add(firstId);
    federationAdminServiceImpl.deleteRegistryEntriesByMetacardIds(ids);
    verify(catalogFramework).delete(any(DeleteRequest.class));
  }

  @Test
  public void testGetRegistryMetacards() throws Exception {
    Metacard findThisMetacard = testMetacard;
    findThisMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true));
    QueryRequest request = getTestQueryRequest();
    QueryResponse response =
        getPopulatedTestQueryResponse(request, findThisMetacard, getTestMetacard());
    when(security.getSystemSubject()).thenReturn(subject);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<Metacard> metacards = federationAdminServiceImpl.getRegistryMetacards();
    assertThat(metacards, hasSize(2));
  }

  @Test
  public void testGetRegistryMetacardsWithDestinations() throws Exception {
    String destination = TEST_DESTINATION;
    Set<String> destinations = new HashSet<>();
    destinations.add(destination);
    Metacard findThisMetacard = testMetacard;
    findThisMetacard.setAttribute(
        new AttributeImpl(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true));
    QueryRequest request = getTestQueryRequest();
    QueryResponse response =
        getPopulatedTestQueryResponse(request, findThisMetacard, getTestMetacard());
    when(security.getSystemSubject()).thenReturn(subject);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<Metacard> metacards = federationAdminServiceImpl.getRegistryMetacards(destinations);
    assertThat(metacards, hasSize(2));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsWithUnsupportedQueryException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(UnsupportedQueryException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsWithSourceUnavailableException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(SourceUnavailableException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsWithFederationException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(FederationException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test
  public void testGetLocalRegistryMetacards() throws Exception {
    QueryRequest request = getTestQueryRequest();
    QueryResponse response =
        getPopulatedTestQueryResponse(request, getTestMetacard(), getTestMetacard());
    when(security.getSystemSubject()).thenReturn(subject);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<Metacard> metacards = federationAdminServiceImpl.getLocalRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
    assertThat(metacards, hasSize(2));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsWithUnsupportedQueryException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(UnsupportedQueryException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getLocalRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsWithSourceUnavailableException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(SourceUnavailableException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getLocalRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsWithFederationException() throws Exception {
    when(security.getSystemSubject()).thenReturn(subject);
    doThrow(FederationException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getLocalRegistryMetacards();
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test
  public void testGetRegistryMetacardsByRegistryIds() throws Exception {
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    QueryRequest request = getTestQueryRequest();
    QueryResponse response =
        getPopulatedTestQueryResponse(
            request, getTestMetacard(), getTestMetacard(), getTestMetacard());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    List<Metacard> metacards = federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids);
    assertThat(metacards, hasSize(3));
    verify(catalogFramework).query(captor.capture());

    assertThat(
        "Filter didn't contain the 'registry' and 'registry-remote' tag",
        filterAdapter.adapt(
            captor.getValue().getQuery(), new TagsFilterDelegate(RegistryConstants.REGISTRY_TAG)),
        is(true));
  }

  @Test
  public void testGetRegistryMetacardsByRegistryIdsIncludeInternal() throws Exception {
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, getTestMetacard());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids, true);
    verify(catalogFramework).query(captor.capture());

    HashSet<String> tags = new HashSet<>();
    tags.add(RegistryConstants.REGISTRY_TAG);
    tags.add(RegistryConstants.REGISTRY_TAG_INTERNAL);
    assertThat(
        "Filter didn't contain the 'registry' and 'registry-remote' tag",
        filterAdapter.adapt(captor.getValue().getQuery(), new TagsFilterDelegate(tags)),
        is(true));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsByRegistryIdsWithEmptyList() throws Exception {
    List<String> ids = new ArrayList<>();
    federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework, never()).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsByRegistryIdsWithSourceUnavailableException()
      throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsByRegistryIdsWithUnsupportedQueryException()
      throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(UnsupportedQueryException.class);
    federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryMetacardsByRegistryIdsWithFederationException() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class))).thenThrow(FederationException.class);
    federationAdminServiceImpl.getRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test
  public void testGetLocalRegistryMetacardsByRegistryIds() throws Exception {
    QueryRequest request = getTestQueryRequest();
    QueryResponse response =
        getPopulatedTestQueryResponse(
            request, getTestMetacard(), getTestMetacard(), getTestMetacard());
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    List<Metacard> metacards =
        federationAdminServiceImpl.getLocalRegistryMetacardsByRegistryIds(ids);
    assertThat(metacards, hasSize(3));
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsByRegistryIdsWithEmptyList() throws Exception {
    List<String> ids = new ArrayList<>();
    federationAdminServiceImpl.getLocalRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework, never()).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsByRegistryIdsWithSourceUnavailableException()
      throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(SourceUnavailableException.class);
    federationAdminServiceImpl.getLocalRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsByRegistryIdsWithUnsupportedQueryException()
      throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class)))
        .thenThrow(UnsupportedQueryException.class);
    federationAdminServiceImpl.getLocalRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryMetacardsByRegistryIdsWithFederationException() throws Exception {
    List<String> ids = new ArrayList<>();
    ids.add(RegistryObjectMetacardType.REGISTRY_ID);
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "1");
    ids.add(RegistryObjectMetacardType.REGISTRY_ID + "2");
    when(catalogFramework.query(any(QueryRequest.class))).thenThrow(FederationException.class);
    federationAdminServiceImpl.getLocalRegistryMetacardsByRegistryIds(ids);
    verify(catalogFramework).query(any(QueryRequest.class));
  }

  @Test
  public void testGetLocalRegistryObjects() throws Exception {
    Metacard localMetacardOne = testMetacard;
    Metacard localMetacardTwo = testMetacard;
    List<Metacard> localMetacards = new ArrayList<>();
    localMetacards.add(localMetacardOne);
    localMetacards.add(localMetacardTwo);
    doReturn(localMetacards).when(federationAdminServiceImpl).getLocalRegistryMetacards();
    JAXBElement<RegistryPackageType> jaxbRegistryPackage =
        EbrimConstants.RIM_FACTORY.createRegistryPackage(getTestRegistryPackage());
    when(parser.unmarshal(
            any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class)))
        .thenReturn(jaxbRegistryPackage);
    List<RegistryPackageType> packages = federationAdminServiceImpl.getLocalRegistryObjects();
    assertThat(packages, hasSize(2));
    verify(parser, times(2))
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryObjectsWithEmptyMetadata() throws Exception {
    Metacard localMetacard = getTestMetacard();
    localMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, ""));
    List<Metacard> localMetacards = new ArrayList<>();
    localMetacards.add(localMetacard);
    doReturn(localMetacards).when(federationAdminServiceImpl).getLocalRegistryMetacards();
    federationAdminServiceImpl.getLocalRegistryObjects();
    verify(parser, never())
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryObjectsWithQueryException() throws Exception {
    doThrow(FederationAdminException.class)
        .when(federationAdminServiceImpl)
        .getLocalRegistryMetacards();
    federationAdminServiceImpl.getLocalRegistryObjects();
    verify(parser, never())
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetLocalRegistryObjectsWithParserException() throws Exception {
    Metacard localMetacard = testMetacard;
    List<Metacard> localMetacards = new ArrayList<>();
    localMetacards.add(localMetacard);
    doReturn(localMetacards).when(federationAdminServiceImpl).getLocalRegistryMetacards();
    when(parser.unmarshal(
            any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class)))
        .thenThrow(ParserException.class);
    federationAdminServiceImpl.getLocalRegistryObjects();
    verify(parser)
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test
  public void testGetLocalRegistryObjectsWithNoRegistryEntries() throws Exception {
    List<Metacard> localMetacards = new ArrayList<>();
    doReturn(localMetacards).when(federationAdminServiceImpl).getLocalRegistryMetacards();
    List<RegistryPackageType> packages = federationAdminServiceImpl.getLocalRegistryObjects();
    assertThat(packages, empty());
    verify(parser, never())
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test
  public void testGetRegistryObjects() throws Exception {
    Metacard metacardOne = testMetacard;
    Metacard metacardTwo = testMetacard;
    JAXBElement<RegistryPackageType> jaxbRegistryPackage =
        EbrimConstants.RIM_FACTORY.createRegistryPackage(getTestRegistryPackage());
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, metacardOne, metacardTwo);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    when(parser.unmarshal(
            any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class)))
        .thenReturn(jaxbRegistryPackage);
    List<RegistryPackageType> regObjects = federationAdminServiceImpl.getRegistryObjects();
    assertThat(regObjects, hasSize(2));
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(parser, times(2))
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryObjectsWithEmptyMetadata() throws Exception {
    Metacard metacard = getTestMetacard();
    metacard.setAttribute(new AttributeImpl(Metacard.METADATA, ""));
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, metacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    federationAdminServiceImpl.getRegistryObjects();
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(parser, never())
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryObjectsWithQueryException() throws Exception {
    doThrow(FederationAdminException.class).when(catalogFramework).query(any(QueryRequest.class));
    federationAdminServiceImpl.getRegistryObjects();
    // throws exception
    catalogFramework.query(any(QueryRequest.class));
  }

  @Test(expected = FederationAdminException.class)
  public void testGetRegistryObjectsWithParserException() throws Exception {
    Metacard metacard = testMetacard;
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request, metacard);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    doThrow(ParserException.class)
        .when(parser)
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
    federationAdminServiceImpl.getRegistryObjects();
    // throws exception
    catalogFramework.query(any(QueryRequest.class));
  }

  @Test
  public void testGetRegistryObjectsWithNoRegistryEntries() throws Exception {
    QueryRequest request = getTestQueryRequest();
    QueryResponse response = getPopulatedTestQueryResponse(request);
    when(catalogFramework.query(any(QueryRequest.class))).thenReturn(response);
    List<RegistryPackageType> packages = federationAdminServiceImpl.getRegistryObjects();
    assertThat(packages, empty());
    verify(catalogFramework).query(any(QueryRequest.class));
    verify(parser, never())
        .unmarshal(any(ParserConfigurator.class), eq(JAXBElement.class), any(InputStream.class));
  }

  private Metacard getTestMetacard() {
    return new MetacardImpl(new RegistryObjectMetacardType());
  }

  private Filter getTestFilter() {
    return FILTER_FACTORY.and(
        FILTER_FACTORY.like(
            FILTER_FACTORY.property(Metacard.CONTENT_TYPE),
            RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME),
        FILTER_FACTORY.like(
            FILTER_FACTORY.property(Metacard.TAGS), RegistryConstants.REGISTRY_TAG));
  }

  private QueryRequest getTestQueryRequest() {
    Filter filter = getTestFilter();
    SortBy sortBy = FILTER_FACTORY.sort(Core.CREATED, SortOrder.ASCENDING);
    Query query = new QueryImpl(filter);
    ((QueryImpl) query).setSortBy(sortBy);
    QueryRequest request = new QueryRequestImpl(query);
    request.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
    return request;
  }

  private QueryResponse getPopulatedTestQueryResponse(QueryRequest request, Metacard... metacards) {
    List<Result> results = new ArrayList<>();
    for (Metacard metacard : metacards) {
      results.add(new ResultImpl(metacard));
    }

    return getTestQueryResponse(request, results);
  }

  private QueryResponse getTestQueryResponse(QueryRequest request, List<Result> results) {
    return new QueryResponseImpl(request, results, results.size());
  }

  private RegistryPackageType getTestRegistryPackage() {
    return EbrimConstants.RIM_FACTORY.createRegistryPackageType();
  }

  private Metacard getPopulatedTestRegistryMetacard() {
    Metacard registryMetacard = getTestMetacard();
    registryMetacard.setAttribute(
        new AttributeImpl(
            RegistryObjectMetacardType.REGISTRY_ID, RegistryObjectMetacardType.REGISTRY_ID));
    registryMetacard.setAttribute(new AttributeImpl(Metacard.MODIFIED, new Date()));
    registryMetacard.setAttribute(
        new AttributeImpl(
            Metacard.TAGS, Collections.singletonList(RegistryConstants.REGISTRY_TAG)));
    registryMetacard.setAttribute(new AttributeImpl(Metacard.ID, TEST_METACARD_ID));
    registryMetacard.setAttribute(new AttributeImpl(Metacard.METADATA, TEST_XML_STRING));
    return registryMetacard;
  }
}
