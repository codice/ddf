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

import ddf.security.SecurityConstants;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.liberty.paos.Request;
import ddf.security.liberty.paos.Response;
import ddf.security.liberty.paos.impl.RequestBuilder;
import ddf.security.liberty.paos.impl.RequestMarshaller;
import ddf.security.liberty.paos.impl.RequestUnmarshaller;
import ddf.security.liberty.paos.impl.ResponseBuilder;
import ddf.security.liberty.paos.impl.ResponseMarshaller;
import ddf.security.liberty.paos.impl.ResponseUnmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.SecurityPermission;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.shiro.subject.Subject;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.paos.PaosInInterceptor;
import org.codice.ddf.cxf.paos.PaosOutInterceptor;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This factory helps construct clients for secure restful communication. For now, the getForSubject
 * methods should only be used to support DDF<->DDF interop, because:
 *
 * <ol>
 *   <li>Most non-DDF systems do not know how to handle SAML assertions in the auth header.
 * </ol>
 */
public class SecureCxfClientFactoryImpl<T> implements SecureCxfClientFactory<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureCxfClientFactoryImpl.class);
  public static final String HTTPS = "https";

  private final JAXRSClientFactoryBean clientFactory;

  private final boolean disableCnCheck;

  private final boolean allowRedirects;

  private final Class<T> interfaceClass;

  private static final Integer DEFAULT_CONNECTION_TIMEOUT = 30000;

  private static final Integer DEFAULT_RECEIVE_TIMEOUT = 60000;

  // A default small value greater than one to handle loopbacks after validation.  Configurable
  private static final Integer SAME_URI_REDIRECT_MAX = 3;

  private static final String AUTO_REDIRECT_ALLOW_REL_URI = "http.redirect.relative.uri";

  private static final String AUTO_REDIRECT_MAX_SAME_URI_COUNT = "http.redirect.max.same.uri.count";

  private static final SecurityPermission CREATE_CLIENT_PERMISSION =
      new SecurityPermission("createCxfClient");

  private Integer sameUriRedirectMax = SAME_URI_REDIRECT_MAX;

  private boolean basicAuth = false;

  private Integer connectionTimeout;

  private Integer receiveTimeout;

  private ClientKeyInfo keyInfo = null;

  private String sslProtocol;

  static {
    OpenSAMLUtil.initSamlEngine();
    XMLObjectProviderRegistry xmlObjectProviderRegistry =
        ConfigurationService.get(XMLObjectProviderRegistry.class);
    xmlObjectProviderRegistry.registerObjectProvider(
        Request.DEFAULT_ELEMENT_NAME,
        new RequestBuilder(),
        new RequestMarshaller(),
        new RequestUnmarshaller());
    xmlObjectProviderRegistry.registerObjectProvider(
        Response.DEFAULT_ELEMENT_NAME,
        new ResponseBuilder(),
        new ResponseMarshaller(),
        new ResponseUnmarshaller());
  }

  /**
   * @see #SecureCxfClientFactoryImpl(String, Class, java.util.List, Interceptor, boolean, boolean)
   */
  public SecureCxfClientFactoryImpl(String endpointUrl, Class<T> interfaceClass) {
    this(endpointUrl, interfaceClass, null, null, false, false);
  }

  public SecureCxfClientFactoryImpl(
      String endpointUrl, Class<T> interfaceClass, String username, String password) {
    this(endpointUrl, interfaceClass, null, null, false, false, null, null, username, password);
  }

  public SecureCxfClientFactoryImpl(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects) {
    this(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        new PropertyResolver(endpointUrl));
  }

  /**
   * Constructs a factory that will return security-aware cxf clients. Once constructed, use the
   * getClient* methods to retrieve a fresh client with the same configuration. Providing {@link
   * WebClient} to interfaceClass will create a generic web client.
   *
   * <p>This factory can and should be cached. The clients it constructs should not be.
   *
   * @param endpointUrl the remote url to connect to
   * @param interfaceClass an interface representing the resource at the remote url
   * @param providers optional list of providers to further configure the client
   * @param interceptor optional message interceptor for the client
   * @param disableCnCheck disable ssl check for common name / host name match
   * @param allowRedirects allow this client to follow redirects
   */
  public SecureCxfClientFactoryImpl(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      PropertyResolver propertyResolver) {
    if (StringUtils.isEmpty(endpointUrl) || interfaceClass == null) {
      throw new IllegalArgumentException(
          "Called without a valid URL, will not be able to connect.");
    }

    if (propertyResolver != null) {
      endpointUrl = propertyResolver.getResolvedString();
    } else {
      LOGGER.warn(
          "Called without a valid propertyResolver, system properties in URI may not resolve.");
    }

    this.interfaceClass = interfaceClass;
    this.disableCnCheck = disableCnCheck;
    this.allowRedirects = allowRedirects;

    JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
    jaxrsClientFactoryBean.setServiceClass(interfaceClass);
    jaxrsClientFactoryBean.setAddress(endpointUrl);
    jaxrsClientFactoryBean.setClassLoader(interfaceClass.getClassLoader());
    jaxrsClientFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
    jaxrsClientFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

    if (!basicAuth && StringUtils.startsWithIgnoreCase(endpointUrl, HTTPS)) {
      jaxrsClientFactoryBean.getInInterceptors().add(new PaosInInterceptor(Phase.RECEIVE));
      jaxrsClientFactoryBean.getOutInterceptors().add(new PaosOutInterceptor(Phase.POST_LOGICAL));
    }

    if (CollectionUtils.isNotEmpty(providers)) {
      jaxrsClientFactoryBean.setProviders(providers);
    }

    if (interceptor != null) {
      jaxrsClientFactoryBean.getInInterceptors().add(interceptor);
    }

    this.clientFactory = jaxrsClientFactoryBean;
  }

  /**
   * Constructs a factory that will return security-aware cxf clients. Once constructed, use the
   * getClient* methods to retrieve a fresh client with the same configuration. Providing {@link
   * WebClient} to interfaceClass will create a generic web client.
   *
   * <p>This factory can and should be cached. The clients it constructs should not be.
   *
   * @param endpointUrl the remote url to connect to
   * @param interfaceClass an interface representing the resource at the remote url
   * @param providers optional list of providers to further configure the client
   * @param interceptor optional message interceptor for the client
   * @param disableCnCheck disable ssl check for common name / host name match
   * @param allowRedirects allow this client to follow redirects
   * @param connectionTimeout timeout for the connection
   * @param receiveTimeout timeout for receiving responses
   */
  @SuppressWarnings("squid:S00107")
  public SecureCxfClientFactoryImpl(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout) {

    this(endpointUrl, interfaceClass, providers, interceptor, disableCnCheck, allowRedirects);

    this.connectionTimeout = connectionTimeout;

    this.receiveTimeout = receiveTimeout;
  }

  /**
   * Constructs a factory that will return security-aware cxf clients. Once constructed, use the
   * getClient* methods to retrieve a fresh client with the same configuration. Providing {@link
   * WebClient} to interfaceClass will create a generic web client.
   *
   * <p>This factory can and should be cached. The clients it constructs should not be.
   *
   * @param endpointUrl the remote url to connect to
   * @param interfaceClass an interface representing the resource at the remote url
   * @param providers optional list of providers to further configure the client
   * @param interceptor optional message interceptor for the client
   * @param disableCnCheck disable ssl check for common name / host name match
   * @param allowRedirects allow this client to follow redirects
   * @param connectionTimeout timeout for the connection
   * @param receiveTimeout timeout for receiving responses
   * @param keyInfo client key info for 2-way ssl
   * @param sslProtocol SSL protocol to use (e.g. TLSv1.2)
   */
  @SuppressWarnings("squid:S00107")
  public SecureCxfClientFactoryImpl(
      String endpointUrl,
      Class<T> interfaceClass,
      List<?> providers,
      Interceptor<? extends Message> interceptor,
      boolean disableCnCheck,
      boolean allowRedirects,
      Integer connectionTimeout,
      Integer receiveTimeout,
      ClientKeyInfo keyInfo,
      String sslProtocol) {

    this(endpointUrl, interfaceClass, providers, interceptor, disableCnCheck, allowRedirects);

    this.connectionTimeout = connectionTimeout;

    this.receiveTimeout = receiveTimeout;

    this.keyInfo = keyInfo;

    this.sslProtocol = sslProtocol;
  }

  /**
   * Constructs a factory that will return security-aware cxf clients. Once constructed, use the
   * getClient* methods to retrieve a fresh client with the same configuration. Providing {@link
   * WebClient} to interfaceClass will create a generic web client.
   *
   * <p>This factory can and should be cached. The clients it constructs should not be.
   *
   * <p>This constructor represents a quick fix only.
   *
   * @param endpointUrl the remote url to connect to
   * @param interfaceClass an interface representing the resource at the remote url
   * @param providers optional list of providers to further configure the client
   * @param interceptor optional message interceptor for the client
   * @param disableCnCheck disable ssl check for common name / host name match
   * @param allowRedirects allow this client to follow redirects
   * @param connectionTimeout timeout for the connection
   * @param receiveTimeout timeout for receiving responses
   * @param username a String representing the username
   * @param password a String representing a password
   */
  @SuppressWarnings("squid:S00107")
  public SecureCxfClientFactoryImpl(
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

    this(
        endpointUrl,
        interfaceClass,
        providers,
        interceptor,
        disableCnCheck,
        allowRedirects,
        connectionTimeout,
        receiveTimeout);

    this.clientFactory.setPassword(password);
    this.clientFactory.setUsername(username);
    this.basicAuth = true;
  }

  public T getClient() {
    return getClientForSubject(null);
  }

  public WebClient getWebClient() {
    return getWebClientForSubject(null);
  }

  /**
   * Clients produced by this method will be secured with two-way ssl and the provided security
   * subject.
   *
   * <p>The returned client should NOT be reused between requests! This method should be called for
   * each new request in order to ensure that the security token is up-to-date each time.
   */
  public final T getClientForSubject(Subject subject) {
    final java.lang.SecurityManager security = System.getSecurityManager();

    if (security != null) {
      security.checkPermission(CREATE_CLIENT_PERMISSION);
    }

    return AccessController.doPrivileged(
        (PrivilegedAction<T>)
            () -> {
              String asciiString = clientFactory.getAddress();

              T newClient = getNewClient();

              if (!basicAuth && StringUtils.startsWithIgnoreCase(asciiString, HTTPS)) {
                if (subject instanceof ddf.security.Subject) {
                  RestSecurity.setSubjectOnClient(
                      (ddf.security.Subject) subject, WebClient.client(newClient));
                }
              }

              auditRemoteConnection(asciiString);

              return newClient;
            });
  }

  public Integer getSameUriRedirectMax() {
    return sameUriRedirectMax;
  }

  public void setSameUriRedirectMax(Integer sameUriRedirectMax) {
    if (sameUriRedirectMax != null && sameUriRedirectMax > 0) {
      this.sameUriRedirectMax = sameUriRedirectMax;
    } else {
      LOGGER.warn("Cannot set redirect to invalid value: {}", sameUriRedirectMax);
    }
  }

  private void auditRemoteConnection(String asciiString) {
    try {
      URI uri = new URI(asciiString);
      String host = uri.getHost();
      InetAddress inetAddress = InetAddress.getByName(host);
      SecurityLogger.audit(
          "Setting up remote connection to federated node [{}].", inetAddress.getHostAddress());
    } catch (Exception e) {
      LOGGER.debug(
          "Unhandled exception while attempting to determine the IP address for a federated node, might be a DNS issue.",
          e);
      SecurityLogger.audit(
          "Unable to determine the IP address for a federated node [{}], might be a DNS issue.",
          asciiString);
    }
  }

  /**
   * Convenience method to get a {@link WebClient} instead of a {@link
   * org.apache.cxf.jaxrs.client.ClientProxyImpl ClientProxyImpl}.
   *
   * @see #getClientForSubject(Subject subject)
   */
  public WebClient getWebClientForSubject(Subject subject) {
    return getWebClient(getClientForSubject(subject));
  }

  private WebClient getWebClient(Object client) {
    return WebClient.fromClient(WebClient.client(client), true);
  }

  private T getNewClient() {
    T clientImpl =
        interfaceClass.equals(WebClient.class)
            ? (T) clientFactory.create()
            : JAXRSClientFactory.fromClient(clientFactory.create(), interfaceClass);

    ClientConfiguration clientConfig = WebClient.getConfig(clientImpl);
    clientConfig.getRequestContext().put(Message.MAINTAIN_SESSION, Boolean.TRUE);

    configureConduit(clientConfig);
    configureTimeouts(clientConfig, connectionTimeout, receiveTimeout);
    return clientImpl;
  }

  @SuppressWarnings("squid:S3776")
  private void configureConduit(ClientConfiguration clientConfig) {
    HTTPConduit httpConduit = clientConfig.getHttpConduit();
    if (httpConduit == null) {
      LOGGER.info("HTTPConduit was null for {}. Unable to configure security.", this);
      return;
    }

    if (allowRedirects) {
      HTTPClientPolicy clientPolicy = httpConduit.getClient();
      if (clientPolicy != null) {
        clientPolicy.setAutoRedirect(true);
        Bus bus = clientConfig.getBus();
        if (bus != null) {
          bus.getProperties().put(AUTO_REDIRECT_ALLOW_REL_URI, true);
          bus.getProperties().put(AUTO_REDIRECT_MAX_SAME_URI_COUNT, getSameUriRedirectMax());
        }
      }
    }

    TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();
    if (tlsParams == null) {
      tlsParams = new TLSClientParameters();
    }

    tlsParams.setDisableCNCheck(disableCnCheck);

    tlsParams.setUseHttpsURLConnectionDefaultHostnameVerifier(!disableCnCheck);
    String cipherSuites = System.getProperty("https.cipherSuites");
    if (cipherSuites != null) {
      tlsParams.setCipherSuites(Arrays.asList(cipherSuites.split(",")));
    }

    KeyStore keyStore = null;
    KeyStore trustStore = null;
    try {
      keyStore = SecurityConstants.newKeystore();
      trustStore = SecurityConstants.newTruststore();
    } catch (KeyStoreException e) {
      LOGGER.debug(
          "Unable to create keystore instance of type {}",
          System.getProperty(SecurityConstants.KEYSTORE_TYPE),
          e);
    }
    Path keyStoreFile;
    if (keyInfo != null && StringUtils.isNotBlank(keyInfo.getKeystorePath())) {
      keyStoreFile = Paths.get(keyInfo.getKeystorePath());
    } else {
      keyStoreFile = Paths.get(SecurityConstants.getKeystorePath());
    }

    Path trustStoreFile = Paths.get(SecurityConstants.getTruststorePath());
    String ddfHome = System.getProperty("ddf.home");
    if (ddfHome != null) {
      Path ddfHomePath = Paths.get(ddfHome);
      if (!keyStoreFile.isAbsolute()) {
        keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
      }
      if (!trustStoreFile.isAbsolute()) {
        trustStoreFile = Paths.get(ddfHomePath.toString(), trustStoreFile.toString());
      }
    }
    String keyStorePassword = SecurityConstants.getKeystorePassword();
    String trustStorePassword = SecurityConstants.getTruststorePassword();
    if (!Files.isReadable(keyStoreFile) || !Files.isReadable(trustStoreFile)) {
      LOGGER.debug(
          "Unable to read system key/trust store files: [ {} ] [ {} ]",
          keyStoreFile,
          trustStoreFile);
      return;
    }
    try (InputStream kfis = Files.newInputStream(keyStoreFile)) {
      if (keyStore != null) {
        keyStore.load(kfis, keyStorePassword.toCharArray());
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.debug("Unable to load system key file.", e);
    }
    try (InputStream tfis = Files.newInputStream(trustStoreFile)) {
      if (trustStore != null) {
        trustStore.load(tfis, trustStorePassword.toCharArray());
      }
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.debug("Unable to load system trust file.", e);
    }

    KeyManager[] keyManagers = null;
    try {
      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
      keyManagers = keyManagerFactory.getKeyManagers();
      tlsParams.setKeyManagers(keyManagers);
    } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
      LOGGER.debug("Unable to initialize KeyManagerFactory.", e);
    }

    TrustManager[] trustManagers = null;
    try {
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      trustManagers = trustManagerFactory.getTrustManagers();
      tlsParams.setTrustManagers(trustManagers);
    } catch (NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.debug("Unable to initialize TrustManagerFactory.", e);
    }

    if (keyInfo != null) {
      LOGGER.trace("Using keystore file: {}, alias: {}", keyStoreFile, keyInfo.getAlias());
      tlsParams.setUseHttpsURLConnectionDefaultSslSocketFactory(false);
      tlsParams.setCertAlias(keyInfo.getAlias());
      try {
        if (keyManagers == null) {
          throw new KeyManagementException("keyManagers was null");
        }

        boolean validProtocolFound = false;
        String validProtocolsStr = System.getProperty("jdk.tls.client.protocols");
        if (StringUtils.isNotBlank(validProtocolsStr)) {
          String[] validProtocols = validProtocolsStr.split(",");
          for (String validProtocol : validProtocols) {
            if (validProtocol.equals(sslProtocol)) {
              validProtocolFound = true;
              break;
            }
          }
          if (!validProtocolFound) {
            LOGGER.error(
                "{} is not in list of valid SSL protocols {}", sslProtocol, validProtocolsStr);
          }

        } else {
          validProtocolFound = true;
        }
        if (validProtocolFound) {
          tlsParams.setSSLSocketFactory(
              getSSLSocketFactory(sslProtocol, keyInfo.getAlias(), keyManagers, trustManagers));
        }
      } catch (KeyManagementException | NoSuchAlgorithmException e) {
        LOGGER.debug("Unable to override default SSL Socket Factory", e);
      }
    } else {
      tlsParams.setUseHttpsURLConnectionDefaultSslSocketFactory(true);
      tlsParams.setCertAlias(SystemBaseUrl.INTERNAL.getHost());
    }

    httpConduit.setTlsClientParameters(tlsParams);
  }

  /**
   * Configures the connection and receive timeouts. If any of the parameters are null, the timeouts
   * will be set to the system default.
   *
   * @param clientConfiguration Client configuration used for outgoing requests.
   * @param connectionTimeout Connection timeout in milliseconds.
   * @param receiveTimeout Receive timeout in milliseconds.
   */
  protected void configureTimeouts(
      ClientConfiguration clientConfiguration, Integer connectionTimeout, Integer receiveTimeout) {

    HTTPConduit httpConduit = clientConfiguration.getHttpConduit();
    if (httpConduit == null) {
      LOGGER.info("HTTPConduit was null for {}. Unable to configure timeouts", this);
      return;
    }
    HTTPClientPolicy httpClientPolicy = httpConduit.getClient();

    if (httpClientPolicy == null) {
      httpClientPolicy = new HTTPClientPolicy();
    }

    if (connectionTimeout != null) {
      httpClientPolicy.setConnectionTimeout(connectionTimeout);
    } else {
      httpClientPolicy.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
    }

    if (receiveTimeout != null) {
      httpClientPolicy.setReceiveTimeout(receiveTimeout);
    } else {
      httpClientPolicy.setReceiveTimeout(DEFAULT_RECEIVE_TIMEOUT);
    }

    if (httpClientPolicy.isSetConnectionTimeout()) {
      LOGGER.debug("Connection timeout has been set.");
    } else {
      LOGGER.debug("Connection timeout has NOT been set.");
    }
    if (httpClientPolicy.isSetReceiveTimeout()) {
      LOGGER.debug("Receive timeout has been set.");
    } else {
      LOGGER.debug("Receive timeout has NOT been set.");
    }

    httpConduit.setClient(httpClientPolicy);
  }

  public void addOutInterceptors(Interceptor<? extends Message> inteceptor) {
    this.clientFactory.getOutInterceptors().add(inteceptor);
  }

  private SSLSocketFactory getSSLSocketFactory(
      String sslProtocol, String alias, KeyManager[] keyManagers, TrustManager[] trustManagers)
      throws KeyManagementException, NoSuchAlgorithmException {

    if (ArrayUtils.isNotEmpty(keyManagers)) {
      for (int i = 0; i < keyManagers.length; i++) {
        if (keyManagers[i] instanceof X509KeyManager) {
          keyManagers[i] = new AliasSelectorKeyManager((X509KeyManager) keyManagers[i], alias);
        }
      }
    }

    SSLContext context = SSLContext.getInstance(sslProtocol);
    context.init(keyManagers, trustManagers, null);

    return context.getSocketFactory();
  }

  /**
   * X509 certificate selector for retrieving certificate for a specific alias. Based off of code
   * from
   * https://alesaudate.wordpress.com/2010/08/09/how-to-dynamically-select-a-certificate-alias-when-invoking-web-services/
   */
  public static class AliasSelectorKeyManager implements X509KeyManager {

    private X509KeyManager keyManager;
    private String alias;

    public AliasSelectorKeyManager(X509KeyManager keyManager, String alias) {
      this.keyManager = keyManager;
      this.alias = alias;
    }

    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
      if (keyManager == null) {
        return null;
      }

      if (alias == null) {
        return keyManager.chooseClientAlias(keyTypes, issuers, socket);
      }

      for (String keyType : keyTypes) {
        String[] validAliases = keyManager.getClientAliases(keyType, issuers);
        if (validAliases != null) {
          for (String validAlias : validAliases) {
            if (validAlias.equals(alias)) {
              return alias;
            }
          }
        }
      }

      return null;
    }

    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
      return keyManager.chooseServerAlias(keyType, issuers, socket);
    }

    public X509Certificate[] getCertificateChain(String alias) {
      return keyManager.getCertificateChain(alias);
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
      return keyManager.getClientAliases(keyType, issuers);
    }

    public PrivateKey getPrivateKey(String alias) {
      return keyManager.getPrivateKey(alias);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
      return keyManager.getServerAliases(keyType, issuers);
    }
  }
}
