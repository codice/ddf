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

        /* For the name, we construct a query searching for exactly the query string (the phrase
           query), a query searching for all the terms in the query string (the AND query), and a
           query searching for any of the terms in the query string (the OR query). We take the
           maximum of the scores generated by these three queries and use that as the score for the
           name. */

        // Surround with quotes so Lucene looks for the words in the query as a phrase.
        final Query phraseNameQuery = nameQueryParser.parse("\"" + queryString + "\"");
        // Phrase query gets the biggest boost - 3.2 was obtained after some experimentation.
        phraseNameQuery.setBoost(3.2f);

        // By default, QueryParser uses OR to separate terms.
        // We give OR queries the lowest boost because they're not as good as phrase matches or
        // AND matches - 1 (the default boost value) was obtained after some experimentation.
        final Query orNameQuery = nameQueryParser.parse(queryString);

        nameQueryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        final Query andNameQuery = nameQueryParser.parse(queryString);
        // We give AND queries the second-biggest boost because they're better than OR matches but
        // not as good as phrase matches - 2 was obtained after some experimentation.
        andNameQuery.setBoost(2f);

        final List<Query> nameQueryList = Arrays.asList(phraseNameQuery, orNameQuery, andNameQuery);
        // This query will score each document by the maximum of the three sub-queries.
        final Query nameQuery = new DisjunctionMaxQuery(nameQueryList, 0);

        final QueryParser alternateNamesQueryParser =
                new QueryParser(GeoNamesLuceneConstants.ALTERNATE_NAMES_FIELD, standardAnalyzer);

        // For the alternate names, we perform an AND query and an OR query, both of which are
        // boosted less than the name query because the alternate names are generally not as
        // important.
        final Query orAlternateNamesQuery = alternateNamesQueryParser.parse(queryString);
        // The OR query gets a lower boost - 0.5 was obtained after some experimentation.
        orAlternateNamesQuery.setBoost(0.5f);

        alternateNamesQueryParser.setDefaultOperator(QueryParser.AND_OPERATOR);
        // The AND query gets a higher boost - 1 (the default boost value) was obtained after some
        // experimentation.
        final Query andAlternateNamesQuery = alternateNamesQueryParser.parse(queryString);

        final List<Query> alternateNamesQueryList =
                Arrays.asList(orAlternateNamesQuery, andAlternateNamesQuery);
        // This query will score each document by the maximum of the two sub-queries.
        final Query alternateNamesQuery = new DisjunctionMaxQuery(alternateNamesQueryList, 0);

        final List<Query> queryList = Arrays.asList(nameQuery, alternateNamesQuery);

        // This query will score each document by the sum of the two sub-queries, since both the
        // name and the alternate names are important.
        // The boost values ensure that how well the query matches the name has a bigger impact on
        // the final score than how well it matches the alternate names.
        final DisjunctionMaxQuery disjunctionMaxQuery = new DisjunctionMaxQuery(queryList, 1.0f);

        // This is the boost we calculated at index time, and it is applied in the CustomScoreQuery.
        final FunctionQuery boostQuery =
                new FunctionQuery(new FloatFieldSource(GeoNamesLuceneConstants.BOOST_FIELD));

        return new CustomScoreQuery(disjunctionMaxQuery, boostQuery);
    }
}
