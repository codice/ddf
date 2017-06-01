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
package org.codice.ddf.admin.sources.opensearch.persist;

import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.SOURCE_NAME;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.CREATE;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.report.Report.createReport;
import static org.codice.ddf.admin.api.services.OpensearchServiceProperties.openSearchConfigToServiceProps;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CreateOpenSearchSourcePersistMethod
        extends PersistMethod<OpenSearchSourceConfiguration> {
    public static final String CREATE_OPENSEARCH_SOURCE_ID = CREATE;

    public static final String DESCRIPTION = "Attempts to create and persist a OpenSearch source given a configuration.";
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(SOURCE_NAME, ENDPOINT_URL);
    private static final List<String> OPTIONAL_FIELDS = ImmutableList.of(USERNAME, PASSWORD);
    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "OpenSearch Source successfully created.");
    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Failed to create OpenSearch Source.");

    public CreateOpenSearchSourcePersistMethod() {
        super(CREATE_OPENSEARCH_SOURCE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(OpenSearchSourceConfiguration configuration) {
        Report validationResults = new Report(configuration.validate(REQUIRED_FIELDS));
        if (validationResults.containsFailureMessages()) {
            return validationResults;
        }

        Configurator configurator = new Configurator();
        configurator.createManagedService(configuration.factoryPid(), openSearchConfigToServiceProps(configuration));
        OperationReport report = configurator.commit();
        return createReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                report.containsFailedResults() ? ConfigurationMessage.FAILED_PERSIST : SUCCESSFUL_PERSIST);
    }

}
