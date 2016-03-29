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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.cxf.SecureCxfClientFactory;
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

    private URL callbackURI;

    private GetRecordsType request;

    private QueryRequest query;

    private TransformerManager transformerManager;

    private SendEvent sendEvent;

    private Metacard metacard;

    private QueryType queryType;

    private ElementSetNameType elementSetNameType;

    private QueryResponseTransformer transformer;

    private BinaryContent binaryContent;

    private WebClient webclient;

    private SecureCxfClientFactory<CswSubscribe> cxfClientFactory;

    private Response response;

    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();

    private List<AccessPlugin> accessPlugins = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
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
        cxfClientFactory = mock(SecureCxfClientFactory.class);
        response = mock(Response.class);
        headers.put(Subject.class.toString(), Arrays.asList(new Subject[] {mock(Subject.class)}));
        AccessPlugin accessPlugin = mock(AccessPlugin.class);
        accessPlugins.add(accessPlugin);
        when(cxfClientFactory.getWebClient()).thenReturn(webclient);
        when(webclient.head()).thenReturn(response);
        when(webclient.invoke(anyString(), any(QueryResponse.class))).thenReturn(response);
        when(response.getHeaders()).thenReturn(headers);
        when(accessPlugin.processPostQuery(any(QueryResponse.class))).thenAnswer(new Answer<QueryResponse>() {
            @Override
            public QueryResponse answer(InvocationOnMock invocationOnMock) throws Throwable {
                return (QueryResponse) invocationOnMock.getArguments()[0];
            }
        });

        sendEvent = new SendEventExtension(transformerManager, request, query, cxfClientFactory);

    }

    public void verifyResults() throws Exception {
        verify(webclient, times(2)).invoke(anyString(), anyObject());
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

    private class SendEventExtension extends SendEvent {

        public SendEventExtension(TransformerManager transformerManager, GetRecordsType request,
                QueryRequest query, SecureCxfClientFactory<CswSubscribe> cxfClientFactory)
                throws CswException {
            super(transformerManager, request, query);
            this.cxfClientFactory = cxfClientFactory;
        }

        List<AccessPlugin> getAccessPlugins() throws InvalidSyntaxException {
            return accessPlugins;
        }
    }
}