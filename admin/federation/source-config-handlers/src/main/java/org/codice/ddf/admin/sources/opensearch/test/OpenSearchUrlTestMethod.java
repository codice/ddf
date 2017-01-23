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
package org.codice.ddf.admin.sources.opensearch.test;

import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_CONNECT;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.CANNOT_VERIFY;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.MANUAL_URL_TEST_ID;
import static org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons.VERIFIED_URL;
import static org.codice.ddf.admin.sources.opensearch.OpenSearchSourceUtils.isAvailable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.commons.SourceHandlerCommons;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class OpenSearchUrlTestMethod extends TestMethod<OpenSearchSourceConfiguration> {

    public static final String OPENSEARCH_URL_TEST_ID = MANUAL_URL_TEST_ID;
    public static final String DESCRIPTION = "Attempts to verify a given URL is an OpenSearch endpoint.";
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(ENDPOINT_URL);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(VERIFIED_URL, "URL has been verified as an OpenSearch endpoint.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CANNOT_CONNECT, "Specified URL could not be verified as an OpenSearch endpoint.");

    // TODO: tbatie - 1/13/17 - This isn't actually being returned anywhere and should be
    public static final Map<String, String> WARNING_TYPES = ImmutableMap.of(CANNOT_VERIFY, "Reached URL, but could not verify as an OpenSearch endpoint.");

    public OpenSearchUrlTestMethod() {
        super(OPENSEARCH_URL_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public Report test(OpenSearchSourceConfiguration configuration) {
        Report testReport = new Report();
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);

        if (!results.isEmpty()) {
            return testReport.messages(results);
        }

        Optional<ConfigurationMessage> message = SourceHandlerCommons.endpointIsReachable(configuration);
        if (message.isPresent()) {
            return testReport.messages(message.get());
        }

        return Report.createReport(SUCCESS_TYPES, FAILURE_TYPES, WARNING_TYPES, isAvailable(configuration.endpointUrl(), configuration) ? VERIFIED_URL : CANNOT_CONNECT);
    }
}
