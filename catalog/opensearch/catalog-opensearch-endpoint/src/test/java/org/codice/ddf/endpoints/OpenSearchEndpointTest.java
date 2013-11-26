/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package org.codice.ddf.endpoints;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.codice.ddf.opensearch.query.OpenSearchQuery;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opengis.filter.Filter;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.AttributeBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;

public class OpenSearchEndpointTest {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(OpenSearchEndpointTest.class));

    /**
     * Test method for
     * {@link org.codice.ddf.endpoints.OpenSearchEndpoint#processQuery(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, javax.ws.rs.core.UriInfo, java.lang.String, java.lang.String)}
     * .
     * 
     * This test will verify that the string "local" in the sources passed to
     * OpenSearchEndpoint.processQuery is replaced with the local site name (in this case the mocked
     * name "TestSiteName"). The QueryRequest object is checked when the framework.query is called
     * to retrieve the OpenSearchQuery, which contains the Set of sites. An assertion within the
     * Answer object for the call framework.query checks that the sites Set is contains the
     * TEST_SITE_NAME.
     * 
     * @throws URISyntaxException
     * @throws FederationException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws UnsupportedEncodingException
     * @throws CatalogTransformerException
     */
    @SuppressWarnings("unchecked")
    @Test(expected = RuntimeException.class)
    public void testProcessQueryForProperHandlingOfSiteNameLOCAL() throws URISyntaxException,
        UnsupportedQueryException, SourceUnavailableException, FederationException,
        UnsupportedEncodingException, CatalogTransformerException {

        // ***Test setup***
        final String TEST_SITE_NAME = "TestSiteName";

        CatalogFramework mockFramework = mock(CatalogFramework.class);
        FilterBuilder mockFilterBuilder = mock(FilterBuilder.class);

        AttributeBuilder mockAB = mock(AttributeBuilder.class);
        ExpressionBuilder mockEB = mock(ExpressionBuilder.class);
        ContextualExpressionBuilder mockCEB = mock(ContextualExpressionBuilder.class);
        Filter mockFilter = mock(Filter.class);

        when(mockFilterBuilder.attribute(anyString())).thenReturn(mockAB);
        when(mockAB.is()).thenReturn(mockEB);
        when(mockEB.like()).thenReturn(mockCEB);
        when(mockCEB.text(anyString())).thenReturn(mockFilter);

        String searchTerms = "searchForThis";

        // "local" MUST be included
        String sources = "test, local";
        String count = "200";

        // dummy UriInfo, not really used functionally
        UriInfo mockUriInfo = mock(UriInfo.class);
        URI uri = new URI("test");
        when(mockUriInfo.getRequestUri()).thenReturn(uri);

        MultivaluedMap<String, String> mockMVMap = mock(MultivaluedMap.class);
        when(mockMVMap.get("subscription")).thenReturn(null);
        when(mockMVMap.get("interval")).thenReturn(null);
        when(mockUriInfo.getQueryParameters()).thenReturn(mockMVMap);

        @SuppressWarnings("unused")
        BinaryContent mockByteContent = mock(BinaryContent.class);

        // Check on the sites passed in to framework.query
        when(mockFramework.query(any(QueryRequest.class))).thenAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) {
                QueryRequest queryRequest = (QueryRequest) invocation.getArguments()[0];

                // ***Test verification***
                // This assert is the whole point of this unit test
                Assert.assertTrue(((OpenSearchQuery) queryRequest.getQuery()).getSiteIds()
                        .contains(TEST_SITE_NAME));

                return new QueryResponseImpl(queryRequest);
            }
        });

        // setup the BinaryContent for the call to Response.ok(...)
        // This is just needed to get the method to complete, the
        BinaryContent mockBinaryContent = mock(BinaryContent.class);
        InputStream is = new ByteArrayInputStream("Test String From InputStream".getBytes("UTF-8"));
        when(mockBinaryContent.getInputStream()).thenReturn(is);
        when(mockBinaryContent.getMimeTypeValue()).thenReturn("text/plain");

        when(mockFramework.transform(any(QueryResponse.class), anyString(), anyMap())).thenReturn(
                mockBinaryContent);

        OpenSearchEndpoint osEndPoint = new OpenSearchEndpoint(mockFramework, mockFilterBuilder);

        // Call ddfConfigurationUpdated to set the id values to the site name we want
        // local to be replaced with
        Map<String, String> ddfProperties = new HashMap<String, String>();
        ddfProperties.put("id", TEST_SITE_NAME);
        osEndPoint.configurationUpdateCallback(ddfProperties);

        // ***Test Execution***
        osEndPoint
                .processQuery(searchTerms, null, sources, null, null, count, null, null, null,
                        null, null, null, null, null, null, null, null, null, mockUriInfo, null,
                        null, null);

    }
    
    @Test
    public void testNullGetSubject() {
        CatalogFramework mockFramework = mock(CatalogFramework.class);
        FilterBuilder mockFilterBuilder = mock(FilterBuilder.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        
        OpenSearchEndpoint osEndPoint = new OpenSearchEndpoint(mockFramework, mockFilterBuilder);
        
        assertNull(osEndPoint.getSubject(request));
    }

}
