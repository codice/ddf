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

import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.SERVICE_PID;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.SourceConfigurationHandler.DELETE;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DeleteOpenSearchSourcePersistMethod
        extends PersistMethod<OpenSearchSourceConfiguration> {
    public static final String DELETE_OPENSEARCH_SOURCE_ID = DELETE;

    public static final String DESCRIPTION =
            "Attempts to delete an OpenSearch source with the given configuration.";

    private static final List<String> REQUIRED_FIELDS = ImmutableList.of(SERVICE_PID);

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST,
            "The CSW Source was successfully deleted.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST,
            "Failed to delete CSW source.");

    public DeleteOpenSearchSourcePersistMethod() {
        super(DELETE_OPENSEARCH_SOURCE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(OpenSearchSourceConfiguration configuration) {
        List<ConfigurationMessage> results =
                configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new Report(results);
        }
        Configurator configurator = new Configurator();
        ConfigReport report;
        // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
        configurator.deleteManagedService(configuration.servicePid());
        report = configurator.commit();
        return report.containsFailedResults() ? new Report(buildMessage(FAILURE, FAILED_PERSIST, FAILURE_TYPES.get(FAILED_PERSIST)))
            : new Report(buildMessage(SUCCESS, SUCCESSFUL_PERSIST, SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
    }

}
