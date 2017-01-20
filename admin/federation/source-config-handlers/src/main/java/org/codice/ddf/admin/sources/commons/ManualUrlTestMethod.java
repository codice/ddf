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
package org.codice.ddf.admin.sources.commons;

import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_CONNECT;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.MANUAL_URL_TEST_ID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.REACHED_URL;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.sources.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ManualUrlTestMethod extends TestMethod<SourceConfiguration> {

    public static final String DESCRIPTION = "Attempts to reach a specified URL.";
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(ENDPOINT_URL);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(REACHED_URL, "Successfully connected to URL.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CANNOT_CONNECT, "Could not reach specified URL.");

    public ManualUrlTestMethod() {
        super(MANUAL_URL_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report test(SourceConfiguration configuration) {
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new Report(results);
        }
        Optional<ConfigurationMessage> message = SourceHandlerCommons.endpointIsReachable(configuration);
        return message.isPresent() ? new Report(message.get()) :
                new Report(buildMessage(SUCCESS, REACHED_URL, SUCCESS_TYPES.get(REACHED_URL)));
    }
}
