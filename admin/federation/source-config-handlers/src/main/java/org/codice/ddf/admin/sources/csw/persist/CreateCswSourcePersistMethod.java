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
package org.codice.ddf.admin.sources.csw.persist;

import static org.codice.ddf.admin.api.config.services.CswServiceProperties.cswConfigToServiceProps;
import static org.codice.ddf.admin.api.config.sources.CswSourceConfiguration.FORCE_SPATIAL_FILTER;
import static org.codice.ddf.admin.api.config.sources.CswSourceConfiguration.OUTPUT_SCHEMA;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.FACTORY_PID;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.SOURCE_NAME;
import static org.codice.ddf.admin.api.config.sources.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.CREATE;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.SUCCESSFUL_PERSIST;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CreateCswSourcePersistMethod extends PersistMethod<CswSourceConfiguration> {

    public static final String CREATE_CSW_SOURCE_ID = CREATE;
    public static final String DESCRIPTION = "Attempts to create and persist a CSW source given a configuration.";

    //// TODO: tbatie - 1/17/17 - Add the event service address field EVENT_SERVICE_ADDRESS
    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(SOURCE_NAME, ENDPOINT_URL, FACTORY_PID);
    private static final List<String> OPTIONAL_FIELDS = ImmutableList.of(USERNAME, PASSWORD, OUTPUT_SCHEMA, FORCE_SPATIAL_FILTER);
    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "CSW Source successfully created.");
    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Failed to create CSW Source.");

    public CreateCswSourcePersistMethod() {
        super(CREATE_CSW_SOURCE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(CswSourceConfiguration configuration) {
        List<ConfigurationMessage> results = configuration.validate(REQUIRED_FIELDS);
        if (!results.isEmpty()) {
            return new Report(results);
        }

        Configurator configurator = new Configurator();
        configurator.createManagedService(configuration.factoryPid(), cswConfigToServiceProps(configuration));
        OperationReport report = configurator.commit();
        return report.containsFailedResults() ? new Report(buildMessage(FAILURE, FAILED_PERSIST, FAILURE_TYPES.get(FAILED_PERSIST)))
                 : new Report(buildMessage(SUCCESS, SUCCESSFUL_PERSIST, SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
    }

}
