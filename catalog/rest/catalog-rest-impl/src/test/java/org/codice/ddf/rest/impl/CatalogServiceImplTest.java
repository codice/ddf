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
package org.codice.ddf.rest.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.tika.io.IOUtils;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.attachment.impl.AttachmentParserImpl;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.rest.service.CatalogServiceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests methods of the {@link CatalogServiceImpl} */
public class CatalogServiceImplTest {

  private static final int INTERNAL_SERVER_ERROR = 500;

  private static final String SAMPLE_ID = "12345678900987654321abcdeffedcba";

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogServiceImplTest.class);

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

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    HttpHeaders headers = mock(HttpHeaders.class);

    try {
      catalogService.addDocument(
          headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
          mock(MultipartBody.class),
          null,
          null);
    } catch (CatalogServiceException e) {
      assertEquals(e.getMessage(), "No content found, cannot do CREATE.");
    }
  }

  @Test
  public void testAddDocumentFrameworkIngestException() throws Exception {

    assertExceptionThrown(IngestException.class);
  }

  @Test
  public void testAddDocumentFrameworkSourceUnavailableException() throws Exception {

    assertExceptionThrown(SourceUnavailableException.class);
  }

  @Test
  public void testAddDocumentPositiveCase() throws Exception {

    CatalogFramework framework = givenCatalogFramework();

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    String response =
        catalogService.addDocument(
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
            mock(MultipartBody.class),
            null,
            new ByteArrayInputStream("".getBytes()));

    LOGGER.debug(ToStringBuilder.reflectionToString(response));

    assertThat(response, equalTo(SAMPLE_ID));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithAttributeOverrides() throws Exception {

    CatalogFramework framework = givenCatalogFramework();

    AttributeDescriptor descriptor =
        new AttributeDescriptorImpl(
            "custom.attribute", true, true, false, false, BasicTypes.STRING_TYPE);

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));
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

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };

    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    catalogService.setUuidGenerator(uuidGenerator);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Attachment attachment =
        new Attachment(
            "parse.resource", new ByteArrayInputStream("Some Text".getBytes()), contentDisposition);
    attachments.add(attachment);
    ContentDisposition contentDisposition2 =
        new ContentDisposition("form-data; name=custom.attribute; ");
    Attachment attachment2 =
        new Attachment(
            descriptor.getName(),
            new ByteArrayInputStream("CustomValue".getBytes()),
            contentDisposition2);
    attachments.add(attachment2);
    MultipartBody multipartBody = new MultipartBody(attachments);

    String response =
        catalogService.addDocument(
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
            multipartBody,
            null,
            new ByteArrayInputStream("".getBytes()));

    LOGGER.debug(ToStringBuilder.reflectionToString(response));

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

  @Test
  @SuppressWarnings({"unchecked"})
  public void testAddDocumentWithMetadataPositiveCase() throws Exception {

    CatalogFramework framework = givenCatalogFramework();

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));
    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any())).thenReturn(new MetacardImpl());
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };

    UuidGenerator uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());
    catalogService.setUuidGenerator(uuidGenerator);

    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

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

    String response =
        catalogService.addDocument(
            headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
            multipartBody,
            null,
            new ByteArrayInputStream("".getBytes()));

    LOGGER.debug(ToStringBuilder.reflectionToString(response));

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
  public void testParseAttachments()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException {
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

    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogServiceImpl, Collections.singletonList(getSimpleTransformer()));

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

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseAttachments(attachments, "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

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

    attachmentInfoAndMetacard = catalogServiceImpl.parseAttachments(attachments, "xml");

    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("<meta>beta</meta>"));
    assertThat(attachmentInfoAndMetacard.getValue().getAttribute("foo"), equalTo(null));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testParseParts()
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

    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogServiceImpl, Collections.singletonList(getSimpleTransformer()));

    List<Part> parts = new ArrayList<>();
    Part part =
        createPart(
            "parse.resource",
            new ByteArrayInputStream("Some Text".getBytes()),
            "form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Part part1 =
        createPart(
            "parse.metadata",
            new ByteArrayInputStream("Some Text Again".getBytes()),
            "form-data; name=parse.metadata; filename=C:\\DDF\\metacard.xml");

    parts.add(part);
    parts.add(part1);

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getParts()).thenReturn(parts);

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseParts(parts, "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

    Part part2 =
        createPart(
            "metadata",
            new ByteArrayInputStream("<meta>beta</meta>".getBytes()),
            "form-data; name=metadata; filename=C:\\DDF\\metacard.xml");
    Part part3 =
        createPart(
            "foo",
            new ByteArrayInputStream("bar".getBytes()),
            "form-data; name=foo; filename=C:\\DDF\\metacard.xml");

    parts.add(part2);
    parts.add(part3);

    attachmentInfoAndMetacard = catalogServiceImpl.parseParts(parts, "xml");

    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("<meta>beta</meta>"));
    assertThat(attachmentInfoAndMetacard.getValue().getAttribute("foo"), equalTo(null));
  }

  @Test
  public void testParseAttachmentsWithAttributeOverrides()
      throws IngestException, SourceUnavailableException {
    CatalogFramework framework = givenCatalogFramework();
    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    when(attributeRegistry.lookup(Topic.KEYWORD))
        .thenReturn(Optional.of(new TopicAttributes().getAttributeDescriptor(Topic.KEYWORD)));
    when(attributeRegistry.lookup(Core.LOCATION))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.LOCATION)));

    List<Attachment> attachments = new ArrayList<>();
    ContentDisposition contentDisposition =
        new ContentDisposition("form-data; name=parse.resource; filename=/path/to/metacard.txt");
    Attachment attachment =
        new Attachment(
            "parse.resource", new ByteArrayInputStream("Some Text".getBytes()), contentDisposition);
    ContentDisposition contentDisposition1 =
        new ContentDisposition("form-data; name=parse.location");
    Attachment attachment1 =
        new Attachment(
            "parse.location",
            new ByteArrayInputStream("POINT(0 0)".getBytes()),
            contentDisposition1);
    ContentDisposition contentDisposition2 =
        new ContentDisposition("form-data; name=parse.topic.keyword");
    Attachment attachment2 =
        new Attachment(
            "parse.topic.keyword",
            new ByteArrayInputStream("keyword1".getBytes()),
            contentDisposition2);
    ContentDisposition contentDisposition3 =
        new ContentDisposition("form-data; name=parse.topic.keyword");
    Attachment attachment3 =
        new Attachment(
            "parse.topic.keyword",
            new ByteArrayInputStream("keyword2".getBytes()),
            contentDisposition3);
    attachments.add(attachment);
    attachments.add(attachment1);
    attachments.add(attachment2);
    attachments.add(attachment3);

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseAttachments(attachments, null);
    Metacard metacard = attachmentInfoAndMetacard.getValue();

    assertThat(metacard.getAttribute(Core.LOCATION).getValues(), hasItem("POINT(0 0)"));
    assertThat(metacard.getAttribute(Topic.KEYWORD).getValues(), hasItems("keyword1", "keyword2"));
  }

  @Test
  public void testParsePartsWithAttributeOverrides() throws Exception {
    CatalogFramework framework = givenCatalogFramework();
    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    when(attributeRegistry.lookup(Topic.KEYWORD))
        .thenReturn(Optional.of(new TopicAttributes().getAttributeDescriptor(Topic.KEYWORD)));
    when(attributeRegistry.lookup(Core.LOCATION))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.LOCATION)));

    List<Part> parts = new ArrayList<>();
    Part part =
        createPart(
            "parse.resource",
            new ByteArrayInputStream("Some Text".getBytes()),
            "form-data; name=parse.resource; filename=/path/to/metacard.txt");
    Part part1 =
        createPart(
            "parse.location",
            new ByteArrayInputStream("POINT(0 0)".getBytes()),
            "form-data; name=parse.location");

    Part part2 =
        createPart(
            "parse.topic.keyword",
            new ByteArrayInputStream("keyword1".getBytes()),
            "form-data; name=parse.topic.keyword");
    Part part3 =
        createPart(
            "parse.topic.keyword",
            new ByteArrayInputStream("keyword2".getBytes()),
            "form-data; name=parse.topic.keyword");

    parts.add(part);
    parts.add(part1);
    parts.add(part2);
    parts.add(part3);

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getParts()).thenReturn(parts);

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseParts(parts, null);
    Metacard metacard = attachmentInfoAndMetacard.getValue();

    assertThat(metacard.getAttribute(Core.LOCATION).getValues(), hasItem("POINT(0 0)"));
    assertThat(metacard.getAttribute(Topic.KEYWORD).getValues(), hasItems("keyword1", "keyword2"));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testParseAttachmentsTooLarge()
      throws IOException, CatalogTransformerException, SourceUnavailableException, IngestException,
          InvalidSyntaxException {
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

    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogServiceImpl, Collections.singletonList(getSimpleTransformer()));

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

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseAttachments(attachments, "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

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

    attachmentInfoAndMetacard = catalogServiceImpl.parseAttachments(attachments, "xml");

    // Ensure that the metadata was not overriden because it was too large to be parsed
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));
    assertThat(attachmentInfoAndMetacard.getValue().getAttribute("foo"), equalTo(null));
  }

  @Test
  @SuppressWarnings({"unchecked"})
  public void testParsePartsTooLarge()
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

    CatalogServiceImpl catalogServiceImpl =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));
    when(attributeRegistry.lookup("foo")).thenReturn(Optional.empty());

    addMatchingService(catalogServiceImpl, Collections.singletonList(getSimpleTransformer()));

    List<Part> parts = new ArrayList<>();
    Part part =
        createPart(
            "parse.resource",
            new ByteArrayInputStream("Some Text".getBytes()),
            "form-data; name=parse.resource; filename=C:\\DDF\\metacard.txt");
    Part part1 =
        createPart(
            "parse.metadata",
            new ByteArrayInputStream("Some Text Again".getBytes()),
            "form-data; name=parse.metadata; filename=C:\\DDF\\metacard.xml");

    parts.add(part);
    parts.add(part1);

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    when(httpServletRequest.getParts()).thenReturn(parts);

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard =
        catalogServiceImpl.parseParts(parts, "xml");
    assertThat(attachmentInfoAndMetacard.getValue().getMetadata(), equalTo("Some Text Again"));

    Part part2 =
        createPart(
            "metadata",
            new ByteArrayInputStream(Strings.repeat("hi", 100_000).getBytes()),
            "form-data; name=metadata; filename=C:\\DDF\\metacard.xml");
    Part part3 =
        createPart(
            "foo",
            new ByteArrayInputStream("bar".getBytes()),
            "form-data; name=foo; filename=C:\\DDF\\metacard.xml");

    parts.add(part2);
    parts.add(part3);

    attachmentInfoAndMetacard = catalogServiceImpl.parseParts(parts, "xml");

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

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    BinaryContent content = catalogService.getSourcesInfo();
    assertEquals(jsonMimeTypeString, content.getMimeTypeValue());

    String responseMessage = IOUtils.toString(content.getInputStream());
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
  public void testGetMetacardAsXml() throws Exception {

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
    when(framework.transform(isA(Metacard.class), anyString(), isNull(Map.class)))
        .thenReturn(content);

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    // Add a MimeTypeToINputTransformer that the REST endpoint will call to create the metacard
    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));
    catalogService.setTikaMimeTypeResolver(new TikaMimeTypeResolver());
    FilterBuilder filterBuilder = new GeotoolsFilterBuilder();
    catalogService.setFilterBuilder(filterBuilder);

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

    BinaryContent binaryContent =
        catalogService.createMetacard(
            multipartBody, CatalogServiceImpl.DEFAULT_METACARD_TRANSFORMER);
    InputStream responseEntity = binaryContent.getInputStream();
    String responseXml = IOUtils.toString(responseEntity);
    assertEquals(metacardXml, responseXml);
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

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));
    BundleContext bundleContext = mock(BundleContext.class);
    Collection<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    ServiceReference serviceReference = mock(ServiceReference.class);
    InputTransformer inputTransformer = mock(InputTransformer.class);
    when(inputTransformer.transform(any())).thenReturn(metacard);
    when(bundleContext.getService(serviceReference)).thenReturn(inputTransformer);
    serviceReferences.add(serviceReference);
    when(bundleContext.getServiceReferences(InputTransformer.class, "(id=xml)"))
        .thenReturn(serviceReferences);

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry) {
          @Override
          BundleContext getBundleContext() {
            return bundleContext;
          }
        };
    String generatedMcardId = UUID.randomUUID().toString();
    when(uuidGenerator.generateUuid()).thenReturn(generatedMcardId);
    catalogService.setUuidGenerator(uuidGenerator);
    when(attributeRegistry.lookup(Core.METADATA))
        .thenReturn(Optional.of(new CoreAttributes().getAttributeDescriptor(Core.METADATA)));

    addMatchingService(catalogService, Collections.singletonList(inputTransformer));

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

    MultipartBody multipartBody = new MultipartBody(attachments);

    return catalogService.addDocument(
        headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
        multipartBody,
        null,
        new ByteArrayInputStream("".getBytes()));
  }

  @SuppressWarnings({"unchecked"})
  private void assertExceptionThrown(Class<? extends Throwable> klass) throws Exception {

    CatalogFramework framework = mock(CatalogFramework.class);

    when(framework.create(isA(CreateRequest.class))).thenThrow(klass);

    when(framework.create(isA(CreateStorageRequest.class))).thenThrow(klass);

    HttpHeaders headers = createHeaders(Collections.singletonList(MediaType.APPLICATION_JSON));

    CatalogServiceImpl catalogService =
        new CatalogServiceImpl(framework, attachmentParser, attributeRegistry);

    addMatchingService(catalogService, Collections.singletonList(getSimpleTransformer()));

    try {
      catalogService.addDocument(
          headers.getRequestHeader(HttpHeaders.CONTENT_TYPE),
          mock(MultipartBody.class),
          null,
          new ByteArrayInputStream("".getBytes()));
    } catch (InternalServerErrorException e) {
      if (klass.getName().equals(SourceUnavailableException.class.getName())) {
        assertThat(e.getResponse().getStatus(), equalTo(INTERNAL_SERVER_ERROR));
      }
    } catch (CatalogServiceException e) {
      if (klass.getName().equals(IngestException.class.getName())) {
        assertEquals(e.getMessage(), "Error while storing entry in catalog: ");
      } else {
        fail();
      }
    }
  }

  private CatalogFramework givenCatalogFramework()
      throws IngestException, SourceUnavailableException {
    CatalogFramework framework = mock(CatalogFramework.class);

    Metacard returnMetacard = mock(Metacard.class);

    when(returnMetacard.getId()).thenReturn(CatalogServiceImplTest.SAMPLE_ID);

    when(framework.create(isA(CreateRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));

    when(framework.create(isA(CreateStorageRequest.class)))
        .thenReturn(new CreateResponseImpl(null, null, Collections.singletonList(returnMetacard)));

    return framework;
  }

  private Part createPart(String name, InputStream inputStream, String contentDisposition)
      throws IOException {
    Part part = mock(Part.class);
    when(part.getName()).thenReturn(name);
    when(part.getInputStream()).thenReturn(inputStream);
    when(part.getHeader("Content-Disposition")).thenReturn(contentDisposition);
    when(part.getContentType()).thenReturn(MediaType.APPLICATION_OCTET_STREAM);
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
      CatalogServiceImpl catalogServiceImpl, List<InputTransformer> sortedListOfTransformers) {

    MimeTypeToTransformerMapper matchingService = mock(MimeTypeToTransformerMapper.class);

    when(matchingService.findMatches(eq(InputTransformer.class), isA(MimeType.class)))
        .thenReturn((List) sortedListOfTransformers);

    catalogServiceImpl.setMimeTypeToTransformerMapper(matchingService);

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
