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

package org.codice.ddf.spatial.geocoding;

import java.util.List;

/**
 * A {@code GeoEntryExtractor} provides methods for extracting {@link GeoEntry} objects from various
 * GeoNames resources.
 */
public interface GeoEntryExtractor {

    /**
     * Sets the url
     * @param url
     */
    void setUrl(String url);

    /**
     * Extracts GeoNames entries from a resource as {@link GeoEntry} objects, all at once, providing
     * updates about the extraction progress.
     *
     * @param resource         the resource containing GeoNames entries
     * @param progressCallback the callback to receive updates about the extraction progress, may
     *                         be null if you don't want any updates
     * @return the list of {@code GeoEntry} objects corresponding to the GeoNames entries in the
     *         resource
     * @throws GeoEntryExtractionException     if an error occurs while extracting GeoNames entries
     *                                         from the resource
     * @throws GeoNamesRemoteDownloadException if an error occurs while downloading from the resource
     */
    List<GeoEntry> getGeoEntries(String resource, ProgressCallback progressCallback)
            throws GeoEntryExtractionException, GeoNamesRemoteDownloadException;

    /**
     * Extracts GeoNames entries from a resource as {@link GeoEntry} objects and passes each
     * {@code GeoEntry} object through the callback {@code extractionCallback}. The callback is
     * called exactly once for each {@code GeoEntry} object extracted from the resource.
     * <p>
     * This method should be used instead of {@link #getGeoEntries(String, ProgressCallback)} if the
     * resource contains a very large number of entries.
     *
     * @param resource           the resource containing GeoNames entries
     * @param extractionCallback the callback that receives each extracted {@code GeoEntry} object,
     *                           must not be null
     * @throws IllegalArgumentException        if {@code extractionCallback} is null
     * @throws GeoEntryExtractionException     if an error occurs while extracting GeoNames entries from
     *                                         the resource
     * @throws GeoNamesRemoteDownloadException if an error occurs while downloading from the resource
     */
    void pushGeoEntriesToExtractionCallback(String resource, ExtractionCallback extractionCallback)
            throws GeoEntryExtractionException, GeoNamesRemoteDownloadException;

    /**
     * An {@code ExtractionCallback} provides a method for receiving a {@link GeoEntry} object that
     * has been extracted from a resource.
     */
    interface ExtractionCallback extends ProgressCallback {
        /**
         * Receives a {@link GeoEntry} object from
         * {@link #pushGeoEntriesToExtractionCallback(String, ExtractionCallback)}
         *
         * @param newEntry the {@code GeoEntry} object just extracted
         * @throws GeoEntryIndexingException if an error occurs adding a new entry to the index
         */
        void extracted(GeoEntry newEntry) throws GeoEntryIndexingException;
    }
}
