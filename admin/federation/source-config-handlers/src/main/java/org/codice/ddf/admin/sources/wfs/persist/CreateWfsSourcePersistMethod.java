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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.sources.SourceConfigurationHandler.CREATE;

import java.util.Map;

import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.wfs.WfsSourceConfiguration;

import com.google.common.collect.ImmutableMap;

public class CreateWfsSourcePersistMethod extends PersistMethod<WfsSourceConfiguration> {
    public static final String CREATE_WFS_SOURCE_ID = CREATE;

    public static final String DESCRIPTION =
            "Attempts to create and persist a WFS source given a configuration.";

    //Required fields
    public static final String SOURCE_NAME = "id";

    public static final String WFS_URL = "endpointUrl";

    public static final String FACTORY_PID = "factoryPid";

    //Optional fields
    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    //Result types
    private static final String SOURCE_CREATED = "sourceCreated";

    private static final String CREATION_FAILED = "creationFailed";

    private static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(SOURCE_NAME,
            "A unique name to identify the source.",
            WFS_URL,
            "The URL at which the WFS endpoint is located",
            FACTORY_PID,
            "The pid of the managed service factory to use to create the WFS Source.");

    private static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A user name used to authenticate with the WFS Source.",
            PASSWORD,
            "A password used to authenticate with the WFS Source.");

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SOURCE_CREATED,
            "WFS Source successfully created.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CREATION_FAILED,
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
        Configurator configurator = new Configurator();
        ConfigReport report;
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        report = configurator.commit();
        return report.containsFailedResults() ?
                new TestReport(buildMessage(FAILURE, "Failed to create WFS Source")) :
                new TestReport(buildMessage(SUCCESS, "WFS Source created"));
    }
}
