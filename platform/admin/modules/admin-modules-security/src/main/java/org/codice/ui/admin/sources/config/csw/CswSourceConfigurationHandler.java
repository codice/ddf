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

package org.codice.ui.admin.sources.config.csw;

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

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
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
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.Configurator;
import org.w3c.dom.Document;

public class CswSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String CSW_SOURCE_CONFIGURATION_HANDLER_ID =
            "CswSourceConfigurationHandler";

    public static final String GET_CAPABILITIES_PARAMS = "?service=CSW&request=GetCapabilities";

    public static final String CSW_PROFILE_FACTORY_PID = "Csw_Federation_Profile_Source";

    public static final String CSW_GMD_FACTORY_PID = "Gmd_Csw_Federated_Source";

    public static final String CSW_SPEC_FACTORY_PID = "Csw_Federated_Source";

    private static final List<String> URL_FORMATS = Arrays.asList("https://%s:%d/services/csw",
            "https://%s:%d/csw",
            "http://%s:%d/services/csw",
            "http://%s:%d/csw");

    private static final String GMD_OUTPUT_SCHEMA = "http://www.isotc211.org/2005/gmd";

    private static final String HAS_CATALOG_METACARD_EXP =
            "//ows:OperationsMetadata//ows:Operation[@name='GetRecords']/ows:Parameter[@name='OutputSchema' or @name='outputSchema']/ows:Value/text()='urn:catalog:metacard'";

    private static final String HAS_GMD_ISO_EXP =
            "//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='OutputSchema' or @name='outputSchema']/ows:Value/text()='http://www.isotc211.org/2005/gmd'";

    private static final String GET_FIRST_OUTPUT_SCHEMA =
            "//ows:OperationsMetadata/ows:Operation[@name='GetRecords']/ows:Parameter[@name='OutputSchema' or @name='outputSchema']/ows:Value[1]/text()";

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            configuration.endpointUrl(confirmCswEndpointUrl(configuration));
            if (configuration.endpointUrl()
                    .equals(NONE_FOUND)) {
                results.add(new ConfigurationMessage("No CSW endpoint found.", FAILURE));
                return new ProbeReport(results);
            }
            try {
                configuration = getPreferredConfig(configuration);
            } catch (CswSourceCreationException e) {
                results.add(new ConfigurationMessage(
                        "Failed to create configuration from valid request to valid endpoint.",
                        FAILURE));
                return new ProbeReport(results);
            }
            results.add(new ConfigurationMessage("Discovered CSW endpoint.", SUCCESS));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(CSW_SOURCE_CONFIGURATION_HANDLER_ID));
        }
        results.add(new ConfigurationMessage("No such probe.", FAILURE));
        return new ProbeReport(results);
    }

    @Override
    public TestReport test(String testId, SourceConfiguration baseConfiguration) {
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
            List<ConfigurationMessage> results = new ArrayList<>();
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
                try {
                    getPreferredConfig(configuration);
                    results.add(buildMessage(SUCCESS,
                            "Specified URL has been verified as a CSW endpoint."));
                } catch (CswSourceCreationException e) {
                    results.add(buildMessage(WARNING,
                            "Cannot discover CSW profile, defaulting to Specification."));
                    configuration.factoryPid(CSW_SPEC_FACTORY_PID);
                }
            } else {
                results.add(buildMessage(WARNING,
                        "Specified URL could not be verified as a CSW endpoint, configuration will default to Specification."));
                configuration.factoryPid(CSW_SPEC_FACTORY_PID);
            }
            return new TestReport(results);
        }
        return new TestReport(buildMessage(FAILURE, "No such test."));
    }

    @Override
    public TestReport persist(SourceConfiguration configuration) {
        Configurator configurator = new Configurator();
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        configurator.commit();
        return new TestReport(buildMessage(SUCCESS, "CSW Source Created"));
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        // TODO: tbatie - 11/22/16 - Return configurations based on back end
        throw new UnsupportedOperationException("The csw getConfigurations is not implemented yet");
    }

    @Override
    public String getConfigurationHandlerId() {
        return CSW_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    private String confirmCswEndpointUrl(CswSourceConfiguration configuration) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        configuration.sourceHostName(),
                        configuration.sourcePort()))
                .filter(url -> isAvailable(url, configuration))
                .findFirst()
                .orElse(NONE_FOUND);
    }

    private boolean isAvailable(String url, SourceConfiguration config) {
        String contentType;
        int status;
        long contentLength;
        HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(PING_TIMEOUT)
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
            return false;
        } catch (IOException e) {
            try {
                // We want to trust any root CA, but maintain all other standard SSL checks
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
        }
        return false;
    }

    private CswSourceConfiguration getPreferredConfig(CswSourceConfiguration configToUpdate)
            throws CswSourceCreationException {
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpGet getCapabilitiesRequest = new HttpGet(
                configToUpdate.endpointUrl() + GET_CAPABILITIES_PARAMS);
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
            if ((Boolean) xpath.compile(HAS_CATALOG_METACARD_EXP)
                    .evaluate(capabilitiesXml, XPathConstants.BOOLEAN)) {
                return (CswSourceConfiguration) configToUpdate.factoryPid(CSW_PROFILE_FACTORY_PID);
            } else if ((Boolean) xpath.compile(HAS_GMD_ISO_EXP)
                    .evaluate(capabilitiesXml, XPathConstants.BOOLEAN)) {
                return ((CswSourceConfiguration) configToUpdate.factoryPid(CSW_GMD_FACTORY_PID)).outputSchema(
                        GMD_OUTPUT_SCHEMA);
            } else {
                return ((CswSourceConfiguration) (configToUpdate.factoryPid(CSW_SPEC_FACTORY_PID))).outputSchema(
                        xpath.compile(GET_FIRST_OUTPUT_SCHEMA)
                                .evaluate(capabilitiesXml));
            }
        } catch (Exception e) {
            throw new CswSourceCreationException();
        }
    }

}
