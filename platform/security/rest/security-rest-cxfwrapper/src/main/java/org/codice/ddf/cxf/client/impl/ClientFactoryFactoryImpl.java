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
package org.codice.ddf.cxf.client.impl;

import java.util.List;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;

public class ClientFactoryFactoryImpl implements ClientFactoryFactory {
  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout,
      String username,
      String password) {
    return new SecureCxfClientFactoryImpl<>(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        connectionTimeout,
        receiveTimeout,
        username,
        password);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
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
      String sslProtocol) {
    return new SecureCxfClientFactoryImpl<>(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        connectionTimeout,
        receiveTimeout,
        new ClientKeyInfo(certAlias, keystorePath),
        sslProtocol);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout) {
    return new SecureCxfClientFactoryImpl<>(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        connectionTimeout,
        receiveTimeout);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      PropertyResolver propertyResolver) {
    return new SecureCxfClientFactoryImpl<>(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        propertyResolver);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects) {
    return new SecureCxfClientFactoryImpl<>(
        endpointUrl, interfaceClass, providers, interceptor, disableCnCheck, allowRedirects);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass, String username, String password) {
    return new SecureCxfClientFactoryImpl<>(endpointUrl, interfaceClass, username, password);
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass) {
    return new SecureCxfClientFactoryImpl<>(endpointUrl, interfaceClass);
  }
}
