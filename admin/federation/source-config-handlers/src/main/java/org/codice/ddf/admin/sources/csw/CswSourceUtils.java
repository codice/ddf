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
package org.codice.ddf.admin.sources.csw;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ddf.admin.api.config.services.CswServiceProperties.CSW_GMD_FACTORY_PID;
import static org.codice.ddf.admin.api.config.services.CswServiceProperties.CSW_PROFILE_FACTORY_PID;
import static org.codice.ddf.admin.api.config.services.CswServiceProperties.CSW_SPEC_FACTORY_PID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.OWS_NAMESPACE_CONTEXT;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.PING_TIMEOUT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
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
import org.codice.ddf.admin.api.config.sources.CswSourceConfiguration;
import org.w3c.dom.Document;

public class CswSourceUtils {


    public static final String GET_CAPABILITIES_PARAMS = "?service=CSW&request=GetCapabilities";

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

    // Given a config with an endpoint URL, determines if that URL is a functional CSW endpoint.
    public static boolean isAvailable(String url, CswSourceConfiguration config) {
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
        } catch (SSLPeerUnverifiedException e) {
            config.certError(true);
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

    // Given a configuration, determines the preferred CSW source type and output schema and returns
    // a config with the appropriate factoryPid and Output Schema.
    public static Optional<CswSourceConfiguration> getPreferredConfig(CswSourceConfiguration config) {
        CswSourceConfiguration preferred = new CswSourceConfiguration(config);
        HttpClient client = HttpClientBuilder.create()
                .build();
        HttpGet getCapabilitiesRequest = new HttpGet(
                preferred.endpointUrl() + GET_CAPABILITIES_PARAMS);
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
                return Optional.of((CswSourceConfiguration) preferred.factoryPid(CSW_PROFILE_FACTORY_PID));
            } else if ((Boolean) xpath.compile(HAS_GMD_ISO_EXP)
                    .evaluate(capabilitiesXml, XPathConstants.BOOLEAN)) {
                return Optional.of(((CswSourceConfiguration) preferred.factoryPid(CSW_GMD_FACTORY_PID)).outputSchema(
                        GMD_OUTPUT_SCHEMA));
            } else {
                return Optional.of(((CswSourceConfiguration) (preferred.factoryPid(CSW_SPEC_FACTORY_PID))).outputSchema(
                        xpath.compile(GET_FIRST_OUTPUT_SCHEMA)
                                .evaluate(capabilitiesXml)));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // Determines the correct CSW endpoint URL format given a config with a Hostname and Port
    public static Optional<String> confirmEndpointUrl(CswSourceConfiguration config) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        config.sourceHostName(),
                        config.sourcePort()))
                .filter(url -> CswSourceUtils.isAvailable(url, config) || config.certError())
                .findFirst()
                .map(Optional::of)
                .orElse(Optional.empty());
    }

}
