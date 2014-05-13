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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Factory that creates {@link SolrServer} instances. Currently will create a
 * {@link EmbeddedSolrServer} instance.
 * 
 */
public final class SolrServerFactory {

    private static final String DEFAULT_HTTP_ADDRESS = "http://localhost:8181/solr";

    private static final String DEFAULT_SCHEMA_XML = "schema.xml";

    private static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";

    private static final String DEFAULT_SOLR_XML = "solr.xml";

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrServerFactory.class);

    public static final String CORE_NAME = "core1";

    /** Hiding constructor **/
    private SolrServerFactory() {

    }

    /**
     * @return {@link SolrServer} instance
     */
    public static SolrServer getEmbeddedSolrServer() {
        return getEmbeddedSolrServer(DEFAULT_SOLRCONFIG_XML, null, null);
    }

    public static EmbeddedSolrServer getEmbeddedSolrServer(String solrConfigXml) {
        return getEmbeddedSolrServer(solrConfigXml, null, null);

    }

    public static SolrServer getHttpSolrServer(String url) {

        return new HttpSolrServer(url);
    }

    /**
     * Creates an {@link HttpSolrServer} with the {@link SolrServerFactory#DEFAULT_HTTP_ADDRESS}
     * url.
     * 
     * @return SolrServer
     */
    static SolrServer getHttpSolrServer() {

        return new HttpSolrServer(DEFAULT_HTTP_ADDRESS);
    }

    /**
     * Provides an already instantiated {@link SolrServer} object. If an instance has not already
     * been instantiated, then the single instance will be instantiated with the provided
     * configuration file. If an instance already exists, it cannot be overwritten with a new
     * configuration.
     * 
     * @param solrConfigXml
     *            the name of the solr configuration filename such as solrconfig.xml
     * @param schemaXml
     *            filename of the schema such as schema.xml
     * @param givenConfigFileProxy
     *            a ConfigurationFileProxy instance. If instance is <code>null</code>, a new
     *            {@link ConfigurationFileProxy} is used instead.
     * @return {@link SolrServer} instance
     */
    public static EmbeddedSolrServer getEmbeddedSolrServer(String solrConfigXml, String schemaXml,
            ConfigurationFileProxy givenConfigFileProxy) {

        LOGGER.debug("Retrieving embedded solr with the following properties: [{},{},{}]" , solrConfigXml, schemaXml, givenConfigFileProxy);

        String solrConfigFileName = DEFAULT_SOLRCONFIG_XML;
        String schemaFileName = DEFAULT_SCHEMA_XML;

        if (isNotBlank(solrConfigXml)) {
            solrConfigFileName = solrConfigXml;
        }

        if (isNotBlank(schemaXml)) {
            schemaFileName = schemaXml;
        }

        ConfigurationFileProxy configProxy = givenConfigFileProxy;

        if (givenConfigFileProxy == null) {
            configProxy = new ConfigurationFileProxy(null, ConfigurationStore.getInstance());
        }

        File configurationDir = new File(ConfigurationFileProxy.DEFAULT_SOLR_CONFIG_PARENT_DIR,
                ConfigurationFileProxy.SOLR_CONFIG_LOCATION_IN_BUNDLE);
        configProxy.writeBundleFilesTo(configurationDir);

        File solrConfigFile = getConfigFile(solrConfigFileName, configProxy);
        File solrSchemaFile = getConfigFile(schemaFileName, configProxy);
        File solrFile = getConfigFile(DEFAULT_SOLR_XML, configProxy);

        File solrConfigHome = new File(solrConfigFile.getParent());

        SolrConfig solrConfig = null;
        IndexSchema indexSchema = null;
        CoreContainer container = null;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(SolrServerFactory.class.getClassLoader());

            // NamedSPILoader uses the thread context classloader to lookup
            // codecs, posting formats, and analyzers
            solrConfig = new SolrConfig(solrConfigHome.getParent(), solrConfigFileName,
                    new InputSource(FileUtils.openInputStream(solrConfigFile)));
            indexSchema = new IndexSchema(solrConfig, schemaFileName, new InputSource(
                    FileUtils.openInputStream(solrSchemaFile)));
            container = CoreContainer.createAndLoad(solrConfigHome.getAbsolutePath(),
                    solrFile);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Parser configuration exception loading index schema", e);
        } catch (IOException e) {
            LOGGER.warn("IO exception loading index schema", e);
        } catch (SAXException e) {
            LOGGER.warn("SAX exception loading index schema", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        CoreDescriptor coreDescriptor = new CoreDescriptor(container, CORE_NAME, solrConfig
                .getResourceLoader().getInstanceDir());

        File dataDir = configProxy.getDataDirectory();
        LOGGER.debug("Using data directory [{}]", dataDir);
        SolrCore core = new SolrCore(CORE_NAME, dataDir.getAbsolutePath(), solrConfig, indexSchema,
                coreDescriptor);
        container.register(CORE_NAME, core, false);

        return new EmbeddedSolrServer(container, CORE_NAME);
    }

    private static File getConfigFile(String configFileName, ConfigurationFileProxy configProxy) {
        File result = null;
        try {
            URL url = configProxy.getResource(configFileName);
            result = new File(new URI(url.toString()).getPath());
        } catch (URISyntaxException e) {
            LOGGER.warn("URI exception loading configuration file", e);
        }
        return result;
    }

}
