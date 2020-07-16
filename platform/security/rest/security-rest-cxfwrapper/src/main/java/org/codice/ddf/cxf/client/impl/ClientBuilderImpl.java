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

import ddf.security.audit.SecurityLogger;
import ddf.security.service.SecurityManager;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.client.ClientBuilder;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.oauth.OAuthSecurity;
import org.codice.ddf.security.jaxrs.SamlSecurity;

public class ClientBuilderImpl<T> implements ClientBuilder<T> {

  protected String endpointUrl;

  protected Class<T> interfaceClass;

  protected List<?> entityProviders;

  protected Interceptor<? extends Message> interceptor;

  protected boolean disableCnCheck;

  protected boolean allowRedirects;

  protected boolean useOAuth;

  protected boolean useSamlEcp;

  protected boolean useSubjectRetrievalInterceptor;

  protected Integer connectionTimeout;

  protected Integer receiveTimeout;

  protected String username;

  protected String password;

  protected ClientKeyInfo clientKeyInfo;

  protected String sslProtocol;

  protected String sourceId;

  protected URI discoveryUrl;

  protected String clientId;

  protected String clientSecret;

  protected String oauthFlow;

  protected Map<String, String> additionalOauthParameters;

  protected PropertyResolver propertyResolver;

  private OAuthSecurity oauthSecurity;

  private SamlSecurity samlSecurity;

  private SecurityLogger securityLogger;

  private SecurityManager securityManager;

  public ClientBuilderImpl(
      OAuthSecurity oauthSecurity,
      SamlSecurity samlSecurity,
      SecurityLogger securityLogger,
      SecurityManager securityManager) {
    this.oauthSecurity = oauthSecurity;
    this.samlSecurity = samlSecurity;
    this.securityLogger = securityLogger;
    this.securityManager = securityManager;
  }

  @Override
  public SecureCxfClientFactory<T> build() {
    SecureCxfClientFactoryImpl<T> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(
            endpointUrl,
            interfaceClass,
            entityProviders,
            interceptor,
            disableCnCheck,
            allowRedirects,
            useOAuth,
            useSamlEcp,
            useSubjectRetrievalInterceptor,
            propertyResolver,
            connectionTimeout,
            receiveTimeout,
            sourceId,
            discoveryUrl,
            clientId,
            clientSecret,
            oauthFlow,
            username,
            password,
            clientKeyInfo,
            sslProtocol,
            additionalOauthParameters,
            oauthSecurity,
            samlSecurity,
            securityLogger,
            securityManager);
    secureCxfClientFactory.initialize();
    return secureCxfClientFactory;
  }

  @Override
  public ClientBuilder<T> endpoint(String endpointUrl) {
    this.endpointUrl = endpointUrl;
    return this;
  }

  @Override
  public ClientBuilder<T> interfaceClass(Class<T> interfaceClass) {
    this.interfaceClass = interfaceClass;
    return this;
  }

  @Override
  public ClientBuilder<T> entityProviders(List<?> entityProviders) {
    this.entityProviders = entityProviders;
    return this;
  }

  @Override
  public ClientBuilder<T> interceptor(Interceptor<? extends Message> interceptor) {
    this.interceptor = interceptor;
    return this;
  }

  @Override
  public ClientBuilder<T> disableCnCheck(boolean disableCnCheck) {
    this.disableCnCheck = disableCnCheck;
    return this;
  }

  @Override
  public ClientBuilder<T> allowRedirects(boolean allowRedirects) {
    this.allowRedirects = allowRedirects;
    return this;
  }

  @Override
  public ClientBuilder<T> connectionTimeout(Integer connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  @Override
  public ClientBuilder<T> receiveTimeout(Integer receiveTimeout) {
    this.receiveTimeout = receiveTimeout;
    return this;
  }

  @Override
  public ClientBuilder<T> username(String username) {
    this.username = username;
    return this;
  }

  @Override
  public ClientBuilder<T> password(String password) {
    this.password = password;
    return this;
  }

  @Override
  public ClientBuilder<T> clientKeyInfo(String certAlias, Path keystorePath) {
    this.clientKeyInfo = new ClientKeyInfo(certAlias, keystorePath);
    return this;
  }

  @Override
  public ClientBuilder<T> sslProtocol(String sslProtocol) {
    this.sslProtocol = sslProtocol;
    return this;
  }

  @Override
  public ClientBuilder<T> sourceId(String sourceId) {
    this.sourceId = sourceId;
    return this;
  }

  @Override
  public ClientBuilder<T> discovery(URI discoveryUrl) {
    this.discoveryUrl = discoveryUrl;
    return this;
  }

  @Override
  public ClientBuilder<T> clientId(String clientId) {
    this.clientId = clientId;
    return this;
  }

  @Override
  public ClientBuilder<T> clientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
    return this;
  }

  @Override
  public ClientBuilder<T> oauthFlow(String oauthFlow) {
    this.oauthFlow = oauthFlow;
    return this;
  }

  @Override
  public ClientBuilder<T> additionalOauthParameters(Map<String, String> additionalParameters) {
    this.additionalOauthParameters = additionalParameters;
    return this;
  }

  @Override
  public ClientBuilder<T> propertyResolver(PropertyResolver propertyResolver) {
    this.propertyResolver = propertyResolver;
    return this;
  }

  @Override
  public ClientBuilder<T> useOAuth(boolean useOAuth) {
    this.useOAuth = useOAuth;
    return this;
  }

  @Override
  public ClientBuilder<T> useSamlEcp(boolean useSamlEcp) {
    this.useSamlEcp = useSamlEcp;
    return this;
  }

  @Override
  public ClientBuilder<T> useSubjectRetrievalInterceptor(boolean useSubjectRetrievalInterceptor) {
    this.useSubjectRetrievalInterceptor = useSubjectRetrievalInterceptor;
    return this;
  }
}
