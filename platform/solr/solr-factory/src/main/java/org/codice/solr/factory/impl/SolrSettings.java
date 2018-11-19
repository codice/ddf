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

import ddf.security.encryption.EncryptionService;
import java.io.File;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Convenience class for aggregating information about how the application is configured to
 * communicate with Solr. Several system properties are stored here for convenience, and to
 * centralize validation of mandatory properties. Settings controls by metatypes are also available.
 * Methods for validating and transforming the raw properties/settings are also collected here.
 *
 * <p>The static variables are not final variables for two reasons. First, it makes testing
 * difficult or impossible. Second, some properties are set in a properties file, but others are
 * configured at runtime.
 */
public class SolrSettings {

  private static final String DEFAULT_SCHEMA_XML = "schema.xml";
  private static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";
  private static EncryptionService encryptionService;
  private final Properties properties;

  public SolrSettings() {
    properties =
        AccessController.doPrivileged((PrivilegedAction<Properties>) () -> System.getProperties());
  }

  String concatenatePaths(String first, String more) {
    return Paths.get(first, more).toString();
  }

  public String getDefaultSchemaXml() {
    return DEFAULT_SCHEMA_XML;
  }

  public String getDefaultSolrconfigXml() {
    return DEFAULT_SOLRCONFIG_XML;
  }

  public boolean useTls() {
    return StringUtils.startsWithIgnoreCase(getUrl(), "https");
  }

  /**
   * Return the value of the system property for Solr data directory. This is the root of where the
   * data for individual cores exist. Individual cores have their own data directories.
   *
   * @return String representation of a file path.
   */
  public String getRootDataDir() {
    return properties.getProperty("solr.data.dir");
  }

  public boolean isSolrDataDirWritable() {
    return StringUtils.isNotEmpty(getRootDataDir()) && new File(getRootDataDir()).canWrite();
  }

  public String getCoreUrl(String coreName) {
    return getUrl() + "/" + coreName;
  }

  public String getCoreDataDir(String coreName) {
    return concatenatePaths(getCoreDir(coreName), "data");
  }

  public String getCoreDir(String coreName) {
    return concatenatePaths(getRootDataDir(), coreName);
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported cipher suites
   * do not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public String[] getSupportedCipherSuites() {
    return commaSeparatedToArray(properties.getProperty("https.cipherSuites"));
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported protocols do
   * not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public String[] getSupportedProtocols() {
    return commaSeparatedToArray(properties.getProperty("https.protocols"));
  }

  /**
   * Gets the Solr server HTTP address.
   *
   * @return Solr server HTTP address
   */
  public String getUrl() {
    return properties.getProperty("solr.http.url");
  }

  public boolean useBasicAuth() {
    return Boolean.valueOf(properties.getProperty("solr.useBasicAuth"));
  }

  private String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {

    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }

  public String getSolrUsername() {
    return properties.getProperty("solr.username");
  }

  public String getPlainTextSolrPassword() {
    Validate.notNull(
        encryptionService,
        "Provide class with the encryption service before invoking this method.");
    Validate.notNull(getEncryptedPassword(), "No password for Solr was set.");
    return encryptionService.decrypt(getEncryptedPassword());
  }

  private String getEncryptedPassword() {
    return properties.getProperty("solr.password");
  }

  public EncryptionService getEncryptionService() {
    return encryptionService;
  }

  public static void setEncryptionService(EncryptionService service) {
    encryptionService = service;
  }
}
