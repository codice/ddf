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
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;

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
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class OpenSearchSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            "OpenSearchSourceConfigurationHandler";

    private static final String OPENSEARCH_SOURCE_DISPLAY_NAME = "OpenSearch Source";

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
                results.add(buildMessage(FAILURE, "No opensearch endpoint found."));
                return new ProbeReport(results);
            } else if(configuration.certError()) {
                results.add(buildMessage(WARNING, "The discovered URL has incorrectly configured SSL certificates and is likely insecure."));
                return new ProbeReport(results);
            }
            configuration.factoryPid(OPENSEARCH_FACTORY_PID);
            results.add(new ConfigurationMessage("Discovered opensearch endpoint.", SUCCESS));
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
                        "Specified URL has been verified as an opensearch endpoint."));
            } else {
                results.add(buildMessage(WARNING,
                        "Specified URL could not be verified as an opensearch endpoint."));
            }
            return new TestReport(results);
        }
        return new TestReport(buildMessage(FAILURE, "No such test."));
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        //TODO: add reflection methods to make configMap work
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case "create":
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create Open Search Source")) :
                    new TestReport(buildMessage(SUCCESS, "Open Search Source created"));
        case "delete":
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete Open Search Source")) :
                    new TestReport(buildMessage(SUCCESS, "Open Search Source deleted"));
        default:
            return new TestReport(buildMessage(FAILURE, "Uknown persist id: " + persistId));
        }
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        return configurator.getManagedServiceConfigs(OPENSEARCH_FACTORY_PID)
                .values()
                .stream()
                .map(serviceProps -> new OpenSearchSourceConfiguration(serviceProps))
                .collect(Collectors.toList());
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
    public Class getConfigClass() {
        return OpenSearchSourceConfiguration.class;
    }

    private String confirmOpenSearchEndpointUrl(OpenSearchSourceConfiguration configuration) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        configuration.sourceHostName(),
                        configuration.sourcePort()))
                .filter(url -> isAvailable(url, configuration) || configuration.certError())
                .findFirst()
                .orElse(NONE_FOUND);
    }

    private boolean isAvailable(String url, SourceConfiguration config) {
        int status;
        String contentType;
        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(PING_TIMEOUT)
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
        } catch(SSLPeerUnverifiedException e) {
            config.certError(true);
            return false;
        } catch (IOException e) {
            if(e instanceof SSLPeerUnverifiedException){
                config.certError(true);
                return false;
            }
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

    @Override
    public String getSourceDisplayName() {
        return OPENSEARCH_SOURCE_DISPLAY_NAME;
    }
}
