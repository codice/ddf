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
package org.codice.ddf.admin.sources.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.sources.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.sources.impl.probe.DiscoverSourcesProbeMethod;
import org.codice.ddf.admin.sources.impl.probe.GetConfigHandlersProbeMethod;
import org.codice.ddf.admin.sources.impl.test.ValidUrlTestMethod;

import com.google.common.collect.ImmutableList;

// TODO: tbatie - 12/14/16 - Let's figure out a better name than impl
public class SourceConfigurationHandlerImpl extends
        DefaultConfigurationHandler<SourceConfiguration> {

    public static final String SOURCE_CONFIGURATION_HANDLER_ID = SourceConfiguration.CONFIGURATION_TYPE;

    private List<SourceConfigurationHandler> srcHandlers;

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return ImmutableList.of(new DiscoverSourcesProbeMethod(srcHandlers), new GetConfigHandlersProbeMethod(srcHandlers));
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return ImmutableList.of(new ValidUrlTestMethod());
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return null;
    }

    @Override
    public List getConfigurations() {
        return srcHandlers.stream()
                .map(ConfigurationHandler::getConfigurations)
                .flatMap(List<Object>::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String getConfigurationHandlerId() {
        return SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new SourceConfiguration().getConfigurationType();
    }

    public void setSourceConfigurationHandlers(
            List<SourceConfigurationHandler> sourceConfigurationHandlers) {
        this.srcHandlers = sourceConfigurationHandlers;
    }
}
