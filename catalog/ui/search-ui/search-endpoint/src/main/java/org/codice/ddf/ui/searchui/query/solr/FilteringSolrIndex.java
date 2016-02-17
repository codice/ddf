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
package org.codice.ddf.ui.searchui.query.solr;

import static org.codice.solr.factory.EmbeddedSolrFactory.IMMEMORY_SOLRCONFIG_XML;
import static org.codice.solr.factory.EmbeddedSolrFactory.getConfigFile;
import static org.codice.solr.factory.SolrServerFactory.DEFAULT_SCHEMA_XML;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.codice.solr.factory.ConfigurationFileProxy;
import org.codice.solr.factory.ConfigurationStore;
import org.codice.solr.factory.EmbeddedSolrFactory;
import org.codice.solr.factory.SolrCoreContainer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrFilterDelegateFactoryImpl;

public class FilteringSolrIndex {

    private static volatile IndexSchema indexSchema;

    private SolrCatalogProvider provider;

    FilteringSolrIndex(SolrCatalogProvider provider) {
        this.provider = provider;
    }

    public FilteringSolrIndex(String queryId, FilterAdapter filterAdapter, QueryRequest request) {
        this(createInMemorySolrProvider(queryId, filterAdapter, request));
    }

    private static SolrCatalogProvider createInMemorySolrProvider(String queryId,
            FilterAdapter filterAdapter, QueryRequest request) {
        ConfigurationStore.getInstance()
                .setInMemory(true);
        ConfigurationStore.getInstance()
                .setForceAutoCommit(true);
        ConfigurationFileProxy configurationFileProxy = new ConfigurationFileProxy(
                ConfigurationStore.getInstance());

        SolrFilterDelegateFactory solrFilterDelegateFactory = new SolrFilterDelegateFactoryImpl();

        return new SolrCatalogProvider(createSolrServer(queryId, configurationFileProxy),
                filterAdapter,
                solrFilterDelegateFactory,
                new FilteringDynamicSchemaResolver(filterAdapter,
                        solrFilterDelegateFactory,
                        request));
    }

    private static EmbeddedSolrServer createSolrServer(String coreName,
            ConfigurationFileProxy configProxy) {

        File configFile = getConfigFile(IMMEMORY_SOLRCONFIG_XML, configProxy);
        File schemaFile = getConfigFile(DEFAULT_SCHEMA_XML, configProxy);
        File solrConfigHome = new File(configFile.getParent());

        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(EmbeddedSolrFactory.class.getClassLoader());

            SolrConfig solrConfig = new SolrConfig(Paths.get(solrConfigHome.getParent()),
                    IMMEMORY_SOLRCONFIG_XML,
                    new InputSource(FileUtils.openInputStream(configFile)));

            if (indexSchema == null) {
                indexSchema = new IndexSchema(solrConfig,
                        DEFAULT_SCHEMA_XML,
                        new InputSource(FileUtils.openInputStream(schemaFile)));
            }
            SolrResourceLoader loader = new SolrResourceLoader(Paths.get(solrConfigHome.getAbsolutePath()));
            SolrCoreContainer container = new SolrCoreContainer(loader);

            CoreDescriptor coreDescriptor = new CoreDescriptor(container,
                    coreName,
                    solrConfig.getResourceLoader()
                            .getInstancePath()
                            .toString());
            SolrCore core = new SolrCore(coreName,
                    null,
                    solrConfig,
                    indexSchema,
                    null,
                    coreDescriptor,
                    null,
                    null,
                    null);

            container.register(coreName, core, false);

            return new EmbeddedSolrServer(container, coreName);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Unable to parse Solr configuration file", e);
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(tccl);
        }
    }

    /**
     * Must be synchronized since force auto commit is enabled.  If too many commits happen at
     * the same time, performance is impacted and might log “too many warming searchers” warnings.
     */
    public synchronized CreateResponse add(List<Result> results) throws IngestException {
        return provider.create(new CreateRequestImpl(getMetacards(results)));
    }

    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {
        return provider.query(request);
    }

    public void shutdown() {
        provider.shutdown();
    }

    private List<Metacard> getMetacards(List<Result> results) {
        List<Metacard> metacards = new ArrayList<>(results.size());

        for (Result result : results) {
            if (result != null && result.getMetacard() != null) {
                metacards.add(result.getMetacard());
            }
        }

        return metacards;
    }
}
