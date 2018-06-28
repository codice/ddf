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
import com.google.common.io.Closeables;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.schema.IndexSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @deprecated Removing support for embedded and standalone Solr in the future.
 *     <p>Maintain embedded Solr configuration and schema files.
 *     <p><i>Note:</i> The corresponding {@link SolrConfig} and {@link IndexSchema} will be
 *     constructed the first time they are retrieved.
 */
@Deprecated
class EmbeddedSolrFiles {
  private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddedSolrFiles.class);

  private final String coreName;
  private final String configName;
  private final File configFile;
  private final String schemaName;
  private final File schemaFile;
  private final ConfigurationFileProxy configProxy;
  private volatile SolrConfig solrConfig = null;
  private volatile IndexSchema schemaIndex = null;
  private volatile String dataDirPath = null;

  @VisibleForTesting // required by Spock to spy on an existing object
  EmbeddedSolrFiles() {
    this.coreName = null;
    this.configName = null;
    this.configFile = null;
    this.schemaName = null;
    this.schemaFile = null;
    this.configProxy = null;
  }

  /**
   * Constructor.
   *
   * @param coreName name of the Solr core
   * @param configXml name of the Solr configuration file
   * @param schemaXmls file names of the Solr core schemas to attempt to load (will start with the
   *     first and fallback to the others in order if unavailable)
   * @param configProxy {@link ConfigurationFileProxy} instance to use
   * @throws IllegalArgumentException if <code>coreName</code>, <code>configXml</code>, <code>
   *     schemaXmls</code>, or <code>configProxy</code> is <code>null</code> or if unable to find
   *     any files
   */
  public EmbeddedSolrFiles(
      String coreName, String configXml, String[] schemaXmls, ConfigurationFileProxy configProxy) {
    Validate.notNull(coreName, "invalid null Solr core name");
    Validate.notNull(configXml, "invalid null Solr config file");
    Validate.notNull(schemaXmls, "invalid null Solr schema files");
    Validate.notEmpty(schemaXmls, "missing Solr schema files");
    Validate.noNullElements(schemaXmls, "invalid null Solr schema file");
    Validate.notNull(configProxy, "invalid null Solr config proxy");
    this.coreName = coreName;
    this.configName = configXml;
    this.configProxy = configProxy;
    this.configFile = FileUtils.toFile(configProxy.getResource(configXml, coreName));
    Validate.notNull(configFile, "Unable to find Solr configuration file: " + configXml);
    File solrSchemaFile = null;
    String schemaXml = null;

    for (final String s : schemaXmls) {
      schemaXml = s;
      solrSchemaFile = FileUtils.toFile(configProxy.getResource(schemaXml, coreName));
      if (solrSchemaFile != null) {
        break;
      }
    }
    Validate.notNull(
        solrSchemaFile, "Unable to find Solr schema file(s): " + Arrays.toString(schemaXmls));
    this.schemaFile = solrSchemaFile;
    this.schemaName = schemaXml;
  }

  /**
   * Gets the schema file.
   *
   * @return the corresponding schema file
   */
  public File getSchemaFile() {
    return schemaFile;
  }

  /**
   * Gets the configuration file.
   *
   * @return the corresponding config file
   */
  public File getConfigFile() {
    return configFile;
  }

  /**
   * Gets the home directory where the configuration resides.
   *
   * @return the home directory where the configuration resides
   */
  public File getConfigHome() {
    return configFile.getParentFile();
  }

  /**
   * Creates or retrieves the schema index corresponding to this set of Solr files.
   *
   * @return a corresponding index for the schema
   * @throws IllegalArgumentException if unable to load or parse the corresponding schema or config
   */
  public IndexSchema getSchemaIndex() {
    if (schemaIndex == null) {
      final SolrConfig cfg = getConfig(); // make sure it is initialized
      InputStream is = null;

      LOGGER.debug(
          "Loading and creating index for {} schema using file [{} ({})]",
          coreName,
          schemaName,
          schemaFile);
      try {
        is = FileUtils.openInputStream(schemaFile);
        this.schemaIndex = newIndexSchema(cfg, schemaName, new InputSource(is));
      } catch (IOException e) {
        LOGGER.debug(
            "failed to open {} Solr schema file [{} ({})]", coreName, schemaName, schemaFile, e);
        throw new IllegalArgumentException("Unable to open Solr schema file: " + schemaName, e);
      } catch (RuntimeException e) { // thrown as is by IndexSchema()
        LOGGER.debug(
            "failed to parse {} Solr schema file [{} ({})]", coreName, schemaName, schemaFile, e);
        throw new IllegalArgumentException("Unable to parse Solr schema file: " + schemaName, e);
      } finally {
        Closeables.closeQuietly(is);
      }
    }
    return schemaIndex;
  }

  /**
   * Creates or retrieves the Solr config for this set of Solr files.
   *
   * @return a corresponding Solr config
   * @throws IllegalArgumentException if unable to load or parse the corresponding config
   */
  public SolrConfig getConfig() {
    if (solrConfig == null) {
      InputStream is = null;

      LOGGER.debug(
          "Loading and creating Solr config for {} using file [{} ({})]",
          coreName,
          configName,
          configFile);
      try {
        is = FileUtils.openInputStream(configFile);
      } catch (IOException e) {
        LOGGER.debug(
            "failed to open {} Solr config file [{} ({})]", coreName, configName, configFile, e);
        throw new IllegalArgumentException(
            "Unable to open Solr configuration file: " + configName, e);
      }
      try {
        this.solrConfig =
            newConfig(getConfigHome().getParentFile().toPath(), configName, new InputSource(is));
      } catch (ParserConfigurationException | IOException | SAXException e) {

        LOGGER.debug(
            "failed to parse {} Solr config file [{} ({})]", coreName, configName, configFile, e);
        throw new IllegalArgumentException(
            "Unable to parse Solr configuration file: " + configName, e);
      } finally {
        Closeables.closeQuietly(is);
      }
    }
    return solrConfig;
  }

  /**
   * Retrieves the path to the data directory used for the Solr embedded files.
   *
   * @return the corresponding path to the data directory if any
   */
  @Nullable
  public String getDataDirPath() {
    if (dataDirPath == null) {
      final File dataDir = configProxy.getDataDirectory();

      if (dataDir != null) {
        dataDirPath = Paths.get(dataDir.getAbsolutePath(), coreName, "data").toString();
        LOGGER.debug("Solr({}): Using data directory [{}]", coreName, dataDirPath);
      }
    }
    return dataDirPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(coreName, configFile, schemaFile);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof EmbeddedSolrFiles) {
      final EmbeddedSolrFiles f = (EmbeddedSolrFiles) obj;

      return coreName.equals(f.coreName)
          && configFile.equals(f.configFile)
          && schemaFile.equals(f.schemaFile);
    }
    return false;
  }

  @Override
  public String toString() {
    return coreName + ',' + configName + ',' + schemaName;
  }

  @VisibleForTesting
  IndexSchema newIndexSchema(SolrConfig cfg, String name, InputSource source) {
    return new IndexSchema(cfg, name, source);
  }

  @VisibleForTesting
  SolrConfig newConfig(Path instanceDir, String name, InputSource is)
      throws ParserConfigurationException, IOException, SAXException {
    return new SolrConfig(instanceDir, name, is);
  }
}
