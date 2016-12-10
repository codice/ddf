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

import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;

@Path("/")
public class ResourceDownloadEndpoint {

    public static final String CONTEXT_PATH = "/catalog/downloads";

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceDownloadEndpoint.class);

    private static final String ERROR_MESSAGE_TEMPLATE =
            "Unable to download the product associated with metacard [%s] from source [%s] to the product cache.";

    private final CatalogFramework catalogFramework;

    private final ReliableResourceDownloadManager downloadManager;

    public ResourceDownloadEndpoint(CatalogFramework catalogFramework,
            ReliableResourceDownloadManager downloadManager) {
        this.catalogFramework = catalogFramework;
        this.downloadManager = downloadManager;
    }

    @GET
    @Path("/{sourceId}/{metacardId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response startDownloadToCacheOnly(@PathParam("sourceId") String sourceId,
            @PathParam("metacardId") String metacardId) throws DownloadToCacheOnlyException {

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
        } catch (IOException | ResourceNotSupportedException e) {
            String message = String.format(ERROR_MESSAGE_TEMPLATE,
                    metacardId,
                    sourceId);
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

        String message = String.format(
                "The product associated with metacard [%s] from source [%s] is being downloaded to the product cache.",
                metacardId,
                sourceId);
        return Response.ok(message, MediaType.TEXT_PLAIN_TYPE)
                .build();
    }
}
