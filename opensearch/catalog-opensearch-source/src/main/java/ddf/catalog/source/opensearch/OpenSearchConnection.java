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
import ddf.security.settings.SecuritySettingsService;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.codice.ddf.endpoints.OpenSearch;
import org.codice.ddf.endpoints.rest.RESTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * This class wraps the CXF JAXRS code to make it easier to use and also easier to test. Most of
 * the CXF code uses static methods to construct the web clients, which is inherently difficult to
 * mock up when testing.
 */
public class OpenSearchConnection {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(OpenSearchConnection.class);

    protected OpenSearch openSearch;

    protected RESTService restService;

    protected Client openSearchClient;

    protected Client restServiceClient;

    private FilterAdapter filterAdapter;

    private String username;

    private String password;

    private SecuritySettingsService securitySettingsService;

    /**
     * Default Constructor
     * @param endpointUrl - OpenSearch URL to connect to
     * @param filterAdapter - adapter to translate between DDF REST and OpenSearch
     * @param securitySettings - Service used to obtain settings for secure communications.
     * @param username - Basic Auth user name
     * @param password - Basic Auth password
     */
    public OpenSearchConnection(String endpointUrl, FilterAdapter filterAdapter,
            SecuritySettingsService securitySettings, String username, String password) {
        this.filterAdapter = filterAdapter;
        this.username = username;
        this.password = password;
        this.securitySettingsService = securitySettings;
        openSearch = JAXRSClientFactory.create(endpointUrl, OpenSearch.class);
        openSearchClient = WebClient.client(openSearch);

        RestUrl restUrl = newRestUrl(endpointUrl);
        if (restUrl != null) {
            restService = JAXRSClientFactory.create(restUrl.buildUrl(), RESTService.class);
            restServiceClient = WebClient.client(restService);

            if (StringUtils.startsWithIgnoreCase(endpointUrl, "https")) {
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
        if (StringUtils.startsWithIgnoreCase(url, "https")) {
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
            if (StringUtils.startsWithIgnoreCase(url, "https")) {
                setTLSOptions(tmp);
            }
        }
        return tmp;
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

        TLSClientParameters tlsParams = securitySettingsService.getTLSParameters();
        tlsParams.setDisableCNCheck(true);
        httpConduit.setTlsClientParameters(tlsParams);

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
