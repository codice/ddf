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
package ddf.security.sts.claimsHandler;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.sts.claims.ClaimsHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import ddf.security.encryption.EncryptionService;

/**
 * Tests out the ClaimsHandlerManager.
 * 
 */
public class ClaimsHandlerManagerTest {

    private BundleContext context;

    private EncryptionService encryptService;

    private ServiceRegistration<ClaimsHandler> handlerReg;

    /**
     * Create a new BundleContext, EncryptionService, and ServiceRegistration
     * before each test.
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        context = mock(BundleContext.class);
        encryptService = mock(EncryptionService.class);
        handlerReg = mock(ServiceRegistration.class);
        when(
                context.registerService(eq(ClaimsHandler.class), any(ClaimsHandler.class),
                        Matchers.<Dictionary<String, Object>> any())).thenReturn(handlerReg);
    }

    /**
     * Test registration of the role and ldap claims handler.
     */
    @Test
    public void registerHandlers() {
        Map<String, String> managerConfig = new HashMap<String, String>();
        managerConfig.put("ldapBindUserDn", "cn=admin");
        managerConfig.put("userBaseDn", "ou=users,dc=example,dc=com");
        managerConfig.put("groupBaseDn", "ou=groups,dc=example,dc=com");
        managerConfig.put("userNameAttribute", "uid");
        managerConfig.put("url", "ldap://ldap:1389");
        managerConfig.put("userDn", "cn=admin");
        managerConfig.put("password", "ENC(c+GitDfYAMTDRESXSDDsMw==)");
        managerConfig.put("propertyFileLocation", "etc/ws-security/attributeMap.properties");

        ClaimsHandlerManager manager = new ClaimsHandlerManager(encryptService, context,
                managerConfig);
        // verify initial registration
        verify(context, times(2)).registerService(eq(ClaimsHandler.class),
                any(ClaimsHandler.class), Matchers.<Dictionary<String, Object>> any());
        verify(handlerReg, never()).unregister();

        // new role and ldap should be unregistered and then registered
        managerConfig.put("url", "ldap://test-ldap:1389");
        manager.update(managerConfig);
        verify(context, times(4)).registerService(eq(ClaimsHandler.class),
                any(ClaimsHandler.class), Matchers.<Dictionary<String, Object>> any());
        verify(handlerReg, times(2)).unregister();
    }

}
