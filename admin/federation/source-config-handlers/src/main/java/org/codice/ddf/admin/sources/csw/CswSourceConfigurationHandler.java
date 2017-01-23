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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_PERSIST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_PROBE_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.sources.SourceConfiguration;
import org.codice.ddf.admin.api.sources.SourceConfigurationHandler;
import org.codice.ddf.admin.sources.csw.persist.CreateCswSourcePersistMethod;
import org.codice.ddf.admin.sources.csw.persist.DeleteCswSourcePersistMethod;
import org.codice.ddf.admin.sources.csw.probe.DiscoverCswSourceProbeMethod;
import org.codice.ddf.admin.sources.csw.test.CswUrlTestMethod;

public class CswSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String CSW_SOURCE_CONFIGURATION_HANDLER_ID =
            "CswSourceConfigurationHandler";

    List<TestMethod> testMethods = Arrays.asList(new CswUrlTestMethod());

    List<ProbeMethod> probeMethods = Arrays.asList(new DiscoverCswSourceProbeMethod());

    List<PersistMethod> persistMethods = Arrays.asList(new CreateCswSourcePersistMethod(),
            new DeleteCswSourcePersistMethod());

    private static final String CSW_SOURCE_DISPLAY_NAME = "CSW Source";

    public static final String CSW_PROFILE_FACTORY_PID = "Csw_Federation_Profile_Source";

    public static final String CSW_GMD_FACTORY_PID = "Gmd_Csw_Federated_Source";

    public static final String CSW_SPEC_FACTORY_PID = "Csw_Federated_Source";

    //public static final String RETRIEVE_CONFIGURATION = "retrieveConfiguration";

    private static final List<String> CSW_FACTORY_PIDS = Arrays.asList(CSW_PROFILE_FACTORY_PID,
            CSW_GMD_FACTORY_PID,
            CSW_SPEC_FACTORY_PID);

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
        /*case RETRIEVE_CONFIGURATION:
            SourceConfiguration mockedCswSource =
                    new CswSourceConfiguration(baseConfiguration).sourceUserName("exampleUserName")
                            .factoryPid(CSW_PROFILE_FACTORY_PID)
                            .sourceUserPassword("exampleUserPassword");
            return new ProbeReport(buildMessage(SUCCESS, "Found and created CSW Source configuration"))
                    .addProbeResult(RETRIEVE_CONFIGURATION, mockedCswSource);
        */
        Optional<ProbeMethod> probeMethod = probeMethods.stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();

        return probeMethod.isPresent() ?
                probeMethod.get()
                        .probe(configuration) :
                new ProbeReport(new ConfigurationMessage(NO_PROBE_FOUND));
    }

    @Override
    public TestReport test(String testId, SourceConfiguration baseConfiguration) {
        CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
        Optional<TestMethod> testMethod = testMethods.stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get()
                        .test(configuration) :
                new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        CswSourceConfiguration config = new CswSourceConfiguration(configuration);
        Optional<PersistMethod> persistMethod = persistMethods.stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();
        return persistMethod.isPresent() ? persistMethod.get().persist(config) :
                new TestReport(new ConfigurationMessage(NO_PERSIST_FOUND));
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
    public CapabilitiesReport getCapabilities() {
        return null;
    }

    @Override
    public String getConfigurationHandlerId() {
        return CSW_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return CswSourceConfiguration.class;
    }

    @Override
    public String getSourceDisplayName() {
        return CSW_SOURCE_DISPLAY_NAME;
    }
}
