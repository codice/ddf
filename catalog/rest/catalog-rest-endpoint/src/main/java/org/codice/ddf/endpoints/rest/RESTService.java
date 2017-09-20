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

import java.io.InputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

/** REST endpoint interface */
@Path("/")
public interface RESTService {

  String CONTEXT_ROOT = "catalog";
  String SOURCES_PATH = "/sources";

  /**
   * REST Get. Retrieves the metadata entry specified by the id. Transformer argument is optional,
   * but is used to specify what format the data should be returned.
   *
   * @param id
   * @param transformerParam (OPTIONAL)
   * @param uriInfo
   * @return
   * @throws InternalServerErrorException
   */
  @GET
  @Path("/{id}")
  Response getDocument(
      @PathParam("id") String id,
      @QueryParam("transform") String transformerParam,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest);

  /**
   * REST Get. Retrieves information regarding sources available.
   *
   * @param uriInfo
   * @param httpRequest
   * @return
   */
  @GET
  @Path(SOURCES_PATH)
  Response getDocument(@Context UriInfo uriInfo, @Context HttpServletRequest httpRequest);

  /**
   * REST Get. Retrieves the metadata entry specified by the id from the federated source specified
   * by sourceid. Transformer argument is optional, but is used to specify what format the data
   * should be returned.
   *
   * @param sourceid
   * @param id
   * @param transformerParam
   * @param uriInfo
   * @return
   */
  @GET
  @Path(SOURCES_PATH + "/{sourceid}/{id}")
  Response getDocument(
      @PathParam("sourceid") String sourceid,
      @PathParam("id") String id,
      @QueryParam("transform") String transformerParam,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest httpRequest);

  /**
   * REST Put. Updates the specified metadata entry with the provided metadata.
   *
   * @param id
   * @param message
   * @return
   */
  @PUT
  @Path("/{id}")
  Response updateDocument(
      @PathParam("id") String id,
      @Context HttpHeaders headers,
      @Context HttpServletRequest httpRequest,
      @QueryParam("transform") String transformerParam,
      InputStream message);

  /**
   * REST Put. Updates the specified metadata entry with the provided metadata.
   *
   * @param id
   * @param message
   * @return
   */
  @PUT
  @Path("/{id}")
  Response updateDocument(
      @PathParam("id") String id,
      @Context HttpHeaders headers,
      @Context HttpServletRequest httpRequest,
      MultipartBody multipartBody,
      @QueryParam("transform") String transformerParam,
      InputStream message);

  /**
   * REST Post. Creates a new metadata entry in the catalog.
   *
   * @param message
   * @return
   */
  @POST
  Response addDocument(
      @Context HttpHeaders headers,
      @Context UriInfo requestUriInfo,
      @Context HttpServletRequest httpRequest,
      @QueryParam("transform") String transformerParam,
      InputStream message);

  /**
   * REST Post. Creates a new metadata entry in the catalog.
   *
   * @param message
   * @return
   */
  @POST
  Response addDocument(
      @Context HttpHeaders headers,
      @Context UriInfo requestUriInfo,
      @Context HttpServletRequest httpRequest,
      MultipartBody multipartBody,
      @QueryParam("transform") String transformerParam,
      InputStream message);

  /**
   * REST Delete. Deletes a record from the catalog.
   *
   * @param id
   * @return
   */
  @DELETE
  @Path("/{id}")
  Response deleteDocument(@PathParam("id") String id, @Context HttpServletRequest httpRequest);
}
