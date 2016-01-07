/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.admin.module.service;

public interface GeocodingMBean {

    /**
     * Calls {@link org.codice.ddf.spatial.geocoding.GeoEntryIndexer#updateIndex}
     * with the given URL and country code.
     * @param url - the URL of the GeoNames server
     * @param createIndex - true if the index is to be created, false if the index is to be appended to
     *
     * @return true on successful execution, false when an error occurs in the GeoEntryIndexer
     */
    boolean updateGeoIndexWithUrl(String url, String createIndex);

    /**
     * Calls {@link org.codice.ddf.spatial.geocoding.GeoEntryIndexer#updateIndex}
     * with the given path and contentFolderId in the /content/store.
     *
     * @param fileName - the filename to be added to the GeoEntry index
     * @param contentFolderId - the path to the fileName within /content/store
     * @param createIndex - true if the index is to be created, false if the index is to be appended to
     *
     * @return true on successful execution, false when an error occurs in the GeoEntryIndexer
     */
    boolean updateGeoIndexWithFilePath(String fileName, String contentFolderId, String createIndex);

    /**
     * Returns the value of the progressCallback through Jolokia.
     * *
     * @return The current value of the progressCallback in the {@link org.codice.ddf.spatial.geocoding.GeoEntryExtractor}
     */
    int progressCallback();

}
