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

import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.ENDPOINT_URL;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.ID;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.PASSWORD;
import static org.codice.ddf.admin.api.config.federation.SourceConfiguration.USERNAME;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.handler.SourceConfigurationHandler.CREATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableMap;

public class CreateWfsSourcePersistMethod extends PersistMethod<WfsSourceConfiguration> {
    public static final String CREATE_WFS_SOURCE_ID = CREATE;

    public static final String DESCRIPTION =
            "Attempts to create and persist a WFS source given a configuration.";

    private static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(ID,
            "A unique name to identify the source.",
            ENDPOINT_URL,
            "The URL at which the WFS endpoint is located",
            FACTORY_PID,
            "The pid of the managed service factory to use to create the WFS Source.");

    private static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A user name used to authenticate with the WFS Source.",
            PASSWORD,
            "A password used to authenticate with the WFS Source.");

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST,
            "WFS Source successfully created.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST,
            "Failed to create WFS Source.");

    public CreateWfsSourcePersistMethod() {
        super(CREATE_WFS_SOURCE_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public TestReport persist(WfsSourceConfiguration configuration) {
        List<ConfigurationMessage> results =
                configuration.validate(new ArrayList(REQUIRED_FIELDS.keySet()));
        if (!results.isEmpty()) {
            return new TestReport(results);
        }
        Configurator configurator = new Configurator();
        ConfigReport report;
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        report = configurator.commit();
        return report.containsFailedResults() ?
                new TestReport(buildMessage(FAILURE, FAILED_PERSIST, FAILURE_TYPES.get(FAILED_PERSIST))) :
                new TestReport(buildMessage(SUCCESS, SUCCESSFUL_PERSIST, SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
    }

}
