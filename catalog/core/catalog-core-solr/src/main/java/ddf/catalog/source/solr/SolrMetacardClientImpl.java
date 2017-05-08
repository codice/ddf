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
package ddf.catalog.source.solr;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.measure.Distance;

public class SolrMetacardClientImpl implements SolrMetacardClient {

    protected static final String RELEVANCE_SORT_FIELD = "score";

    private static final String DISTANCE_SORT_FUNCTION = "geodist()";

    private static final String DISTANCE_SORT_FIELD = "_distance_";

    private static final String GEOMETRY_SORT_FIELD =
            Metacard.GEOGRAPHY + SchemaFields.GEO_SUFFIX + SchemaFields.SORT_KEY_SUFFIX;

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrMetacardClientImpl.class);

    private static final String QUOTE = "\"";

    public static final String SORT_FIELD_KEY = "sfield";

    public static final String POINT_KEY = "pt";

    private final SolrClient client;

    private final SolrFilterDelegateFactory filterDelegateFactory;

    private final FilterAdapter filterAdapter;

    private final DynamicSchemaResolver resolver;

    public SolrMetacardClientImpl(SolrClient client, FilterAdapter catalogFilterAdapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory,
            DynamicSchemaResolver dynamicSchemaResolver) {
        this.client = client;
        filterDelegateFactory = solrFilterDelegateFactory;
        filterAdapter = catalogFilterAdapter;
        resolver = dynamicSchemaResolver;
    }

    public SolrClient getClient() {
        return client;
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        if (request == null || request.getQuery() == null) {
            return new QueryResponseImpl(request, new ArrayList<Result>(), true, 0L);
        }

        SolrQuery query = getSolrQuery(request, filterDelegateFactory.newInstance(resolver));

        long totalHits;
        List<Result> results = new ArrayList<>();
        try {
            QueryResponse solrResponse = client.query(query, SolrRequest.METHOD.POST);
            totalHits = solrResponse.getResults()
                    .getNumFound();
            SolrDocumentList docs = solrResponse.getResults();

            for (SolrDocument doc : docs) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("SOLR DOC: {}",
                            doc.getFieldValue(Metacard.ID + SchemaFields.TEXT_SUFFIX));
                }
                ResultImpl tmpResult;
                try {
                    tmpResult = createResult(doc);
                } catch (MetacardCreationException e) {
                    throw new UnsupportedQueryException("Could not create metacard(s).", e);
                }

                results.add(tmpResult);
            }

        } catch (SolrServerException | IOException | SolrException e) {
            throw new UnsupportedQueryException("Could not complete solr query.", e);
        }

        SourceResponse sourceResponse = new SourceResponseImpl(request, results, totalHits);

        return sourceResponse;
    }

    @Override
    public List<Metacard> query(String queryString) throws UnsupportedQueryException {
        SolrQuery query = new SolrQuery();
        query.setQuery(queryString);
        try {
            QueryResponse solrResponse = client.query(query, SolrRequest.METHOD.POST);
            SolrDocumentList docs = solrResponse.getResults();

            List<Metacard> results = new ArrayList<>();
            for (SolrDocument doc : docs) {
                try {
                    results.add(createMetacard(doc));
                } catch (MetacardCreationException e) {
                    throw new UnsupportedQueryException("Could not create metacard(s).", e);
                }
            }

            return results;
        } catch (SolrServerException | IOException e) {
            throw new UnsupportedQueryException("Could not complete solr query.", e);
        }

    }

    @Override
    public Set<ContentType> getContentTypes() {
        Set<ContentType> finalSet = new HashSet<>();

        String contentTypeField = resolver.getField(Metacard.CONTENT_TYPE,
                AttributeType.AttributeFormat.STRING,
                true);
        String contentTypeVersionField = resolver.getField(Metacard.CONTENT_TYPE_VERSION,
                AttributeType.AttributeFormat.STRING,
                true);

        /*
         * If we didn't find the field, it most likely means it does not exist. If it does not
         * exist, then we can safely say that no content types are in this catalog provider
         */
        if (contentTypeField == null || contentTypeVersionField == null) {
            return finalSet;
        }

        SolrQuery query = new SolrQuery(contentTypeField + ":[* TO *]");
        query.setFacet(true);
        query.addFacetField(contentTypeField);
        query.addFacetPivotField(contentTypeField + "," + contentTypeVersionField);

        try {
            QueryResponse solrResponse = client.query(query, SolrRequest.METHOD.POST);
            List<FacetField> facetFields = solrResponse.getFacetFields();
            for (Map.Entry<String, List<PivotField>> entry : solrResponse.getFacetPivot()) {

                // if no content types have an associated version, the list of pivot fields will be
                // empty.
                // however, the content type names can still be obtained via the facet fields.
                if (CollectionUtils.isEmpty(entry.getValue())) {
                    LOGGER.debug(
                            "No content type versions found associated with any available content types.");

                    if (CollectionUtils.isNotEmpty(facetFields)) {
                        // Only one facet field was added. That facet field may contain multiple
                        // values (content type names).
                        for (FacetField.Count currContentType : facetFields.get(0)
                                .getValues()) {
                            // unknown version, so setting it to null
                            ContentType contentType =
                                    new ContentTypeImpl(currContentType.getName(), null);

                            finalSet.add(contentType);
                        }
                    }
                } else {
                    for (PivotField pf : entry.getValue()) {

                        String contentTypeName = pf.getValue()
                                .toString();
                        LOGGER.debug("contentTypeName: {}", contentTypeName);

                        if (CollectionUtils.isEmpty(pf.getPivot())) {
                            // if there are no sub-pivots, that means that there are no content type
                            // versions
                            // associated with this content type name
                            LOGGER.debug(
                                    "Content type does not have associated contentTypeVersion: {}",
                                    contentTypeName);
                            ContentType contentType = new ContentTypeImpl(contentTypeName,
                                    null);

                            finalSet.add(contentType);

                        } else {
                            for (PivotField innerPf : pf.getPivot()) {

                                LOGGER.debug("contentTypeVersion: {}. For contentTypeName: {}",
                                        innerPf.getValue(),
                                        contentTypeName);

                                ContentType contentType = new ContentTypeImpl(contentTypeName,
                                        innerPf.getValue()
                                                .toString());

                                finalSet.add(contentType);
                            }
                        }
                    }
                }
            }

        } catch (SolrServerException | IOException e) {
            LOGGER.info("Solr exception getting content types", e);
        }

        return finalSet;
    }

    protected SolrQuery getSolrQuery(QueryRequest request, SolrFilterDelegate solrFilterDelegate)
            throws UnsupportedQueryException {
        solrFilterDelegate.setSortPolicy(request.getQuery()
                .getSortBy());

        SolrQuery query = filterAdapter.adapt(request.getQuery(), solrFilterDelegate);

        return postAdapt(request, solrFilterDelegate, query);
    }

    protected SolrQuery postAdapt(QueryRequest request, SolrFilterDelegate filterDelegate,
            SolrQuery query) throws UnsupportedQueryException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Prepared Query: {}", query.getQuery());
            if (query.getFilterQueries() != null && query.getFilterQueries().length > 0) {
                LOGGER.debug("Filter Queries: {}", Arrays.toString(query.getFilterQueries()));
            }
        }

        /* Start Index */
        if (request.getQuery()
                .getStartIndex() < 1) {
            throw new UnsupportedQueryException("Start index must be greater than 0");
        }

        // Solr is 0-based
        query.setStart(request.getQuery()
                .getStartIndex() - 1);

        if (queryingForAllRecords(request)) {
            try {
                query.setRows(queryForNumberOfRows(query));
            } catch (SolrServerException | IOException | SolrException | ArithmeticException exception) {
                throw new UnsupportedQueryException("Could not retrieve number of records.", exception);
            }
        } else {
            query.setRows(request.getQuery()
                    .getPageSize());
        }

        setSortProperty(request, query, filterDelegate);

        return query;
    }

    private boolean queryingForAllRecords(QueryRequest request) {
        return request.getQuery()
                .getPageSize() < 1;
    }

    private int queryForNumberOfRows(SolrQuery query) throws SolrServerException, IOException {
        int numRows;
        query.setRows(0);
        QueryResponse solrResponse = client.query(query, SolrRequest.METHOD.POST);
        numRows = Math.toIntExact(solrResponse.getResults()
                .getNumFound());
        return numRows;
    }

    private void addDistanceSort(SolrQuery query, String sortField, SolrQuery.ORDER order,
            SolrFilterDelegate delegate) {
        if (delegate.isSortedByDistance()) {
            query.addSort(DISTANCE_SORT_FUNCTION, order);
            query.setFields("*",
                    RELEVANCE_SORT_FIELD,
                    DISTANCE_SORT_FIELD + ":" + DISTANCE_SORT_FUNCTION);
            query.add(SORT_FIELD_KEY, sortField);
            query.add(POINT_KEY, delegate.getSortedDistancePoint());
        }
    }

    protected String setSortProperty(QueryRequest request, SolrQuery query,
            SolrFilterDelegate solrFilterDelegate) {
        SortBy sortBy = request.getQuery()
                .getSortBy();
        String sortProperty = "";

        if (sortBy != null && sortBy.getPropertyName() != null) {
            sortProperty = sortBy.getPropertyName()
                    .getPropertyName();
            SolrQuery.ORDER order = SolrQuery.ORDER.desc;

            if (sortBy.getSortOrder() == SortOrder.ASCENDING) {
                order = SolrQuery.ORDER.asc;
            }

            query.setFields("*", RELEVANCE_SORT_FIELD);

            if (Result.RELEVANCE.equals(sortProperty)) {
                query.addSort(RELEVANCE_SORT_FIELD, order);
            } else if (Result.DISTANCE.equals(sortProperty)) {
                addDistanceSort(query, GEOMETRY_SORT_FIELD, order, solrFilterDelegate);
            } else if (sortProperty.equals(Result.TEMPORAL)) {
                query.addSort(resolver.getSortKey(resolver.getField(Metacard.EFFECTIVE,
                        AttributeType.AttributeFormat.DATE,
                        false)), order);
            } else {
                List<String> resolvedProperties = resolver.getAnonymousField(sortProperty);

                if (!resolvedProperties.isEmpty()) {
                    for (String sortField : resolvedProperties) {
                        if (sortField.endsWith(SchemaFields.GEO_SUFFIX)) {
                            addDistanceSort(query,
                                    resolver.getSortKey(sortField),
                                    order,
                                    solrFilterDelegate);
                        } else if (!(sortField.endsWith(SchemaFields.BINARY_SUFFIX)
                                || sortField.endsWith(SchemaFields.OBJECT_SUFFIX))) {
                            query.addSort(resolver.getSortKey(sortField), order);
                        }
                    }
                } else {
                    LOGGER.debug(
                            "No schema field was found for sort property [{}]. No sort field was added to the query.",
                            sortProperty);
                }

            }

        }
        return resolver.getSortKey(sortProperty);
    }

    private ResultImpl createResult(SolrDocument doc) throws MetacardCreationException {
        ResultImpl result = new ResultImpl(createMetacard(doc));

        if (doc.get(RELEVANCE_SORT_FIELD) != null) {
            result.setRelevanceScore(((Float) (doc.get(RELEVANCE_SORT_FIELD))).doubleValue());
        }

        if (doc.get(DISTANCE_SORT_FIELD) != null) {
            Object distance = doc.getFieldValue(DISTANCE_SORT_FIELD);

            if (distance != null) {
                LOGGER.debug("Distance returned from Solr [{}]", distance);
                double convertedDistance = new Distance(Double.valueOf(distance.toString()),
                        Distance.LinearUnit.KILOMETER).getAs(Distance.LinearUnit.METER);

                result.setDistanceInMeters(convertedDistance);
            }
        }

        return result;
    }

    private Double degreesToMeters(double distance) {
        return new Distance(DistanceUtils.degrees2Dist(distance,
                DistanceUtils.EARTH_MEAN_RADIUS_KM),
                Distance.LinearUnit.KILOMETER).getAs(Distance.LinearUnit.METER);
    }

    public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
        MetacardType metacardType = resolver.getMetacardType(doc);
        MetacardImpl metacard = new MetacardImpl(metacardType);

        for (String solrFieldName : doc.getFieldNames()) {
            if (!resolver.isPrivateField(solrFieldName)) {
                Collection<Object> fieldValues = doc.getFieldValues(solrFieldName);
                Attribute attr = new AttributeImpl(resolver.resolveFieldName(solrFieldName),
                        resolver.getDocValues(solrFieldName, fieldValues));
                metacard.setAttribute(attr);
            }
        }

        return metacard;
    }

    @Override
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
            client.add(docs);
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

    @Override
    public void deleteByIds(String fieldName, List<? extends Serializable> identifiers,
            boolean forceCommit) throws IOException, SolrServerException {
        if (identifiers == null || identifiers.size() == 0) {
            return;
        }

        if (Metacard.ID.equals(fieldName)) {
            CollectionUtils.transform(identifiers, new Transformer() {
                @Override
                public Object transform(Object o) {
                    return o.toString();
                }
            });
            client.deleteById((List<String>) identifiers);
        } else {
            if (identifiers.size() < SolrCatalogProvider.MAX_BOOLEAN_CLAUSES) {
                client.deleteByQuery(getIdentifierQuery(fieldName, identifiers));
            } else {
                int i = 0;
                for (
                        i = SolrCatalogProvider.MAX_BOOLEAN_CLAUSES;
                        i < identifiers.size(); i += SolrCatalogProvider.MAX_BOOLEAN_CLAUSES) {
                    client.deleteByQuery(getIdentifierQuery(fieldName,
                            identifiers.subList(i - SolrCatalogProvider.MAX_BOOLEAN_CLAUSES, i)));
                }
                client.deleteByQuery(getIdentifierQuery(fieldName,
                        identifiers.subList(i - SolrCatalogProvider.MAX_BOOLEAN_CLAUSES,
                                identifiers.size())));
            }
        }

        if (forceCommit) {
            client.commit();
        }
    }

    @Override
    public void deleteByQuery(String query) throws IOException, SolrServerException {
        client.deleteByQuery(query);
    }

    public String getIdentifierQuery(String fieldName, List<? extends Serializable> identifiers) {
        StringBuilder queryBuilder = new StringBuilder();
        for (Serializable id : identifiers) {
            if (queryBuilder.length() > 0) {
                queryBuilder.append(" OR ");
            }

            queryBuilder.append(fieldName)
                    .append(":")
                    .append(QUOTE)
                    .append(id)
                    .append(QUOTE);
        }
        return queryBuilder.toString();
    }

    private org.apache.solr.client.solrj.response.UpdateResponse softCommit(
            List<SolrInputDocument> docs) throws SolrServerException, IOException {
        return new org.apache.solr.client.solrj.request.UpdateRequest().add(docs)
                .setAction(AbstractUpdateRequest.ACTION.COMMIT,
                /* waitForFlush */true,
                /* waitToMakeVisible */true,
                /* softCommit */true)
                .process(client);
    }
}
