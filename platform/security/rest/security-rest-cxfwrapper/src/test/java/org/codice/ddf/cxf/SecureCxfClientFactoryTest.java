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

import static org.codice.ddf.security.common.jaxrs.RestSecurity.AUTH_HEADER;
import static org.codice.ddf.security.common.jaxrs.RestSecurity.SAML_HEADER_PREFIX;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.codice.ddf.cxf.client.impl.ClientKeyInfo;
import org.codice.ddf.cxf.client.impl.SecureCxfClientFactoryImpl;
import org.codice.ddf.cxf.client.impl.SecureCxfClientFactoryImpl.AliasSelectorKeyManager;
import org.codice.ddf.cxf.paos.PaosInInterceptor;
import org.codice.ddf.cxf.paos.PaosOutInterceptor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Element;

public class SecureCxfClientFactoryTest {

  private static final String INSECURE_ENDPOINT = "http://some.url.com/query";

  private static final String SECURE_ENDPOINT = "https://some.url.com/query";

  File systemKeystoreFile = null;

  File systemTruststoreFile = null;

  String password = "changeit";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
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
    try { // test empty string for url
      secureCxfClientFactory = new SecureCxfClientFactoryImpl<>("", IDummy.class);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try { // null for url
      secureCxfClientFactory = new SecureCxfClientFactoryImpl<>(null, IDummy.class);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try { // null for url and class
      secureCxfClientFactory = new SecureCxfClientFactoryImpl<>(null, null);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try { // null for class
      secureCxfClientFactory = new SecureCxfClientFactoryImpl<>(INSECURE_ENDPOINT, null);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
    invalid = false;
    try {
      secureCxfClientFactory =
          new SecureCxfClientFactoryImpl<>(
              null,
              null,
              null,
              null,
              false,
              false,
              0,
              0,
              new ClientKeyInfo("alias", "keystore"),
              "TLSv1.1");
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertThat(invalid, is(true));
  }

  @Test
  public void testInsecureWebClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(INSECURE_ENDPOINT, IDummy.class);
    WebClient client = secureCxfClientFactory.getWebClient();

    assertThat(hasEcpEnabled(client), is(false));
    assertThat(client.getBaseURI().toASCIIString().equals(INSECURE_ENDPOINT), is(true));
  }

  @Test
  public void testInsecureWebClientForSubject() throws Exception {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(INSECURE_ENDPOINT, IDummy.class);
    Subject subject = setupMockSubject();
    WebClient client = secureCxfClientFactory.getWebClientForSubject(subject);

    assertThat(hasEcpEnabled(client), is(false));
    assertThat(client.getBaseURI().toASCIIString().equals(INSECURE_ENDPOINT), is(true));
    assertThat(client.getHeaders().get(AUTH_HEADER), is(nullValue()));
  }

  @Test
  public void testSecureClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(SECURE_ENDPOINT, IDummy.class);
    IDummy client = secureCxfClientFactory.getClient();

    assertThat(hasEcpEnabled(client), is(true));
  }

  @Test
  public void testSecureWebClient() {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(SECURE_ENDPOINT, IDummy.class);
    WebClient client = secureCxfClientFactory.getWebClient();

    assertThat(hasEcpEnabled(client), is(true));
    assertThat(client.getBaseURI().toASCIIString().equals(SECURE_ENDPOINT), is(true));
  }

  @Test
  public void testSecureClientForSubject() throws Exception {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(SECURE_ENDPOINT, IDummy.class);
    Subject subject = setupMockSubject();
    IDummy client = secureCxfClientFactory.getClientForSubject(subject);

    assertThat(hasEcpEnabled(client), is(true));
    assertThat(
        WebClient.client(client).getHeaders().get(AUTH_HEADER).get(0),
        startsWith(SAML_HEADER_PREFIX));
  }

  @Test
  public void testSecureWebClientForSubject() throws Exception {
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(SECURE_ENDPOINT, IDummy.class);
    Subject subject = setupMockSubject();
    WebClient client = secureCxfClientFactory.getWebClientForSubject(subject);

    assertThat(hasEcpEnabled(client), is(true));
    assertThat(client.getBaseURI().toASCIIString().equals(SECURE_ENDPOINT), is(true));
    assertThat(client.getHeaders().get(AUTH_HEADER).get(0), startsWith(SAML_HEADER_PREFIX));
  }

  @Test
  public void validateConduit() {
    IDummy clientForSubject =
        new SecureCxfClientFactoryImpl<>(SECURE_ENDPOINT, IDummy.class, null, null, true, true)
            .getClient();
    HTTPConduit httpConduit =
        WebClient.getConfig(WebClient.client(clientForSubject)).getHttpConduit();
    assertThat(httpConduit.getTlsClientParameters().isDisableCNCheck(), is(true));
  }

  @Test
  public void testHttpsClientWithSystemProperty() {
    PropertyResolver mockPropertyResolver = mock(PropertyResolver.class);
    when(mockPropertyResolver.getResolvedString()).thenReturn(SECURE_ENDPOINT);
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactoryImpl<>(
            SECURE_ENDPOINT, IDummy.class, null, null, false, false, mockPropertyResolver);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertThat(unsecuredClient.getBaseURI().toASCIIString(), is(SECURE_ENDPOINT));
    verify(mockPropertyResolver).getResolvedString();
  }

  @Test
  public void testKeyInfo() {
    String alias = "alias";
    String keystorePath = "/path/to/keystore";

    ClientKeyInfo keyInfo = new ClientKeyInfo(alias, keystorePath);
    assertThat(keyInfo.getAlias(), is(alias));
    assertThat(keyInfo.getKeystorePath(), is(keystorePath));
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
    public boolean isGuest() {
      return false;
    }

    @Override
    public String getName() {
      return "Dummy Subject";
    }
  }
}
