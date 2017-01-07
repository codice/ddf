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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.api.sources.SourceUtils.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.sources.SourceUtils.MANUAL_URL_TEST_ID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.sources.SourceConfiguration;
import org.codice.ddf.admin.api.sources.SourceConfigurationHandler;
import org.codice.ddf.admin.sources.csw.test.CswUrlTestMethod;

public class CswSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String CSW_SOURCE_CONFIGURATION_HANDLER_ID =
            "CswSourceConfigurationHandler";

    List<TestMethod> testMethods = Arrays.asList(new CswUrlTestMethod());

    private static final String CSW_SOURCE_DISPLAY_NAME = "CSW Source";

    public static final String CSW_PROFILE_FACTORY_PID = "Csw_Federation_Profile_Source";

    public static final String CSW_GMD_FACTORY_PID = "Gmd_Csw_Federated_Source";

    public static final String CSW_SPEC_FACTORY_PID = "Csw_Federated_Source";

    public static final String RETRIEVE_CONFIGURATION  = "retrieveConfiguration";

    private static final List<String> CSW_FACTORY_PIDS = Arrays.asList(
            CSW_PROFILE_FACTORY_PID,
            CSW_GMD_FACTORY_PID,
            CSW_SPEC_FACTORY_PID);

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case RETRIEVE_CONFIGURATION:
            SourceConfiguration mockedCswSource =
                    new CswSourceConfiguration(baseConfiguration).sourceUserName("exampleUserName")
                            .factoryPid(CSW_PROFILE_FACTORY_PID)
                            .sourceUserPassword("exampleUserPassword");
            return new ProbeReport(buildMessage(SUCCESS, "Found and create CSW Source configuration")).addProbeResult(RETRIEVE_CONFIGURATION, mockedCswSource);
        case DISCOVER_SOURCES_ID:
            Optional<String> url = CswSourceUtils.confirmEndpointUrl(configuration);
            if (url.isPresent()) {
                configuration.endpointUrl(url.get());
            } else if(configuration.certError()) {
                results.add(buildMessage(FAILURE, "The discovered URL has incorrectly configured SSL certificates and is insecure."));
                return new ProbeReport(results);
            } else {
                results.add(buildMessage(FAILURE, "No CSW endpoint found."));
                return new ProbeReport(results);
            }
            try {
                configuration = CswSourceUtils.getPreferredConfig(configuration);
            } catch (CswSourceCreationException e) {
                results.add(new ConfigurationMessage(
                        "Failed to create configuration from valid request to valid endpoint.",
                        FAILURE));
                return new ProbeReport(results);
            }
            results.add(new ConfigurationMessage("Discovered CSW endpoint.", SUCCESS));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(CSW_SOURCE_CONFIGURATION_HANDLER_ID));
        default:
            results.add(new ConfigurationMessage("No such probe.", FAILURE));
            return new ProbeReport(results);
        }
    }

    @Override
    public TestReport test(String testId, SourceConfiguration baseConfiguration) {
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            CswSourceConfiguration configuration = new CswSourceConfiguration(baseConfiguration);
            Optional<TestMethod> testMethod = testMethods.stream()
                    .filter(method -> method.id().equals(testId))
                    .findFirst();

            return testMethod.isPresent() ?
                    testMethod.get().test(configuration) :
                    new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
        default:
            return new TestReport(buildMessage(FAILURE, "No such test."));
        }
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case CREATE:
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create CSW Source")) :
                    new TestReport(buildMessage(SUCCESS, "CSW Source created"));
        case DELETE:
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete CSW Source")) :
                    new TestReport(buildMessage(SUCCESS, "CSW Source deleted"));
        default:
            return new TestReport(buildMessage(FAILURE, "Unknown persist id: " + persistId));
        }
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
