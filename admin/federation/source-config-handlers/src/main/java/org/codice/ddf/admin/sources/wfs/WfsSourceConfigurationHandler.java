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
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.NO_METHOD_FOUND;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.config.federation.sources.WfsSourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.sources.wfs.persist.CreateWfsSourcePersistMethod;
import org.codice.ddf.admin.sources.wfs.persist.DeleteWfsSourcePersistMethod;
import org.codice.ddf.admin.sources.wfs.probe.DiscoverWfsSourceProbeMethod;
import org.codice.ddf.admin.sources.wfs.test.WfsUrlTestMethod;

public class WfsSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String WFS_SOURCE_CONFIGURATION_HANDLER_ID = WfsSourceConfiguration.CONFIGURATION_TYPE;

    List<TestMethod> testMethods = Arrays.asList(new WfsUrlTestMethod());

    List<ProbeMethod> probeMethods = Arrays.asList(new DiscoverWfsSourceProbeMethod());

    List<PersistMethod> persistMethods = Arrays.asList(new CreateWfsSourcePersistMethod(),
            new DeleteWfsSourcePersistMethod());


    private static final List<String> WFS_FACTORY_PIDS = Arrays.asList(WFS1_FACTORY_PID,
            WFS2_FACTORY_PID);

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        WfsSourceConfiguration configuration = new WfsSourceConfiguration(baseConfiguration);
        Optional<ProbeMethod> probeMethod = probeMethods.stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();
        return probeMethod.isPresent() ?
                probeMethod.get()
                        .probe(configuration) :
                new ProbeReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport test(String testId, SourceConfiguration baseConfiguration) {
        WfsSourceConfiguration configuration = new WfsSourceConfiguration(baseConfiguration);
        Optional<TestMethod> testMethod = testMethods.stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get()
                        .test(configuration) :
                new TestReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport persist(String persistId, SourceConfiguration configuration) {
        WfsSourceConfiguration config = new WfsSourceConfiguration(configuration);
        Optional<PersistMethod> persistMethod = persistMethods.stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();

        return persistMethod.isPresent() ?
                persistMethod.get()
                        .persist(config) :
                new TestReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
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
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(WfsSourceConfiguration.class.getSimpleName(),
                WfsSourceConfiguration.class);
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
