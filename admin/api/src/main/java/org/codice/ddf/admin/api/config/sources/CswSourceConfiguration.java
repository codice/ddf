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

package org.codice.ddf.admin.api.config.sources;

import static org.codice.ddf.admin.api.config.validation.SourceValidationUtils.validateCswFactoryPid;
import static org.codice.ddf.admin.api.config.validation.SourceValidationUtils.validateCswOutputSchema;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.validation.ValidationUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableMap;

public class CswSourceConfiguration extends SourceConfiguration {

    public static final String CONFIGURATION_TYPE = "csw-source";

    public static final String OUTPUT_SCHEMA = "outputSchema";
    public static final String FORCE_SPATIAL_FILTER = "forceSpatialFilter";

    private static final Map<String, Function<CswSourceConfiguration, List<ConfigurationMessage>>> FIELDS_TO_VALIDATION_FUNC = new ImmutableMap.Builder<String, Function<CswSourceConfiguration, List<ConfigurationMessage>>>()
            .putAll(getBaseFieldValidationMap())
            .put(FACTORY_PID, config -> validateCswFactoryPid(config.factoryPid(), FACTORY_PID))
            .put(OUTPUT_SCHEMA, config -> validateCswOutputSchema(config.outputSchema(), OUTPUT_SCHEMA))
            .build();

    private String outputSchema;
    private String forceSpatialFilter;

    public CswSourceConfiguration() {}

    public CswSourceConfiguration(SourceConfiguration baseConfig) {
        super(baseConfig);
        if (baseConfig instanceof CswSourceConfiguration) {
            outputSchema(((CswSourceConfiguration) baseConfig).outputSchema());
            forceSpatialFilter(((CswSourceConfiguration) baseConfig).forceSpatialFilter());
        }
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        return ValidationUtils.validate(fields, this, FIELDS_TO_VALIDATION_FUNC);
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, CswSourceConfiguration.class);
    }

    //Getters
    public String outputSchema() {
        return outputSchema;
    }
    public String forceSpatialFilter() {
        return forceSpatialFilter;
    }

    //Setters
    public CswSourceConfiguration outputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
        return this;
    }
    public CswSourceConfiguration forceSpatialFilter(String forceSpatialFilter) {
        this.forceSpatialFilter = forceSpatialFilter;
        return this;
    }
}
