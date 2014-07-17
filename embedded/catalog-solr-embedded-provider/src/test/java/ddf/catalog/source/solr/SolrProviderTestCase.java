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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.cassandra.embedded.CassandraConfig;
import org.codice.ddf.cassandra.embedded.CassandraEmbeddedServer;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
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

    protected static TestSolrFilterBuilder filterBuilder = new TestSolrFilterBuilder();

    protected static SolrCatalogProvider provider = null;

    private static final int TEST_METHOD_NAME_INDEX = 3;

    private static final int ONE_SECOND = 1;
    private static final int TIME_STEP_10SECONDS = 10 * ONE_SECOND;
    private static final int TIME_STEP_30SECONDS = 30 * ONE_SECOND;
    private static final int A_LITTLE_WHILE = TIME_STEP_10SECONDS;

    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("RUNNING one-time setup.");

        // Start embedded cassandra server using the platform-app's CassandraEmbeddedServer
        // (Embedded Cassandra server required for Solr unit tests because Solr update chain
        // defined in solrconfig.xml specifes all entries in Solr be duplicated in Cassandra
        // using the CassandraUpdrateRequestProcessor)
        String workingDir = System.getProperty("user.dir") + File.separator + "target";
        System.setProperty("karaf.home", workingDir);
        CassandraConfig cassandraConfig = new CassandraConfig("DDF Cluster", 9160, 9042, 7000, 7001);
        cassandraConfig.setCommitLogFolder("/commitlog");
        cassandraConfig.setDataFolder("/data");
        cassandraConfig.setSavedCachesFolder("/saved_caches");
        CassandraEmbeddedServer cassandraServer = new CassandraEmbeddedServer(cassandraConfig);
     
/*WORKS - cassandra-unit approach for starting embedded Cassandra server
        System.setProperty("log4j.configuration", "file:" + workingDir + "log4j.properties");
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("/cassandra.yaml");
        Cluster cluster = new Cluster.Builder().addContactPoints("localhost").withPort(9042).build();
        Session session = cluster.connect();
        session.execute("CREATE KEYSPACE ddf WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}");
        session.execute("USE ddf");
*/ 
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

    protected List<Metacard> addMetacardWithModifiedDate(DateTime now) throws IngestException {
        List<Metacard> list = new ArrayList<Metacard>();
        MockMetacard m = new MockMetacard(Library.getFlagstaffRecord());
        m.setEffectiveDate(dateNow(now));
        list.add(m);
        create(list);
        return list;
    }

    protected List<Result> getResultsForFilteredQuery(Filter filter) throws UnsupportedQueryException {
        QueryImpl query = new QueryImpl(filter);

        SourceResponse sourceResponse = provider.query(new QueryRequestImpl(query));
        return sourceResponse.getResults();
    }

    protected Date dateAfterNow(DateTime now) {
        return now.plusSeconds(A_LITTLE_WHILE).toDate();
    }

    protected Date dateBeforeNow(DateTime now) {
        return now.minusSeconds(A_LITTLE_WHILE).toDate();
    }

    protected Date dateNow(DateTime now) {
        return now.toDate();
    }
}
