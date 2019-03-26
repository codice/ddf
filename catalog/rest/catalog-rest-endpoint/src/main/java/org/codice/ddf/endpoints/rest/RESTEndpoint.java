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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.types.Core;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.Resource;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.codec.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.rest.service.CatalogService;
import org.codice.ddf.rest.service.CatalogServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTEndpoint implements RESTService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RESTEndpoint.class);

  private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

  private static final String HEADER_CONTENT_LENGTH = "Content-Length";

  private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

  private static final String BYTES = "bytes";

  private CatalogService catalogService;

  public RESTEndpoint(CatalogService catalogService) {
    LOGGER.trace("Constructing REST Endpoint");
    this.catalogService = catalogService;
    LOGGER.trace(("Rest Endpoint constructed successfully"));
  }

  @HEAD
  @Path("")
  public Response ping() {
    LOGGER.trace("Ping!");
    return Response.ok().build();
  }

  /**
   * REST Head. Retrieves information regarding the entry specified by the id. This can be used to
   * verify that the Range header is supported (the Accept-Ranges header is returned) and to get the
   * size of the requested resource for use in Content-Range requests.
   *
   * @param id
   * @param uriInfo
   * @param httpRequest
   * @return
   */
  @HEAD
  @Path("/{id}")
  public Response getHeaders(
      @PathParam("id") String id,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest) {

    return getHeaders(null, id, uriInfo, httpRequest);
  }

  /**
   * REST Head. Returns headers only. Primarily used to let the client know that range requests
   * (though limited) are accepted.
   *
   * @param sourceid
   * @param id
   * @param uriInfo
   * @param httpRequest
   * @return
   */
  @HEAD
  @Path("/sources/{sourceid}/{id}")
  public Response getHeaders(
      @PathParam("sourceid") String sourceid,
      @PathParam("id") String id,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest) {
    try {
      Response.ResponseBuilder responseBuilder;

      final BinaryContent content =
          catalogService.getHeaders(
              sourceid, id, uriInfo.getAbsolutePath(), uriInfo.getQueryParameters());

      if (content == null) {
        return Response.status(Status.NOT_FOUND)
            .entity("<pre>Unable to retrieve requested metacard.</pre>")
            .type(MediaType.TEXT_HTML)
            .build();
      }

      responseBuilder = Response.noContent();

      // Add the Accept-ranges header to let the client know that we accept ranges in bytes
      responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);

      setFileNameOnResponseBuilder(id, content, responseBuilder);

      long size = content.getSize();
      if (size > 0) {
        responseBuilder.header(HEADER_CONTENT_LENGTH, size);
      }
      return responseBuilder.build();
    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());
    }
  }

  /**
   * REST Get. Retrieves the metadata entry specified by the id. Transformer argument is optional,
   * but is used to specify what format the data should be returned.
   *
   * @param id
   * @param transformerParam (OPTIONAL)
   * @param uriInfo
   * @return
   */
  @GET
  @Path("/{id}")
  public Response getDocument(
      @PathParam("id") String id,
      @QueryParam("transform") String transformerParam,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest) {

    return getDocument(null, id, transformerParam, uriInfo, httpRequest);
  }

  /**
   * REST Get. Retrieves information regarding sources available.
   *
   * @param uriInfo
   * @param httpRequest
   * @return
   */
  @GET
  @Path(SOURCES_PATH)
  public Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest) {
    ResponseBuilder responseBuilder;

    final BinaryContent content = catalogService.getSourcesInfo();
    responseBuilder = Response.ok(content.getInputStream(), content.getMimeTypeValue());

    // Add the Accept-ranges header to let the client know that we accept ranges in bytes
    responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);
    return responseBuilder.build();
  }

  /**
   * REST Get. Retrieves the metadata entry specified by the id from the federated source specified
   * by sourceid. Transformer argument is optional, but is used to specify what format the data
   * should be returned.
   *
   * @param encodedSourceId
   * @param encodedId
   * @param transformerParam
   * @param uriInfo
   * @return
   */
  @GET
  @Path("/sources/{sourceid}/{id}")
  public Response getDocument(
      @Encoded @PathParam("sourceid") String encodedSourceId,
      @Encoded @PathParam("id") String encodedId,
      @QueryParam("transform") String transformerParam,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest) {
    try {
      Response.ResponseBuilder responseBuilder;
      String id = URLDecoder.decode(encodedId, CharEncoding.UTF_8);

      final BinaryContent content =
          catalogService.getDocument(
              encodedSourceId,
              encodedId,
              transformerParam,
              uriInfo.getAbsolutePath(),
              uriInfo.getQueryParameters(),
              httpRequest);

      if (content == null) {
        return Response.status(Status.NOT_FOUND)
            .entity("<pre>Unable to retrieve requested metacard.</pre>")
            .type(MediaType.TEXT_HTML)
            .build();
      }

      LOGGER.debug("Read and transform complete, preparing response.");
      responseBuilder = Response.ok(content.getInputStream(), content.getMimeTypeValue());

      // Add the Accept-ranges header to let the client know that we accept ranges in bytes
      responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);

      setFileNameOnResponseBuilder(id, content, responseBuilder);

      long size = content.getSize();
      if (size > 0) {
        responseBuilder.header(HEADER_CONTENT_LENGTH, size);
      }

      return responseBuilder.build();

    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());

    } catch (DataUsageLimitExceededException e) {
      return Response.status(Status.REQUEST_ENTITY_TOO_LARGE)
          .entity("<pre>" + e.getMessage() + "</pre>")
          .type(MediaType.TEXT_HTML)
          .build();

    } catch (UnsupportedEncodingException e) {
      String exceptionMessage = "Unknown error occurred while processing request: ";
      LOGGER.info(exceptionMessage, e);
      throw new InternalServerErrorException(exceptionMessage);
    }
  }

  @POST
  @Path("/metacard")
  public Response createMetacard(
      MultipartBody multipartBody,
      @Context UriInfo requestUriInfo,
      @QueryParam("transform") String transformerParam) {
    try {
      final BinaryContent content = catalogService.createMetacard(multipartBody, transformerParam);

      Response.ResponseBuilder responseBuilder =
          Response.ok(content.getInputStream(), content.getMimeTypeValue());
      return responseBuilder.build();
    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());
    }
  }

  /**
   * REST Put. Updates the specified entry with the provided document.
   *
   * @param id
   * @param message
   * @return
   */
  @PUT
  @Path("/{id}")
  @Consumes({"text/*", "application/*"})
  public Response updateDocument(
      @PathParam("id") String id,
      @Context HttpHeaders headers,
      @Context HttpServletRequest httpRequest,
      @QueryParam("transform") String transformerParam,
      InputStream message) {
    return updateDocument(id, headers, httpRequest, null, transformerParam, message);
  }

  /**
   * REST Put. Updates the specified entry with the provided document.
   *
   * @param id
   * @param message
   * @return
   */
  @PUT
  @Path("/{id}")
  @Consumes("multipart/*")
  public Response updateDocument(
      @PathParam("id") String id,
      @Context HttpHeaders headers,
      @Context HttpServletRequest httpRequest,
      MultipartBody multipartBody,
      @QueryParam("transform") String transformerParam,
      InputStream message) {
    try {
      List<String> contentTypeList = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);
      catalogService.updateDocument(id, contentTypeList, multipartBody, transformerParam, message);

      return Response.ok().build();

    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());
    }
  }

  @POST
  @Consumes({"text/*", "application/*"})
  public Response addDocument(
      @Context HttpHeaders headers,
      @Context UriInfo requestUriInfo,
      @Context HttpServletRequest httpRequest,
      @QueryParam("transform") String transformerParam,
      InputStream message) {
    return addDocument(headers, requestUriInfo, httpRequest, null, transformerParam, message);
  }

  /**
   * REST Post. Creates a new metadata entry in the catalog.
   *
   * @param message
   * @return
   */
  @POST
  @Consumes("multipart/*")
  public Response addDocument(
      @Context HttpHeaders headers,
      @Context UriInfo requestUriInfo,
      @Context HttpServletRequest httpRequest,
      MultipartBody multipartBody,
      @QueryParam("transform") String transformerParam,
      InputStream message) {
    try {
      List<String> contentTypeList = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);
      String id =
          catalogService.addDocument(contentTypeList, multipartBody, transformerParam, message);

      UriBuilder uriBuilder = requestUriInfo.getAbsolutePathBuilder().path("/" + id);
      ResponseBuilder responseBuilder = Response.created(uriBuilder.build());
      responseBuilder.header(Core.ID, id);

      return responseBuilder.build();

    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());
    }
  }

  /**
   * REST Delete. Deletes a record from the catalog.
   *
   * @param id
   * @return
   */
  @DELETE
  @Path("/{id}")
  public Response deleteDocument(
      @PathParam("id") String id, @Context HttpServletRequest httpRequest) {
    try {
      catalogService.deleteDocument(id);
      return Response.ok(id).build();

    } catch (CatalogServiceException e) {
      return createBadRequestResponse(e.getMessage());
    }
  }

  private void setFileNameOnResponseBuilder(
      String id, BinaryContent content, ResponseBuilder responseBuilder) {
    String filename = null;

    if (content instanceof Resource) {
      // If we got a resource, we can extract the filename.
      filename = ((Resource) content).getName();
    } else {
      String fileExtension = catalogService.getFileExtensionForMimeType(content.getMimeTypeValue());
      if (StringUtils.isNotBlank(fileExtension)) {
        filename = id + fileExtension;
      }
    }

    if (StringUtils.isNotBlank(filename)) {
      LOGGER.debug("filename: {}", filename);
      responseBuilder.header(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
    }
  }

  private Response createBadRequestResponse(String entityMessage) {
    return Response.status(Status.BAD_REQUEST)
        .entity("<pre>" + entityMessage + "</pre>")
        .type(MediaType.TEXT_HTML)
        .build();
  }
}
