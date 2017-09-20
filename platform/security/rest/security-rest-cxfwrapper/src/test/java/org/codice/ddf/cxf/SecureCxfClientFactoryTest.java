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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.codice.ddf.configuration.PropertyResolver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SecureCxfClientFactoryTest {

  private static final String INSECURE_ENDPOINT = "http://some.url.com/query";

  private static final String SECURE_ENDPOINT = "https://some.url.com/query";

  private static final String USERNAME = "username";

  private static final String PASSWORD = "password";

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
  public void testConstructor() throws SecurityServiceException {
    //negative tests
    SecureCxfClientFactory<IDummy> secureCxfClientFactory;
    boolean invalid = false;
    try { //test empty string for url
      secureCxfClientFactory = new SecureCxfClientFactory<>("", IDummy.class);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertTrue(invalid);
    invalid = false;
    try { //null for url
      secureCxfClientFactory = new SecureCxfClientFactory<>(null, IDummy.class);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertTrue(invalid);
    invalid = false;
    try { //null for url and class
      secureCxfClientFactory = new SecureCxfClientFactory<>(null, null);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertTrue(invalid);
    invalid = false;
    try { //null for class
      secureCxfClientFactory = new SecureCxfClientFactory<>(INSECURE_ENDPOINT, null);
    } catch (IllegalArgumentException e) {
      invalid = true;
    }
    assertTrue(invalid);
  }

  @Test
  public void testInsecureClient() throws SecurityServiceException {
    // positive case
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactory<>(INSECURE_ENDPOINT, IDummy.class);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(INSECURE_ENDPOINT));
    // negative cases
    boolean subject = true;
    secureCxfClientFactory.getClientForSubject(getSubject());
    assertTrue(subject);
    boolean system = true;
    secureCxfClientFactory.getClient();
    assertTrue(system);
    boolean unsecured = true;
    secureCxfClientFactory.getClient();
    assertTrue(unsecured);
  }

  @Test
  public void testInsecureWebClient() throws SecurityServiceException {
    // positive case
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactory<>(INSECURE_ENDPOINT, IDummy.class);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(INSECURE_ENDPOINT));
    // negative cases
    boolean subject = true;
    secureCxfClientFactory.getWebClientForSubject(getSubject());
    assertTrue(subject);
    boolean system = true;
    secureCxfClientFactory.getWebClient();
    assertTrue(system);
    boolean unsecured = true;
    secureCxfClientFactory.getClient();
    assertTrue(unsecured);
  }

  @Test
  public void testHttpsClient() throws SecurityServiceException {
    // positive case
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactory<>(SECURE_ENDPOINT, IDummy.class);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(SECURE_ENDPOINT));
    // negative cases
    boolean subject = true;
    secureCxfClientFactory.getClientForSubject(getSubject());
    assertTrue(subject);
    boolean system = true;
    secureCxfClientFactory.getClient();
    assertTrue(system);
    boolean unsecured = true;
    secureCxfClientFactory.getClient();
    assertTrue(unsecured);
  }

  @Test
  public void testHttpsWebClient() throws SecurityServiceException {
    // positive case
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactory<>(SECURE_ENDPOINT, IDummy.class);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(SECURE_ENDPOINT));
    // negative cases
    boolean subject = true;
    secureCxfClientFactory.getWebClientForSubject(getSubject());
    assertTrue(subject);
    boolean system = true;
    secureCxfClientFactory.getWebClient();
    assertTrue(system);
    boolean unsecured = true;
    secureCxfClientFactory.getWebClient();
    assertTrue(unsecured);
  }

  @Test
  public void validateConduit() throws SecurityServiceException {
    IDummy clientForSubject =
        new SecureCxfClientFactory<>(SECURE_ENDPOINT, IDummy.class, null, null, true, true)
            .getClient();
    HTTPConduit httpConduit =
        WebClient.getConfig(WebClient.client(clientForSubject)).getHttpConduit();
    assertThat(httpConduit.getTlsClientParameters().isDisableCNCheck(), is(true));
  }

  @Test
  public void testHttpsClientWithSystemProperty() {
    PropertyResolver mockPropertyResolver = mock(PropertyResolver.class);
    when(mockPropertyResolver.getResolvedString()).thenReturn(SECURE_ENDPOINT);
    // positive case
    SecureCxfClientFactory<IDummy> secureCxfClientFactory =
        new SecureCxfClientFactory<>(
            SECURE_ENDPOINT, IDummy.class, null, null, false, false, mockPropertyResolver);
    Client unsecuredClient = WebClient.client(secureCxfClientFactory.getClient());
    assertThat(unsecuredClient.getBaseURI().toASCIIString(), is(SECURE_ENDPOINT));
    verify(mockPropertyResolver).getResolvedString();
    // negative cases
    IDummy result;
    result = secureCxfClientFactory.getClientForSubject(getSubject());
    assertThat(result, notNullValue());
    result = secureCxfClientFactory.getClient();
    assertThat(result, notNullValue());
    secureCxfClientFactory.getClient();
    assertThat(result, notNullValue());
  }

  @Test
  public void testWebClient() {
    PropertyResolver mockPropertyResolver = mock(PropertyResolver.class);
    when(mockPropertyResolver.getResolvedString()).thenReturn(SECURE_ENDPOINT);
    // positive case
    SecureCxfClientFactory<WebClient> secureCxfClientFactory =
        new SecureCxfClientFactory<>(
            SECURE_ENDPOINT, WebClient.class, null, null, false, false, mockPropertyResolver);
    WebClient client = secureCxfClientFactory.getWebClient();
    assertThat(client, notNullValue());
  }

  private DummySubject getSubject() {
    return new DummySubject(new DefaultSecurityManager(), new SimplePrincipalCollection());
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
  }

  private class MockWrapper<T> extends SecureCxfClientFactory<T> {

    public MockWrapper(String endpointUrl, Class<T> interfaceClass)
        throws SecurityServiceException {
      super(endpointUrl, interfaceClass);
    }
  }
}
