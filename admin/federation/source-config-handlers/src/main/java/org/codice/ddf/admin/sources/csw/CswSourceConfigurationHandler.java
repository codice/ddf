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

package org.codice.ddf.admin.sources.csw;

import static org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration.CSW_GMD_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration.CSW_PROFILE_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration.CSW_SOURCE_DISPLAY_NAME;
import static org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration.CSW_SPEC_FACTORY_PID;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.config.federation.sources.CswSourceConfiguration;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.csw.persist.CreateCswSourcePersistMethod;
import org.codice.ddf.admin.sources.csw.persist.DeleteCswSourcePersistMethod;
import org.codice.ddf.admin.sources.csw.probe.DiscoverCswSourceProbeMethod;
import org.codice.ddf.admin.sources.csw.test.CswUrlTestMethod;

public class CswSourceConfigurationHandler extends DefaultConfigurationHandler<SourceConfiguration>
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String CSW_SOURCE_CONFIGURATION_HANDLER_ID =
            CswSourceConfiguration.CONFIGURATION_TYPE;

    private static final List<String> CSW_FACTORY_PIDS = Arrays.asList(CSW_PROFILE_FACTORY_PID,
            CSW_GMD_FACTORY_PID,
            CSW_SPEC_FACTORY_PID);

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return Arrays.asList(new DiscoverCswSourceProbeMethod());
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return Arrays.asList(new CswUrlTestMethod());
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return Arrays.asList(new CreateCswSourcePersistMethod(),
                new DeleteCswSourcePersistMethod());
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        return CSW_FACTORY_PIDS.stream()
                .flatMap(factoryPid -> configurator.getManagedServiceConfigs(factoryPid)
                        .values()
                        .stream())
                .map(CswSourceConfiguration::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getConfigurationHandlerId() {
        return CSW_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new CswSourceConfiguration().getConfigurationType();
    }

    @Override
    public String getSourceDisplayName() {
        return CSW_SOURCE_DISPLAY_NAME;
    }
}
