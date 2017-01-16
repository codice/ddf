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

import static org.codice.ddf.admin.api.commons.SourceUtils.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.HOSTNAME;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PORT;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.INTERNAL_ERROR;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.sources.csw.CswSourceConfigurationHandler.CSW_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.csw.CswSourceCreationException;
import org.codice.ddf.admin.sources.csw.CswSourceUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DiscoverCswSourceProbeMethod extends ProbeMethod<CswSourceConfiguration> {

    public static final String CSW_DISCOVER_SOURCES_ID = DISCOVER_SOURCES_ID;
    public static final String DESCRIPTION =
            "Attempts to discover a CSW endpoint based on a hostname and port using optional authentication information.";

    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(HOSTNAME, PORT);

    public static final List<String> OPTIONAL_FIELDS = ImmutableList.of(USERNAME, PASSWORD);

    private static final String ENDPOINT_DISCOVERED = "endpoint-discovered";
    private static final String CERT_ERROR = "cert-error";
    private static final String NO_ENDPOINT = "no-endpoint";
    private static final String BAD_CONFIG = "bad-config";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(ENDPOINT_DISCOVERED,
            "Discovered CSW endpoint.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CERT_ERROR,
            "The discovered source has incorrectly configured SSL certificates and is insecure.",
            NO_ENDPOINT,
            "No CSW endpoint found.",
            BAD_CONFIG,
            "Endpoint discovered, but could not create valid configuration.",
            INTERNAL_ERROR, "Failed to create configuration from valid request to valid endpoint.");

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
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);
        if(!results.isEmpty()) {
            return new ProbeReport(results);
        }
        Optional<String> url = CswSourceUtils.confirmEndpointUrl(configuration);
        if (url.isPresent()) {
            configuration.endpointUrl(url.get());
        } else if (configuration.certError()) {
            results.add(new ConfigurationMessage(FAILURE, CERT_ERROR, FAILURE_TYPES.get(CERT_ERROR)));
            return new ProbeReport(results);
        } else {
            results.add(new ConfigurationMessage(FAILURE, NO_ENDPOINT, FAILURE_TYPES.get(NO_ENDPOINT)));
            return new ProbeReport(results);
        }
        try {
            configuration = CswSourceUtils.getPreferredConfig(configuration);
        } catch (CswSourceCreationException e) {
            results.add(new ConfigurationMessage(FAILURE, INTERNAL_ERROR, FAILURE_TYPES.get(INTERNAL_ERROR), e));
            return new ProbeReport(results);
        }
        results.add(new ConfigurationMessage(SUCCESS, ENDPOINT_DISCOVERED, SUCCESS_TYPES.get(ENDPOINT_DISCOVERED)));
        return new ProbeReport(results).probeResult(DISCOVER_SOURCES_ID,
                configuration.configurationHandlerId(CSW_SOURCE_CONFIGURATION_HANDLER_ID));
    }

}
