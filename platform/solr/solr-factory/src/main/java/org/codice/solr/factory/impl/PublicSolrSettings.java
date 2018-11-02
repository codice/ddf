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
import java.io.File;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * Convenience class for aggregating information about how the application is configured to
 * communicate with Solr. Several system properties are stored here for convenience and to
 * centralize validator of mandatory properties. The static variables are not final variables
 * initialized in a static block because that makes testing difficult or impossible. Instead, the
 * variables are private, with package-private getters methods and no setter methods. Restricting
 * access is important because they may contain sensetive information.
 */
public class PublicSolrSettings {

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

  static {
    loadSystemProperties();
  }

  {
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
  }

  PublicSolrSettings() {}

  /**
   * After settings the system properties in a test method, invoke this method to read those
   * properties.
   */
  @VisibleForTesting
  static void loadSystemProperties() {
    new PublicSolrSettings();
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

  /**
   * Return the value of the system property for Solr data directory. This is the root of where the
   * data for individual cores exist. Individual cores have their own data directories. * @return
   * String representation of a file path.
   */
  static String getRootDataDir() {
    return solrDataDir;
  }

  static boolean isSolrDataDirWritable() {
    return StringUtils.isNotEmpty(getRootDataDir()) && new File(getRootDataDir()).canWrite();
  }

  static String getCoreUrl(String coreName) {
    return getUrl() + "/" + coreName;
  }

  static String getCoreDataDir(String coreName) {
    return concatenatePaths(getCoreDir(coreName), "data");
  }

  static String getCoreDir(String coreName) {
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

  static boolean useBasicAuth() {
    return Boolean.valueOf(solrUseBasicAuth);
  }

  static boolean useTls() {
    return StringUtils.startsWithIgnoreCase(getUrl(), "https");
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {

    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }

  public static void setDisableTextPath(boolean bool) {
    disableTextPath = bool;
  }

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
