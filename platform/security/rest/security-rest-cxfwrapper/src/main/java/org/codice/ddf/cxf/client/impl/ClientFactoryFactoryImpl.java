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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.client.ClientBuilder;
import org.codice.ddf.cxf.client.ClientBuilderFactory;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;

@Deprecated
public class ClientFactoryFactoryImpl implements ClientFactoryFactory {

  private ClientBuilderFactory clientBuilderFactory;

  public void setClientBuilderFactory(ClientBuilderFactory clientBuilderFactory) {
    this.clientBuilderFactory = clientBuilderFactory;
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
      String username,
      String password) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .username(username)
          .password(password)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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

    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .clientKeyInfo(certAlias, Paths.get(keystorePath))
          .sslProtocol(sslProtocol)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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
      String sslProtocol,
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String oauthFlow) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .clientKeyInfo(certAlias, Paths.get(keystorePath))
          .sslProtocol(sslProtocol)
          .sourceId(sourceId)
          .discovery(new URI(discoveryUrl))
          .clientId(clientId)
          .clientSecret(clientSecret)
          .oauthFlow(oauthFlow)
          .useOAuth(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String oauthFlow) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .sourceId(sourceId)
          .discovery(new URI(discoveryUrl))
          .clientId(clientId)
          .clientSecret(clientSecret)
          .oauthFlow(oauthFlow)
          .useOAuth(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String username,
      String password,
      Map<String, String> additionalOauthParameters) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .connectionTimeout(connectionTimeout)
          .receiveTimeout(receiveTimeout)
          .sourceId(sourceId)
          .discovery(new URI(discoveryUrl))
          .clientId(clientId)
          .clientSecret(clientSecret)
          .username(username)
          .password(password)
          .additionalOauthParameters(additionalOauthParameters)
          .useOAuth(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
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
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .propertyResolver(propertyResolver)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .entityProviders(providers)
          .interceptor(interceptor)
          .disableCnCheck(disableCnCheck)
          .allowRedirects(allowRedirects)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass, String username, String password) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .username(username)
          .password(password)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl,
      Class<T> interfaceClass,
      String sourceId,
      String discoveryUrl,
      String clientId,
      String clientSecret,
      String oauthFlow) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .sourceId(sourceId)
          .discovery(new URI(discoveryUrl))
          .clientId(clientId)
          .clientSecret(clientSecret)
          .oauthFlow(oauthFlow)
          .useOAuth(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @Override
  public <T> SecureCxfClientFactory<T> getSecureCxfClientFactory(
      String endpointUrl, Class<T> interfaceClass) {
    ClientBuilder<T> clientBuilder = clientBuilderFactory.getClientBuilder();
    try {
      return clientBuilder
          .endpoint(new URI(endpointUrl))
          .interfaceClass(interfaceClass)
          .useSamlEcp(true)
          .build();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
