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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.BeforeClass;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;

public abstract class SolrProviderTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSolrProvider.class);

    protected static final int ALL_RESULTS = -1;

    protected static final String MASKED_ID = "scp";

    protected static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    protected static SolrCatalogProvider provider = null;

    private static final int TEST_METHOD_NAME_INDEX = 3;

    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("RUNNING setup.");
        ConfigurationStore.getInstance().setDataDirectoryPath("target/solr");
        ConfigurationStore.getInstance().setForceAutoCommit(true);
        ConfigurationFileProxy configurationFileProxy = new ConfigurationFileProxy(null,
                ConfigurationStore.getInstance());

        provider = new SolrCatalogProvider(SolrServerFactory.getEmbeddedSolrServer(
                "solrconfig.xml", "schema.xml", configurationFileProxy),
                new GeotoolsFilterAdapterImpl(), new SolrFilterDelegateFactoryImpl());

        // Mask the id, this is something that the CatalogFramework would
        // usually do
        provider.setId(MASKED_ID);
    }

    protected QueryRequest quickQuery(Filter filter) {
        return new QueryRequestImpl(new QueryImpl(filter));
    }

    protected static void messageBreak(String string) {
        String stars = StringUtils.repeat("*", string.length() + 2);
        LOGGER.info(stars);
        LOGGER.info("* {}", string);
        LOGGER.info(stars);
    }

    protected static void deleteAllIn(SolrCatalogProvider solrProvider) throws IngestException,
        UnsupportedQueryException {
        deleteAllIn(solrProvider, TEST_METHOD_NAME_INDEX);
    }

    protected static void deleteAllIn(SolrCatalogProvider solrProvider, int methodNameIndex)
        throws IngestException, UnsupportedQueryException {
        messageBreak(Thread.currentThread().getStackTrace()[methodNameIndex].getMethodName() + "()");

        boolean isCaseSensitive = false;
        boolean isFuzzy = false;

        QueryImpl query = null;
        SourceResponse sourceResponse = null;
        CommonQueryBuilder queryBuilder = new CommonQueryBuilder();
        query = queryBuilder.like(Metacard.ID, "*", isCaseSensitive, isFuzzy);
        query.setPageSize(ALL_RESULTS);
        sourceResponse = solrProvider.query(new QueryRequestImpl(query));

        List<String> ids = new ArrayList<String>();
        for (Result r : sourceResponse.getResults()) {
            ids.add(r.getMetacard().getId());
        }

        LOGGER.info("Records found for deletion: {}", ids);

        provider.delete(new DeleteRequestImpl(ids.toArray(new String[ids.size()])));

        LOGGER.info("Deletion complete. -----------");
    }

    protected void deleteAll() throws IngestException, UnsupportedQueryException {
        deleteAllIn(provider);
    }

    protected void queryAndVerifyCount(int count, Filter filter) throws UnsupportedQueryException {
        Query query = new QueryImpl(filter);
        QueryRequest request = new QueryRequestImpl(query);
        SourceResponse response = provider.query(request);

        assertEquals(count, response.getResults().size());
    }

    protected DeleteResponse delete(String identifier) throws IngestException {
        return delete(new String[] {identifier});
    }

    protected DeleteResponse delete(String[] identifier) throws IngestException {
        DeleteResponse deleteResponse = provider.delete(new DeleteRequestImpl(identifier));

        return deleteResponse;
    }

    protected UpdateResponse update(String id, Metacard metacard) throws IngestException {
        String[] ids = {id};
        return update(ids, Arrays.asList(metacard));
    }

    protected UpdateResponse update(String[] ids, List<Metacard> list) throws IngestException {
        UpdateResponse updateResponse = provider.update(new UpdateRequestImpl(ids, list));

        return updateResponse;
    }

    protected CreateResponse create(Metacard metacard) throws IngestException {
        return create(Arrays.asList(metacard));
    }

    protected CreateResponse create(List<Metacard> metacards) throws IngestException {
        return createIn(metacards, provider);
    }

    protected static CreateResponse createIn(List<Metacard> metacards, SolrCatalogProvider solrProvider)
        throws IngestException {
        CreateResponse createResponse = solrProvider.create(new CreateRequestImpl(metacards));

        return createResponse;
    }

}
