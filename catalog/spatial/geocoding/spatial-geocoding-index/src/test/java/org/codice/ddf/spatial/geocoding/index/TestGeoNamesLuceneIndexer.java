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

package org.codice.ddf.spatial.geocoding.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestGeoNamesLuceneIndexer extends TestBase {
    private static final String RESOURCE_PATH =
            new File(".").getAbsolutePath() + "/src/test/resources/geonames/";

    private static final String INDEX_PATH = RESOURCE_PATH + "index";

    private IndexWriter indexWriter;

    private GeoNamesLuceneIndexer geoNamesLuceneIndexer;

    private ArgumentCaptor<Document> documentArgumentCaptor;

    private static final String[] NAMES = {"Phoenix", "Tempe", "Glendale", "Mesa", "Tucson",
            "Flagstaff", "Scottsdale", "Gilbert", "Queen Creek"};

    private static final double[] LATS = {1.2, -3.4, 5.6, -7.8, 9.1, -2.3, 4.5, -6.7, 8.9};

    private static final double[] LONS = {-1.2, 3.4, -5.6, 7.8, -9.1, 2.3, -4.5, 6.7, -8.9};

    private static final String[] FEATURE_CODES = {"PPL", "ADM", "PCL", "ADM1", "ADM2", "ADM3",
            "PPLA", "PPLA2", "PPLC"};

    private static final long[] POPS = {0, 10, 100, 1000, 10000, 100000, 1000000, 10000000,
            100000000};

    private static final String[] ALT_NAMES = {"alt1, alt2", "alt3", "", "alt4", "alt5,alt6,alt7",
            "alt-8,alt-9", "alt-10", "alt 1.1, alt1.2", "alt2.1,alt3.4"};

    private static final GeoEntry GEO_ENTRY_1 = createGeoEntry(0);

    private static final GeoEntry GEO_ENTRY_2 = createGeoEntry(1);

    private static final GeoEntry GEO_ENTRY_3 = createGeoEntry(2);

    private static final GeoEntry GEO_ENTRY_4 = createGeoEntry(3);

    private static final GeoEntry GEO_ENTRY_5 = createGeoEntry(4);

    private static final GeoEntry GEO_ENTRY_6 = createGeoEntry(5);

    private static final GeoEntry GEO_ENTRY_7 = createGeoEntry(6);

    private static final GeoEntry GEO_ENTRY_8 = createGeoEntry(7);

    private static final GeoEntry GEO_ENTRY_9 = createGeoEntry(8);

    private static final List<GeoEntry> GEO_ENTRY_LIST = Arrays
            .asList(GEO_ENTRY_1, GEO_ENTRY_2, GEO_ENTRY_3, GEO_ENTRY_4, GEO_ENTRY_5, GEO_ENTRY_6,
                    GEO_ENTRY_7, GEO_ENTRY_8, GEO_ENTRY_9);

    private static GeoEntry createGeoEntry(final int index) {
        return new GeoEntry.Builder().name(NAMES[index]).latitude(LATS[index])
                .longitude(LONS[index]).featureCode(FEATURE_CODES[index]).population(POPS[index])
                .alternateNames(ALT_NAMES[index]).build();
    }

    @After
    public void tearDown() {
        // Delete the directory created by the indexer.
        FileUtils.deleteQuietly(new File(RESOURCE_PATH));
    }

    private void configureMocks() throws GeoEntryIndexingException, IOException {
        indexWriter = mock(IndexWriter.class);
        geoNamesLuceneIndexer = spy(new GeoNamesLuceneIndexer());
        doReturn(indexWriter).when(geoNamesLuceneIndexer)
                .createIndexWriter(anyBoolean(), any(Directory.class));
        geoNamesLuceneIndexer.setIndexLocation(INDEX_PATH);
        documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
    }

    private GeoEntry createGeoEntryFromDocument(final Document document) {
        return new GeoEntry.Builder().name(document.get(GeoNamesLuceneConstants.NAME_FIELD))
                .latitude(Double.parseDouble(document.get(GeoNamesLuceneConstants.LATITUDE_FIELD)))
                .longitude(
                        Double.parseDouble(document.get(GeoNamesLuceneConstants.LONGITUDE_FIELD)))
                .featureCode(document.get(GeoNamesLuceneConstants.FEATURE_CODE_FIELD))
                .population(Long.parseLong(document.get(GeoNamesLuceneConstants.POPULATION_FIELD)))
                .alternateNames(document.get(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD))
                .build();
    }

    private void verifyDocumentList(final List<Document> documentList) {
        assertEquals(GEO_ENTRY_LIST.size(), documentList.size());

        for (int i = 0; i < documentList.size(); ++i) {
            verifyGeoEntry(createGeoEntryFromDocument(documentList.get(i)), NAMES[i], LATS[i],
                    LONS[i], FEATURE_CODES[i], POPS[i], ALT_NAMES[i]);
        }
    }

    @Test
    public void testCreateIndexFromList() throws GeoEntryIndexingException, IOException {
        configureMocks();

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, null);

        verify(indexWriter, times(GEO_ENTRY_LIST.size()))
                .addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);
    }

    @Test
    public void testCreateIndexFromListWithProgressUpdates()
            throws GeoEntryIndexingException, IOException {
        configureMocks();

        final ProgressCallback progressCallback = mock(ProgressCallback.class);

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, progressCallback);

        verify(indexWriter, times(GEO_ENTRY_LIST.size()))
                .addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);

        verify(progressCallback, times(1)).updateProgress(0);
        verify(progressCallback, times(1)).updateProgress(100);
    }

    @Test
    public void testCreateIndexFromExtractor()
            throws IOException, GeoEntryIndexingException, GeoNamesRemoteDownloadException,
            GeoEntryExtractionException {
        configureMocks();

        final ProgressCallback progressCallback = mock(ProgressCallback.class);

        geoNamesLuceneIndexer.updateIndex(null, new GeoEntryExtractor() {
                    @Override
                    public List<GeoEntry> getGeoEntries(final String resource,
                            final ProgressCallback progressCallback) {
                        return null;
                    }

                    @Override
                    public void pushGeoEntriesToExtractionCallback(final String resource,
                            final ExtractionCallback extractionCallback) throws GeoEntryExtractionException {
                        try {
                            extractionCallback.extracted(GEO_ENTRY_1);
                            extractionCallback.extracted(GEO_ENTRY_2);
                            extractionCallback.extracted(GEO_ENTRY_3);
                            extractionCallback.extracted(GEO_ENTRY_4);
                            extractionCallback.extracted(GEO_ENTRY_5);
                            extractionCallback.extracted(GEO_ENTRY_6);
                            extractionCallback.extracted(GEO_ENTRY_7);
                            extractionCallback.extracted(GEO_ENTRY_8);
                            extractionCallback.extracted(GEO_ENTRY_9);
                            extractionCallback.updateProgress(100);
                        } catch(GeoEntryIndexingException e) {
                            throw new GeoEntryExtractionException("Unable to add entry.", e);
                        }
                    }
                }, true, progressCallback);

        verify(indexWriter, times(9)).addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);

        verify(progressCallback, times(1)).updateProgress(100);
    }

    @Test
    public void testDirectoryCreatedForNewIndex() throws GeoEntryIndexingException {
        assertFalse("The 'geonames/index' directory should not exist.",
                Files.exists(Paths.get(INDEX_PATH)));

        geoNamesLuceneIndexer = new GeoNamesLuceneIndexer();

        geoNamesLuceneIndexer.setIndexLocation(INDEX_PATH);

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, null);

        assertTrue(Files.exists(Paths.get(INDEX_PATH)));
    }
}
