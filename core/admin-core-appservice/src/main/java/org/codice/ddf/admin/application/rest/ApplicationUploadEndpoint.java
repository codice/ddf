/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.application.rest;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The REST Endpoint for the Application Service that provides an endpoint to upload new/upgraded applications
 * to the system.
 *
 * @author Scott Tustison
 * @author ddf.isgs@lmco.com
 *
 */
@Path("/")
public class ApplicationUploadEndpoint {
    private final ApplicationService appService;

    private Logger logger = LoggerFactory.getLogger(ApplicationUploadEndpoint.class);

    private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

    private static final String DEFAULT_FILE_NAME = "file.jar";

    private static final String DEFAULT_FILE_LOCATION = "data/installer/uploads/";

    private static final String JAR_EXT = "jar";

    private static final String KAR_EXT = "kar";

    public ApplicationUploadEndpoint(ApplicationService appService) {
        this.appService = appService;
    }

    @POST
    @Path("/")
    public Response create(MultipartBody multipartBody, @Context UriInfo requestUriInfo) {
        logger.trace("ENTERING: create");

        InputStream inputStream = null;
        String filename;
        Response response = null;

        List<Attachment> attachmentList = multipartBody.getAllAttachments();
        File newFile = null;
        for(Attachment attachment : attachmentList) {
            filename = attachment.getContentDisposition().getParameter(FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME);

            if (StringUtils.isEmpty(filename)) {
                logger.debug("Filename not found, using default.");
                filename = DEFAULT_FILE_NAME;
            } else {
                filename = FilenameUtils.getName(filename);
                logger.debug("Filename: {}", filename);
            }

            try {
                inputStream = attachment.getDataHandler().getInputStream();
                if (inputStream != null && inputStream.available() == 0) {
                    inputStream.reset();
                }
            } catch (IOException e) {
                logger.warn("IOException reading stream from file attachment in multipart body", e);
                Response.ResponseBuilder responseBuilder = Response.serverError();
                response = responseBuilder.build();
                closeInputStream(inputStream);
            }

            if (filename.endsWith(JAR_EXT) || filename.endsWith(KAR_EXT)) {
                if (inputStream != null) {
                    try {
                        File uploadDir = new File(DEFAULT_FILE_LOCATION);
                        if (!uploadDir.exists()) {
                            uploadDir.mkdirs();
                        }

                        newFile = new File(uploadDir, filename);
                        newFile.createNewFile();

                        FileOutputStream outputStream = new FileOutputStream(newFile);

                        int read;
                        byte[] bytes = new byte[1024];

                        while ((read = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, read);
                        }

                        outputStream.flush();

                        outputStream.close();

                    } catch (IOException e) {
                        logger.warn("Unable to write file.", e);
                        Response.ResponseBuilder responseBuilder = Response.serverError();
                        response = responseBuilder.build();
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e) {
                            logger.warn("Unable to close the input file stream.", e);
                        }
                    }
                } else {
                    logger.debug("No file attachment found");
                    Response.ResponseBuilder responseBuilder = Response.serverError();
                    response = responseBuilder.build();
                    closeInputStream(inputStream);
                }
            } else {
                logger.debug("Wrong file type.");
                Response.ResponseBuilder responseBuilder = Response.serverError();
                responseBuilder.status(415);
                response = responseBuilder.build();
                closeInputStream(inputStream);
            }
        }

        if(response == null) {
            try {
                appService.addApplication(newFile.toURI());

                Response.ResponseBuilder responseBuilder = Response.ok();
                response = responseBuilder.build();
            } catch (ApplicationServiceException e) {
                logger.error("Unable to add the application to the server: " + newFile.toString(), e);
                Response.ResponseBuilder responseBuilder = Response.serverError();
                response = responseBuilder.build();
            }
        }

        logger.trace("EXITING: create");

        return response;
    }

    private void closeInputStream(InputStream inputStream) {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.warn("Unable to close the input file stream.", e);
            }
        }
    }

}
