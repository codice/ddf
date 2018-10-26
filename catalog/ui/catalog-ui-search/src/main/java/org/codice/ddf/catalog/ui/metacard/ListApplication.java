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
package org.codice.ddf.catalog.ui.metacard;

import static ddf.catalog.data.AttributeType.AttributeFormat.BINARY;
import static ddf.catalog.data.AttributeType.AttributeFormat.OBJECT;
import static spark.Spark.post;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.catalog.ui.metacard.impl.StorableResourceImpl;
import org.codice.ddf.catalog.ui.metacard.internal.Splitter;
import org.codice.ddf.catalog.ui.metacard.internal.SplitterLocator;
import org.codice.ddf.catalog.ui.metacard.internal.StorableResource;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Response;
import spark.servlet.SparkApplication;

public class ListApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(ListApplication.class);

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final String LIST_TYPE_HEADER = "List-Type";

  private static final int MAX_INPUT_SIZE = 65_536;

  private final MimeTypeMapper mimeTypeMapper;

  private final CatalogFramework catalogFramework;

  private final UuidGenerator uuidGenerator;

  private final SplitterLocator splitterLocator;

  private final AttachmentParser attachmentParser;

  private AttributeRegistry attributeRegistry;

  public ListApplication(
      MimeTypeMapper mimeTypeMapper,
      CatalogFramework catalogFramework,
      UuidGenerator uuidGenerator,
      SplitterLocator splitterLocator,
      AttachmentParser attachmentParser,
      AttributeRegistry attributeRegistry) {
    this.mimeTypeMapper = mimeTypeMapper;
    this.catalogFramework = catalogFramework;
    this.uuidGenerator = uuidGenerator;
    this.splitterLocator = splitterLocator;
    this.attachmentParser = attachmentParser;
    this.attributeRegistry = attributeRegistry;
  }

  @Override
  public void init() {
    post(
        "/list/import",
        (request, response) -> {
          MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
          request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);

          String listType = request.headers(LIST_TYPE_HEADER);

          if (StringUtils.isBlank(listType)) {
            String exceptionMessage = String.format("The header %s must be set.", LIST_TYPE_HEADER);
            LOGGER.info(exceptionMessage);
            createBadRequestResponse(exceptionMessage, response);
            return null;
          }

          List<Part> parts = new ArrayList<>(request.raw().getParts());

          Pair<AttachmentInfo, Metacard> attachmentInfo = parseAttachments(parts);

          if (attachmentInfo == null) {
            String exceptionMessage = "Unable to parse the attachments.";
            LOGGER.debug(exceptionMessage);
            createBadRequestResponse(exceptionMessage, response);
            return null;
          }

          try (TemporaryFileBackedOutputStream temporaryFileBackedOutputStream =
              new TemporaryFileBackedOutputStream()) {

            IOUtils.copy(attachmentInfo.getLeft().getStream(), temporaryFileBackedOutputStream);

            for (Splitter splitter : lookupSplitters(attachmentInfo.getLeft().getContentType())) {
              if (attemptToSplitAndStore(
                  response, listType, attachmentInfo, temporaryFileBackedOutputStream, splitter))
                break;
            }
          }

          IOUtils.closeQuietly(attachmentInfo.getLeft().getStream());

          return "";
        });
  }

  private boolean attemptToSplitAndStore(
      Response response,
      String listType,
      Pair<AttachmentInfo, Metacard> attachmentInfo,
      TemporaryFileBackedOutputStream temporaryFileBackedOutputStream,
      Splitter splitter)
      throws IOException {
    List<String> ids = new LinkedList<>();
    List<String> errorMessages = new LinkedList<>();

    boolean isSplitSuccessful = false;

    try (InputStream temporaryInputStream =
        temporaryFileBackedOutputStream.asByteSource().openStream()) {

      AttachmentInfo temporaryAttachmentInfo =
          new AttachmentInfoImpl(
              temporaryInputStream,
              attachmentInfo.getLeft().getFilename(),
              attachmentInfo.getLeft().getContentType());

      try (Stream<StorableResource> stream =
          createStream(temporaryAttachmentInfo, splitter, listType)) {
        isSplitSuccessful = true;
        stream
            .sequential()
            .map(storableResource -> appendMessageIfError(errorMessages, storableResource))
            .filter(storableResource -> !storableResource.isError())
            .forEach(
                storableResource ->
                    storeAndClose(
                        ids::add, errorMessages::add, storableResource, attachmentInfo.getRight()));
      } catch (IOException e) {
        LOGGER.debug("Failed to split the incoming data. Trying the next splitter.", e);
      }

      if (isSplitSuccessful) {
        /** TODO: DDF-3800 - Display these error messages in the UI. */
        errorMessages.forEach(s -> LOGGER.debug("Unable to ingest split item: {}", s));

        response.header("Added-IDs", String.join(",", ids));
        return true;
      }
    }
    return false;
  }

  private void storeAndClose(
      Consumer<String> idConsumer,
      Consumer<String> errorMessageConsumer,
      StorableResource storableResource,
      Metacard right) {

    try {
      store(getAttachmentInfo(storableResource), idConsumer, errorMessageConsumer, right);
    } catch (IOException e) {
      LOGGER.debug("Unable to create AttachmentInfo: ", e);
    } finally {
      try {
        storableResource.close();
      } catch (Exception e) {
        LOGGER.trace("Unable to close resource. Will continue.", e);
      }
    }
  }

  private Stream<StorableResource> createStream(
      AttachmentInfo attachmentInfo, Splitter splitter, String listType) throws IOException {
    return splitter.split(
        createStorableResource(attachmentInfo),
        Collections.singletonMap(LIST_TYPE_HEADER, listType));
  }

  private StorableResource createStorableResource(AttachmentInfo attachmentInfo)
      throws IOException {
    return new StorableResourceImpl(
        attachmentInfo.getStream(), attachmentInfo.getContentType(), attachmentInfo.getFilename());
  }

  private StorableResource appendMessageIfError(
      List<String> errorMessages, StorableResource storableResource) {
    if (storableResource.isError()) {
      errorMessages.add(storableResource.getErrorMessage());
    }
    return storableResource;
  }

  private AttachmentInfo getAttachmentInfo(StorableResource storableResource) throws IOException {
    return new AttachmentInfoImpl(
        storableResource.getInputStream(),
        storableResource.getFilename(),
        storableResource
            .getMimeType()
            .orElse(contentTypeFromFilename(storableResource.getFilename())));
  }

  Pair<AttachmentInfo, Metacard> parseAttachments(List<Part> contentParts) {

    if (contentParts.size() == 1) {
      Part contentPart = contentParts.get(0);

      InputStream attachmentInputStream = null;

      try {
        attachmentInputStream = contentPart.getInputStream();
      } catch (IOException e) {
        LOGGER.debug("IOException reading stream from file attachment in multipart body.", e);
      }

      return new ImmutablePair<>(
          attachmentParser.generateAttachmentInfo(
              attachmentInputStream,
              contentPart.getContentType(),
              contentPart.getSubmittedFileName()),
          null);
    }

    Map<String, AttributeImpl> attributeMap = new HashMap<>();
    Metacard metacard = null;
    AttachmentInfo attachmentInfo = null;

    for (Part attachment : contentParts) {
      String name = attachment.getName();
      String parsedName = (name.startsWith("parse.")) ? name.substring(6) : name;
      try {
        InputStream inputStream = attachment.getInputStream();
        switch (name) {
          case "parse.resource":
            attachmentInfo =
                attachmentParser.generateAttachmentInfo(
                    inputStream,
                    attachment.getContentType().toString(),
                    attachment.getSubmittedFileName());
            break;
          default:
            parseOverrideAttributes(attributeMap, parsedName, inputStream);
            break;
        }
      } catch (IOException e) {
        LOGGER.debug(
            "Unable to get input stream for mime attachment. Ignoring override attribute: {}",
            name,
            e);
      }
    }
    if (attachmentInfo == null) {
      throw new IllegalArgumentException("No parse.resource specified in request.");
    }
    metacard = new MetacardImpl();

    Set<AttributeDescriptor> missingDescriptors = new HashSet<>();
    for (Attribute attribute : attributeMap.values()) {
      if (metacard.getMetacardType().getAttributeDescriptor(attribute.getName()) == null) {
        attributeRegistry.lookup(attribute.getName()).ifPresent(missingDescriptors::add);
      }
      metacard.setAttribute(attribute);
    }

    if (!missingDescriptors.isEmpty()) {
      MetacardType original = metacard.getMetacardType();
      MetacardImpl newMetacard = new MetacardImpl(metacard);
      newMetacard.setType(new MetacardTypeImpl(original.getName(), original, missingDescriptors));
      metacard = newMetacard;
    }

    return new ImmutablePair<>(attachmentInfo, metacard);
  }

  private void parseOverrideAttributes(
      Map<String, AttributeImpl> attributeMap, String parsedName, InputStream inputStream) {
    attributeRegistry
        .lookup(parsedName)
        .ifPresent(
            descriptor ->
                parseAttribute(
                    attributeMap,
                    parsedName,
                    inputStream,
                    descriptor.getType().getAttributeFormat()));
  }

  private void parseAttribute(
      Map<String, AttributeImpl> attributeMap,
      String parsedName,
      InputStream inputStream,
      AttributeType.AttributeFormat attributeFormat) {
    try (InputStream is = inputStream;
        InputStream boundedStream = new BoundedInputStream(is, MAX_INPUT_SIZE + 1)) {
      if (attributeFormat == OBJECT) {
        LOGGER.debug("Object type not supported for override");
        return;
      }

      byte[] bytes = IOUtils.toByteArray(boundedStream);
      if (bytes.length > MAX_INPUT_SIZE) {
        LOGGER.debug("Attribute length is limited to {} bytes", MAX_INPUT_SIZE);
        return;
      }

      AttributeImpl attribute;
      if (attributeMap.containsKey(parsedName)) {
        attribute = attributeMap.get(parsedName);
      } else {
        attribute = new AttributeImpl(parsedName, Collections.emptyList());
        attributeMap.put(parsedName, attribute);
      }

      if (attributeFormat == BINARY) {
        attribute.addValue(bytes);
        return;
      }

      String value = new String(bytes, Charset.defaultCharset());

      switch (attributeFormat) {
        case XML:
        case GEOMETRY:
        case STRING:
          attribute.addValue(value);
          break;
        case BOOLEAN:
          attribute.addValue(Boolean.valueOf(value));
          break;
        case SHORT:
          attribute.addValue(Short.valueOf(value));
          break;
        case LONG:
          attribute.addValue(Long.valueOf(value));
          break;
        case INTEGER:
          attribute.addValue(Integer.valueOf(value));
          break;
        case FLOAT:
          attribute.addValue(Float.valueOf(value));
          break;
        case DOUBLE:
          attribute.addValue(Double.valueOf(value));
          break;
        case DATE:
          try {
            Instant instant = Instant.parse(value);
            attribute.addValue(Date.from(instant));
          } catch (DateTimeParseException e) {
            LOGGER.debug("Unable to parse instant '{}'", attribute, e);
          }
          break;
        default:
          LOGGER.debug("Attribute format '{}' not supported", attributeFormat);
          break;
      }
    } catch (IOException e) {
      LOGGER.debug("Unable to read attribute to override", e);
    }
  }

  private AttachmentInfo parseAttachment(Part contentPart) {
    InputStream stream = null;

    try {
      stream = contentPart.getInputStream();
    } catch (IOException e) {
      LOGGER.info("IOException reading stream from file attachment in multipart body", e);
    }

    return attachmentParser.generateAttachmentInfo(
        stream, contentPart.getContentType(), contentPart.getSubmittedFileName());
  }

  private String contentTypeFromFilename(String filename) {
    String fileExtension = FilenameUtils.getExtension(filename);
    String contentType = null;
    try {
      contentType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
    } catch (MimeTypeResolutionException e) {
      LOGGER.debug("Unable to get contentType based on filename extension {}", fileExtension);
    }
    LOGGER.debug("Refined contentType = {}", contentType);
    return contentType;
  }

  private List<Splitter> lookupSplitters(String mimeType) throws MimeTypeParseException {
    List<Splitter> splitters = splitterLocator.find(new MimeType(mimeType));
    if (CollectionUtils.isEmpty(splitters)) {
      LOGGER.debug("Unable to find a splitter for mime-type {}", mimeType);
    }
    return splitters;
  }

  private void store(
      AttachmentInfo createInfo,
      Consumer<String> idConsumer,
      Consumer<String> errorMessageConsumer,
      Metacard right) {

    CreateStorageRequest streamCreateRequest =
        new CreateStorageRequestImpl(
            Collections.singletonList(
                new IncomingContentItem(
                    uuidGenerator,
                    createInfo.getStream(),
                    createInfo.getContentType(),
                    createInfo.getFilename(),
                    right)),
            null);
    try {
      CreateResponse createResponse = catalogFramework.create(streamCreateRequest);

      createResponse.getCreatedMetacards().stream().map(Metacard::getId).forEach(idConsumer);

    } catch (IngestException e) {
      String errorMessage = "Error while storing entry in catalog.";
      LOGGER.info(errorMessage, e);
      INGEST_LOGGER.warn(errorMessage, e);
      errorMessageConsumer.accept(errorMessage);
    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot create catalog entry because source is unavailable.";
      LOGGER.info(exceptionMessage, e);
      INGEST_LOGGER.warn(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    }
  }

  private void createBadRequestResponse(String entityMessage, Response response) {
    response.status(Status.BAD_REQUEST.getStatusCode());
    response.body("<pre>" + entityMessage + "</pre>");
    response.type(MediaType.TEXT_HTML);
  }

  protected static class IncomingContentItem extends ContentItemImpl {

    private InputStream inputStream;

    private IncomingContentItem(
        UuidGenerator uuidGenerator,
        InputStream inputStream,
        String mimeTypeRawData,
        String filename,
        Metacard right) {
      super(uuidGenerator.generateUuid(), null, mimeTypeRawData, filename, 0L, right);
      this.inputStream = inputStream;
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }
  }

  private static class AttachmentInfoImpl implements AttachmentInfo {

    private InputStream inputStream;
    private String filename;
    private String contentType;

    AttachmentInfoImpl(InputStream inputStream, String filename, String contentType) {
      this.inputStream = inputStream;
      this.filename = filename;
      this.contentType = contentType;
    }

    @Override
    public InputStream getStream() {
      return inputStream;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public String getContentType() {
      return contentType;
    }
  }
}
