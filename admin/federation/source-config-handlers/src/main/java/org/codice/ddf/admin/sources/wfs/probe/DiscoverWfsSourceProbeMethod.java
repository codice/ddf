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
package org.codice.ddf.admin.sources.wfs.probe;

import static org.codice.ddf.admin.api.commons.SourceUtils.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.HOSTNAME;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PORT;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.sources.wfs.WfsSourceConfigurationHandler.WFS_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.wfs.WfsSourceCreationException;
import org.codice.ddf.admin.sources.wfs.WfsSourceUtils;

import com.google.common.collect.ImmutableMap;

public class DiscoverWfsSourceProbeMethod extends ProbeMethod<WfsSourceConfiguration> {
    public static final String WFS_DISCOVER_SOURCES_ID = DISCOVER_SOURCES_ID;

    public static final String DESCRIPTION =
            "Attempts to discover a Wfs endpoint based on a hostname and port using optional authentication information.";

    private static final String ENDPOINT_DISCOVERED = "endpointDiscovered";
    private static final String CERT_ERROR = "certError";
    private static final String NO_ENDPOINT = "noEndpoint";
    private static final String BAD_CONFIG = "badConfig";

    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(HOSTNAME,
            "The hostname to query for Wfs capabilites.",
            PORT,
            "The port to connect over when searching for Wfs capabilities.");

    public static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A username to use for basic auth connections when searching for Wfs capabilities. If password is provided, username must be as well.",
            PASSWORD,
            "A password to use for basic auth connections when searching for Wfs capabilities. If username is provided, password must be as well.");

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(ENDPOINT_DISCOVERED,
            "Discovered Wfs endpoint.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CERT_ERROR,
            "The discovered source has incorrectly configured SSL certificates and is insecure.",
            NO_ENDPOINT,
            "No Wfs endpoint found.",
            // TODO: tbatie - 1/13/17 - We should be able to handle this
            BAD_CONFIG,
            "Endpoint discovered, but could not create valid configuration.");

    public DiscoverWfsSourceProbeMethod() {
        super(WFS_DISCOVER_SOURCES_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public ProbeReport probe(WfsSourceConfiguration configuration) {
        // TODO: tbatie - 1/13/17 - this is really messy
        List<ConfigurationMessage> results =
                configuration.validate(new ArrayList(REQUIRED_FIELDS.keySet()));
        if (!results.isEmpty()) {
            return new ProbeReport(results);
        }
        Optional<String> url = WfsSourceUtils.confirmEndpointUrl(configuration);
        if (url.isPresent()) {
            configuration.endpointUrl(url.get());
        } else {
            results.add(buildMessage(FAILURE, NO_ENDPOINT, FAILURE_TYPES.get(NO_ENDPOINT)));
            return new ProbeReport(results);
        }

        try {
            configuration = WfsSourceUtils.getPreferredConfig(configuration);
        } catch (WfsSourceCreationException e) {
            results.add(buildMessage(FAILURE, BAD_CONFIG, FAILURE_TYPES.get(BAD_CONFIG)));
            return new ProbeReport(results);
        }
        results.add(buildMessage(SUCCESS, ENDPOINT_DISCOVERED, SUCCESS_TYPES.get(ENDPOINT_DISCOVERED)));
        return new ProbeReport(results).probeResult(DISCOVER_SOURCES_ID,
                configuration.configurationHandlerId(WFS_SOURCE_CONFIGURATION_HANDLER_ID));
    }

}

