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
package org.codice.ddf.cxf;

import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.settings.SecuritySettingsService;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Test;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecureCxfClientFactoryTest {

    private String insecureEndpoint = "http://some.url.com/query";

    private String secureEndpoint = "https://some.url.com/query";

    @Test
    public void testInsecure() throws SecurityServiceException {
        // positive case
        SecureCxfClientFactory secureCxfClientFactory = new SecureCxfClientFactory(insecureEndpoint,
                Dummy.class);
        Client unsecuredClient = (Client) secureCxfClientFactory.getUnsecuredClient();
        assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(insecureEndpoint));
        // negative cases
        boolean subject = false;
        try {
            secureCxfClientFactory.getClientForSubject(getSubject());
        } catch (SecurityServiceException e) {
            subject = true;
        }
        assertTrue(subject);
        boolean system = false;
        try {
            secureCxfClientFactory.getClientForSystem();
        } catch (SecurityServiceException e) {
            system = true;
        }
        assertTrue(system);
    }

    @Test
    public void testSecure() throws SecurityServiceException {
        SecureCxfClientFactory secureCxfClientFactory = new MockWrapper(secureEndpoint,
                Dummy.class);
        Client client = (Client) secureCxfClientFactory.getClientForSubject(getSubject());
    }

    private DummySubject getSubject() {
        return new DummySubject(new DefaultSecurityManager(), new SimplePrincipalCollection());
    }

    public interface Dummy {
        @GET
        public Response ok();
    }

    private class DummySubject extends DelegatingSubject implements Subject {

        public DummySubject(org.apache.shiro.mgt.SecurityManager manager,
                PrincipalCollection principals) {
            super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
        }

        @Override
        public boolean isAnonymous() {
            return false;
        }
    }

    private class MockWrapper extends SecureCxfClientFactory<Dummy> {

        public MockWrapper(String endpointUrl, Class interfaceClass)
                throws SecurityServiceException {
            super(endpointUrl, interfaceClass);
        }

        public MockWrapper(String endpointUrl, Class interfaceClass, String username,
                String password) throws SecurityServiceException {
            super(endpointUrl, interfaceClass, username, password);
        }

        public MockWrapper(String endpointUrl, Class interfaceClass, String username,
                String password, List providers, boolean disableCnCheck)
                throws SecurityServiceException {
            super(endpointUrl, interfaceClass, username, password, providers, disableCnCheck);
        }

        @Override
        <S> S getOsgiService(Class<S> clazz) throws SecurityServiceException {
            if (clazz.getName().equals(SecuritySettingsService.class.getName())) {
                SecuritySettingsService securitySettingsService = mock(
                        SecuritySettingsService.class);
                when(securitySettingsService.getTLSParameters())
                        .thenReturn(new TLSClientParameters());
                return (S) securitySettingsService;
            } else if (clazz.getName().equals(SecurityManager.class.getName())) {
                SecurityManager securityManager = mock(SecurityManager.class);
                when(securityManager.getSubject(notNull())).thenReturn(getSubject());
                return (S) securityManager;
            }
            return null;
        }
    }

}
