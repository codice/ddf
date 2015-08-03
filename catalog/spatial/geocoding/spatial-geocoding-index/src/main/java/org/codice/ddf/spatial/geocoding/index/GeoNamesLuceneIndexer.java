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
 **/

package org.codice.ddf.spatial.geocoding.index;

import static org.apache.lucene.index.IndexWriterConfig.OpenMode;
import static org.codice.ddf.spatial.geocoding.GeoEntryExtractor.ExtractionCallback;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;

public class GeoNamesLuceneIndexer implements GeoEntryIndexer {
    private static final Analyzer ANALYZER = new StandardAnalyzer();

    private String indexLocation;

    public void setIndexLocation(final String indexLocation) {
        this.indexLocation = indexLocation;
    }

    @Override
    public void updateIndex(final List<GeoEntry> geoEntryList, final boolean create,
            final ProgressCallback progressCallback) {
        buildIndex(geoEntryList, create, progressCallback);
    }

    @Override
    public void updateIndex(final String resource, final GeoEntryExtractor geoEntryExtractor,
            final boolean create, final ProgressCallback progressCallback) {
        Directory directory;

        try {
            directory = FSDirectory.open(Paths.get(indexLocation));
        } catch (IOException e) {
            throw new GeoEntryIndexingException("Couldn't open the directory for the index, " +
                    indexLocation, e);
        }

        // Try-with-resources to ensure the IndexWriter always gets closed.
        try (final IndexWriter indexWriter = createIndexWriter(create, directory)) {
            final ExtractionCallback extractionCallback = new ExtractionCallback() {
                @Override
                public void extracted(final GeoEntry newEntry) {
                    try {
                        addDocument(indexWriter, newEntry);
                    } catch (IOException e) {
                        throw new GeoEntryIndexingException("Error writing to the index.", e);
                    }
                }

                @Override
                public void updateProgress(final int progress) {
                    if (progressCallback != null) {
                        progressCallback.updateProgress(progress);
                    }
                }
            };

            try {
                geoEntryExtractor.getGeoEntriesStreaming(resource, extractionCallback);
            } catch (GeoEntryExtractionException | GeoEntryIndexingException e) {
                // Need to roll back here before the IndexWriter is closed at the end of the try
                // block.
                indexWriter.rollback();
                throw e;
            }
        } catch (IOException e) {
            throw new GeoEntryIndexingException("Error writing to the index.", e);
        }
    }

    IndexWriter createIndexWriter(final boolean create, final Directory directory)
            throws IOException {
        final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(ANALYZER);
        indexWriterConfig.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND);
        return new IndexWriter(directory, indexWriterConfig);
    }

    private void buildIndex(final List<GeoEntry> geoEntryList, final boolean create,
            final ProgressCallback progressCallback) {
        Directory directory;

        try {
            directory = FSDirectory.open(Paths.get(indexLocation));
        } catch (IOException e) {
            throw new GeoEntryIndexingException("Couldn't open the directory for the index, " +
                    indexLocation, e);
        }

        // Try-with-resources to ensure the IndexWriter always gets closed.
        try (final IndexWriter indexWriter = createIndexWriter(create, directory)) {
            try {
                indexGeoEntries(indexWriter, geoEntryList, progressCallback);
            } catch (IOException e) {
                // Need to roll back here before the IndexWriter is closed at the end of the try
                // block.
                indexWriter.rollback();
                throw e;
            }
        } catch (IOException e) {
            throw new GeoEntryIndexingException("Error writing to the index.", e);
        }
    }

    private void indexGeoEntries(final IndexWriter indexWriter, final List<GeoEntry> geoEntryList,
            final ProgressCallback progressCallback) throws IOException {
        int progress = 0;
        int currentEntry = 0;
        final int numGeoEntries = geoEntryList.size();
        for (GeoEntry geoEntry : geoEntryList) {
            addDocument(indexWriter, geoEntry);
            if (currentEntry == (int) (numGeoEntries * (progress / 100.0f))) {
                if (progressCallback != null) {
                    progressCallback.updateProgress(progress);
                }
                progress += 5;
            }

            ++currentEntry;
        }
        // Since we start counting the GeoEntries at 0, the progress callback won't be called in the
        // above loop when progress is 100. In any case, we need to give a progress update when
        // the work is complete.
        if (progressCallback != null) {
            progressCallback.updateProgress(100);
        }
    }

    private void addDocument(final IndexWriter indexWriter, final GeoEntry geoEntry)
            throws IOException {
        final Document document = new Document();
        document.add(new TextField("name", geoEntry.getName(), Field.Store.YES));
        document.add(new DoubleField("latitude", geoEntry.getLatitude(), Field.Store.YES));
        document.add(new DoubleField("longitude", geoEntry.getLongitude(), Field.Store.YES));
        document.add(new TextField("feature_code", geoEntry.getFeatureCode(), Field.Store.YES));
        document.add(new LongField("population", geoEntry.getPopulation(), Field.Store.YES));
        indexWriter.addDocument(document);
    }
}
