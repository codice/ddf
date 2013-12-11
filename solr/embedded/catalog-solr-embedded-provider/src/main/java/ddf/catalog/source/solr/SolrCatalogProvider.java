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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

import com.spatial4j.core.distance.DistanceUtils;

import ddf.catalog.data.AttributeType.AttributeFormat;
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
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateResponseImpl;
import ddf.catalog.operation.impl.DeleteResponseImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;

/**
 * {@link CatalogProvider} implementation using Apache Solr 4+
 * 
 */
public class SolrCatalogProvider extends MaskableImpl implements CatalogProvider {

    private static final String COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE = "Could not complete delete request.";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String RELEVANCE_SORT_FIELD = "score";

    private static final String QUOTE = "\"";

    private static final String REQUEST_MUST_NOT_BE_NULL_MESSAGE = "Request must not be null";

    private static final Logger LOGGER = Logger.getLogger(SolrCatalogProvider.class);

    private static final double HASHMAP_DEFAULT_LOAD_FACTOR = 0.75;

    private static final String ENTERED = "ENTERED: ";

    private static final String EXITED = "EXITED: ";

    private FilterAdapter filterAdapter;

    private DynamicSchemaResolver resolver;

    private SolrServer server;

    private SolrFilterDelegateFactory solrFilterDelegateFactory;

    private static Properties describableProperties = new Properties();

    static {
        try {
            describableProperties.load(SolrCatalogProvider.class
                    .getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE));
        } catch (IOException e) {
            LOGGER.info(e);
        }

    }

    /**
     * Constructor that creates a new instance and allows for a custom {@link DynamicSchemaResolver}
     * 
     * @param server
     * @param adapter
     *            injected implementation of FilterAdapter
     * @param resolver
     */
    public SolrCatalogProvider(SolrServer server, FilterAdapter adapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory, DynamicSchemaResolver resolver) {

        if (server == null) {
            throw new IllegalArgumentException("SolrServer cannot be null.");
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Constructing " + SolrCatalogProvider.class.getName() + " with server ["
                    + server + "]");
        }

        this.server = server;
        this.filterAdapter = adapter;
        this.solrFilterDelegateFactory = solrFilterDelegateFactory;
        this.resolver = resolver;
        resolver.addFieldsFromServer(server);
    }

    /**
     * Convenience constructor that creates a new DynamicSchemaResolver
     * 
     * @param server
     * @param adapter
     *            injected implementation of FilterAdapter
     */
    public SolrCatalogProvider(SolrServer server, FilterAdapter adapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        this(server, adapter, solrFilterDelegateFactory, new DynamicSchemaResolver());
    }

    @Override
    public Set<ContentType> getContentTypes() {

        Set<ContentType> finalSet = new HashSet<ContentType>();

        String contentTypeField = resolver.getField(Metacard.CONTENT_TYPE, AttributeFormat.STRING,
                true);
        String contentTypeVersionField = resolver.getField(Metacard.CONTENT_TYPE_VERSION,
                AttributeFormat.STRING, true);

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
            QueryResponse solrResponse = server.query(query, METHOD.POST);
            List<FacetField> facetFields = solrResponse.getFacetFields();
            for (Entry<String, List<PivotField>> entry : solrResponse.getFacetPivot()) {

                // if no content types have an associated version, the list of pivot fields will be
                // empty.
                // however, the content type names can still be obtained via the facet fields.
                if (CollectionUtils.isEmpty(entry.getValue())) {
                    LOGGER.debug("No content type versions found associated with any available content types.");

                    if (CollectionUtils.isNotEmpty(facetFields)) {
                        // Only one facet field was added. That facet field may contain multiple
                        // values (content type names).
                        for (FacetField.Count currContentType : facetFields.get(0).getValues()) {
                            // unknown version, so setting it to null
                            ContentTypeImpl contentType = new ContentTypeImpl(
                                    currContentType.getName(), null);

                            finalSet.add(contentType);
                        }
                    }
                } else {
                    for (PivotField pf : entry.getValue()) {

                        String contentTypeName = pf.getValue().toString();
                        LOGGER.debug("contentTypeName:" + contentTypeName);

                        if (CollectionUtils.isEmpty(pf.getPivot())) {
                            // if there are no sub-pivots, that means that there are no content type
                            // versions
                            // associated with this content type name
                            LOGGER.debug("Content type does not have associated contentTypeVersion: "
                                    + contentTypeName);
                            ContentTypeImpl contentType = new ContentTypeImpl(contentTypeName, null);

                            finalSet.add(contentType);

                        } else {
                            for (PivotField innerPf : pf.getPivot()) {

                                LOGGER.debug("contentTypeVersion:" + innerPf.getValue()
                                        + ". For contentTypeName: " + contentTypeName);

                                ContentTypeImpl contentType = new ContentTypeImpl(contentTypeName,
                                        innerPf.getValue().toString());

                                finalSet.add(contentType);
                            }
                        }
                    }
                }
            }

        } catch (SolrServerException e) {
            LOGGER.info(e);
        }

        return finalSet;
    }

    @Override
    public boolean isAvailable() {

        try {

            SolrPingResponse ping = server.ping();

            return "OK".equals(ping.getResponse().get("status"));
        } catch (Exception e) {
            /*
             * if we get any type of exception, whether declared by Solr or not, we do not want to
             * fail, we just want to return false
             */
            LOGGER.warn("Solr Server ping request/response failed.", e);
        }

        return false;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {

        return isAvailable();
    }

    @Override
    public String getDescription() {

        return describableProperties.getProperty("description");
    }

    @Override
    public String getOrganization() {

        return describableProperties.getProperty("organization");
    }

    @Override
    public String getTitle() {

        return describableProperties.getProperty("name");
    }

    @Override
    public String getVersion() {

        return describableProperties.getProperty("version");
    }

    @Override
    public void maskId(String id) {

        LOGGER.info("Sitename changed from [" + getId() + "] to [" + id + "]");
        super.maskId(id);
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {

        LOGGER.debug(ENTERED + "query");

        if (request == null || request.getQuery() == null) {
            return new QueryResponseImpl(request, new ArrayList<Result>(), true, 0L);
        }

        long totalHits = 0L;

        List<Result> results = new ArrayList<Result>();

        SolrFilterDelegate solrFilterDelegate = solrFilterDelegateFactory.newInstance(resolver);

        solrFilterDelegate.setSortPolicy(request.getQuery().getSortBy());

        SolrQuery query = filterAdapter.adapt(request.getQuery(), solrFilterDelegate);

        // Solr does not support outside parenthesis in certain queries and throws EOF exception.
        String queryPhrase = query.getQuery().trim();
        if (queryPhrase.matches("\\(\\s*\\{!.*\\)")) {
            query.setQuery(queryPhrase.replaceAll("^\\(\\s*|\\s*\\)$", ""));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Prepared Query: " + query.getQuery());
        }

        if (request.getQuery().getPageSize() < 1) {
            query.setRows(Integer.MAX_VALUE);
        } else {
            query.setRows(request.getQuery().getPageSize());
        }

        /* Sorting */
        SortBy sortBy = request.getQuery().getSortBy();

        String sortProperty = "";

        if (sortBy != null && sortBy.getPropertyName() != null) {

            sortProperty = sortBy.getPropertyName().getPropertyName();

            ORDER order = ORDER.desc;

            if (sortBy.getSortOrder() == SortOrder.ASCENDING) {
                order = ORDER.asc;
            }

            if (Result.RELEVANCE.equals(sortProperty) || Result.DISTANCE.equals(sortProperty)) {
                query.setFields("*", RELEVANCE_SORT_FIELD);
                query.setSortField(RELEVANCE_SORT_FIELD, order);
            } else if (sortProperty.equals(Result.TEMPORAL)) {
                query.addSortField(
                        resolver.getField(Metacard.EFFECTIVE, AttributeFormat.DATE, false), order);
            } else {

                List<String> resolvedProperties = resolver.getAnonymousField(sortProperty);

                if (!resolvedProperties.isEmpty()) {
                    for (String sortField : resolvedProperties) {
                        query.addSortField(sortField, order);
                    }

                    query.add("fl", "*," + RELEVANCE_SORT_FIELD);
                } else {
                    LOGGER.info("No schema field was found for sort property [" + sortProperty
                            + "]. No sort field was added to the query.");
                }

            }

        }

        /* Start Index */
        if (request.getQuery().getStartIndex() < 1) {
            throw new UnsupportedQueryException("Start index must be greater than 0");
        }

        // solr is 0-based
        query.setStart(request.getQuery().getStartIndex() - 1);

        try {
            QueryResponse solrResponse = server.query(query, METHOD.POST);

            totalHits = solrResponse.getResults().getNumFound();

            SolrDocumentList docs = solrResponse.getResults();

            for (SolrDocument doc : docs) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("SOLR DOC:"
                            + doc.getFieldValue(Metacard.ID + SchemaFields.TEXT_SUFFIX));
                }
                ResultImpl tmpResult;
                try {
                    tmpResult = createResult(doc, sortProperty);
                    // TODO: register metacard type???
                } catch (MetacardCreationException e) {
                    LOGGER.warn(e);
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

        LOGGER.debug("EXITING: query");
        return sourceResponseImpl;
    }

    private Double degreesToMeters(double distance) {
        return new Distance(
                DistanceUtils.degrees2Dist(distance, DistanceUtils.EARTH_MEAN_RADIUS_KM),
                LinearUnit.KILOMETER).getAs(LinearUnit.METER);
    }

    @Override
    public CreateResponse create(CreateRequest request) throws IngestException {

        LOGGER.debug(ENTERED + " CREATE");

        if (request == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        List<Metacard> metacards = request.getMetacards();

        List<Metacard> output = new ArrayList<Metacard>();

        if (metacards == null) {
            return new CreateResponseImpl(request, null, output);
        }

        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

        for (Metacard metacard : metacards) {

            boolean isSourceIdSet = (metacard.getSourceId() != null && !"".equals(metacard
                    .getSourceId()));

            /*
             * If an ID is not provided, then one is generated so that documents are unique. Solr
             * will not accept documents unless the id is unique.
             */
            if (metacard.getId() == null || metacard.getId().equals("")) {
                if (isSourceIdSet) {
                    throw new IngestException("Metacard from a separate distribution must have ID");
                }
                metacard.setAttribute(new AttributeImpl(Metacard.ID, generatePrimaryKey()));
            }

            SolrInputDocument solrInputDocument = new SolrInputDocument();

            // TODO: register metacard type here.
            try {
                resolver.addFields(metacard, solrInputDocument);
            } catch (MetacardCreationException e) {
                LOGGER.warn(e);
                throw new IngestException("MetacardType could not be read.");
            }

            docs.add(solrInputDocument);
            if (!isSourceIdSet) {
                metacard.setSourceId(getId());
            }
            output.add(metacard);

        }

        try {
            if (!isForcedAutoCommit()) {
                server.add(docs);
            } else {
                softCommit(docs);
            }
        } catch (SolrServerException e) {
            LOGGER.warn(e);
            throw new IngestException("Server could not ingest metacard(s).");
        } catch (SolrException e) {
            LOGGER.warn(e);
            throw new IngestException("Server could not ingest metacard(s).");
        } catch (IOException e) {
            LOGGER.warn(e);
        }

        CreateResponseImpl createResponseImpl = new CreateResponseImpl(request, null, output);

        return createResponseImpl;

    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {

        if (updateRequest == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        // for the modified date, possibly will be replaced by a plugin?
        Date now = new Date();

        List<Entry<Serializable, Metacard>> updates = updateRequest.getUpdates();

        // the list of updates, both new and old metacards
        ArrayList<Update> updateList = new ArrayList<Update>();

        String attributeName = updateRequest.getAttributeName();

        // need an attribute name in order to do query
        if (attributeName == null) {
            throw new IngestException("Attribute name cannot be null. "
                    + "Please provide the name of the attribute.");
        }

        List<String> identifiers = new ArrayList<String>();

        // if we have nothing to update, send the empty list
        if (updates == null || updates.size() == 0) {
            return new UpdateResponseImpl(updateRequest, null, new ArrayList<Update>());
        }

        /* 1. QUERY */

        // Loop to get all identifiers
        for (Entry<Serializable, Metacard> updateEntry : updates) {
            identifiers.add(updateEntry.getKey().toString());
        }

        /* 1a. Create the old Metacard Query */
        String attributeQuery = getQuery(attributeName, identifiers);

        SolrQuery query = new SolrQuery(attributeQuery);

        QueryResponse idResults = null;

        /* 1b. Execute Query */
        try {

            idResults = server.query(query, METHOD.POST);

        } catch (SolrServerException e) {
            LOGGER.warn(e);
        }

        // CHECK if we got any results back
        if (idResults != null && idResults.getResults() != null
                && idResults.getResults().size() != 0) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("Found " + idResults.getResults().size() + " current metacard(s).");
            }

            // CHECK updates size assertion
            if (idResults.getResults().size() > updates.size()) {
                throw new IngestException(
                        "Found more metacards than updated metacards provided. Please ensure your attribute values match unique records.");
            }

        } else {

            LOGGER.info("No results found for given attribute values.");

            // return an empty list
            return new UpdateResponseImpl(updateRequest, null, new ArrayList<Update>());

        }

        /*
         * According to HashMap javadoc, if initialCapacity > (max entries / load factor), then no
         * rehashing will occur. We purposely calculate the correct capacity for no rehashing.
         */

        /*
         * A map is used to store the metacards so that the order of metacards returned will not
         * matter. If we use a List and the metacards are out of order, we might not match the new
         * metacards properly with the old metacards.
         */
        int initialHashMapCapacity = (int) (idResults.getResults().size() / HASHMAP_DEFAULT_LOAD_FACTOR) + 1;

        // map of old metacards to be populated
        Map<Serializable, Metacard> idToMetacardMap = new HashMap<Serializable, Metacard>(
                initialHashMapCapacity);

        /* 1c. Populate list of old metacards */

        for (SolrDocument doc : idResults.getResults()) {
            Metacard old = null;
            try {
                old = createMetacard(doc);
            } catch (MetacardCreationException e) {
                throw new IngestException("Could not create metacard(s).");
            }

            if (!idToMetacardMap.containsKey(old.getAttribute(attributeName).getValue())) {
                idToMetacardMap.put(old.getAttribute(attributeName).getValue(), old);
            } else {
                throw new IngestException(
                        "The attribute value given ["
                                + old.getAttribute(attributeName).getValue()
                                + "] matched multiple records. Attribute values must at most match only one unique Metacard.");
            }

        }

        /* 2. Update the cards */

        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();
        for (Entry<Serializable, Metacard> updateEntry : updates) {

            String localKey = updateEntry.getKey().toString();

            /* 2a. Prepare new Metacard */
            MetacardImpl newMetacard = new MetacardImpl(updateEntry.getValue());
            // Find the exact oldMetacard that corresponds with this newMetacard
            Metacard oldMetacard = idToMetacardMap.get(localKey);

            // We need to skip because of partial updates such as one entry
            // matched but another did not
            if (oldMetacard != null) {

                prepareForUpdate(now, oldMetacard.getId(), newMetacard, oldMetacard);

                /* 2b. Build Solr Document */
                SolrInputDocument solrInputDocument = new SolrInputDocument();

                try {
                    resolver.addFields(newMetacard, solrInputDocument);
                } catch (MetacardCreationException e) {
                    throw new IngestException(e);
                }

                /* 2c. Add to list of updates */
                docs.add(solrInputDocument);

                newMetacard.setSourceId(getId());

                updateList.add(new UpdateImpl(newMetacard, oldMetacard));
            }

        }

        try {

            if (!isForcedAutoCommit()) {
                server.add(docs);
            } else {
                softCommit(docs);
            }

        } catch (SolrServerException e) {
            LOGGER.warn(e);
            throw new IngestException("Provider is not able to process the request.");
        } catch (IOException e) {
            LOGGER.warn(e);
            throw new IngestException("Provider is not able to process the request.");
        }

        return new UpdateResponseImpl(updateRequest, null, updateList);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {

        LOGGER.debug(ENTERED + "DELETE");

        if (deleteRequest == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        List<Metacard> deletedMetacards = new ArrayList<Metacard>();

        String attributeName = deleteRequest.getAttributeName();

        if (attributeName == null) {
            throw new IngestException(
                    "Attribute name cannot be null. Please provide the name of the attribute.");
        }

        @SuppressWarnings("unchecked")
        List<? extends Serializable> identifiers = deleteRequest.getAttributeValues();

        if (identifiers == null || identifiers.size() == 0) {
            LOGGER.debug(EXITED + " DELETE");
            return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);
        }

        /* 1. Query first for the records */

        StringBuilder queryBuilder = new StringBuilder();

        for (int i = 0; i < identifiers.size(); i++) {

            if (i != 0) {
                queryBuilder.append(" OR ");
            }

            queryBuilder.append(attributeName + SchemaFields.TEXT_SUFFIX + ":" + QUOTE
                    + identifiers.get(i) + QUOTE);

        }

        SolrQuery query = new SolrQuery(queryBuilder.toString());
        query.setRows(identifiers.size());

        QueryResponse solrResponse = null;
        try {
            solrResponse = server.query(query, METHOD.POST);
        } catch (SolrServerException e) {
            LOGGER.info(e);
            throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
        }

        SolrDocumentList docs = solrResponse.getResults();

        for (SolrDocument doc : docs) {

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SOLR DOC:"
                        + doc.getFieldValue(Metacard.ID + SchemaFields.TEXT_SUFFIX));
            }

            try {
                deletedMetacards.add(createMetacard(doc));
            } catch (MetacardCreationException e) {
                LOGGER.info(e);
                throw new IngestException("Could not create metacard(s).");
            }

        }
        /* 2. Delete */

        try {
            if (Metacard.ID.equals(attributeName)) {
                server.deleteById((List<String>) identifiers);
            } else {
                // solr deleteByQuery(queryBuilder.toString()) does not work,
                // SOLR BUG back in 4.0.0
                // so we have to delete by id
                List<String> metacardIdentfiers = new ArrayList<String>();
                for (Metacard deletedMetacard : deletedMetacards) {
                    metacardIdentfiers.add(deletedMetacard.getId());
                }
                server.deleteById(metacardIdentfiers);
            }

            // the assumption is if something was deleted, it should be gone
            // right away, such as expired data, etc.
            // so we commit to ensure that it is gone.
            server.commit();

        } catch (SolrServerException e) {
            LOGGER.error(e);
            throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
        } catch (IOException e) {
            LOGGER.error(e);
            throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
        }

        new DeleteResponseImpl(deleteRequest, null, deletedMetacards);

        LOGGER.debug(EXITED + " DELETE");
        return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);
    }

    /**
     * @param docs
     * @return
     * @throws SolrServerException
     * @throws IOException
     */
    private org.apache.solr.client.solrj.response.UpdateResponse softCommit(
            List<SolrInputDocument> docs) throws SolrServerException, IOException {
        boolean waitForFlush = true;
        boolean waitToMakeVisible = true;
        boolean softCommit = true;
        return new org.apache.solr.client.solrj.request.UpdateRequest().add(docs)
                .setAction(ACTION.COMMIT, waitForFlush, waitToMakeVisible, softCommit)
                .process(server);
    }

    private void prepareForUpdate(Date now, String keyId, MetacardImpl newMetacard,
            Metacard oldMetacard) {
        // overwrite the id, in case it has not been done properly/already
        newMetacard.setId(keyId);
        // copy over the created date, we can only have that info from the old
        // card
        newMetacard.setCreatedDate(oldMetacard.getCreatedDate());
        // overwrite the modified date, it should be replaced to the current
        // time
        newMetacard.setModifiedDate(now);
        // copy over the effective date in case it is null, effective date must
        // be populated
        if (newMetacard.getEffectiveDate() == null) {
            newMetacard.setEffectiveDate(now);
        }

    }

    private String getQuery(String attributeName, List<String> ids) throws IngestException {

        StringBuilder queryBuilder = new StringBuilder();

        List<String> mappedNames = resolver.getAnonymousField(attributeName);

        if (mappedNames.isEmpty()) {
            throw new IngestException("Could not resolve attribute name [" + attributeName + "]");
        }

        for (int i = 0; i < ids.size(); i++) {

            String id = ids.get(i);

            if (i > 0) {
                queryBuilder.append(" OR ");
            }

            queryBuilder.append(mappedNames.get(0)).append(":").append(QUOTE).append(id)
                    .append(QUOTE);
        }

        String query = queryBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("query = [" + query + "]");
        }
        return query;
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
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Distance returned from Solr [" + distance + "]");
                    }
                    double convertedDistance = degreesToMeters(Double.valueOf(distance.toString()));

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Converted distance into meters [" + convertedDistance + "]");
                    }
                    result.setDistanceInMeters(convertedDistance);
                }
            }

        }
        return result;
    }

    /**
     * Given a document from the server, this method creates a {@link Metacard}. It populates the
     * source id and {@link MetacardType}, as well as all the fields from the {@link SolrDocument}
     * 
     * @param doc
     *            {@link SolrDocument} from the Solr Server
     * @return a metacard
     * @throws MetacardCreationException
     */
    private MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {

        MetacardType metacardType = resolver.getMetacardType(doc);

        MetacardImpl metacard = new MetacardImpl(metacardType);

        for (String solrFieldName : doc.getFieldNames()) {
            if (!resolver.isPrivateField(solrFieldName)) {
                Serializable value = resolver.getDocValue(solrFieldName,
                        doc.getFieldValue(solrFieldName));
                metacard.setAttribute(resolver.resolveFieldName(solrFieldName), value);
            }
        }

        metacard.setSourceId(getId());

        return metacard;
    }

    private String generatePrimaryKey() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public boolean isForcedAutoCommit() {
        return ConfigurationStore.getInstance().isForceAutoCommit();
    }

    public void shutdown() {
        LOGGER.info("Shutting down solr server.");
        server.shutdown();
    }

}
