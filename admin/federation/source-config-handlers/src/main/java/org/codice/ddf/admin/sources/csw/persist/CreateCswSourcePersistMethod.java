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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.sources.SourceConfigurationHandler.CREATE;

import java.util.Map;

import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.csw.CswSourceConfiguration;

import com.google.common.collect.ImmutableMap;

public class CreateCswSourcePersistMethod extends PersistMethod<CswSourceConfiguration> {

    public static final String CREATE_CSW_SOURCE_ID = CREATE;

    public static final String DESCRIPTION =
            "Attempts to create and persist a CSW source given a configuration.";

    //Required fields
    public static final String SOURCE_NAME = "id";

    public static final String CSW_URL = "cswUrl";

    public static final String EVENT_SERVICE_ADDRESS = "eventServiceAddress";

    public static final String FACTORY_PID = "factoryPid";

    //Optional fields
    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String OUTPUT_SCHEMA = "outputSchema";

    public static final String FORCE_SPATIAL_FILTER = "forceSpatialFilter";

    //Result types
    public static final String SOURCE_CREATED = "sourceCreated";

    private static final String CREATION_FAILED = "creationFailed";

    // Field -> Description maps
    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(SOURCE_NAME,
            "A descriptive name for the source to be configured.",
            CSW_URL,
            "The URL at which the CSW endpoint is located.",
            EVENT_SERVICE_ADDRESS,
            "The subscription URL for notification of events from this source.",
            FACTORY_PID,
            "The pid of the managed service factory used to create the source (determines configuration type).");

    private static final Map<String, String> OPTIONAL_FIELDS = ImmutableMap.of(USERNAME,
            "A user name used to authenticate with the source.",
            PASSWORD,
            "A password used to authenticate with the source.",
            OUTPUT_SCHEMA,
            "Specifies the schema the source uses to output data (only needed for CSW Specification sources).",
            FORCE_SPATIAL_FILTER,
            "Specifies a spatial filter for the source (only needed for CSW Specification sources).");

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SOURCE_CREATED,
            "CSW Source successfully created.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CREATION_FAILED,
            "Failed to create CSW Source.");

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
    public TestReport persist(CswSourceConfiguration configuration) {
        Configurator configurator = new Configurator();
        ConfigReport report;
        configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
        report = configurator.commit();
        return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE,
                "Failed to create CSW Source")) : new TestReport(buildMessage(SUCCESS,
                "CSW Source created"));
    }
}
