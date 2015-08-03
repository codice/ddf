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
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestGeoNamesLuceneIndexer extends TestBase {
    private static final String RESOURCE_PATH = new File(".").getAbsolutePath() +
            "/src/test/resources/geonames/";

    private static final String INDEX_PATH = RESOURCE_PATH + "index";

    private static final String NAME_1 = "Phoenix";
    private static final String NAME_2 = "Tempe";
    private static final String NAME_3 = "Glendale";

    private static final double LAT_1 = 1.234;
    private static final double LAT_2 = -12.34;
    private static final double LAT_3 = -1.234;

    private static final double LON_1 = 56.78;
    private static final double LON_2 = 5.678;
    private static final double LON_3 = -5.678;

    private static final String FEATURE_CODE_1 = "PPL";
    private static final String FEATURE_CODE_2 = "ADM";
    private static final String FEATURE_CODE_3 = "PCL";

    private static final long POP_1 = 1000000;
    private static final long POP_2 = 10000000;
    private static final long POP_3 = 100000000;

    private IndexWriter indexWriter;
    private GeoNamesLuceneIndexer geoNamesLuceneIndexer;
    private ArgumentCaptor<Document> documentArgumentCaptor;

    private static final GeoEntry GEO_ENTRY_1 = new GeoEntry.Builder()
            .name(NAME_1)
            .latitude(LAT_1)
            .longitude(LON_1)
            .featureCode(FEATURE_CODE_1)
            .population(POP_1)
            .build();

    private static final GeoEntry GEO_ENTRY_2 = new GeoEntry.Builder()
            .name(NAME_2)
            .latitude(LAT_2)
            .longitude(LON_2)
            .featureCode(FEATURE_CODE_2)
            .population(POP_2)
            .build();

    private static final GeoEntry GEO_ENTRY_3 = new GeoEntry.Builder()
            .name(NAME_3)
            .latitude(LAT_3)
            .longitude(LON_3)
            .featureCode(FEATURE_CODE_3)
            .population(POP_3)
            .build();

    private static final List<GeoEntry> GEO_ENTRY_LIST =
            Arrays.asList(GEO_ENTRY_1, GEO_ENTRY_2, GEO_ENTRY_3);

    @After
    public void tearDown() {
        // Delete the directory created by the indexer.
        FileUtils.deleteQuietly(new File(RESOURCE_PATH));
    }

    private void configureMocks() throws IOException {
        indexWriter = mock(IndexWriter.class);
        geoNamesLuceneIndexer = spy(new GeoNamesLuceneIndexer());
        doReturn(indexWriter)
                .when(geoNamesLuceneIndexer)
                .createIndexWriter(anyBoolean(), any(Directory.class));
        geoNamesLuceneIndexer.setIndexLocation(INDEX_PATH);
        documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
    }

    private GeoEntry createGeoEntryFromDocument(final Document document) {
        return new GeoEntry.Builder()
                .name(document.get("name"))
                .latitude(Double.parseDouble(document.get("latitude")))
                .longitude(Double.parseDouble(document.get("longitude")))
                .featureCode(document.get("feature_code"))
                .population(Long.parseLong(document.get("population")))
                .build();
    }

    private void verifyDocumentList(final List<Document> documentList) {
        assertEquals(3, documentList.size());

        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(0)), NAME_1, LAT_1, LON_1,
                FEATURE_CODE_1, POP_1);
        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(1)), NAME_2, LAT_2, LON_2,
                FEATURE_CODE_2, POP_2);
        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(2)), NAME_3, LAT_3, LON_3,
                FEATURE_CODE_3, POP_3);
    }

    @Test
    public void testCreateIndexFromList() throws IOException {
        configureMocks();

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, null);

        verify(indexWriter, times(GEO_ENTRY_LIST.size()))
                .addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);
    }

    @Test
    public void testCreateIndexFromListWithProgressUpdates() throws IOException {
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
    public void testCreateIndexFromExtractor() throws IOException {
        configureMocks();

        final ProgressCallback progressCallback = mock(ProgressCallback.class);

        geoNamesLuceneIndexer.updateIndex(null, new GeoEntryExtractor() {
                    @Override
                    public List<GeoEntry> getGeoEntries(final String resource,
                            final ProgressCallback progressCallback) {
                        return null;
                    }

                    @Override
                    public void getGeoEntriesStreaming(final String resource,
                            final ExtractionCallback extractionCallback) {
                        extractionCallback.extracted(GEO_ENTRY_1);
                        extractionCallback.extracted(GEO_ENTRY_2);
                        extractionCallback.extracted(GEO_ENTRY_3);
                        extractionCallback.updateProgress(100);
                    }
                },
                true,
                progressCallback);

        verify(indexWriter, times(3)).addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);

        verify(progressCallback, times(1)).updateProgress(100);
    }

    @Test
    public void testDirectoryCreatedForNewIndex() {
        assertFalse("The 'geonames/index' directory should not exist.",
                Files.exists(Paths.get(INDEX_PATH)));

        geoNamesLuceneIndexer = new GeoNamesLuceneIndexer();

        geoNamesLuceneIndexer.setIndexLocation(INDEX_PATH);

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, null);

        assertTrue(Files.exists(Paths.get(INDEX_PATH)));
    }
}
