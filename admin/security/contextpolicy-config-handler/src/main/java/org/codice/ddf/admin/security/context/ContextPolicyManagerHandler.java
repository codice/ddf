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

package org.codice.ddf.admin.security.context;

import static org.codice.ddf.admin.api.config.services.PolicyManagerServiceProperties.contextPolicyServiceToContextPolicyConfig;

import java.util.Arrays;
import java.util.List;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.context.ContextPolicyConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.security.context.persist.EditContextPolicyMethod;
import org.codice.ddf.admin.security.context.probe.AvailableOptionsProbeMethod;

public class ContextPolicyManagerHandler extends DefaultConfigurationHandler<ContextPolicyConfiguration> {

    private ConfigurationHandler ldapConfigHandler;
    public static final String CONTEXT_POLICY_MANAGER_HANDLER_ID = ContextPolicyConfiguration.CONFIGURATION_TYPE;

    @Override
    public String getConfigurationHandlerId() {
        return CONTEXT_POLICY_MANAGER_HANDLER_ID;
    }

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return Arrays.asList(new AvailableOptionsProbeMethod(ldapConfigHandler));
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return null;
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return Arrays.asList(new EditContextPolicyMethod());
    }

    @Override
    public List<ContextPolicyConfiguration> getConfigurations() {
        return Arrays.asList(contextPolicyServiceToContextPolicyConfig());
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ContextPolicyConfiguration().getConfigurationType();
    }

    public void setLdapConfigHandler(ConfigurationHandler ldapConfigHandler) {
        this.ldapConfigHandler = ldapConfigHandler;
    }
}
