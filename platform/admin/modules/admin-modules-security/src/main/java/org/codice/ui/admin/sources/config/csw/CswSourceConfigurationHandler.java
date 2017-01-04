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

package org.codice.ui.admin.sources.config.csw;

import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.DISCOVER_SOURCES_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.MANUAL_URL_TEST_ID;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.sources.config.SourceConfigurationHandler;
import org.codice.ui.admin.sources.config.SourceUtils;
import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class CswSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String CSW_SOURCE_CONFIGURATION_HANDLER_ID =
            "CswSourceConfigurationHandler";

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
            Optional<ConfigurationMessage> message = SourceUtils.endpointIsReachable(configuration);
            if(message.isPresent()){
                return new TestReport(message.get());
            }
            return CswSourceUtils.discoverUrlCapabilites(configuration);
        default:
            return new TestReport(buildMessage(FAILURE, "No such test."));
        }
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case "create":
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create CSW Source")) :
                    new TestReport(buildMessage(SUCCESS, "CSW Source created"));
        case "delete":
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete CSW Source")) :
                    new TestReport(buildMessage(SUCCESS, "CSW Source deleted"));
        default:
            return new TestReport(buildMessage(FAILURE, "Uknown persist id: " + persistId));
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
