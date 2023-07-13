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
import ddf.catalog.data.Metacard;
import ddf.catalog.resource.DataUsageLimitExceededException;
import ddf.catalog.resource.Resource;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTEndpoint extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(RESTEndpoint.class);

  private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

  private static final String HEADER_CONTENT_LENGTH = "Content-Length";

  private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

  private static final String BYTES = "bytes";

  private static final String HEADER_RANGE = "Range";

  private static final String BYTES_EQUAL = "bytes=";

  private static final String PRE_FORMAT = "<pre>%s</pre>";

  private static final String UNABLE_TO_RETRIEVE_REQUESTED_METACARD =
      "Unable to retrieve requested metacard.";
  public static final String NOT_FOUND = "Not Found";

  private CatalogService catalogService;

  private ServletFileUpload upload;

  public RESTEndpoint(CatalogService catalogService) {
    LOGGER.trace("Constructing REST Endpoint");
    this.catalogService = catalogService;
    LOGGER.trace(("Rest Endpoint constructed successfully"));
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    verifyUploadParser(config.getServletContext());
  }

  @Override
  protected void doHead(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String[] pathParts = Objects.toString(req.getPathInfo(), "").split("/");
    if (patternMatches(req, "/sources")) {
      getSources(resp, false);
    } else if (patternMatches(req, "/{id}")) {
      getHeaders(null, pathParts[1], req, resp);
    } else if (patternMatches(req, "/sources/{sourceid}/{id}")) {
      getHeaders(pathParts[2], pathParts[3], req, resp);
    } else {
      ping(resp);
    }
  }

  public void ping(HttpServletResponse response) {
    LOGGER.trace("Ping!");
    response.setStatus(200);
  }

  /**
   * REST Head. Returns headers only. Primarily used to let the client know that range requests
   * (though limited) are accepted.
   *
   * @param sourceid
   * @param id
   * @param request
   * @param response
   */
  public void getHeaders(
      String sourceid, String id, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      String transform = getTransform(request);
      final BinaryContent content =
          catalogService.getHeaders(
              sourceid,
              id,
              transform,
              request.getRequestURL().toString(),
              request.getParameterMap());

      if (content == null) {
        sendNotFoundResponse(UNABLE_TO_RETRIEVE_REQUESTED_METACARD, response);
      }

      response.setContentType(content.getMimeTypeValue());
      // Add the Accept-ranges header to let the client know that we accept ranges in bytes
      response.addHeader(HEADER_ACCEPT_RANGES, BYTES);

      setFileNameOnResponseBuilder(id, content, response);

      long size = content.getSize();
      if (size > 0) {
        response.addHeader(HEADER_CONTENT_LENGTH, String.valueOf(size));
      }
      response.setStatus(200);
    } catch (CatalogServiceException e) {
      sendBadRequestResponse(e.getMessage(), response);
    } catch (ServletException e) {
      LOGGER.info(e.getMessage());
      sendErrorResponse(e.getMessage(), response);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String[] pathParts = Objects.toString(req.getPathInfo(), "").split("/");
    if (patternMatches(req, "/sources")) {
      getSources(resp, true);
    } else if (patternMatches(req, "/{id}")) {
      getDocument(null, pathParts[1], req, resp);
    } else if (patternMatches(req, "/sources/{sourceid}/{id}")) {
      getDocument(pathParts[2], pathParts[3], req, resp);
    } else {
      sendNotFoundResponse(NOT_FOUND, resp);
    }
  }

  /**
   * REST Get. Retrieves information regarding sources available.
   *
   * @param response
   * @param writeOutput
   * @return
   */
  public void getSources(HttpServletResponse response, boolean writeOutput) throws IOException {
    final BinaryContent content = catalogService.getSourcesInfo();

    response.setStatus(200);
    response.setContentType(content.getMimeTypeValue());
    if (writeOutput) {
      content.getInputStream().transferTo(response.getOutputStream());
    }

    // Add the Accept-ranges header to let the client know that we accept ranges in bytes
    response.addHeader(HEADER_ACCEPT_RANGES, BYTES);
  }

  /**
   * REST Get. Retrieves the metadata entry specified by the id from the federated source specified
   * by sourceid. Transformer argument is optional, but is used to specify what format the data
   * should be returned.
   *
   * @param sourceId
   * @param id
   * @param request
   * @param response
   */
  public void getDocument(
      String sourceId, String id, HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    try {
      final BinaryContent content =
          catalogService.getDocument(
              sourceId,
              id,
              getTransform(request),
              getRangeStart(request),
              request.getRequestURL().toString(),
              request.getParameterMap());

      if (content == null) {
        sendNotFoundResponse(UNABLE_TO_RETRIEVE_REQUESTED_METACARD, response);
      } else {
        LOGGER.debug("Read and transform complete, preparing response.");
        response.setStatus(200);
        response.setContentType(content.getMimeTypeValue());
        content.getInputStream().transferTo(response.getOutputStream());

        // Add the Accept-ranges header to let the client know that we accept ranges in bytes
        response.addHeader(HEADER_ACCEPT_RANGES, BYTES);

        setFileNameOnResponseBuilder(id, content, response);

        long size = content.getSize();
        if (size > 0) {
          response.addHeader(HEADER_CONTENT_LENGTH, String.valueOf(size));
        }
      }
    } catch (CatalogServiceException e) {
      sendBadRequestResponse(e.getMessage(), response);
    } catch (DataUsageLimitExceededException e) {
      sendHtmlError(413, e.getMessage(), response);
    } catch (UnsupportedEncodingException e) {
      String exceptionMessage = "Unknown error occurred while processing request: ";
      LOGGER.info(exceptionMessage, e);
      throw new ServletException(exceptionMessage);
    } catch (ServletException | UnsupportedQueryException e) {
      LOGGER.info(e.getMessage());
      sendErrorResponse(e.getMessage(), response);
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (patternMatches(req, "/")) {
      addDocument(req, resp);
    } else {
      sendHtmlError(405, "Method Not Allowed", resp);
    }
  }

  /**
   * REST Post. Creates a new metadata entry in the catalog.
   *
   * @param request
   * @param response
   */
  public void addDocument(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      List<String> contentTypeList = Collections.list(request.getHeaders("Content-Type"));
      String transform = getTransform(request);
      List<FileItem> fileItems = parseFormUpload(request);

      String id =
          catalogService.addDocument(
              contentTypeList, fileItems, transform, request.getInputStream());

      response.setStatus(201);
      response.addHeader(Metacard.ID, id);
      response.addHeader("location", request.getRequestURL().append("/" + id).toString());
    } catch (CatalogServiceException | FileUploadException | ServletException e) {
      sendBadRequestResponse(e.getMessage(), response);
    }
  }

  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String[] pathParts = Objects.toString(req.getPathInfo(), "").split("/");
    if (patternMatches(req, "/{id}")) {
      updateDocument(pathParts[1], req, resp);
    } else {
      sendHtmlError(405, "Method Not Allowed", resp);
    }
  }

  /**
   * REST Put. Updates the specified entry with the provided document.
   *
   * @param id
   * @param request
   * @param response
   */
  public void updateDocument(String id, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    try {
      List<String> contentTypeList = Collections.list(request.getHeaders("Content-Type"));
      String transform = getTransform(request);
      List<FileItem> fileItems = parseFormUpload(request);

      catalogService.updateDocument(
          id, contentTypeList, fileItems, transform, request.getInputStream());

      response.setStatus(200);

    } catch (CatalogServiceException | FileUploadException | ServletException e) {
      sendBadRequestResponse(e.getMessage(), response);
    }
  }

  @Override
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String[] pathParts = Objects.toString(req.getPathInfo(), "").split("/");
    if (patternMatches(req, "/{id}")) {
      deleteDocument(pathParts[1], resp);
    } else {
      sendHtmlError(405, "Method Not Allowed", resp);
    }
  }

  /**
   * REST Delete. Deletes a record from the catalog.
   *
   * @param id
   * @return
   */
  public void deleteDocument(String id, HttpServletResponse response) throws IOException {
    try {
      catalogService.deleteDocument(id);
      response.setStatus(200);
      response.setContentType("text/plain");
      response.getOutputStream().print(id);

    } catch (CatalogServiceException | ServletException e) {
      sendBadRequestResponse(e.getMessage(), response);
    }
  }

  private static String getTransform(HttpServletRequest request) {
    String transform = null;
    if (request.getParameterValues("transform") != null
        && request.getParameterValues("transform").length == 1) {
      transform = request.getParameterValues("transform")[0];
    }
    return transform;
  }

  private void setFileNameOnResponseBuilder(
      String id, BinaryContent content, HttpServletResponse response) {
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
      response.addHeader(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"");
    }
  }

  private void sendBadRequestResponse(String message, HttpServletResponse response)
      throws IOException {
    sendHtmlError(400, message, response);
  }

  private void sendNotFoundResponse(String message, HttpServletResponse response)
      throws IOException {
    sendHtmlError(404, message, response);
  }

  private void sendErrorResponse(String message, HttpServletResponse response) throws IOException {
    sendHtmlError(500, message, response);
  }

  private void sendHtmlError(int statusCode, String message, HttpServletResponse response)
      throws IOException {
    response.setStatus(statusCode);
    response.setContentType("text/html");
    response.getOutputStream().print(String.format(PRE_FORMAT, message));
  }

  private boolean patternMatches(HttpServletRequest req, String pathPattern) {
    String path = Objects.toString(req.getPathInfo(), "");
    String pattern = Objects.toString(pathPattern, "");
    if (path.equals(pattern)) {
      return true;
    }

    String[] pathParts = path.split("/");
    String[] patternParts = pattern.split("/");
    if (Arrays.equals(pathParts, patternParts)) {
      return true;
    }

    if (pathParts.length != patternParts.length) {
      return false;
    }

    for (int i = 0; i < pathParts.length; i++) {
      String pathPart = pathParts[i];
      String patternPart = patternParts[i];
      if (!pathPart.equalsIgnoreCase(patternPart)
          && !patternPart.startsWith("{")
          && !patternPart.endsWith("}")) {
        return false;
      }
    }

    return true;
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

  private List<FileItem> parseFormUpload(HttpServletRequest request) throws FileUploadException {
    boolean isMultipart = ServletFileUpload.isMultipartContent(request);

    if (!isMultipart) {
      return null;
    }

    verifyUploadParser(request.getServletContext());
    return upload.parseRequest(request);
  }

  private void verifyUploadParser(ServletContext context) {
    if (upload != null) {
      return;
    }

    File tempDir = (File) context.getAttribute("javax.servlet.context.tempdir");
    if (tempDir == null) {
      LOGGER.debug(
          "Unable to determine servlet temporary directory. Using system temporary directory instead.");

      tempDir = FileUtils.getTempDirectory();
    }

    DiskFileItemFactory factory = new DiskFileItemFactory();
    factory.setRepository(tempDir);

    upload = new ServletFileUpload(factory);
  }
}
