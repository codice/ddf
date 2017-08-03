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
package org.codice.solr.factory.impl;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.DirectoryFactory;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.util.concurrent.Futures;

/**
 * Factory class used to create new {@link EmbeddedSolrServer} clients.
 * <br/>
 * Uses the following system properties when creating an instance:
 * <ul>
 * <li>solr.data.dir: Absolute path to the directory where the Solr data will be stored</li>
 * </ul>
 */
public class EmbeddedSolrFactory implements SolrClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedSolrFactory.class);

    private static final String DEFAULT_EMBEDDED_CORE_NAME = "embedded";

    public static final String IMMEMORY_SOLRCONFIG_XML = "solrconfig-inmemory.xml";

    @Override
    public Future<SolrClient> newClient(String core) {
        ConfigurationStore configStore = ConfigurationStore.getInstance();

        if (System.getProperty("solr.data.dir") != null) {
            configStore.setDataDirectoryPath(System.getProperty("solr.data.dir"));
        }

        ConfigurationFileProxy configProxy = new ConfigurationFileProxy(configStore);
        return Futures.immediateFuture(getEmbeddedSolrServer(core,
                HttpSolrClientFactory.DEFAULT_SOLRCONFIG_XML,
                null,
                configProxy));
    }

    /**
     * Creates a new {@link EmbeddedSolrServer} using the default Solr core name
     * ({@value #DEFAULT_EMBEDDED_CORE_NAME}), configuration file
     * ({@value HttpSolrClientFactory#DEFAULT_SOLRCONFIG_XML}) and schema.
     *
     * @return a new {@link EmbeddedSolrServer} instance
     */
    public static SolrClient getEmbeddedSolrServer() {
        return getEmbeddedSolrServer(DEFAULT_EMBEDDED_CORE_NAME,
                HttpSolrClientFactory.DEFAULT_SOLRCONFIG_XML,
                null,
                null);
    }

    /**
     * Creates a new {@link EmbeddedSolrServer} using the Solr configuration file provided. Uses
     * the default core name ({@value #DEFAULT_EMBEDDED_CORE_NAME}) and schema.
     *
     * @param solrConfigXml name of the Solr configuration file. Defaults to
     *                      {@value HttpSolrClientFactory#DEFAULT_SOLRCONFIG_XML} if
     *                      {@code null}.
     * @return a new {@link EmbeddedSolrServer} instance
     */
    public static EmbeddedSolrServer getEmbeddedSolrServer(@Nullable String solrConfigXml) {
        return getEmbeddedSolrServer(DEFAULT_EMBEDDED_CORE_NAME, solrConfigXml, null, null);
    }

    /**
     * Creates a new {@link EmbeddedSolrServer} using the Solr core and configuration file names,
     * schema and configuration file proxy provided.
     *
     * @param coreName             name of the Solr core
     * @param solrConfigXml        name of the Solr configuration file. Defaults to
     *                             {@value HttpSolrClientFactory#DEFAULT_SOLRCONFIG_XML} if
     *                             {@code null}.
     * @param schemaXml            file name of the Solr core schema. Defaults to
     *                             {@value HttpSolrClientFactory#DEFAULT_SCHEMA_XML} if
     *                             {@code null}.
     * @param givenConfigFileProxy {@link ConfigurationFileProxy} instance to use. If {@code null},
     *                             a new {@link ConfigurationFileProxy} will be used.
     * @return a new {@link EmbeddedSolrServer} instance
     */
    public static EmbeddedSolrServer getEmbeddedSolrServer(String coreName,
            @Nullable String solrConfigXml, @Nullable String schemaXml,
            @Nullable ConfigurationFileProxy givenConfigFileProxy) {

        LOGGER.debug("Retrieving embedded solr with the following properties: [{},{},{}]",
                solrConfigXml,
                schemaXml,
                givenConfigFileProxy);

        String solrConfigFileName = HttpSolrClientFactory.DEFAULT_SOLRCONFIG_XML;
        String schemaFileName = HttpSolrClientFactory.DEFAULT_SCHEMA_XML;

        if (isNotBlank(solrConfigXml)) {
            solrConfigFileName = solrConfigXml;
        }

        if (isNotBlank(schemaXml)) {
            schemaFileName = schemaXml;
        }

        ConfigurationFileProxy configProxy = givenConfigFileProxy;

        if (givenConfigFileProxy == null) {
            configProxy = new ConfigurationFileProxy(ConfigurationStore.getInstance());
        }

        configProxy.writeSolrConfiguration(coreName);
        File solrConfigFile = getConfigFile(solrConfigFileName, configProxy, coreName);
        File solrSchemaFile = getConfigFile(schemaFileName, configProxy, coreName);

        if (solrSchemaFile == null) {
            solrSchemaFile = getConfigFile("managed-schema", configProxy, coreName);
            if (solrSchemaFile == null) {
                throw new IllegalArgumentException("Unable to find Solr schema file.");
            }
        }

        File solrConfigHome = new File(solrConfigFile.getParent());

        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(EmbeddedSolrFactory.class.getClassLoader());

            // NamedSPILoader uses the thread context classloader to lookup
            // codecs, posting formats, and analyzers
            SolrConfig solrConfig = new SolrConfig(Paths.get(solrConfigHome.getParent()),
                    solrConfigFileName,
                    new InputSource(FileUtils.openInputStream(solrConfigFile)));
            IndexSchema indexSchema = new IndexSchema(solrConfig,
                    schemaFileName,
                    new InputSource(FileUtils.openInputStream(solrSchemaFile)));
            SolrResourceLoader loader =
                    new SolrResourceLoader(Paths.get(solrConfigHome.getAbsolutePath()));
            SolrCoreContainer container = new SolrCoreContainer(loader);

            String dataDirPath = null;
            if (!ConfigurationStore.getInstance()
                    .isInMemory()) {
                File dataDir = configProxy.getDataDirectory();
                if (dataDir != null) {
                    dataDirPath = Paths.get(dataDir.getAbsolutePath(), coreName, "data")
                            .toString();
                    LOGGER.debug("Using data directory [{}]", dataDirPath);
                }
            } else {
                PluginInfo info = solrConfig.getPluginInfo(DirectoryFactory.class.getName());
                if (info != null && !"solr.RAMDirectoryFactory".equals(info.className)) {
                    LOGGER.debug("Using in-memory configuration without RAMDirectoryFactory.");
                }
            }
            CoreDescriptor coreDescriptor = new CoreDescriptor(coreName,
                    solrConfig.getResourceLoader().getInstancePath(),
                    new Properties(),
                    false
                    );

            SolrCore core = new SolrCore(container,
                    coreName,
                    dataDirPath,
                    solrConfig,
                    indexSchema,
                    null,
                    coreDescriptor,
                    null,
                    null,
                    null,
                    false);
            container.register(coreName, core, false, true);

            return new EmbeddedSolrServer(container, coreName);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new IllegalArgumentException(
                    "Unable to parse Solr configuration file: " + solrConfigFileName, e);
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(tccl);
        }
    }

    public static File getConfigFile(String configFileName, ConfigurationFileProxy configProxy,
            String core) {
        return FileUtils.toFile(configProxy.getResource(configFileName, core));
    }
}
