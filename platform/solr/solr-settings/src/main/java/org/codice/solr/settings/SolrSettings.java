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
package org.codice.solr.settings;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.encryption.EncryptionService;
import java.io.File;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
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
  protected static String solrUseBasicAuth;
  protected static String httpsCipherSuites;
  protected static String httpsProtocols;
  protected static String solrHttpUrl;
  private static String solrDataDir;
  private static boolean disableTextPath;
  private static boolean inMemory;
  private static Double nearestNeighborDistanceLimit;
  private static boolean forceAutoCommit;
  private static String clientType;
  private static EncryptionService encryptionService;
  private static String encryptedPassword;
  private static String username;

  /**
   * To make testing possible, thee variables are not initialized in a static block. Instead, they
   * are initialized in a package private static method.
   */
  static {
    loadSystemProperties();
  }

  /**
   * After settings the system properties in a test method, invoke this method to read those
   * properties. This method is not intended called by other classes in a production system.
   */
  @VisibleForTesting
  static void loadSystemProperties() {
    solrDataDir =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.data.dir"));
    solrHttpUrl =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.http.url"));
    solrUseBasicAuth =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.useBasicAuth"));
    httpsCipherSuites =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("https.cipherSuites"));
    httpsProtocols =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("https.protocols"));
    // TODO: HOW MANY OF SOLR'S SETTINGS SHOULD BE COLLECTED HERE? SOME? ALL?
    //    clientType =
    //        AccessController.doPrivileged(
    //            (PrivilegedAction<String>) () -> System.getProperty("solr.client",
    // "HttpSolrClient"));
    encryptedPassword =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.password"));
    username =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.username"));
  }

  static String concatenatePaths(String first, String more) {
    return Paths.get(first, more).toString();
  }

  public static String getDefaultSchemaXml() {
    return DEFAULT_SCHEMA_XML;
  }

  public static String getDefaultSolrconfigXml() {
    return DEFAULT_SOLRCONFIG_XML;
  }

  /**
   * Return the value of the system property for Solr data directory. This is the root of where the
   * data for individual cores exist. Individual cores have their own data directories. * @return
   * String representation of a file path.
   */
  public static String getRootDataDir() {
    return solrDataDir;
  }

  public static boolean isSolrDataDirWritable() {
    return StringUtils.isNotEmpty(getRootDataDir()) && new File(getRootDataDir()).canWrite();
  }

  public static String getCoreUrl(String coreName) {
    return getUrl() + "/" + coreName;
  }

  public static String getCoreDataDir(String coreName) {
    return concatenatePaths(getCoreDir(coreName), "data");
  }

  public static String getCoreDir(String coreName) {
    return concatenatePaths(getRootDataDir(), coreName);
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported cipher suites
   * do not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedCipherSuites() {
    return commaSeparatedToArray(httpsCipherSuites);
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported protocols do
   * not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedProtocols() {
    return commaSeparatedToArray(httpsProtocols);
  }

  /**
   * Gets the Solr server HTTP address.
   *
   * @return Solr server HTTP address
   */
  public static String getUrl() {
    return solrHttpUrl;
  }

  public static boolean useBasicAuth() {
    return Boolean.valueOf(solrUseBasicAuth);
  }

  public static boolean useTls() {
    return StringUtils.startsWithIgnoreCase(getUrl(), "https");
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {

    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }

  public static void setDisableTextPath(boolean bool) {
    disableTextPath = bool;
  }

  /**
   * Return the string that indicate what kind of Solr configuration the application should use.
   * E.g. cloud, server, embedded.
   *
   * @return Name of Solr client
   */
  //  public static String getClientType() {
  //    return clientType;
  //  }
  public static void setEncryptionService(EncryptionService service) {
    encryptionService = service;
  }

  public static String getSolrUsername() {
    return username;
  }

  public static String getPlainTextSolrPassword() {
    Validate.notNull(
        encryptionService,
        "Provide class with the encryption service before invoking this method.");
    return encryptionService.decrypt(encryptedPassword);
  }

  public static String encryptString(String plainText) {
    Validate.notNull(
        encryptionService,
        "Provide class with the encryption service before invoking this method.");
    return encryptionService.encrypt(plainText);
  }

  /**
   * The properties inMemory, nearestNeighborLimitDistance, forceAutocommit, and disapblePathText
   * are set in the RemoteSolrCatalogProvider object, which in turn is tied to the adminUI. The
   * SolrProperties class (this class) is embedded in different places and that means the state is
   * potentially invalid in one place or another. However, this class replaces an older singleton
   * class (ConfigurationStore) which would have suffered from the same problem.
   *
   * <p>A solution is to inject the RemoteSolrCatalogProvider into the SolrClientFactoryImpl when a
   * new instance of the SolrClientFactoryImpl is created. However, that could create a circular
   * dependency because platform classes should not depend on catalog classes.
   *
   * <p>A solution to this second problem is to create a RemoteSolrCatalogSettings interface in the
   * platform layer that both this class can reference and the RemoteSolrCatalogSettings can
   * implement.
   */
  public static boolean isDisableTextPath() {
    return disableTextPath;
  }

  public static boolean isInMemory() {
    return inMemory;
  }

  public static void setInMemory(boolean bool) {
    inMemory = bool;
  }

  public static Double getNearestNeighborDistanceLimit() {
    return nearestNeighborDistanceLimit;
  }

  public static void setNearestNeighborDistanceLimit(Double value) {
    nearestNeighborDistanceLimit = Math.abs(value);
  }

  /** @return true, if forcing auto commit is turned on */
  public static boolean isForceAutoCommit() {
    return forceAutoCommit;
  }

  /**
   * @param bool When set to true, this will force a soft commit upon every solr transaction such as
   *     insert, delete,
   */
  public static void setForceAutoCommit(boolean bool) {
    forceAutoCommit = bool;
  }
}
