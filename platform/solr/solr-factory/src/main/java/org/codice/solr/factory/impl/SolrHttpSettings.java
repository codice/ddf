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
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * Class to offload configuration-related tasks from the HttpSolrClientFactory. Some methods
 * are package private to protect keystore or other password information.
 */
public class SolrHttpSettings extends SolrFactorySettings {

  private static String keyStoreType;
  private static String solrUseBasicAuth;
  private static String httpsCipherSuites;
  private static String httpsProtocols;
  private static String keyStore;
  private static String keyStorePass;
  private static String solrHttpUrl;
  private static String trustStore;
  private static String trustStorePass;

  static {
    loadSystemProperties();
  }

  {
    solrHttpUrl = getProperty("solr.http.url");
    solrUseBasicAuth = getProperty("solr.useBasicAuth");
    trustStore = getProperty("javax.net.ssl.trustStore");
    trustStorePass = getProperty("javax.net.ssl.trustStorePassword");
    httpsCipherSuites = getProperty("https.cipherSuites");
    httpsProtocols = getProperty("https.protocols");
    keyStore = getProperty("javax.net.ssl.keyStore");
    keyStorePass = getProperty("javax.net.ssl.keyStorePassword");
    keyStoreType = getProperty("javax.net.ssl.keyStoreType");
  }

  /**
   * After settings the system properties in a test method, invoke this method to read those
   * properties.
   */
  @VisibleForTesting
  static void loadSystemProperties() {
    SolrFactorySettings.loadSystemProperties();
    new SolrHttpSettings();
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported cipher suites
   * do not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedCipherSuites() {
    return commaSeparatedToArray(getHttpsCipherSuites());
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported protocols do
   * not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedProtocols() {
    return commaSeparatedToArray(getHttpsProtocols());
  }

  /**
   * Gets the default Solr server secure HTTP address.
   *
   * @return Solr server secure HTTP address
   */
  public static String getDefaultHttpsAddress() {
    return getSolrHttpUrl();
  }

  static boolean useBasicAuth() {
    return Boolean.valueOf(getSolrUseBasicAuth());
  }

  static boolean isSslConfigured() {
    return Stream.of(getKeyStore(), getKeyStorePass(), getTrustStore(), getTrustStorePass())
        .noneMatch(StringUtils::isNotEmpty);
  }

  static boolean useTls() {
    return StringUtils.startsWithIgnoreCase(getSolrHttpUrl(), "https");
  }

  static String getKeyStoreType() {
    return keyStoreType;
  }

  static String getSolrUseBasicAuth() {
    return solrUseBasicAuth;
  }

  static String getHttpsCipherSuites() {
    return httpsCipherSuites;
  }

  static String getHttpsProtocols() {
    return httpsProtocols;
  }

  static String getKeyStore() {
    return keyStore;
  }

  static String getKeyStorePass() {
    return keyStorePass;
  }

  static String getSolrHttpUrl() {
    return solrHttpUrl;
  }

  static String getTrustStore() {
    return trustStore;
  }

  static String getTrustStorePass() {
    return trustStorePass;
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {

    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }
}
