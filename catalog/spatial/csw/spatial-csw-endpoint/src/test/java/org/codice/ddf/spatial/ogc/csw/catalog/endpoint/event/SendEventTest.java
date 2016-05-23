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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSubscribe;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.InvalidSyntaxException;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.security.Subject;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

public class SendEventTest {

    private Security mockSecurity;

    private URL callbackURI;

    private GetRecordsType request;

    private QueryRequest query;

    private TransformerManager transformerManager;

    private SendEventExtension sendEvent;

    private Metacard metacard;

    private QueryType queryType;

    private ElementSetNameType elementSetNameType;

    private QueryResponseTransformer transformer;

    private BinaryContent binaryContent;

    private WebClient webclient;

    private SecureCxfClientFactory<CswSubscribe> mockCxfClientFactory;

    private Response response;

    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    private List<AccessPlugin> accessPlugins = new ArrayList<>();

    private Subject subject;

    @Before
    public void setUp() throws Exception {
        System.setProperty("ddf.home", ".");
        callbackURI = new URL("https://localhost:12345/services/csw/subscription/event");
        ObjectFactory objectFactory = new ObjectFactory();
        request = new GetRecordsType();
        request.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        request.setResultType(ResultType.RESULTS);
        request.getResponseHandler()
                .add(callbackURI.toString());
        queryType = new QueryType();
        elementSetNameType = new ElementSetNameType();
        elementSetNameType.setValue(ElementSetType.BRIEF);
        queryType.setElementSetName(elementSetNameType);
        request.setAbstractQuery(objectFactory.createAbstractQuery(queryType));
        transformerManager = mock(TransformerManager.class);
        transformer = mock(QueryResponseTransformer.class);
        binaryContent = mock(BinaryContent.class);
        when(transformerManager.getTransformerBySchema(Matchers.contains(CswConstants.CSW_OUTPUT_SCHEMA))).thenReturn(
                transformer);
        when(transformer.transform(any(SourceResponse.class), anyMap())).thenReturn(binaryContent);
        when(binaryContent.getByteArray()).thenReturn("byte array with message contents".getBytes());
        query = mock(QueryRequest.class);

        metacard = mock(Metacard.class);
        webclient = mock(WebClient.class);
        mockCxfClientFactory = mock(SecureCxfClientFactory.class);
        response = mock(Response.class);
        subject = mock(Subject.class);

        mockSecurity = mock(Security.class);
        headers.put(Subject.class.toString(), Arrays.asList(new Subject[] {subject}));
        AccessPlugin accessPlugin = mock(AccessPlugin.class);
        accessPlugins.add(accessPlugin);
        when(mockCxfClientFactory.getWebClient()).thenReturn(webclient);
        //        when(webclient.head()).thenReturn(response);
        when(webclient.invoke(anyString(), any(QueryResponse.class))).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(accessPlugin.processPostQuery(any(QueryResponse.class))).thenAnswer(new Answer<QueryResponse>() {
            @Override
            public QueryResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (QueryResponse) invocationOnMock.getArguments()[0];
            }
        });

        sendEvent = new SendEventExtension(transformerManager,
                request,
                query,
                mockCxfClientFactory);
        sendEvent.setSubject(subject);

    }

    public void verifyResults() throws Exception {
        verify(webclient, times(1)).invoke(anyString(), anyObject());
    }

    @Test
    public void testCreated() throws Exception {
        sendEvent.created(metacard);
        verifyResults();

    }

    @Test
    public void testUpdatedHit() throws Exception {
        sendEvent.updatedHit(metacard, metacard);
        verifyResults();
    }

    @Test
    public void testUpdatedMiss() throws Exception {
        sendEvent.updatedMiss(metacard, metacard);
        verifyResults();
    }

    @Test
    public void testDeleted() throws Exception {
        sendEvent.deleted(metacard);
        verifyResults();
    }

    @Test
    public void testIsAvailableRetryBackoff() throws Exception {
        when(webclient.invoke(eq("HEAD"), isNull())).thenThrow(new RuntimeException("test"));
        sendEvent.setSubject(subject);
        long lastPing = sendEvent.getLastPing();
        boolean available = true;
        //loop until we get to a backoff that will be long enough not to cause intermitent test failures
        while (sendEvent.getRetryCount() < 7) {
            available = sendEvent.ping();
        }
        assertFalse(available);
        assertNotEquals(lastPing, sendEvent.getLastPing());
        lastPing = sendEvent.getLastPing();
        //run again this time within a backoff period and verify that it doesn't retry this is
        available = sendEvent.ping();
        assertFalse(available);
        assertEquals(7, sendEvent.getRetryCount());
        assertEquals(lastPing, sendEvent.getLastPing());

    }

    @Test
    public void testIsAvailableSubjectExperation() throws Exception {
        when(webclient.invoke(eq("HEAD"), isNull())).thenReturn(response);
        when(mockSecurity.getExpires(subject)).thenReturn(new Date(
                System.currentTimeMillis() + 600000L))
                .thenReturn(new Date(System.currentTimeMillis() + 600000L))
                .thenReturn(new Date());
        sendEvent.setSubject(subject);
        boolean available = false;
        while (!sendEvent.ping()) {

        }
        long lastPing = sendEvent.getLastPing();
        //sleep incase the test runs too fast we want to make sure their is a time difference
        Thread.sleep(1);
        //run within the expiration period of the assertion
        available = sendEvent.ping();
        assertTrue(available);
        assertEquals(lastPing, sendEvent.getLastPing());
        //sleep incase the test runs too fast we want to make sure their is a time difference
        Thread.sleep(1);
        //run with expired assertion
        available = sendEvent.ping();
        assertTrue(available);
        assertNotEquals(lastPing, sendEvent.getLastPing());
    }

    @Test
    public void testIsAvailableNoExperation() throws Exception {
        long lastPing = sendEvent.getLastPing();
        when(webclient.invoke(eq("HEAD"), isNull())).thenReturn(response);
        boolean available = false;
        while (!sendEvent.ping()) {

        }
        assertNotEquals(lastPing, sendEvent.getLastPing());
        lastPing = sendEvent.getLastPing();
        //run within the expiration period of the assertion
        available = sendEvent.ping();
        assertTrue(available);
        assertEquals(lastPing, sendEvent.getLastPing());
    }

    private class SendEventExtension extends SendEvent {

        public SendEventExtension(TransformerManager transformerManager, GetRecordsType request,
                QueryRequest query, SecureCxfClientFactory<CswSubscribe> mockCxfClientFactory)
                throws CswException {
            super(transformerManager, request, query);
            super.cxfClientFactory = mockCxfClientFactory;
            super.security = mockSecurity;
        }

        List<AccessPlugin> getAccessPlugins() throws InvalidSyntaxException {
            return accessPlugins;
        }

        public void setSubject(Subject subject) {
            super.subject = subject;
        }
    }
}