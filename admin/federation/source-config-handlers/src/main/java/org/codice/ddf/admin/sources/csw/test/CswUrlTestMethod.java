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
package org.codice.ddf.admin.sources.csw.test;

import static org.codice.ddf.admin.api.config.services.CswServiceProperties.CSW_SPEC_FACTORY_PID;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.WARNING;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_CONNECT;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_VERIFY;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.MANUAL_URL_TEST_ID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.VERIFIED_URL;
import static org.codice.ddf.admin.sources.csw.CswSourceUtils.getPreferredConfig;
import static org.codice.ddf.admin.sources.csw.CswSourceUtils.isAvailable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CswUrlTestMethod extends TestMethod<CswSourceConfiguration> {

    public static final String CSW_URL_TEST_ID = MANUAL_URL_TEST_ID;
    public static final String DESCRIPTION = "Attempts to verify a given URL is a CSW endpoint.";
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(ENDPOINT_URL);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(VERIFIED_URL, "URL has been verified as a CSW endpoint.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CANNOT_CONNECT, "Could not reach specified URL.");
    public static final Map<String, String> WARNING_TYPES = ImmutableMap.of(CANNOT_VERIFY, "Reached URL, but could not verify as CSW endpoint.");

    public CswUrlTestMethod() {
        super(CSW_URL_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public Report test(CswSourceConfiguration configuration) {
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new Report(results);
        }
        Optional<ConfigurationMessage> message = SourceHandlerCommons.endpointIsReachable(configuration);
        if (message.isPresent()) {
            return new Report(message.get());
        }
        return discoverUrlCapabilities(configuration);
    }

    // Given a config with a endpoint URL, finds the most specific applicable CSW source type and
    //   sets the config's factoryPid and output schema appropriately, defaulting to generic
    //   specification with feedback if unable to determine CSW type
    public static Report discoverUrlCapabilities(CswSourceConfiguration config) {
        if (isAvailable(config.endpointUrl(), config)) {
            Optional<CswSourceConfiguration> preferred = getPreferredConfig(config);
            if (preferred.isPresent()) {
                config.factoryPid(preferred.get().factoryPid());
                config.outputSchema(preferred.get().outputSchema());
                return new Report(buildMessage(SUCCESS,
                        VERIFIED_URL,
                        SUCCESS_TYPES.get(VERIFIED_URL)));
            } else {
                config.factoryPid(CSW_SPEC_FACTORY_PID);
                return new Report(buildMessage(WARNING,
                        CANNOT_VERIFY,
                        FAILURE_TYPES.get(CANNOT_VERIFY)));

            }
        } else {
            config.factoryPid(CSW_SPEC_FACTORY_PID);
            return new Report(buildMessage(WARNING,
                    CANNOT_VERIFY,
                    FAILURE_TYPES.get(CANNOT_VERIFY)));
        }
    }

}
