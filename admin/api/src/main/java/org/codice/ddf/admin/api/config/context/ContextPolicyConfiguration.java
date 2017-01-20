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

package org.codice.ddf.admin.api.config.context;

import static org.codice.ddf.admin.api.validation.SecurityValidationUtils.validateContextPolicyBins;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateContextPaths;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.validation.ValidationUtils;

import com.google.common.collect.ImmutableMap;

/**
 * A {@link ContextPolicyConfiguration} is a container for {@link ContextPolicyBin}s and white listed
 * context policies.
 */
public class ContextPolicyConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "context-policy-manager";

    public static final String CONTEXT_POLICY_BINS = "contextPolicyBins";
    public static final String WHITE_LIST_CONTEXTS  = "whiteListContexts";
    public static final List<String> ALL_FIELDS = Arrays.asList(CONTEXT_POLICY_BINS, WHITE_LIST_CONTEXTS);

    private static final Map<String, Function<ContextPolicyConfiguration, List<ConfigurationMessage>>> FIELD_TO_VALIDATION_FUNC = new ImmutableMap.Builder<String, Function<ContextPolicyConfiguration, List<ConfigurationMessage>>>()
            .put(CONTEXT_POLICY_BINS, config -> validateContextPolicyBins(config.contextPolicyBins(), CONTEXT_POLICY_BINS))
            .put(WHITE_LIST_CONTEXTS, config -> validateContextPaths(config.whiteListContexts(), WHITE_LIST_CONTEXTS))
            .build();

    private List<ContextPolicyBin> contextPolicyBins;
    private List<String> whiteListContexts;

    public List<ConfigurationMessage> validate(List<String> fields) {
        return ValidationUtils.validate(fields, this, FIELD_TO_VALIDATION_FUNC);
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, ContextPolicyConfiguration.class);
    }

    //Getters
    public List<ContextPolicyBin> contextPolicyBins() {
        return contextPolicyBins;
    }
    public List<String> whiteListContexts() {
        return whiteListContexts;
    }

    //Setters
    public ContextPolicyConfiguration contextPolicyBins(List<ContextPolicyBin> contextPolicyBins) {
        this.contextPolicyBins = contextPolicyBins;
        return this;
    }
    public ContextPolicyConfiguration whiteListContexts(List<String> whiteListContexts) {
        this.whiteListContexts = whiteListContexts;
        return this;
    }
}


