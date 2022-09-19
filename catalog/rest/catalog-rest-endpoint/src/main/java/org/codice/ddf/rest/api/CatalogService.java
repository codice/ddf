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
package org.codice.ddf.rest.api;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.resource.DataUsageLimitExceededException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.commons.fileupload.FileItem;

/** Catalog service interface */
public interface CatalogService {

  /**
   * Retrieves header information regarding the entry specified by the id such as the Accept-Ranges
   * and Content-Range headers.
   */
  BinaryContent getHeaders(
      String sourceid,
      String id,
      String transform,
      String absolutePath,
      Map<String, String[]> parameters)
      throws CatalogServiceException, ServletException;

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
      long bytesToSkip,
      String absolutePath,
      Map<String, String[]> parameters)
      throws CatalogServiceException, DataUsageLimitExceededException, ServletException;

  /** Updates the specified metadata entry with the provided metadata. */
  void updateDocument(
      String id,
      List<String> contentTypeList,
      List<FileItem> fileItems,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException;

  /** Creates a new metadata entry in the catalog. */
  String addDocument(
      List<String> contentTypeList,
      List<FileItem> fileItems,
      String transformerParam,
      InputStream message)
      throws CatalogServiceException, ServletException;

  /** Deletes a record from the catalog. */
  void deleteDocument(String id) throws CatalogServiceException, ServletException;

  /** Retrieves the file extension from the specified Mime Type. */
  String getFileExtensionForMimeType(String mimeType);
}
