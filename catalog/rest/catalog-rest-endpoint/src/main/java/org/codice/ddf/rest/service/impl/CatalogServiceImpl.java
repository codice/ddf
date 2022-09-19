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

import com.google.common.collect.Iterables;
import ddf.action.Action;
import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.InternalIngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletException;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.rest.api.CatalogService;
import org.codice.ddf.rest.api.CatalogServiceException;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.owasp.html.HtmlPolicyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogServiceImpl implements CatalogService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogServiceImpl.class);

  public static final String DEFAULT_METACARD_TRANSFORMER = "xml";

  private static final String BYTES_TO_SKIP = "BytesToSkip";

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final String FILE_ATTACHMENT_CONTENT_ID = "file";

  private static final String JSON_MIME_TYPE_STRING = "application/json";

  private static final String NO_FILE_CONTENTS_ATT_FOUND = "No file contents attachment found";

  private static final int MAX_INPUT_SIZE = 65_536;

  private FilterBuilder filterBuilder;

  private UuidGenerator uuidGenerator;

  protected static MimeType jsonMimeType;

  static {
    MimeType mime = null;
    try {
      mime = new MimeType(JSON_MIME_TYPE_STRING);
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failed to create json mimetype.");
    }
    jsonMimeType = mime;
  }

  private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  private MimeTypeResolver tikaMimeTypeResolver;

  protected AttachmentParser attachmentParser;

  protected AttributeRegistry attributeRegistry;

  protected CatalogFramework catalogFramework;

  public CatalogServiceImpl(
      CatalogFramework framework,
      AttachmentParser attachmentParser,
      AttributeRegistry attributeRegistry) {
    LOGGER.trace("Constructing CatalogServiceImpl");
    this.catalogFramework = framework;
    this.attachmentParser = attachmentParser;
    this.attributeRegistry = attributeRegistry;
    LOGGER.trace(("CatalogServiceImpl constructed successfully"));
  }

  protected BundleContext getBundleContext() {
    Bundle bundle = FrameworkUtil.getBundle(CatalogServiceImpl.class);
    return bundle == null ? null : bundle.getBundleContext();
  }

  @Override
  public BinaryContent getHeaders(
      String sourceid,
      String id,
      String transform,
      String absolutePath,
      Map<String, String[]> parameters)
      throws CatalogServiceException, ServletException {
    QueryResponse queryResponse;
    Metacard card = null;
    LOGGER.trace("getHeaders");

    if (id == null) {
      throw new CatalogServiceException("No ID specified.");
    }

    LOGGER.debug("Got id: {}", id);
    LOGGER.debug("Map of query parameters: \n{}", parameters);

    Map<String, Serializable> convertedMap = new HashMap<>(parameters);
    convertedMap.put("url", absolutePath);

    LOGGER.debug("Map converted, retrieving product.");

    // default to xml if no transformer specified
    try {
      String transformer = DEFAULT_METACARD_TRANSFORMER;
      if (transform != null && !transform.isBlank()) {
        transformer = transform;
      }

      Filter filter = getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id);

      Collection<String> sources = null;
      if (sourceid != null) {
        sources = new ArrayList<>();
        sources.add(sourceid);
      }

      QueryRequestImpl request = new QueryRequestImpl(new QueryImpl(filter), sources);
      request.setProperties(convertedMap);
      queryResponse = catalogFramework.query(request, null);

      // pull the metacard out of the blocking queue
      List<Result> results = queryResponse.getResults();

      if (results != null && !results.isEmpty()) {
        card = results.get(0).getMetacard();
      }

      if (card == null) {
        return null;
      }

      LOGGER.debug("Calling transform.");
      final BinaryContent content = catalogFramework.transform(card, transformer, convertedMap);
      LOGGER.debug("Read and transform complete, preparing response.");

      return content;

    } catch (FederationException e) {
      String exceptionMessage = "READ failed due to unexpected exception: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (CatalogTransformerException e) {
      String exceptionMessage = "Unable to transform Metacard.  Try different transformer: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot obtain query results because source is unavailable: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (UnsupportedQueryException e) {
      String errorMessage = "Specified query is unsupported.  Change query and resubmit: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage + e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new CatalogServiceException(e.getMessage());
    }
  }

  public JSONObject sourceActionToJSON(Action action) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("title", action.getTitle());
    jsonObject.put("url", action.getUrl().toString());
    jsonObject.put("description", action.getDescription());
    jsonObject.put("id", action.getId());
    return jsonObject;
  }

  @Override
  public BinaryContent getSourcesInfo() {
    JSONArray resultsList = new JSONArray();
    SourceInfoResponse sources;
    String sourcesString;

    try {
      SourceInfoRequestEnterprise sourceInfoRequestEnterprise =
          new SourceInfoRequestEnterprise(true);

      sources = catalogFramework.getSourceInfo(sourceInfoRequestEnterprise);
      for (SourceDescriptor source : sources.getSourceInfo()) {
        JSONObject sourceObj = new JSONObject();
        sourceObj.put("id", source.getSourceId());
        sourceObj.put("version", source.getVersion() != null ? source.getVersion() : "");
        sourceObj.put("available", Boolean.valueOf(source.isAvailable()));

        List<JSONObject> sourceActions =
            source.getActions().stream().map(this::sourceActionToJSON).collect(Collectors.toList());

        sourceObj.put("sourceActions", sourceActions);

        JSONArray contentTypesObj = new JSONArray();
        if (source.getContentTypes() != null) {
          for (ContentType contentType : source.getContentTypes()) {
            if (contentType != null && contentType.getName() != null) {
              JSONObject contentTypeObj = new JSONObject();
              contentTypeObj.put("name", contentType.getName());
              contentTypeObj.put(
                  "version", contentType.getVersion() != null ? contentType.getVersion() : "");
              contentTypesObj.add(contentTypeObj);
            }
          }
        }
        sourceObj.put("contentTypes", contentTypesObj);
        resultsList.add(sourceObj);
      }
    } catch (SourceUnavailableException e) {
      LOGGER.info("Unable to retrieve Sources. {}", e.getMessage());
      LOGGER.debug("Unable to retrieve Sources", e);
    }

    sourcesString = JSONValue.toJSONString(resultsList);
    return new BinaryContentImpl(
        new ByteArrayInputStream(sourcesString.getBytes(StandardCharsets.UTF_8)), jsonMimeType);
  }

  @Override
  public BinaryContent getDocument(
      String encodedSourceId,
      String encodedId,
      String transformerParam,
      long bytesToSkip,
      String absolutePath,
      Map<String, String[]> queryParameters)
      throws CatalogServiceException, DataUsageLimitExceededException, ServletException {

    QueryResponse queryResponse;
    Metacard card = null;
    LOGGER.trace("GET");

    if (encodedId == null) {
      throw new CatalogServiceException("No ID specified.");
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Got id: {}", encodedId);
      LOGGER.debug("Got service: {}", transformerParam);
      LOGGER.debug("Map of query parameters: \n{}", queryParameters);
    }
    Map<String, Serializable> convertedMap = new HashMap<>(queryParameters);
    convertedMap.put("url", absolutePath);

    LOGGER.debug("Map converted, retrieving product.");

    // default to xml if no transformer specified
    try {
      String id = URLDecoder.decode(encodedId, CharEncoding.UTF_8);

      String transformer = DEFAULT_METACARD_TRANSFORMER;
      if (transformerParam != null) {
        transformer = transformerParam;
      }
      Filter filter = getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id);

      Collection<String> sources = null;
      if (encodedSourceId != null) {
        String sourceid = URLDecoder.decode(encodedSourceId, CharEncoding.UTF_8);
        sources = new ArrayList<>();
        sources.add(sourceid);
      }

      QueryRequestImpl request = new QueryRequestImpl(new QueryImpl(filter), sources);
      request.setProperties(convertedMap);
      queryResponse = catalogFramework.query(request, null);

      // pull the metacard out of the blocking queue
      List<Result> results = queryResponse.getResults();

      // TODO: should be poll? do we want to specify a timeout? (will
      // return null if timeout elapsed)
      if (results != null && !results.isEmpty()) {
        card = results.get(0).getMetacard();
      }

      if (card == null) {
        return null;
      }

      // Check for Range header set the value in the map appropriately so that the
      // catalogFramework can take care of the skipping
      if (bytesToSkip > 0) {
        LOGGER.debug("Bytes to skip: {}", bytesToSkip);
        convertedMap.put(BYTES_TO_SKIP, bytesToSkip);
      }

      LOGGER.debug("Calling transform.");
      final BinaryContent content = catalogFramework.transform(card, transformer, convertedMap);
      LOGGER.debug("Read and transform complete, preparing response.");

      return content;

    } catch (FederationException e) {
      String exceptionMessage = "READ failed due to unexpected exception: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (CatalogTransformerException e) {
      String exceptionMessage = "Unable to transform Metacard.  Try different transformer: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot obtain query results because source is unavailable: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (UnsupportedQueryException e) {
      String errorMessage = "Specified query is unsupported.  Change query and resubmit: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage + e.getMessage());
    } catch (DataUsageLimitExceededException e) {
      String errorMessage = "Unable to process request. Data usage limit exceeded: ";
      LOGGER.debug(errorMessage, e);
      throw new DataUsageLimitExceededException(errorMessage + e.getMessage());
    } catch (RuntimeException | UnsupportedEncodingException e) {
      String exceptionMessage = "Unknown error occurred while processing request.";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage);
    }
  }

  @Override
  public void updateDocument(
      String id,
      List<String> contentTypeList,
      List<FileItem> fileItems,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException {
    LOGGER.trace("PUT");

    if (id == null || message == null) {
      String errorResponseString = "Both ID and content are needed to perform UPDATE.";
      LOGGER.info(errorResponseString);
      throw new CatalogServiceException(errorResponseString);
    }

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    if (CollectionUtils.isNotEmpty(fileItems)) {
      attachmentInfoAndMetacard = parseFormUpload(fileItems, transformerParam);
    } else {
      LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
    }

    updateDocument(attachmentInfoAndMetacard, id, contentTypeList, transformerParam, message);
  }

  private void updateDocument(
      Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard,
      String id,
      List<String> contentTypeList,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException {
    try {
      MimeType mimeType = getMimeType(contentTypeList);

      if (attachmentInfoAndMetacard == null) {
        UpdateRequest updateRequest =
            new UpdateRequestImpl(id, generateMetacard(mimeType, id, message, transformerParam));
        catalogFramework.update(updateRequest);
      } else {
        UpdateStorageRequest streamUpdateRequest =
            new UpdateStorageRequestImpl(
                Collections.singletonList(
                    new IncomingContentItem(
                        id,
                        attachmentInfoAndMetacard.getKey().getStream(),
                        attachmentInfoAndMetacard.getKey().getContentType(),
                        attachmentInfoAndMetacard.getKey().getFilename(),
                        0,
                        attachmentInfoAndMetacard.getValue())),
                null);
        catalogFramework.update(streamUpdateRequest);
      }

      LOGGER.debug("Metacard {} updated.", id);

    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot update catalog entry: Source is unavailable: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error cataloging updated metadata: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (MetacardCreationException | IngestException e) {
      String errorMessage = "Error cataloging updated metadata: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage + e.getMessage());
    }
  }

  @Override
  public String addDocument(
      List<String> contentTypeList,
      List<FileItem> fileItems,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException {
    LOGGER.debug("POST");

    if (message == null) {
      String errorMessage = "No content found, cannot do CREATE.";
      LOGGER.info(errorMessage);
      throw new CatalogServiceException(errorMessage);
    }

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    if (CollectionUtils.isNotEmpty(fileItems)) {
      attachmentInfoAndMetacard = parseFormUpload(fileItems, transformerParam);
    } else {
      LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
    }

    return addDocument(attachmentInfoAndMetacard, contentTypeList, transformerParam, message);
  }

  private String addDocument(
      Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard,
      List<String> contentTypeList,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException {
    try {
      LOGGER.debug("POST");

      MimeType mimeType = getMimeType(contentTypeList);
      CreateResponse createResponse;
      if (attachmentInfoAndMetacard == null) {
        CreateRequest createRequest =
            new CreateRequestImpl(generateMetacard(mimeType, null, message, transformerParam));
        createResponse = catalogFramework.create(createRequest);
      } else {
        String id =
            attachmentInfoAndMetacard.getValue() == null
                ? null
                : attachmentInfoAndMetacard.getValue().getId();
        if (id == null) {
          id = uuidGenerator.generateUuid();
        }
        CreateStorageRequest streamCreateRequest =
            new CreateStorageRequestImpl(
                Collections.singletonList(
                    new IncomingContentItem(
                        id,
                        attachmentInfoAndMetacard.getKey().getStream(),
                        attachmentInfoAndMetacard.getKey().getContentType(),
                        attachmentInfoAndMetacard.getKey().getFilename(),
                        0L,
                        attachmentInfoAndMetacard.getValue())),
                null);
        createResponse = catalogFramework.create(streamCreateRequest);
      }

      String id = createResponse.getCreatedMetacards().get(0).getId();
      LOGGER.debug("Create Response id [{}]", id);

      LOGGER.debug("Entry successfully saved, id: {}", id);
      if (INGEST_LOGGER.isInfoEnabled()) {
        INGEST_LOGGER.info("Entry successfully saved, id: {}", id);
      }

      return id;

    } catch (SourceUnavailableException e) {
      String exceptionMessage = "Cannot create catalog entry because source is unavailable: ";
      LOGGER.info(exceptionMessage, e);
      // Catalog framework logs these exceptions to the ingest logger so we don't have to.
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error while storing entry in catalog: ";
      LOGGER.info(exceptionMessage, e);
      // Catalog framework logs these exceptions to the ingest logger so we don't have to.
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (MetacardCreationException | IngestException e) {
      String errorMessage = "Error while storing entry in catalog: ";
      LOGGER.info(errorMessage, e);
      // Catalog framework logs these exceptions to the ingest logger so we don't have to.
      throw new CatalogServiceException(errorMessage + e.getMessage());
    } finally {
      IOUtils.closeQuietly(message);
    }
  }

  public Pair<AttachmentInfo, Metacard> parseFormUpload(
      List<FileItem> fileItems, String transformerParam) {
    if (fileItems.size() == 1) {
      FileItem fileItem = Iterables.get(fileItems, 0);

      try (InputStream inputStream = fileItem.getInputStream()) {
        return new ImmutablePair<>(
            attachmentParser.generateAttachmentInfo(
                inputStream, fileItem.getContentType(), fileItem.getName()),
            null);
      } catch (IOException e) {
        LOGGER.debug("IOException reading stream from file attachment in multipart body.", e);
      }
    }

    Metacard metacard = null;
    AttachmentInfo attachmentInfo = null;
    Map<String, AttributeImpl> attributeMap = new HashMap<>();

    for (FileItem fileItem : fileItems) {
      String name = fileItem.getFieldName();
      String parsedName = (name.startsWith("parse.")) ? name.substring(6) : name;

      try (InputStream inputStream = fileItem.getInputStream()) {
        switch (name) {
          case "parse.resource":
            attachmentInfo =
                attachmentParser.generateAttachmentInfo(
                    inputStream, fileItem.getContentType(), fileItem.getName());
            break;
          case "parse.metadata":
            metacard =
                parseMetacard(transformerParam, metacard, fileItem.getContentType(), inputStream);
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
    if (metacard == null) {
      metacard = new MetacardImpl();
    }

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
        InputStream boundedStream = new BoundedInputStream(is, MAX_INPUT_SIZE + 1L)) {
      if (attributeFormat == AttributeType.AttributeFormat.OBJECT) {
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

      if (attributeFormat == AttributeType.AttributeFormat.BINARY) {
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

  private Metacard parseMetacard(
      String transformerParam, Metacard metacard, String contentType, InputStream inputStream) {
    String transformer = DEFAULT_METACARD_TRANSFORMER;
    if (transformerParam != null) {
      transformer = transformerParam;
    }
    try {
      MimeType mimeType = new MimeType(contentType);
      metacard = generateMetacard(mimeType, null, inputStream, transformer);
    } catch (MimeTypeParseException | MetacardCreationException e) {
      LOGGER.debug("Unable to parse metadata {}", contentType);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return metacard;
  }

  @Override
  public void deleteDocument(String id) throws CatalogServiceException, ServletException {
    LOGGER.debug("DELETE");
    try {
      if (id != null) {
        DeleteRequestImpl deleteReq =
            new DeleteRequestImpl(new HtmlPolicyBuilder().toFactory().sanitize(id));

        catalogFramework.delete(deleteReq);
        LOGGER.debug("Attempting to delete Metacard with id: {}", id);
      } else {
        String errorMessage = "ID of entry not specified, cannot do DELETE.";
        LOGGER.info(errorMessage);
        throw new CatalogServiceException(errorMessage);
      }
    } catch (SourceUnavailableException ce) {
      String exceptionMessage =
          "Could not delete entry from catalog since the source is unavailable: ";
      LOGGER.info(exceptionMessage, ce);
      throw new ServletException(exceptionMessage + ce.getMessage());
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error deleting entry from catalog: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage + e.getMessage());
    } catch (IngestException e) {
      String errorMessage = "Error deleting entry from catalog: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage + e.getMessage());
    }
  }

  private Metacard generateMetacard(
      MimeType mimeType, String id, InputStream message, String transformerId)
      throws MetacardCreationException {

    Metacard generatedMetacard = null;

    List<InputTransformer> listOfCandidates =
        mimeTypeToTransformerMapper.findMatches(InputTransformer.class, mimeType);
    List<String> stackTraceList = new ArrayList<>();

    LOGGER.trace("Entering generateMetacard.");

    LOGGER.debug("List of matches for mimeType [{}]: {}", mimeType, listOfCandidates);

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {

      try {
        if (null != message) {
          IOUtils.copy(message, fileBackedOutputStream);
        } else {
          throw new MetacardCreationException(
              "Could not copy bytes of content message.  Message was NULL.");
        }
      } catch (IOException e) {
        throw new MetacardCreationException("Could not copy bytes of content message.", e);
      }

      Iterator<InputTransformer> it = listOfCandidates.iterator();
      if (StringUtils.isNotEmpty(transformerId)) {
        BundleContext bundleContext = getBundleContext();
        Collection<ServiceReference<InputTransformer>> serviceReferences =
            bundleContext.getServiceReferences(
                InputTransformer.class, "(id=" + transformerId + ")");
        it = serviceReferences.stream().map(bundleContext::getService).iterator();
      }

      while (it.hasNext()) {
        InputTransformer transformer = it.next();
        try (InputStream inputStreamMessageCopy =
            fileBackedOutputStream.asByteSource().openStream()) {
          generatedMetacard = transformer.transform(inputStreamMessageCopy);
        } catch (CatalogTransformerException | IOException e) {
          List<String> stackTraces = Arrays.asList(ExceptionUtils.getRootCauseStackTrace(e));
          stackTraceList.add(
              String.format("Transformer [%s] could not create metacard.", transformer));
          stackTraceList.addAll(stackTraces);
          LOGGER.debug("Transformer [{}] could not create metacard.", transformer, e);
        }
        if (generatedMetacard != null) {
          break;
        }
      }

      if (generatedMetacard == null) {
        throw new MetacardCreationException(
            String.format(
                "Could not create metacard with mimeType %s : %s",
                mimeType, StringUtils.join(stackTraceList, "\n")));
      }

      if (id != null) {
        generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
      }
      LOGGER.debug("Metacard id is {}", generatedMetacard.getId());

    } catch (IOException e) {
      throw new MetacardCreationException("Could not create metacard.", e);
    } catch (InvalidSyntaxException e) {
      throw new MetacardCreationException("Could not determine transformer", e);
    }
    return generatedMetacard;
  }

  private MimeType getMimeType(List<String> contentTypeList) {
    String singleMimeType = null;

    if (contentTypeList != null && !contentTypeList.isEmpty()) {
      singleMimeType = contentTypeList.get(0);
      LOGGER.debug("Encountered [{}] {}", singleMimeType, "Content-Type");
    }

    MimeType mimeType = null;

    // Sending a null argument to MimeType causes NPE
    if (singleMimeType != null) {
      try {
        mimeType = new MimeType(singleMimeType);
      } catch (MimeTypeParseException e) {
        LOGGER.debug("Could not parse mime type from headers.", e);
      }
    }

    return mimeType;
  }

  @Override
  public String getFileExtensionForMimeType(String mimeType) {
    String fileExtension = this.tikaMimeTypeResolver.getFileExtensionForMimeType(mimeType);
    LOGGER.debug("Mime Type [{}] resolves to file extension [{}].", mimeType, fileExtension);
    return fileExtension;
  }

  public void setMimeTypeToTransformerMapper(
      MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
    this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
  }

  public FilterBuilder getFilterBuilder() {
    return filterBuilder;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  public void setTikaMimeTypeResolver(MimeTypeResolver mimeTypeResolver) {
    this.tikaMimeTypeResolver = mimeTypeResolver;
  }

  public void setUuidGenerator(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  protected static class IncomingContentItem extends ContentItemImpl {

    private InputStream inputStream;

    public IncomingContentItem(
        String id,
        InputStream inputStream,
        String mimeTypeRawData,
        String filename,
        long size,
        Metacard metacard) {
      super(id, null, mimeTypeRawData, filename, size, metacard);
      this.inputStream = inputStream;
    }

    @Override
    public InputStream getInputStream() {
      return inputStream;
    }
  }
}
