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

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.DataUsageLimitExceededException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.attachment.AttachmentInfo;

/** Catalog service interface */
public interface CatalogService {

  String CONTEXT_ROOT = "catalog";
  String SOURCES_PATH = "/sources";

  /**
   * Retrieves header information regarding the entry specified by the id such as the Accept-Ranges
   * and Content-Range headers.
   */
  BinaryContent getHeaders(
      String sourceid, String id, URI absolutePath, MultivaluedMap<String, String> queryParameters)
      throws CatalogServiceException;

  /** Retrieves information regarding available sources. */
  BinaryContent getSourcesInfo();

  /**
   * Retrieves the metadata entry specified by the id from the federated source specified by
   * sourceid. Transformer argument is optional, but is used to specify what format the data should
   * be returned.
   */
  BinaryContent getDocument(
      String encodedSourceId,
      String encodedId,
      String transformerParam,
      URI absolutePath,
      MultivaluedMap<String, String> queryParameters,
      HttpServletRequest httpRequest)
      throws CatalogServiceException, DataUsageLimitExceededException;

  /** Creates a new metacard. */
  BinaryContent createMetacard(MultipartBody multipartBody, String transformerParam)
      throws CatalogServiceException;

  /** Creates a new metacard. */
  BinaryContent createMetacard(HttpServletRequest httpServletRequest, String transformerParam)
      throws CatalogServiceException;

  /** Updates the specified metadata entry with the provided metadata. */
  void updateDocument(
      String id,
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException;

  /** Updates the specified metadata entry with the provided metadata. */
  void updateDocument(
      String id,
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException;

  /** Creates a new metadata entry in the catalog. */
  String addDocument(
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException;

  /** Creates a new metadata entry in the catalog. */
  String addDocument(
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException;

  Map.Entry<AttachmentInfo, Metacard> parseParts(
      Collection<Part> contentParts, String transformerParam);

  /** Deletes a record from the catalog. */
  void deleteDocument(String id) throws CatalogServiceException;

  /** Retrieves the file extension fro the specified Mime Type. */
  String getFileExtensionForMimeType(String mimeType);
}
