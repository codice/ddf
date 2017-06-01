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

import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.BAD_CONFIG;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_CONNECT;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_VERIFY;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CERT_ERROR;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CONFIG_CREATED;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CONFIG_FROM_URL_ID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.UNTRUSTED_CA;
import static org.codice.ddf.admin.sources.csw.CswSourceConfigurationHandler.CSW_SOURCE_CONFIGURATION_HANDLER_ID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.commons.UrlAvailability;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.sources.csw.CswSourceUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CswConfigFromUrlProbeMethod extends ProbeMethod<CswSourceConfiguration>{

    public static final String CSW_CONFIG_FROM_URL_ID = CONFIG_FROM_URL_ID;
    public static final String DESCRIPTION = "Attempts to create a CSW configuration from a verified CSW URL.";
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(ENDPOINT_URL);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(CONFIG_CREATED, "Created CSW configuration from provided URL.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(
            BAD_CONFIG, "Failed to create a config from provided and verified URL.",
            CANNOT_CONNECT, "The URL provided could not be reached.",
            CERT_ERROR, "The URL provided has improperly configured SSL Certificates and is insecure.");
    public static final Map<String, String> WARNING_TYPES = ImmutableMap.of(
            CANNOT_VERIFY, "CSW capabilities could not be discovered, this may not be a CSW endpoint, defaulting to Specification configuration.",
            UNTRUSTED_CA, "The URL's SSL certificate has been signed by an untrusted certificate authority, and is likely insecure.");

    public CswConfigFromUrlProbeMethod() {
        super(CSW_CONFIG_FROM_URL_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public ProbeReport probe(CswSourceConfiguration configuration) {
        List<ConfigurationMessage> validationErrors = configuration.validate(REQUIRED_FIELDS);
        if (!validationErrors.isEmpty()) {
            return new ProbeReport(validationErrors);
        }
        UrlAvailability status = CswSourceUtils.getUrlAvailability(configuration.endpointUrl());
        String result;
        Map<String, Object> probeResult = new HashMap<>();
        if (status.isAvailable()) {
            Optional<CswSourceConfiguration> config = CswSourceUtils.getPreferredConfig(configuration);
            if (config.isPresent()) {
                result = status.isTrustedCertAuthority() ? CONFIG_CREATED : UNTRUSTED_CA;
                probeResult.put(CSW_CONFIG_FROM_URL_ID, config.get().configurationHandlerId(CSW_SOURCE_CONFIGURATION_HANDLER_ID));
            } else {
                result = BAD_CONFIG;
            }
        } else {
            result = status.isCertError() ? CERT_ERROR : CANNOT_CONNECT;
        }
        return ProbeReport.createProbeReport(SUCCESS_TYPES, FAILURE_TYPES, WARNING_TYPES, result).probeResults(probeResult);
    }
}
