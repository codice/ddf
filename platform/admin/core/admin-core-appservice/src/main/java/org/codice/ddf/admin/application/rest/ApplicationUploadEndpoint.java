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
package org.codice.ddf.admin.application.rest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.impl.ApplicationFileInstaller;
import org.codice.ddf.admin.application.service.impl.ZipFileApplicationDetails;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST Endpoint for the Application Service that provides an endpoint to upload new/upgraded
 * applications to the system.
 */
@Path("/")
public class ApplicationUploadEndpoint {
  private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

  private static final String DEFAULT_FILE_NAME = "file.jar";

  private static String defaultFileLocation =
      new AbsolutePathResolver("data/installer/uploads/").getPath();

  private static final String JAR_EXT = "jar";

  private static final String KAR_EXT = "kar";

  private final ApplicationService appService;

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationUploadEndpoint.class);

  public ApplicationUploadEndpoint(ApplicationService appService) {
    this.appService = appService;
  }

  @POST
  @Path("/update")
  @Produces("application/json")
  public Response update(MultipartBody multipartBody, @Context UriInfo requestUriInfo) {
    LOGGER.trace("ENTERING: update");

    Response response;

    List<Attachment> attachmentList = multipartBody.getAllAttachments();
    File newFile = null;
    for (Attachment attachment : attachmentList) {
      newFile = createFileFromAttachement(attachment);
    }

    try {
      if (newFile != null) {
        ZipFileApplicationDetails appDetails = ApplicationFileInstaller.getAppDetails(newFile);

        if (appDetails != null) {
          // lets get the existing app if it exists.
          Application existingApplication = appService.getApplication(appDetails.getName());
          boolean wasExistingAppStarted = false; // assume false until proved
          // otherwise.
          if (existingApplication != null) {
            wasExistingAppStarted = appService.isApplicationStarted(existingApplication);
            appService.removeApplication(existingApplication);
          }
          appService.addApplication(newFile.toURI());

          // if application was started before it was removed, lets try and start it.
          if (wasExistingAppStarted) {
            appService.startApplication(appDetails.getName());
          }
        } else {
          throw new ApplicationServiceException(
              "No Application details could be extracted from the provided file.");
        }
      } else {
        throw new ApplicationServiceException("No file attachment provided.");
      }

      // we need to output valid JSON to the client so fileupload can correctly call
      // done/fail callbacks correctly.
      Response.ResponseBuilder responseBuilder =
          Response.ok("{\"status\":\"success\"}").type("application/json");
      response = responseBuilder.build();
    } catch (ApplicationServiceException e) {
      LOGGER.warn("Unable to update an application on the server: {}", newFile, e);
      Response.ResponseBuilder responseBuilder = Response.serverError();
      response = responseBuilder.build();
    }

    LOGGER.trace("EXITING: update");

    return response;
  }

  @POST
  @Path("/")
  public Response create(MultipartBody multipartBody, @Context UriInfo requestUriInfo) {
    LOGGER.trace("ENTERING: create");

    Response response;

    List<Attachment> attachmentList = multipartBody.getAllAttachments();
    File newFile = null;
    for (Attachment attachment : attachmentList) {
      newFile = createFileFromAttachement(attachment);
    }

    try {
      if (newFile != null) {
        appService.addApplication(newFile.toURI());
      }

      Response.ResponseBuilder responseBuilder = Response.ok();
      response = responseBuilder.build();
    } catch (ApplicationServiceException e) {
      LOGGER.warn("Unable to add the application to the server: {}", newFile, e);
      Response.ResponseBuilder responseBuilder = Response.serverError();
      response = responseBuilder.build();
    }

    LOGGER.trace("EXITING: create");

    return response;
  }

  /**
   * Copies the attachment to a system file location. Once copied, a file is returned of the copied
   * file.
   *
   * @param attachment the attachment to copy and extract.
   * @return The file of the copied attachment.
   */
  private File createFileFromAttachement(Attachment attachment) {
    InputStream inputStream = null;
    String filename = null;
    File newFile = null;
    if (attachment.getContentDisposition() != null) {
      filename =
          attachment
              .getContentDisposition()
              .getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME);
    }

    if (StringUtils.isEmpty(filename)) {
      LOGGER.debug("Filename not found, using default.");
      filename = DEFAULT_FILE_NAME;
    } else {
      filename = FilenameUtils.getName(filename);
      LOGGER.debug("Filename: {}", filename);
    }

    try {
      inputStream = attachment.getDataHandler().getInputStream();
      if (inputStream != null && inputStream.available() == 0) {
        inputStream.reset();
      }
    } catch (IOException e) {
      LOGGER.debug("IOException reading stream from file attachment in multipart body", e);
      IOUtils.closeQuietly(inputStream);
    }

    if (filename.endsWith(JAR_EXT) || filename.endsWith(KAR_EXT)) {
      if (inputStream != null) {
        try {
          File uploadDir = new File(defaultFileLocation);
          if (!uploadDir.exists()) {
            if (uploadDir.mkdirs()) {
              LOGGER.info("Unable to make directory {}", uploadDir.getAbsolutePath());
            }
          }

          newFile = new File(uploadDir, filename);

          FileUtils.copyInputStreamToFile(inputStream, newFile);

        } catch (IOException e) {
          LOGGER.debug("Unable to write file.", e);
          newFile = null;
        } finally {
          IOUtils.closeQuietly(inputStream);
        }
      } else {
        LOGGER.debug("No file attachment found");
      }
    } else {
      LOGGER.debug("Wrong file type.");
      Response.ResponseBuilder responseBuilder = Response.serverError();
      responseBuilder.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415);
      IOUtils.closeQuietly(inputStream);
    }
    return newFile;
  }

  /**
   * Setter method for DEFAULT_FILE_LOCATION for testing purposes
   *
   * @param fileLocation the desired fileLocation
   */
  void setDefaultFileLocation(String fileLocation) {
    defaultFileLocation = new AbsolutePathResolver(fileLocation).getPath();
  }
}
