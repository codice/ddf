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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;

/**
 * Class to offload configuration-related tasks from the HttpSolrClientFactory. Some methods are
 * package private to protect keystore or other password information.
 */
final class ProtectedSolrSettings extends PublicSolrSettings {

  private static String keyStoreType;
  private static String keyStore;
  private static String keyStorePass;
  private static String trustStore;
  private static String trustStorePass;

  static {
    loadSystemProperties();
  }

  {
    trustStore =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.trustStore"));
    trustStorePass =
        AccessController.doPrivileged(
            (PrivilegedAction<String>)
                () -> System.getProperty("javax.net.ssl.trustStorePassword"));
    keyStore =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStore"));
    keyStorePass =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStorePassword"));
    keyStoreType =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStoreType"));
  }

  /**
   * After settings the system properties in a test method, invoke this method to read those
   * properties.
   */
  @VisibleForTesting
  static void loadSystemProperties() {
    PublicSolrSettings.loadSystemProperties();
    new ProtectedSolrSettings();
  }

  static boolean isSslConfigured() {
    return Stream.of(getKeyStore(), getKeyStorePass(), getTrustStore(), getTrustStorePass())
        .noneMatch(StringUtils::isNotEmpty);
  }

  static String getKeyStoreType() {
    return keyStoreType;
  }

  static String getKeyStore() {
    return keyStore;
  }

  static String getKeyStorePass() {
    return keyStorePass;
  }

  static String getTrustStore() {
    return trustStore;
  }

  static String getTrustStorePass() {
    return trustStorePass;
  }
}
