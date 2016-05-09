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
 **/
package org.codice.ddf.registry.federationadmin.service;

import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Service that provides the interface to get and add registry metacards
 */
public interface FederationAdminService {

    /**
     * Add the given metacard to the catalog
     *
     * @param metacard
     *          Metacard to be stored
     * @throws SourceUnavailableException
     *          If the local provider is not available.
     * @throws IngestException
     *          If any errors were encountered during ingest
     */
    void addLocalEntry(Metacard metacard) throws SourceUnavailableException, IngestException;

    /**
     *  Get a list of registry metacards
     *
     * @return List<Metacard>
     * @throws UnsupportedQueryException
     *          If runtime exception occured while performing the query.
     *          If the query fails validation.
     * @throws SourceUnavailableException
     *          If the local provider is not available.
     * @throws FederationException
     *          If any StopProcessing Exceptions were thrown by the catalog while processing the query
     */
    List<Metacard> getRegistryMetacards()
            throws UnsupportedQueryException, SourceUnavailableException, FederationException;
}
