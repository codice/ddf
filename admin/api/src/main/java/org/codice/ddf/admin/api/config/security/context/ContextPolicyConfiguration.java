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

package org.codice.ddf.admin.api.config.security.context;

import java.util.List;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;

public class ContextPolicyConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "context-policy-manager";

    private List<ContextPolicyBin> contextPolicyBins;

    private List<String> whiteListContexts;

    public List<ContextPolicyBin> contextPolicyBins() {
        return contextPolicyBins;
    }

    public ContextPolicyConfiguration contextPolicyBins(List<ContextPolicyBin> contextPolicyBins) {
        this.contextPolicyBins = contextPolicyBins;
        return this;
    }

    public List<String> whiteListContexts() {
        return whiteListContexts;
    }

    public ContextPolicyConfiguration whiteListContexts(List<String> whiteListContexts) {
        this.whiteListContexts = whiteListContexts;
        return this;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, ContextPolicyConfiguration.class);
    }
}


