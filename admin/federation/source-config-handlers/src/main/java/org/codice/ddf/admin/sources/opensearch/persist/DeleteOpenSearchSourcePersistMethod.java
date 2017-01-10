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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.sources.SourceConfigurationHandler.DELETE;

import java.util.Map;

import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.opensearch.OpenSearchSourceConfiguration;

import com.google.common.collect.ImmutableMap;

public class DeleteOpenSearchSourcePersistMethod
        extends PersistMethod<OpenSearchSourceConfiguration> {
    public static final String DELETE_OPENSEARCH_SOURCE_ID = DELETE;

    public static final String DESCRIPTION =
            "Attempts to delete an OpenSearch source with the given configuration.";

    public static final String SERVICE_PID = "servicePid";

    private static final String SOURCE_DELETED = "sourceDeleted";

    private static final String DELETE_FAILED = "deleteFailed";

    private static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(SERVICE_PID,
            "The unique pid of the service to be deleted.");

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SOURCE_DELETED,
            "The CSW Source was successfully deleted.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(DELETE_FAILED,
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
    public TestReport persist(OpenSearchSourceConfiguration configuration) {
        Configurator configurator = new Configurator();
        ConfigReport report;
        // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
        configurator.deleteManagedService(configuration.servicePid());
        report = configurator.commit();
        return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE,
                "Failed to delete OpenSearch Source")) : new TestReport(buildMessage(SUCCESS,
                "OpenSearch Source deleted"));
    }
}
