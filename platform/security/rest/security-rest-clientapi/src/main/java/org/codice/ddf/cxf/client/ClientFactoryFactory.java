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

import java.util.List;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.codice.ddf.configuration.PropertyResolver;

/**
 * Factory class for creating secure CXF client factory objects that can generate WebClients
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be * removed in a future version of the library. </b>
 */
public interface ClientFactoryFactory {
  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param connectionTimeout
   * @param receiveTimeout
   * @param username
   * @param password
   * @param <T>
   * @return
   */
  @SuppressWarnings("squid:S00107")
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout,
      String username,
      String password);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param connectionTimeout
   * @param receiveTimeout
   * @param certAlias
   * @param keystorePath
   * @param sslProtocol
   * @param <T>
   * @return
   */
  @SuppressWarnings("squid:S00107")
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout,
      String certAlias,
      String keystorePath,
      String sslProtocol);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param connectionTimeout
   * @param receiveTimeout
   * @param <T>
   * @return
   */
  @SuppressWarnings("squid:S00107")
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param connectionTimeout
   * @param receiveTimeout
   * @param sourceId
   * @param discoveryUrl
   * @param clientId
   * @param clientSecret
   * @param oauthFlow
   * @param <T>
   * @return
   */
  @SuppressWarnings("squid:S00107")
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout,
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String oauthFlow);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param propertyResolver
   * @param <T>
   * @return
   */
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      PropertyResolver propertyResolver);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param providers
   * @param interceptor
   * @param disableCnCheck
   * @param allowRedirects
   * @param <T>
   * @return
   */
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param username
   * @param password
   * @param <T>
   * @return
   */
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass, String username, String password);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param sourceId
   * @param discoveryUrl
   * @param clientId
   * @param clientSecret
   * @param oauthFlow
   * @param <T>
   * @return
   */
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String oauthFlow);

  /**
   * Returns an initialized SecureCxfClientFactory
   *
   * @param endpointUrl
   * @param interfaceClass
   * @param <T>
   * @return
   */
  <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass);
}
