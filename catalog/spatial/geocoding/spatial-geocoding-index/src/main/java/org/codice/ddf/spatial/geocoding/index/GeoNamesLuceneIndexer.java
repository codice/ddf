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
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
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

    public static final DefaultSimilarity SIMILARITY = new DefaultSimilarity() {
        @Override
        public float lengthNorm(final FieldInvertState fieldInvertState) {
            if (fieldInvertState.getName().equals(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD)) {
                return 1.0f;
            } else {
                return super.lengthNorm(fieldInvertState) / (float) Math.sqrt(fieldInvertState.getLength());
            }
        }
    };

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
            throw new GeoEntryIndexingException(
                    "Couldn't open the directory for the index, " + indexLocation, e);
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
        indexWriterConfig.setSimilarity(SIMILARITY);
        return new IndexWriter(directory, indexWriterConfig);
    }

    private void buildIndex(final List<GeoEntry> geoEntryList, final boolean create,
            final ProgressCallback progressCallback) {
        Directory directory;

        try {
            directory = FSDirectory.open(Paths.get(indexLocation));
        } catch (IOException e) {
            throw new GeoEntryIndexingException(
                    "Couldn't open the directory for the index, " + indexLocation, e);
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
        document.add(new TextField(GeoNamesLuceneConstants.NAME_FIELD, geoEntry.getName(),
                Field.Store.YES));
        document.add(new DoubleField(GeoNamesLuceneConstants.LATITUDE_FIELD, geoEntry.getLatitude(),
                Field.Store.YES));
        document.add(new DoubleField(GeoNamesLuceneConstants.LONGITUDE_FIELD,
                geoEntry.getLongitude(), Field.Store.YES));
        document.add(new StoredField(GeoNamesLuceneConstants.FEATURE_CODE_FIELD,
                geoEntry.getFeatureCode()));
        document.add(new StoredField(GeoNamesLuceneConstants.POPULATION_FIELD,
                geoEntry.getPopulation()));
        document.add(new TextField(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD,
                geoEntry.getAlternateNames(), Field.Store.NO));

        final float boost = calculateBoost(geoEntry);
        document.add(new FloatDocValuesField(GeoNamesLuceneConstants.BOOST_FIELD, boost));

        indexWriter.addDocument(document);
    }

    private float calculateBoost(final GeoEntry geoEntry) {
        final String featureCode = geoEntry.getFeatureCode();

        float boost = 1.0f;

        if (featureCode.startsWith(GeoCodingConstants.ADMINISTRATIVE_DIVISION)) {
            boost += 0.4f;
            if (featureCode.endsWith(GeoCodingConstants.DIVISION_FIRST_ORDER)) {
                boost += 1.2f;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_SECOND_ORDER)) {
                boost += 0.6f;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_THIRD_ORDER)) {
                boost += 0.4f;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_FOURTH_ORDER)) {
                boost += 0.2f;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_FIFTH_ORDER)) {
                boost += 0.0f;
            }
        } else if (featureCode.startsWith(GeoCodingConstants.POLITICAL_ENTITY)) {
            boost += 2.5f;
        } else if (featureCode.startsWith(GeoCodingConstants.POPULATED_PLACE)) {
            boost += 1.5f;
            if (featureCode.endsWith(GeoCodingConstants.SEAT_FIRST_ORDER)) {
                boost += 0.5f;
            } else if (featureCode.endsWith(GeoCodingConstants.SEAT_SECOND_ORDER)) {
                boost += 0.4f;
            } else if (featureCode.endsWith(GeoCodingConstants.SEAT_THIRD_ORDER)) {
                boost += 0.3f;
            } else if (featureCode.endsWith(GeoCodingConstants.SEAT_FOURTH_ORDER)) {
                boost += 0.2f;
            } else if (featureCode.endsWith(GeoCodingConstants.CAPITAL)) {
                boost += 0.8f;
            }
        }

        final long population = geoEntry.getPopulation();
        // Populated places get a small initial boost.
        boost += population > 0 ? 0.1f : 0;
        // A population of 25,000,000 or more will give the max population boost.
        boost += Math.min(population / 25000000.0f, 1.0f) * 5.0f;
        return boost;
    }
}
