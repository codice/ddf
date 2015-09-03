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
package ddf.content.plugin.cataloger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.content.plugin.PluginExecutionException;
import ddf.security.common.util.Security;

/**
 * Cataloger provides the create, update, and delete capabilities for entries in the Metadata
 * Catalog (MDC) by invoking the {@link CatalogFramework}. Cataloger is the single point of entry
 * from the Content Framework to the {@link CatalogFramework}.
 */
public class Cataloger {
    private static final String CREATE_WARNING_MSG = "Unable to create catalog entry";

    private static final String UPDATE_WARNING_MSG = "Unable to update catalog entry";

    private static final String DELETE_WARNING_MSG = "Unable to delete catalog entry";

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(Cataloger.class));

    private CatalogFramework catalogFramework;

    /**
     * @param catalogFramework the parent framework for this Cataloger
     */
    public Cataloger(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    /**
     * Creates a catalog entry in the Metadata Catalog (MDC) using the {@link CatalogFramework} for
     * the specified {@link Metacard}.
     *
     * @param metacard the {@link Metacard} to create a catalog entry for
     * @return the catalog ID created in the MDC
     * @throws PluginExecutionException on failure to create the metacard
     */
    public String createMetacard(Metacard metacard) throws PluginExecutionException {
        logger.trace("ENTERING: createMetacard");

        String catalogId = null;
        if (metacard != null) {
            logger.debug("Creating catalog CreateRequest with metacard  (ID = " + metacard.getId()
                    + ")");
            final CreateRequestImpl catalogCreateRequest = new CreateRequestImpl(metacard);
            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String id = null;
                    try {
                        logger.debug("Calling catalog framework");

                        CreateResponse catalogCreateResponse = catalogFramework
                                .create(catalogCreateRequest);
                        List<Metacard> createdMetacards = catalogCreateResponse
                                .getCreatedMetacards();
                        if (createdMetacards != null && createdMetacards.size() == 1) {
                            logger.debug("Catalog Framework returned a metacard");
                            Metacard createdMetacard = createdMetacards.get(0);
                            id = createdMetacard.getId();
                        }
                    } catch (IngestException | SourceUnavailableException e) {
                        String msg = CREATE_WARNING_MSG + "\n" + e.getMessage();
                        logger.warn(msg, e);
                        throw new PluginExecutionException(msg, e);
                    }
                    return id;
                }
            };

            try {
                catalogId = Security.runWithSystemSubjectFallback(callable);
            } catch (Exception e) {
                String msg = CREATE_WARNING_MSG + "\n" + e.getMessage();
                logger.warn(msg, e);
                throw new PluginExecutionException(msg, e);
            }
        }
        logger.debug("catalogId = " + catalogId);
        logger.trace("EXITING: createMetacard");

        return catalogId;
    }

    // Can only update catalog entry by product URI from Content Framework because the catalog ID is
    // never
    // provided as an input from the client (via ContentRestEndpoint)
    public String updateMetacard(String productUri, Metacard metacard)
            throws PluginExecutionException {
        logger.trace("ENTERING: updateMetacard");

        String updatedCatalogId = null;

        if (metacard != null) {
            logger.debug("Creating catalog UpdateRequest with metacard  (ProductURI = " + productUri
                    + ")");
            URI[] productUris;
            try {
                productUris = new URI[] {new URI(productUri)};
            } catch (URISyntaxException e1) {
                throw new PluginExecutionException(e1);
            }
            List<Metacard> metacards = Collections.singletonList(metacard);
            final UpdateRequestImpl catalogUpdateRequest = new UpdateRequestImpl(productUris,
                    metacards);

            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String id = null;
                    try {
                        logger.debug("Calling catalog framework");
                        UpdateResponse catalogUpdateResponse = catalogFramework
                                .update(catalogUpdateRequest);
                        List<Update> updatedMetacards = catalogUpdateResponse.getUpdatedMetacards();
                        if (updatedMetacards != null && updatedMetacards.size() == 1) {
                            logger.debug("Catalog Framework returned a metacard");
                            Update updatedMetacard = updatedMetacards.get(0);
                            id = updatedMetacard.getNewMetacard().getId();
                            logger.debug("updatedCatalogId = " + id);
                        }
                    } catch (IngestException | SourceUnavailableException e) {
                        String msg = UPDATE_WARNING_MSG + "\n" + e.getMessage();
                        logger.warn(msg, e);
                        throw new PluginExecutionException(msg, e);
                    }
                    return id;
                }
            };

            try {
                updatedCatalogId = Security.runWithSystemSubjectFallback(callable);
            } catch (Exception e) {
                String msg = UPDATE_WARNING_MSG + "\n" + e.getMessage();
                logger.warn(msg, e);
                throw new PluginExecutionException(msg, e);
            }
        }

        logger.trace("EXITING: updateMetacard");

        return updatedCatalogId;
    }

    // Can only update catalog entry by product URI from Content Framework because the catalog ID is
    // never
    // provided as an input from the client (via ContentRestEndpoint)
    public String deleteMetacard(String productUri) throws PluginExecutionException {
        logger.trace("ENTERING: deleteMetacard");

        String deletedCatalogId = null;

        if (productUri != null) {
            logger.debug(
                    "Creating catalog DeleteRequest with metacard  (URI = " + productUri + ")");
            URI uri;
            try {
                uri = new URI(productUri);
            } catch (URISyntaxException e) {
                throw new PluginExecutionException(e);
            }

            final DeleteRequestImpl catalogDeleteRequest = new DeleteRequestImpl(uri);

            Callable<String> callable = new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String id = null;
                    try {
                        logger.debug("Calling catalog framework");
                        DeleteResponse catalogDeleteResponse = catalogFramework
                                .delete(catalogDeleteRequest);
                        List<Metacard> deletedMetacards = catalogDeleteResponse
                                .getDeletedMetacards();
                        if (deletedMetacards != null && deletedMetacards.size() == 1) {
                            logger.debug("Catalog Framework returned a metacard");
                            Metacard deletedMetacard = deletedMetacards.get(0);
                            id = deletedMetacard.getId();
                            logger.debug("deletedCatalogId = " + id);
                        }
                    } catch (IngestException | SourceUnavailableException e) {
                        String msg = DELETE_WARNING_MSG + "\n" + e.getMessage();
                        logger.warn(msg, e);
                        throw new PluginExecutionException(msg, e);
                    }
                    return id;
                }
            };

            try {
                deletedCatalogId = Security.runWithSystemSubjectFallback(callable);
            } catch (Exception e) {
                String msg = DELETE_WARNING_MSG + "\n" + e.getMessage();
                logger.warn(msg, e);
                throw new PluginExecutionException(msg, e);
            }

        }

        logger.trace("EXITING: deleteMetacard");

        return deletedCatalogId;
    }
}
