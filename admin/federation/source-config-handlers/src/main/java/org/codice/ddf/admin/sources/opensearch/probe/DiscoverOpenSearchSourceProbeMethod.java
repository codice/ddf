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

import static org.codice.ddf.admin.api.config.services.OpensearchServiceProperties.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.PORT;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.SOURCE_HOSTNAME;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.BAD_CONFIG;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CERT_ERROR;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.ENDPOINT_DISCOVERED;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.NO_ENDPOINT;
import static org.codice.ddf.admin.sources.opensearch.OpenSearchSourceConfigurationHandler.OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.opensearch.OpenSearchSourceUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DiscoverOpenSearchSourceProbeMethod
        extends ProbeMethod<OpenSearchSourceConfiguration> {

    public static final String OPENSEARCH_DISCOVER_SOURCES_ID = DISCOVER_SOURCES_ID;
    public static final String DESCRIPTION = "Attempts to discover a OpenSearch endpoint based on a hostname and port using optional authentication information.";

    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(SOURCE_HOSTNAME, PORT);
    public static final List<String> OPTIONAL_FIELDS = ImmutableList.of(USERNAME, PASSWORD);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(ENDPOINT_DISCOVERED, "Discovered OpenSearch endpoint.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(
            CERT_ERROR, "The discovered source has incorrectly configured SSL certificates and is insecure.",
            NO_ENDPOINT, "No OpenSearch endpoint found.",
            BAD_CONFIG, "Endpoint discovered, but could not create valid configuration.");

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
                configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new ProbeReport(results);
        }
        Optional<String> url = OpenSearchSourceUtils.confirmEndpointUrl(configuration);
        if (url.isPresent()) {
            configuration.endpointUrl(url.get());
            configuration.factoryPid(OPENSEARCH_FACTORY_PID);
            results.add(buildMessage(SUCCESS, ENDPOINT_DISCOVERED, SUCCESS_TYPES.get(ENDPOINT_DISCOVERED)));
            return new ProbeReport(results).probeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID));

        } else if (configuration.certError()) {
            results.add(buildMessage(FAILURE, CERT_ERROR, FAILURE_TYPES.get(CERT_ERROR)));
            return new ProbeReport(results);
        } else {
            results.add(buildMessage(FAILURE, NO_ENDPOINT, FAILURE_TYPES.get(NO_ENDPOINT)));
            return new ProbeReport(results);
        }
    }

}
