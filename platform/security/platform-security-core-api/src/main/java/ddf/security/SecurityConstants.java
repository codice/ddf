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
package ddf.security;

import java.security.KeyStore;
import java.security.KeyStoreException;

public final class SecurityConstants {
  /**
   * String used to retrieve the security LOGGER in each class that wishes to log to the security
   * log. Guarantees that all classes use the same security LOGGER.
   */
  public static final String SECURITY_LOGGER = "securityLogger";

  /**
   * Property name for the security subject. The subject is added to incoming requests under this
   * property name. The security framework retrieves the subject using this property name in order
   * to perform any security operations involving the subject of the request.
   */
  public static final String SECURITY_SUBJECT = "ddf.security.subject";

  /**
   * Property name for the java security subject. The java subject is added to incoming requests
   * under this property name. The security framework retrieves the java subject using this property
   * name in order to perform any security operations involving the java subject of the request.
   */
  public static final String SECURITY_JAVA_SUBJECT = "ddf.security.java.subject";

  /** Property key to obtain the saml assertion from a query request / response. */
  public static final String SAML_ASSERTION = "saml.assertion";

  /** Property key to obtain the legacy saml cookie from an incoming HTTP Request */
  public static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

  /** Property key to obtain the legacy saml cookie reference from an incoming HTTP request */
  public static final String SAML_COOKIE_REF = "or.codice.websso.saml.ref";

  /** Name of the header containing the saml assertion for HTTP requests/responses */
  public static final String SAML_HEADER_NAME = "Authorization";

  /** Keystore/truststore related system property keys */
  public static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";

  public static final String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

  public static final String KEYSTORE_PATH = "javax.net.ssl.keyStore";

  public static final String KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";

  public static final String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  public static final String TRUSTSTORE_PATH = "javax.net.ssl.trustStore";

  public static final String TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

  /**
   * Helper method for retrieving the keystore
   *
   * @return The path to the keystore
   */
  public static String getKeystorePath() {
    return System.getProperty(KEYSTORE_PATH);
  }

  /**
   * Helper method for retrieving the keystore password
   *
   * @return The keystore password
   */
  public static String getKeystorePassword() {
    return System.getProperty(KEYSTORE_PASSWORD);
  }

  /**
   * Helper method for retrieving the truststore
   *
   * @return The path to the truststore
   */
  public static String getTruststorePath() {
    return System.getProperty(TRUSTSTORE_PATH);
  }

  /**
   * Helper method for retrieving the truststore password
   *
   * @return The truststore password
   */
  public static String getTruststorePassword() {
    return System.getProperty(TRUSTSTORE_PASSWORD);
  }

  public static KeyStore newKeystore() throws KeyStoreException {
    return KeyStore.getInstance(System.getProperty(SecurityConstants.KEYSTORE_TYPE));
  }

  public static KeyStore newTruststore() throws KeyStoreException {
    return KeyStore.getInstance(System.getProperty(SecurityConstants.TRUSTSTORE_TYPE));
  }

  private SecurityConstants() {}
}
