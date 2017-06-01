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
package org.codice.ddf.admin.sources.opensearch;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CERT_ERROR;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.PING_TIMEOUT;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
import org.codice.ddf.admin.api.config.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.handler.commons.UrlAvailability;

public class OpenSearchSourceUtils {
    private static final List<String> URL_FORMATS = Arrays.asList(
            "https://%s:%d/services/catalog/query",
            "https://%s:%d/catalog/query",
            "http://%s:%d/services/catalog/query",
            "http://%s:%d/catalog/query");

    //Given a config, returns the correct URL format for the endpoint if one exists
    public static Optional<String> confirmEndpointUrl(OpenSearchSourceConfiguration config) {
        return URL_FORMATS.stream()
                .map(formatUrl -> String.format(formatUrl,
                        config.sourceHostName(),
                        config.sourcePort()))
                .map(url -> {
                    UrlAvailability avail = OpenSearchSourceUtils.getUrlAvailability(url);
                    if (avail.isAvailable()) {
                        return url;
                    }
                    if (avail.isCertError()) {
                        return CERT_ERROR;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .map(Optional::of)
                .orElse(Optional.empty());
    }

    // Given a configuration with and endpointUrl, determines if that URL is available as an OS source
    public static UrlAvailability getUrlAvailability(String url) {
        UrlAvailability result = new UrlAvailability();
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
                return result.trustedCertAuthority(true).certError(false).available(true);
            } else {
                return result.trustedCertAuthority(true).certError(false).available(false);
            }
        } catch (SSLPeerUnverifiedException e) {
            // This is the hostname != cert name case - if this occurs, the URL's SSL cert configuration
            // is incorrect, or a serious network security issue has occurred.
            return result.trustedCertAuthority(false).certError(true).available(false);
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
                    return result.trustedCertAuthority(false).certError(false).available(true);
                }
            } catch (Exception e1) {
                return result.trustedCertAuthority(false).certError(false).available(false);
            }
        }
        return result;
    }
}
