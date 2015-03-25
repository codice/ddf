/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.solr;

import com.spatial4j.core.distance.DistanceUtils;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.measure.Distance;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SolrMetacardClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrMetacardClient.class);

    protected static final String RELEVANCE_SORT_FIELD = "score";

    private static final String QUOTE = "\"";

    private final SolrServer server;

    private final SolrFilterDelegateFactory filterDelegateFactory;

    private final FilterAdapter filterAdapter;

    private final DynamicSchemaResolver resolver;

    public SolrMetacardClient(SolrServer solrServer, FilterAdapter catalogFilterAdapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory, DynamicSchemaResolver
            dynamicSchemaResolver) {
        server = solrServer;
        filterDelegateFactory = solrFilterDelegateFactory;
        filterAdapter = catalogFilterAdapter;
        resolver = dynamicSchemaResolver;
    }

    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        if (request == null || request.getQuery() == null) {
            return new QueryResponseImpl(request, new ArrayList<Result>(), true, 0L);
        }

        SolrQuery query = getSolrQuery(request, filterDelegateFactory.newInstance(resolver));
        String sortProperty = getSortProperty(request, query);

        long totalHits;
        List<Result> results = new ArrayList<>();
        try {
            QueryResponse solrResponse = server.query(query, SolrRequest.METHOD.POST);
            totalHits = solrResponse.getResults().getNumFound();
            SolrDocumentList docs = solrResponse.getResults();

            for (SolrDocument doc : docs) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("SOLR DOC: {}", doc.getFieldValue(Metacard.ID + SchemaFields
                            .TEXT_SUFFIX));
                }
                ResultImpl tmpResult;
                try {
                    tmpResult = createResult(doc, sortProperty);
                    // TODO: register metacard type???
                } catch (MetacardCreationException e) {
                    LOGGER.warn("Metacard creation exception creating result", e);
                    throw new UnsupportedQueryException("Could not create metacard(s).");
                }

                results.add(tmpResult);
            }

        } catch (SolrServerException e) {
            LOGGER.warn("Failure in Solr server query.", e);
            throw new UnsupportedQueryException("Could not complete solr query.");
        } catch (SolrException e) {
            LOGGER.error("Could not complete solr query.", e);
            throw new UnsupportedQueryException("Could not complete solr query.");
        }

        SourceResponseImpl sourceResponseImpl = new SourceResponseImpl(request, results);

        /* Total Count */
        sourceResponseImpl.setHits(totalHits);

        return sourceResponseImpl;
    }

    protected SolrQuery getSolrQuery(QueryRequest request, SolrFilterDelegate solrFilterDelegate)
            throws UnsupportedQueryException {
        solrFilterDelegate.setSortPolicy(request.getQuery().getSortBy());
        SolrQuery query = filterAdapter.adapt(request.getQuery(), solrFilterDelegate);

        // Solr does not support outside parenthesis in certain queries and throws EOF exception.
        String queryPhrase = query.getQuery().trim();
        if (queryPhrase.matches("\\(\\s*\\{!.*\\)")) {
            query.setQuery(queryPhrase.replaceAll("^\\(\\s*|\\s*\\)$", ""));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Prepared Query: {}", query.getQuery());
            if (query.getFilterQueries() != null && query.getFilterQueries().length > 0) {
                LOGGER.debug("Filter Queries: {}", Arrays.toString(query.getFilterQueries()));
            }
        }

        if (request.getQuery().getPageSize() < 1) {
            query.setRows(Integer.MAX_VALUE);
        } else {
            query.setRows(request.getQuery().getPageSize());
        }

        /* Start Index */
        if (request.getQuery().getStartIndex() < 1) {
            throw new UnsupportedQueryException("Start index must be greater than 0");
        }

        // Solr is 0-based
        query.setStart(request.getQuery().getStartIndex() - 1);

        return query;
    }

    protected String getSortProperty(QueryRequest request, SolrQuery query) {
        SortBy sortBy = request.getQuery().getSortBy();
        String sortProperty = "";

        if (sortBy != null && sortBy.getPropertyName() != null) {
            sortProperty = sortBy.getPropertyName().getPropertyName();
            SolrQuery.ORDER order = SolrQuery.ORDER.desc;

            if (sortBy.getSortOrder() == SortOrder.ASCENDING) {
                order = SolrQuery.ORDER.asc;
            }

            if (Result.RELEVANCE.equals(sortProperty) || Result.DISTANCE.equals(sortProperty)) {
                query.setFields("*", RELEVANCE_SORT_FIELD);
                query.addSort(RELEVANCE_SORT_FIELD, order);
            } else if (sortProperty.equals(Result.TEMPORAL)) {
                query.addSort(
                        resolver.getField(Metacard.EFFECTIVE, AttributeType.AttributeFormat.DATE, false), order);
            } else {
                List<String> resolvedProperties = resolver.getAnonymousField(sortProperty);

                if (!resolvedProperties.isEmpty()) {
                    for (String sortField : resolvedProperties) {
                        query.addSort(sortField, order);
                    }

                    query.add("fl", "*," + RELEVANCE_SORT_FIELD);
                } else {
                    LOGGER.info(
                            "No schema field was found for sort property [{}]. No sort field was added to the query.",
                            sortProperty);
                }

            }

        }
        return sortProperty;
    }

    private ResultImpl createResult(SolrDocument doc, String sortProperty)
            throws MetacardCreationException {
        ResultImpl result = new ResultImpl(createMetacard(doc));

        if (doc.get(RELEVANCE_SORT_FIELD) != null) {
            if (Result.RELEVANCE.equals(sortProperty)) {
                result.setRelevanceScore(((Float) (doc.get(RELEVANCE_SORT_FIELD))).doubleValue());
            } else if (Result.DISTANCE.equals(sortProperty)) {
                Object distance = doc.getFieldValue(RELEVANCE_SORT_FIELD);

                if (distance != null) {
                    LOGGER.debug("Distance returned from Solr [{}]", distance);
                    double convertedDistance = degreesToMeters(Double.valueOf(distance.toString()));

                    LOGGER.debug("Converted distance into meters [{}]", convertedDistance);
                    result.setDistanceInMeters(convertedDistance);
                }
            }
        }

        return result;
    }

    private Double degreesToMeters(double distance) {
        return new Distance(
                DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM),
                Distance.LinearUnit.KILOMETER).getAs(Distance.LinearUnit.METER);
    }

    public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
        MetacardType metacardType = resolver.getMetacardType(doc);
        MetacardImpl metacard = new MetacardImpl(metacardType);

        for (String solrFieldName : doc.getFieldNames()) {
            if (!resolver.isPrivateField(solrFieldName)) {
                Serializable value = resolver.getDocValue(solrFieldName,
                        doc.getFieldValue(solrFieldName));
                metacard.setAttribute(resolver.resolveFieldName(solrFieldName), value);
            }
        }

        return metacard;
    }

    public List<SolrInputDocument> add(List<Metacard> metacards, boolean forceAutoCommit)
            throws IOException, SolrServerException, MetacardCreationException {
        if (metacards == null || metacards.size() == 0) {
            return null;
        }

        List<SolrInputDocument> docs = new ArrayList<>();
        for (Metacard metacard : metacards) {
            docs.add(getSolrInputDocument(metacard));
        }

        if (!forceAutoCommit) {
            server.add(docs);
        } else {
            softCommit(docs);
        }

        return docs;
    }

    protected SolrInputDocument getSolrInputDocument(Metacard metacard)
            throws MetacardCreationException {
        SolrInputDocument solrInputDocument = new SolrInputDocument();

        resolver.addFields(metacard, solrInputDocument);

        return solrInputDocument;
    }

    public void deleteByIds(String fieldName, List<? extends Serializable> identifiers,
            boolean forceAutoCommit) throws IOException, SolrServerException {
        if (identifiers == null || identifiers.size() == 0) {
            return;
        }

        server.deleteByQuery(getIdentifierQuery(fieldName, identifiers));

        if (forceAutoCommit) {
            server.commit();
        }
    }

    public String getIdentifierQuery(String fieldName, List<? extends Serializable> identifiers) {
        StringBuilder queryBuilder = new StringBuilder();
        for (Serializable id : identifiers) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" OR ");
            }

            queryBuilder.append(fieldName).append(":").append(QUOTE).append(id).append(QUOTE);
        }
        return queryBuilder.toString();
    }

    private org.apache.solr.client.solrj.response.UpdateResponse softCommit(
            List<SolrInputDocument> docs) throws SolrServerException, IOException {
        return new org.apache.solr.client.solrj.request.UpdateRequest().add(docs)
                .setAction(AbstractUpdateRequest.ACTION.COMMIT,
                        /* waitForFlush */ true,
                        /* waitToMakeVisible */ true,
                        /* softCommit */ true).process(server);
    }

}
