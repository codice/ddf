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
package org.codice.ddf.endpoints.rest;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Path("/")
public class RESTEndpoint implements RESTService {

    private static final String DEFAULT_METACARD_TRANSFORMER = "xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(RESTEndpoint.class);

    private static String JSON_MIME_TYPE_STRING = "application/json";

    private static final String HEADER_RANGE = "Range";

    private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";

    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    private static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";

    private static final String BYTES = "bytes";

    private static final String BYTES_EQUAL = "bytes=";

    private static MimeType JSON_MIME_TYPE = null;

    private FilterBuilder filterBuilder;

    private CatalogFramework catalogFramework;

    private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

    private MimeTypeResolver tikaMimeTypeResolver;

    static {
        MimeType mime = null;
        try {
            mime = new MimeType(JSON_MIME_TYPE_STRING);
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Failed to create json mimetype.");
        }
        JSON_MIME_TYPE = mime;

    }

    public RESTEndpoint(CatalogFramework framework) {
        LOGGER.debug("constructing rest endpoint");
        this.catalogFramework = framework;
    }

    /**
     * REST Head. Retrieves information regarding the entry specified by the id.
     * This can be used to verify that the Range header is supported (the Accept-Ranges header is returned) and to
     * get the size of the requested resource for use in Content-Range requests.
     *
     * @param id
     * @param uriInfo
     * @param httpRequest
     * @return
     */
    @HEAD
    @Path("/{id:.*}")
    public Response getHeaders(
            @PathParam("id") String id,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest httpRequest) {

        return getHeaders(null, id, uriInfo, httpRequest);
    }

    /**
     * REST Head. Returns headers only.  Primarily used to let the client know that range requests (though limited)
     * are accepted.
     *
     * @param sourceid
     * @param id
     * @param uriInfo
     * @param httpRequest
     * @return
     */
    @HEAD
    @Path("/sources/{sourceid}/{id:.*}")
    public Response getHeaders(
            @PathParam("sourceid") String sourceid,
            @PathParam("id") String id,
            @Context UriInfo uriInfo,
            @Context HttpServletRequest httpRequest) {

        Response response;
        Response.ResponseBuilder responseBuilder;
        QueryResponse queryResponse;
        Metacard card = null;
        Subject subject;

        LOGGER.debug("getHeaders");
        URI absolutePath = uriInfo.getAbsolutePath();
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();

        if (id != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Got id: " + id);
                LOGGER.debug("Map of query parameters: \n" + map.toString());
            }

            subject = getSubject(httpRequest);
            if (subject == null) {
                LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
            }

            Map<String, Serializable> convertedMap = convert(map);
            convertedMap.put("url", absolutePath.toString());

            LOGGER.debug("Map converted, retrieving product.");

            // default to xml if no transformer specified
            try {
                String transformer = DEFAULT_METACARD_TRANSFORMER;

                Filter filter = getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id);

                Collection<String> sources = null;
                if (sourceid != null) {
                    sources = new ArrayList<String>();
                    sources.add(sourceid);
                }

                QueryRequestImpl request = new QueryRequestImpl(new QueryImpl(filter), sources);
                request.setProperties(convertedMap);
                if (subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    request.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }
                queryResponse = catalogFramework.query(request, null);

                // pull the metacard out of the blocking queue
                List<Result> results = queryResponse.getResults();

                // TODO: should be poll? do we want to specify a timeout? (will
                // return null if timeout elapsed)
                if (results != null && !results.isEmpty()) {
                    card = results.get(0).getMetacard();
                }

                if (card == null) {
                    throw new ServerErrorException("Unable to retrieve requested metacard.",
                            Status.NOT_FOUND);
                }

                LOGGER.debug("Calling transform.");
                final BinaryContent content = catalogFramework.transform(card, transformer, convertedMap);
                LOGGER.debug("Read and transform complete, preparing response.");

                responseBuilder = Response.noContent();

                // Add the Accept-ranges header to let the client know that we accept ranges in bytes
                responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);

                String filename = null;

                if (content instanceof Resource) {
                    // If we got a resource, we can extract the filename.
                    filename = ((Resource) content).getName();
                } else {
                    String fileExtension = getFileExtensionForMimeType(content.getMimeTypeValue());
                    if (StringUtils.isNotBlank(fileExtension)) {
                        filename = id + fileExtension;
                    }
                }

                if (StringUtils.isNotBlank(filename)) {
                    LOGGER.debug("filename: {}", filename);
                    responseBuilder.header(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename
                            + "\"");
                }

                long size = content.getSize();
                if (size > 0) {
                    responseBuilder.header(HEADER_CONTENT_LENGTH, size);
                }

                response = responseBuilder.build();

            } catch (FederationException e) {
                String exceptionMessage = "READ failed due to unexpected exception: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (CatalogTransformerException e) {
                String exceptionMessage = "Unable to transform Metacard.  Try different transformer: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (SourceUnavailableException e) {
                String exceptionMessage = "Cannot obtain query results because source is unavailable: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (UnsupportedQueryException e) {
                String exceptionMessage = "Specified query is unsupported.  Change query and resubmit: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.BAD_REQUEST);
            }
            // The catalog framework will throw this if any of the transformers blow up. We need to
            // catch this exception
            // here or else execution will return to CXF and we'll lose this message and end up with
            // a huge stack trace
            // in a GUI or whatever else is connected to this endpoint
            catch (IllegalArgumentException e) {
                throw new ServerErrorException(e, Status.BAD_REQUEST);
            }
        } else {
            throw new ServerErrorException("No ID specified.", Status.BAD_REQUEST);
        }
        return response;
    }

    /**
     * REST Get. Retrieves the metadata entry specified by the id. Transformer argument is optional,
     * but is used to specify what format the data should be returned.
     *
     * @param id
     * @param transformerParam (OPTIONAL)
     * @param uriInfo
     * @return
     * @throws ServerErrorException
     */
    @GET
    @Path("/{id:.*}")
    public Response getDocument(@PathParam("id")
                                    String id, @QueryParam("transform")
                                    String transformerParam, @Context
                                    UriInfo uriInfo, @Context
                                    HttpServletRequest httpRequest) {

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
    @Path("/sources")
    public Response getDocument(@Context
                                    UriInfo uriInfo, @Context
                                    HttpServletRequest httpRequest) {
        BinaryContent content;
        ResponseBuilder responseBuilder;
        String sourcesString = null;

        JSONArray resultsList = new JSONArray();
        SourceInfoResponse sources;
        try {
            Subject subject = getSubject(httpRequest);
            if (subject == null) {
                LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
            }

            SourceInfoRequestEnterprise sourceInfoRequestEnterprise = new SourceInfoRequestEnterprise(true);

            if (subject != null) {
                LOGGER.debug("Adding {} property with value {} to request.",
                        SecurityConstants.SECURITY_SUBJECT, subject);
                sourceInfoRequestEnterprise.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
            }

            sources = catalogFramework.getSourceInfo(sourceInfoRequestEnterprise);
            for (SourceDescriptor source : sources.getSourceInfo()) {
                JSONObject sourceObj = new JSONObject();
                sourceObj.put("id", source.getSourceId());
                sourceObj.put("version", source.getVersion() != null ? source.getVersion() : "");
                sourceObj.put("available", new Boolean(source.isAvailable()));
                JSONArray contentTypesObj = new JSONArray();
                if (source.getContentTypes() != null) {
                    for (ContentType contentType : source.getContentTypes()) {
                        if (contentType != null && contentType.getName() != null) {
                            JSONObject contentTypeObj = new JSONObject();
                            contentTypeObj.put("name", contentType.getName());
                            contentTypeObj.put("version",
                                    contentType.getVersion() != null ? contentType.getVersion()
                                            : ""
                            );
                            contentTypesObj.add(contentTypeObj);
                        }
                    }
                }
                sourceObj.put("contentTypes", contentTypesObj);
                resultsList.add(sourceObj);
            }
        } catch (SourceUnavailableException e) {
            LOGGER.warn("Unable to retrieve Sources", e);
        }

        sourcesString = JSONValue.toJSONString(resultsList);
        content = new BinaryContentImpl(new ByteArrayInputStream(sourcesString.getBytes()),
                JSON_MIME_TYPE);
        responseBuilder = Response.ok(content.getInputStream(), content.getMimeTypeValue());

        // Add the Accept-ranges header to let the client know that we accept ranges in bytes
        responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);

        return responseBuilder.build();
    }

    /**
     * REST Get. Retrieves the metadata entry specified by the id from the federated source
     * specified by sourceid. Transformer argument is optional, but is used to specify what format
     * the data should be returned.
     *
     * @param sourceid
     * @param id
     * @param transformerParam
     * @param uriInfo
     * @return
     */
    @GET
    @Path("/sources/{sourceid}/{id:.*}")
    public Response getDocument(@PathParam("sourceid")
                                    String sourceid, @PathParam("id")
                                    String id, @QueryParam("transform")
                                    String transformerParam, @Context
                                    UriInfo uriInfo, @Context
                                    HttpServletRequest httpRequest) {

        Response response = null;
        Response.ResponseBuilder responseBuilder;
        QueryResponse queryResponse;
        Metacard card = null;
        Subject subject;

        LOGGER.debug("GET");
        URI absolutePath = uriInfo.getAbsolutePath();
        MultivaluedMap<String, String> map = uriInfo.getQueryParameters();

        if (id != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Got id: " + id);
                LOGGER.debug("Got service: " + transformerParam);
                LOGGER.debug("Map of query parameters: \n" + map.toString());
            }

            subject = getSubject(httpRequest);
            if (subject == null) {
                LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
            }

            Map<String, Serializable> convertedMap = convert(map);
            convertedMap.put("url", absolutePath.toString());

            LOGGER.debug("Map converted, retrieving product.");

            // default to xml if no transformer specified
            try {
                String transformer = DEFAULT_METACARD_TRANSFORMER;
                if (transformerParam != null) {
                    transformer = transformerParam;
                }
                Filter filter = getFilterBuilder().attribute(Metacard.ID).is().equalTo().text(id);

                Collection<String> sources = null;
                if (sourceid != null) {
                    sources = new ArrayList<String>();
                    sources.add(sourceid);
                }

                QueryRequestImpl request = new QueryRequestImpl(new QueryImpl(filter), sources);
                request.setProperties(convertedMap);
                if (subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    request.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }
                queryResponse = catalogFramework.query(request, null);

                // pull the metacard out of the blocking queue
                List<Result> results = queryResponse.getResults();

                // TODO: should be poll? do we want to specify a timeout? (will
                // return null if timeout elapsed)
                if (results != null && !results.isEmpty()) {
                    card = results.get(0).getMetacard();
                }

                if (card == null) {
                    throw new ServerErrorException("Unable to retrieve requested metacard.",
                            Status.NOT_FOUND);
                }

                LOGGER.debug("Calling transform.");
                final BinaryContent content = catalogFramework.transform(card, transformer, convertedMap);
                LOGGER.debug("Read and transform complete, preparing response.");

                long size = content.getSize();

                // Check for Range header and skip appropriately
                long rangeStart = getRangeStart(httpRequest);

                if (rangeStart > 0) {

                    LOGGER.debug("rangeStart: {}", rangeStart);

                    responseBuilder = Response.status(Status.PARTIAL_CONTENT);

                    content.getInputStream().skip(rangeStart);

                    responseBuilder.entity(content.getInputStream());
                    responseBuilder.type(content.getMimeTypeValue());
                } else {
                    if (rangeStart < 0) {
                        throw new UnsupportedQueryException("Invalid range value.");
                    } else {
                        responseBuilder = Response.ok(content.getInputStream(),
                                content.getMimeTypeValue());
                    }
                }

                // Add the Accept-ranges header to let the client know that we accept ranges in bytes
                responseBuilder.header(HEADER_ACCEPT_RANGES, BYTES);

                String filename = null;

                if (content instanceof Resource) {
                    // If we got a resource, we can extract the filename.
                    filename = ((Resource) content).getName();
                } else {
                    String fileExtension = getFileExtensionForMimeType(content.getMimeTypeValue());
                    if (StringUtils.isNotBlank(fileExtension)) {
                        filename = id + fileExtension;
                    }
                }

                if (StringUtils.isNotBlank(filename)) {
                    LOGGER.debug("filename: {}", filename);
                    responseBuilder.header(HEADER_CONTENT_DISPOSITION, "inline; filename=\"" + filename
                            + "\"");
                }

                response = responseBuilder.build();
            } catch (FederationException e) {
                String exceptionMessage = "READ failed due to unexpected exception: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (CatalogTransformerException e) {
                String exceptionMessage = "Unable to transform Metacard.  Try different transformer: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (SourceUnavailableException e) {
                String exceptionMessage = "Cannot obtain query results because source is unavailable: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
            } catch (UnsupportedQueryException e) {
                String exceptionMessage = "Specified query is unsupported.  Change query and resubmit: "
                        + e.getMessage();
                throw new ServerErrorException(exceptionMessage, Status.BAD_REQUEST);
            } catch (IOException e) {
                String exceptionMessage = "Error retrieving range: "
                        + e.getMessage();
            }

            // The catalog framework will throw this if any of the transformers blow up. We need to
            // catch this exception
            // here or else execution will return to CXF and we'll lose this message and end up with
            // a huge stack trace
            // in a GUI or whatever else is connected to this endpoint
            catch (IllegalArgumentException e) {
                throw new ServerErrorException(e, Status.BAD_REQUEST);
            }
        } else {
            throw new ServerErrorException("No ID specified.", Status.BAD_REQUEST);
        }
        return response;
    }

    /**
     * REST Put. Updates the specified metadata entry with the provided metadata.
     *
     * @param id
     * @param message
     * @return
     */
    @PUT
    @Path("/{id:.*}")
    public Response updateDocument(@PathParam("id")
                                       String id, @Context
                                       HttpHeaders headers, @Context HttpServletRequest httpRequest, InputStream message) {
        LOGGER.debug("PUT");
        Response response;

        try {
            if (id != null && message != null) {

                Subject subject = getSubject(httpRequest);
                if (subject == null) {
                    LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
                }

                MimeType mimeType = getMimeType(headers);
                UpdateRequestImpl updateReq = new UpdateRequestImpl(id, generateMetacard(mimeType,
                        id, message));

                if(subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    updateReq.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                catalogFramework.update(updateReq);
                response = Response.ok().build();
            } else {
                String errorResponseString = "Both ID and content are needed to perform UPDATE.";
                LOGGER.warn(errorResponseString);
                throw new ServerErrorException(errorResponseString, Status.BAD_REQUEST);
            }
        } catch (SourceUnavailableException e) {
            String exceptionMessage = "Cannot updated catalog entry because source is unavailable: "
                    + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        } catch (MetacardCreationException e) {
            String exceptionMessage = "Unable to update Metacard with provided metadata: "
                    + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.BAD_REQUEST);
        } catch (IngestException e) {
            String exceptionMessage = "Error cataloging updated metadata: " + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    /**
     * REST Post. Creates a new metadata entry in the catalog.
     *
     * @param message
     * @return
     */
    @POST
    public Response addDocument(@Context HttpHeaders headers,
                                @Context UriInfo requestUriInfo,
                                @Context HttpServletRequest httpRequest,
                                InputStream message) {
        LOGGER.debug("POST");
        Response response;

        MimeType mimeType = getMimeType(headers);

        try {
            if (message != null) {
                Subject subject = getSubject(httpRequest);
                if (subject == null) {
                    LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
                }

                CreateRequestImpl createReq = new CreateRequestImpl(generateMetacard(mimeType,
                        null, message));


                if(subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    createReq.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                CreateResponse createResponse = catalogFramework.create(createReq);

                String id = createResponse.getCreatedMetacards().get(0).getId();

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Create Response id [" + id + "]");
                }

                UriBuilder uriBuilder = requestUriInfo.getAbsolutePathBuilder().path("/" + id);

                ResponseBuilder responseBuilder = Response.created(uriBuilder.build());

                responseBuilder.header(Metacard.ID, id);

                response = responseBuilder.build();

                LOGGER.debug("Entry successfully saved, id: " + id);

            } else {
                String errorMessage = "No content found, cannot do CREATE.";
                LOGGER.warn(errorMessage);
                throw new ServerErrorException(errorMessage, Status.BAD_REQUEST);
            }
        } catch (SourceUnavailableException e) {
            String exceptionMessage = "Cannot create catalog entry because source is unavailable: "
                    + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        } catch (IngestException e) {
            String exceptionMessage = "Error while storing entry in catalog: " + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        } catch (MetacardCreationException e) {
            String exceptionMessage = "Unable to create Metacard from provided metadata: "
                    + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.BAD_REQUEST);
        }

        return response;
    }

    /**
     * REST Delete. Deletes a record from the catalog.
     *
     * @param id
     * @return
     */
    @DELETE
    @Path("/{id:.*}")
    public Response deleteDocument(@PathParam("id") String id, @Context HttpServletRequest httpRequest) {
        LOGGER.debug("DELETE");
        Response response;
        try {
            if (id != null) {
                Subject subject = getSubject(httpRequest);
                if (subject == null) {
                    LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
                }

                DeleteRequestImpl deleteReq = new DeleteRequestImpl(id);

                if(subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    deleteReq.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                catalogFramework.delete(deleteReq);
                response = Response.ok(id).build();
            } else {
                String errorMessage = "ID of entry not specified, cannot do DELETE.";
                LOGGER.warn(errorMessage);
                throw new ServerErrorException(errorMessage, Status.BAD_REQUEST);
            }
        } catch (SourceUnavailableException ce) {
            String exceptionMessage = "Could not delete entry from catalog since the source is unavailable: "
                    + ce.getMessage();
            LOGGER.warn(exceptionMessage, ce.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        } catch (IngestException e) {
            String exceptionMessage = "Error deleting entry from catalog: " + e.getMessage();
            LOGGER.warn(exceptionMessage, e.getCause());
            throw new ServerErrorException(exceptionMessage, Status.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    private Map<String, Serializable> convert(MultivaluedMap<String, String> map) {
        Map<String, Serializable> convertedMap = new HashMap<String, Serializable>();

        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();

            if (value.size() == 1) {
                convertedMap.put(key, value.get(0));
            } else {
                // List is not serializable so we make it a String array
                convertedMap.put(key, value.toArray());
            }
        }

        return convertedMap;
    }

    private Metacard generateMetacard(MimeType mimeType, String id, InputStream message)
            throws MetacardCreationException {

        List<InputTransformer> listOfCandidates = mimeTypeToTransformerMapper.findMatches(
                InputTransformer.class, mimeType);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("List of matches for mimeType [" + mimeType + "]:" + listOfCandidates);
        }

        Metacard generatedMetacard = null;

        byte[] messageBytes;
        try {
            messageBytes = IOUtils.toByteArray(message);
        } catch (IOException e) {
            throw new MetacardCreationException("Could not copy bytes of content message.", e);
        }

        Iterator<InputTransformer> it = listOfCandidates.iterator();

        while (it.hasNext()) {

            InputStream inputStreamMessageCopy = new ByteArrayInputStream(messageBytes);
            InputTransformer transformer = null;

            try {
                transformer = (InputTransformer) it.next();
                generatedMetacard = transformer.transform(inputStreamMessageCopy);
            } catch (CatalogTransformerException e) {
                LOGGER.debug("Transformer [" + transformer + "] could not create metacard.", e);
            } catch (IOException e) {
                LOGGER.debug("Transformer [" + transformer + "] could not create metacard. ", e);
            }
            if (generatedMetacard != null) {
                break;
            }
        }

        if (generatedMetacard == null) {
            throw new MetacardCreationException("Could not create metacard with mimeType "
                    + mimeType + ". No valid transformers found.");
        }

        if (id != null) {
            generatedMetacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        } else {
            LOGGER.debug("Metacard had a null id");
        }
        return generatedMetacard;

    }

    private MimeType getMimeType(HttpHeaders headers) {
        List<String> contentTypeList = headers.getRequestHeader(HttpHeaders.CONTENT_TYPE);

        String singleMimeType = null;

        if (contentTypeList != null && !contentTypeList.isEmpty()) {
            singleMimeType = contentTypeList.get(0);
            LOGGER.debug("Encountered [" + singleMimeType + "] " + HttpHeaders.CONTENT_TYPE);
        }

        MimeType mimeType = null;

        // Sending a null argument to MimeType causes NPE
        if (singleMimeType != null) {
            try {
                mimeType = new MimeType(singleMimeType);
            } catch (MimeTypeParseException e) {
                LOGGER.debug("Could not parse mime type from headers.", e);
            }
        }

        return mimeType;
    }

    private String getFileExtensionForMimeType(String mimeType) {
        String fileExtension = this.tikaMimeTypeResolver.getFileExtensionForMimeType(mimeType);
        LOGGER.debug("Mime Type [{}] resolves to file extension [{}].",
                mimeType, fileExtension);
        return fileExtension;
    }

    private boolean rangeHeaderExists(HttpServletRequest httpRequest) {
        boolean response = false;

        if (null != httpRequest) {
            if (null != httpRequest.getHeader(HEADER_RANGE)) {
                response = true;
            }
        }

        return response;
    }

    // Return 0 (beginning of stream) if the range header does not exist.
    private long getRangeStart(HttpServletRequest httpRequest) throws UnsupportedQueryException {
        long response = 0;

        if (httpRequest != null) {
            if (rangeHeaderExists(httpRequest)) {
                String rangeHeader = httpRequest.getHeader(HEADER_RANGE);
                String range = getRange(rangeHeader);

                if (range != null) {
                    response = Long.valueOf(range);
                }
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
                    response = rangeHeader.substring(BYTES_EQUAL.length(), rangeHeader.lastIndexOf("-"));
                } else {
                    response = rangeHeader.substring(BYTES_EQUAL.length());
                }
            } else {
                throw new UnsupportedQueryException("Invalid range header: " + rangeHeader);
            }
        }

        return response;
    }

    protected Subject getSubject(HttpServletRequest request) {
        Subject subject = null;
        if (request != null) {
            subject = (Subject) request.getAttribute(SecurityConstants.SECURITY_SUBJECT);
        } else {
            LOGGER.debug("No servlet request found, cannot obtain user credentials.");
        }
        return subject;
    }

    public MimeTypeToTransformerMapper getMimeTypeToTransformerMapper() {
        return mimeTypeToTransformerMapper;
    }

    public void setMimeTypeToTransformerMapper(
            MimeTypeToTransformerMapper mimeTypeToTransformerMapper) {
        this.mimeTypeToTransformerMapper = mimeTypeToTransformerMapper;
    }

    public FilterBuilder getFilterBuilder() {
        return filterBuilder;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setTikaMimeTypeResolver(MimeTypeResolver mimeTypeResolver) {
        this.tikaMimeTypeResolver = mimeTypeResolver;
    }
}
