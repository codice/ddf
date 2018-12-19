/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.solr.factory.impl;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.IndexDeletionPolicyWrapper;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.update.UpdateHandler;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to create new {@link EmbeddedSolrServer} clients. <br>
 * Uses the following system properties when creating an instance:
 *
 * <ul>
 *   <li>solr.data.dir: Absolute path to the directory where the Solr data will be stored
 * </ul>
 */
@Deprecated
public class EmbeddedSolrFactory implements SolrClientFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedSolrFactory.class);

  private static final Map<EmbeddedSolrFiles, IndexSchema> INDEX_CACHE = new ConcurrentHashMap<>();

  public static final String IMMEMORY_SOLRCONFIG_XML = "solrconfig-inmemory.xml";

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String core) {
    final ConfigurationStore configStore = ConfigurationStore.getInstance();

    if (System.getProperty("solr.data.dir") != null) {
      configStore.setDataDirectoryPath(System.getProperty("solr.data.dir"));
    }
    return newClient(
        core,
        HttpSolrClientFactory.DEFAULT_SOLRCONFIG_XML,
        HttpSolrClientFactory.DEFAULT_SCHEMA_XML,
        configStore,
        new ConfigurationFileProxy(configStore));
  }

  /**
   * Requests the creation of a new embedded {@code SolrClient} for a specific Solr core name.
   *
   * @param core the name of the Solr core to create to create a client for
   * @param configXml the name of the Solr configuration file
   * @param schemaXml the name of the Solr core schema
   * @param configStore the {@link ConfigurationStore} instance to use
   * @param configProxy the {@link ConfigurationFileProxy} instance to use
   * @return the newly created {@code SolrClient}
   * @throws IllegalArgumentException if <code>core</code>, <code>configXml</code>, <code>schemaXml
   *     </code>, <code>configStore</code> or <code>configProxy</code> is <code>null</code>
   */
  public org.codice.solr.client.solrj.SolrClient newClient(
      String core,
      String configXml,
      String schemaXml,
      ConfigurationStore configStore,
      ConfigurationFileProxy configProxy) {
    Validate.notNull(core, "invalid null Solr core name");
    Validate.notNull(configXml, "invalid null Solr config file");
    Validate.notNull(schemaXml, "invalid null Solr schema file");
    Validate.notNull(configStore, "invalid null Solr config store");
    Validate.notNull(configProxy, "invalid null Solr config proxy");
    LOGGER.debug("Solr({}): Creating an embedded Solr client", core);
    return new SolrClientAdapter(
        core, () -> getEmbeddedSolrServer(core, configXml, schemaXml, configStore, configProxy));
  }

  /**
   * Creates a new {@link EmbeddedSolrServer} using the Solr core and configuration file names,
   * schema and configuration file proxy provided.
   *
   * @param coreName name of the Solr core
   * @param configXml name of the Solr configuration file
   * @param schemaXml the name of the Solr core schema
   * @param configStore the {@link ConfigurationStore} instance to use
   * @param configProxy {@link ConfigurationFileProxy} instance to use
   * @return a new {@link EmbeddedSolrServer} instance
   * @throws IllegalArgumentException if <code>coreName</code>, <code>configXml</code>, <code>
   *     schemaXml</code>, <code>configStore</code>, or <code>configProxy</code> is <code>null
   *     </code> or if it cannot find the Solr files
   */
  @VisibleForTesting
  EmbeddedSolrServer getEmbeddedSolrServer(
      String coreName,
      String configXml,
      String schemaXml,
      ConfigurationStore configStore,
      ConfigurationFileProxy configProxy) {
    if (!configStore.isInMemory()) {
      configProxy.writeSolrConfiguration(coreName);
    }
    final EmbeddedSolrFiles files =
        newFiles(coreName, configXml, new String[] {schemaXml, "managed-schema"}, configProxy);

    LOGGER.debug(
        "Solr({}): Retrieving embedded solr with the following properties: [{},{}]",
        coreName,
        files,
        configProxy);
    final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

    try (final Closer closer = new Closer()) {
      // NamedSPILoader uses the thread context classloader to lookup
      // codecs, posting formats, and analyzers
      Thread.currentThread().setContextClassLoader(EmbeddedSolrFactory.class.getClassLoader());
      final SolrResourceLoader loader = closer.with(newLoader(files.getConfigHome().toPath()));
      final SolrCoreContainer container = newContainer(loader);
      final SolrConfig solrConfig = files.getConfig();
      final CoreDescriptor coreDescriptor =
          newDescriptor(
              coreName, solrConfig.getResourceLoader().getInstancePath(), new Properties(), false);
      final IndexSchema indexSchema =
          EmbeddedSolrFactory.INDEX_CACHE.computeIfAbsent(files, EmbeddedSolrFiles::getSchemaIndex);
      final SolrCore core =
          closer.with(
              newCore(
                  container,
                  coreName,
                  files.getDataDirPath(),
                  solrConfig,
                  indexSchema,
                  null,
                  coreDescriptor,
                  null,
                  null,
                  null,
                  false));

      container.register(coreName, core, false, true);
      return closer.returning(newServer(container, coreName));
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  @VisibleForTesting
  EmbeddedSolrFiles newFiles(
      String coreName, String configXml, String[] schemaXmls, ConfigurationFileProxy configProxy) {
    return new EmbeddedSolrFiles(coreName, configXml, schemaXmls, configProxy);
  }

  @VisibleForTesting
  SolrResourceLoader newLoader(Path configHome) {
    return new SolrResourceLoader(configHome);
  }

  @VisibleForTesting
  SolrCoreContainer newContainer(SolrResourceLoader loader) {
    return new SolrCoreContainer(loader);
  }

  @VisibleForTesting
  CoreDescriptor newDescriptor(
      String coreName, Path instancePath, Properties properties, boolean isZooKeeperAware) {
    return new CoreDescriptor(coreName, instancePath, properties, isZooKeeperAware);
  }

  @VisibleForTesting
  EmbeddedSolrServer newServer(SolrCoreContainer container, String coreName) {
    return new EmbeddedSolrServer(container, coreName);
  }

  @VisibleForTesting
  @SuppressWarnings({
    "squid:S00107", /* parameters are required by the SolrCore API */
    "PMD.ExcessiveParameterList" /* parameters are required by the SolrCore API */
  })
  SolrCore newCore(
      SolrCoreContainer container,
      String coreName,
      String dataDirPath,
      SolrConfig solrConfig,
      IndexSchema schema,
      @Nullable NamedList configSetProperties,
      CoreDescriptor coreDescriptor,
      @Nullable UpdateHandler updateHandler,
      @Nullable IndexDeletionPolicyWrapper delPolicy,
      @Nullable SolrCore prev,
      boolean reload) {
    return new SolrCore(
        container,
        coreName,
        dataDirPath,
        solrConfig,
        schema,
        configSetProperties,
        coreDescriptor,
        updateHandler,
        delPolicy,
        prev,
        reload);
  }

  @VisibleForTesting
  static void resetIndexCache(Map<EmbeddedSolrFiles, IndexSchema> cache) {
    EmbeddedSolrFactory.INDEX_CACHE.clear();
    EmbeddedSolrFactory.INDEX_CACHE.putAll(cache);
  }

  @VisibleForTesting
  static boolean indexCacheEquals(Map<EmbeddedSolrFiles, IndexSchema> cache) {
    return EmbeddedSolrFactory.INDEX_CACHE.equals(cache);
  }
}
