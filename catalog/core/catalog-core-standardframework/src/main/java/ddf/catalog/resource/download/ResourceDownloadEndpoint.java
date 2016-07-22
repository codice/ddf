/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.download;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.boon.json.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

/**
 * REST endpoint class used to manage metacard product downloads.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
@Path("/")
public class ResourceDownloadEndpoint {

    public static final String CONTEXT_PATH = "/catalog/downloads";

    public static final String SOURCE_PARAM = "source";

    public static final String METACARD_PARAM = "metacard";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDownloadEndpoint.class);

    private static final String ERROR_MESSAGE_TEMPLATE =
            "Unable to download the product associated with metacard [%s] from source [%s] to the product cache.";

    private final CatalogFramework catalogFramework;

    private final ReliableResourceDownloadManager downloadManager;

    private final ObjectMapper objectMapper;

    static class ResourceDownloadResponse {
        private String downloadId;

        private ResourceDownloadResponse(String downloadId) {
            this.downloadId = downloadId;
        }

        public String getDownloadId() {
            return downloadId;
        }
    }

    public ResourceDownloadEndpoint(CatalogFramework catalogFramework,
            ReliableResourceDownloadManager downloadManager, ObjectMapper objectMapper) {
        this.catalogFramework = catalogFramework;
        this.downloadManager = downloadManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Gets the list of active product downloads, or starts an asynchronous download of a specific
     * metacard product to the product cache if called with query parameters.
     * <p/>
     * This method supports both starting new downloads and returning the current list of active
     * downloads based on the presence of query parameters. This behavior is required because
     * some clients (e.g., {@link ddf.action.Action}) only support GET and need to be able to start
     * downloads.
     *
     * @param sourceId   ID of the federated source where the product should be downloaded from
     * @param metacardId ID of the metacard that contains the product to download
     * @return response object containing either the ID of the download that can be used as a
     * product to perform operations on that download using this endpoint, or list of all the
     * currently active downloads
     * @throws DownloadToCacheOnlyException thrown if the product download couldn't be started
     */
    @GET
    @Produces(APPLICATION_JSON)
    public Response getDownloadListOrDownloadToCache(@QueryParam(SOURCE_PARAM) String sourceId,
            @QueryParam(METACARD_PARAM) String metacardId) throws DownloadToCacheOnlyException {

        if ((sourceId == null) && (metacardId == null)) {
            return getDownloadList();
        }

        return downloadToCache(sourceId, metacardId);
    }

    /**
     * Starts an asynchronous download of a specific metacard product to the product cache.
     *
     * @param sourceId   ID of the federated source where the product should be downloaded from
     * @param metacardId ID of the metacard that contains the product to download
     * @return response object containing the ID of the download that can be used as a resource
     * to perform operations on that download using this endpoint
     * @throws DownloadToCacheOnlyException
     */
    @POST
    @Produces(APPLICATION_JSON)
    public Response downloadToCache(@FormParam(SOURCE_PARAM) String sourceId,
            @FormParam(METACARD_PARAM) String metacardId) throws DownloadToCacheOnlyException {

        if (sourceId == null) {
            throw new DownloadToCacheOnlyException(Status.BAD_REQUEST, "Source ID missing");
        }

        if (metacardId == null) {
            throw new DownloadToCacheOnlyException(Status.BAD_REQUEST, "Metacard ID missing");
        }

        ResourceRequest resourceRequest = new ResourceRequestById(metacardId);

        if (!downloadManager.isCacheEnabled()) {
            String message = "Caching of products is not enabled.";
            LOGGER.error(message);
            throw new DownloadToCacheOnlyException(Status.BAD_REQUEST, message);
        }

        try {
            LOGGER.debug(
                    "Attempting to download the product associated with metacard [{}] from source [{}] to the product cache.",
                    metacardId,
                    sourceId);
            ResourceResponse resourceResponse = catalogFramework.getResource(resourceRequest,
                    sourceId);

            if (resourceResponse == null) {
                String message = String.format(ERROR_MESSAGE_TEMPLATE, metacardId, sourceId);
                LOGGER.error(message);
                throw new DownloadToCacheOnlyException(Status.INTERNAL_SERVER_ERROR, message);
            }

            ResourceDownloadResponse resourceDownloadResponse =
                    new ResourceDownloadResponse((String) resourceResponse.getPropertyValue(
                            ReliableResourceDownloadManager.DOWNLOAD_ID_PROPERTY_KEY));

            return Response.ok(objectMapper.toJson(resourceDownloadResponse))
                    .build();
        } catch (IOException | ResourceNotSupportedException e) {
            String message = String.format(ERROR_MESSAGE_TEMPLATE, metacardId, sourceId);
            LOGGER.error(message, e);
            throw new DownloadToCacheOnlyException(Status.INTERNAL_SERVER_ERROR, message);
        } catch (ResourceNotFoundException e) {
            String message = String.format(
                    ERROR_MESSAGE_TEMPLATE + " The product could not be found.",
                    metacardId,
                    sourceId);
            LOGGER.error(message, e);
            throw new DownloadToCacheOnlyException(Status.NOT_FOUND, message);
        }
    }

    /**
     * Gets the list of active product downloads.
     *
     * @return response object containing the list of all the currently active downloads
     * @throws DownloadToCacheOnlyException thrown if the product download couldn't be started
     */
    public Response getDownloadList() throws DownloadToCacheOnlyException {
        try {
            List<DownloadInfo> downloadsInProgress = downloadManager.getDownloadsInProgress();
            return Response.ok(objectMapper.toJson(downloadsInProgress))
                    .build();
        } catch (RuntimeException e) {
            LOGGER.error("Unable to get list of downloads.", e);
            throw new DownloadToCacheOnlyException(Status.INTERNAL_SERVER_ERROR,
                    "Unable to get list of downloads");
        }
    }
}
