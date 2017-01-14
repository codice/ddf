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
package org.codice.ddf.admin.sources.wfs;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ddf.admin.api.commons.SourceUtils.OWS_NAMESPACE_CONTEXT;
import static org.codice.ddf.admin.api.commons.SourceUtils.PING_TIMEOUT;
import static org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration.WFS1_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration.WFS2_FACTORY_PID;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
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
import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.w3c.dom.Document;

public class WfsSourceUtils {

    public static final String GET_CAPABILITIES_PARAMS = "?service=WFS&request=GetCapabilities";

    private static final List<String> URL_FORMATS = Arrays.asList("https://%s:%d/services/wfs",
            "https://%s:%d/wfs",
            "http://%s:%d/services/wfs",
            "http://%s:%d/wfs");

    public static Optional<String> confirmEndpointUrl(WfsSourceConfiguration config) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        config.sourceHostName(),
                        config.sourcePort()))
                .filter(url -> isAvailable(url, config) || config.certError())
                .map(Optional::of)
                .findFirst()
                .orElse(Optional.empty());
    }

    public static boolean isAvailable(String url, WfsSourceConfiguration config) {
        int status;
        long contentLength;
        String contentType;
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
        } catch (SSLPeerUnverifiedException e) {
            config.certError(true);
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

    public static WfsSourceConfiguration getPreferredConfig(WfsSourceConfiguration configuration)
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
            if (wfsVersion.equals("2.0.0")) {
                return (WfsSourceConfiguration) configuration.factoryPid(
                        WFS2_FACTORY_PID);
            }
            return (WfsSourceConfiguration) configuration.factoryPid(WFS1_FACTORY_PID);
        } catch (Exception e) {
            throw new WfsSourceCreationException();
        }
    }

}
