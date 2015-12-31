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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.karaf.jaas.config.JaasRealm;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests out LdapLoginConfig functionality.
 *
 */
public class LdapLoginConfigTest {

    private BundleContext context;

    private ServiceRegistration<JaasRealm> jaasRealm;

    /**
     * Sets up a new context and jaasrealm before each test
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        context = mock(BundleContext.class);
        jaasRealm = mock(ServiceRegistration.class);
        when(context.registerService(eq(JaasRealm.class),
                any(JaasRealm.class),
                Matchers.<Dictionary<String, Object>>any())).thenReturn(jaasRealm);
    }

    /**
     * Verifies that the jaasrealm is property registered and unregistered.
     */
    @Test
    public void testRegisterLdapModule() {
        Map<String, String> ldapProps = new HashMap<String, String>();

        System.setProperty("https.cipherSuites",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
        System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");

        LdapLoginConfig ldapConfig = new LdapLoginConfig() {
            @Override
            protected BundleContext getContext() {
                return context;
            }
        };
        ldapConfig.setLdapBindUserDn("cn=admin");
        ldapConfig.setLdapBindUserPass("password");
        ldapConfig.setLdapUrl("ldaps://ldap:1636");
        ldapConfig.setUserBaseDn("ou=users,dc=example,dc=com");
        ldapConfig.setGroupBaseDn("ou=groups,dc=example,dc=com");
        ldapConfig.setKeyAlias("server");
        ldapConfig.setStartTls(false);
        ldapConfig.configure();

        verify(context).registerService(eq(JaasRealm.class),
                any(JaasRealm.class),
                Matchers.<Dictionary<String, Object>>any());

        ldapProps.put(LdapLoginConfig.LDAP_BIND_USER_DN, "cn=admin");
        ldapProps.put(LdapLoginConfig.LDAP_BIND_USER_PASS, "secret");
        ldapProps.put(LdapLoginConfig.USER_BASE_DN, "ou=users,dc=example,dc=com");
        ldapProps.put(LdapLoginConfig.GROUP_BASE_DN, "ou=groups,dc=example,dc=com");
        ldapProps.put(LdapLoginConfig.KEY_ALIAS, "server");
        ldapProps.put(LdapLoginConfig.LDAP_URL, "ldaps://test-ldap:1636");
        ldapProps.put(LdapLoginConfig.START_TLS, "false");
        ldapConfig.update(ldapProps);
        // verify previous service was unregistered
        verify(jaasRealm).unregister();
        verify(context, times(2)).registerService(eq(JaasRealm.class),
                any(JaasRealm.class),
                Matchers.<Dictionary<String, Object>>any());

    }

}
