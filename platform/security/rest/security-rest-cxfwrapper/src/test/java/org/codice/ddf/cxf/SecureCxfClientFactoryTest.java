/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.cxf;

import static org.junit.Assert.assertTrue;

import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Test;

import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;

public class SecureCxfClientFactoryTest {

    private String insecureEndpoint = "http://some.url.com/query";

    private String secureEndpoint = "https://some.url.com/query";

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
            secureCxfClientFactory = new SecureCxfClientFactory<>(insecureEndpoint, null);
        } catch (IllegalArgumentException e) {
            invalid = true;
        }
        assertTrue(invalid);
    }

    @Test
    public void testInsecureClient() throws SecurityServiceException {
        // positive case
        SecureCxfClientFactory<IDummy> secureCxfClientFactory = new SecureCxfClientFactory<>(
                insecureEndpoint, IDummy.class);
        Client unsecuredClient = WebClient.client(secureCxfClientFactory.getUnsecuredClient());
        assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(insecureEndpoint));
        // negative cases
        boolean subject = false;
        try { //can't get secure client from insecure endpoint
            secureCxfClientFactory.getClientForSubject(getSubject());
        } catch (SecurityServiceException e) {
            subject = true;
        }
        assertTrue(subject);
        boolean system = false;
        try { //can't get secure client from insecure endpoint
            secureCxfClientFactory.getClientForBasicAuth("username", "password");
        } catch (SecurityServiceException e) {
            system = true;
        }
        assertTrue(system);
        boolean unsecured = true;
        try { //should be able to get an unsecured client
            secureCxfClientFactory.getUnsecuredClient();
        } catch (SecurityServiceException e) {
            unsecured = false;
        }
        assertTrue(unsecured);
    }

    @Test
    public void testInsecureWebClient() throws SecurityServiceException {
        // positive case
        SecureCxfClientFactory<IDummy> secureCxfClientFactory = new SecureCxfClientFactory<>(
                insecureEndpoint, IDummy.class);
        Client unsecuredClient = WebClient.client(secureCxfClientFactory.getUnsecuredClient());
        assertTrue(unsecuredClient.getBaseURI().toASCIIString().equals(insecureEndpoint));
        // negative cases
        boolean subject = false;
        try { //can't get secure client from insecure endpoint
            secureCxfClientFactory.getWebClientForSubject(getSubject());
        } catch (SecurityServiceException e) {
            subject = true;
        }
        assertTrue(subject);
        boolean system = false;
        try { //can't get secure client from insecure endpoint
            secureCxfClientFactory.getWebClientForBasicAuth("username", "password");
        } catch (SecurityServiceException e) {
            system = true;
        }
        assertTrue(system);
        boolean unsecured = true;
        try { //should be able to get an unsecured client
            secureCxfClientFactory.getUnsecuredWebClient();
        } catch (SecurityServiceException e) {
            unsecured = false;
        }
        assertTrue(unsecured);
    }

    @Test
    public void testGetOSGI() throws SecurityServiceException {
        MockWrapper<IDummy> mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class);
        //mockWrapper.getOsgiService((mock(SecuritySettingsService.class)).getClass());
    }

    private void validateConfig(MockWrapper<IDummy> factory, String username, String password,
            boolean disableCnCheck) throws SecurityServiceException {
        IDummy clientForSubject = factory.getClientForBasicAuth(username, password);
        HTTPConduit httpConduit = WebClient.getConfig(WebClient.client(clientForSubject))
                .getHttpConduit();
        AuthorizationPolicy authorization = httpConduit.getAuthorization();
        assertTrue(StringUtils.equals(authorization.getUserName(), (username)));
        assertTrue(StringUtils.equals(authorization.getPassword(), (password)));
        assertTrue(disableCnCheck == httpConduit.getTlsClientParameters().isDisableCNCheck());
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

        public DummySubject(org.apache.shiro.mgt.SecurityManager manager,
                PrincipalCollection principals) {
            super(principals, true, null, new SimpleSession(UUID.randomUUID().toString()), manager);
        }

        @Override
        public boolean isAnonymous() {
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
