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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneConstants;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;

import ddf.catalog.data.Metacard;

public class GeoNamesQueryLuceneDirectoryIndex extends GeoNamesQueryLuceneIndex {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(GeoNamesQueryLuceneDirectoryIndex.class);

    private String indexLocation;

    public void setIndexLocation(final String indexLocation) {
        this.indexLocation = indexLocation;
    }

    private static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;

    // The GeoNames feature codes for cities, excluding cities that no longer exist or that have
    // been destroyed.
    private static final String[] CITY_FEATURE_CODES = {"PPL", "PPLA", "PPLA2", "PPLA3", "PPLA4",
            "PPLC", "PPLCH", "PPLF", "PPLG", "PPLL", "PPLR", "PPLS", "PPLX"};

    private static final BooleanQuery PPL_QUERY = new BooleanQuery();

    static {
        // Create an OR query on the feature_code field that will accept any of the above feature
        // codes.
        for (String fc : CITY_FEATURE_CODES) {
            PPL_QUERY.add(new TermQuery(new Term(GeoNamesLuceneConstants.FEATURE_CODE_FIELD, fc)),
                    BooleanClause.Occur.SHOULD);
        }
    }

    private static final Sort SORT = new Sort(
            new SortField(GeoNamesLuceneConstants.POPULATION_DOCVALUES_FIELD, SortField.Type.LONG,
                    true /* sort descending */));

    @Override
    protected Directory createDirectory() throws IOException {
        return FSDirectory.open(Paths.get(indexLocation));
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
        Directory directory;

        try {
            directory = createDirectory();
            if (!indexExists(directory)) {
                throw new GeoEntryQueryException("There is no index at " + indexLocation);
            }
        } catch (IOException e) {
            throw new GeoEntryQueryException("Error opening the index directory.", e);
        }

        return doQuery(queryString, maxResults, directory);
    }

    /**
     * Calculates the bearing from the first point to the second point (i.e. the <em>initial
     * bearing</em>) in degrees.
     *
     * @param lat1  the latitude of the first point, in degrees
     * @param lon1  the longitude of the first point, in degrees
     * @param lat2  the latitude of the second point, in degrees
     * @param lon2  the longitude of the second point, in degrees
     * @return the bearing from the first point to the second point, in degrees
     */
    private static double getBearing(final double lat1, final double lon1, final double lat2,
            final double lon2) {
        final double lonDiffRads = Math.toRadians(lon2 - lon1);
        final double lat1Rads = Math.toRadians(lat1);
        final double lat2Rads = Math.toRadians(lat2);
        final double y = Math.sin(lonDiffRads) * Math.cos(lat2Rads);
        final double x =
                Math.cos(lat1Rads) * Math.sin(lat2Rads) - Math.sin(lat1Rads) * Math.cos(lat2Rads)
                        * Math.cos(lonDiffRads);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    /**
     * Takes a bearing in degrees and returns the corresponding cardinal direction as a string.
     *
     * @param bearing  the bearing, in degrees, in the range [0, 360)
     * @return the cardinal direction corresponding to {@code bearing} (N, NE, E, SE, S, SW, W, NW)
     * @throws IllegalArgumentException if {@code bearing} is not in the range [0, 360)
     */
    private static String bearingToCardinalDirection(final double bearing) {
        if (bearing < 0 || bearing >= 360) {
            throw new IllegalArgumentException(bearing + " is not in the range [0, 360)");
        }

        final String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[(int) Math.round(bearing / 45)];
    }

    @Override
    public List<String> getNearestCities(final Metacard metacard, final int radiusInKm,
            final int maxResults) {
        if (metacard == null) {
            throw new IllegalArgumentException("metacard must not be null.");
        }

        if (radiusInKm <= 0) {
            throw new IllegalArgumentException("radiusInKm must be positive.");
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException("maxResults must be positive.");
        }

        Directory directory;

        try {
            directory = createDirectory();
            if (!indexExists(directory)) {
                throw new GeoEntryQueryException("There is no index at " + indexLocation);
            }
        } catch (IOException e) {
            throw new GeoEntryQueryException(
                    "Error opening the index directory at " + indexLocation, e);
        }

        try (final IndexReader indexReader = createIndexReader(directory)) {
            final IndexSearcher indexSearcher = createIndexSearcher(indexReader);

            final String location = metacard.getLocation();
            Shape shape = null;

            try {
                if (StringUtils.isNotBlank(location)) {
                    shape = SPATIAL_CONTEXT.readShapeFromWkt(location);
                }
            } catch (ParseException e) {
                LOGGER.warn("Couldn't parse WKT: {}", location, e);
            }

            final List<String> closestCities = new ArrayList<>();

            if (shape != null) {
                final SpatialPrefixTree grid = new GeohashPrefixTree(SPATIAL_CONTEXT,
                        GeoNamesLuceneConstants.GEOHASH_LEVELS);

                final SpatialStrategy strategy = new RecursivePrefixTreeStrategy(grid,
                        GeoNamesLuceneConstants.GEO_FIELD);

                // Create a spatial filter that will select the documents that are in the specified
                // search radius around the metacard's center.
                final Point center = shape.getCenter();
                final double searchRadiusDegrees = radiusInKm * DistanceUtils.KM_TO_DEG;
                final SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects,
                        SPATIAL_CONTEXT.makeCircle(center, searchRadiusDegrees));
                final Filter filter = strategy.makeFilter(args);

                // Query for all the documents in the index that are cities, then filter those
                // results for the ones that are in the search area.
                final BooleanQuery booleanQuery = new BooleanQuery();
                booleanQuery.add(PPL_QUERY, BooleanClause.Occur.MUST);
                booleanQuery.add(filter, BooleanClause.Occur.FILTER);

                final TopDocs topDocs = indexSearcher.search(booleanQuery, maxResults, SORT);

                if (topDocs.totalHits > 0) {
                    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                        final double lat = Double.parseDouble(indexSearcher.doc(scoreDoc.doc)
                                .get(GeoNamesLuceneConstants.LATITUDE_FIELD));
                        final double lon = Double.parseDouble(indexSearcher.doc(scoreDoc.doc)
                                .get(GeoNamesLuceneConstants.LONGITUDE_FIELD));

                        final double dist = SPATIAL_CONTEXT.calcDistance(center, lon, lat)
                                * DistanceUtils.DEG_TO_KM;

                        final double bearing = getBearing(lat, lon, center.getY(), center.getX());

                        final String cardinalDirection = bearingToCardinalDirection(bearing);

                        final String name = indexSearcher.doc(scoreDoc.doc)
                                .get(GeoNamesLuceneConstants.NAME_FIELD);

                        final String cityContext = String
                                .format("%.2f km %s of %s", dist, cardinalDirection, name);

                        closestCities.add(cityContext);
                    }
                }
            }

            return closestCities;
        } catch (IOException e) {
            throw new GeoEntryQueryException("Error reading the index", e);
        }
    }
}
