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
package org.codice.ddf.rest.service;

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
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.InternalIngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
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
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import net.minidev.json.JSONObject;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.owasp.html.HtmlPolicyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCatalogService implements CatalogService {

  public static final String DEFAULT_METACARD_TRANSFORMER = "xml";

  private static final String BYTES_TO_SKIP = "BytesToSkip";

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCatalogService.class);

  private static final Logger INGEST_LOGGER = LoggerFactory.getLogger(Constants.INGEST_LOGGER_NAME);

  private static final String HEADER_RANGE = "Range";

  private static final String FILE_ATTACHMENT_CONTENT_ID = "file";

  private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

  private static final String BYTES_EQUAL = "bytes=";

  private static final String JSON_MIME_TYPE_STRING = "application/json";

  private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

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

  public AbstractCatalogService(
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
    Bundle bundle = FrameworkUtil.getBundle(AbstractCatalogService.class);
    return bundle == null ? null : bundle.getBundleContext();
  }

  @Override
  public BinaryContent getHeaders(
      String sourceid, String id, URI absolutePath, MultivaluedMap<String, String> queryParameters)
      throws CatalogServiceException {
    QueryResponse queryResponse;
    Metacard card = null;
    LOGGER.trace("getHeaders");

    if (id != null) {
      LOGGER.debug("Got id: {}", id);
      LOGGER.debug("Map of query parameters: \n{}", queryParameters);

      Map<String, Serializable> convertedMap = convert(queryParameters);
      convertedMap.put("url", absolutePath.toString());

      LOGGER.debug("Map converted, retrieving product.");

      // default to xml if no transformer specified
      try {
        String transformer = DEFAULT_METACARD_TRANSFORMER;

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

        // TODO: should be poll? do we want to specify a timeout? (will
        // return null if timeout elapsed)
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
        throw new InternalServerErrorException(exceptionMessage);
      } catch (CatalogTransformerException e) {
        String exceptionMessage = "Unable to transform Metacard.  Try different transformer: ";
        LOGGER.info(exceptionMessage, e);
        throw new InternalServerErrorException(exceptionMessage);
      } catch (SourceUnavailableException e) {
        String exceptionMessage = "Cannot obtain query results because source is unavailable: ";
        LOGGER.info(exceptionMessage, e);
        throw new InternalServerErrorException(exceptionMessage);
      } catch (UnsupportedQueryException e) {
        String errorMessage = "Specified query is unsupported.  Change query and resubmit: ";
        LOGGER.info(errorMessage, e);
        throw new CatalogServiceException(errorMessage);
        // The catalog framework will throw this if any of the transformers blow up. We need to
        // catch this exception
        // here or else execution will return to CXF and we'll lose this message and end up with
        // a huge stack trace
        // in a GUI or whatever else is connected to this endpoint
      } catch (IllegalArgumentException e) {
        throw new CatalogServiceException(e.getMessage());
      }
    } else {
      throw new CatalogServiceException("No ID specified.");
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
  public abstract BinaryContent getSourcesInfo();

  @Override
  public BinaryContent getDocument(
      String encodedSourceId,
      String encodedId,
      String transformerParam,
      URI absolutePath,
      MultivaluedMap<String, String> queryParameters,
      HttpServletRequest httpRequest)
      throws CatalogServiceException, DataUsageLimitExceededException,
          InternalServerErrorException {

    QueryResponse queryResponse;
    Metacard card = null;
    LOGGER.trace("GET");

    if (encodedId != null) {
      LOGGER.debug("Got id: {}", encodedId);
      LOGGER.debug("Got service: {}", transformerParam);
      LOGGER.debug("Map of query parameters: \n{}", queryParameters);

      Map<String, Serializable> convertedMap = convert(queryParameters);
      convertedMap.put("url", absolutePath.toString());

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
        // catalogFramework
        // can take care of the skipping
        long bytesToSkip = getRangeStart(httpRequest);

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
        throw new InternalServerErrorException(exceptionMessage);
      } catch (CatalogTransformerException e) {
        String exceptionMessage = "Unable to transform Metacard.  Try different transformer: ";
        LOGGER.info(exceptionMessage, e);
        throw new InternalServerErrorException(exceptionMessage);
      } catch (SourceUnavailableException e) {
        String exceptionMessage = "Cannot obtain query results because source is unavailable: ";
        LOGGER.info(exceptionMessage, e);
        throw new InternalServerErrorException(exceptionMessage);
      } catch (UnsupportedQueryException e) {
        String errorMessage = "Specified query is unsupported.  Change query and resubmit: ";
        LOGGER.info(errorMessage, e);
        throw new CatalogServiceException(errorMessage);
      } catch (DataUsageLimitExceededException e) {
        String errorMessage = "Unable to process request. Data usage limit exceeded: ";
        LOGGER.debug(errorMessage, e);
        throw new DataUsageLimitExceededException(errorMessage);
        // The catalog framework will throw this if any of the transformers blow up.
        // We need to catch this exception here or else execution will return to CXF and
        // we'll lose this message and end up with a huge stack trace in a GUI or whatever
        // else is connected to this endpoint
      } catch (RuntimeException | UnsupportedEncodingException e) {
        String exceptionMessage = "Unknown error occurred while processing request.";
        LOGGER.info(exceptionMessage, e);
        throw new InternalServerErrorException(exceptionMessage);
      }
    } else {
      throw new CatalogServiceException("No ID specified.");
    }
  }

  @Override
  public BinaryContent createMetacard(MultipartBody multipartBody, String transformerParam)
      throws CatalogServiceException {
    LOGGER.trace("ENTERING: createMetacard");

    String contentUri = multipartBody.getAttachmentObject("contentUri", String.class);
    LOGGER.debug("contentUri = {}", contentUri);

    InputStream stream = null;
    String contentType = null;

    Attachment contentPart = multipartBody.getAttachment(FILE_ATTACHMENT_CONTENT_ID);
    if (contentPart != null) {
      // Example Content-Type header:
      // Content-Type: application/json;id=geojson
      if (contentPart.getContentType() != null) {
        contentType = contentPart.getContentType().toString();
      }

      // Get the file contents as an InputStream and ensure the stream is positioned
      // at the beginning
      try {
        stream = contentPart.getDataHandler().getInputStream();
        if (stream != null && stream.available() == 0) {
          stream.reset();
        }
      } catch (IOException e) {
        LOGGER.info("IOException reading stream from file attachment in multipart body", e);
      }
    } else {
      LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
    }

    return createMetacard(stream, contentType, transformerParam);
  }

  @Override
  public BinaryContent createMetacard(
      HttpServletRequest httpServletRequest, String transformerParam)
      throws CatalogServiceException {
    LOGGER.trace("ENTERING: createMetacard");

    InputStream stream = null;
    String contentType = null;

    try {
      Part contentPart = httpServletRequest.getPart(FILE_ATTACHMENT_CONTENT_ID);
      if (contentPart != null) {
        // Example Content-Type header:
        // Content-Type: application/json;id=geojson
        if (contentPart.getContentType() != null) {
          contentType = contentPart.getContentType();
        }

        // Get the file contents as an InputStream and ensure the stream is positioned
        // at the beginning
        try {
          stream = contentPart.getInputStream();
          if (stream != null && stream.available() == 0) {
            stream.reset();
          }
        } catch (IOException e) {
          LOGGER.info("IOException reading stream from file attachment in multipart body", e);
        }
      } else {
        LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
      }
    } catch (ServletException | IOException e) {
      LOGGER.info("No file contents part found: ", e);
    }

    return createMetacard(stream, contentType, transformerParam);
  }

  private BinaryContent createMetacard(
      InputStream stream, String contentType, String transformerParam)
      throws CatalogServiceException {
    String transformer = DEFAULT_METACARD_TRANSFORMER;
    if (transformerParam != null) {
      transformer = transformerParam;
    }

    MimeType mimeType = null;
    if (contentType != null) {
      try {
        mimeType = new MimeType(contentType);
      } catch (MimeTypeParseException e) {
        LOGGER.debug("Unable to create MimeType from raw data {}", contentType);
      }
    } else {
      LOGGER.debug("No content type specified in request");
    }

    try {
      Metacard metacard = generateMetacard(mimeType, null, stream, null);
      String metacardId = metacard.getId();
      LOGGER.debug("Metacard {} created", metacardId);
      LOGGER.debug(
          "Transforming metacard {} to {} to be able to return it to client",
          metacardId,
          transformer);
      final BinaryContent content = catalogFramework.transform(metacard, transformer, null);
      LOGGER.debug(
          "Metacard to {} transform complete for {}, preparing response.", transformer, metacardId);

      LOGGER.trace("EXITING: createMetacard");
      return content;

    } catch (MetacardCreationException | CatalogTransformerException e) {
      throw new CatalogServiceException("Unable to create metacard");
    } finally {
      try {
        if (stream != null) {
          stream.close();
        }
      } catch (IOException e) {
        LOGGER.debug("Unexpected error closing stream", e);
      }
    }
  }

  @Override
  public void updateDocument(
      String id,
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
    LOGGER.trace("PUT");

    if (id == null || message == null) {
      String errorResponseString = "Both ID and content are needed to perform UPDATE.";
      LOGGER.info(errorResponseString);
      throw new CatalogServiceException(errorResponseString);
    }

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    if (multipartBody != null) {
      List<Attachment> contentParts = multipartBody.getAllAttachments();
      if (CollectionUtils.isNotEmpty(contentParts)) {
        attachmentInfoAndMetacard = parseAttachments(contentParts, transformerParam);
      } else {
        LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
      }
    }

    updateDocument(attachmentInfoAndMetacard, id, contentTypeList, transformerParam, message);
  }

  @Override
  public void updateDocument(
      String id,
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
    LOGGER.trace("PUT");

    if (id == null || message == null) {
      String errorResponseString = "Both ID and content are needed to perform UPDATE.";
      LOGGER.info(errorResponseString);
      throw new CatalogServiceException(errorResponseString);
    }

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    try {
      if (httpServletRequest != null) {
        Collection<Part> contentParts = httpServletRequest.getParts();
        if (CollectionUtils.isNotEmpty(contentParts)) {
          attachmentInfoAndMetacard = parseParts(contentParts, transformerParam);
        } else {
          LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
        }
      }
    } catch (ServletException | IOException e) {
      LOGGER.info("Unable to get contents part: ", e);
    }

    updateDocument(attachmentInfoAndMetacard, id, contentTypeList, transformerParam, message);
  }

  private void updateDocument(
      Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard,
      String id,
      List<String> contentTypeList,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
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
      throw new InternalServerErrorException(exceptionMessage);
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error cataloging updated metadata: ";
      LOGGER.info(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    } catch (MetacardCreationException | IngestException e) {
      String errorMessage = "Error cataloging updated metadata: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage);
    }
  }

  @Override
  public String addDocument(
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
    LOGGER.debug("POST");

    if (message == null) {
      String errorMessage = "No content found, cannot do CREATE.";
      LOGGER.info(errorMessage);
      throw new CatalogServiceException(errorMessage);
    }

    Pair<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    if (multipartBody != null) {
      List<Attachment> contentParts = multipartBody.getAllAttachments();
      if (CollectionUtils.isNotEmpty(contentParts)) {
        attachmentInfoAndMetacard = parseAttachments(contentParts, transformerParam);
      } else {
        LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
      }
    }

    return addDocument(attachmentInfoAndMetacard, contentTypeList, transformerParam, message);
  }

  @Override
  public String addDocument(
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
    LOGGER.debug("POST");

    if (message == null) {
      String errorMessage = "No content found, cannot do CREATE.";
      LOGGER.info(errorMessage);
      throw new CatalogServiceException(errorMessage);
    }

    Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard = null;
    try {
      if (httpServletRequest != null) {
        Collection<Part> contentParts = httpServletRequest.getParts();
        if (CollectionUtils.isNotEmpty(contentParts)) {
          attachmentInfoAndMetacard = parseParts(contentParts, transformerParam);
        } else {
          LOGGER.debug(NO_FILE_CONTENTS_ATT_FOUND);
        }
      }
    } catch (ServletException | IOException e) {
      LOGGER.info("Unable to get contents part: ", e);
    }

    return addDocument(attachmentInfoAndMetacard, contentTypeList, transformerParam, message);
  }

  private String addDocument(
      Map.Entry<AttachmentInfo, Metacard> attachmentInfoAndMetacard,
      List<String> contentTypeList,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException {
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
      throw new InternalServerErrorException(exceptionMessage);
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error while storing entry in catalog: ";
      LOGGER.info(exceptionMessage, e);
      // Catalog framework logs these exceptions to the ingest logger so we don't have to.
      throw new InternalServerErrorException(exceptionMessage);
    } catch (MetacardCreationException | IngestException e) {
      String errorMessage = "Error while storing entry in catalog: ";
      LOGGER.info(errorMessage, e);
      // Catalog framework logs these exceptions to the ingest logger so we don't have to.
      throw new CatalogServiceException(errorMessage);
    } finally {
      IOUtils.closeQuietly(message);
    }
  }

  public Pair<AttachmentInfo, Metacard> parseAttachments(
      List<Attachment> contentParts, String transformerParam) {

    if (contentParts.size() == 1) {
      Attachment contentPart = contentParts.get(0);

      InputStream attachmentInputStream = null;

      try {
        attachmentInputStream = contentPart.getDataHandler().getInputStream();
      } catch (IOException e) {
        LOGGER.debug("IOException reading stream from file attachment in multipart body.", e);
      }

      return new ImmutablePair<>(
          attachmentParser.generateAttachmentInfo(
              attachmentInputStream,
              contentPart.getContentType().toString(),
              contentPart
                  .getContentDisposition()
                  .getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME)),
          null);
    }

    Map<String, AttributeImpl> attributeMap = new HashMap<>();
    Metacard metacard = null;
    AttachmentInfo attachmentInfo = null;

    for (Attachment attachment : contentParts) {
      String name = attachment.getContentDisposition().getParameter("name");
      String parsedName = (name.startsWith("parse.")) ? name.substring(6) : name;
      try {
        InputStream inputStream = attachment.getDataHandler().getInputStream();
        switch (name) {
          case "parse.resource":
            attachmentInfo =
                attachmentParser.generateAttachmentInfo(
                    inputStream,
                    attachment.getContentType().toString(),
                    attachment
                        .getContentDisposition()
                        .getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME));
            break;
          case "parse.metadata":
            metacard = parseMetadata(transformerParam, metacard, attachment, inputStream);
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

  @Override
  public Map.Entry<AttachmentInfo, Metacard> parseParts(
      Collection<Part> contentParts, String transformerParam) {
    if (contentParts.size() == 1) {
      Part part = Iterables.get(contentParts, 0);

      try (InputStream inputStream = part.getInputStream()) {
        ContentDisposition contentDisposition =
            new ContentDisposition(part.getHeader(HEADER_CONTENT_DISPOSITION));
        return new ImmutablePair<>(
            attachmentParser.generateAttachmentInfo(
                inputStream,
                part.getContentType(),
                contentDisposition.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME)),
            null);

      } catch (IOException e) {
        LOGGER.debug("IOException reading stream from file attachment in multipart body.", e);
      }
    }

    Metacard metacard = null;
    AttachmentInfo attachmentInfo = null;
    Map<String, AttributeImpl> attributeMap = new HashMap<>();

    for (Part part : contentParts) {
      String name = part.getName();
      String parsedName = (name.startsWith("parse.")) ? name.substring(6) : name;

      try (InputStream inputStream = part.getInputStream()) {
        ContentDisposition contentDisposition =
            new ContentDisposition(part.getHeader(HEADER_CONTENT_DISPOSITION));
        switch (name) {
          case "parse.resource":
            attachmentInfo =
                attachmentParser.generateAttachmentInfo(
                    inputStream,
                    part.getContentType(),
                    contentDisposition.getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME));
            break;
          case "parse.metadata":
            metacard = parseMetacard(transformerParam, metacard, part, inputStream);
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
      AttributeFormat attributeFormat) {
    try (InputStream is = inputStream;
        InputStream boundedStream = new BoundedInputStream(is, MAX_INPUT_SIZE + 1L)) {
      if (attributeFormat == AttributeFormat.OBJECT) {
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

      if (attributeFormat == AttributeFormat.BINARY) {
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

  private Metacard parseMetadata(
      String transformerParam, Metacard metacard, Attachment attachment, InputStream inputStream) {
    String transformer = DEFAULT_METACARD_TRANSFORMER;
    if (transformerParam != null) {
      transformer = transformerParam;
    }
    try {
      MimeType mimeType = new MimeType(attachment.getContentType().toString());
      metacard = generateMetacard(mimeType, null, inputStream, transformer);
    } catch (MimeTypeParseException | MetacardCreationException e) {
      LOGGER.debug("Unable to parse metadata {}", attachment.getContentType());
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return metacard;
  }

  private Metacard parseMetacard(
      String transformerParam, Metacard metacard, Part part, InputStream inputStream) {
    String transformer = "xml";
    if (transformerParam != null) {
      transformer = transformerParam;
    }
    try {
      MimeType mimeType = new MimeType(part.getContentType());
      metacard = generateMetacard(mimeType, null, inputStream, transformer);
    } catch (MimeTypeParseException | MetacardCreationException e) {
      LOGGER.debug("Unable to parse metadata {}", part.getContentType());
    }
    return metacard;
  }

  @Override
  public void deleteDocument(String id) throws CatalogServiceException {
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
      throw new InternalServerErrorException(exceptionMessage);
    } catch (InternalIngestException e) {
      String exceptionMessage = "Error deleting entry from catalog: ";
      LOGGER.info(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    } catch (IngestException e) {
      String errorMessage = "Error deleting entry from catalog: ";
      LOGGER.info(errorMessage, e);
      throw new CatalogServiceException(errorMessage);
    }
  }

  private Map<String, Serializable> convert(MultivaluedMap<String, String> map) {
    Map<String, Serializable> convertedMap = new HashMap<>();

    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
      String key = entry.getKey();
      List<String> value = entry.getValue();

      if (value.size() == 1) {
        convertedMap.put(key, value.get(0));
      } else {
        // List is not serializable so we make it a String array
        convertedMap.put(key, value.toArray());
      }
    }

    return convertedMap;
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
      LOGGER.debug("Encountered [{}] {}", singleMimeType, HttpHeaders.CONTENT_TYPE);
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

  private boolean rangeHeaderExists(HttpServletRequest httpRequest) {
    boolean response = false;

    if (httpRequest != null && httpRequest.getHeader(HEADER_RANGE) != null) {
      response = true;
    }

    return response;
  }

  // Return 0 (beginning of stream) if the range header does not exist.
  private long getRangeStart(HttpServletRequest httpRequest) throws UnsupportedQueryException {
    long response = 0;

    if (httpRequest != null && rangeHeaderExists(httpRequest)) {
      String rangeHeader = httpRequest.getHeader(HEADER_RANGE);
      String range = getRange(rangeHeader);

      if (range != null) {
        response = Long.parseLong(range);
      }
    }

    return response;
  }

  private String getRange(String rangeHeader) throws UnsupportedQueryException {
    String response = null;

    if (rangeHeader != null) {
      if (rangeHeader.startsWith(BYTES_EQUAL)) {
        String tempString = rangeHeader.substring(BYTES_EQUAL.length());
        if (tempString.contains("-")) {
          response = rangeHeader.substring(BYTES_EQUAL.length(), rangeHeader.lastIndexOf('-'));
        } else {
          response = rangeHeader.substring(BYTES_EQUAL.length());
        }
      } else {
        throw new UnsupportedQueryException("Invalid range header: " + rangeHeader);
      }
    }

    return response;
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
