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
package org.codice.ddf.attachment.impl;

import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.attachment.AttachmentInfo;
import org.codice.ddf.attachment.AttachmentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttachmentParserImpl implements AttachmentParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentParserImpl.class);

  private static final String DEFAULT_FILE_EXTENSION = "bin";

  private static final String DEFAULT_FILE_NAME = "file";

  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

  /**
   * Basic mime types that will be attempted to refine to a more accurate mime type based on the
   * file extension of the filename specified in the create request.
   */
  private static final List<String> REFINEABLE_MIME_TYPES =
      Arrays.asList(DEFAULT_MIME_TYPE, "text/plain");

  private final MimeTypeMapper mimeTypeMapper;

  public AttachmentParserImpl(MimeTypeMapper mimeTypeMapper) {
    this.mimeTypeMapper = mimeTypeMapper;
  }

  @Override
  public AttachmentInfo generateAttachmentInfo(
      InputStream inputStream, String contentType, String submittedFilename) {

    try {
      if (inputStream != null && inputStream.available() == 0) {
        inputStream.reset();
      }
    } catch (IOException e) {
      LOGGER.debug("IOException reading stream from file attachment in multipart body", e);
    }

    String filename = submittedFilename;

    if (StringUtils.isEmpty(filename)) {
      LOGGER.debug(
          "No filename parameter provided - generating default filename: fileExtension={}",
          DEFAULT_FILE_EXTENSION);
      String fileExtension = DEFAULT_FILE_EXTENSION;
      try {
        fileExtension = mimeTypeMapper.getFileExtensionForMimeType(contentType);
        if (StringUtils.isEmpty(fileExtension)) {
          fileExtension = DEFAULT_FILE_EXTENSION;
        }
      } catch (MimeTypeResolutionException e) {
        LOGGER.debug("Exception getting file extension for contentType = {}", contentType);
      }
      filename = DEFAULT_FILE_NAME + "." + fileExtension;
      LOGGER.debug("No filename parameter provided - default to {}", filename);
    } else {
      filename = FilenameUtils.getName(filename);

      if (StringUtils.isEmpty(contentType) || REFINEABLE_MIME_TYPES.contains(contentType)) {
        contentType = contentTypeFromFilename(filename);
      }
    }

    return new AttachmentInfoImpl(inputStream, filename, contentType);
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
}
