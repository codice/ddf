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
package org.codice.ddf.admin.sources.wfs.persist;

import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.SERVICE_PID;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.DELETE;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.report.Report.createReport;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DeleteWfsSourcePersistMethod extends PersistMethod<WfsSourceConfiguration> {

    public static final String DELETE_WFS_SOURCE_ID = DELETE;

    public static final String DESCRIPTION = "Attempts to delete a WFS Source with the given configuration.";
    private static final List<String> REQUIRED_FIELDS = ImmutableList.of(SERVICE_PID);
    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "The CSW Source was successfully deleted.");
    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Failed to delete CSW source.");

    public DeleteWfsSourcePersistMethod() {
        super(DELETE_WFS_SOURCE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(WfsSourceConfiguration configuration) {
        Report results = new Report(configuration.validate(REQUIRED_FIELDS));
        if (!results.containsFailureMessages()) {
            return results;
        }

        Configurator configurator = new Configurator();
        configurator.deleteManagedService(configuration.servicePid());
        OperationReport report = configurator.commit();

        return createReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                report.containsFailedResults() ? FAILED_PERSIST : SUCCESSFUL_PERSIST);
    }

}
