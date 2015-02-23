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

import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.settings.SecuritySettingsService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.shiro.subject.Subject;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.CredentialException;
import org.apache.ws.security.components.crypto.Merlin;
import org.codice.ddf.security.common.PropertiesLoader;
import org.codice.ddf.security.handler.api.PKIAuthenticationToken;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class SecureCxfClientFactory<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureCxfClientFactory.class);

    private final Client cxfClient;

    /**
     * @see #SecureCxfClientFactory(String, Class, String, String, java.util.List, boolean)
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass)
            throws SecurityServiceException {
        this(endpointUrl, interfaceClass, null, null);
    }

    /**
     * @see #SecureCxfClientFactory(String, Class, String, String, java.util.List, boolean)
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass, String username,
            String password) throws SecurityServiceException {
        this(endpointUrl, interfaceClass, username, password, null, false);
    }

    /**
     * Constructs a factory that will return security-aware cxf clients. Once constructed,
     * use the getClient* methods to retrieve a fresh client  with the same configuration.
     * <p/>
     * This factory can and should be cached. The clients it constructs should not be.
     *
     * @param endpointUrl    the remote url to connect to
     * @param interfaceClass an interface representing the resource at the remote url
     * @param username       optional username for basic auth
     * @param password       optional password for basic auth
     * @param providers      optional list of providers to further configure the client
     * @param disableCnCheck disable ssl check for common name / host name match
     */
    public SecureCxfClientFactory(String endpointUrl, Class<T> interfaceClass, String username,
            String password, List<?> providers, boolean disableCnCheck)
            throws SecurityServiceException {
        if (StringUtils.isEmpty(endpointUrl) || interfaceClass == null) {
            throw new IllegalArgumentException(
                    "Called without a valid URL, will not be able to connect.");
        }

        JAXRSClientFactoryBean jaxrsClientFactoryBean = new JAXRSClientFactoryBean();
        jaxrsClientFactoryBean.setServiceClass(interfaceClass);
        jaxrsClientFactoryBean.setAddress(endpointUrl);
        jaxrsClientFactoryBean.setClassLoader(interfaceClass.getClassLoader());
        jaxrsClientFactoryBean.getInInterceptors().add(new LoggingInInterceptor());
        jaxrsClientFactoryBean.getOutInterceptors().add(new LoggingOutInterceptor());

        if (CollectionUtils.isNotEmpty(providers)) {
            jaxrsClientFactoryBean.setProviders(providers);
        }

        Client cxfClient = WebClient.client(jaxrsClientFactoryBean.create(interfaceClass));
        if (cxfClient == null) {
            throw new SecurityServiceException("Could not construct base client");
        }
        ClientConfiguration clientConfig = WebClient.getConfig(cxfClient);

        if (!StringUtils.startsWithIgnoreCase(endpointUrl, "https")) {
            LOGGER.warn("Cannot secure non-https connection " + endpointUrl
                    + ", only unsecured clients will be created");
        } else {
            initSecurity(clientConfig, username, password);
            if (disableCnCheck) {
                disableCnCheck(clientConfig);
            }
        }

        this.cxfClient = cxfClient;
    }

    /**
     * Clients produced by this method will be secured with basic authentication
     * (if a username and password were provided), two-way ssl,
     * and the provided security subject.
     * <p/>
     * The returned client should NOT be reused between requests!
     * This method should be called for each new request in order to ensure
     * that the security subject is up-to-date each time.
     *
     * @see SecureCxfClientFactory
     */
    public T getClientForSubject(Subject subject) throws SecurityServiceException {
        String asciiString = cxfClient.getBaseURI().toASCIIString();
        if (!StringUtils.startsWithIgnoreCase(asciiString, "https")) {
            throw new SecurityServiceException("Cannot secure non-https connection " + asciiString);
        }

        T newClient = getUnsecuredClient();

        if (subject instanceof ddf.security.Subject) {
            RestSecurity.setSubjectOnClient((ddf.security.Subject) subject, (Client) newClient);
        } else {
            throw new SecurityServiceException("Not a ddf subject " + subject);
        }

        return newClient;
    }

    /**
     * Clients produced by this method will be secured with basic authentication
     * (if a username and password were provided), two-way ssl,
     * and the system security token (x509 cert).
     * <p/>
     * The system security token does expire, so the returned client should not
     * be cached. Acquire a new client instead by calling this method again.
     */
    public T getClientForSystem() throws SecurityServiceException {
        String asciiString = cxfClient.getBaseURI().toASCIIString();
        if (!StringUtils.startsWithIgnoreCase(asciiString, "https")) {
            throw new SecurityServiceException("Cannot secure non-https connection " + asciiString);
        }
        // In order to get the system x509 user subject, we need to:
        // A. Load the keystore from the filesystem.
        // B. Figure out which private key to use, normally there's only one.
        // C. Use Merlin to convert the private cert chain to bytes.
        // D. Construct a pki token and request a subject for it.
        try (FileInputStream storeStream = new FileInputStream(
                System.getProperty("javax.net.ssl.keyStore"))) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(storeStream,
                    System.getProperty("javax.net.ssl.keyStorePassword").toCharArray());
            Merlin merlin = new Merlin(
                    PropertiesLoader.loadProperties("etc/ws-security/server/signature.properties"));
            // Assuming there is exactly one private key in the keystore...
            // TODO: what if there are multiple keys?
            Certificate[] certificateChain = keyStore
                    .getCertificateChain(keyStore.aliases().nextElement());
            X509Certificate[] x509Certificates = Arrays
                    .copyOf(certificateChain, certificateChain.length, X509Certificate[].class);
            PKIAuthenticationToken pkiAuthenticationToken = new PKIAuthenticationToken(
                    x509Certificates[0].getSubjectDN(),
                    merlin.getBytesFromCertificates(x509Certificates),
                    PKIAuthenticationToken.DEFAULT_REALM);
            ddf.security.Subject subject = getSecurityManager().getSubject(pkiAuthenticationToken);
            return getClientForSubject(subject);
        } catch (CertificateException | IOException
                | NoSuchAlgorithmException | KeyStoreException | WSSecurityException | CredentialException e) {
            throw new SecurityServiceException(e);
        }
    }

    /**
     * Clients produced by this method will be completely unsecured.
     * <p/>
     * Since there is no security information to expire, this client may be reused.
     */
    public T getUnsecuredClient() {
        WebClient webClient = WebClient.fromClient(cxfClient);
        WebClient.getConfig(webClient).getRequestContext()
                .put(Message.MAINTAIN_SESSION, Boolean.TRUE);
        return (T) webClient;
    }

    private SecuritySettingsService getSecuritySettingsService() throws SecurityServiceException {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        if (bundleContext != null) {
            ServiceReference<SecuritySettingsService> serviceReference = bundleContext
                    .getServiceReference(SecuritySettingsService.class);
            if (serviceReference != null) {
                SecuritySettingsService service = bundleContext.getService(serviceReference);
                if (service != null) {
                    return service;
                }
            }
        }
        throw new SecurityServiceException("Could not get SecuritySettingsService");
    }

    private SecurityManager getSecurityManager() throws SecurityServiceException {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        if (bundleContext != null) {
            ServiceReference<SecurityManager> serviceReference = bundleContext
                    .getServiceReference(SecurityManager.class);
            if (serviceReference != null) {
                SecurityManager service = bundleContext.getService(serviceReference);
                if (service != null) {
                    return service;
                }
            }
        }
        throw new SecurityServiceException("Could not get SecurityManager");
    }

    private void disableCnCheck(ClientConfiguration clientConfig) throws SecurityServiceException {
        HTTPConduit httpConduit = clientConfig.getHttpConduit();
        if (httpConduit == null) {
            throw new SecurityServiceException(
                    "HTTPConduit was null for " + this + ". Unable to disable CN Check");
        }

        TLSClientParameters tlsParams = httpConduit.getTlsClientParameters();

        tlsParams.setDisableCNCheck(true);
    }

    /*
     * Add TLS and Basic Auth credentials to the underlying {@link org.apache.cxf.transport.http.HTTPConduit}
     * This includes two-way ssl assuming that the platform keystores are configured correctly
     */
    private void initSecurity(ClientConfiguration clientConfig, String username, String password)
            throws SecurityServiceException {
        HTTPConduit httpConduit = clientConfig.getHttpConduit();
        if (httpConduit == null) {
            throw new SecurityServiceException(
                    "HTTPConduit was null for " + this + ". Unable to configure security.");
        }

        if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            if (httpConduit.getAuthorization() != null) {
                httpConduit.getAuthorization().setUserName(username);
                httpConduit.getAuthorization().setPassword(password);
            }
        }

        TLSClientParameters tlsParams = getSecuritySettingsService().getTLSParameters();
        httpConduit.setTlsClientParameters(tlsParams);
    }

}
