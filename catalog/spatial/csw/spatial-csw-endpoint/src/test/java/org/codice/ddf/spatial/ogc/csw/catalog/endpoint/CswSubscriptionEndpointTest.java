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
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event.CswSubscription;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import ch.qos.logback.classic.Level;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.EventProcessor;
import ddf.catalog.event.Subscription;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.security.SecurityConstants;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;

public class CswSubscriptionEndpointTest {
    private static final ch.qos.logback.classic.Logger CSW_LOGGER =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(CswEndpoint.class);

    private static final String RESPONSE_HANDLER_URL = "https://somehost:12345/test";

    private static final String VALID_TYPES = "csw:Record,csw:Record";

    private static final String METACARD_SCHEMA = "urn:catalog:metacard";

    private static BundleContext mockContext;

    CswSubscriptionEndpoint cswSubscriptionEndpoint;

    private TransformerManager mockMimeTypeManager;

    private TransformerManager mockSchemaManager;

    private Validator validator;

    private CswQueryFactory queryFactory;

    private QueryRequest query;

    private ServiceRegistration serviceRegistration;

    private ServiceReference subscriptionReference;

    private Bundle bundle;

    private Filter osgiFilter;

    private ServiceReference configAdminRef;

    private ConfigurationAdmin configAdmin;

    private Configuration config;

    private Long bundleId = 42L;

    private static final String FILTER_STR = "filter serialized to a string";

    private CswSubscription subscription;

    String subscriptionId = "urn:uuid:1234";

    private GetRecordsRequest defaultRequest;

    private EventProcessor eventProcessor;

    private TransformerManager mockInputManager;

    File systemKeystoreFile = null;

    File systemTruststoreFile = null;

    String password = "changeit";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
        FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
        InputStream systemKeyStream = CswSubscriptionEndpointTest.class.getResourceAsStream(
                "/serverKeystore.jks");
        IOUtils.copy(systemKeyStream, systemKeyOutStream);

        systemTruststoreFile = temporaryFolder.newFile("serverTruststore.jks");
        FileOutputStream systemTrustOutStream = new FileOutputStream(systemTruststoreFile);
        InputStream systemTrustStream = CswSubscriptionEndpointTest.class.getResourceAsStream(
                "/serverTruststore.jks");
        IOUtils.copy(systemTrustStream, systemTrustOutStream);

        System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
        System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "jks");
        System.setProperty("ddf.home", "");
        System.setProperty(SecurityConstants.KEYSTORE_PATH, systemKeystoreFile.getAbsolutePath());
        System.setProperty(SecurityConstants.TRUSTSTORE_PATH,
                systemTruststoreFile.getAbsolutePath());
        System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, password);
        System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, password);

        eventProcessor = mock(EventProcessor.class);
        mockInputManager = mock(TransformerManager.class);
        mockContext = mock(BundleContext.class);
        mockMimeTypeManager = mock(TransformerManager.class);
        mockSchemaManager = mock(TransformerManager.class);
        validator = mock(Validator.class);
        queryFactory = mock(CswQueryFactory.class);
        query = mock(QueryRequest.class);
        when(queryFactory.getQuery(any(GetRecordsType.class))).thenReturn(query);

        serviceRegistration = mock(ServiceRegistration.class);
        subscriptionReference = mock(ServiceReference.class);
        bundle = mock(Bundle.class);
        osgiFilter = mock(org.osgi.framework.Filter.class);
        configAdminRef = mock(ServiceReference.class);
        configAdmin = mock(ConfigurationAdmin.class);
        config = mock(Configuration.class);
        Configuration[] configArry = {config};

        defaultRequest = createDefaultGetRecordsRequest();
        subscription = new CswSubscription(defaultRequest.get202RecordsType(),
                query,
                mockMimeTypeManager);

        when(osgiFilter.toString()).thenReturn(FILTER_STR);
        doReturn(serviceRegistration).when(mockContext)
                .registerService(eq(Subscription.class.getName()),
                        any(Subscription.class),
                        any(Dictionary.class));
        doReturn(configAdminRef).when(mockContext)
                .getServiceReference(eq(ConfigurationAdmin.class.getName()));
        when(serviceRegistration.getReference()).thenReturn(subscriptionReference);
        doReturn(bundle).when(subscriptionReference)
                .getBundle();
        when(subscriptionReference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(bundleId);
        when(mockContext.createFilter(anyString())).thenReturn(osgiFilter);
        when(mockContext.getService(eq(configAdminRef))).thenReturn(configAdmin);
        when(mockContext.getService(eq(subscriptionReference))).thenReturn(subscription);
        when(configAdmin.listConfigurations(eq(FILTER_STR))).thenReturn(configArry);
        when(configAdmin.createFactoryConfiguration(anyString(), isNull(String.class))).thenReturn(
                config);

        cswSubscriptionEndpoint = new CswSubscriptionEndpointStub(eventProcessor,
                mockMimeTypeManager,
                mockSchemaManager,
                mockInputManager,
                validator,
                queryFactory,
                mockContext);
    }

    @Test
    public void testDeleteRecordsSubscription() throws Exception {
        // TODO: fix
        //        cswSubscriptionEndpoint.addOrUpdateSubscription(defaultRequest.get202RecordsType(), true);
        Response response = cswSubscriptionEndpoint.deleteRecordsSubscription(subscriptionId);
        assertThat(Response.Status.OK.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));
        verify(serviceRegistration).unregister();
        verify(config).delete();

    }

    @Test
    public void testDeleteRecordsSubscriptionNoSubscription() throws Exception {
        String requestId = "requestId";
        Response response = cswSubscriptionEndpoint.deleteRecordsSubscription(requestId);
        assertThat(Response.Status.NOT_FOUND.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));

    }

    @Test
    public void testGetRecordsSubscriptionNoSubscription() throws Exception {
        String requestId = "requestId";
        Response response = cswSubscriptionEndpoint.getRecordsSubscription(requestId);
        assertThat(Response.Status.NOT_FOUND.getStatusCode(),
                is(response.getStatusInfo()
                        .getStatusCode()));

    }

    @Test
    public void testGetRecordsSubscription() throws Exception {
        // TODO: fix
        //        cswSubscriptionEndpoint.addOrUpdateSubscription(defaultRequest.get202RecordsType(), true);
        Response response = cswSubscriptionEndpoint.getRecordsSubscription(subscriptionId);
        AcknowledgementType getAck = (AcknowledgementType) response.getEntity();
        assertThat(defaultRequest.get202RecordsType(),
                is(((JAXBElement<GetRecordsType>) getAck.getEchoedRequest()
                        .getAny()).getValue()));
        verify(mockContext).getService(eq(subscriptionReference));

    }

    @Test
    public void testUpdateRecordsSubscription() throws Exception {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(RESPONSE_HANDLER_URL);
        Response response = cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest);
        AcknowledgementType createAck = (AcknowledgementType) response.getEntity();
        getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(RESPONSE_HANDLER_URL);
        getRecordsRequest.setResultType(ResultType.HITS.value());
        response = cswSubscriptionEndpoint.updateRecordsSubscription(createAck.getRequestId(),
                getRecordsRequest.get202RecordsType());
        AcknowledgementType updateAck = (AcknowledgementType) response.getEntity();
        assertThat(((GetRecordsType) ((JAXBElement) updateAck.getEchoedRequest()
                .getAny()).getValue()).getResultType(), is(ResultType.HITS));
        verify(serviceRegistration).unregister();
        verify(config).delete();
        verify(mockContext, times(2)).registerService(eq(Subscription.class.getName()),
                any(Subscription.class),
                any(Dictionary.class));

    }

    @Test
    public void testCreateRecordsSubscriptionGET() throws Exception {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(RESPONSE_HANDLER_URL);
        getRecordsRequest.setVersion("");
        Response response = cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest);
        AcknowledgementType createAck = (AcknowledgementType) response.getEntity();
        assertThat(createAck, notNullValue());
        verify(mockContext).registerService(eq(Subscription.class.getName()),
                any(Subscription.class),
                any(Dictionary.class));

    }

    @Test
    public void testCreateRecordsSubscriptionPOST() throws Exception {
        CSW_LOGGER.setLevel(Level.DEBUG);
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(RESPONSE_HANDLER_URL);
        Response response =
                cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest.get202RecordsType());
        AcknowledgementType createAck = (AcknowledgementType) response.getEntity();
        assertThat(createAck, notNullValue());
        assertThat(createAck.getRequestId(), notNullValue());
        CSW_LOGGER.setLevel(Level.INFO);
        verify(mockContext).registerService(eq(Subscription.class.getName()),
                any(Subscription.class),
                any(Dictionary.class));

    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTwithoutResponseHandler() throws Exception {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler(null);
        cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest.get202RecordsType());

    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionGETNullRequest() throws CswException {
        cswSubscriptionEndpoint.createRecordsSubscription((GetRecordsRequest) null);

    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTNullRequest() throws CswException {
        cswSubscriptionEndpoint.createRecordsSubscription((GetRecordsType) null);

    }

    @Test(expected = CswException.class)
    public void testCreateRecordsSubscriptionPOSTBadResponseHandler() throws CswException {
        GetRecordsRequest getRecordsRequest = createDefaultGetRecordsRequest();
        getRecordsRequest.setResponseHandler("[]@!$&'()*+,;=");
        cswSubscriptionEndpoint.createRecordsSubscription(getRecordsRequest.get202RecordsType());

    }

    private GetRecordsRequest createDefaultGetRecordsRequest() {
        GetRecordsRequest grr = new GetRecordsRequest();
        grr.setRequestId(subscriptionId);
        grr.setResponseHandler(RESPONSE_HANDLER_URL);
        grr.setService(CswConstants.CSW);
        grr.setVersion(CswConstants.VERSION_2_0_2);
        grr.setRequest(CswConstants.GET_RECORDS);
        grr.setNamespace(CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.CSW_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.CSW_OUTPUT_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.OGC_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.OGC_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA

                + CswConstants.XMLNS_DEFINITION_PREFIX + CswConstants.GML_NAMESPACE_PREFIX
                + CswConstants.EQUALS + CswConstants.GML_SCHEMA
                + CswConstants.XMLNS_DEFINITION_POSTFIX + CswConstants.COMMA);

        grr.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        grr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
        grr.setTypeNames(VALID_TYPES);
        return grr;
    }

    @Test
    public void testCreateOrUpdateSubscriptionPersitanceFalse() throws Exception {
        ObjectFactory objectFactory = new ObjectFactory();
        GetRecordsType getRecordsType = createDefaultGetRecordsRequest().get202RecordsType();
        QueryType queryType = new QueryType();
        getRecordsType.setAbstractQuery(objectFactory.createQuery(queryType));

        // TODO: fix
        //        cswSubscriptionEndpoint.createOrUpdateSubscription(getRecordsType, subscriptionId, false);
        verify(mockContext).registerService(eq(Subscription.class.getName()),
                any(Subscription.class),
                any(Dictionary.class));
    }

    @Test
    public void testDeletedSubscription() throws Exception {
        assertThat(cswSubscriptionEndpoint.deleteSubscription(subscriptionId), is(false));
        // TODO: fix
        //        cswSubscriptionEndpoint.addOrUpdateSubscription(defaultRequest.get202RecordsType(), true);
        assertThat(cswSubscriptionEndpoint.deleteSubscription(subscriptionId), is(true));
        verify(serviceRegistration, times(1)).unregister();
        verify(config, times(2)).delete();
    }

    @Test
    public void testCreateEvent() throws Exception {
        cswSubscriptionEndpoint.createEvent(getRecordsResponse(1));
        verify(eventProcessor).notifyCreated(any(Metacard.class));
    }

    @Test
    public void testUpdateEvent() throws Exception {

        cswSubscriptionEndpoint.updateEvent(getRecordsResponse(2));
        verify(eventProcessor).notifyUpdated(any(Metacard.class), any(Metacard.class));

    }

    @Test
    public void testDeleteEvent() throws Exception {
        cswSubscriptionEndpoint.deleteEvent(getRecordsResponse(1));
        verify(eventProcessor).notifyDeleted(any(Metacard.class));
    }

    @Test(expected = CswException.class)
    public void testCreateEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.createEvent(getRecordsResponse);
    }

    @Test(expected = CswException.class)
    public void testUpdateEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.updateEvent(getRecordsResponse);
    }

    @Test(expected = CswException.class)
    public void testDeleteEventInvalidSchema() throws Exception {
        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        cswSubscriptionEndpoint.deleteEvent(getRecordsResponse);
    }

    private GetRecordsResponseType getRecordsResponse(int metacardCount)
            throws IOException, CatalogTransformerException {
        InputTransformer inputTransformer = mock(InputTransformer.class);
        when(mockInputManager.getTransformerBySchema(METACARD_SCHEMA)).thenReturn(inputTransformer);
        Metacard metacard = mock(Metacard.class);
        when(inputTransformer.transform(any(InputStream.class))).thenReturn(metacard);

        GetRecordsResponseType getRecordsResponse = new GetRecordsResponseType();
        SearchResultsType searchResults = new SearchResultsType();
        searchResults.setRecordSchema(METACARD_SCHEMA);
        getRecordsResponse.setSearchResults(searchResults);
        List<Object> any = new ArrayList<>();
        Node node = mock(Node.class);
        for (int i = 0; i < metacardCount; i++) {
            any.add(node);
        }
        searchResults.setAny(any);
        return getRecordsResponse;
    }

    public static class CswSubscriptionEndpointStub extends CswSubscriptionEndpoint {
        private final BundleContext bundleContext;

        public CswSubscriptionEndpointStub(EventProcessor eventProcessor,
                TransformerManager mimeTypeTransformerManager,
                TransformerManager schemaTransformerManager,
                TransformerManager inputTransformerManager, Validator validator,
                CswQueryFactory queryFactory, BundleContext context) {
            super(eventProcessor,
                    mimeTypeTransformerManager,
                    schemaTransformerManager,
                    inputTransformerManager,
                    validator,
                    queryFactory,
                    // TODO: fix
                    null);
            this.bundleContext = context;
        }

        @Override
        BundleContext getBundleContext() {
            return this.bundleContext;
        }
    }

}