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
import ddf.security.service.SecurityServiceException;
import ddf.security.settings.SecuritySettingsService;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
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

import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SecureCxfClientFactoryTest {

    private String insecureEndpoint = "http://some.url.com/query";

    private String secureEndpoint = "https://some.url.com/query";

    @Test
    public void testInsecure() throws SecurityServiceException {
        // positive case
        SecureCxfClientFactory<IDummy> secureCxfClientFactory = new SecureCxfClientFactory<>(
                insecureEndpoint, IDummy.class);
        Client unsecuredClient = WebClient.client(secureCxfClientFactory.getUnsecuredClient());
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
        // positive cases
        MockWrapper<IDummy> mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class);
        validateConfig(mockWrapper, null, null, false);
        mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class, "foobar", "foobaz");
        validateConfig(mockWrapper, "foobar", "foobaz", false);
        List<MockProvider> providers = Arrays.asList(new MockProvider());
        mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class, "bazfoo", "bazbar", providers,
                true);
        validateConfig(mockWrapper, "bazfoo", "bazbar", true);
        // negative case
        boolean unsecured = false;
        try {
            mockWrapper.getUnsecuredClient();
        } catch (SecurityServiceException e) {
            unsecured = true;
        }
        assertTrue(unsecured);
    }

    private void validateConfig(MockWrapper<IDummy> factory, String username, String password,
            boolean disableCnCheck) throws SecurityServiceException {
        IDummy clientForSubject = factory.getClientForSubject(getSubject());
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

        public MockWrapper(String endpointUrl, Class<T> interfaceClass, String username,
                String password) throws SecurityServiceException {
            super(endpointUrl, interfaceClass, username, password);
        }

        public MockWrapper(String endpointUrl, Class<T> interfaceClass, String username,
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
            }
            return null;
        }
    }

    private class MockProvider implements MessageBodyReader {

        @Override
        public boolean isReadable(Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType) {
            return false;
        }

        @Override
        public Object readFrom(Class type, Type genericType, Annotation[] annotations,
                MediaType mediaType, MultivaluedMap httpHeaders, InputStream entityStream)
                throws IOException, WebApplicationException {
            return null;
        }
    }

}
