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
package org.codice.ddf.admin.sources.csw.probe;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.sources.SourceConfigurationHandlerImpl.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.sources.csw.CswSourceConfigurationHandler.CSW_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.csw.CswSourceCreationException;
import org.codice.ddf.admin.sources.csw.CswSourceUtils;

import com.google.common.collect.ImmutableMap;

public class DiscoverCswSourceProbeMethod extends ProbeMethod<CswSourceConfiguration> {

    public static final String CSW_DISCOVER_SOURCES_ID = DISCOVER_SOURCES_ID;

    public static final String DESCRIPTION =
            "Attempts to discover a CSW endpoint based on a hostname and port using optional authentication information.";

    public static final String HOSTNAME = "hostname";

    public static final String PORT = "port";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(HOSTNAME,
            "The hostname to query for CSW capabilities.",
            PORT,
            "The port to connect over when searching for CSW capabilities.");

    public static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A username to use for basic auth connections when searching for CSW capabilities.",
            PASSWORD,
            "A password to use for basic auth connections when searching for CSW capabilities.");

    private static final String ENDPOINT_DISCOVERED = "endpointDiscovered";

    private static final String CERT_ERROR = "certError";

    private static final String NO_ENDPOINT = "noEndpoint";

    private static final String BAD_CONFIG = "badConfig";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(ENDPOINT_DISCOVERED,
            "Discovered CSW endpoint.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CERT_ERROR,
            "The discovered source has incorrectly configured SSL certificates and is insecure.",
            NO_ENDPOINT,
            "No CSW endpoint found.",
            BAD_CONFIG,
            "Endpoint discovered, but could not create valid configuration.");

    public DiscoverCswSourceProbeMethod() {
        super(CSW_DISCOVER_SOURCES_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public ProbeReport probe(CswSourceConfiguration configuration) {
        List<ConfigurationMessage> results = new ArrayList();
        Optional<String> url = CswSourceUtils.confirmEndpointUrl(configuration);
        if (url.isPresent()) {
            configuration.endpointUrl(url.get());
        } else if (configuration.certError()) {
            results.add(buildMessage(FAILURE,
                    "The discovered URL has incorrectly configured SSL certificates and is insecure."));
            return new ProbeReport(results);
        } else {
            results.add(buildMessage(FAILURE, "No CSW endpoint found."));
            return new ProbeReport(results);
        }
        try {
            configuration = CswSourceUtils.getPreferredConfig(configuration);
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
}
