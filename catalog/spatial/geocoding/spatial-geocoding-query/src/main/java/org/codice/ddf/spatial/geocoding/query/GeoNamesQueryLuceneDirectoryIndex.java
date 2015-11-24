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

package org.codice.ddf.spatial.geocoding.query;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneIndexer;

import ddf.catalog.data.Metacard;

public class GeoNamesQueryLuceneDirectoryIndex extends GeoNamesQueryLuceneIndex {
    private String indexLocation;

    public void setIndexLocation(final String indexLocation) {
        this.indexLocation = indexLocation;
    }

    @Override
    protected Directory openDirectory() throws IOException {
        return FSDirectory.open(Paths.get(indexLocation));
    }

    private Directory openDirectoryAndCheckForIndex() {
        Directory directory;

        try {
            directory = openDirectory();
            if (!indexExists(directory)) {
                directory.close();
                throw new GeoEntryQueryException("There is no index at " + indexLocation);
            }

            return directory;
        } catch (IOException e) {
            throw new GeoEntryQueryException(
                    "Error opening the index directory at " + indexLocation, e);
        }
    }

    @Override
    protected IndexReader createIndexReader(final Directory directory) throws IOException {
        return DirectoryReader.open(directory);
    }

    @Override
    protected IndexSearcher createIndexSearcher(final IndexReader indexReader) {
        final IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(GeoNamesLuceneIndexer.SIMILARITY);
        return indexSearcher;
    }

    @Override
    public List<GeoEntry> query(final String queryString, final int maxResults) {
        final Directory directory = openDirectoryAndCheckForIndex();

        return doQuery(queryString, maxResults, directory);
    }

    @Override
    public List<NearbyLocation> getNearestCities(final Metacard metacard, final int radiusInKm,
            final int maxResults) {
        final Directory directory = openDirectoryAndCheckForIndex();

        return doGetNearestCities(metacard, radiusInKm, maxResults, directory);
    }
}
