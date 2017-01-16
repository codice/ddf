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

package org.codice.ddf.admin.sources.wfs;

import static org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration.WFS1_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration.WFS2_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration.WFS_SOURCE_DISPLAY_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.wfs.persist.CreateWfsSourcePersistMethod;
import org.codice.ddf.admin.sources.wfs.persist.DeleteWfsSourcePersistMethod;
import org.codice.ddf.admin.sources.wfs.probe.DiscoverWfsSourceProbeMethod;
import org.codice.ddf.admin.sources.wfs.test.WfsUrlTestMethod;

public class WfsSourceConfigurationHandler extends DefaultConfigurationHandler<SourceConfiguration>
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String WFS_SOURCE_CONFIGURATION_HANDLER_ID = WfsSourceConfiguration.CONFIGURATION_TYPE;

    private static final List<String> WFS_FACTORY_PIDS = Arrays.asList(WFS1_FACTORY_PID,
            WFS2_FACTORY_PID);

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return Arrays.asList(new DiscoverWfsSourceProbeMethod());
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return Arrays.asList(new WfsUrlTestMethod());
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return Arrays.asList(new CreateWfsSourcePersistMethod(),
                new DeleteWfsSourcePersistMethod());
    }

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration configuration) {
        return super.probe(probeId, new WfsSourceConfiguration(configuration));
    }

    @Override
    public TestReport test(String testId, SourceConfiguration configuration) {
        return super.test(testId, new WfsSourceConfiguration(configuration));
    }

    @Override
    public TestReport persist(String persistId, SourceConfiguration configuration) {
        return super.persist(persistId, new WfsSourceConfiguration(configuration));
    }


    @Override
    public List<SourceConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        return WFS_FACTORY_PIDS.stream()
                .flatMap(factoryPid -> configurator.getManagedServiceConfigs(factoryPid)
                        .values()
                        .stream())
                .map(serviceProps -> new WfsSourceConfiguration(serviceProps))
                .collect(Collectors.toList());
    }

    @Override
    public String getConfigurationHandlerId() {
        return WFS_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new WfsSourceConfiguration().getConfigurationType();
    }

    @Override
    public String getSourceDisplayName() {
        return WFS_SOURCE_DISPLAY_NAME;
    }
}
