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
package ddf.ldap.ldaplogin;

import static ddf.ldap.ldaplogin.SslLdapLoginModule.BIND_METHOD_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_PASSWORD_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_USERNAME_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.KDC_ADDRESS_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.REALM_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_BASE_DN_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_FILTER_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_NAME_ATTRIBUTE_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_SEARCH_SUBTREE_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_BASE_DN_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_FILTER_OPTIONS_KEY;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_SEARCH_SUBTREE_OPTIONS_KEY;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import com.unboundid.util.ssl.KeyStoreKeyManager;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import ddf.security.encryption.EncryptionService;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

public class LdapModuleTest {

  public static final String USER_CN = "tstark";

  public static final String EXPECTED_GROUP_CN = "avengers";

  // Password needs to match the user's password in the LDIF file used
  // to populate the LDAP server's Directory Information Tree.
  public static final String USER_PASSWORD = "password1";

  @Rule
  public final ProvideSystemProperty testProperties =
      ProvideSystemProperty.fromResource("/test.properties");

  private TestServer server;

  private TestModule module;

  public LdapModuleTest() throws IOException {}

  @Before
  public void startup() throws LdapException {

    server = TestServer.getInstance();
    LdapLoginConfig ldapLoginConfig = new LdapLoginConfig(null);
    ConnectionFactory ldapConnectionFactory =
        ldapLoginConfig.createLdapConnectionFactory(TestServer.getUrl("ldap"), false);
    module =
        TestModule.getInstance(
            TestServer.getClientOptions(),
            new GenericObjectPool<>(new LdapConnectionPooledObjectFactory(ldapConnectionFactory)));
  }

  @After
  public void shutdown() {
    server.shutdown();
    server = null;
    module = null;
  }

  @Test
  public void testLdapLoginAndLogout() throws InterruptedException, LoginException {

    server.useSimpleAuth().startListening();
    module
        .setUsernameAndPassword(USER_CN, USER_PASSWORD)
        .login()
        .assertThatPrincipals(server.expectedPrincipals());
    module.logout().assertThatPrincipals(server.emptyPrincipals());
  }

  @Test
  public void testNoAuth() throws InterruptedException, LoginException {

    server.startListening();
    module
        .putOption(BIND_METHOD_OPTIONS_KEY, "none")
        .putOption(CONNECTION_USERNAME_OPTIONS_KEY, "")
        .putOption(CONNECTION_PASSWORD_OPTIONS_KEY, "")
        .setUsernameAndPassword(USER_CN, USER_PASSWORD)
        .login()
        .assertThatPrincipals(server.expectedPrincipals());
  }

  @Test
  public void testLdapSecureLogin() throws LoginException {

    server.useSimpleAuth().startListening();
    module
        .setUsernameAndPassword(USER_CN, USER_PASSWORD)
        .login()
        .assertThatPrincipals(server.expectedPrincipals());
  }

  // Unable to get the LDAP server to accept startTLS. Have tried with
  // Apache Directory Studio, an OpenDJ client, and ;an UnboundedID client.
  @Ignore
  @Test
  public void testStartTlsWithLdap() throws LoginException {
    server.useSimpleAuth();
    module
        .setUsernameAndPassword(USER_CN, USER_PASSWORD)
        .login()
        .assertThatPrincipals(server.expectedPrincipals());
  }

  @Test
  public void testIncorrectPassword() throws LoginException {
    server.startListening();
    module
        .setUsernameAndPassword(USER_CN, "Ce n'est pas un mot de passe")
        .login()
        .assertThatPrincipals(server.emptyPrincipals());
  }

  @Test(expected = LoginException.class)
  public void testInvalidPassword() throws LoginException {
    server.startListening();
    module.setUsernameAndPassword("<>", "").login();
  }

  @Test
  public void testUserSwitchNoAuthToSimpleAuth() throws LoginException {

    server.startListening();
    module
        .setUsernameAndPassword(USER_CN, USER_PASSWORD)
        .putOption(BIND_METHOD_OPTIONS_KEY, "none")
        .login();
    assertThat(module.getBindMethod(), equalToIgnoringCase("simple"));
  }

  @Test
  public void testSasl() {
    // TODO: DDF-2877 - Enhance LDAP test for alt protocols
  }

  @Test
  public void testGssapiSasl() {
    // TODO: DDF-2877 - Enhance LDAP test for alt protocols

  }

  @Test
  public void testDigestMd5Sasl() {
    // TODO: DDF-2877 - Enhance LDAP test for alt protocols

  }

  private static class TestModule {
    SslLdapLoginModule realModule;

    private Map<String, String> options;

    private CallbackHandler callbackHandler;

    private TestModule() {}

    public static TestModule getInstance(
        Map<String, String> options, GenericObjectPool<Connection> connectionPool) {
      TestModule object = new TestModule();
      object.options = new HashMap<>(options);
      object.realModule = new SslLdapLoginModule();
      EncryptionService mockEncryptionService = mock(EncryptionService.class);
      when(mockEncryptionService.decryptValue(anyString())).then(returnsFirstArg());
      object.realModule.setEncryptionService(mockEncryptionService);
      object.realModule.setLdapConnectionPool(connectionPool);
      return object;
    }

    public TestModule login() throws LoginException {
      realModule.initialize(new Subject(), callbackHandler, new HashMap<String, String>(), options);
      realModule.login();
      return this;
    }

    public TestModule logout() throws LoginException {
      realModule.logout();
      return this;
    }

    public TestModule setUsernameAndPassword(String username, String password) {
      callbackHandler =
          callbacks -> {
            ((NameCallback) callbacks[0]).setName(username);
            ((PasswordCallback) callbacks[1])
                .setPassword(password == null ? null : password.toCharArray());
          };
      return this;
    }

    public String getBindMethod() {
      return realModule.getBindMethod();
    }

    public void assertThatPrincipals(Collection<Principal> expectedPrincipals) {
      Set<Principal> actualPrincipals = realModule.getPrincipals();
      assertThat(actualPrincipals, hasSize(expectedPrincipals.size()));
      for (Principal each : expectedPrincipals) {
        assertThat(actualPrincipals, hasItem(each));
      }
    }

    public TestModule putOption(String key, String value) {
      options.put(key, value);
      return this;
    }
  } // end inner class

  private static class TestServer {

    private InMemoryDirectoryServer realServer;

    private InMemoryDirectoryServerConfig serverConfig;

    public static String getBaseDistinguishedName() {
      return "dc=example,dc=com";
    }

    public static TestServer getInstance() {
      TestServer object = new TestServer();
      try {
        InMemoryListenerConfig ldapConfig =
            InMemoryListenerConfig.createLDAPConfig(getBaseDistinguishedName(), getLdapPort());
        InMemoryListenerConfig ldapsConfig =
            InMemoryListenerConfig.createLDAPSConfig(
                "ldaps",
                getLdapSecurePort(),
                object.getServerSSLContext().getServerSocketFactory());
        object.serverConfig = new InMemoryDirectoryServerConfig(getBaseDistinguishedName());
        object.serverConfig.setListenerConfigs(ldapConfig, ldapsConfig);

      } catch (LDAPException e) {
        fail(e.getMessage());
      }
      return object;
    }

    public static String getBasicAuthPassword() {
      return "secret";
    }

    public static String getBasicAuthDn() {
      return "cn=admin";
    }

    public static Map<String, String> getClientOptions() {

      HashMap<String, String> options = new HashMap<>();
      options.put(CONNECTION_USERNAME_OPTIONS_KEY, getBasicAuthDn());
      options.put(CONNECTION_PASSWORD_OPTIONS_KEY, getBasicAuthPassword());
      options.put(USER_BASE_DN_OPTIONS_KEY, getBaseDistinguishedName());
      options.put(USER_FILTER_OPTIONS_KEY, String.format("(%s)", "uid=tstark"));
      options.put(USER_SEARCH_SUBTREE_OPTIONS_KEY, "true");
      options.put(
          ROLE_FILTER_OPTIONS_KEY,
          String.format("(member=uid=%%u,ou=users,%s)", getBaseDistinguishedName()));
      options.put(
          ROLE_BASE_DN_OPTIONS_KEY, String.format("ou=groups,%s", getBaseDistinguishedName()));
      options.put(ROLE_NAME_ATTRIBUTE_OPTIONS_KEY, "cn");
      options.put(ROLE_SEARCH_SUBTREE_OPTIONS_KEY, "true");
      options.put(BIND_METHOD_OPTIONS_KEY, "Simple");
      options.put(REALM_OPTIONS_KEY, "");
      options.put(KDC_ADDRESS_OPTIONS_KEY, "");
      return options;
    }

    public static int getLdapPort() {
      // return server.getListenPort("ldap");
      return 1389;
    }

    public static int getLdapSecurePort() {
      // return server.getListenPort("ldaps");
      return 1636;
    }

    public static String getUrl(String protocol) {
      String url = null;
      switch (protocol) {
        case "ldap":
          url = String.format("ldap://localhost:%s", getLdapPort());
          break;
        case "ldaps":
          url = String.format("ldaps://localhost:%s", getLdapSecurePort());
          break;
        default:
          fail("Unknown LDAP bind protocol");
      }
      return url;
    }

    SSLContext getServerSSLContext() {
      try {
        char[] keyStorePassword = "changeit".toCharArray();
        String keystore = getClass().getResource("/serverKeystore.jks").getFile();
        KeyStoreKeyManager keyManager =
            new KeyStoreKeyManager(keystore, keyStorePassword, "JKS", "localhost");
        String truststore = getClass().getResource("/serverTruststore.jks").getFile();
        TrustStoreTrustManager trustManager =
            new TrustStoreTrustManager(truststore, keyStorePassword, null, false);
        return new SSLUtil(keyManager, trustManager).createSSLContext();
      } catch (GeneralSecurityException e) {
        fail(e.getMessage());
      }
      return null;
    }

    public TestServer useSimpleAuth() {

      try {
        serverConfig.addAdditionalBindCredentials(getBasicAuthDn(), getBasicAuthPassword());
      } catch (LDAPException e) {
        fail(e.getMessage());
      }
      return this;
    }

    public TestServer startListening() {
      try {
        realServer = new InMemoryDirectoryServer(serverConfig);
        realServer.startListening();
      } catch (LDAPException e) {
        fail(e.getMessage());
      }
      loadLdifFile();
      return this;
    }

    public void shutdown() {
      if (realServer != null) {
        realServer.shutDown(true);
      }
      realServer = null;
    }

    // The actual values are controlled by the contents of the LDIF file. In this case,
    // we expect two roles, one for identify and one for the user's sole group.
    public Set<Principal> expectedPrincipals() {
      Set<Principal> set = new HashSet<>();
      set.add(new UserPrincipal(USER_CN));
      set.add(new RolePrincipal(EXPECTED_GROUP_CN));
      return set;
    }

    public Set<Principal> emptyPrincipals() {
      return new HashSet<>();
    }

    void loadLdifFile() {
      try (InputStream ldifStream = getClass().getResourceAsStream("/test-ldap.ldif")) {
        assertThat("Cannot find LDIF test resource file", ldifStream, is(notNullValue()));
        LDIFReader reader = new LDIFReader(ldifStream);
        LDIFChangeRecord readEntry;
        while ((readEntry = reader.readChangeRecord()) != null) {
          readEntry.processChange(realServer);
        }
      } catch (IOException | LDIFException | LDAPException e) {
        fail(e.getMessage());
      }
    }
  } // end inner class
}
