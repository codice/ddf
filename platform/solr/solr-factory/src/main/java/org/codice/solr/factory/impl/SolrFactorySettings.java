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

import static org.codice.solr.factory.impl.SolrHttpSettings.getSolrHttpUrl;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.commons.lang.StringUtils;

/**
 * Convenience class for aggregating information about how the application is configured to
 * communicate with Solr. Several system properties are stored here for convenience and to
 * centralize validator of mandatory properties. The static variables are not final variables
 * initialized in a static block because that makes testing difficult or impossible. Instead, the
 * variables are private, with package-private getters methods and no setter methods. Restricting
 * access is important because they may contain sensetive information.
 */
public class SolrFactorySettings {

  private static final String DEFAULT_SCHEMA_XML = "schema.xml";
  private static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";
  private static String solrDataDir;

  static {
    loadSystemProperties();
  }

  {
    solrDataDir = getProperty("solr.data.dir");
  }

  SolrFactorySettings() {}

  /**
   * After settings the system properties in a test method, invoke this method to read those
   * properties.
   */
  @VisibleForTesting
  static void loadSystemProperties() {
    new SolrFactorySettings();
  }

  static String getProperty(String propertyName) {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty(propertyName));
  }

  static String concatenatePaths(String first, String more) {
    return Paths.get(first, more).toString();
  }

  static String getDefaultSchemaXml() {
    return DEFAULT_SCHEMA_XML;
  }

  static String getDefaultSolrconfigXml() {
    return DEFAULT_SOLRCONFIG_XML;
  }

  static String getSolrDataDir() {
    return solrDataDir;
  }

  static boolean isSolrDataDirWritable() {
    return StringUtils.isNotEmpty(getSolrDataDir()) && new File(getSolrDataDir()).canWrite();
  }

  static String getCoreUrl(String coreName) {
    return getSolrHttpUrl() + "/" + coreName;
  }

  static String getCoreDataDir(String coreName) {
    return concatenatePaths(getCoreDir(coreName), "data");
  }

  static String getCoreDir(String coreName) {
    return concatenatePaths(getSolrDataDir(), coreName);
  }
}
