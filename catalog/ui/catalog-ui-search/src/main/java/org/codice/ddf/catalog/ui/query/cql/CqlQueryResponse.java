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
package org.codice.ddf.catalog.ui.query.cql;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.codice.ddf.catalog.ui.query.delegate.SearchTermsDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.ActionRegistry;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.UnsupportedQueryException;

public class CqlQueryResponse {

    private static final Logger LOGGER = LoggerFactory.getLogger(CqlQueryResponse.class);

    private static final SearchTermsDelegate SEARCH_TERMS_DELEGATE = new SearchTermsDelegate();

    private final List<CqlResult> results;

    private final String id;

    private final long hits;

    private final long count;

    private final long elapsed;

    private final String source;

    private final boolean successful;

    private final Map<String, Map<String, MetacardAttribute>> types;

    public CqlQueryResponse(String id, QueryResponse queryResponse, String source,
            long elapsedTime, FilterAdapter filterAdapter, ActionRegistry actionRegistry) {
        this.id = id;
        elapsed = elapsedTime;
        this.source = source;

        final Set<String> searchTerms = extractSearchTerms(queryResponse, filterAdapter);

        count = queryResponse.getResults()
                .size();
        hits = queryResponse.getHits();
        successful = isSuccessful(queryResponse.getProcessingDetails());
        types = queryResponse.getResults()
                .stream()
                .map(Result::getMetacard)
                .filter(Objects::nonNull)
                .map(Metacard::getMetacardType)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toMap(MetacardType::getName,
                        mt -> mt.getAttributeDescriptors()
                                .stream()
                                .collect(Collectors.toMap(AttributeDescriptor::getName,
                                        MetacardAttribute::new))));

        results = queryResponse.getResults()
                .stream()
                .map(result -> new CqlResult(result,
                        searchTerms,
                        queryResponse.getRequest(),
                        filterAdapter,
                        actionRegistry))
                .collect(Collectors.toList());

        Map<String, Long> docsContainingTerms = results.stream()
                .map(CqlResult::getMatches)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .collect(Collectors
                        .groupingBy(Function.identity(), Collectors.counting()));

        // TODO remove
        results.stream()
                .forEach(result -> {
                    Map<String, Integer> resultMatches = result.getMatches();
                    if (resultMatches != null) {
                        double tfidf = searchTerms.stream()
                                .map(term -> {
                                    String matchTerm = term.replace(".*", "%");
                                    double tf = resultMatches.getOrDefault(matchTerm, 0);
                                    double idf = Math.log10(((double) results.size()) / Math.max((double) docsContainingTerms.getOrDefault(
                                            matchTerm,
                                            0L), 0.000001));
                                    return tf * idf;
                                })
                                .collect(Collectors.summingDouble(Double::doubleValue));
                        result.setTestRelevance(tfidf);
                    }
                });
    }

    private Set<String> extractSearchTerms(QueryResponse queryResponse, FilterAdapter filterAdapter) {
        Set<String> searchTerms = Collections.emptySet();
        try {
            searchTerms = filterAdapter.adapt(queryResponse.getRequest().getQuery(),
                    SEARCH_TERMS_DELEGATE);
            LOGGER.info("Keywords: {}", String.join(", ", searchTerms)); // TODO remove
        } catch (UnsupportedQueryException e) {
            LOGGER.debug("Unable to parse search terms", e);
        }
        return searchTerms;
    }

    public List<CqlResult> getResults() {
        return results;
    }

    public long getHits() {
        return hits;
    }

    public long getElapsed() {
        return elapsed;
    }

    public String getSource() {
        return source;
    }

    public long getCount() {
        return count;
    }

    public boolean isSuccessful() {
        return successful;
    }

    private boolean isSuccessful(final Set<ProcessingDetails> details) {
        for (ProcessingDetails detail : details) {
            if (detail.hasException()) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Map<String, MetacardAttribute>> getTypes() {
        return types;
    }

    public String getId() {
        return id;
    }

}
