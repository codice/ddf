/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
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
        when(
                context.registerService(eq(JaasRealm.class), any(JaasRealm.class),
                        Matchers.<Dictionary<String, Object>> any())).thenReturn(jaasRealm);
    }

    /**
     * Verifies that the jaasrealm is property registered and unregistered.
     */
    @Test
    public void testRegisterLdapModule() {
        Map<String, String> ldapProps = new HashMap<String, String>();
        ldapProps.put("ldapBindUserDn", "cn=admin");
        ldapProps.put("ldapBindUserPass", "ENC(c+GitDfYAMTDRESXSDDsMw==)");
        ldapProps.put("ldapUrl", "ldaps://ldap:1636");
        ldapProps.put("userBaseDn", "ou=users,dc=example,dc=com");
        ldapProps.put("groupBaseDn", "ou=groups,dc=example,dc=com");
        ldapProps.put("keyAlias", "server");

        LdapLoginConfig ldapConfig = new LdapLoginConfig(context, ldapProps);
        verify(context).registerService(eq(JaasRealm.class), any(JaasRealm.class),
                Matchers.<Dictionary<String, Object>> any());

        ldapProps.put("ldapUrl", "ldaps://test-ldap:1636");
        ldapConfig.update(ldapProps);
        // verify previous service was unregistered
        verify(jaasRealm).unregister();
        verify(context, times(2)).registerService(eq(JaasRealm.class), any(JaasRealm.class),
                Matchers.<Dictionary<String, Object>> any());

    }

}
