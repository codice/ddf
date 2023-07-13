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
package org.codice.ddf.rest.service.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Strings;
import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Topic;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.impl.SourceDescriptorImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.mime.tika.TikaMimeTypeResolver;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.activation.MimeType;
import javax.servlet.ServletException;
import javax.servlet.http.Part;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.attachment.impl.AttachmentParserImpl;
import org.codice.ddf.endpoints.rest.CatalogService;
import org.codice.ddf.endpoints.rest.CatalogServiceException;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests methods of the {@link CatalogService} */
public class CatalogServiceTest {

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogServiceTest.class);

  private static final String GET_OUTPUT_TYPE = "UTF-8";

  private AttachmentParser attachmentParser;

  private AttributeRegistry attributeRegistry;

  @Before
  public void setup() throws MimeTypeResolutionException {
    MimeTypeMapper mimeTypeMapper = mock(MimeTypeMapper.class);
    when(mimeTypeMapper.getMimeTypeForFileExtension("txt")).thenReturn("text/plain");
    when(mimeTypeMapper.getMimeTypeForFileExtension("xml")).thenReturn("text/xml");

    attachmentParser = new AttachmentParserImpl(mimeTypeMapper);

    attributeRegistry = mock(AttributeRegistry.class);
  }

  @Test
  public void testAddDocumentNullMessage() {

    CatalogFramework framework = mock(CatalogFramework.class);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);

    try {
      catalogService.addDocument(
          Collections.emptyList(), List.of(mock(FileItem.class)), null, null);
    } catch (CatalogServiceException | ServletException e) {
      assertEquals(e.getMessage(), "No content found, cannot do CREATE.");
    }
  }

  @Test
  public void testAddDocumentFrameworkIngestException() throws Exception {

    assertExceptionThrown(new IngestException("test"));
  }

  @Test
  public void testAddDocumentFrameworkSourceUnavailableException() throws Exception {

    assertExceptionThrown(new SourceUnavailableException("test"));
  }

  @Test
  public void testAddDocumentPositiveCase() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);
    FileItem fileItem = mock(FileItem.class);
    when(fileItem.getInputStream())
        .thenReturn(InputStream.nullInputStream(), InputStream.nullInputStream());

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    catalogService.setUuidGenerator(uuidGenerator);
    when(uuidGenerator.generateUuid()).thenReturn("12345678900987654321abcdeffedcba");

    String response =
        catalogService.addDocument(
            List.of("application/json"), List.of(fileItem), null, InputStream.nullInputStream());

    assertThat(response, equalTo(SAMPLE_ID));
  }

  @Test
  public void testAddDocumentPositiveCaseWithOverrides() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);
    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", null, "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock("parse.title", "application/octet-stream", "test", null);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    catalogService.setUuidGenerator(uuidGenerator);
    when(uuidGenerator.generateUuid()).thenReturn("12345678900987654321abcdeffedcba");

    String response =
        catalogService.addDocument(
            List.of("application/json"),
            List.of(fileItem1, fileItem2),
            null,
            InputStream.nullInputStream());

    assertThat(response, equalTo(SAMPLE_ID));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithAttributeOverrides() throws Exception {

    CatalogFramework framework = givenCatalogFramework();

    AttributeDescriptor descriptor =
        new AttributeDescriptorImpl(
            "custom.attribute", true, true, false, false, BasicTypes.STRING_TYPE);

    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any())).thenReturn(new MetacardImpl());
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    when(attributeRegistry.lookup("custom.attribute")).thenReturn(Optional.of(descriptor));

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry) {
          @Override
          protected BundleContext getBundleContext() {
            return bundleContext;
          }
        };

    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    catalogService.setUuidGenerator(uuidGenerator);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock("custom.attribute", "application/octet-stream", "CustomValue", null);

    String response =
        catalogService.addDocument(
            List.of("application/json"),
            List.of(fileItem1, fileItem2),
            null,
            InputStream.nullInputStream());

    ArgumentCaptor<CreateStorageRequest> captor =
        ArgumentCaptor.forClass(CreateStorageRequest.class);
    verify(framework, times(1)).create(captor.capture());
    assertThat(
        captor
            .getValue()
            .getContentItems()
            .get(0)
            .getMetacard()
            .getMetacardType()
            .getAttributeDescriptor(descriptor.getName()),
        equalTo(descriptor));
  }

  private static FileItem createFileItemMock(
      String fieldName, String contentType, String content, String filename) throws IOException {
    FileItem result = mock(FileItem.class);
    when(result.getFieldName()).thenReturn(fieldName);
    when(result.getContentType()).thenReturn(contentType);
    if (content == null) {
      when(result.getInputStream())
          .thenReturn(InputStream.nullInputStream(), InputStream.nullInputStream());
    } else {
      when(result.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    }
    if (filename != null) {
      when(result.getName()).thenReturn(filename);
    }
    return result;
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithMetadataPositiveCase() throws Exception {

    CatalogFramework framework = givenCatalogFramework();

    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any())).thenReturn(new MetacardImpl());
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry) {
          @Override
          protected BundleContext getBundleContext() {
            return bundleContext;
          }
        };

    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    catalogService.setUuidGenerator(uuidGenerator);

    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock(
            "parse.metadata",
            "application/octet-stream",
            "Some Text Again",
            "C:\\DDF\\metacard.xml");
    FileItem fileItem3 =
        createFileItemMock("metadata", "text/xml", "<meta>beta</meta>", "C:\\DDF\\metacard.xml");

    String response =
        catalogService.addDocument(
            List.of("application/json"),
            List.of(fileItem1, fileItem2, fileItem3),
            null,
            InputStream.nullInputStream());

    assertThat(response, equalTo(SAMPLE_ID));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithMetadataMetacardId() throws Exception {
    String inputMcardId = "123456789987654321";
    MetacardImpl inputMcard = new MetacardImpl();
    inputMcard.setId(inputMcardId);
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);

    String response = mcardIdTest(inputMcard, uuidGenerator);

    assertThat(response, equalTo(inputMcardId));

    verify(uuidGenerator, never()).generateUuid();
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithMetadataNoMcardId() throws Exception {

    MetacardImpl inputMcard = new MetacardImpl();
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);

    String response = mcardIdTest(inputMcard, uuidGenerator);

    assertThat(response, notNullValue());

    verify(uuidGenerator, times(1)).generateUuid();
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testParseFormUpload()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException, ServletException {
    CatalogFramework framework = givenCatalogFramework();
    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata("Some Text Again");
    when(inputTransformer.transform(any())).thenReturn(metacard);
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry) {
          @Override
          protected BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    FileItem fileItem1 =
        createFileItemMock(
            "parse.resource", "application/octet-stream", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock(
            "parse.metadata",
            "application/octet-stream",
            "Some Text Again",
            "C:\\DDF\\metacard.xml");

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogService.parseFormUpload(List.of(fileItem1, fileItem2), "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

    FileItem fileItem3 =
        createFileItemMock("metadata", "text/xml", "<meta>beta</meta>", "C:\\DDF\\metacard.xml");
    FileItem fileItem4 =
        createFileItemMock("foo", "application/octet-stream", "bar", "C:\\DDF\\metacard.txt");

    attachmentInfoAndMetacard =
        catalogService.parseFormUpload(List.of(fileItem1, fileItem2, fileItem3, fileItem4), "xml");

    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("<meta>beta</meta>"));
    assertThat(attachmentInfoAndMetacard.getValue().getAttribute("foo"), equalTo(null));
  }

  @Test
  public void testParseFormUploadWithAttributeOverrides() throws Exception {
    CatalogFramework framework = givenCatalogFramework();
    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);

    when(attributeRegistry.lookup(Topic.KEYWORD))
        .thenReturn(Optional.of(new TopicAttributes().getAttributeDescriptor(Topic.KEYWORD)));
    when(attributeRegistry.lookup(Core.LOCATION))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.LOCATION)));

    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock("parse.location", "application/octet-stream", "POINT(0 0)", null);
    FileItem fileItem3 =
        createFileItemMock("parse.topic.keyword", "application/octet-stream", "keyword1", null);
    FileItem fileItem4 =
        createFileItemMock("parse.topic.keyword", "application/octet-stream", "keyword2", null);

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogService.parseFormUpload(List.of(fileItem1, fileItem2, fileItem3, fileItem4), null);
    Metacard metacard = attachmentInfoAndMetacard.getValue();

    assertThat(metacard.getAttribute(Core.LOCATION).getValues(), hasItem("POINT(0 0)"));
    assertThat(metacard.getAttribute(Topic.KEYWORD).getValues(), hasItems("keyword1", "keyword2"));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testParseFormUploadTooLarge()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException, ServletException {
    CatalogFramework framework = givenCatalogFramework();
    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata("Some Text Again");
    when(inputTransformer.transform(any())).thenReturn(metacard);
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry) {
          @Override
          protected BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock(
            "parse.metadata",
            "application/octet-stream",
            "Some Text Again",
            "C:\\DDF\\metacard.xml");

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogService.parseFormUpload(List.of(fileItem1, fileItem2), "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

    FileItem fileItem3 =
        createFileItemMock(
            "metadata", "text/xml", Strings.repeat("hi", 100_000), "C:\\DDF\\metacard.xml");
    FileItem fileItem4 =
        createFileItemMock("foo", "application/octet-stream", "bar", "C:\\DDF\\metacard.txt");

    attachmentInfoAndMetacard =
        catalogService.parseFormUpload(List.of(fileItem1, fileItem2, fileItem3, fileItem4), "xml");

    // Ensure that the metadata was not overriden because it was too large to be parsed
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));
    assertThat(attachmentInfoAndMetacard.getValue().getAttribute("foo"), equalTo(null));
  }

  /** Tests getting source information */
  @Test
  @SuppressWarnings({"unchecked"})
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
    SourceDescriptorImpl localDescriptor =
        new SourceDescriptorImpl(localSourceId, contentTypes, Collections.emptyList());
    localDescriptor.setVersion(version);
    localDescriptor.setAvailable(true);
    SourceDescriptorImpl fed1Descriptor =
        new SourceDescriptorImpl(fed1SourceId, contentTypes, Collections.emptyList());
    fed1Descriptor.setVersion(version);
    fed1Descriptor.setAvailable(true);
    SourceDescriptorImpl fed2Descriptor =
        new SourceDescriptorImpl(fed2SourceId, null, Collections.emptyList());
    fed2Descriptor.setAvailable(true);

    sourceDescriptors.add(localDescriptor);
    sourceDescriptors.add(fed1Descriptor);
    sourceDescriptors.add(fed2Descriptor);

    SourceInfoResponse sourceInfoResponse =
        new SourceInfoResponseImpl(null, null, sourceDescriptors);

    CatalogFramework framework = mock(CatalogFramework.class);
    when(framework.getSourceInfo(isA(SourceInfoRequestEnterprise.class)))
        .thenReturn(sourceInfoResponse);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);

    BinaryContent content = catalogService.getSourcesInfo();
    assertEquals(jsonMimeTypeString, content.getMimeTypeValue());

    String responseMessage = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
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
   * Tests that a geojson input has its InputTransformer invoked by the REST endpoint to create a
   * metacard that is then converted to XML and returned from the REST endpoint.
   */
  @Test
  @SuppressWarnings({"unchecked"})
  public void testGetMetacardAsXmlFromGeojson() throws Exception {

    CatalogFramework framework = givenCatalogFramework();
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
    when(framework.transform(isA(Metacard.class), anyString(), isNull())).thenReturn(content);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);

    // Add a MimeTypeToINputTransformer that the REST endpoint will call to create the metacard
    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));
    catalogService.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    catalogService.setFilterBuilder(filterBuilder);
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    catalogService.setUuidGenerator(uuidGenerator);
    when(uuidGenerator.generateUuid()).thenReturn("12345678900987654321abcdeffedcba");

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
    FileItem fileItem =
        createFileItemMock(
            "parse.resource", "application/json;id=geojson", json, "C:\\DDF\\geojson_valid.json");

    String id =
        catalogService.addDocument(
            List.of("multipart/form-data"),
            List.of(fileItem),
            CatalogService.DEFAULT_METACARD_TRANSFORMER,
            mock(InputStream.class));
    assertEquals("12345678900987654321abcdeffedcba", id);
  }

  private String mcardIdTest(Metacard metacard, UuidGenerator uuidGenerator) throws Exception {
    CatalogFramework framework = mock(CatalogFramework.class);

    when(framework.create(isA(CreateStorageRequest.class)))
        .thenAnswer(
            args -> {
              ContentItem item =
                  ((CreateStorageRequest) args.getArguments()[0]).getContentItems().get(0);
              item.getMetacard().setAttribute(new AttributeImpl(Core.ID, item.getId()));
              return new CreateResponseImpl(
                  null, new HashMap<>(), Collections.singletonList(item.getMetacard()));
            });

    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any())).thenReturn(metacard);
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry) {
          @Override
          protected BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    String generatedMcardId = UUID.randomUUID().toString();
    when(uuidGenerator.generateUuid()).thenReturn(generatedMcardId);
    catalogService.setUuidGenerator(uuidGenerator);
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));

    addMatchingService(catalogService, Collections.singletonList(inputTransformer));

    FileItem fileItem1 =
        createFileItemMock("parse.resource", "text/plain", "Some Text", "C:\\DDF\\metacard.txt");
    FileItem fileItem2 =
        createFileItemMock(
            "parse.metadata",
            "application/octet-stream",
            "Some Text Again",
            "C:\\DDF\\metacard.xml");

    return catalogService.addDocument(
        List.of("application/json"),
        List.of(fileItem1, fileItem2),
        null,
        InputStream.nullInputStream());
  }

  @SuppressWarnings({"unchecked"})
  private void assertExceptionThrown(Throwable thrown) throws Exception {

    CatalogFramework framework = mock(CatalogFramework.class);

    when(framework.create(isA(CreateRequest.class))).thenThrow(thrown);
    when(framework.create(isA(CreateStorageRequest.class))).thenThrow(thrown);

    CatalogService catalogService =
        new CatalogService(framework, attachmentParser, attributeRegistry);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));
    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    catalogService.setUuidGenerator(uuidGenerator);
    when(uuidGenerator.generateUuid()).thenReturn("12345678900987654321abcdeffedcba");
    FileItem fileItem = mock(FileItem.class);
    when(fileItem.getInputStream()).thenReturn(InputStream.nullInputStream());

    try {
      catalogService.addDocument(
          List.of("application/json"), List.of(fileItem), null, InputStream.nullInputStream());
    } catch (ServletException e) {
      if (thrown.getClass().getName().equals(SourceUnavailableException.class.getName())) {
        assertEquals(
            e.getMessage(), "Cannot create catalog entry because source is unavailable: test");
      }
    } catch (CatalogServiceException e) {
      if (thrown.getClass().getName().equals(IngestException.class.getName())) {
        assertEquals(e.getMessage(), "Error while storing entry in catalog: test");
      } else {
        fail();
      }
    }
  }

  private CatalogFramework givenCatalogFramework()
      throws IngestException, SourceUnavailableException {
    CatalogFramework framework = mock(CatalogFramework.class);

    Metacard returnMetacard = mock(Metacard.class);

    when(returnMetacard.getId()).thenReturn(CatalogServiceTest.SAMPLE_ID);

    when(framework.create(isA(CreateRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));

    when(framework.create(isA(CreateStorageRequest.class)))
        .thenAnswer(
            (Answer<CreateResponseImpl>)
                invocationOnMock -> {
                  assertThat(
                      ((CreateStorageRequest) invocationOnMock.getArguments()[0])
                          .getContentItems()
                          .get(0)
                          .getInputStream()
                          .available(),
                      greaterThanOrEqualTo(0));
                  return new CreateResponseImpl(
                      null, null, Collections.singletonList(returnMetacard));
                });

    return framework;
  }

  private Part createPart(String name, InputStream inputStream, String contentDisposition)
      throws IOException {
    Part part = mock(Part.class);
    when(part.getName()).thenReturn(name);
    when(part.getInputStream()).thenReturn(inputStream);
    when(part.getHeader("Content-Disposition")).thenReturn(contentDisposition);
    when(part.getContentType()).thenReturn("application/octet-stream");
    return part;
  }

  private Metacard getSimpleMetacard() {
    MetacardImpl generatedMetacard = new MetacardImpl();
    generatedMetacard.setMetadata(getSample());
    generatedMetacard.setId(SAMPLE_ID);

    return generatedMetacard;
  }

  private InputTransformer getSimpleTransformer() {
    return new InputTransformer() {

      @Override
      public Metacard transform(InputStream input, String id) {
        return getSimpleMetacard();
      }

      @Override
      public Metacard transform(InputStream input) {
        return getSimpleMetacard();
      }
    };
  }

  @SuppressWarnings({"unchecked"})
  private MimeTypeToTransformerMapper addMatchingService(
      CatalogService catalogService, List<InputTransformer> sortedListOfTransformers) {

    MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

    when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
        .thenReturn((List) sortedListOfTransformers);

    catalogService.setMimeTypeToTransformerMapper(matchingService);

    return matchingService;
  }

  private String getSample() {
    return "<xml></xml>";
  }
}
