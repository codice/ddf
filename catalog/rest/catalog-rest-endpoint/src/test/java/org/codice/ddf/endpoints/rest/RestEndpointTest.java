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
package org.codice.ddf.endpoints.rest;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Strings;
import ddf.catalog.CatalogFramework;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
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
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.tika.TikaMimeTypeResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import javax.activation.MimeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests methods of the {@link RESTEndpoint} */
@SuppressWarnings({"JavaDoc", "unchecked"})
public class RestEndpointTest {
  private static final int OK = 200;

  private static final int NO_CONTENT = 204;

  private static final int INTERNAL_SERVER_ERROR = 500;

  private static final int BAD_REQUEST = 400;

  private static final int NOT_FOUND = 404;

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private static final String ENDPOINT_ADDRESS = "http://localhost:8181/services/catalog";

  private static final Logger LOGGER = LoggerFactory.getLogger(RestEndpointTest.class);

  private static final String LOCAL_RETRIEVE_ADDRESS = "http://localhost:8181/services/catalog";

  private static final String FED_RETRIEVE_ADDRESS =
      "http://localhost:8181/services/catalog/sources/test/abc123456def";

  private static final String GET_SITENAME = "test";

  private static final String GET_ID = "abc123456def";

  private static final String GET_STREAM = "Test string for inputstream.";

  private static final String GET_OUTPUT_TYPE = "UTF-8";

  private static final String GET_MIME_TYPE = "text/xml";

  private static final String GET_KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";

  private static final String GET_FILENAME = "example.xml";

  private static final String GET_TYPE_OUTPUT =
      "{Content-Type=[text/xml], Accept-Ranges=[bytes], "
          + "Content-Disposition=[inline; filename=\""
          + GET_FILENAME
          + "\"]}";

  private static final String GET_KML_TYPE_OUTPUT =
      "{Content-Type=[application/vnd.google-earth.kml+xml], "
          + "Accept-Ranges=[bytes], Content-Disposition=[inline; filename=\""
          + GET_ID
          + ".kml"
          + "\"]}";

  private static final String HEADER_RANGE = "Range";

  private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

  private static final String ACCEPT_RANGES_VALUE = "bytes";

  private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

  private static final String CONTENT_DISPOSITION_VALUE =
      "inline; filename=\"" + GET_FILENAME + "\"";

  @BeforeClass
  public static void initialize() throws Exception {}

  @Test
  public void testAddDocumentNullMessage() {

    CatalogFramework framework = mock(CatalogFramework.class);

    RESTEndpoint rest = new RESTEndpoint(framework);

    HttpHeaders headers = mock(HttpHeaders.class);

    Response response =
        rest.addDocument(
            headers,
            mock(UriInfo.class),
            mock(HttpServletRequest.class),
            mock(MultipartBody.class),
            null,
            null);
    assertEquals(response.getStatus(), BAD_REQUEST);
    assertEquals(response.getEntity(), "<pre>No content found, cannot do CREATE.</pre>");
  }

  @Test
  public void testAddDocumentFrameworkIngestException()
      throws IngestException, SourceUnavailableException, URISyntaxException,
          MetacardCreationException {

    assertExceptionThrown(IngestException.class);
  }

  @Test
  public void testAddDocumentFrameworkSourceUnavailableException()
      throws IngestException, SourceUnavailableException, URISyntaxException,
          MetacardCreationException {

    assertExceptionThrown(SourceUnavailableException.class);
  }

  @Test
  public void testAddDocumentPositiveCase()
      throws IOException, CatalogTransformerException, IngestException, SourceUnavailableException,
          URISyntaxException, MetacardCreationException {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));

    RESTEndpoint rest = new RESTEndpoint(framework);

    addMatchingService(rest, getSimpleMetacard());

    UriInfo info = givenUriInfo(SAMPLE_ID);

    Response response =
        rest.addDocument(
            headers,
            info,
            mock(HttpServletRequest.class),
            mock(MultipartBody.class),
            null,
            new ByteArrayInputStream("".getBytes()));

    LOGGER.debug(ToStringBuilder.reflectionToString(response));

    assertThat(response.getStatus(), equalTo(201));

    assertThat(response.getMetadata(), notNullValue());

    assertThat(response.getMetadata().get(Metacard.ID).get(0).toString(), equalTo(SAMPLE_ID));
  }

  @Test
  public void testAddDocumentWithMetadataPositiveCase()
      throws IOException, CatalogTransformerException, IngestException, SourceUnavailableException,
          URISyntaxException, InvalidSyntaxException, MimeTypeResolutionException,
          MetacardCreationException {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(new MetacardImpl()));

    Transform transform = mock(Transform.class);
    when(transform.transform(any(MimeType.class), any(), any(), any(), any(), any()))
        .thenReturn(transformResponse);

    RESTEndpoint rest =
        new RESTEndpoint(framework) {
          @Override
          public Transform getTransform() {
            return transform;
          }
        };

    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    rest.setUuidGenerator(uuidGenerator);
    rest.setMetacardTypes(Collections.singletonList(MetacardImpl.BASIC_METACARD));
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");
    rest.setMimeTypeMapper(mimeTypeMapper);

    addMatchingService(rest, getSimpleMetacard());

    UriInfo info = givenUriInfo(SAMPLE_ID);

    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Attachment attachment =
        new Attachment(
            "parse.resource", new ByteArrayInputStream("Some Text".getBytes()), contentDisposition);
    attachments.add(attachment);
    ContentDisposition contentDisposition1 =
        new ContentDisposition("form-data; name=parse.metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment1 =
        new Attachment(
            "parse.metadata",
            new ByteArrayInputStream("Some Text Again".getBytes()),
            contentDisposition1);
    attachments.add(attachment1);
    ContentDisposition contentDisposition2 =
        new ContentDisposition("form-data; name=metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment2 =
        new Attachment(
            "metadata",
            new ByteArrayInputStream("<meta>beta</meta>".getBytes()),
            contentDisposition2);
    attachments.add(attachment2);
    MultipartBody multipartBody = new MultipartBody(attachments);

    Response response =
        rest.addDocument(
            headers,
            info,
            mock(HttpServletRequest.class),
            multipartBody,
            null,
            new ByteArrayInputStream("".getBytes()));

    LOGGER.debug(ToStringBuilder.reflectionToString(response));

    assertThat(response.getStatus(), equalTo(201));

    assertThat(response.getMetadata(), notNullValue());

    assertThat(response.getMetadata().get(Metacard.ID).get(0).toString(), equalTo(SAMPLE_ID));
  }

  @Test
  public void testParseAttachments()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException, MimeTypeResolutionException, URISyntaxException,
          MetacardCreationException {
    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata("Some Text Again");
    when(inputTransformer.transform(any())).thenReturn(metacard);

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    Transform transform = mock(Transform.class);
    when(transform.transform(
            any(MimeType.class),
            any(String.class),
            any(Supplier.class),
            any(InputStream.class),
            eq("xml"),
            any(Map.class)))
        .thenReturn(transformResponse);

    RESTEndpoint rest =
        new RESTEndpoint(framework) {
          @Override
          public Transform getTransform() {
            return transform;
          }
        };
    rest.setMetacardTypes(Collections.singletonList(MetacardImpl.BASIC_METACARD));
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");
    rest.setMimeTypeMapper(mimeTypeMapper);

    addMatchingService(rest, getSimpleMetacard());

    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Attachment attachment =
        new Attachment(
            "parse.resource", new ByteArrayInputStream("Some Text".getBytes()), contentDisposition);
    attachments.add(attachment);
    ContentDisposition contentDisposition1 =
        new ContentDisposition("form-data; name=parse.metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment1 =
        new Attachment(
            "parse.metadata",
            new ByteArrayInputStream("Some Text Again".getBytes()),
            contentDisposition1);
    attachments.add(attachment1);

    RESTEndpoint.CreateInfo createInfo = rest.parseAttachments(attachments, "xml");
    assertThat(createInfo.getMetacard().getMetadata(), equalTo("Some Text Again"));

    ContentDisposition contentDisposition2 =
        new ContentDisposition("form-data; name=metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment2 =
        new Attachment(
            "metadata",
            new ByteArrayInputStream("<meta>beta</meta>".getBytes()),
            contentDisposition2);
    attachments.add(attachment2);

    ContentDisposition contentDisposition3 =
        new ContentDisposition("form-data; name=foo; filename=C:\\DDF\\metacard.xml");
    Attachment attachment3 =
        new Attachment("foo", new ByteArrayInputStream("bar".getBytes()), contentDisposition3);
    attachments.add(attachment3);

    createInfo = rest.parseAttachments(attachments, "xml");

    assertThat(createInfo.getMetacard().getMetadata(), equalTo("<meta>beta</meta>"));
    assertThat(createInfo.getMetacard().getAttribute("foo"), equalTo(null));
  }

  @Test
  public void testParseAttachmentsTooLarge()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException, MimeTypeResolutionException, URISyntaxException,
          MetacardCreationException {
    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata("Some Text Again");
    when(inputTransformer.transform(any())).thenReturn(metacard);

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    Transform transform = mock(Transform.class);
    when(transform.transform(
            any(MimeType.class),
            any(String.class),
            any(Supplier.class),
            any(InputStream.class),
            eq("xml"),
            any(Map.class)))
        .thenReturn(transformResponse);

    RESTEndpoint rest =
        new RESTEndpoint(framework) {
          @Override
          public Transform getTransform() {
            return transform;
          }
        };
    rest.setMetacardTypes(Collections.singletonList(MetacardImpl.BASIC_METACARD));
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");
    rest.setMimeTypeMapper(mimeTypeMapper);

    addMatchingService(rest, getSimpleMetacard());

    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Attachment attachment =
        new Attachment(
            "parse.resource", new ByteArrayInputStream("Some Text".getBytes()), contentDisposition);
    attachments.add(attachment);
    ContentDisposition contentDisposition1 =
        new ContentDisposition("form-data; name=parse.metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment1 =
        new Attachment(
            "parse.metadata",
            new ByteArrayInputStream("Some Text Again".getBytes()),
            contentDisposition1);
    attachments.add(attachment1);

    RESTEndpoint.CreateInfo createInfo = rest.parseAttachments(attachments, "xml");
    assertThat(createInfo.getMetacard().getMetadata(), equalTo("Some Text Again"));

    ContentDisposition contentDisposition2 =
        new ContentDisposition("form-data; name=metadata; filename=C:\\DDF\\metacard.xml");
    Attachment attachment2 =
        new Attachment(
            "metadata",
            new ByteArrayInputStream(Strings.repeat("hi", 100_000).getBytes()),
            contentDisposition2);
    attachments.add(attachment2);

    ContentDisposition contentDisposition3 =
        new ContentDisposition("form-data; name=foo; filename=C:\\DDF\\metacard.xml");
    Attachment attachment3 =
        new Attachment("foo", new ByteArrayInputStream("bar".getBytes()), contentDisposition3);
    attachments.add(attachment3);

    createInfo = rest.parseAttachments(attachments, "xml");

    // Ensure that the metadata was not overriden because it was too large to be parsed
    assertThat(createInfo.getMetacard().getMetadata(), equalTo("Some Text Again"));
    assertThat(createInfo.getMetacard().getAttribute("foo"), equalTo(null));
  }
  /**
   * Tests local retrieve with a null QueryResponse
   *
   * @throws Exception
   */
  @Test
  public void testGetDocumentLocalNullQueryResponse() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
    Response response = executeTest(framework, transformer, true, null);
    assertEquals(response.getStatus(), NOT_FOUND);
    assertEquals(response.getEntity(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /**
   * Tests federated retrieve with a null QueryResponse
   *
   * @throws Exception
   */
  @Test
  public void testGetDocumentFedNullQueryResponse() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String transformer = mockTestSetup(framework, TestType.QUERY_RESPONSE_TEST);
    Response response = executeTest(framework, transformer, false, null);
    assertEquals(response.getStatus(), NOT_FOUND);
    assertEquals(response.getEntity(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /**
   * Tests local retrieve with a null Metacard
   *
   * @throws Exception
   */
  @Test
  public void testGetDocumentLocalNullMetacard() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
    Response response = executeTest(framework, transformer, true, null);
    assertEquals(response.getStatus(), NOT_FOUND);
    assertEquals(response.getEntity(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /**
   * Tests federated retrieve with a null Metacard
   *
   * @throws Exception
   */
  @Test
  public void testGetDocumentFedNullMetacard() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String transformer = mockTestSetup(framework, TestType.METACARD_TEST);
    Response response = executeTest(framework, transformer, false, null);
    assertEquals(response.getStatus(), NOT_FOUND);
    assertEquals(response.getEntity(), "<pre>Unable to retrieve requested metacard.</pre>");
  }

  /**
   * Tests local retrieve with a successful response
   *
   * @throws Exception
   */
  @Test
  public void testGetDocumentLocalSuccess() throws Exception {

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
   * @throws Exception
   */
  @Test
  public void testGetDocumentFedSuccess() throws Exception {

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
   * @throws Exception
   */
  @Test
  public void testGetDocumentSourcesSuccess() throws Exception {

    final String localSourceId = "local";
    final String fed1SourceId = "fed1";
    final String fed2SourceId = "fed2";
    final String version = "4.0";
    final String jsonMimeTypeString = "application/json";

    Set<ContentType> contentTypes = new HashSet<>();
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

    Set<SourceDescriptor> sourceDescriptors = new HashSet<>();
    SourceDescriptorImpl localDescriptor = new SourceDescriptorImpl(localSourceId, contentTypes);
    localDescriptor.setVersion(version);
    localDescriptor.setAvailable(true);
    SourceDescriptorImpl fed1Descriptor = new SourceDescriptorImpl(fed1SourceId, contentTypes);
    fed1Descriptor.setVersion(version);
    fed1Descriptor.setAvailable(true);
    SourceDescriptorImpl fed2Descriptor = new SourceDescriptorImpl(fed2SourceId, null);
    fed2Descriptor.setAvailable(true);

    sourceDescriptors.add(localDescriptor);
    sourceDescriptors.add(fed1Descriptor);
    sourceDescriptors.add(fed2Descriptor);

    SourceInfoResponse sourceInfoResponse =
        new SourceInfoResponseImpl(null, null, sourceDescriptors);

    CatalogFramework framework = mock(CatalogFramework.class);
    when(framework.getSourceInfo(isA(SourceInfoRequestEnterprise.class)))
        .thenReturn(sourceInfoResponse);

    RESTEndpoint restEndpoint = new RESTEndpoint(framework);

    Response response = restEndpoint.getDocument(null, null);
    assertEquals(OK, response.getStatus());
    assertEquals(jsonMimeTypeString, response.getMetadata().get("Content-Type").get(0));

    String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
    JSONArray srcList = (JSONArray) new JSONParser().parse(responseMessage);

    assertEquals(3, srcList.size());

    for (Object o : srcList) {
      JSONObject src = (JSONObject) o;
      assertEquals(true, src.get("available"));
      String id = (String) src.get("id");
      if (id.equals(localSourceId)) {
        assertThat(
            (Iterable<Object>) src.get("contentTypes"), hasItems(contentTypesInJSON.toArray()));
        assertEquals(contentTypes.size(), ((JSONArray) src.get("contentTypes")).size());
        assertEquals(version, src.get("version"));
      } else if (id.equals(fed1SourceId)) {
        assertThat(
            (Iterable<Object>) src.get("contentTypes"), hasItems(contentTypesInJSON.toArray()));
        assertEquals(contentTypes.size(), ((JSONArray) src.get("contentTypes")).size());
        assertEquals(version, src.get("version"));
      } else if (id.equals(fed2SourceId)) {
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
   * @throws Exception
   */
  @Test
  public void testGetDocumentResourceLocalSuccess() throws Exception {

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
   * @throws Exception
   */
  @Test
  public void testGetDocumentResourceFedSuccess() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String transformer = mockTestSetup(framework, TestType.RESOURCE_TEST);
    Response response = executeTest(framework, transformer, false, null);

    String responseMessage = IOUtils.toString((ByteArrayInputStream) response.getEntity());
    assertEquals(GET_STREAM, responseMessage);
    assertEquals(OK, response.getStatus());
    assertEquals(GET_TYPE_OUTPUT, response.getMetadata().toString());
  }

  /**
   * Tests that a geojson input has its InputTransformer invoked by the REST endpoint to create a
   * metacard that is then converted to XML and returned from the REST endpoint.
   *
   * @throws Exception
   */
  @Test
  public void testGetMetacardAsXml() throws Exception {

    CatalogFramework framework = givenCatalogFramework(SAMPLE_ID);
    String metacardXml =
        "<metacard ns2:id=\"assigned-when-ingested\">\r\n"
            + "<type>type.metacard</type>\r\n"
            + "<string name=\"title\">\r\n"
            + "<value>Title goes here ...</value>\r\n"
            + "</string>\r\n"
            + "<string name=\"metadata\">\r\n"
            + "<value>metadata goes here ...</value>\r\n"
            + "</metacard>";

    // Mock XmlMetacardTransformer that CatalogFramework will call to convert generated
    // metacard into XML to be returned from REST endpoint.
    final BinaryContent content = mock(BinaryContent.class);
    InputStream inputStream = new ByteArrayInputStream(metacardXml.getBytes(GET_OUTPUT_TYPE));
    when(content.getInputStream()).thenReturn(inputStream);
    when(content.getMimeTypeValue()).thenReturn("application/json;id=geojson");
    when(framework.transform(isA(Metacard.class), anyString(), isNull(Map.class)))
        .thenReturn(content);

    RESTEndpoint restEndpoint = new RESTEndpoint(framework);

    addMatchingService(restEndpoint, getSimpleMetacard());
    restEndpoint.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    restEndpoint.setFilterBuilder(filterBuilder);

    String json =
        "{\r\n"
            + "    \"properties\": {\r\n"
            + "        \"title\": \"myTitle\",\r\n"
            + "        \"thumbnail\": \"CA==\",\r\n"
            + "        \"resource-uri\": \"http://example.com\",\r\n"
            + "        \"created\": \"2012-09-01T00:09:19.368+0000\",\r\n"
            + "        \"metadata-content-type-version\": \"myVersion\",\r\n"
            + "        \"metadata-content-type\": \"myType\",\r\n"
            + "        \"metadata\": \"<xml>metadata goes here ...</xml>\",\r\n"
            + "        \"modified\": \"2012-09-01T00:09:19.368+0000\"\r\n"
            + "    },\r\n"
            + "    \"type\": \"Feature\",\r\n"
            + "    \"geometry\": {\r\n"
            + "        \"type\": \"Point\",\r\n"
            + "        \"coordinates\": [\r\n"
            + "            30.0,\r\n"
            + "            10.0\r\n"
            + "        ]\r\n"
            + "    }\r\n"
            + "} ";

    // Sample headers for a multipart body specifying a geojson file to have a metacard created for:
    //    Content-Disposition: form-data; name="file"; filename="C:\DDF\geojson_valid.json"
    //    Content-Type: application/json;id=geojson
    InputStream is = IOUtils.toInputStream(json);
    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=file; filename=C:\\DDF\\geojson_valid.json");
    Attachment attachment = new Attachment("file_part", is, contentDisposition);
    attachments.add(attachment);
    MediaType mediaType = new MediaType(MediaType.APPLICATION_JSON, "id=geojson");
    MultipartBody multipartBody = new MultipartBody(attachments, mediaType, true);

    UriInfo uriInfo = createSpecificUriInfo(LOCAL_RETRIEVE_ADDRESS);
    Response response =
        restEndpoint.createMetacard(
            multipartBody, uriInfo, RESTEndpoint.DEFAULT_METACARD_TRANSFORMER);
    assertEquals(OK, response.getStatus());
    InputStream responseEntity = (InputStream) response.getEntity();
    String responseXml = IOUtils.toString(responseEntity);
    assertEquals(metacardXml, responseXml);
  }

  /**
   * Test using a Head request to find out if Range headers are supported and to get resource size
   * of a local resource for use when using Range headers.
   *
   * @throws Exception
   */
  @Test
  public void testHeadRequestLocal() throws Exception {

    boolean isLocal = true;

    Response response = headTest(isLocal);

    assertEquals(NO_CONTENT, response.getStatus());
    assertEquals(ACCEPT_RANGES_VALUE, response.getHeaderString(HEADER_ACCEPT_RANGES));
    assertEquals(CONTENT_DISPOSITION_VALUE, response.getHeaderString(HEADER_CONTENT_DISPOSITION));
  }

  /**
   * Test using a Head request to find out if Range headers are supported and to get resource size
   * of a resource at a federated site for use when using Range headers.
   *
   * @throws Exception
   */
  @Test
  public void testHeadRequestFederated() throws Exception {

    boolean isLocal = false;

    Response response = headTest(isLocal);

    assertEquals(NO_CONTENT, response.getStatus());
    assertEquals(ACCEPT_RANGES_VALUE, response.getHeaderString(HEADER_ACCEPT_RANGES));
    assertEquals(CONTENT_DISPOSITION_VALUE, response.getHeaderString(HEADER_CONTENT_DISPOSITION));
  }

  private Response headTest(boolean local)
      throws CatalogTransformerException, URISyntaxException, UnsupportedEncodingException,
          UnsupportedQueryException, SourceUnavailableException, FederationException,
          IngestException {

    MetacardImpl metacard;
    List<Result> list = new ArrayList<>();
    Result result = mock(Result.class);
    InputStream inputStream;
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
    when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
        .thenReturn(resource);

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

  /**
   * Creates the mock setup for the GET tests above. Parameters provide the CatalogFramework, which
   * will be setup for the test, and also specify which test case is being run.
   *
   * @param framework
   * @param testType
   * @return
   * @throws Exception
   */
  protected String mockTestSetup(CatalogFramework framework, TestType testType)
      throws Exception, ResourceNotSupportedException {
    String transformer = null;
    QueryResponse queryResponse = mock(QueryResponse.class);
    when(framework.query(isA(QueryRequest.class), isNull(FederationStrategy.class)))
        .thenReturn(queryResponse);

    List<Result> list = null;
    MetacardImpl metacard = null;
    Result result = mock(Result.class);
    InputStream inputStream;

    switch (testType) {
      case QUERY_RESPONSE_TEST:
        when(queryResponse.getResults()).thenReturn(list);
        break;

      case METACARD_TEST:
        list = new ArrayList<>();
        list.add(result);
        when(queryResponse.getResults()).thenReturn(list);

        when(result.getMetacard()).thenReturn(metacard);
        break;

      case RESOURCE_TEST:
        transformer = "resource";
        /* FALLTHRU */
        // fall through
      case SUCCESS_TEST:
        list = new ArrayList<>();
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
        when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
            .thenReturn(resource);
        break;

      case KML_TEST:
        transformer = "kml";
        list = new ArrayList<>();
        list.add(result);
        when(queryResponse.getResults()).thenReturn(list);

        metacard = new MetacardImpl();
        metacard.setSourceId(GET_SITENAME);
        when(result.getMetacard()).thenReturn(metacard);

        BinaryContent content = mock(BinaryContent.class);
        inputStream = new ByteArrayInputStream(GET_STREAM.getBytes(GET_OUTPUT_TYPE));
        when(content.getInputStream()).thenReturn(inputStream);
        when(content.getMimeTypeValue()).thenReturn(GET_KML_MIME_TYPE);
        when(framework.transform(isA(Metacard.class), anyString(), isA(Map.class)))
            .thenReturn(content);
        break;
    }

    return transformer;
  }

  private Response executeTest(
      CatalogFramework framework, String transformer, boolean local, HttpServletRequest request)
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

  protected void assertExceptionThrown(Class<? extends Throwable> klass)
      throws IngestException, SourceUnavailableException, URISyntaxException,
          MetacardCreationException {

    CatalogFramework framework = mock(CatalogFramework.class);

    when(framework.create(isA(CreateRequest.class))).thenThrow(klass);

    when(framework.create(isA(CreateStorageRequest.class))).thenThrow(klass);

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));

    RESTEndpoint rest = new RESTEndpoint(framework);

    addMatchingService(rest, getSimpleMetacard());

    UriInfo info = givenUriInfo(SAMPLE_ID);

    try {
      Response response =
          rest.addDocument(
              headers,
              info,
              mock(HttpServletRequest.class),
              mock(MultipartBody.class),
              null,
              new ByteArrayInputStream("".getBytes()));
      if (klass.getName().equals(IngestException.class.getName())) {
        assertEquals(response.getStatus(), BAD_REQUEST);
      } else {
        fail();
      }
    } catch (InternalServerErrorException e) {
      if (klass.getName().equals(SourceUnavailableException.class.getName())) {
        assertThat(e.getResponse().getStatus(), equalTo(INTERNAL_SERVER_ERROR));
      }
    }
  }

  protected CatalogFramework givenCatalogFramework(String returnId)
      throws IngestException, SourceUnavailableException {
    CatalogFramework framework = mock(CatalogFramework.class);

    Metacard returnMetacard = mock(Metacard.class);

    when(returnMetacard.getId()).thenReturn(returnId);

    when(framework.create(isA(CreateRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));

    when(framework.create(isA(CreateStorageRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));

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
      when(inputTransformer.transform(isA(InputStream.class), isA(String.class)))
          .thenReturn(generatedMetacard);
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

  private Transform addMatchingService(RESTEndpoint rest, Metacard metacard)
      throws MetacardCreationException {

    Transform transform = mock(Transform.class);

    TransformResponse transformResponse = mock(TransformResponse.class);
    when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));

    when(transform.transform(any(MimeType.class), any(), any(), any(), any(), any()))
        .thenReturn(transformResponse);

    rest.setTransform(transform);

    return transform;
  }

  private HttpHeaders createHeaders(List<String> mimeTypeList) {

    HttpHeaders headers = mock(HttpHeaders.class);

    when(headers.getRequestHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(mimeTypeList);

    return headers;
  }

  private String getSample() {
    return "<xml></xml>";
  }

  protected enum TestType {
    QUERY_RESPONSE_TEST,
    METACARD_TEST,
    SUCCESS_TEST,
    RESOURCE_TEST,
    KML_TEST
  }
}
