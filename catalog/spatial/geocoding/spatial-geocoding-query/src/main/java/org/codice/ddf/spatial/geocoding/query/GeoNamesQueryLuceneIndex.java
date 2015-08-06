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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queries.function.FunctionQuery;
import org.apache.lucene.queries.function.valuesource.FloatFieldSource;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.index.GeoNamesLuceneConstants;

public abstract class GeoNamesQueryLuceneIndex implements GeoEntryQueryable {
    protected abstract Directory createDirectory() throws IOException;

    protected abstract IndexReader createIndexReader(Directory directory) throws IOException;

    protected abstract IndexSearcher createIndexSearcher(IndexReader indexReader);

    protected boolean indexExists(final Directory directory) throws IOException {
        return DirectoryReader.indexExists(directory);
    }

    protected List<GeoEntry> doQuery(final String queryString, final int maxResults,
            final Directory directory) {
        if (StringUtils.isBlank(queryString)) {
            throw new IllegalArgumentException("The query string cannot be null or empty.");
        }

        if (maxResults < 1) {
            throw new IllegalArgumentException("maxResults must be positive.");
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
                    results.add(new GeoEntry.Builder()
                            .name(document.get(GeoNamesLuceneConstants.NAME_FIELD))
                            .latitude(Double.parseDouble(
                                    document.get(GeoNamesLuceneConstants.LATITUDE_FIELD)))
                            .longitude(Double.parseDouble(
                                    document.get(GeoNamesLuceneConstants.LONGITUDE_FIELD)))
                            .featureCode(document.get(GeoNamesLuceneConstants.FEATURE_CODE_FIELD))
                            .population(Long.parseLong(
                                    document.get(GeoNamesLuceneConstants.POPULATION_FIELD)))
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

        final QueryParser nameQueryParser =
                new QueryParser(GeoNamesLuceneConstants.NAME_FIELD, standardAnalyzer);
        nameQueryParser.setEnablePositionIncrements(false);

        // Surround with quotes so Lucene looks for the words in the query as a phrase.
        final Query nameQuery = nameQueryParser.parse("\"" + queryString + "\"");
        // We settled on the value 3.2 after some experimentation. It provides a boost to query
        // matches on the name so that names matching the query will matter more to the overall
        // score than any alternate names matching the query. Additionally, it helps ensure that
        // places with names matching the query very well score higher than places with names that
        // don't match the query as well but may have bigger boosts from their population or feature
        // code.
        nameQuery.setBoost(3.2f);

        final QueryParser alternateNamesQueryParser =
                new QueryParser(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD, standardAnalyzer);
        final Query alternateNamesQuery = alternateNamesQueryParser.parse(queryString);

        final List<Query> queryList = Arrays.asList(nameQuery, alternateNamesQuery);

        final DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery(queryList, 1.0f);
        final FunctionQuery boostQuery =
                new FunctionQuery(new FloatFieldSource(GeoNamesLuceneConstants.BOOST_FIELD));

        return new CustomScoreQuery(disjunctionMaxQuery, boostQuery);
    }
}
