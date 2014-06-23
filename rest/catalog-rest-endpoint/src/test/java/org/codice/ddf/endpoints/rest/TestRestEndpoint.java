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
package org.codice.ddf.endpoints.rest;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.mime.tika.TikaMimeTypeResolver;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.tika.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests methods of the {@link RESTEndpoint}
 */
public class TestRestEndpoint {
    private static final int BYTES_TO_SKIP = 16;

    private static final int OK = 200;

    private static final int NO_CONTENT = 204;

    private static final int PARTIAL_CONTENT = 206;

    private static final int INTERNAL_SERVER_ERROR = 500;

    private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

    private static final String ENDPOINT_ADDRESS = "http://localhost:8181/services/catalog";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestRestEndpoint.class);

    private static final String LOCAL_RETRIEVE_ADDRESS = "http://localhost:8181/services/catalog";

    private static final String FED_RETRIEVE_ADDRESS = "http://localhost:8181/services/catalog/sources/test/abc123456def";

    private static final String GET_SITENAME = "test";

    private static final String GET_ID = "abc123456def";

    private static final String GET_STREAM = "Test string for inputstream.";

    private static final String GET_OUTPUT_TYPE = "UTF-8";

    private static final String GET_MIME_TYPE = "text/xml";

    private static final String GET_KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

    private static final String GET_FILENAME = "example.xml";

    private static final String GET_TYPE_OUTPUT = "{Content-Type=[text/xml], Accept-Ranges=[bytes], " +
            "Content-Disposition=[inline; filename=\"" + GET_FILENAME + "\"]}";

    private static final String GET_KML_TYPE_OUTPUT = "{Content-Type=[application/vnd.google-earth.kml+xml], " +
            "Accept-Ranges=[bytes], Content-Disposition=[inline; filename=\"" + GET_ID + ".kml" + "\"]}";

    private static final String HEADER_RANGE = "Range";

    private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

    private static final String ACCEPT_RANGES_VALUE = "bytes";

    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    private static final String CONTENT_DISPOSITION_VALUE = "inline; filename=\"" + GET_FILENAME + "\"";

    @BeforeClass
    public static void initialize() throws Exception {
    }

    /**
     * Tests a null mimetype is sent to matching service.
     *
     * @throws URISyntaxException
     * @throws SourceUnavailableException
     * @throws IngestException
     */
    @Test
    public void testNullMimeType() throws URISyntaxException, IngestException,
            SourceUnavailableException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

        RESTEndpoint rest = new RESTEndpoint(framework);

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        List list = Arrays.asList(getSimpleTransformer());

        when(matchingService.findMatches(eq(InputTransformer.class), isNull(MimeType.class)))
                .thenReturn(list);

        rest.setMimeTypeToTransformerMapper(matchingService);

        HttpHeaders headers = mock(HttpHeaders.class);

        rest.addDocument(headers, givenUriInfo(SAMPLE_ID), mock(HttpServletRequest.class), new ByteArrayInputStream("".getBytes()));

        verify(matchingService, atLeastOnce()).findMatches(eq(InputTransformer.class),
                isNull(MimeType.class));
    }

    /**
     * Tests a null mimetype is sent to matching service when a MimeType could not be parsed
     *
     * @throws URISyntaxException
     * @throws SourceUnavailableException
     * @throws IngestException
     */
    @Test
    public void testInvalidMimeType() throws URISyntaxException, IngestException,
            SourceUnavailableException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

        RESTEndpoint rest = new RESTEndpoint(framework);

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        List list = Arrays.asList(getSimpleTransformer());

        when(matchingService.findMatches(eq(InputTransformer.class), isNull(MimeType.class)))
                .thenReturn(list);

        rest.setMimeTypeToTransformerMapper(matchingService);

        HttpHeaders headers = createHeaders(Arrays.asList("!INVALID!"));

        rest.addDocument(headers, givenUriInfo(SAMPLE_ID), mock(HttpServletRequest.class), new ByteArrayInputStream("".getBytes()));

        verify(matchingService, atLeastOnce()).findMatches(eq(InputTransformer.class),
                isNull(MimeType.class));
    }

    @Test(expected = ServerErrorException.class)
    public void testAddDocumentNullMessage() {

        CatalogFramework framework = mock(CatalogFramework.class);

        RESTEndpoint rest = new RESTEndpoint(framework);

        HttpHeaders headers = mock(HttpHeaders.class);

        rest.addDocument(headers, mock(UriInfo.class), mock(HttpServletRequest.class), null);

    }

    @Test(expected = ServerErrorException.class)
    public void testAddDocumentNoTransformer() {

        CatalogFramework framework = mock(CatalogFramework.class);

        HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

        RESTEndpoint rest = new RESTEndpoint(framework);

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        List list = new ArrayList<InputTransformer>();

        when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
                .thenReturn(list);

        rest.setMimeTypeToTransformerMapper(matchingService);

        InputStream is = new ByteArrayInputStream("".getBytes());

        rest.addDocument(headers, mock(UriInfo.class), mock(HttpServletRequest.class), is);

    }

    @Test(expected = ServerErrorException.class)
    public void testAddDocumentNoMatchingTransformer() {

        CatalogFramework framework = mock(CatalogFramework.class);

        HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

        RESTEndpoint rest = new RESTEndpoint(framework);

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        InputTransformer transformer = mock(InputTransformer.class);

        try {
            when(transformer.transform(isA(InputStream.class))).thenThrow(
                    CatalogTransformerException.class);
        } catch (IOException e) {
            LOGGER.debug("Exception occurred during test", e);
        } catch (CatalogTransformerException e) {
            LOGGER.debug("Exception occurred during test", e);
        }

        when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
                .thenReturn((List) Arrays.asList(transformer));

        rest.setMimeTypeToTransformerMapper(matchingService);

        InputStream is = new ByteArrayInputStream("".getBytes());

        rest.addDocument(headers, mock(UriInfo.class), mock(HttpServletRequest.class), is);

    }

    @Test()
    public void testAddDocumentFrameworkIngestException() throws IngestException,
            SourceUnavailableException, URISyntaxException {

        assertExceptionThrown(IngestException.class);

    }

    @Test()
    public void testAddDocumentFrameworkSourceUnavailableException() throws IngestException,
            SourceUnavailableException, URISyntaxException {

        assertExceptionThrown(SourceUnavailableException.class);

    }

    @Test(expected = ServerErrorException.class)
    public void testAddDocumentNoMatchingTransformer2() {

        CatalogFramework framework = mock(CatalogFramework.class);

        HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

        RESTEndpoint rest = new RESTEndpoint(framework);

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        InputTransformer transformer = mock(InputTransformer.class);

        try {
            when(transformer.transform(isA(InputStream.class))).thenThrow(IOException.class);
        } catch (IOException e) {
            LOGGER.debug("Exception occurred during test", e);
        } catch (CatalogTransformerException e) {
            LOGGER.debug("Exception occurred during test", e);
        }

        when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
                .thenReturn((List) Arrays.asList(transformer));

        rest.setMimeTypeToTransformerMapper(matchingService);

        InputStream is = new ByteArrayInputStream("".getBytes());

        rest.addDocument(headers, mock(UriInfo.class), mock(HttpServletRequest.class), is);

    }

    @Test()
    public void testAddDocumentPositiveCase() throws IOException, CatalogTransformerException,
            IngestException, SourceUnavailableException, URISyntaxException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

        HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

        RESTEndpoint rest = new RESTEndpoint(framework);

        addMatchingService(rest, Arrays.asList(getSimpleTransformer()));

        UriInfo info = givenUriInfo(SAMPLE_ID);

        Response response = rest
                .addDocument(headers, info, mock(HttpServletRequest.class), new ByteArrayInputStream("".getBytes()));

        LOGGER.debug(ToStringBuilder.reflectionToString(response));

        assertThat(response.getStatus(), equalTo(201));

        assertThat(response.getMetadata(), notNullValue());

        assertThat(response.getMetadata().get(Metacard.ID).get(0).toString(), equalTo(SAMPLE_ID));
    }

    /**
     * Tests local retrieve with a null QueryResponse
     *
     * @throws URISyntaxException
     * @throws SourceUnavailableException
     * @throws IngestException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test(expected = ServerErrorException.class)
    public void testGetDocumentLocalNullQueryResponse() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
        executeTest(framework, transformer, true, null);
    }

    /**
     * Tests federated retrieve with a null QueryResponse
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test(expected = ServerErrorException.class)
    public void testGetDocumentFedNullQueryResponse() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
        executeTest(framework, transformer, false, null);
    }

    /**
     * Tests local retrieve with a null Metacard
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test(expected = ServerErrorException.class)
    public void testGetDocumentLocalNullMetacard() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
        executeTest(framework, transformer, true, null);
    }

    /**
     * Tests federated retrieve with a null Metacard
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test(expected = ServerErrorException.class)
    public void testGetDocumentFedNullMetacard() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
        executeTest(framework, transformer, false, null);
    }

    /**
     * Tests local retrieve with a successful response
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test
    public void testGetDocumentLocalSuccess() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.SUCCESS_TEST);
        Response response = executeTest(framework, transformer, true, null);

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        assertEquals(GET_STREAM, responseMessage);
        assertEquals(OK, response.getStatus());
        assertEquals(GET_TYPE_OUTPUT, response.getMetadata().toString());
    }

    @Test
    public void testGetDocumentKml() throws Exception {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.KML_TEST);
        Response response = executeTest(framework, transformer, true, null);

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        assertEquals(GET_STREAM, responseMessage);
        assertEquals(OK, response.getStatus());
        assertEquals(GET_KML_TYPE_OUTPUT, response.getMetadata().toString());
    }

    /**
     * Tests federated retrieve with a successful response
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test
    public void testGetDocumentFedSuccess() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.SUCCESS_TEST);
        Response response = executeTest(framework, transformer, false, null);

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        assertEquals(GET_STREAM, responseMessage);
        assertEquals(OK, response.getStatus());
        assertEquals(GET_TYPE_OUTPUT, response.getMetadata().toString());
    }

    /**
     * Tests getting source information
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws UnsupportedEncodingException
     * @throws ParseException
     */
    @Test
    public void testGetDocumentSourcesSuccess() throws SourceUnavailableException,
            UnsupportedQueryException, FederationException, CatalogTransformerException,
            URISyntaxException, ParseException, IOException {

        final String LOCAL_SOURCE_ID = "local";
        final String FED1_SOURCE_ID = "fed1";
        final String FED2_SOURCE_ID = "fed2";
        final String VERSION = "4.0";
        final String JSON_MIME_TYPE_STRING = "application/json";

        Set<ContentType> contentTypes = new HashSet<ContentType>();
        contentTypes.add(new ContentTypeImpl("ct1", "v1"));
        contentTypes.add(new ContentTypeImpl("ct2", "v2"));
        contentTypes.add(new ContentTypeImpl("ct3", null));
        JSONArray contentTypesInJSON = new JSONArray();
        for (ContentType ct : contentTypes) {
            JSONObject ob = new JSONObject();
            ob.put("name", ct.getName());
            ob.put("version", ct.getVersion() != null ? ct.getVersion() : "");
            contentTypesInJSON.add(ob);
        }

        Set<SourceDescriptor> sourceDescriptors = new HashSet<SourceDescriptor>();
        SourceDescriptorImpl localDescriptor = new SourceDescriptorImpl(LOCAL_SOURCE_ID,
                contentTypes);
        localDescriptor.setVersion(VERSION);
        SourceDescriptorImpl fed1Descriptor = new SourceDescriptorImpl(FED1_SOURCE_ID, contentTypes);
        fed1Descriptor.setVersion(VERSION);
        SourceDescriptorImpl fed2Descriptor = new SourceDescriptorImpl(FED2_SOURCE_ID, null);

        sourceDescriptors.add(localDescriptor);
        sourceDescriptors.add(fed1Descriptor);
        sourceDescriptors.add(fed2Descriptor);

        SourceInfoResponse sourceInfoResponse = new SourceInfoResponseImpl(null, null,
                sourceDescriptors);

        CatalogFramework framework = mock(CatalogFramework.class);
        when(framework.getSourceInfo(isA(SourceInfoRequestEnterprise.class))).thenReturn(
                sourceInfoResponse);

        RESTEndpoint restEndpoint = new RESTEndpoint(framework);

        Response response = restEndpoint.getDocument(null, null);
        assertEquals(OK, response.getStatus());
        assertEquals(JSON_MIME_TYPE_STRING, response.getMetadata().get("Content-Type").get(0));

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        JSONArray srcList = (JSONArray) new JSONParser().parse(responseMessage);

        assertEquals(3, srcList.size());

        for (Object o : srcList) {
            JSONObject src = (JSONObject) o;
            assertEquals(true, src.get("available"));
            String id = (String) src.get("id");
            if (id.equals(LOCAL_SOURCE_ID)) {
                assertThat((Iterable<Object>) src.get("contentTypes"),
                        hasItems(contentTypesInJSON.toArray()));
                assertEquals(contentTypes.size(), ((JSONArray) src.get("contentTypes")).size());
                assertEquals(VERSION, src.get("version"));
            } else if (id.equals(FED1_SOURCE_ID)) {
                assertThat((Iterable<Object>) src.get("contentTypes"),
                        hasItems(contentTypesInJSON.toArray()));
                assertEquals(contentTypes.size(), ((JSONArray) src.get("contentTypes")).size());
                assertEquals(VERSION, src.get("version"));
            } else if (id.equals(FED2_SOURCE_ID)) {
                assertEquals(0, ((JSONArray) src.get("contentTypes")).size());
                assertEquals("", src.get("version"));
            } else {
                fail("Invalid ID returned");
            }
        }
    }

    /**
     * Tests retrieving a local resource with a successful response
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test
    public void testGetDocumentResourceLocalSuccess() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.RESOURCE_TEST);
        Response response = executeTest(framework, transformer, true, null);

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        assertEquals(GET_STREAM, responseMessage);
        assertEquals(OK, response.getStatus());
        assertEquals(GET_TYPE_OUTPUT, response.getMetadata().toString());
    }

    /**
     * Tests retrieving a federated resource with a successful response
     *
     * @throws URISyntaxException
     * @throws IngestException
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws ResourceNotSupportedException
     * @throws ResourceNotFoundException
     * @throws IOException
     */
    @Test
    public void testGetDocumentResourceFedSuccess() throws URISyntaxException, IngestException,
            SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        String transformer = mockTestSetup(framework, TestType.RESOURCE_TEST);
        Response response = executeTest(framework, transformer, false, null);

        String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
        assertEquals(GET_STREAM, responseMessage);
        assertEquals(OK, response.getStatus());
        assertEquals(GET_TYPE_OUTPUT, response.getMetadata().toString());
    }

    /**
     * Tests a null mimetype is sent to matching service.
     *
     * @throws URISyntaxException
     * @throws SourceUnavailableException
     * @throws IngestException
     */
    @Test
    public void testNullgetSubject() throws URISyntaxException, IngestException,
            SourceUnavailableException {

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
        HttpServletRequest request = mock(HttpServletRequest.class);

        RESTEndpoint rest = new RESTEndpoint(framework);

        assertNull(rest.getSubject(request));
    }

    /**
     * Test using a Head request to find out if Range headers are supported and to get resource size of a local
     * resource for use when using Range headers.
     *
     * @throws SourceUnavailableException
     * @throws IngestException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws IOException
     * @throws URISyntaxException
     * @throws ResourceNotFoundException
     * @throws ResourceNotSupportedException
     */
    @Test
    public void testHeadRequestLocal()
            throws SourceUnavailableException, IngestException, UnsupportedQueryException,
            FederationException, CatalogTransformerException, IOException, URISyntaxException,
            ResourceNotFoundException, ResourceNotSupportedException {

        boolean isLocal = true;

        Response response = headTest(isLocal);

        assertEquals(NO_CONTENT, response.getStatus());
        assertEquals(ACCEPT_RANGES_VALUE, response.getHeaderString(HEADER_ACCEPT_RANGES));
        assertEquals(CONTENT_DISPOSITION_VALUE, response.getHeaderString(HEADER_CONTENT_DISPOSITION));
    }

    /**
     * Test using a Head request to find out if Range headers are supported and to get resource size of a resource
     * at a federated site for use when using Range headers.
     *
     * @throws SourceUnavailableException
     * @throws IngestException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws IOException
     * @throws URISyntaxException
     * @throws ResourceNotFoundException
     * @throws ResourceNotSupportedException
     */
    @Test
    public void testHeadRequestFederated()
            throws SourceUnavailableException, IngestException, UnsupportedQueryException,
            FederationException, CatalogTransformerException, IOException, URISyntaxException,
            ResourceNotFoundException, ResourceNotSupportedException {

        boolean isLocal = false;

        Response response = headTest(isLocal);

        assertEquals(NO_CONTENT, response.getStatus());
        assertEquals(ACCEPT_RANGES_VALUE, response.getHeaderString(HEADER_ACCEPT_RANGES));
        assertEquals(CONTENT_DISPOSITION_VALUE, response.getHeaderString(HEADER_CONTENT_DISPOSITION));
    }

    private Response headTest(boolean local)
            throws CatalogTransformerException, URISyntaxException, UnsupportedEncodingException, UnsupportedQueryException, SourceUnavailableException, FederationException, IngestException {

        MetacardImpl metacard = null;
        List<Result> list = new ArrayList<Result>();
        Result result = mock(Result.class);
        InputStream inputStream = null;
        UriInfo uriInfo;
        Response response;

        CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

        list.add(result);

        QueryResponse queryResponse = mock(QueryResponse.class);

        when(queryResponse.getResults()).thenReturn(list);

        when(framework.query(isA(QueryRequest.class), isNull(FederationStrategy.class)))
                .thenReturn(queryResponse);

        metacard = new MetacardImpl();
        metacard.setSourceId(GET_SITENAME);
        when(result.getMetacard()).thenReturn(metacard);

        Resource resource = mock(Resource.class);
        inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
        when(resource.getInputStream()).thenReturn(inputStream);
        when(resource.getMimeTypeValue()).thenReturn(GET_MIME_TYPE);
        when(resource.getName()).thenReturn(GET_FILENAME);
        when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class))).thenReturn(
                resource);

        RESTEndpoint restEndpoint = new RESTEndpoint(framework);
        restEndpoint.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        restEndpoint.setFilterBuilder(filterBuilder);

        uriInfo = createSpecificUriInfo(LOCAL_RETRIEVE_ADDRESS);

        if (local) {
            response = restEndpoint.getHeaders(GET_ID, uriInfo, null);
        } else {
            response = restEndpoint.getHeaders(null, GET_ID, uriInfo, null);
        }

        return response;
    }

    /**
     * Creates a UriInfo with a user specified URL
     *
     * @param url
     * @return
     * @throws URISyntaxException
     */
    protected UriInfo createSpecificUriInfo(String url) throws URISyntaxException {

        UriInfo uriInfo = mock(UriInfo.class);
        URI uri = new URI(url);

        when(uriInfo.getAbsolutePath()).thenReturn(uri);
        when(uriInfo.getQueryParameters()).thenReturn(mock(MultivaluedMap.class));

        return uriInfo;
    }

    protected enum TestType {
        QUERY_RESPONSE_TEST, METACARD_TEST, SUCCESS_TEST, RESOURCE_TEST, KML_TEST
    }

    /**
     * Creates the mock setup for the GET tests above. Parameters provide the CatalogFramework, which will be
     * setup for the test, and also specify which test case is being run.
     *
     * @param framework
     * @param testType
     * @return
     * @throws SourceUnavailableException
     * @throws UnsupportedQueryException
     * @throws FederationException
     * @throws CatalogTransformerException
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ResourceNotSupportedException
     */
    protected String mockTestSetup(CatalogFramework framework, TestType testType)
            throws SourceUnavailableException, UnsupportedQueryException, FederationException,
            CatalogTransformerException, IOException, ResourceNotFoundException,
            ResourceNotSupportedException {
        String transformer = null;
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(framework.query(isA(QueryRequest.class), isNull(FederationStrategy.class)))
                .thenReturn(queryResponse);

        List<Result> list = null;
        MetacardImpl metacard = null;
        Result result = mock(Result.class);
        InputStream inputStream = null;

        switch (testType) {
            case QUERY_RESPONSE_TEST:
                when(queryResponse.getResults()).thenReturn(list);
                break;

            case METACARD_TEST:
                list = new ArrayList<Result>();
                list.add(result);
                when(queryResponse.getResults()).thenReturn(list);

                when(result.getMetacard()).thenReturn(metacard);
                break;

            case RESOURCE_TEST:
                transformer = "resource";
            /* FALLTHRU */

            case SUCCESS_TEST:
                list = new ArrayList<Result>();
                list.add(result);
                when(queryResponse.getResults()).thenReturn(list);

                metacard = new MetacardImpl();
                metacard.setSourceId(GET_SITENAME);
                when(result.getMetacard()).thenReturn(metacard);

                Resource resource = mock(Resource.class);
                inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
                when(resource.getInputStream()).thenReturn(inputStream);
                when(resource.getMimeTypeValue()).thenReturn(GET_MIME_TYPE);
                when(resource.getName()).thenReturn(GET_FILENAME);
                when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class))).thenReturn(
                        resource);
                break;

            case KML_TEST:
                transformer = "kml";
                list = new ArrayList<Result>();
                list.add(result);
                when(queryResponse.getResults()).thenReturn(list);

                metacard = new MetacardImpl();
                metacard.setSourceId(GET_SITENAME);
                when(result.getMetacard()).thenReturn(metacard);

                BinaryContent content = mock(BinaryContent.class);
                inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
                when(content.getInputStream()).thenReturn(inputStream);
                when(content.getMimeTypeValue()).thenReturn(GET_KML_MIME_TYPE);
                when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class))).thenReturn(
                        content);
                break;
        }

        return transformer;
    }

    private Response executeTest(CatalogFramework framework, String transformer, boolean local,
                                 HttpServletRequest request)
            throws URISyntaxException {


        RESTEndpoint restEndpoint = new RESTEndpoint(framework);
        restEndpoint.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
        FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
        restEndpoint.setFilterBuilder(filterBuilder);

        UriInfo uriInfo;
        Response response;
        if (local) {
            uriInfo = createSpecificUriInfo(LOCAL_RETRIEVE_ADDRESS);
            response = restEndpoint.getDocument(GET_ID, transformer, uriInfo, request);
        } else {
            uriInfo = createSpecificUriInfo(FED_RETRIEVE_ADDRESS);
            response = restEndpoint.getDocument(GET_SITENAME, GET_ID, transformer, uriInfo, request);
        }

        return response;
    }

    protected void assertExceptionThrown(Class<? extends Throwable> klass) throws IngestException,
            SourceUnavailableException, URISyntaxException {

        CatalogFramework framework = mock(CatalogFramework.class);

        when(framework.create(isA(CreateRequest.class))).thenThrow(klass);

        HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

        RESTEndpoint rest = new RESTEndpoint(framework);

        addMatchingService(rest, Arrays.asList(getSimpleTransformer()));

        UriInfo info = givenUriInfo(SAMPLE_ID);

        try {
            rest.addDocument(headers, info, mock(HttpServletRequest.class), new ByteArrayInputStream("".getBytes()));
            fail();
        } catch (ServerErrorException e) {
            assertThat(e.getResponse().getStatus(), equalTo(INTERNAL_SERVER_ERROR));
        }
    }

    protected CatalogFramework givenCatalogFramework(String returnId) throws IngestException,
            SourceUnavailableException {
        CatalogFramework framework = mock(CatalogFramework.class);

        Metacard returnMetacard = mock(Metacard.class);

        when(returnMetacard.getId()).thenReturn(returnId);

        when(framework.create(isA(CreateRequest.class))).thenReturn(
                new CreateResponseImpl(null, null, Arrays.asList(returnMetacard)));
        return framework;
    }

    protected UriInfo givenUriInfo(String metacardId) throws URISyntaxException {
        UriInfo info = mock(UriInfo.class);

        UriBuilder builder = mock(UriBuilder.class);

        when(builder.path("/" + metacardId)).thenReturn(builder);

        when(builder.build()).thenReturn(new URI(ENDPOINT_ADDRESS));

        when(info.getAbsolutePathBuilder()).thenReturn(builder);
        return info;
    }

    protected InputTransformer getMockInputTransformer() {
        InputTransformer inputTransformer = mock(InputTransformer.class);

        Metacard generatedMetacard = getSimpleMetacard();

        try {
            when(inputTransformer.transform(isA(InputStream.class))).thenReturn(generatedMetacard);
            when(inputTransformer.transform(isA(InputStream.class), isA(String.class))).thenReturn(
                    generatedMetacard);
        } catch (IOException e) {
            LOGGER.debug("Exception occurred during test", e);
        } catch (CatalogTransformerException e) {
            LOGGER.debug("Exception occurred during test", e);
        }
        return inputTransformer;
    }

    protected Metacard getSimpleMetacard() {
        MetacardImpl generatedMetacard = new MetacardImpl();
        generatedMetacard.setMetadata(getSample());
        generatedMetacard.setId(SAMPLE_ID);

        return generatedMetacard;
    }

    private InputTransformer getSimpleTransformer() {
        return new InputTransformer() {

            @Override
            public Metacard transform(InputStream input, String id) throws IOException,
                    CatalogTransformerException {
                return getSimpleMetacard();
            }

            @Override
            public Metacard transform(InputStream input) throws IOException,
                    CatalogTransformerException {
                return getSimpleMetacard();
            }
        };
    }

    private MimeTypeToTransformerMapper addMatchingService(RESTEndpoint rest,
                                                           List<InputTransformer> sortedListOfTransformers) {

        MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

        when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
                .thenReturn((List) sortedListOfTransformers);

        rest.setMimeTypeToTransformerMapper(matchingService);

        return matchingService;
    }

    private HttpServletRequest createServletRequest(String bytesToSkip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HEADER_RANGE)).thenReturn("bytes=" + bytesToSkip + "-");

        return request;
    }

    private HttpHeaders createHeaders(List<String> mimeTypeList) {

        HttpHeaders headers = mock(HttpHeaders.class);

        when(headers.getRequestHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(mimeTypeList);

        return headers;
    }

    private String getSample() {
        return "<xml></xml>";
    }

}
