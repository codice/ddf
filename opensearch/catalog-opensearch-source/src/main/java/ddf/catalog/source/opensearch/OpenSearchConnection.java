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
package ddf.catalog.source.opensearch;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.codice.ddf.endpoints.OpenSearch;
import org.codice.ddf.endpoints.rest.RESTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.core.Cookie;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;

/**
 * This class wraps the CXF JAXRS code to make it easier to use and also easier to test. Most of
 * the CXF code uses static methods to construct the web clients, which is inherently difficult to
 * mock up when testing.
 */
public class OpenSearchConnection {

    protected static final String[] SSL_ALLOWED_ALGORITHMS =
            {
                    ".*_WITH_AES_.*"
            };

    protected static final String[] SSL_DISALLOWED_ALGORITHMS =
            {
                    ".*_WITH_NULL_.*", ".*_DH_anon_.*"
            };

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(OpenSearchConnection.class);

    private static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";

    protected OpenSearch openSearch;

    protected RESTService restService;

    protected Client openSearchClient;

    protected Client restServiceClient;

    private FilterAdapter filterAdapter;

    private EncryptionService encryptionService;

    private String trustStorePath;

    private String trustStorePassword;

    private String keyStorePath;

    private String keyStorePassword;

    private String username;

    private String password;

    /**
     * Default Constructor
     * @param endpointUrl - OpenSearch URL to connect to
     * @param filterAdapter - adapter to translate between DDF REST and OpenSearch
     * @param keyStorePassword - SSL Keystore password
     * @param keyStorePath - SSL Keystore file path
     * @param trustStorePassword - SSL Truststore password
     * @param trustStorePath - SSL Truststore file path
     * @param username - Basic Auth user name
     * @param password - Basic Auth password
     * @param encryptionService - decrypt service for passwords
     */
    public OpenSearchConnection(String endpointUrl, FilterAdapter filterAdapter,
            String keyStorePassword, String keyStorePath, String trustStorePassword,
            String trustStorePath, String username, String password, EncryptionService encryptionService) {
        this.filterAdapter = filterAdapter;
        this.keyStorePassword = keyStorePassword;
        this.keyStorePath = keyStorePath;
        this.trustStorePassword = trustStorePassword;
        this.trustStorePath = trustStorePath;
        this.username = username;
        this.password = password;
        this.encryptionService = encryptionService;
        openSearch = JAXRSClientFactory.create(endpointUrl, OpenSearch.class);
        openSearchClient = WebClient.client(openSearch);

        RestUrl restUrl = newRestUrl(endpointUrl);
        if (restUrl != null) {
            restService = JAXRSClientFactory.create(restUrl.buildUrl(), RESTService.class);
            restServiceClient = WebClient.client(restService);

            if (endpointUrl.startsWith("https") && StringUtils.isNotEmpty(keyStorePassword)
                    && StringUtils.isNotEmpty(keyStorePath) && StringUtils.isNotEmpty(trustStorePassword)
                    && StringUtils.isNotEmpty(trustStorePath)) {
                setTLSOptions(openSearchClient);
                setTLSOptions(restServiceClient);
            }
        }
    }

    /**
     * Generates a DDF REST URL from an OpenSearch URL
     * @param query
     * @param endpointUrl
     * @return URL in String format
     */
    private String createRestUrl(Query query, String endpointUrl, boolean retrieveResource) {

        String url = null;
        RestFilterDelegate delegate = null;
        RestUrl restUrl = newRestUrl(endpointUrl);
        restUrl.setRetrieveResource(retrieveResource);

        if (restUrl != null) {
            delegate = new RestFilterDelegate(restUrl);
        }

        if (delegate != null) {
            try {
                filterAdapter.adapt(query, delegate);
                url = delegate.getRestUrl().buildUrl();
            } catch (UnsupportedQueryException e) {
                LOGGER.debug("Not a REST request.", e);
            }

        }

        return url;
    }

    /**
     * Creates a new RestUrl object based on an OpenSearch URL
     * @param url
     * @return RestUrl object for a DDF REST endpoint
     */
    private RestUrl newRestUrl(String url) {
        RestUrl restUrl = null;
        try {
            restUrl = RestUrl.newInstance(url);
            restUrl.setRetrieveResource(true);
        } catch (MalformedURLException e) {
            LOGGER.info("Bad url given for remote source", e);
        } catch (URISyntaxException e) {
            LOGGER.info("Bad url given for remote source", e);
        }
        return restUrl;
    }

    /**
     * Returns the OpenSearch {@link org.apache.cxf.jaxrs.client.WebClient}
     * @return {@link org.apache.cxf.jaxrs.client.WebClient}
     */
    public WebClient getOpenSearchWebClient() {
        return WebClient.fromClient(openSearchClient);
    }

    /**
     * Returns the DDF REST {@link org.apache.cxf.jaxrs.client.WebClient}
     * @return {@link org.apache.cxf.jaxrs.client.WebClient}
     */
    public WebClient getRestWebClient() {
        if (restServiceClient != null) {
            return WebClient.fromClient(restServiceClient);
        }
        return null;
    }

    /**
     * Returns an arbitrary {@link org.apache.cxf.jaxrs.client.WebClient} for any {@link org.apache.cxf.jaxrs.client.Client}
     * @param client {@link org.apache.cxf.jaxrs.client.Client}
     * @return {@link org.apache.cxf.jaxrs.client.WebClient}
     */
    public WebClient getWebClientFromClient(Client client) {
        return WebClient.fromClient(client);
    }

    /**
     * Creates a new OpenSearch {@link org.apache.cxf.jaxrs.client.Client} based on a String URL
     * @param url
     * @return {@link org.apache.cxf.jaxrs.client.Client}
     */
    public Client newOpenSearchClient(String url) {
        OpenSearch proxy = JAXRSClientFactory.create(url, OpenSearch.class);
        Client tmp = WebClient.client(proxy);
        if (url.startsWith("https")) {
            setTLSOptions(tmp);
        }
        return tmp;
    }

    /**
     * Creates a new DDF REST {@link org.apache.cxf.jaxrs.client.Client} based on an OpenSearch
     * String URL.
     * @param url - OpenSearch URL
     * @param query - Query to be performed
     * @param metacardId - MetacardId to search for
     * @param retrieveResource - true if this is a resource request
     * @return {@link org.apache.cxf.jaxrs.client.Client}
     */
    public Client newRestClient(String url, Query query, String metacardId,
            boolean retrieveResource) {
        if (query != null) {
            url = createRestUrl(query, url, retrieveResource);
        } else {
            RestUrl restUrl = newRestUrl(url);

            if (restUrl != null) {
                if(StringUtils.isNotEmpty(metacardId)) {
                    restUrl.setId(metacardId);
                }
                restUrl.setRetrieveResource(retrieveResource);
                url = restUrl.buildUrl();
            }
        }
        Client tmp = null;
        if (url != null) {
            RESTService proxy = JAXRSClientFactory.create(url, RESTService.class);
            tmp = WebClient.client(proxy);
            if (url.startsWith("https")) {
                setTLSOptions(tmp);
            }
        }
        return tmp;
    }

    /**
     * Sets a subject cookie on a {@link org.apache.cxf.jaxrs.client.WebClient} and returns the
     * resulting client.
     * @param webClient - {@link org.apache.cxf.jaxrs.client.WebClient} to update
     * @param subject - {@link ddf.security.Subject} to inject
     * @return {@link org.apache.cxf.jaxrs.client.WebClient}
     */
    public WebClient setSubjectOnWebClient(WebClient webClient, Subject subject) {
        if(subject != null) {
            Cookie cookie = createSamlCookie(subject);
            return webClient.cookie(cookie);
        }
        return webClient;
    }

    /**
     * Creates a cookie to be returned to the browser if the token was successfully exchanged for
     * a SAML assertion.
     *
     * @param subject - {@link ddf.security.Subject} to create the cookie from
     */
    private Cookie createSamlCookie(Subject subject) {
        Cookie cookie = null;
        org.w3c.dom.Element samlToken = null;
        try {
            for (Object principal : subject.getPrincipals().asList()) {
                if (principal instanceof SecurityAssertion) {
                    samlToken = ((SecurityAssertion) principal).getSecurityToken()
                            .getToken();
                }
            }

            if (samlToken != null) {
                cookie = new Cookie(SAML_COOKIE_NAME, encodeSaml(samlToken));
            }
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to parse SAML assertion from subject.", e);
        }
        return cookie;
    }

    /**
     * Encodes the SAML assertion as a deflated Base64 String so that it can be used as a Cookie.
     *
     * @param token
     * @return String
     * @throws WSSecurityException
     */
    private String encodeSaml(org.w3c.dom.Element token) throws WSSecurityException {
        AssertionWrapper assertion = new AssertionWrapper(token);
        String samlStr = assertion.assertionToString();
        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(samlStr.getBytes());
        return Base64Utility.encode(deflatedToken);
    }

    /**
     * Add TLS and Basic Auth credentials to the underlying {@link org.apache.cxf.transport.http.HTTPConduit}
     * @param client
     */
    private void setTLSOptions(Client client) {
        ClientConfiguration clientConfiguration = WebClient.getConfig(client);

        HTTPConduit httpConduit = clientConfiguration.getHttpConduit();

        if(StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
            if(httpConduit.getAuthorization() != null) {
                httpConduit.getAuthorization().setUserName(username);
                httpConduit.getAuthorization().setPassword(password);
            }
        }

        try {
            TLSClientParameters tlsParams = new TLSClientParameters();
            tlsParams.setDisableCNCheck(true);

            // the default type is JKS
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

            // add the truststore if it exists
            File truststore = new File(trustStorePath);
            if (truststore.exists() && trustStorePassword != null) {
                FileInputStream fis = new FileInputStream(truststore);
                try {
                    LOGGER.debug("Loading trustStore");
                    trustStore.load(fis,
                            encryptionService.decryptValue(trustStorePassword).toCharArray());
                } catch (IOException e) {
                    LOGGER.error("Unable to load truststore. {}", truststore, e);
                } catch (CertificateException e) {
                    LOGGER.error("Unable to load certificates from keystore. {}", truststore, e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
                TrustManagerFactory trustFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory
                                .getDefaultAlgorithm());
                trustFactory.init(trustStore);
                LOGGER.debug("trust manager factory initialized");
                TrustManager[] tm = trustFactory.getTrustManagers();
                tlsParams.setTrustManagers(tm);
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            // add the keystore if it exists
            File keystore = new File(keyStorePath);
            if (keystore.exists() && keyStorePassword != null) {
                FileInputStream fis = new FileInputStream(keystore);
                try {
                    LOGGER.debug("Loading keyStore");
                    keyStore.load(fis,
                            encryptionService.decryptValue(keyStorePassword).toCharArray());
                } catch (IOException e) {
                    LOGGER.error("Unable to load keystore. {}", keystore, e);
                } catch (CertificateException e) {
                    LOGGER.error("Unable to load certificates from keystore. {}", keystore, e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
                KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                        .getDefaultAlgorithm());
                keyFactory.init(keyStore, keyStorePassword.toCharArray());
                LOGGER.debug("key manager factory initialized");
                KeyManager[] km = keyFactory.getKeyManagers();
                tlsParams.setKeyManagers(km);
            }

            // this sets the algorithms that we accept for SSL
            FiltersType filter = new FiltersType();
            filter.getInclude().addAll(Arrays.asList(SSL_ALLOWED_ALGORITHMS));
            filter.getExclude().addAll(Arrays.asList(SSL_DISALLOWED_ALGORITHMS));
            tlsParams.setCipherSuitesFilter(filter);

            httpConduit.setTlsClientParameters(tlsParams);
        } catch (KeyStoreException e) {
            LOGGER.error("Unable to read keystore: ", e);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Problems creating SSL socket. Usually this is "
                    + "referring to the certificate sent by the server not being trusted by the client.",
                    e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Unable to locate one of the SSL stores: {} | {}", trustStorePath,
                    keyStorePath, e);
        } catch (UnrecoverableKeyException e) {
            LOGGER.error("Unable to read keystore: ", e);
        }
    }

    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
