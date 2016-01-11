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

import static org.apache.lucene.index.IndexWriterConfig.OpenMode;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.TestBase;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneConstants;
import org.junit.Before;
import org.junit.Test;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.Shape;

public class TestGeoNamesQueryLuceneIndex extends TestBase {
    private Directory directory;

    private GeoNamesQueryLuceneDirectoryIndex directoryIndex;

    private static final String NAME_1 = "Phoenix";

    private static final String NAME_2 = "Phoenix Airport";

    private static final String NAME_3 = "Glendale";

    private static final double LAT_1 = 1.234;

    private static final double LAT_2 = 1.25;

    private static final double LAT_3 = 1;

    private static final double LON_1 = 56.78;

    private static final double LON_2 = 56.5;

    private static final double LON_3 = 57;

    private static final String FEATURE_CODE_1 = "PPL";

    private static final String FEATURE_CODE_2 = "AIRP";

    private static final String FEATURE_CODE_3 = "PPLC";

    private static final long POP_1 = 100000000;

    private static final long POP_2 = 10000000;

    private static final long POP_3 = 1000000;

    private static final String ALT_NAMES_1 = "alt1,alt2";

    private static final String ALT_NAMES_2 = "alt3";

    private static final String ALT_NAMES_3 = "";

    private static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;

    private SpatialStrategy strategy;

    private static final GeoEntry GEO_ENTRY_1 = new GeoEntry.Builder().name(NAME_1)
            .latitude(LAT_1)
            .longitude(LON_1)
            .featureCode(FEATURE_CODE_1)
            .population(POP_1)
            .alternateNames(ALT_NAMES_1)
            .build();

    private static final GeoEntry GEO_ENTRY_2 = new GeoEntry.Builder().name(NAME_2)
            .latitude(LAT_2)
            .longitude(LON_2)
            .featureCode(FEATURE_CODE_2)
            .population(POP_2)
            .alternateNames(ALT_NAMES_2)
            .build();

    private static final GeoEntry GEO_ENTRY_3 = new GeoEntry.Builder().name(NAME_3)
            .latitude(LAT_3)
            .longitude(LON_3)
            .featureCode(FEATURE_CODE_3)
            .population(POP_3)
            .alternateNames(ALT_NAMES_3)
            .build();

    private void initializeIndex() throws IOException {
        directory = new RAMDirectory();

        final IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
        indexWriterConfig.setOpenMode(OpenMode.CREATE);

        final IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);

        indexWriter.addDocument(createDocumentFromGeoEntry(GEO_ENTRY_1));
        indexWriter.addDocument(createDocumentFromGeoEntry(GEO_ENTRY_2));
        indexWriter.addDocument(createDocumentFromGeoEntry(GEO_ENTRY_3));

        indexWriter.close();
    }

    @Before
    public void setUp() throws IOException {
        directoryIndex = spy(new GeoNamesQueryLuceneDirectoryIndex());
        directoryIndex.setIndexLocation(null);

        final SpatialPrefixTree grid = new GeohashPrefixTree(SPATIAL_CONTEXT,
                GeoNamesLuceneConstants.GEOHASH_LEVELS);

        strategy = new RecursivePrefixTreeStrategy(grid, GeoNamesLuceneConstants.GEO_FIELD);

        initializeIndex();

        doReturn(directory).when(directoryIndex)
                .openDirectory();
    }

    private Document createDocumentFromGeoEntry(final GeoEntry geoEntry) {
        final Document document = new Document();

        document.add(new TextField(GeoNamesLuceneConstants.NAME_FIELD,
                geoEntry.getName(),
                Field.Store.YES));
        document.add(new DoubleField(GeoNamesLuceneConstants.LATITUDE_FIELD,
                geoEntry.getLatitude(),
                Field.Store.YES));
        document.add(new DoubleField(GeoNamesLuceneConstants.LONGITUDE_FIELD,
                        geoEntry.getLongitude(),
                        Field.Store.YES));
        document.add(new StringField(GeoNamesLuceneConstants.FEATURE_CODE_FIELD,
                geoEntry.getFeatureCode(),
                Field.Store.YES));
        document.add(new LongField(GeoNamesLuceneConstants.POPULATION_FIELD,
                        geoEntry.getPopulation(),
                        Field.Store.YES));
        document.add(new NumericDocValuesField(GeoNamesLuceneConstants.POPULATION_DOCVALUES_FIELD,
                geoEntry.getPopulation()));

        document.add(new TextField(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD,
                geoEntry.getAlternateNames(),
                Field.Store.NO));

        final Shape point = SPATIAL_CONTEXT.makePoint(geoEntry.getLongitude(),
                geoEntry.getLatitude());
        for (IndexableField field : strategy.createIndexableFields(point)) {
            document.add(field);
        }

        return document;
    }

    @Test
    public void testQueryWithExactlyMaxResults() throws IOException, ParseException {
        final int requestedMaxResults = 2;
        final String queryString = "phoenix";

        final List<GeoEntry> results = directoryIndex.query(queryString, requestedMaxResults);
        assertThat(results.size(), is(requestedMaxResults));

        final GeoEntry firstResult = results.get(0);
        // We don't store the alternate names, so we don't get them back with the query results.
        // The entry with the name "phoenix" will come first because the name matches the query
        // exactly.
        verifyGeoEntry(firstResult, NAME_1, LAT_1, LON_1, FEATURE_CODE_1, POP_1, null);

        final GeoEntry secondResult = results.get(1);
        // We don't store the alternate names, so we don't get them back with the query results.
        verifyGeoEntry(secondResult, NAME_2, LAT_2, LON_2, FEATURE_CODE_2, POP_2, null);
    }

    @Test
    public void testQueryWithLessThanMaxResults() throws IOException, ParseException {
        final int requestedMaxResults = 2;
        final int actualResults = 1;
        final String queryString = "glendale";

        final List<GeoEntry> results = directoryIndex.query(queryString, requestedMaxResults);
        assertThat(results.size(), is(actualResults));

        final GeoEntry firstResult = results.get(0);
        // We don't store the alternate names, so we don't get them back with the query results.
        verifyGeoEntry(firstResult, NAME_3, LAT_3, LON_3, FEATURE_CODE_3, POP_3, null);
    }

    @Test
    public void testQueryWithNoResults() throws IOException, ParseException {
        final int requestedMaxResults = 2;
        final int actualResults = 0;
        final String queryString = "another place";

        final List<GeoEntry> results = directoryIndex.query(queryString, requestedMaxResults);
        assertThat(results.size(), is(actualResults));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankQuery() {
        directoryIndex.query("", 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullQuery() {
        directoryIndex.query(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryZeroMaxResults() {
        directoryIndex.query("phoenix", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testQueryNegativeMaxResults() {
        directoryIndex.query("phoenix", -1);
    }

    @Test(expected = GeoEntryQueryException.class)
    public void testQueryNoExistingIndex() throws IOException {
        doReturn(false).when(directoryIndex)
                .indexExists(directory);
        directoryIndex.query("phoenix", 1);
    }

    @Test
    public void testExceptionInDirectoryCreation() throws IOException {
        doThrow(IOException.class).when(directoryIndex)
                .openDirectory();

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because an IOException was thrown "
                    + " when creating the directory.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by an IOException.",
                    e.getCause(),
                    instanceOf(IOException.class));
        }
    }

    @Test
    public void testExceptionInIndexReaderCreation() throws IOException {
        doThrow(IOException.class).when(directoryIndex)
                .createIndexReader(any(Directory.class));

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because an IOException was thrown "
                    + "when creating the IndexReader.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by an IOException.",
                    e.getCause(),
                    instanceOf(IOException.class));
        }
    }

    @Test
    public void testExceptionInQueryParsing() throws ParseException {
        doThrow(ParseException.class).when(directoryIndex)
                .createQuery(anyString());

        try {
            directoryIndex.query("phoenix", 1);
            fail("Should have thrown a GeoEntryQueryException because a ParseException was "
                    + "thrown when creating the Query.");
        } catch (GeoEntryQueryException e) {
            assertThat("The GeoEntryQueryException was not caused by a ParseException.",
                    e.getCause(),
                    instanceOf(ParseException.class));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNearestCitiesNullMetacard() throws java.text.ParseException {
        directoryIndex.getNearestCities(null, 1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNearestCitiesNegativeRadius() throws java.text.ParseException {
        directoryIndex.getNearestCities("POINT (56.78 1.5)", -1, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNearestCitiesNegativeMaxResults() throws java.text.ParseException {
        directoryIndex.getNearestCities("POINT (56.78 1.5)", 1, -1);
    }

    @Test
    public void testNearestCitiesWithMaxResults() throws java.text.ParseException {
        String testPoint = "POINT (56.78 1)";

        final int requestedMaxResults = 2;

        final List<NearbyLocation> nearestCities = directoryIndex.getNearestCities(testPoint,
                50,
                requestedMaxResults);
        assertThat(nearestCities.size(), is(requestedMaxResults));

        /* These distances values were obtained from
           http://www.movable-type.co.uk/scripts/latlong.html

           Phoenix is first because it has a higher population.
           Additionally, "Phoenix Airport" (GEO_ENTRY_2) is within 50 km of (56.78, 1), but it
           should not be included in the results because its feature code is AIRP (not a city).
        */
        final NearbyLocation first = nearestCities.get(0);
        assertThat(first.getCardinalDirection(), is("S"));

        final double firstDistance = first.getDistance();
        assertThat(String.format("%.2f", firstDistance), is("26.02"));

        assertThat(first.getName(), is("Phoenix"));

        final NearbyLocation second = nearestCities.get(1);
        assertThat(second.getCardinalDirection(), is("W"));

        final double secondDistance = second.getDistance();
        assertThat(String.format("%.2f", secondDistance), is("24.46"));

        assertThat(second.getName(), is("Glendale"));
    }

    @Test
    public void testNearestCitiesWithLessThanMaxResults() throws java.text.ParseException {
        String testPoint = "POINT (56.78 1.5)";

        final int requestedMaxResults = 2;
        final int actualResults = 1;

        final List<NearbyLocation> nearestCities = directoryIndex.getNearestCities(testPoint,
                50,
                requestedMaxResults);
        assertThat(nearestCities.size(), is(actualResults));

        /* This distance value was obtained from http://www.movable-type.co.uk/scripts/latlong.html

           Additionally, "Phoenix Airport" (GEO_ENTRY_2) is within 50 km of (56.78, 1.5), but it
           should not be included in the results because its feature code is AIRP (not a city).
        */
        final NearbyLocation first = nearestCities.get(0);
        assertThat(first.getCardinalDirection(), is("N"));

        final double distance = first.getDistance();
        assertThat(String.format("%.2f", distance), is("29.58"));

        assertThat(first.getName(), is("Phoenix"));
    }

    @Test
    public void testNearestCitiesWithNoResults() throws java.text.ParseException {
        String testPoint = "POINT (0 1)";

        final int requestedMaxResults = 2;
        final int actualResults = 0;

        final List<NearbyLocation> nearestCities = directoryIndex.getNearestCities(testPoint,
                50,
                requestedMaxResults);
        assertThat(nearestCities.size(), is(actualResults));
    }

    @Test(expected = java.text.ParseException.class)
    public void testNearestCitiesWithBadWKT() throws java.text.ParseException {
        String testPoint = "POINT 56.78 1.5)";

        final int requestedMaxResults = 2;

        final List<NearbyLocation> nearestCities = directoryIndex.getNearestCities(testPoint,
                50,
                requestedMaxResults);
    }

    @Test(expected = java.text.ParseException.class)
    public void testNearestCitiesWithBlankWKT() throws java.text.ParseException {
        String testPoint = "";

        final int requestedMaxResults = 2;
        final int actualResults = 0;

        final List<NearbyLocation> nearestCities = directoryIndex.getNearestCities(testPoint,
                50,
                requestedMaxResults);
        assertThat(nearestCities.size(), is(actualResults));
    }

    @Test(expected = GeoEntryQueryException.class)
    public void testNearestCitiesNoExistingIndex() throws IOException, java.text.ParseException {
        doReturn(false).when(directoryIndex)
                .indexExists(directory);
        directoryIndex.getNearestCities("POINT (56.78 1.5)", 5, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoGetNearestCitiesNullShape() {
        List<NearbyLocation> nearestCities = directoryIndex.doGetNearestCities(null,
                10,
                10,
                directory);
    }

    @Test(expected = GeoEntryQueryException.class)
    public void testDoGetNearestCitiesIOExceptionBranch() throws IOException {
        doThrow(IOException.class).when(directoryIndex)
                .createIndexReader(directory);
        Shape shape = mock(Shape.class);
        List<NearbyLocation> nearestCities = directoryIndex.doGetNearestCities(shape,
                10,
                10,
                directory);
    }
}
