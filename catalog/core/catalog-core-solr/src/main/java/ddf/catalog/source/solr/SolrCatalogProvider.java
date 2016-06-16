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
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.PivotField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.codice.solr.factory.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
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
import ddf.catalog.operation.impl.UpdateImpl;
import ddf.catalog.operation.impl.UpdateResponseImpl;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;

/**
 * {@link CatalogProvider} implementation using Apache Solr
 */
public class SolrCatalogProvider extends MaskableImpl implements CatalogProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrCatalogProvider.class);

    private static final String COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE =
            "Could not complete delete request.";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String QUOTE = "\"";

    private static final String REQUEST_MUST_NOT_BE_NULL_MESSAGE = "Request must not be null";

    private static final double HASHMAP_DEFAULT_LOAD_FACTOR = 0.75;

    public static final int MAX_BOOLEAN_CLAUSES = 1024;

    private static Properties describableProperties = new Properties();

    static {
        try (InputStream propertiesStream = ddf.catalog.source.solr.SolrCatalogProvider.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(propertiesStream);
        } catch (IOException e) {
            LOGGER.info("IO exception loading describable properties", e);
        }
    }

    private DynamicSchemaResolver resolver;

    private SolrClient solr;

    private SolrMetacardClient client;

    /**
     * Constructor that creates a new instance and allows for a custom {@link DynamicSchemaResolver}
     *
     * @param solrClient Solr client
     * @param adapter    injected implementation of FilterAdapter
     * @param resolver   Solr schema resolver
     */
    public SolrCatalogProvider(SolrClient solrClient, FilterAdapter adapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory, DynamicSchemaResolver resolver) {
        if (solrClient == null) {
            throw new IllegalArgumentException("SolrClient cannot be null.");
        }

        this.solr = solrClient;
        this.resolver = resolver;

        LOGGER.debug("Constructing {} with Solr client [{}]",
                SolrCatalogProvider.class.getName(),
                solr);

        resolver.addFieldsFromClient(solrClient);
        this.client = new ProviderSolrMetacardClient(solrClient,
                adapter,
                solrFilterDelegateFactory,
                resolver);
    }

    /**
     * Convenience constructor that creates a new ddf.catalog.source.solr.DynamicSchemaResolver
     *
     * @param solrClient Solr client
     * @param adapter    injected implementation of FilterAdapter
     */
    public SolrCatalogProvider(SolrClient solrClient, FilterAdapter adapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        this(solrClient, adapter, solrFilterDelegateFactory, new DynamicSchemaResolver());
    }

    @Override
    public Set<ContentType> getContentTypes() {

        Set<ContentType> finalSet = new HashSet<>();

        String contentTypeField = resolver.getField(Metacard.CONTENT_TYPE,
                AttributeFormat.STRING,
                true);
        String contentTypeVersionField = resolver.getField(Metacard.CONTENT_TYPE_VERSION,
                AttributeFormat.STRING,
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
            QueryResponse solrResponse = solr.query(query, METHOD.POST);
            List<FacetField> facetFields = solrResponse.getFacetFields();
            for (Entry<String, List<PivotField>> entry : solrResponse.getFacetPivot()) {

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
                            ContentTypeImpl contentType =
                                    new ContentTypeImpl(currContentType.getName(), null);

                            finalSet.add(contentType);
                        }
                    }
                } else {
                    for (PivotField pf : entry.getValue()) {

                        String contentTypeName = pf.getValue()
                                .toString();
                        LOGGER.debug("contentTypeName:{}", contentTypeName);

                        if (CollectionUtils.isEmpty(pf.getPivot())) {
                            // if there are no sub-pivots, that means that there are no content type
                            // versions
                            // associated with this content type name
                            LOGGER.debug(
                                    "Content type does not have associated contentTypeVersion: {}",
                                    contentTypeName);
                            ContentTypeImpl contentType = new ContentTypeImpl(contentTypeName,
                                    null);

                            finalSet.add(contentType);

                        } else {
                            for (PivotField innerPf : pf.getPivot()) {

                                LOGGER.debug("contentTypeVersion:{}. For contentTypeName: {}",
                                        innerPf.getValue(),
                                        contentTypeName);

                                ContentTypeImpl contentType = new ContentTypeImpl(contentTypeName,
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

    @Override
    public boolean isAvailable() {
        try {
            SolrPingResponse ping = solr.ping();

            return "OK".equals(ping.getResponse()
                    .get("status"));
        } catch (Exception e) {
            /*
             * if we get any type of exception, whether declared by Solr or not, we do not want to
             * fail, we just want to return false
             */
            LOGGER.warn("Solr ping request/response failed.", e);
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
        LOGGER.trace("Sitename changed from [{}] to [{}]", getId(), id);
        super.maskId(id);
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        return client.query(request);
    }

    @Override
    public CreateResponse create(CreateRequest request) throws IngestException {
        if (request == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        List<Metacard> metacards = request.getMetacards();
        List<Metacard> output = new ArrayList<>();

        if (metacards == null) {
            return new CreateResponseImpl(request, null, output);
        }

        for (Metacard metacard : metacards) {
            boolean isSourceIdSet =
                    (metacard.getSourceId() != null && !"".equals(metacard.getSourceId()));
            /*
             * If an ID is not provided, then one is generated so that documents are unique. Solr
             * will not accept documents unless the id is unique.
             */
            if (metacard.getId() == null || metacard.getId()
                    .equals("")) {
                if (isSourceIdSet) {
                    throw new IngestException("Metacard from a separate distribution must have ID");
                }
                metacard.setAttribute(new AttributeImpl(Metacard.ID, generatePrimaryKey()));
            }

            if (!isSourceIdSet) {
                metacard.setSourceId(getId());
            }
            output.add(metacard);
        }

        try {
            client.add(output, isForcedAutoCommit());
        } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
            throw new IngestException("Solr could not ingest metacard(s).");
        }
        // TODO (RCZ) - Do we need to put the req props into the response????
        return new CreateResponseImpl(request, null, output);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
        if (updateRequest == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        List<Entry<Serializable, Metacard>> updates = updateRequest.getUpdates();

        // the list of updates, both new and old metacards
        ArrayList<Update> updateList = new ArrayList<>();

        String attributeName = updateRequest.getAttributeName();

        // need an attribute name in order to do query
        if (attributeName == null) {
            throw new IngestException("Attribute name cannot be null. "
                    + "Please provide the name of the attribute.");
        }

        List<String> identifiers = new ArrayList<>();

        // if we have nothing to update, send the empty list
        if (updates == null || updates.size() == 0) {
            return new UpdateResponseImpl(updateRequest, null, new ArrayList<Update>());
        }

        /* 1. QUERY */

        // Loop to get all identifiers
        for (Entry<Serializable, Metacard> updateEntry : updates) {
            identifiers.add(updateEntry.getKey()
                    .toString());
        }

        /* 1a. Create the old Metacard Query */
        String attributeQuery = getQuery(attributeName, identifiers);

        SolrQuery query = new SolrQuery(attributeQuery);

        QueryResponse idResults = null;

        /* 1b. Execute Query */
        try {
            idResults = solr.query(query, METHOD.POST);
        } catch (SolrServerException | IOException e) {
            LOGGER.warn("Solr exception during query", e);
        }

        // CHECK if we got any results back
        if (idResults != null && idResults.getResults() != null && idResults.getResults()
                .size() != 0) {

            LOGGER.info("Found {} current metacard(s).",
                    idResults.getResults()
                            .size());

            // CHECK updates size assertion
            if (idResults.getResults()
                    .size() > updates.size()) {
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
        int initialHashMapCapacity = (int) (idResults.getResults()
                .size() / HASHMAP_DEFAULT_LOAD_FACTOR) + 1;

        // map of old metacards to be populated
        Map<Serializable, Metacard> idToMetacardMap = new HashMap<>(initialHashMapCapacity);

        /* 1c. Populate list of old metacards */
        for (SolrDocument doc : idResults.getResults()) {
            Metacard old;
            try {
                old = client.createMetacard(doc);
            } catch (MetacardCreationException e) {
                throw new IngestException("Could not create metacard(s).");
            }

            if (!idToMetacardMap.containsKey(old.getAttribute(attributeName)
                    .getValue())) {
                idToMetacardMap.put(old.getAttribute(attributeName)
                        .getValue(), old);
            } else {
                throw new IngestException(
                        "The attribute value given [" + old.getAttribute(attributeName)
                                .getValue()
                                + "] matched multiple records. Attribute values must at most match only one unique Metacard.");
            }
        }

        /* 2. Update the cards */
        List<Metacard> newMetacards = new ArrayList<>();
        for (Entry<Serializable, Metacard> updateEntry : updates) {
            String localKey = updateEntry.getKey()
                    .toString();

            /* 2a. Prepare new Metacard */
            MetacardImpl newMetacard = new MetacardImpl(updateEntry.getValue());
            // Find the exact oldMetacard that corresponds with this newMetacard
            Metacard oldMetacard = idToMetacardMap.get(localKey);

            // We need to skip because of partial updates such as one entry
            // matched but another did not
            if (oldMetacard != null) {
                // overwrite the id, in case it has not been done properly/already
                newMetacard.setId(oldMetacard.getId());

                newMetacard.setSourceId(getId());

                newMetacards.add(newMetacard);
                updateList.add(new UpdateImpl(newMetacard, oldMetacard));
            }

        }

        try {
            client.add(newMetacards, isForcedAutoCommit());
        } catch (SolrServerException | SolrException | IOException | MetacardCreationException e) {
            throw new IngestException("Solr could not ingest metacard(s).");
        }

        return new UpdateResponseImpl(updateRequest, updateRequest.getProperties(), updateList);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {

        if (deleteRequest == null) {
            throw new IngestException(REQUEST_MUST_NOT_BE_NULL_MESSAGE);
        }

        List<Metacard> deletedMetacards = new ArrayList<>();

        String attributeName = deleteRequest.getAttributeName();
        if (StringUtils.isBlank(attributeName)) {
            throw new IngestException(
                    "Attribute name cannot be empty. Please provide the name of the attribute.");
        }

        @SuppressWarnings("unchecked")
        List<? extends Serializable> identifiers = deleteRequest.getAttributeValues();

        if (identifiers == null || identifiers.size() == 0) {
            return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);
        }

        if (identifiers.size() <= MAX_BOOLEAN_CLAUSES) {
            deleteListOfMetacards(deletedMetacards, identifiers, attributeName);
        } else {
            List<? extends Serializable> identifierPaged = null;
            int currPagingSize = 0;

            for (
                    currPagingSize = MAX_BOOLEAN_CLAUSES;
                    currPagingSize < identifiers.size(); currPagingSize += MAX_BOOLEAN_CLAUSES) {
                identifierPaged = identifiers.subList(currPagingSize - MAX_BOOLEAN_CLAUSES,
                        currPagingSize);
                deleteListOfMetacards(deletedMetacards, identifierPaged, attributeName);
            }
            identifierPaged = identifiers.subList(currPagingSize - MAX_BOOLEAN_CLAUSES,
                    identifiers.size());
            deleteListOfMetacards(deletedMetacards, identifierPaged, attributeName);
        }
        return new DeleteResponseImpl(deleteRequest, null, deletedMetacards);

    }

    private void deleteListOfMetacards(List<Metacard> deletedMetacards,
            List<? extends Serializable> identifiers, String attributeName) throws IngestException {
        String fieldName = attributeName + SchemaFields.TEXT_SUFFIX;
        SolrDocumentList docs = getSolrDocumentList(identifiers, fieldName);
        createListOfDeletedMetacards(deletedMetacards, docs);

        try {
            // the assumption is if something was deleted, it should be gone
            // right away, such as expired data, etc.
            // so we force the commit
            client.deleteByIds(fieldName, identifiers, true);
        } catch (SolrServerException | IOException e) {
            throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
        }
    }

    private void createListOfDeletedMetacards(List<Metacard> deletedMetacards,
            SolrDocumentList docs) throws IngestException {

        for (SolrDocument doc : docs) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SOLR DOC: {}",
                        doc.getFieldValue(Metacard.ID + SchemaFields.TEXT_SUFFIX));
            }

            try {
                deletedMetacards.add(client.createMetacard(doc));
            } catch (MetacardCreationException e) {
                LOGGER.info("Metacard creation exception creating metacards during delete", e);
                throw new IngestException("Could not create metacard(s).");
            }

        }
    }

    private SolrDocumentList getSolrDocumentList(List<? extends Serializable> identifierPaged,
            String fieldName) throws IngestException {
        SolrQuery query = new SolrQuery(client.getIdentifierQuery(fieldName, identifierPaged));
        query.setRows(identifierPaged.size());

        QueryResponse solrResponse;
        try {
            solrResponse = solr.query(query, METHOD.POST);
        } catch (SolrServerException | IOException e) {
            LOGGER.info("Solr exception deleting request message", e);
            throw new IngestException(COULD_NOT_COMPLETE_DELETE_REQUEST_MESSAGE);
        }
        return solrResponse.getResults();
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

            queryBuilder.append(mappedNames.get(0))
                    .append(":")
                    .append(QUOTE)
                    .append(id)
                    .append(QUOTE);
        }

        String query = queryBuilder.toString();

        LOGGER.debug("query = [{}]", query);

        return query;
    }

    private String generatePrimaryKey() {
        return UUID.randomUUID()
                .toString()
                .replaceAll("-", "");
    }

    public boolean isForcedAutoCommit() {
        return ConfigurationStore.getInstance()
                .isForceAutoCommit();
    }

    public void shutdown() {
        LOGGER.info("Closing down Solr client.");
        try {
            solr.close();
        } catch (IOException e) {
            LOGGER.info("Failed to close Solr client.", e);
        }
    }

    private class ProviderSolrMetacardClient extends SolrMetacardClient {

        public ProviderSolrMetacardClient(SolrClient client, FilterAdapter catalogFilterAdapter,
                SolrFilterDelegateFactory solrFilterDelegateFactory,
                DynamicSchemaResolver dynamicSchemaResolver) {
            super(client, catalogFilterAdapter, solrFilterDelegateFactory, dynamicSchemaResolver);
        }

        @Override
        public MetacardImpl createMetacard(SolrDocument doc) throws MetacardCreationException {
            MetacardImpl metacard = super.createMetacard(doc);
            metacard.setSourceId(getId());
            return metacard;
        }
    }

}
