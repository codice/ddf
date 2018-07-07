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
package org.codice.ddf.catalog.ui.catalog;

import static org.codice.ddf.rest.service.CatalogService.BYTES;
import static org.codice.ddf.rest.service.CatalogService.HEADER_ACCEPT_RANGES;
import static org.codice.ddf.rest.service.CatalogService.HEADER_CONTENT_DISPOSITION;
import static org.codice.ddf.rest.service.CatalogService.HEADER_CONTENT_LENGTH;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.head;
import static spark.Spark.post;
import static spark.Spark.put;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.http.HttpStatus;
import org.codice.ddf.rest.service.CatalogException;
import org.codice.ddf.rest.service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

public class CatalogApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogApplication.class);

  private static final String ECLIPSE_MULTIPART_CONFIG = "org.eclipse.jetty.multipartConfig";

  private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

  private CatalogService catalogService;

  public CatalogApplication(CatalogService catalogService) {
    this.catalogService = catalogService;
  }

  @Override
  public void init() {
    head(
        "/catalog/",
        (req, res) -> {
          res.status(HttpStatus.SC_OK);
          return res;
        });

    head("/catalog/:id", (req, res) -> getHeaders(req, res, null, req.params(":id")));

    head(
        "/catalog/sources/:sourceid/:id",
        (req, res) -> getHeaders(req, res, req.params(":sourceid"), req.params(":id")));

    get(
        "/catalog/sources",
        (req, res) -> {
          final BinaryContent content = catalogService.getDocument();

          res.status(HttpStatus.SC_OK);
          res.body(IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8));
          res.type(content.getMimeTypeValue());

          // Add the Accept-ranges header to let the client know that we accept ranges in bytes
          res.header(HEADER_ACCEPT_RANGES, BYTES);
          return res;
        });

    get(
        "/catalog/:id",
        (req, res) -> getDocument(req, res, null, req.params(":id"), req.queryParams("transform")));

    get(
        "/catalog/sources/:sourceid/:id",
        (req, res) ->
            getDocument(
                req,
                res,
                req.params(":sourceid"),
                req.params(":id"),
                req.queryParams("transform")));

    post(
        "/catalog/metacard",
        (req, res) -> {
          try {
            final BinaryContent content =
                catalogService.createMetacard(req.raw(), req.queryParams("transform"));

            res.status(HttpStatus.SC_OK);
            res.body(IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8));
            res.type(content.getMimeTypeValue());
            return res;
          } catch (CatalogException e) {
            return createBadRequestResponse(res, e.getMessage());
          }
        });

    post(
        "/catalog/",
        (req, res) -> {
          if (req.contentType().startsWith("multipart/")) {
            req.attribute(
                ECLIPSE_MULTIPART_CONFIG,
                new MultipartConfigElement(System.getProperty(JAVA_IO_TMPDIR)));

            return addDocument(
                res,
                req.raw().getRequestURL(),
                req.contentType(),
                req.queryParams("transform"),
                req.raw(),
                new ByteArrayInputStream(req.bodyAsBytes()));
          }

          if (req.contentType().startsWith("text/")
              || req.contentType().startsWith("application/")) {
            return addDocument(
                res,
                req.raw().getRequestURL(),
                req.contentType(),
                req.queryParams("transform"),
                null,
                new ByteArrayInputStream(req.bodyAsBytes()));
          }

          res.status(HttpStatus.SC_NOT_FOUND);
          return res;
        });

    put(
        "/catalog/:id",
        (req, res) -> {
          if (req.contentType().startsWith("multipart/")) {
            req.attribute(
                ECLIPSE_MULTIPART_CONFIG,
                new MultipartConfigElement(System.getProperty(JAVA_IO_TMPDIR)));

            return updateDocument(
                res,
                req.params(":id"),
                req.contentType(),
                req.queryParams("transform"),
                req.raw(),
                new ByteArrayInputStream(req.bodyAsBytes()));
          }

          if (req.contentType().startsWith("text/")
              || req.contentType().startsWith("application/")) {
            return updateDocument(
                res,
                req.params(":id"),
                req.contentType(),
                req.queryParams("transform"),
                null,
                new ByteArrayInputStream(req.bodyAsBytes()));
          }

          res.status(HttpStatus.SC_NOT_FOUND);
          return res;
        });

    delete(
        "/catalog/:id",
        (req, res) -> {
          try {
            String id = req.params(":id");
            catalogService.deleteDocument(id);
            return javax.ws.rs.core.Response.ok(id).build();

          } catch (CatalogException e) {
            return createBadRequestResponse(res, e.getMessage());
          }
        });
  }

  private Response getHeaders(Request req, Response res, String sourceid, String id) {
    try {
      String filename = null;

      MultivaluedMap<String, String> queryParamsMap = getQueryParamsMap(req);
      URI absolutePath = new URI(req.raw().getRequestURL().toString());

      final BinaryContent content =
          catalogService.getHeaders(sourceid, id, absolutePath, queryParamsMap);

      if (content == null) {
        res.status(HttpStatus.SC_NOT_FOUND);
        res.body("<pre>Unable to retrieve requested metacard.</pre>");
        res.type(MediaType.TEXT_HTML);
        return res;
      }

      res.status(HttpStatus.SC_NO_CONTENT);

      // Add the Accept-ranges header to let the client know that we accept ranges in bytes
      res.header(HEADER_ACCEPT_RANGES, BYTES);

      if (content instanceof Resource) {
        // If we got a resource, we can extract the filename.
        filename = ((Resource) content).getName();
      } else {
        String fileExtension =
            catalogService.getFileExtensionForMimeType(content.getMimeTypeValue());
        if (StringUtils.isNotBlank(fileExtension)) {
          filename = id + fileExtension;
        }
      }

      if (StringUtils.isNotBlank(filename)) {
        LOGGER.debug("filename: {}", filename);
        res.header(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
      }

      long size = content.getSize();
      if (size > 0) {
        res.header(HEADER_CONTENT_LENGTH, String.valueOf(size));
      }
      return res;
    } catch (CatalogException | URISyntaxException e) {
      return createBadRequestResponse(res, e.getMessage());
    }
  }

  private Response getDocument(
      Request req,
      Response res,
      String encodedSourceId,
      String encodedId,
      String transformerParam) {
    try {
      MultivaluedMap<String, String> queryParamsMap = getQueryParamsMap(req);
      URI absolutePath = new URI(req.raw().getRequestURL().toString());
      String id = URLDecoder.decode(encodedId, CharEncoding.UTF_8);

      final BinaryContent content =
          catalogService.getDocument(
              encodedSourceId,
              encodedId,
              transformerParam,
              absolutePath,
              queryParamsMap,
              req.raw());

      if (content == null) {
        res.status(HttpStatus.SC_NOT_FOUND);
        res.body("<pre>Unable to retrieve requested metacard.</pre>");
        res.type(MediaType.TEXT_HTML);
        return res;
      }

      LOGGER.debug("Read and transform complete, preparing response.");
      res.status(HttpStatus.SC_OK);
      res.body(IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8));
      res.type(content.getMimeTypeValue());

      // Add the Accept-ranges header to let the client know that we accept ranges in bytes
      res.header(HEADER_ACCEPT_RANGES, BYTES);

      String filename = null;

      if (content instanceof Resource) {
        // If we got a resource, we can extract the filename.
        filename = ((Resource) content).getName();
      } else {
        String fileExtension =
            catalogService.getFileExtensionForMimeType(content.getMimeTypeValue());
        if (StringUtils.isNotBlank(fileExtension)) {
          filename = id + fileExtension;
        }
      }

      if (StringUtils.isNotBlank(filename)) {
        LOGGER.debug("filename: {}", filename);
        res.header(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
      }

      long size = content.getSize();
      if (size > 0) {
        res.header(HEADER_CONTENT_LENGTH, String.valueOf(size));
      }

      return res;
    } catch (CatalogException | URISyntaxException e) {
      return createBadRequestResponse(res, e.getMessage());

    } catch (DataUsageLimitExceededException e) {
      res.status(HttpStatus.SC_REQUEST_TOO_LONG);
      res.body("<pre>" + e.getMessage() + "</pre>");
      res.type(MediaType.TEXT_HTML);
      return res;
    } catch (IOException e) {
      String exceptionMessage = "Unknown error occurred while processing request: ";
      LOGGER.info(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    }
  }

  private Response updateDocument(
      Response res,
      String id,
      String contentType,
      String transformerParam,
      HttpServletRequest httpServletRequest,
      InputStream inputStream) {
    try {
      List<String> contentTypeList = ImmutableList.of(contentType);
      catalogService.updateDocument(
          id, contentTypeList, httpServletRequest, transformerParam, inputStream);

      res.status(HttpStatus.SC_OK);
      return res;

    } catch (CatalogException e) {
      return createBadRequestResponse(res, e.getMessage());
    }
  }

  private Response addDocument(
      Response res,
      StringBuffer requestUrl,
      String contentType,
      String transformerParam,
      HttpServletRequest httpServletRequest,
      InputStream inputStream) {
    try {
      List<String> contentTypeList = ImmutableList.of(contentType);
      String id =
          catalogService.addDocument(
              contentTypeList, httpServletRequest, transformerParam, inputStream);

      URI uri = new URI(requestUrl.toString());
      UriBuilder uriBuilder = new UriBuilderImpl(uri).path("/" + id);

      res.redirect(uriBuilder.build().toString(), HttpStatus.SC_CREATED);
      res.header(Metacard.ID, id);
      return res;

    } catch (CatalogException | URISyntaxException e) {
      return createBadRequestResponse(res, e.getMessage());
    }
  }

  private MultivaluedMap<String, String> getQueryParamsMap(Request request) {
    MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
    request.queryParams().forEach(param -> map.addAll(param, request.queryMap(param).values()));
    return map;
  }

  private Response createBadRequestResponse(Response response, String entityMessage) {
    response.status(HttpStatus.SC_BAD_REQUEST);
    response.type(MediaType.TEXT_HTML);
    response.body("<pre>" + entityMessage + "</pre>");
    return response;
  }
}
