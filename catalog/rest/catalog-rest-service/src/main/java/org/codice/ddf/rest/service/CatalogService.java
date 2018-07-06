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
import ddf.catalog.resource.DataUsageLimitExceededException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

public interface CatalogService {

  String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

  String HEADER_CONTENT_LENGTH = "Content-Length";

  String HEADER_ACCEPT_RANGES = "Accept-Ranges";

  String BYTES = "bytes";

  BinaryContent getHeaders(
      String sourceid, String id, URI absolutePath, MultivaluedMap<String, String> queryParameters)
      throws CatalogException;

  BinaryContent getDocument();

  BinaryContent getDocument(
      String encodedSourceId,
      String encodedId,
      String transformerParam,
      URI absolutePath,
      MultivaluedMap<String, String> queryParameters,
      HttpServletRequest httpRequest)
      throws CatalogException, DataUsageLimitExceededException;

  BinaryContent createMetacard(MultipartBody multipartBody, String transformerParam)
      throws CatalogException;

  BinaryContent createMetacard(HttpServletRequest httpServletRequest, String transformerParam)
      throws CatalogException;

  void updateDocument(
      String id,
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogException;

  void updateDocument(
      String id,
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogException;

  String addDocument(
      List<String> contentTypeList,
      MultipartBody multipartBody,
      String transformerParam,
      InputStream message)
      throws CatalogException;

  String addDocument(
      List<String> contentTypeList,
      HttpServletRequest httpServletRequest,
      String transformerParam,
      InputStream message)
      throws CatalogException;

  void deleteDocument(String id) throws CatalogException;

  String getFileExtensionForMimeType(String mimeType);
}
