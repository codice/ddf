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

package org.codice.ddf.spatial.geocoding.index;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.store.Directory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.After;
import org.junit.Test;
import org.mockito.Matchers;

public class TestGeoNamesLuceneIndexerExceptions {
    private static final String ABSOLUTE_PATH = new File(".").getAbsolutePath();

    private static final String TEST_PATH = "/src/test/resources/geonames/";

    private GeoNamesLuceneIndexer geoNamesLuceneIndexer;

    private IndexWriter indexWriter;

    private static final GeoEntry GEO_ENTRY = new GeoEntry.Builder().name("name").latitude(1)
            .longitude(2).featureCode("code").population(3).alternateNames("").build();

    @After
    public void tearDown() {
        // Delete the directory created by the indexer.
        FileUtils.deleteQuietly(new File(ABSOLUTE_PATH + TEST_PATH));
    }

    private void configureMocks() throws GeoEntryIndexingException, IOException {
        indexWriter = mock(IndexWriter.class);
        doThrow(IOException.class).when(indexWriter)
                .addDocument(Matchers.<Iterable<IndexableField>>any());
        geoNamesLuceneIndexer = spy(new GeoNamesLuceneIndexer());
        doReturn(indexWriter).when(geoNamesLuceneIndexer)
                .createIndexWriter(anyBoolean(), any(Directory.class));
    }

    @Test
    public void testExceptionWhenAddingDocumentByExtractor()
            throws GeoEntryIndexingException, GeoEntryExtractionException,
            GeoNamesRemoteDownloadException, IOException {
        configureMocks();
        geoNamesLuceneIndexer.setIndexLocation(ABSOLUTE_PATH + TEST_PATH + "index");

        try {
            final GeoEntryExtractor geoEntryExtractor = new GeoEntryExtractor() {
                @Override
                public List<GeoEntry> getGeoEntries(final String resource,
                        final ProgressCallback progressCallback) {
                    return null;
                }

                @Override
                public void pushGeoEntriesToExtractionCallback(final String resource,
                        final ExtractionCallback extractionCallback)
                        throws GeoEntryExtractionException {
                    extractionCallback.updateProgress(0);
                    try {
                        extractionCallback.extracted(GEO_ENTRY);
                    } catch (GeoEntryIndexingException e) {
                        throw new GeoEntryExtractionException("Unable to add entry.", e);
                    }
                }
            };

            geoNamesLuceneIndexer.updateIndex("", geoEntryExtractor, true, null);
            fail("Should have thrown a GeoEntryExtractionException; extractionCallback.extract() threw "
                    + "a GeoEntryIndexingException because addDocument() threw an "
                    + "IOException.");
        } catch (GeoEntryExtractionException e) {
            assertThat(e.getCause(), instanceOf(GeoEntryIndexingException.class));
        }
    }

    @Test
    public void testExceptionWhenAddingDocumentByList()
            throws IOException, GeoEntryIndexingException {
        configureMocks();
        geoNamesLuceneIndexer.setIndexLocation(ABSOLUTE_PATH + TEST_PATH + "index");

        try {
            geoNamesLuceneIndexer.updateIndex(Collections.singletonList(GEO_ENTRY), true, null);
            fail("Should have thrown a GeoEntryIndexingException because addDocument() threw an "
                    + "IOException.");
        } catch (GeoEntryIndexingException e) {
            assertThat(e.getCause(), instanceOf(IOException.class));
            verify(indexWriter, times(1)).rollback();
        }
    }

    @Test
    public void testExceptionWhenAppendingToNonexistentIndex() {
        try {
            geoNamesLuceneIndexer = new GeoNamesLuceneIndexer();
            geoNamesLuceneIndexer.setIndexLocation(ABSOLUTE_PATH + TEST_PATH + "missing_index");
            geoNamesLuceneIndexer.updateIndex(null, false, null);
            fail("Should have thrown a GeoEntryIndexingException because 'missing_index' is not "
                    + "an existing Lucene index.");
        } catch (GeoEntryIndexingException e) {
            assertThat(e.getCause(), instanceOf(IOException.class));
        }
    }
}
