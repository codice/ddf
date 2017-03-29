/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.ldap.ldaplogin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.fail;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.BIND_METHOD;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_PASSWORD;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_URL;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.CONNECTION_USERNAME;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.KDC_ADDRESS;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.REALM;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_BASE_DN;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_FILTER;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_NAME_ATTRIBUTE;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.ROLE_SEARCH_SUBTREE;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.SSL_STARTTLS;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_BASE_DN;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_FILTER;
import static ddf.ldap.ldaplogin.SslLdapLoginModule.USER_SEARCH_SUBTREE;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import com.github.trevershick.test.ldap.LdapServerResource;
import com.github.trevershick.test.ldap.annotations.LdapAttribute;
import com.github.trevershick.test.ldap.annotations.LdapConfiguration;
import com.github.trevershick.test.ldap.annotations.LdapEntry;

import ddf.security.encryption.impl.EncryptionServiceImpl;

//Create and populate LDAP server
@LdapConfiguration(useRandomPortAsFallback = true, bindDn = "cn=admin", password = "secret", base = @LdapEntry(dn = "dc=example,dc=com", objectclass = {
        "top", "domain"}), entries = {
        @LdapEntry(dn = "ou=groups,dc=example,dc=com", objectclass = {"organizationalUnit", "top"}),
        @LdapEntry(dn = "cn=avengers,ou=groups,dc=example,dc=com", objectclass = {"groupOfNames",
                "top"}, attributes = {
                @LdapAttribute(name = "member", value = "uid=tstark,ou=users,dc=example,dc=com")}),
        @LdapEntry(dn = "ou=users,dc=example,dc=com", objectclass = {"organizationalUnit",
                "top"}, attributes = {@LdapAttribute(name = "ou", value = "organizationalUnit")}),
        @LdapEntry(dn = "uid=tstark,ou=users,dc=example,dc=com", objectclass = {"person",
                "inetOrgPerson", "top"}, attributes = {
                @LdapAttribute(name = "cn", value = "Tony Stark"),
                @LdapAttribute(name = "sn", value = "tstark"),
                @LdapAttribute(name = "employeeType", value = "avenger"),
                @LdapAttribute(name = "userPassword", value = "password1")})})

public class LdapModuleTest {

    @Rule
    public final ProvideSystemProperty httpsCipherSuites = new ProvideSystemProperty(
            "https.cipherSuites",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,"
                    + "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,"
                    + "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

    @Rule
    public final ProvideSystemProperty httpsProtocols = new ProvideSystemProperty("https.protocols",
            "TLSv1.1,TLSv1.2");

    @Rule
    public final ProvideSystemProperty ddfHome = new ProvideSystemProperty("ddf.home", "");

    private LdapServerResource server;

    private Map<String, String> options;

    @Before
    public void startup() {
        try {
            server = new LdapServerResource(this).start();
        } catch (Exception e) {
            fail("Could not start LDAP test server");
        }

        options = new HashMap<>();
        options.put(CONNECTION_URL, getUrl("ldap"));
        options.put(CONNECTION_USERNAME, "cn=admin");
        options.put(USER_BASE_DN, "ou=users,dc=example,dc=com");
        options.put(USER_FILTER, "(uid=tstark)");
        options.put(USER_SEARCH_SUBTREE, "true");
        options.put(ROLE_FILTER, "(member=uid=%u,ou=users,dc=example,dc=com)");
        options.put(ROLE_BASE_DN, "ou=groups,dc=example,dc=com");
        options.put(ROLE_NAME_ATTRIBUTE, "cn");
        options.put(ROLE_SEARCH_SUBTREE, "true");
        options.put(SSL_STARTTLS, "false");
        options.put(BIND_METHOD, "Simple");
        options.put(REALM, "");
        options.put(KDC_ADDRESS, "");

        // If the class-under-test cannot get the encryption service,
        // assume the password is already decrypted.
        options.put(CONNECTION_PASSWORD, "secret");
    }

    @After
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testSuccessfulLdapLogin() throws InterruptedException, LoginException {

        CallbackHandler mockHandler = getNamePasswordHandler("tstark", "password1");
        SslLdapLoginModule module = getTestModule();
        module.initialize(new Subject(), mockHandler, new HashMap<String, String>(), options);
        module.login();

        Set<Principal> principals;
        principals = module.getPrincipals();
        assertThat(principals,
                containsInAnyOrder(new UserPrincipal("tstark"), new RolePrincipal("avengers")));
    }

    @Test
    public void testIncorrectPassword() throws LoginException {
        CallbackHandler mockHandler = getNamePasswordHandler("tstark",
                "Ce n'est pas un mot de passe");
        SslLdapLoginModule module = getTestModule();
        module.initialize(new Subject(), mockHandler, new HashMap<String, String>(), options);
        module.login();
        assertThat("Expected no principals assigned to user", module.getPrincipals(), empty());
    }

    @Test(expected = LoginException.class)
    public void testInvalidPassword() throws LoginException {
        CallbackHandler mockHandler = getNamePasswordHandler("<>", "");
        SslLdapLoginModule module = getTestModule();
        module.initialize(new Subject(), mockHandler, new HashMap<String, String>(), options);
        module.login();
    }

    @Test
    public void testLdapsLogin() {
        // TODO: DDF-2876 - Enhance LDAP tests to use SSL/TLS
    }

    @Test
    public void testStartTls() {
        // TODO: DDF-2876 - Enhance LDAP tests to use SSL/TLS
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

    private SslLdapLoginModule getTestModule() {
        SslLdapLoginModule module = new SslLdapLoginModule();
        module.setEncryptionService(new EncryptionServiceImpl());
        return module;
    }

    private CallbackHandler getNamePasswordHandler(String user, String password) {
        return callbacks -> {
            ((NameCallback) callbacks[0]).setName(user);
            ((PasswordCallback) callbacks[1]).setPassword(password.toCharArray());
        };
    }

    private String getUrl(String protocol) {
        return String.format("%s://localhost:%s", protocol, server.port());
    }
}
