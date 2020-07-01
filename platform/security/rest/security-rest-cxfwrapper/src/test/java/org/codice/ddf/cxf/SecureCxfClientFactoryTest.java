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
package org.codice.ddf.cxf;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;
import javax.net.ssl.X509KeyManager;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.cxf.client.SecureCxfClientFactory;
import org.codice.ddf.cxf.client.impl.ClientBuilderImpl;
import org.codice.ddf.cxf.client.impl.ClientKeyInfo;
import org.codice.ddf.cxf.client.impl.SecureCxfClientFactoryImpl.AliasSelectorKeyManager;
import org.codice.ddf.cxf.paos.PaosInInterceptor;
import org.codice.ddf.cxf.paos.PaosOutInterceptor;
import org.codice.ddf.security.jaxrs.impl.SamlSecurity;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;

public class SecureCxfClientFactoryTest {

  private static URI insecureEndpoint;

  private static URI secureEndpoint;

  File systemKeystoreFile = null;

  File systemTruststoreFile = null;

  String password = "changeit";

  private SamlSecurity samlSecurity = new SamlSecurity();

  private SecurityLogger securityLogger = mock(SecurityLogger.class);

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException, URISyntaxException {
    insecureEndpoint = new URI("http://some.url.com/query");
    secureEndpoint = new URI("https://some.url.com/query");
    systemKeystoreFile = temporaryFolder.newFile("serverKeystore.jks");
    FileOutputStream systemKeyOutStream = new FileOutputStream(systemKeystoreFile);
    InputStream systemKeyStream =
        SecureCxfClientFactoryTest.class.getResourceAsStream("/serverKeystore.jks");
    IOUtils.copy(systemKeyStream, systemKeyOutStream);

    systemTruststoreFile = temporaryFolder.newFile("serverTruststore.jks");
    FileOutputStream systemTrustOutStream = new FileOutputStream(systemTruststoreFile);
    InputStream systemTrustStream =
        SecureCxfClientFactoryTest.class.getResourceAsStream("/serverTruststore.jks");
    IOUtils.copy(systemTrustStream, systemTrustOutStream);

    System.setProperty(SecurityConstants.KEYSTORE_TYPE, "jks");
    System.setProperty(SecurityConstants.TRUSTSTORE_TYPE, "jks");
    System.setProperty("ddf.home", "");
    System.setProperty(SecurityConstants.KEYSTORE_PATH, systemKeystoreFile.getAbsolutePath());
    System.setProperty(SecurityConstants.TRUSTSTORE_PATH, systemTruststoreFile.getAbsolutePath());
    System.setProperty(SecurityConstants.KEYSTORE_PASSWORD, password);
    System.setProperty(SecurityConstants.TRUSTSTORE_PASSWORD, password);
  }

  @Test
  public void testConstructorNegativeCases() {
    // negative tests
    SecureCxfClientFactory<IDummy> secureCxfClientFactory;
    boolean invalid = false;
    try { // null for url
      secureCxfClientFactory =
          new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
              .endpoint(null)
              .interfaceClass(IDummy.class)
              .build();
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try { // null for url and class
      secureCxfClientFactory =
          new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
              .endpoint(null)
              .interfaceClass(null)
              .build();
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try { // null for class
      secureCxfClientFactory =
          new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
              .endpoint(insecureEndpoint)
              .interfaceClass(null)
              .build();
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try {
      secureCxfClientFactory =
          new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
              .endpoint(null)
              .interfaceClass(null)
              .entityProviders(null)
              .interceptor(null)
              .disableCnCheck(false)
              .allowRedirects(false)
              .connectionTimeout(0)
              .receiveTimeout(0)
              .clientKeyInfo("alias", Paths.get("file:keystore"))
              .sslProtocol("TLSv1.1")
              .build();
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
  }

  @Test
  public void testInsecureWebClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
            .endpoint(insecureEndpoint)
            .interfaceClass(IDummy.class)
            .build();
    WebClient client = secureCxfClientFactory.getWebClient();

    assertThat(hasEcpEnabled(client), is(false));
    assertThat(client.getBaseURI().equals(insecureEndpoint), is(true));
  }

  @Test
  public void testSecureClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
            .endpoint(secureEndpoint)
            .interfaceClass(IDummy.class)
            .useSamlEcp(true)
            .build();
    IDummy client = secureCxfClientFactory.getClient();

    assertThat(hasEcpEnabled(client), is(true));
  }

  @Test
  public void testSecureWebClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
            .endpoint(secureEndpoint)
            .interfaceClass(IDummy.class)
            .useSamlEcp(true)
            .build();
    WebClient client = secureCxfClientFactory.getWebClient();

    assertThat(hasEcpEnabled(client), is(true));
    assertThat(client.getBaseURI().equals(secureEndpoint), is(true));
  }

  @Test
  public void validateConduit() {
    IDummy clientForSubject =
        new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
            .endpoint(secureEndpoint)
            .interfaceClass(IDummy.class)
            .entityProviders(null)
            .interceptor(null)
            .disableCnCheck(true)
            .allowRedirects(true)
            .build()
            .getClient();
    HTTPConduit httpConduit =
        WebClient.getConfig(WebClient.client(clientForSubject)).getHttpConduit();
    assertThat(httpConduit.getTlsClientParameters().isDisableCNCheck(), is(true));
  }

  @Test
  public void testHttpsClientWithSystemProperty() {
    PropertyResolver mockPropertyResolver = mock(PropertyResolver.class);
    when(mockPropertyResolver.getResolvedString()).thenReturn(secureEndpoint.toString());
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new ClientBuilderImpl<IDummy>(null, samlSecurity, securityLogger)
            .endpoint(secureEndpoint)
            .interfaceClass(IDummy.class)
            .entityProviders(null)
            .interceptor(null)
            .disableCnCheck(false)
            .allowRedirects(false)
            .propertyResolver(mockPropertyResolver)
            .build();
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertThat(unsecuredClient.getBaseURI(), is(secureEndpoint));
    verify(mockPropertyResolver).getResolvedString();
  }

  @Test
  public void testKeyInfo() {
    String alias = "alias";
    String keystorePath = Paths.get("/path/to/keystore").toString();

    ClientKeyInfo keyInfo = new ClientKeyInfo(alias, Paths.get(keystorePath));
    assertThat(keyInfo.getAlias(), is(alias));
    assertThat(keyInfo.getKeystorePath().toString(), is(keystorePath));
  }

  @Test
  public void testAliasSelectorKeyManager() {
    X509KeyManager keyManager = mock(X509KeyManager.class);
    String alias = "testAlias";
    String[] aliases = new String[] {alias};
    when(keyManager.chooseClientAlias(any(), any(), any())).thenReturn(alias);
    when(keyManager.getClientAliases(any(), any())).thenReturn(aliases);

    AliasSelectorKeyManager aliasSelectorKeyManager =
        new AliasSelectorKeyManager(keyManager, alias);
    String chosenAlias =
        aliasSelectorKeyManager.chooseClientAlias(new String[] {"x509"}, null, null);
    assertThat(chosenAlias, is(alias));
  }

  private boolean hasEcpEnabled(Object client) {
    ClientConfiguration clientConfig = WebClient.getConfig(client);
    return clientConfig.getOutInterceptors().stream().anyMatch(i -> i instanceof PaosOutInterceptor)
        && clientConfig.getInInterceptors().stream().anyMatch(i -> i instanceof PaosInInterceptor);
  }

  private DummySubject getSubject() {
    return new DummySubject(new DefaultSecurityManager(), new SimplePrincipalCollection());
  }

  private Subject setupMockSubject() throws Exception {
    Subject mockSubject = mock(Subject.class);
    PrincipalCollection mockPrincipals = mock(PrincipalCollection.class);
    SecurityAssertion mockSecurityAssertion = mock(SecurityAssertion.class);
    SecurityToken mockToken = mock(SecurityToken.class);

    when(mockSubject.getPrincipals()).thenReturn(mockPrincipals);
    when(mockPrincipals.asList()).thenReturn(Arrays.asList(mockSecurityAssertion));
    when(mockSecurityAssertion.getToken()).thenReturn(mockToken);
    when(mockToken.getToken()).thenReturn(getAssertionElement());

    return mockSubject;
  }

  private Element getAssertionElement() throws Exception {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);

    DocumentBuilder db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    return db.parse(SecureCxfClientFactoryTest.class.getResourceAsStream("/SAMLAssertion.xml"))
        .getDocumentElement();
  }

  private interface IDummy {
    @GET
    public Response ok();
  }

  private class IDummyImpl implements IDummy {

    @Override
    public Response ok() {
      return Response.ok().build();
    }
  }

  private class DummySubject extends DelegatingSubject implements Subject {

    public DummySubject(
        org.apache.shiro.mgt.SecurityManager manager, PrincipalCollection principals) {
      super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
    }

    @Override
    public String getName() {
      return "Dummy Subject";
    }
  }
}
