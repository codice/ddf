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
package org.codice.ddf.admin.sources.wfs.test;

import static org.codice.ddf.admin.api.commons.SourceUtils.MANUAL_URL_TEST_ID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.sources.wfs.WfsSourceUtils.isAvailable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.commons.SourceUtils;
import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class WfsUrlTestMethod extends TestMethod<WfsSourceConfiguration> {

    public static final String WFS_URL_TEST_ID = MANUAL_URL_TEST_ID;

    public static final String DESCRIPTION = "Attempts to verify a given URL is a WFS endpoint.";

    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(ENDPOINT_URL);

    private static final String VERIFIED_URL = "url-verified";

    private static final String CANNOT_CONNECT = "cannot-connect";

    private static final String CANNOT_VERIFY = "cannot-verify";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(VERIFIED_URL,
            "URL has been verified as a WFS endpoint.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CANNOT_CONNECT,
            "Could not reach specified URL.");

    public static final Map<String, String> WARNING_TYPES = ImmutableMap.of(CANNOT_VERIFY,
            "Reached URL, but could not verify as WFS endpoint.");

    public WfsUrlTestMethod() {
        super(WFS_URL_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public TestReport test(WfsSourceConfiguration configuration) {
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new TestReport(results);
        }
        Optional<ConfigurationMessage> message = SourceUtils.endpointIsReachable(configuration);
        if (message.isPresent()) {
            return new TestReport(message.get());
        }

        if (isAvailable(configuration.endpointUrl(), configuration)) {
            return new TestReport(buildMessage(SUCCESS, VERIFIED_URL, SUCCESS_TYPES.get(VERIFIED_URL)));
        } else {
            return new TestReport(buildMessage(FAILURE, CANNOT_VERIFY, FAILURE_TYPES.get(CANNOT_CONNECT)));
        }
    }
}
