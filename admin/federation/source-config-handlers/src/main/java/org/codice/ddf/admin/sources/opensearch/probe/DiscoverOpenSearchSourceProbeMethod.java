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
package org.codice.ddf.admin.sources.opensearch.probe;

import static org.codice.ddf.admin.api.commons.SourceUtils.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.HOSTNAME;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PORT;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.sources.opensearch.OpenSearchSourceConfigurationHandler.OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.opensearch.OpenSearchSourceUtils;

import com.google.common.collect.ImmutableMap;

public class DiscoverOpenSearchSourceProbeMethod
        extends ProbeMethod<OpenSearchSourceConfiguration> {
    public static final String OPENSEARCH_DISCOVER_SOURCES_ID = DISCOVER_SOURCES_ID;

    public static final String DESCRIPTION =
            "Attempts to discover a OpenSearch endpoint based on a hostname and port using optional authentication information.";

    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(HOSTNAME,
            "The hostname to query for OpenSearch capabilites.",
            PORT,
            "The port to connect over when searching for OpenSearch capabilities.");

    public static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A username to use for basic auth connections when searching for OpenSearch capabilities.",
            PASSWORD,
            "A password to use for basic auth connections when searching for OpenSearch capabilities.");

    private static final String ENDPOINT_DISCOVERED = "endpointDiscovered";

    private static final String CERT_ERROR = "certError";

    private static final String NO_ENDPOINT = "noEndpoint";

    private static final String BAD_CONFIG = "badConfig";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(ENDPOINT_DISCOVERED,
            "Discovered OpenSearch endpoint.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CERT_ERROR,
            "The discovered source has incorrectly configured SSL certificates and is insecure.",
            NO_ENDPOINT,
            "No OpenSearch endpoint found.",
            BAD_CONFIG,
            "Endpoint discovered, but could not create valid configuration.");

    public DiscoverOpenSearchSourceProbeMethod() {
        super(OPENSEARCH_DISCOVER_SOURCES_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public ProbeReport probe(OpenSearchSourceConfiguration configuration) {
        List<ConfigurationMessage> results =
                configuration.validate(new ArrayList(REQUIRED_FIELDS.keySet()));
        if (!results.isEmpty()) {
            return new ProbeReport(results);
        }
        Optional<String> url = OpenSearchSourceUtils.confirmEndpointUrl(configuration);
        if (url.isPresent()) {
            configuration.endpointUrl(url.get());
            configuration.factoryPid(OPENSEARCH_FACTORY_PID);
            results.add(new ConfigurationMessage("Discovered OpenSearch endpoint.", SUCCESS));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID));

        } else if (configuration.certError()) {
            results.add(buildMessage(FAILURE,
                    "The discovered URL has incorrectly configured SSL certificates and is insecure."));
            return new ProbeReport(results);
        } else {
            results.add(buildMessage(FAILURE, "No OpenSearch endpoint found."));
            return new ProbeReport(results);
        }
    }

}
