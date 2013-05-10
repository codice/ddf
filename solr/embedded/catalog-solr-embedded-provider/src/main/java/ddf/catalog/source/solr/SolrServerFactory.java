/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.solr;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.impl.CloudSolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.xml.sax.SAXException;

/**
 * Factory that creates {@link SolrServer} instances. Currently will create a
 * {@link EmbeddedSolrServer} instance.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public final class SolrServerFactory {

    private static final String DEFAULT_HTTP_ADDRESS = "http://localhost:8181/solr";

    private static final String DEFAULT_SCHEMA_XML = "schema.xml";

    private static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";

    private static final Logger LOGGER = Logger
            .getLogger(SolrServerFactory.class);

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
     * Creates an {@link HttpSolrServer} with the
     * {@link SolrServerFactory#DEFAULT_HTTP_ADDRESS} url.
     * 
     * @return SolrServer
     */
    static SolrServer getHttpSolrServer() {

        return new HttpSolrServer(DEFAULT_HTTP_ADDRESS);
    }

    /**
     * Provides an already instantiated {@link SolrServer} object. If an
     * instance has not already been instantiated, then the single instance will
     * be instantiated with the provided configuration file. If an instance
     * already exists, it cannot be overwritten with a new configuration.
     * 
     * @param solrConfigFileName
     *            the name of the solr configuration filename such as
     *            solrconfig.xml
     * @param schemaXml
     *            filename of the schema such as schema.xml
     * @param givenConfigFileProxy
     *            a ConfigurationFileProxy instance. If instance is
     *            <code>null</code>, a new {@link ConfigurationFileProxy} is
     *            used instead.
     * @return {@link SolrServer} instance
     */
    public static EmbeddedSolrServer getEmbeddedSolrServer(
            String solrConfigXml, String schemaXml,
            ConfigurationFileProxy givenConfigFileProxy) {

        LOGGER.info("Retrieving embedded solr with the following properties: ["
                + solrConfigXml + "," + schemaXml + "," + givenConfigFileProxy
                + "]");

        String solrConfigFileName = DEFAULT_SOLRCONFIG_XML;

        String schemaFileName = DEFAULT_SCHEMA_XML;

        if (isNotBlank(solrConfigXml)) {
            solrConfigFileName = solrConfigXml;
        }

        if (isNotBlank(schemaXml)) {
            schemaFileName = schemaXml;
        }

        File solrConfigFile = null;

        File solrConfigHome = null;

        ConfigurationFileProxy configProxy = givenConfigFileProxy;

        if (givenConfigFileProxy == null) {
            configProxy = new ConfigurationFileProxy(null,
                    ConfigurationStore.getInstance());
        }

        File configurationDir = new File(ConfigurationFileProxy.DEFAULT_SOLR_CONFIG_PARENT_DIR,
                ConfigurationFileProxy.SOLR_CONFIG_LOCATION_IN_BUNDLE);
        configProxy.writeBundleFilesTo(configurationDir);

        try {
            URL url = configProxy.getResource(solrConfigFileName);

            LOGGER.info("Solr config url: " + url);

            solrConfigFile = new File(new URI(url.toString()).getPath());

            solrConfigHome = new File(solrConfigFile.getParent());
        } catch (URISyntaxException e1) {
            LOGGER.warn(e1);
        }

        SolrConfig solrConfig = null;
        IndexSchema indexSchema = null;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(
                    SolrServerFactory.class.getClassLoader());

            // NamedSPILoader uses the thread context classloader to lookup
            // codecs, posting formats, and analyzers
            solrConfig = new SolrConfig(solrConfigHome.getParent(),
                    solrConfigFileName, null);
            indexSchema = new IndexSchema(solrConfig, schemaFileName, null);
        } catch (ParserConfigurationException e) {
            LOGGER.warn(e);
        } catch (IOException e) {
            LOGGER.warn(e);
        } catch (SAXException e) {
            LOGGER.warn(e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        // this is necessary as a workaround to an
        // incompatibility introduced in between Solr 4.0 to 4.1
        CoreContainer container = new CoreContainer() {
            {
                initShardHandler(null);
            }
        };
        CoreDescriptor dcore = new CoreDescriptor(container, "", solrConfig
                .getResourceLoader().getInstanceDir());
        dcore.setConfigName(solrConfig.getResourceName());
        dcore.setSchemaName(indexSchema.getResourceName());

        File dataDir = configProxy.getDataDirectory();
        LOGGER.info("Using data directory [" + dataDir + "]");
        SolrCore core = new SolrCore(null, dataDir.getAbsolutePath(),
                solrConfig, indexSchema, dcore);
        container.register("core1", core, false);

        return new EmbeddedSolrServer(container, "core1");
    }

}