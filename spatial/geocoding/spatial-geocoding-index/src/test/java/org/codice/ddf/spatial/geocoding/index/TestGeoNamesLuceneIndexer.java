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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class TestGeoNamesLuceneIndexer extends TestBase {
    private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

    private static final String TEST_PATH = "/src/test/resources/geonames/";

    private IndexWriter indexWriter;
    private GeoNamesLuceneIndexer geoNamesLuceneIndexer;
    private ArgumentCaptor<Document> documentArgumentCaptor;

    private static final GeoEntry GEO_ENTRY_1 = new GeoEntry.Builder()
            .name("Phoenix")
            .latitude(1.234)
            .longitude(56.78)
            .featureCode("PPL")
            .population(1000000)
            .build();

    private static final GeoEntry GEO_ENTRY_2 = new GeoEntry.Builder()
            .name("Tempe")
            .latitude(-12.34)
            .longitude(5.678)
            .featureCode("PPL")
            .population(10000000)
            .build();

    private static final GeoEntry GEO_ENTRY_3 = new GeoEntry.Builder()
            .name("Glendale")
            .latitude(-1.234)
            .longitude(-5.678)
            .featureCode("PPL")
            .population(100000000)
            .build();

    private static final List<GeoEntry> GEO_ENTRY_LIST =
            Arrays.asList(GEO_ENTRY_1, GEO_ENTRY_2, GEO_ENTRY_3);

    @Before
    public void setUp() throws IOException {
        indexWriter = mock(IndexWriter.class);
        geoNamesLuceneIndexer = spy(new GeoNamesLuceneIndexer());
        doReturn(indexWriter)
                .when(geoNamesLuceneIndexer)
                .createIndexWriter(anyBoolean(), any(Directory.class));
        geoNamesLuceneIndexer.setIndexLocation(ABSOLUTE_PATH + TEST_PATH + "index");
        documentArgumentCaptor = ArgumentCaptor.forClass(Document.class);
    }

    @After
    public void tearDown() {
        // Delete the directory created by the indexer.
        FileUtils.deleteQuietly(new File(ABSOLUTE_PATH + TEST_PATH));
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

        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(0)), "Phoenix", 1.234, 56.78,
                "PPL", 1000000);
        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(1)), "Tempe", -12.34, 5.678,
                "PPL", 10000000);
        verifyGeoEntry(createGeoEntryFromDocument(documentList.get(2)), "Glendale", -1.234, -5.678,
                "PPL", 100000000);
    }

    @Test
    public void testCreateIndexFromList() throws IOException {
        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, null);

        verify(indexWriter, times(3)).addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);
    }

    @Test
    public void testCreateIndexFromListWithProgressUpdates() throws IOException {
        final ProgressCallback progressCallback = mock(ProgressCallback.class);

        geoNamesLuceneIndexer.updateIndex(GEO_ENTRY_LIST, true, progressCallback);

        verify(indexWriter, times(3)).addDocument(documentArgumentCaptor.capture());

        final List<Document> documentList = documentArgumentCaptor.getAllValues();

        verifyDocumentList(documentList);

        verify(progressCallback, times(1)).updateProgress(0);
        verify(progressCallback, times(1)).updateProgress(100);
    }

    @Test
    public void testCreateIndexFromExtractor() throws IOException {
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
}
