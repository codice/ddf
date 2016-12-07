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

package org.codice.ui.admin.sources.config.wfs;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.DISCOVER_SOURCES_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.MANUAL_URL_TEST_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.NONE_FOUND;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.OWS_NAMESPACE_CONTEXT;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

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
import org.w3c.dom.Document;

public class WfsSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String WFS_SOURCE_CONFIGURATION_HANDLER_ID =
            "WfsSourceConfigurationHandler";

    public static final String GET_CAPABILITIES_PARAMS = "?service=WFS&request=GetCapabilities";

    private static final List<String> URL_FORMATS = Arrays.asList("https://%s:%d/services/wfs",
            "https://%s:%d/wfs",
            "http://%s:%d/services/wfs",
            "http://%s:%d/wfs");

    private static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";

    private static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        WfsSourceConfiguration configuration = new WfsSourceConfiguration(baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            configuration.endpointUrl(confirmWfsEndpointUrl(configuration));
            if (configuration.endpointUrl()
                    .equals(NONE_FOUND)) {
                results.add(new ConfigurationMessage("No WFS endpoint found.", FAILURE));
                return new ProbeReport(results);
            }
            try {
                configuration = getPreferredConfig(configuration);
            } catch (WfsSourceCreationException e) {
                results.add(new ConfigurationMessage(
                        "Failed to create configuration from valid request to valid endpoint.",
                        FAILURE));
                return new ProbeReport(results);
            }
            results.add(new ConfigurationMessage("Discovered WFS endpoint.", SUCCESS));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(WFS_SOURCE_CONFIGURATION_HANDLER_ID));
        default:
            results.add(new ConfigurationMessage("No such probe.", FAILURE));
            return new ProbeReport(results);
        }
    }

    private String confirmWfsEndpointUrl(WfsSourceConfiguration configuration) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        configuration.sourceHostName(),
                        configuration.sourcePort()))
                .filter(url -> isAvailable(url, configuration))
                .findFirst()
                .orElse(NONE_FOUND);
    }

    @Override
    public TestReport test(String testId, SourceConfiguration configuration) {
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            try {
                URLConnection urlConnection = (new URL(
                        configuration.endpointUrl() + GET_CAPABILITIES_PARAMS)).openConnection();
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
                        "Specified URL has been verified as a WFS endpoint."));
            } else {
                results.add(buildMessage(WARNING,
                        "Specified URL could not be verified as a WFS endpoint."));
            }
            return new TestReport(results);
        default:
            results.add(buildMessage(FAILURE, "No such test."));
            return new TestReport(results);
        }
    }

    public TestReport persist(SourceConfiguration configuration) {
        Configurator configurator = new Configurator();
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        configurator.commit();
        return new TestReport(buildMessage(SUCCESS, "WFS Source Created"));
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        // TODO: tbatie - 11/22/16 - Return configurations based on back end
        throw new UnsupportedOperationException(
                "The wfs source getCOnfigurations is not implemented yet");
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(WfsSourceConfiguration.class.getSimpleName(), WfsSourceConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return WFS_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Map.Entry<String, Class> getSubtype() {
        return new Map.Entry<String, Class>() {
            @Override
            public String getKey() {
                return WFS_SOURCE_CONFIGURATION_HANDLER_ID;
            }

            @Override
            public Class getValue() {
                return WfsSourceConfiguration.class;
            }

            @Override
            public Class setValue(Class value) {
                return value;
            }
        };
    }

    private WfsSourceConfiguration getPreferredConfig(WfsSourceConfiguration configuration)
            throws WfsSourceCreationException {
        String wfsVersionExp = "//ows:ServiceIdentification//ows:ServiceTypeVersion/text()";
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpGet getCapabilitiesRequest = new HttpGet(
                configuration.endpointUrl() + GET_CAPABILITIES_PARAMS);
        XPath xpath = XPathFactory.newInstance()
                .newXPath();
        xpath.setNamespaceContext(OWS_NAMESPACE_CONTEXT);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document capabilitiesXml = builder.parse(client.execute(getCapabilitiesRequest)
                    .getEntity()
                    .getContent());
            String wfsVersion = xpath.compile(wfsVersionExp)
                    .evaluate(capabilitiesXml);
            //TODO: Add other properties required to make WFS work here
            if (wfsVersion.equals("2.0.0")) {
                return (WfsSourceConfiguration) configuration.factoryPid(WFS2_FACTORY_PID);
            }
            return (WfsSourceConfiguration) configuration.factoryPid(WFS1_FACTORY_PID);
        } catch (Exception e) {
            throw new WfsSourceCreationException();
        }
    }

    private boolean isAvailable(String url, SourceConfiguration config) {
        int status;
        long contentLength;
        String contentType;
        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(1000)
                        .build())
                .build();
        url += GET_CAPABILITIES_PARAMS;
        HttpGet request = new HttpGet(url);
        try {
            HttpResponse response = client.execute(request);
            status = response.getStatusLine()
                    .getStatusCode();
            contentType = ContentType.getOrDefault(response.getEntity())
                    .getMimeType();
            contentLength = response.getEntity()
                    .getContentLength();
            if (status == HTTP_OK && contentType.equals("text/xml") && contentLength > 0) {
                config.trustedCertAuthority(true);
                return true;
            }
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
                contentLength = response.getEntity()
                        .getContentLength();
                if (status == HTTP_OK && contentType.equals("text/xml") && contentLength > 0) {
                    config.trustedCertAuthority(false);
                    return true;
                }
            } catch (Exception e1) {
                return false;
            }
            return false;
        }
        return false;
    }
}
