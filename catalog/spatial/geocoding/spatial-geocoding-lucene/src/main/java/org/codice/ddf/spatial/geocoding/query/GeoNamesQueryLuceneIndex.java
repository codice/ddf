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

package org.codice.ddf.spatial.geocoding.query;

import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.valuesource.FloatFieldSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
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
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.context.impl.NearbyLocationImpl;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneConstants;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.impl.PointImpl;

public abstract class GeoNamesQueryLuceneIndex implements GeoEntryQueryable {

    private static final SpatialContext SPATIAL_CONTEXT = SpatialContext.GEO;

    private static final Sort SORT =
            new Sort(new SortField(GeoNamesLuceneConstants.POPULATION_DOCVALUES_FIELD,
                    SortField.Type.LONG,
                    true /* sort descending */));

    // The GeoNames feature codes for cities, excluding cities that no longer exist or that have
    // been destroyed.
    private static final String[] CITY_FEATURE_CODES =
            {"PPL", "PPLA", "PPLA2", "PPLA3", "PPLA4", "PPLC", "PPLCH", "PPLF", "PPLG", "PPLL",
                    "PPLR", "PPLS", "PPLX"};

    private static final BooleanQuery PPL_QUERY;

    static {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        // Create an OR query on the feature_code field that will accept any of the above feature
        // codes.
        for (String fc : CITY_FEATURE_CODES) {
            builder.add(new TermQuery(new Term(GeoNamesLuceneConstants.FEATURE_CODE_FIELD, fc)),
                    BooleanClause.Occur.SHOULD);
        }
        PPL_QUERY = builder.build();
    }

    protected abstract Directory openDirectory() throws IOException;

    protected abstract IndexReader createIndexReader(Directory directory) throws IOException;

    protected abstract IndexSearcher createIndexSearcher(IndexReader indexReader);

    protected boolean indexExists(final Directory directory) throws IOException {
        return DirectoryReader.indexExists(directory);
    }

    protected List<GeoEntry> doQuery(final String queryString, final int maxResults,
            final Directory directory) throws GeoEntryQueryException {
        if (StringUtils.isBlank(queryString)) {
            throw new IllegalArgumentException("The query string cannot be null or empty.");
        }

        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be positive.");
        }

        if (directory == null) {
            return Collections.emptyList();
        }

        try (final IndexReader indexReader = createIndexReader(directory)) {
            final IndexSearcher indexSearcher = createIndexSearcher(indexReader);

            final Query query = createQuery(queryString);

            final TopDocs topDocs = indexSearcher.search(query, maxResults);
            if (topDocs.totalHits > 0) {
                final List<GeoEntry> results = new ArrayList<>();
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    final Document document = indexSearcher.doc(scoreDoc.doc);
                    // The alternate names aren't being stored (they are only used for queries),
                    // so we don't retrieve them here.
                    results.add(new GeoEntry.Builder().name(document.get(GeoNamesLuceneConstants.NAME_FIELD))
                            .latitude(Double.parseDouble(document.get(GeoNamesLuceneConstants.LATITUDE_FIELD)))
                            .longitude(Double.parseDouble(document.get(GeoNamesLuceneConstants.LONGITUDE_FIELD)))
                            .featureCode(document.get(GeoNamesLuceneConstants.FEATURE_CODE_FIELD))
                            .population(Long.parseLong(document.get(GeoNamesLuceneConstants.POPULATION_FIELD)))
                            .countryCode(document.get(GeoNamesLuceneConstants.COUNTRY_CODE_FIELD))
                            .build());
                }
                return results;
            } else {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new GeoEntryQueryException("Error reading the index", e);
        } catch (ParseException e) {
            throw new GeoEntryQueryException("Error parsing query", e);
        }
    }

    protected Query createQuery(final String queryString) throws ParseException {
        final StandardAnalyzer standardAnalyzer = new StandardAnalyzer();

        final QueryParser nameQueryParser = new QueryParser(GeoNamesLuceneConstants.NAME_FIELD,
                standardAnalyzer);
        nameQueryParser.setEnablePositionIncrements(false);

        /* For the name, we construct a query searching for exactly the query string (the phrase
        query), a query searching for all the terms in the query string (the AND query), and a
        query searching for any of the terms in the query string (the OR query). We take the
        maximum of the scores generated by these three queries and use that as the score for the
        name. */

        // Surround with quotes so Lucene looks for the words in the query as a phrase.
        // Phrase query gets the biggest boost - 3.2 was obtained after some experimentation.
        final Query phraseNameQuery = new BoostQuery(nameQueryParser.parse(
                "\"" + queryString + "\""), 3.2f);

        // By default, QueryParser uses OR to separate terms.
        // We give OR queries the lowest boost because they're not as good as phrase matches or
        // AND matches - 1 (the default boost value) was obtained after some experimentation.
        final Query orNameQuery = nameQueryParser.parse(queryString);

        nameQueryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        // We give AND queries the second-biggest boost because they're better than OR matches but
        // not as good as phrase matches - 2 was obtained after some experimentation.
        final Query andNameQuery = new BoostQuery(nameQueryParser.parse(queryString), 2f);

        final List<Query> nameQueryList = Arrays.asList(phraseNameQuery, orNameQuery, andNameQuery);
        // This query will score each document by the maximum of the three sub-queries.
        final Query nameQuery = new DisjunctionMaxQuery(nameQueryList, 0);

        final QueryParser alternateNamesQueryParser =
                new QueryParser(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD, standardAnalyzer);

        // For the alternate names, we perform an AND query and an OR query, both of which are
        // boosted less than the name query because the alternate names are generally not as
        // important.
        // The OR query gets a lower boost - 0.5 was obtained after some experimentation.
        final Query orAlternateNamesQuery = new BoostQuery(alternateNamesQueryParser.parse(
                queryString), 0.5f);

        alternateNamesQueryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        // The AND query gets a higher boost - 1 (the default boost value) was obtained after some
        // experimentation.
        final Query andAlternateNamesQuery = alternateNamesQueryParser.parse(queryString);

        final List<Query> alternateNamesQueryList = Arrays.asList(orAlternateNamesQuery,
                andAlternateNamesQuery);
        // This query will score each document by the maximum of the two sub-queries.
        final Query alternateNamesQuery = new DisjunctionMaxQuery(alternateNamesQueryList, 0);

        final List<Query> queryList = Arrays.asList(nameQuery, alternateNamesQuery);

        // This query will score each document by the sum of the two sub-queries, since both the
        // name and the alternate names are important.
        // The boost values ensure that how well the query matches the name has a bigger impact on
        // the final score than how well it matches the alternate names.
        final DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery(queryList, 1.0f);

        // This is the boost we calculated at index time, and it is applied in the CustomScoreQuery.
        final FunctionQuery boostQuery = new FunctionQuery(new FloatFieldSource(
                GeoNamesLuceneConstants.BOOST_FIELD));

        return new CustomScoreQuery(disjunctionMaxQuery, boostQuery);
    }

    protected List<NearbyLocation> doGetNearestCities(final Shape shape, final int radiusInKm,
            final int maxResults, final Directory directory) throws GeoEntryQueryException {

        notNull(shape,
                "GeoNamesQueryLuceneIndex.doGetNearestCities(): argument 'shape' may not be null.");

        if (radiusInKm <= 0) {
            throw new IllegalArgumentException(
                    "GeoNamesQueryLuceneIndex.doGetNearestCities(): radiusInKm must be positive.");
        }

        if (maxResults <= 0) {
            throw new IllegalArgumentException(
                    "GeoNamesQueryLuceneIndex.doGetNearestCities(): maxResults must be positive.");
        }

        if (directory == null) {
            return Collections.emptyList();
        }

        try (final IndexReader indexReader = createIndexReader(directory)) {
            final IndexSearcher indexSearcher = createIndexSearcher(indexReader);

            final List<NearbyLocation> closestCities = new ArrayList<>();

            final Point center = shape.getCenter();

            final Query filter = createSpatialQuery(center, radiusInKm);

            // Query for all the documents in the index that are cities, then filter those
            // results for the ones that are in the search area.
            final BooleanQuery booleanQuery = new BooleanQuery.Builder().add(PPL_QUERY,
                    BooleanClause.Occur.MUST)
                    .add(filter, BooleanClause.Occur.FILTER)
                    .build();

            final TopDocs topDocs = indexSearcher.search(booleanQuery, maxResults, SORT);

            if (topDocs.totalHits > 0) {
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    final double lat = Double.parseDouble(indexSearcher.doc(scoreDoc.doc)
                            .get(GeoNamesLuceneConstants.LATITUDE_FIELD));
                    final double lon = Double.parseDouble(indexSearcher.doc(scoreDoc.doc)
                            .get(GeoNamesLuceneConstants.LONGITUDE_FIELD));

                    final String name = indexSearcher.doc(scoreDoc.doc)
                            .get(GeoNamesLuceneConstants.NAME_FIELD);

                    final NearbyLocation city = new NearbyLocationImpl(center,
                            new PointImpl(lon, lat, SPATIAL_CONTEXT),
                            name);

                    closestCities.add(city);
                }
            }

            return closestCities;
        } catch (IOException e) {
            throw new GeoEntryQueryException("Error reading the index", e);
        }
    }

    protected String doGetCountryCode(Shape shape, int radiusInKm, Directory directory)
            throws GeoEntryQueryException {
        notNull(shape,
                "GeoNamesQueryLuceneIndex.doGetCountryCode(): argument 'shape' may not be null.");

        notNull(directory,
                "GeoNamesQueryLuceneIndex.doGetCountryCode(): argument 'directory' may not be null.");

        if (radiusInKm <= 0) {
            throw new IllegalArgumentException(
                    "GeoNamesQueryLuceneIndex.doGetCountryCode(): radiusInKm must be positive.");
        }

        try (final IndexReader indexReader = createIndexReader(directory)) {

            final IndexSearcher indexSearcher = createIndexSearcher(indexReader);

            final Point center = shape.getCenter();

            final Query filter = createSpatialQuery(center, radiusInKm);

            final BooleanQuery booleanQuery = new BooleanQuery.Builder().add(filter,
                    BooleanClause.Occur.FILTER)
                    .build();

            final TopDocs topDocs = indexSearcher.search(booleanQuery, 1, SORT);

            String countryCode = null;

            if (topDocs.totalHits > 0) {
                countryCode = indexSearcher.doc(topDocs.scoreDocs[0].doc)
                        .get(GeoNamesLuceneConstants.COUNTRY_CODE_FIELD);
            }

            return countryCode;

        } catch (IOException e) {
            throw new GeoEntryQueryException("Error reading the index", e);
        }
    }

    private Query createSpatialQuery(Point shapeCenter, int radiusInKm) {
        final SpatialPrefixTree grid = new GeohashPrefixTree(SPATIAL_CONTEXT,
                GeoNamesLuceneConstants.GEOHASH_LEVELS);

        final SpatialStrategy strategy = new RecursivePrefixTreeStrategy(grid,
                GeoNamesLuceneConstants.GEO_FIELD);

        // Create a spatial filter that will select the documents that are in the specified
        // search radius around the metacard's center.
        final double searchRadiusDegrees = radiusInKm * DistanceUtils.KM_TO_DEG;
        final SpatialArgs args = new SpatialArgs(SpatialOperation.Intersects,
                SPATIAL_CONTEXT.getShapeFactory()
                        .circle(shapeCenter, searchRadiusDegrees));

        return strategy.makeQuery(args);
    }
}
