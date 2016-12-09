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

package org.codice.ui.admin.sources.config.opensearch;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.DISCOVER_SOURCES_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.MANUAL_URL_TEST_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.NONE_FOUND;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.PING_TIMEOUT;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.WARNING;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.sources.config.SourceConfigurationHandler;
import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class OpenSearchSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            "OpenSearchSourceConfigurationHandler";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    private static final List<String> URL_FORMATS = Arrays.asList(
            "https://%s:%d/services/catalog/query",
            "https://%s:%d/catalog/query",
            "http://%s:%d/services/catalog/query",
            "http://%s:%d/catalog/query");

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        OpenSearchSourceConfiguration configuration = new OpenSearchSourceConfiguration(
                baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            configuration.endpointUrl(confirmOpenSearchEndpointUrl(configuration));
            if (configuration.endpointUrl()
                    .equals(NONE_FOUND)) {
                results.add(new ConfigurationMessage("No OpenSearch endpoint found.", FAILURE));
                return new ProbeReport(results);
            }
            configuration.factoryPid(OPENSEARCH_FACTORY_PID);
            results.add(new ConfigurationMessage("Discovered OpenSearch endpoint.", SUCCESS));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID));
        }
        results.add(new ConfigurationMessage("No such probe.", FAILURE));
        return new ProbeReport(results);
    }

    @Override
    public TestReport test(String testId, SourceConfiguration configuration) {
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            try {
                URLConnection urlConnection =
                        (new URL(configuration.endpointUrl())).openConnection();
                urlConnection.setConnectTimeout(PING_TIMEOUT);
                urlConnection.connect();
            } catch (MalformedURLException e) {
                results.add(buildMessage(FAILURE, "URL is improperly formatted."));
                return new TestReport(results);
            } catch (Exception e) {
                results.add(buildMessage(FAILURE, "Unable to reach specified URL."));
                return new TestReport(results);
            }
            if (isAvailable(configuration.endpointUrl(), configuration)) {
                results.add(buildMessage(SUCCESS,
                        "Specified URL has been verified as an OpenSearch endpoint."));
            } else {
                results.add(buildMessage(WARNING,
                        "Specified URL could not be verified as an OpenSearch endpoint."));
            }
            return new TestReport(results);
        }
        return new TestReport(buildMessage(FAILURE, "No such test."));
    }

    @Override
    public TestReport persist(SourceConfiguration configuration) {
        //TODO: add reflection methods to make configMap work
        Configurator configurator = new Configurator();
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        configurator.commit();
        return new TestReport(buildMessage(SUCCESS, "OpenSearch source created"));
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        // TODO: tbatie - 11/22/16 - Return configurations based on back end
        throw new UnsupportedOperationException(
                "The open search getConfigurations is not implemented yet");
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(OpenSearchSourceConfiguration.class.getSimpleName(), OpenSearchSourceConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Map.Entry<String, Class> getSubtype() {
        return new Map.Entry<String, Class>() {
            @Override
            public String getKey() {
                return OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;
            }

            @Override
            public Class getValue() {
                return OpenSearchSourceConfiguration.class;
            }

            @Override
            public Class setValue(Class value) {
                return null;
            }
        };
    }

    private String confirmOpenSearchEndpointUrl(OpenSearchSourceConfiguration configuration) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        configuration.sourceHostName(),
                        configuration.sourcePort()))
                .filter(url -> isAvailable(url, configuration))
                .findFirst()
                .orElse(NONE_FOUND);
    }

    private boolean isAvailable(String url, SourceConfiguration config) {
        int status;
        String contentType;
        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(1000)
                        .build())
                .build();
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            status = response.getStatusLine()
                    .getStatusCode();
            contentType = ContentType.getOrDefault(response.getEntity())
                    .getMimeType();
            if (status == HTTP_OK && contentType.equals("application/atom+xml")) {
                config.trustedCertAuthority(true);
                return true;
            }
            return false;
        } catch (IOException e) {
            try {
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext);
                client = HttpClientBuilder.create()
                        .setDefaultRequestConfig(RequestConfig.custom()
                                .setConnectTimeout(PING_TIMEOUT)
                                .build())
                        .setSSLSocketFactory(sf)
                        .build();
                HttpResponse response = client.execute(request);
                status = response.getStatusLine()
                        .getStatusCode();
                contentType = ContentType.getOrDefault(response.getEntity())
                        .getMimeType();
                if (status == HTTP_OK && contentType.equals("application/atom+xml")) {
                    config.trustedCertAuthority(false);
                    return true;
                }
                return false;
            } catch (Exception e1) {
                return false;
            }
        }

    }
}
