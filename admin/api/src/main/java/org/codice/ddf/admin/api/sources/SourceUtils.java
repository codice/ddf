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
package org.codice.ddf.admin.api.sources;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.REQUIRED_FIELDS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.namespace.NamespaceContext;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.report.TestReport;

public class SourceUtils {

    public static final int PING_TIMEOUT = 500;

    public static final String DISCOVER_SOURCES_ID = "discoverSources";

    public static final String VALID_URL_TEST_ID = "testValidUrl";

    public static final String MANUAL_URL_TEST_ID = "testManualUrl";

    public static Optional<ConfigurationMessage> endpointIsReachable(SourceConfiguration config) {
        try {
            URLConnection urlConnection = (new URL(config.endpointUrl())).openConnection();
            urlConnection.setConnectTimeout(PING_TIMEOUT);
            urlConnection.connect();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return Optional.of(buildMessage(FAILURE, "URL is improperly formatted."));
        } catch (Exception e) {
            Optional.of(buildMessage(FAILURE, "Unable to reach specified URL."));
        }
        return Optional.empty();
    }

    public static TestReport cannotBeNullFields(Map<String, Object> fieldsToCheck) {
        List<ConfigurationMessage> missingFields = new ArrayList<>();

        fieldsToCheck.entrySet()
                .stream()
                .filter(field -> field.getValue() == null && (field.getValue() instanceof String
                        && StringUtils.isEmpty((String) field.getValue())))
                .forEach(field -> missingFields.add(buildMessage(REQUIRED_FIELDS,
                        "Field cannot be empty").configId(field.getKey())));

        return new TestReport(missingFields);
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
