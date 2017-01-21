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
package org.codice.ddf.admin.api.handler.commons;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.INVALID_FIELD;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.Optional;

import javax.xml.namespace.NamespaceContext;

import org.codice.ddf.admin.api.config.sources.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class SourceHandlerCommons {

    //Common probe, persist and test id's
    public static final String DISCOVER_SOURCES_ID = "discover-sources";
    public static final String CONFIG_FROM_URL_ID = "config-from-url";
    public static final String VALID_URL_TEST_ID = "valid-url";
    public static final String MANUAL_URL_TEST_ID = "manual-url";

    //Common success types
    public static final String CONFIG_CREATED = "CONFIG_CREATED";
    public static final String VERIFIED_URL = "VERIFIED_URL";
    public static final String ENDPOINT_DISCOVERED = "ENDPOINT_DISCOVERED";
    public static final String REACHED_URL = "REACHED_URL";

    //Common warning types
    public static final String CANNOT_VERIFY = "CANNOT_VERIFY";
    public static final String UNTRUSTED_CA = "UNTRUSTED_CA";

    //Common failed types
    public static final String CANNOT_CONNECT = "CANNOT_CONNECT";
    public static final String NO_ENDPOINT = "NO_ENDPOINT";
    public static final String CERT_ERROR = "CERT_ERROR";
    public static final String BAD_CONFIG = "BAD_CONFIG";


    public static final int PING_TIMEOUT = 500;

    public static Optional<ConfigurationMessage> endpointIsReachable(SourceConfiguration config) {
        try {
            URLConnection urlConnection = (new URL(config.endpointUrl())).openConnection();
            urlConnection.setConnectTimeout(PING_TIMEOUT);
            urlConnection.connect();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return Optional.of(buildMessage(FAILURE, INVALID_FIELD, "URL is improperly formatted."));
        } catch (Exception e) {
            Optional.of(buildMessage(FAILURE, CANNOT_CONNECT, "Cannot reach URL."));
        }
        return Optional.empty();
    }

    /*********************************************************
     * NamespaceContext for Xpath queries
     *********************************************************/
    public static final NamespaceContext OWS_NAMESPACE_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return prefix.equals("ows") ? "http://www.opengis.net/ows" : null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    };

}
