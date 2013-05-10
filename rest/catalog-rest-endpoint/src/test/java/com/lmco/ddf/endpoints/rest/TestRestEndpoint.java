/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.endpoints.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponseImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeToTransformerMapper;

/**
 * Tests methods of the {@link RESTEndpoint}
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestRestEndpoint {
	private static final int INTERNAL_SERVER_ERROR = 500;

	private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

	private static final String ENDPOINT_ADDRESS = "http://localhost:8181/services/catalog";

	private static final Logger LOGGER = Logger.getLogger(TestRestEndpoint.class);
	
	private static final String LOCAL_RETRIEVE_ADDRESS = "http://localhost:8181/services/catalog";
	private static final String FED_RETRIEVE_ADDRESS = "http://localhost:8181/services/catalog/sources/test/abc123456def";
	private static final String GET_SITENAME = "test";
	private static final String GET_ID = "abc123456def";
	private static final String GET_STREAM = "Test string for inputstream.";
	private static final String GET_OUTPUT_TYPE = "UTF-8";
	private static final String GET_MIME_TYPE = "text/xml";
	private static final String GET_TRANSFORM_TYPE = "xml";
	private static final String GET_TYPE_OUTPUT = "{Content-Type=[text/xml]}";

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
	public void testNullMimeType() throws URISyntaxException, IngestException, SourceUnavailableException {

		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

		RESTEndpoint rest = new RESTEndpoint(framework);

		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

		List list = Arrays.asList(getSimpleTransformer());

		when(matchingService.findMatches(eq(InputTransformer.class), isNull(MimeType.class))).thenReturn(list);

		rest.setMimeTypeToTransformerMapper(matchingService);

		HttpHeaders headers = mock(HttpHeaders.class);

		rest.addDocument(headers, givenUriInfo(SAMPLE_ID), new ByteArrayInputStream("".getBytes()));

		verify(matchingService, atLeastOnce()).findMatches(eq(InputTransformer.class), isNull(MimeType.class));
	}

	/**
	 * Tests a null mimetype is sent to matching service when a MimeType could
	 * not be parsed
	 * 
	 * @throws URISyntaxException
	 * @throws SourceUnavailableException
	 * @throws IngestException
	 */
	@Test
	public void testInvalidMimeType() throws URISyntaxException, IngestException, SourceUnavailableException {

		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

		RESTEndpoint rest = new RESTEndpoint(framework);

		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

		List list = Arrays.asList(getSimpleTransformer());
		
		when(matchingService.findMatches(eq(InputTransformer.class), isNull(MimeType.class))).thenReturn(list);

		rest.setMimeTypeToTransformerMapper(matchingService);

		HttpHeaders headers = createHeaders(Arrays.asList("!INVALID!"));

		rest.addDocument(headers, givenUriInfo(SAMPLE_ID), new ByteArrayInputStream("".getBytes()));

		verify(matchingService, atLeastOnce()).findMatches(eq(InputTransformer.class), isNull(MimeType.class));
	}

	@Test(expected = ServerErrorException.class)
	public void testAddDocumentNullMessage() {

		CatalogFramework framework = mock(CatalogFramework.class);

		RESTEndpoint rest = new RESTEndpoint(framework);

		HttpHeaders headers = mock(HttpHeaders.class);

		rest.addDocument(headers, mock(UriInfo.class), null);

	}

	@Test(expected = ServerErrorException.class)
	public void testAddDocumentNoTransformer() {

		CatalogFramework framework = mock(CatalogFramework.class);

		HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

		RESTEndpoint rest = new RESTEndpoint(framework);

		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

		List list = new ArrayList<InputTransformer>();
		
		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn(list);

		rest.setMimeTypeToTransformerMapper(matchingService);

		InputStream is = new ByteArrayInputStream("".getBytes());

		rest.addDocument(headers, mock(UriInfo.class), is);

	}

	@Test(expected = ServerErrorException.class)
	public void testAddDocumentNoMatchingTransformer() {

		CatalogFramework framework = mock(CatalogFramework.class);

		HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

		RESTEndpoint rest = new RESTEndpoint(framework);

		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

		InputTransformer transformer = mock(InputTransformer.class);

		try {
			when(transformer.transform(isA(InputStream.class))).thenThrow(CatalogTransformerException.class);
		} catch (IOException e) {
			LOGGER.debug(e);
		} catch (CatalogTransformerException e) {
			LOGGER.debug(e);
		}

		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn((List)Arrays.asList(transformer));

		rest.setMimeTypeToTransformerMapper(matchingService);

		InputStream is = new ByteArrayInputStream("".getBytes());

		rest.addDocument(headers, mock(UriInfo.class), is);

	}

	@Test()
	public void testAddDocumentFrameworkIngestException() throws IngestException, SourceUnavailableException,
			URISyntaxException {

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
			LOGGER.debug(e);
		} catch (CatalogTransformerException e) {
			LOGGER.debug(e);
		}

		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn((List)Arrays.asList(transformer));

		rest.setMimeTypeToTransformerMapper(matchingService);

		InputStream is = new ByteArrayInputStream("".getBytes());

		rest.addDocument(headers, mock(UriInfo.class), is);

	}

	@Test()
	public void testAddDocumentPositiveCase() throws IOException, CatalogTransformerException, IngestException,
			SourceUnavailableException, URISyntaxException {

		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

		HttpHeaders headers = createHeaders(Arrays.asList(MediaType.APPLICATION_JSON));

		RESTEndpoint rest = new RESTEndpoint(framework);

		addMatchingService(rest,Arrays.asList(getSimpleTransformer()));

		UriInfo info = givenUriInfo(SAMPLE_ID);

		Response response = rest.addDocument(headers, info, new ByteArrayInputStream("".getBytes()));

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
	 */
	@Test(expected = ServerErrorException.class)
	public void testGetDocumentLocalNullQueryResponse() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		mockTestSetup(framework, true, true, false, false);
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
	 * @throws UnsupportedEncodingException
	 */
	@Test(expected = ServerErrorException.class)
	public void testGetDocumentFedNullQueryResponse() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		mockTestSetup(framework, false, true, false, false);
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
	 * @throws UnsupportedEncodingException
	 */
	@Test(expected = ServerErrorException.class)
	public void testGetDocumentLocalNullMetacard() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		mockTestSetup(framework, true, false, true, false);
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
	 * @throws UnsupportedEncodingException
	 */
	@Test(expected = ServerErrorException.class)
	public void testGetDocumentFedNullMetacard() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		mockTestSetup(framework, false, false, true, false);
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
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testGetDocumentLocalSuccess() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		Response response = mockTestSetup(framework, true, false, false, true);

		String responseMessage = byteArrayConvert((ByteArrayInputStream) response.getEntity());
		assertEquals(responseMessage, GET_STREAM);
		assertEquals(response.getStatus(), 200);
		assertEquals(response.getMetadata().toString(),	GET_TYPE_OUTPUT);
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
	 * @throws UnsupportedEncodingException
	 */
	@Test
	public void testGetDocumentFedSuccess() throws URISyntaxException,
			IngestException, SourceUnavailableException,
			UnsupportedQueryException, FederationException,
			CatalogTransformerException, UnsupportedEncodingException {
		
		CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
		Response response = mockTestSetup(framework, false, false, false, true);

		String responseMessage = byteArrayConvert((ByteArrayInputStream) response.getEntity());
		assertEquals(responseMessage, GET_STREAM);
		assertEquals(response.getStatus(), 200);
		assertEquals(response.getMetadata().toString(), GET_TYPE_OUTPUT);
	}

	/**
	 * Converts a ByteArrayInputStream into a readable/printable String
	 * @param content
	 * @return
	 */
	protected String byteArrayConvert(ByteArrayInputStream content) {
		int streamSize = content.available();
		char[] charArray = new char[streamSize];
		byte[] byteArray = new byte[streamSize];

		content.read(byteArray, 0, streamSize);
		for (int i = 0; i < streamSize;) {
			charArray[i] = (char) (byteArray[i++] & 0xff);
		}

		return new String(charArray);
	}
	
	/**
	 * Creates a UriInfo with a user specified URL
	 * 
	 * @param url
	 * @return
	 * @throws URISyntaxException
	 */
	protected UriInfo createSpecificUriInfo(String url)
			throws URISyntaxException {

		UriInfo uriInfo = mock(UriInfo.class);
		URI uri = new URI(url);

		when(uriInfo.getAbsolutePath()).thenReturn(uri);
		when(uriInfo.getQueryParameters()).thenReturn(mock(MultivaluedMap.class));

		return uriInfo;
	}

	/**
	 * Creates the mock setup for the GET tests above. Parameters specify whether
	 * the test will be for a local retrieve or a federated retrieve. Parameters 
	 * also specify which test case is being run.
	 * 
	 * @param framework
	 * @param local
	 * @param queryResponseTest
	 * @param metacardTest
	 * @param successTest
	 * @return
	 * @throws SourceUnavailableException
	 * @throws UnsupportedQueryException
	 * @throws FederationException
	 * @throws UnsupportedEncodingException
	 * @throws CatalogTransformerException
	 * @throws URISyntaxException
	 */
	protected Response mockTestSetup(CatalogFramework framework, boolean local,
			boolean queryResponseTest, boolean metacardTest, boolean successTest)
			throws SourceUnavailableException, UnsupportedQueryException,
			FederationException, UnsupportedEncodingException,
			CatalogTransformerException, URISyntaxException 
	{
		QueryResponse queryResponse = mock(QueryResponse.class);
		when(framework.query(isA(QueryRequest.class), isNull(FederationStrategy.class))).thenReturn(
				queryResponse);

		if(queryResponseTest)
		{
			List<Result> list = null;
			when(queryResponse.getResults()).thenReturn(list);
		}
		else if(metacardTest)
		{
			List<Result> list = new ArrayList<Result>();
			Result result = mock(Result.class);
			list.add(result);
			when(queryResponse.getResults()).thenReturn(list);

			MetacardImpl metacard = null;
			when(result.getMetacard()).thenReturn(metacard);
		}
		else if(successTest)
		{
			List<Result> list = new ArrayList<Result>();
			Result result = mock(Result.class);
			list.add(result);
			when(queryResponse.getResults()).thenReturn(list);

			MetacardImpl metacard = new MetacardImpl();
			when(result.getMetacard()).thenReturn(metacard);

			BinaryContent binaryContent = mock(BinaryContent.class);
			InputStream inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
			when(binaryContent.getInputStream()).thenReturn(inputStream);
			when(binaryContent.getMimeTypeValue()).thenReturn(GET_MIME_TYPE);
			when(framework.transform(isA(Metacard.class), eq(GET_TRANSFORM_TYPE), isA(Map.class)))
					.thenReturn(binaryContent);
		}	
		
		RESTEndpoint restEndpoint = new RESTEndpoint(framework);
		FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
		restEndpoint.setFilterBuilder(filterBuilder);

		UriInfo uriInfo;
		Response response;
		if(local)
		{
			uriInfo = createSpecificUriInfo(LOCAL_RETRIEVE_ADDRESS);
			response = restEndpoint.getDocument(GET_ID, null, uriInfo);
		}
		else
		{
			uriInfo = createSpecificUriInfo(FED_RETRIEVE_ADDRESS);
			response = restEndpoint.getDocument(GET_SITENAME, GET_ID, null, uriInfo);
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
			rest.addDocument(headers, info, new ByteArrayInputStream("".getBytes()));
			fail();
		} catch (ServerErrorException e) {
			assertThat(e.getResponse().getStatus(), equalTo(INTERNAL_SERVER_ERROR));
		}
	}
	
	protected CatalogFramework givenCatalogFramework(String returnId) throws IngestException, SourceUnavailableException {
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
			when(inputTransformer.transform(isA(InputStream.class), isA(String.class))).thenReturn(generatedMetacard);
		} catch (IOException e) {
			LOGGER.debug(e);
		} catch (CatalogTransformerException e) {
			LOGGER.debug(e);
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
			public Metacard transform(InputStream input, String id) throws IOException, CatalogTransformerException {
				return getSimpleMetacard();
			}

			@Override
			public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
				return getSimpleMetacard();
			}
		};
	}

	private MimeTypeToTransformerMapper addMatchingService(RESTEndpoint rest, List<InputTransformer> sortedListOfTransformers) {

		MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

		when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class))).thenReturn((List)sortedListOfTransformers);

		rest.setMimeTypeToTransformerMapper(matchingService);

		return matchingService;
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
