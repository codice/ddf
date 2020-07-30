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
package org.codice.ddf.cxf.client;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.codice.ddf.configuration.PropertyResolver;

/**
 * Builder class for creating secure CXF client factory objects that can generate WebClients
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be * removed in a future version of the library. </b>
 */
public interface ClientBuilder<T> {

  SecureCxfClientFactory<T> build();

  /**
   * The endpoint the client should connect to.
   *
   * @param endpointUrl
   * @return
   */
  ClientBuilder<T> endpoint(String endpointUrl);

  /**
   * The JAX-RS interface class/type that represents the endpoint
   *
   * @param interfaceClass
   * @return
   */
  ClientBuilder<T> interfaceClass(Class<T> interfaceClass);

  ClientBuilder<T> entityProviders(List<? extends Object> entityProviders);

  ClientBuilder<T> interceptor(Interceptor<? extends Message> interceptor);

  /**
   * Turns the CN check off when performing a TLS/SSL connection. This is for test only.
   *
   * @param disableCnCheck
   * @return
   */
  ClientBuilder<T> disableCnCheck(boolean disableCnCheck);

  /**
   * Whether or not to allow the client to follow redirects.
   *
   * @param allowRedirects
   * @return
   */
  ClientBuilder<T> allowRedirects(boolean allowRedirects);

  /**
   * Time before the connection will fail with no reply.
   *
   * @param connectionTimeout
   * @return
   */
  ClientBuilder<T> connectionTimeout(Integer connectionTimeout);

  /**
   * Time before connection will fail with no response.
   *
   * @param receiveTimeout
   * @return
   */
  ClientBuilder<T> receiveTimeout(Integer receiveTimeout);

  /**
   * Username to use with BASIC authentication.
   *
   * @param username
   * @return
   */
  ClientBuilder<T> username(String username);

  /**
   * Password to use with BASIC authentication
   *
   * @param password
   * @return
   */
  ClientBuilder<T> password(String password);

  /**
   * Key info to use when setting up TLS connection. In general this shouldn't need to be configured
   * as the system will use whatever is set to use via the standard system properties.
   *
   * @param certAlias
   * @param keystorePath
   * @return
   */
  ClientBuilder<T> clientKeyInfo(String certAlias, Path keystorePath);

  /**
   * SSL/TLS protocol to use. This shouldn't need to be used in most instances.
   *
   * @param sslProtocol
   * @return
   */
  ClientBuilder<T> sslProtocol(String sslProtocol);

  /**
   * Source ID for OAuth.
   *
   * @param sourceId
   * @return
   */
  ClientBuilder<T> sourceId(String sourceId);

  /**
   * Discovery URL for OAuth.
   *
   * @param discoveryUrl
   * @return
   */
  ClientBuilder<T> discovery(URI discoveryUrl);

  /**
   * Client ID for OAuth.
   *
   * @param clientId
   * @return
   */
  ClientBuilder<T> clientId(String clientId);

  /**
   * Client secret for OAuth.
   *
   * @param clientSecret
   * @return
   */
  ClientBuilder<T> clientSecret(String clientSecret);

  /**
   * OAuth flow to use.
   *
   * @param oauthFlow
   * @return
   */
  ClientBuilder<T> oauthFlow(String oauthFlow);

  /**
   * Additional OAuth parameters to use.
   *
   * @param additionalParameters
   * @return
   */
  ClientBuilder<T> additionalOauthParameters(Map<String, String> additionalParameters);

  /**
   * Property resolver to use to replace any properties in the endpoint URL
   *
   * @param propertyResolver
   * @return
   */
  ClientBuilder<T> propertyResolver(PropertyResolver propertyResolver);

  /**
   * True will enable the use of OAuth for this client.
   *
   * @param useOAuth
   * @return
   */
  ClientBuilder<T> useOAuth(boolean useOAuth);

  /**
   * True will enable the use of SAML ECP for this client.
   *
   * @param useSamlEcp
   * @return
   */
  ClientBuilder<T> useSamlEcp(boolean useSamlEcp);

  /** Enables the use of the SubjectRetrievalInterceptor for this client. */
  ClientBuilder<T> useSubjectRetrievalInterceptor();
}
