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
package org.codice.ddf.spatial.geocoding;

import java.util.List;

/**
 * A {@code GeoEntryIndexer} provides methods for adding {@link GeoEntry} objects to a new or
 * existing local index.
 */
public interface GeoEntryIndexer {
  /**
   * Updates a GeoNames index with a {@link List} of {@link GeoEntry} objects.
   *
   * @param newEntries the {@code List} of {@code GeoEntry} objects to add to the index
   * @param create true will create a new index and false will add to the existing index
   * @param progressCallback the callback to receive updates about the indexing progress, may be
   *     null if you don't want any updates
   * @param entrySource the source for the GeoEntry data
   * @throws GeoEntryIndexingException if an error occurs while indexing the new entries
   */
  void updateIndex(
      List<GeoEntry> newEntries,
      boolean create,
      ProgressCallback progressCallback,
      String entrySource)
      throws GeoEntryIndexingException;

  /**
   * Updates a GeoNames index with {@link GeoEntry} objects extracted by a {@link
   * GeoEntryExtractor}.
   *
   * @param resource the resource containing GeoNames entries
   * @param geoEntryExtractor the {@code GeoEntryExtractor} that will extract {@code GeoEntry}
   *     objects from {@code resource}
   * @param create true will create a new index and false will add to the existing index
   * @param progressCallback the callback to receive updates about the indexing progress, may be
   *     null if you don't want any updates
   * @throws GeoEntryExtractionException if an error occurs while extracting GeoNames entries from
   *     the resource
   * @throws GeoEntryIndexingException if an error occurs while indexing the new entries
   * @throws GeoNamesRemoteDownloadException if an error occurs while downloading from a remote
   *     source
   */
  void updateIndex(
      String resource,
      GeoEntryExtractor geoEntryExtractor,
      boolean create,
      ProgressCallback progressCallback)
      throws GeoEntryIndexingException, GeoEntryExtractionException,
          GeoNamesRemoteDownloadException;
}
