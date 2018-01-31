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
package org.codice.ddf.registry.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.converters.Converter;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.encryption.EncryptionService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.InsertResultType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionSummaryType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.filter.v_1_1_0.FilterType;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.CswFilterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class RegistryStoreTest {

  private RegistryStoreImpl registryStore;

  private MetacardMarshaller marshaller;

  private Parser parser;

  private BundleContext context;

  private Converter provider;

  private CswSourceConfiguration cswSourceConfiguration;

  private SecureCxfClientFactory factory;

  private TransformerManager transformer;

  private ConfigurationAdmin configAdmin;

  private FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  private FilterBuilder filterBuilder = spy(new GeotoolsFilterBuilder());

  private Configuration configuration;

  private Subject subject;

  private EncryptionService encryptionService;

  private List<Result> queryResults;

  private Dictionary<String, Object> properties;

  private boolean availability = false;

  @Before
  public void setup() throws Exception {
    parser = new XmlParser();
    marshaller = new MetacardMarshaller(new XmlParser());
    context = mock(BundleContext.class);
    provider = mock(Converter.class);
    cswSourceConfiguration = new CswSourceConfiguration();
    factory = mock(SecureCxfClientFactory.class);
    transformer = mock(TransformerManager.class);
    encryptionService = mock(EncryptionService.class);
    configAdmin = mock(ConfigurationAdmin.class);
    configuration = mock(Configuration.class);
    subject = mock(Subject.class);
    queryResults = new ArrayList<>();
    registryStore =
        spy(
            new RegistryStoreImpl(
                context, cswSourceConfiguration, provider, factory, encryptionService) {
              @Override
              protected void validateOperation() {}

              @Override
              public boolean isAvailable() {
                return availability;
              }

              @Override
              protected SourceResponse query(
                  QueryRequest queryRequest,
                  ElementSetType elementSetName,
                  List<QName> elementNames,
                  Csw csw)
                  throws UnsupportedQueryException {
                if (queryResults == null) {
                  throw new UnsupportedQueryException("Test - Bad Query");
                }
                return new SourceResponseImpl(queryRequest, queryResults);
              }

              @Override
              protected CapabilitiesType getCapabilities() {
                return mock(CapabilitiesType.class);
              }

              @Override
              public void configureCswSource() {};

              @Override
              protected Subject getSystemSubject() {
                return subject;
              }

              @Override
              BundleContext getBundleContext() {
                return context;
              }
            });

    registryStore.setFilterBuilder(filterBuilder);
    registryStore.setFilterAdapter(filterAdapter);
    registryStore.setConfigAdmin(configAdmin);
    registryStore.setMetacardMarshaller(new MetacardMarshaller(parser));
    registryStore.setSchemaTransformerManager(transformer);
    registryStore.setAutoPush(true);
    registryStore.setRegistryUrl("http://test.url:0101/example");
    properties = new Hashtable<>();
    properties.put(RegistryStoreImpl.ID, "registryId");
    registryStore.setMetacardMarshaller(marshaller);

    when(configAdmin.getConfiguration(any())).thenReturn(configuration);
    when(configuration.getProperties()).thenReturn(properties);
  }

  @Test(expected = IngestException.class)
  public void testCreateNonRegistryMetacard() throws Exception {
    MetacardImpl mcard = getDefaultMetacard();
    mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, null);
    CreateRequest request = new CreateRequestImpl(mcard);
    registryStore.create(request);
  }

  @Test
  public void testCreateWithExistingMetacard() throws Exception {
    Metacard mcard = getDefaultMetacard();
    queryResults.add(new ResultImpl(mcard));
    CreateRequest request = new CreateRequestImpl(mcard);
    CreateResponse response = registryStore.create(request);
    assertThat(response.getCreatedMetacards().get(0), is(mcard));
  }

  @Test
  public void testCreateNoExistingMetacard() throws Exception {
    Metacard mcard = getDefaultMetacard();
    Csw csw = mock(Csw.class);
    TransactionResponseType responseType = mock(TransactionResponseType.class);
    InsertResultType insertResultType = mock(InsertResultType.class);
    BriefRecordType briefRecord = mock(BriefRecordType.class);
    JAXBElement identifier = mock(JAXBElement.class);
    SimpleLiteral literal = mock(SimpleLiteral.class);
    when(literal.getContent()).thenReturn(Collections.singletonList(mcard.getId()));
    when(identifier.getValue()).thenReturn(literal);
    when(briefRecord.getIdentifier()).thenReturn(Collections.singletonList(identifier));
    when(insertResultType.getBriefRecord()).thenReturn(Collections.singletonList(briefRecord));
    when(responseType.getInsertResult()).thenReturn(Collections.singletonList(insertResultType));
    when(factory.getClientForSubject(any())).thenReturn(csw);
    when(csw.transaction(any())).thenReturn(responseType);
    when(transformer.getTransformerIdForSchema(any())).thenReturn("myInsertType");
    queryResults.add(new ResultImpl(mcard));
    CreateRequest request = new CreateRequestImpl(mcard);
    CreateResponse response = registryStore.create(request);
    assertThat(response.getCreatedMetacards().get(0), is(mcard));
  }

  @Test
  public void testUpdate() throws Exception {
    Csw csw = mock(Csw.class);
    TransactionResponseType responseType = mock(TransactionResponseType.class);
    TransactionSummaryType tst = new TransactionSummaryType();
    tst.setTotalUpdated(new BigInteger("1"));
    when(responseType.getTransactionSummary()).thenReturn(tst);
    when(factory.getClientForSubject(any())).thenReturn(csw);
    when(csw.transaction(any())).thenReturn(responseType);
    when(transformer.getTransformerIdForSchema(any())).thenReturn(null);
    UpdateRequestImpl request = new UpdateRequestImpl("testId", getDefaultMetacard());
    MetacardImpl updatedMcard = getDefaultMetacard();
    updatedMcard.setId("newTestId");
    OperationTransactionImpl opTrans =
        new OperationTransactionImpl(
            OperationTransaction.OperationType.UPDATE, Collections.singletonList(updatedMcard));
    request.getProperties().put(Constants.OPERATION_TRANSACTION_KEY, opTrans);
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.update(request);
    assertThat(
        request.getUpdates().get(0).getValue().getMetadata().contains("value=\"newTestId\""),
        is(true));
  }

  @Test(expected = IngestException.class)
  public void testUpdateNonRegistryMetacard() throws Exception {
    MetacardImpl updatedMcard = getDefaultMetacard();
    updatedMcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, null);
    UpdateRequestImpl request = new UpdateRequestImpl("testId", updatedMcard);
    registryStore.update(request);
  }

  @Test
  public void registryInfoQuery() throws Exception {
    assertThat(registryStore.getRegistryId(), is(""));

    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.registryInfoQuery();
    ArgumentCaptor<QueryRequest> captor = ArgumentCaptor.forClass(QueryRequest.class);
    verify(registryStore).query(captor.capture());
    assertThat(
        captor.getValue().getPropertyValue(SecurityConstants.SECURITY_SUBJECT), notNullValue());

    assertThat(registryStore.getRegistryId(), is("registryId"));
  }

  @Test
  public void testUpdateConfigurationIOException() throws Exception {
    assertThat(registryStore.getRegistryId(), is(""));
    when(configAdmin.getConfiguration(any())).thenThrow(new IOException());
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.registryInfoQuery();
  }

  @Test
  public void testUpdateConfigurationURISyntaxException() throws Exception {
    assertThat(registryStore.getRegistryId(), is(""));
    registryStore.setRegistryUrl("^invalid^^^^uri^^");
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.registryInfoQuery();
    String registryId = (String) properties.get(RegistryObjectMetacardType.REGISTRY_ID);
    assertThat(registryId, is(nullValue()));
  }

  @Test
  public void testUpdateConfigurationUriNoPort() throws Exception {
    assertThat(registryStore.getRegistryId(), is(""));

    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.setRegistryUrl("http://testurl/example");
    registryStore.registryInfoQuery();
    assertThat(registryStore.getRegistryId(), is("registryId"));
  }

  @Test
  public void registryInfoQueryNoIdentityMetacard() throws Exception {
    assertThat(registryStore.getRegistryId(), is(""));

    registryStore.registryInfoQuery();

    assertThat(registryStore.getRegistryId(), is(""));
  }

  @Test
  public void testNonRegistryQuery() throws Exception {
    Filter filter =
        filterBuilder
            .attribute(RegistryObjectMetacardType.REGISTRY_ID)
            .is()
            .like()
            .text("registryId");
    QueryRequest testRequest = new QueryRequestImpl(new QueryImpl(filter));
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    SourceResponse answer = registryStore.query(testRequest);
    List<Result> testResults = answer.getResults();

    assertThat(testResults.size(), is(0));
  }

  @Test
  public void testRegistryQuery() throws Exception {
    Filter filter =
        filterBuilder.attribute(Metacard.TAGS).is().like().text(RegistryConstants.REGISTRY_TAG);
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    QueryRequest testRequest = new QueryRequestImpl(new QueryImpl(filter));
    SourceResponse answer = registryStore.query(testRequest);
    List<Result> testResults = answer.getResults();

    assertThat(testResults.size(), is(1));
  }

  @Test
  public void testRegistryQueryNullGetId() throws Exception {
    Filter filter =
        filterBuilder.attribute(Metacard.TAGS).is().like().text(RegistryConstants.REGISTRY_TAG);
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    QueryRequest testRequest = new QueryRequestImpl(new QueryImpl(filter));

    registryStore.setRegistryId("registryId");
    SourceResponse answer = registryStore.query(testRequest);
    List<Result> testResults = answer.getResults();
    assertThat(testResults.size(), is(1));
  }

  @Test
  public void testRegistryGetIdNotNull() throws Exception {
    Filter filter =
        filterBuilder.attribute(Metacard.TAGS).is().like().text(RegistryConstants.REGISTRY_TAG);
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    QueryRequest testRequest = new QueryRequestImpl(new QueryImpl(filter));

    registryStore.setRegistryId("registryId");
    registryStore.setId("d");
    SourceResponse answer = registryStore.query(testRequest);
    List<Result> testResults = answer.getResults();
    assertThat(testResults.size(), is(1));
  }

  @Test
  public void testDelete() throws Exception {
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    Csw csw = mock(Csw.class);
    TransactionResponseType transResponse = mock(TransactionResponseType.class);
    TransactionSummaryType transSummary = mock(TransactionSummaryType.class);
    when(transResponse.getTransactionSummary()).thenReturn(transSummary);
    when(transSummary.getTotalDeleted()).thenReturn(new BigInteger("1"));
    when(csw.transaction(any(CswTransactionRequest.class))).thenReturn(transResponse);

    when(factory.getClientForSubject(any())).thenReturn(csw);
    when(transformer.getTransformerIdForSchema(any())).thenReturn(null);
    FilterAdapter mockAdaptor = mock(FilterAdapter.class);
    CswFilterFactory filterFactory = new CswFilterFactory(CswAxisOrder.LAT_LON, false);
    FilterType filterType = filterFactory.buildPropertyIsLikeFilter(Metacard.ID, "testId", false);
    when(mockAdaptor.adapt(any(Filter.class), any(FilterDelegate.class))).thenReturn(filterType);
    registryStore.setFilterAdapter(mockAdaptor);
    DeleteRequestImpl request =
        new DeleteRequestImpl(
            Collections.singletonList(RegistryObjectMetacardType.REGISTRY_ID),
            "registryId",
            new HashMap<>());

    OperationTransactionImpl opTrans =
        new OperationTransactionImpl(
            OperationTransaction.OperationType.DELETE,
            Collections.singletonList(getDefaultMetacard()));
    request.getProperties().put(Constants.OPERATION_TRANSACTION_KEY, opTrans);
    registryStore.delete(request);

    verify(filterBuilder).attribute(captor.capture());
    assertThat(captor.getValue(), is("id"));
  }

  @Test
  public void testAvailableCommandNoIdentityNode() throws Exception {
    AvailabilityCommand command = registryStore.getAvailabilityCommand();
    assertThat(command.isAvailable(), is(false));
  }

  @Test
  public void testAvailableCommandQueryException() throws Exception {
    AvailabilityCommand command = registryStore.getAvailabilityCommand();
    queryResults = null;
    assertThat(command.isAvailable(), is(false));
  }

  @Test
  public void testAvailableCommand() throws Exception {
    AvailabilityCommand command = registryStore.getAvailabilityCommand();
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    assertThat(command.isAvailable(), is(true));
  }

  @Test
  public void testInit() throws Exception {
    RegistryStoreImpl registryStore =
        spy(
            new RegistryStoreImpl(
                context, cswSourceConfiguration, provider, factory, encryptionService) {
              @Override
              protected void validateOperation() {}

              @Override
              public boolean isAvailable() {
                return availability;
              }

              @Override
              protected SourceResponse query(
                  QueryRequest queryRequest,
                  ElementSetType elementSetName,
                  List<QName> elementNames,
                  Csw csw)
                  throws UnsupportedQueryException {
                if (queryResults == null) {
                  throw new UnsupportedQueryException("Test - Bad Query");
                }
                return new SourceResponseImpl(queryRequest, queryResults);
              }

              @Override
              public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
                return new SourceResponseImpl(
                    request,
                    Collections.singletonList(
                        new Result() {
                          @Override
                          public Metacard getMetacard() {
                            MetacardImpl metacard = new MetacardImpl();
                            metacard.setAttribute(
                                RegistryObjectMetacardType.REGISTRY_ID, "registryId");
                            metacard.setAttribute(Metacard.TITLE, "title");
                            return metacard;
                          }

                          @Override
                          public Double getRelevanceScore() {
                            return null;
                          }

                          @Override
                          public Double getDistanceInMeters() {
                            return null;
                          }
                        }));
              }

              @Override
              protected CapabilitiesType getCapabilities() {
                return mock(CapabilitiesType.class);
              }

              @Override
              public void configureCswSource() {};

              @Override
              protected Subject getSystemSubject() {
                return subject;
              }

              @Override
              BundleContext getBundleContext() {
                return context;
              }
            });

    registryStore.setFilterBuilder(filterBuilder);
    registryStore.setFilterAdapter(filterAdapter);
    registryStore.setConfigAdmin(configAdmin);
    registryStore.setMetacardMarshaller(new MetacardMarshaller(parser));
    registryStore.setSchemaTransformerManager(transformer);
    registryStore.setAutoPush(true);
    registryStore.setRegistryUrl("http://test.url:0101/example");
    properties = new Hashtable<>();
    properties.put(RegistryStoreImpl.ID, "registryId");
    registryStore.setMetacardMarshaller(marshaller);

    Csw csw = mock(Csw.class);
    when(factory.getClientForSubject(any())).thenReturn(csw);
    cswSourceConfiguration.setCswUrl("https://localhost");
    cswSourceConfiguration.setPollIntervalMinutes(1);
    queryResults.add(new ResultImpl(getDefaultMetacard()));
    registryStore.init();
    assertThat(registryStore.getRegistryId(), is("registryId"));
  }

  private MetacardImpl getDefaultMetacard() {
    InputStream inputStream = getClass().getResourceAsStream("/csw-full-registry-package.xml");
    BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
    String xml = buffer.lines().collect(Collectors.joining("\n"));
    MetacardImpl mcard = new MetacardImpl(new RegistryObjectMetacardType());
    mcard.setId("testId");
    mcard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
    mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "registryId");
    mcard.setContentTypeName(RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME);
    mcard.setMetadata(xml);
    mcard.setTitle("testRegistryMetacard");
    return mcard;
  }
}
