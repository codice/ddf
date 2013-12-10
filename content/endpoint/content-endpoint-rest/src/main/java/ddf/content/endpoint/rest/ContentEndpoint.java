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
package ddf.content.endpoint.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.content.ContentFramework;
import ddf.content.ContentFrameworkException;
import ddf.content.data.ContentItem;
import ddf.content.data.impl.IncomingContentItem;
import ddf.content.operation.CreateRequest;
import ddf.content.operation.CreateResponse;
import ddf.content.operation.DeleteRequest;
import ddf.content.operation.DeleteResponse;
import ddf.content.operation.ReadRequest;
import ddf.content.operation.ReadResponse;
import ddf.content.operation.Request;
import ddf.content.operation.UpdateRequest;
import ddf.content.operation.UpdateResponse;
import ddf.content.operation.impl.CreateRequestImpl;
import ddf.content.operation.impl.DeleteRequestImpl;
import ddf.content.operation.impl.ReadRequestImpl;
import ddf.content.operation.impl.UpdateRequestImpl;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;

/**
 * The REST Endpoint for the Content Framework that provides URLs to create, read, update, and
 * delete content in the Content Repository.
 * 
 * @author rodgersh
 * @author ddf.isgs@lmco.com
 * 
 */
@Path("/")
public class ContentEndpoint {
    private static XLogger logger = new XLogger(LoggerFactory.getLogger(ContentEndpoint.class));

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private static final String DEFAULT_DIRECTIVE = "STORE_AND_PROCESS";

    private static final String DIRECTIVE_ATTACHMENT_CONTENT_ID = "directive";

    private static final String FILE_ATTACHMENT_CONTENT_ID = "file";

    private static final String FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME = "filename";

    private static final String DEFAULT_FILE_NAME = "file";

    private static final String DEFAULT_FILE_EXTENSION = ".bin";

    private static final String CONTENT_ID_HTTP_HEADER = "Content-ID";

    private static final String CONTENT_URI_HTTP_HEADER = "Content-URI";

    private ContentFramework contentFramework;

    private MimeTypeMapper mimeTypeMapper;

    public ContentEndpoint(ContentFramework framework, MimeTypeMapper mimeTypeMapper) {
        logger.debug("ENTERING: ContentEndpoint constructor");

        this.contentFramework = framework;
        this.mimeTypeMapper = mimeTypeMapper;

        logger.debug("EXITING: ContentEndpoint constructor");
    }

    /**
     * Create an entry in the Content Repository and/or the Metadata Catalog based on the request's
     * directive. The input request is in multipart/form-data format, with the expected parts of the
     * body being the directive (STORE, PROCESS, STORE_AND_PROCESS), and the file, with optional
     * filename specified, followed by the contents to be stored. If the filename is not specified
     * for the contents in the body of the input request, then the default filename "file" will be
     * used, with the file extension determined based upon the MIME type.
     * 
     * A sample multipart/form-data request would look like: Content-Type: multipart/form-data;
     * boundary=ARCFormBoundaryfqeylm5unubx1or
     * 
     * --ARCFormBoundaryfqeylm5unubx1or Content-Disposition: form-data; name="directive"
     * 
     * STORE_AND_PROCESS --ARCFormBoundaryfqeylm5unubx1or-- Content-Disposition: form-data;
     * name="myfile.json"; filename="C:\DDF\geojson_valid.json" Content-Type:
     * application/json;id=geojson
     * 
     * <contents to store go here>
     * 
     * @param multipartBody
     *            the multipart/form-data formatted body of the request
     * @param requestUriInfo
     * @return
     * @throws ContentEndpointException
     */
    @POST
    @Path("/")
    public Response create(MultipartBody multipartBody, @Context
    UriInfo requestUriInfo) throws ContentEndpointException {
        logger.trace("ENTERING: create");

        String directive = multipartBody.getAttachmentObject(DIRECTIVE_ATTACHMENT_CONTENT_ID,
                String.class);
        logger.debug("directive = " + directive);

        String contentUri = multipartBody.getAttachmentObject("contentUri", String.class);
        logger.debug("contentUri = " + contentUri);

        InputStream stream = null;
        String filename = null;
        String contentType = null;

        // TODO: For DDF-1970 (multiple files in single create request)
        // Would access List<Attachment> = multipartBody.getAllAttachments() and loop
        // through them getting all of the "file" attachments (and skipping the "directive")
        // But how to support a "contentUri" parameter *per* file attachment? Can it be
        // just another parameter to the name="file" Content-Disposition?
        Attachment contentPart = multipartBody.getAttachment(FILE_ATTACHMENT_CONTENT_ID);
        if (contentPart != null) {
            // Example Content-Type header:
            // Content-Type: application/json;id=geojson
            if (contentPart.getContentType() != null) {
                contentType = contentPart.getContentType().toString();
            }

            filename = contentPart.getContentDisposition().getParameter(
                    FILENAME_CONTENT_DISPOSITION_PARAMETER_NAME);

            // Only interested in attachments for file uploads. Any others should be covered by
            // the FormParam arguments.
            if (StringUtils.isEmpty(filename)) {
                logger.debug("No filename parameter provided - generating default filename");
                String fileExtension = DEFAULT_FILE_EXTENSION;
                try {
                    fileExtension = mimeTypeMapper.getFileExtensionForMimeType(contentType); // DDF-2307
                    if (StringUtils.isEmpty(fileExtension)) {
                        fileExtension = DEFAULT_FILE_EXTENSION;
                    }
                } catch (MimeTypeResolutionException e) {
                    logger.debug("Exception getting file extension for contentType = "
                            + contentType);
                }
                filename = DEFAULT_FILE_NAME + fileExtension; // DDF-2263
                logger.debug("No filename parameter provided - default to " + filename);
            } else {
                filename = FilenameUtils.getName(filename);
            }

            // Get the file contents as an InputStream and ensure the stream is positioned
            // at the beginning
            try {
                stream = contentPart.getDataHandler().getInputStream();
                if (stream != null && stream.available() == 0) {
                    stream.reset();
                }
            } catch (IOException e) {
                logger.warn("IOException reading stream from file attachment in multipart body", e);
            }
        } else {
            logger.debug("No file contents attachment found");
        }

        Response response = doCreate(stream, contentType, directive, filename, contentUri,
                requestUriInfo);

        logger.trace("EXITING: create");

        return response;
    }

    @GET
    @Path("/{id}")
    public Response read(@PathParam("id")
    String id) throws ContentEndpointException {
        logger.trace("ENTERING: read");

        Response response = doRead(id);

        logger.trace("EXITING: read");

        return response;
    }

    @PUT
    @Path("/{id}")
    public Response update(InputStream stream, @PathParam("id")
    String id, @HeaderParam("Content-Type")
    String contentType, @HeaderParam("directive")
    @DefaultValue("STORE_AND_PROCESS")
    String directive) throws ContentEndpointException {
        logger.trace("ENTERING: update");
        logger.debug("directive = " + directive);

        Response response = doUpdate(stream, id, contentType, directive, null);

        logger.trace("EXITING: update");

        return response;
    }

    // Used to only update an entry in the Metadata Catalog, accessing the existing catalog entry
    // via the content URI (which maps to the DAD URI of the catalog entry)
    @PUT
    @Path("/")
    public Response updateCatalogOnly(InputStream stream, @HeaderParam("Content-Type")
    String contentType, @HeaderParam("contentUri")
    String contentUri) throws ContentEndpointException {
        logger.trace("ENTERING: update");
        logger.debug("contentUri = " + contentUri);

        Response response = doUpdate(stream, null, contentType,
                Request.Directive.PROCESS.toString(), contentUri);

        logger.trace("EXITING: update");

        return response;
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id")
    String id, @HeaderParam("directive")
    @DefaultValue("STORE_AND_PROCESS")
    String directive) throws ContentEndpointException {
        logger.trace("ENTERING: delete");
        logger.debug("directive = " + directive);

        Response response = doDelete(id, directive, null);

        logger.trace("EXITING: delete");

        return response;
    }

    // Used to only delete an entry in the Metadata Catalog, accessing the existing catalog entry
    // via the content URI (which maps to the DAD URI of the catalog entry)
    @DELETE
    @Path("/")
    public Response deleteCatalogOnly(@HeaderParam("contentUri")
    String contentUri) throws ContentEndpointException {
        logger.trace("ENTERING: delete");
        logger.debug("contentUri = " + contentUri);

        Response response = doDelete(null, Request.Directive.PROCESS.toString(), contentUri);

        logger.trace("EXITING: delete");

        return response;
    }

    protected Response doCreate(InputStream stream, String contentType, String directive,
            String filename, String contentUri, UriInfo uriInfo) throws ContentEndpointException {
        logger.trace("ENTERING: doCreate");

        if (stream == null) {
            throw new ContentEndpointException("Cannot create content. InputStream is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (contentType == null) {
            throw new ContentEndpointException("Cannot create content. Content-Type is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (StringUtils.isEmpty(directive)) {
            directive = DEFAULT_DIRECTIVE;
        } else {
            // Ensure directive has no extraneous whitespace or newlines - this tends to occur
            // on the values assigned in multipart/form-data.
            // (Was seeing this when testing with Google Chrome Advanced REST Client)
            directive = directive.trim().replace(SystemUtils.LINE_SEPARATOR, "");
        }

        Request.Directive requestDirective = Request.Directive.valueOf(directive);

        String createdContentId = "";
        Response response = null;

        try {
            logger.debug("Preparing content item for contentType = " + contentType);

            ContentItem newItem = new IncomingContentItem(stream, contentType, filename); // DDF-1856
            newItem.setUri(contentUri);
            logger.debug("Creating content item.");

            CreateRequest createRequest = new CreateRequestImpl(newItem, null);
            CreateResponse createResponse = contentFramework
                    .create(createRequest, requestDirective);
            ContentItem contentItem = createResponse.getCreatedContentItem();

            if (contentItem != null) {
                createdContentId = contentItem.getId();
            }

            Response.ResponseBuilder responseBuilder = Response.ok();

            // If content was stored in content repository, i.e., STORE or STORE_AND_PROCESS,
            // then set location URI in HTTP header. However, the location URI is not the
            // physical location in the content repository as ths is hidden from the client.
            if (requestDirective != Request.Directive.PROCESS) {
                responseBuilder.status(Response.Status.CREATED);
                // responseBuilder.location( new URI( "/" + createdContentId ) );
                UriBuilder uriBuilder = UriBuilder.fromUri(uriInfo.getBaseUri());
                uriBuilder = uriBuilder.path("/" + createdContentId);
                responseBuilder.location(uriBuilder.build());
                responseBuilder.header(CONTENT_ID_HTTP_HEADER, createdContentId);
                logger.debug("Content-URI = " + contentItem.getUri());
                responseBuilder.header(CONTENT_URI_HTTP_HEADER, contentItem.getUri());
            }

            addHttpHeaders(createResponse, responseBuilder);

            response = responseBuilder.build();
        } catch (Exception e) {
            logger.warn("Exception caught during create", e);
            Response.ResponseBuilder responseBuilder = Response.ok(e.getMessage());
            responseBuilder.status(Response.Status.BAD_REQUEST);
            response = responseBuilder.build();
        }

        logger.debug("createdContentId = [" + createdContentId + "]");

        logger.trace("EXITING: doCreate");

        return response;
    }

    protected Response doRead(String id) throws ContentEndpointException {
        logger.trace("ENTERING: doRead");

        if (id == null) {
            throw new ContentEndpointException("Cannot read content. ID is null.",
                    Response.Status.BAD_REQUEST);
        }

        Response response = null;

        try {
            ReadRequest readRequest = new ReadRequestImpl(id, null);
            ReadResponse readResponse = contentFramework.read(readRequest);
            ContentItem item = readResponse.getContentItem();
            InputStream result = item.getInputStream();
            Response.ResponseBuilder builder = Response.ok(result);

            String mimeType = item.getMimeTypeRawData();
            if (mimeType != null) {
                builder.type(mimeType);
            } else {
                logger.warn("Unable to determine mime type, defaulting to " + DEFAULT_MIME_TYPE
                        + ".");
                builder.type(DEFAULT_MIME_TYPE);
            }

            try {
                builder.header(HttpHeaders.CONTENT_LENGTH, item.getSize());
            } catch (IOException e) {
                logger.debug(
                        "Total number of bytes is unknown, not sending a length with the response: ",
                        e);
            }

            response = builder.build();

        } catch (Exception e) {
            logger.error("Error retrieving item from content framework.", e);
            Response.ResponseBuilder responseBuilder = Response.ok("Content Item " + id
                    + " does not exist.\n" + e.getMessage());
            responseBuilder.status(Response.Status.NOT_FOUND);
            response = responseBuilder.build();
        }

        logger.trace("EXITING: doRead");

        return response;
    }

    protected Response doUpdate(InputStream stream, String id, String contentType,
            String directive, String contentUri) throws ContentEndpointException {
        logger.trace("ENTERING: doUpdate");

        Request.Directive requestDirective = Request.Directive.valueOf(directive);

        if (stream == null) {
            throw new ContentEndpointException("Cannot update content. InputStream is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (id == null && requestDirective != Request.Directive.PROCESS) {
            throw new ContentEndpointException("Cannot update content. ID is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (contentUri == null && requestDirective == Request.Directive.PROCESS) {
            throw new ContentEndpointException("Cannot update content. Content URI is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (contentType == null) {
            throw new ContentEndpointException("Cannot update content. Content-Type is null.",
                    Response.Status.BAD_REQUEST);
        }

        Response response = null;

        logger.debug("Preparing content item");

        ContentItem itemToUpdate = new IncomingContentItem(id, stream, contentType);
        itemToUpdate.setUri(contentUri);

        ContentItem updatedItem = null;
        try {
            UpdateRequest updateRequest = new UpdateRequestImpl(itemToUpdate, null);
            UpdateResponse updateResponse = contentFramework
                    .update(updateRequest, requestDirective);
            updatedItem = updateResponse.getUpdatedContentItem();
            Response.ResponseBuilder responseBuilder = Response.ok();
            responseBuilder.header(CONTENT_ID_HTTP_HEADER, updatedItem.getId());
            addHttpHeaders(updateResponse, responseBuilder);
            response = responseBuilder.build();
        } catch (Exception e) {
            logger.error("Error updating item in content framework", e);
            Response.ResponseBuilder responseBuilder = Response.ok("Content Item " + id
                    + " not updated.\n" + e.getMessage());
            responseBuilder.status(Response.Status.NOT_FOUND);
            response = responseBuilder.build();
        }

        logger.trace("EXITING: doUpdate");

        return response;
    }

    protected Response doDelete(String id, String directive, String contentUri)
        throws ContentEndpointException {
        logger.trace("ENTERING: doDelete");

        Request.Directive requestDirective = Request.Directive.valueOf(directive);

        if (id == null && requestDirective != Request.Directive.PROCESS) {
            throw new ContentEndpointException("Cannot delete content. ID is null.",
                    Response.Status.BAD_REQUEST);
        }

        if (contentUri == null && requestDirective == Request.Directive.PROCESS) {
            throw new ContentEndpointException("Cannot delete content. Content URI is null.",
                    Response.Status.BAD_REQUEST);
        }

        ContentItem itemToDelete = new IncomingContentItem(id, null, null);
        itemToDelete.setUri(contentUri);

        Response response = null;

        try {
            DeleteRequest deleteRequest = new DeleteRequestImpl(itemToDelete, null);
            DeleteResponse deleteResponse = contentFramework
                    .delete(deleteRequest, requestDirective);
            if (logger.isDebugEnabled()) {
                if (requestDirective == Request.Directive.PROCESS) {
                    logger.debug("Deleted content item with URI = " + contentUri);
                } else {
                    logger.debug("Deleted content item with id = " + id);
                }
            }

            if (deleteResponse.isFileDeleted()) {
                Response.ResponseBuilder responseBuilder = Response.ok();
                responseBuilder.status(Response.Status.NO_CONTENT);
                responseBuilder.header(CONTENT_ID_HTTP_HEADER, deleteResponse.getContentItem()
                        .getId());
                addHttpHeaders(deleteResponse, responseBuilder);
                response = responseBuilder.build();
            } else {
                Response.ResponseBuilder responseBuilder = Response.ok("Content Item " + id
                        + " not deleted");
                responseBuilder.status(Response.Status.NOT_FOUND);
                response = responseBuilder.build();
            }
        } catch (ContentFrameworkException e) {
            logger.error("Error deleting item from content framework", e);
            Response.ResponseBuilder responseBuilder = Response.ok("Content Item " + id
                    + " not found.\n" + e.getMessage());
            responseBuilder.status(Response.Status.NOT_FOUND);
            response = responseBuilder.build();
        }

        logger.trace("EXITING: doDelete");

        return response;
    }

    // Add all response properties as HTTP headers in response.
    // Endpoint does not care what the response properties are - the component
    // that added them, e.g., ContentPlugin, by putting them in the responseProperties
    // vs. properties of the Response intended them for public distribution.
    private <T extends Request> void addHttpHeaders(ddf.content.operation.Response<T> response,
            Response.ResponseBuilder responseBuilder) {
        if (response.hasResponseProperties()) {
            for (String propertyName : (Set<String>) response.getResponsePropertyNames()) {
                String propertyValue = response.getResponsePropertyValue(propertyName);
                if (propertyValue != null && !propertyValue.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("propertyName = [" + propertyName + "] has value ["
                                + propertyValue + "]");
                    }
                    responseBuilder.header(propertyName, propertyValue);
                }
            }
        }
    }
}
