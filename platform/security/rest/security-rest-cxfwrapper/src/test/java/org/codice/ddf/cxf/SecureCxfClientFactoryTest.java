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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import ddf.security.settings.SecuritySettingsService;

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

    /*@Test
    public void testSecureClient() throws SecurityServiceException {
        MockWrapper<IDummy> mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class);
        // positive cases
        boolean tests = true;
        try { //should be able to get secure clients
            validateConfig(mockWrapper, null, null,
                    false); //should work with no (null) username and password
            mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class);
            validateConfig(mockWrapper, "foobar", "foobaz",
                    false); //test with username and password
            List<MockProvider> providers = Arrays.asList(new MockProvider()); //test with provider
            mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class, providers, null, true);
            validateConfig(mockWrapper, "bazfoo", "bazbar", true); //test with CN check disabled
            Interceptor interceptor = mock(Interceptor.class);
            mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class, providers, interceptor,
                    true);
            validateConfig(mockWrapper, "usr", "psw", true); //test with providers and interceptor
        } catch (SecurityServiceException e) {
            tests = false;
        }
        assertTrue(tests);

        // negative case
        boolean unsecured = false;
        try { //should not be able to get unsecured client over https
            mockWrapper.getUnsecuredClient();
        } catch (SecurityServiceException e) {
            unsecured = true;
        }
        assertTrue(unsecured);
        boolean secured = true;
        try { //should be able to get secured client from subject
            mockWrapper.getClientForSubject(getSubject());
        } catch (SecurityServiceException e) {
            secured = false;
        }
        assertTrue(secured);
        boolean badSubject = false;
        try { //bad subject should not allow a secure client
            mockWrapper.getClientForSubject(null);
        } catch (SecurityServiceException e) {
            badSubject = true;
        }
        assertTrue(badSubject);
        boolean basicAuth = true;
        try { //should be able to use basicAuth for secure client
            mockWrapper.getClientForBasicAuth("username", "password");
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
        basicAuth = true;
        try { //testing blank strings with basicAuth
            mockWrapper.getClientForBasicAuth("", "");
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
        basicAuth = true;
        try { //testing nulls with basicAuth
            mockWrapper.getClientForBasicAuth(null, null);
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
    }*/

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

    /*@Test
    public void testSecureWebClient() throws SecurityServiceException {
        MockWrapper<IDummy> mockWrapper = new MockWrapper<>(secureEndpoint, IDummy.class);
        // negative case
        boolean unsecured = false;
        try { //should not be able to get unsecured client over https
            mockWrapper.getUnsecuredWebClient();
        } catch (SecurityServiceException e) {
            unsecured = true;
        }
        assertTrue(unsecured);
        boolean secured = true;
        try { //should be able to get secured client from subject
            mockWrapper.getWebClientForSubject(getSubject());
        } catch (SecurityServiceException e) {
            secured = false;
        }
        assertTrue(secured);
        boolean badSubject = false;
        try { //bad subject should not allow a secure client
            mockWrapper.getWebClientForSubject(null);
        } catch (SecurityServiceException e) {
            badSubject = true;
        }
        assertTrue(badSubject);
        boolean basicAuth = true;
        try { //should be able to use basicAuth for secure client
            mockWrapper.getWebClientForBasicAuth("username", "password");
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
        basicAuth = true;
        try { //testing blank strings with basicAuth
            mockWrapper.getWebClientForBasicAuth("", "");
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
        basicAuth = true;
        try { //testing nulls with basicAuth
            mockWrapper.getWebClientForBasicAuth(null, null);
        } catch (SecurityServiceException e) {
            basicAuth = false;
        }
        assertTrue(basicAuth);
    }*/

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

        public MockWrapper(String endpointUrl, Class<T> interfaceClass, List providers,
                Interceptor<? extends Message> interceptor, boolean disableCnCheck)
                throws SecurityServiceException {
            super(endpointUrl, interfaceClass, providers, interceptor, disableCnCheck);
        }

        public BundleContext getBundleContext() {
            BundleContext bundleContext = mock(BundleContext.class);
            ServiceReference serviceReference = mock(ServiceReference.class);
            SecuritySettingsService securitySettingsService = mock(SecuritySettingsService.class);

            when(bundleContext.getServiceReference(SecuritySettingsService.class))
                    .thenReturn(serviceReference);
            when(bundleContext.getService(any(ServiceReference.class)))
                    .thenReturn(securitySettingsService);
            when(securitySettingsService.getTLSParameters()).thenReturn(new TLSClientParameters());
            return bundleContext;
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
