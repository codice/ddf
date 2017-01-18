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

import static org.codice.ddf.admin.api.config.validation.SourceValidationUtils.validateOpensearchFactoryPid;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.validation.ValidationUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableMap;

public class OpenSearchSourceConfiguration extends SourceConfiguration {

    public static final String CONFIGURATION_TYPE = "opensearch-source";

    private static final Map<String, Function<OpenSearchSourceConfiguration, List<ConfigurationMessage>>> FIELDS_TO_VALIDATION_FUNC = new ImmutableMap.Builder<String, Function<OpenSearchSourceConfiguration, List<ConfigurationMessage>>>()
            .putAll(getBaseFieldValidationMap())
            .put(FACTORY_PID, config -> validateOpensearchFactoryPid(config.factoryPid(), FACTORY_PID))
            .build();

    public OpenSearchSourceConfiguration() {
    }

    public OpenSearchSourceConfiguration(SourceConfiguration baseConfig) {
        super(baseConfig);
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        return ValidationUtils.validate(fields, this, FIELDS_TO_VALIDATION_FUNC);
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, OpenSearchSourceConfiguration.class);
    }
}
