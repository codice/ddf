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

package org.codice.ui.admin.sources.config.wfs;

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
import org.codice.ui.admin.wizard.api.probe.ProbeReport;
import org.codice.ui.admin.wizard.api.test.TestReport;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class WfsSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String WFS_SOURCE_CONFIGURATION_HANDLER_ID =
            "WfsSourceConfigurationHandler";

    private static final String WFS_SOURCE_DISPLAY_NAME = "WFS Source";
    public static final String WFS1_FACTORY_PID = "Wfs_v1_0_0_Federated_Source";
    public static final String WFS2_FACTORY_PID = "Wfs_v2_0_0_Federated_Source";

    private static final List<String> WFS_FACTORY_PIDS = Arrays.asList(WFS1_FACTORY_PID, WFS2_FACTORY_PID);

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        WfsSourceConfiguration configuration = new WfsSourceConfiguration(baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            Optional<String> url = WfsSourceUtils.confirmEndpointUrl(configuration);
            if(url.isPresent()) {
                configuration.endpointUrl(url.get());
            } else {
                results.add(buildMessage(FAILURE, "No WFS endpoint found."));
                return new ProbeReport(results);
            }
            try {
                configuration = WfsSourceUtils.getPreferredConfig(configuration);
            } catch (WfsSourceCreationException e) {
                results.add(buildMessage(FAILURE, "Failed to create configuration from valid request to valid endpoint."));
                return new ProbeReport(results);
            }
            results.add(buildMessage(SUCCESS, "Discovered WFS endpoint."));
            return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                    configuration.configurationHandlerId(WFS_SOURCE_CONFIGURATION_HANDLER_ID));
        default:
            results.add(new ConfigurationMessage("No such probe.", FAILURE));
            return new ProbeReport(results);
        }
    }



    @Override
    public TestReport test(String testId, SourceConfiguration configuration) {
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            WfsSourceConfiguration config = new WfsSourceConfiguration(configuration);
            Optional<ConfigurationMessage> message = SourceUtils.endpointIsReachable(config);
            if (message.isPresent()){
                return new TestReport(message.get());
            }
            return WfsSourceUtils.discoverUrlCapabilities(config);
        default:
            return new TestReport(buildMessage(FAILURE, "No such test."));
        }
    }

    public TestReport persist(SourceConfiguration configuration, String persistId) {
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case "create":
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create WFS Source")) :
                                                    new TestReport(buildMessage(SUCCESS, "WFS Source Created"));
        case "delete":
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete WFS Source")) :
                                                    new TestReport(buildMessage(SUCCESS, "WFS Source Deleted"));
        default:
            return new TestReport(buildMessage(FAILURE, "Uknown persist id: " + persistId));
        }
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        return WFS_FACTORY_PIDS.stream()
                .flatMap(factoryPid -> configurator.getManagedServiceConfigs(factoryPid).values().stream())
                .map(serviceProps -> new WfsSourceConfiguration(serviceProps))
                .collect(Collectors.toList());
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(WfsSourceConfiguration.class.getSimpleName(), WfsSourceConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return WFS_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return WfsSourceConfiguration.class;
    }

    @Override
    public String getSourceDisplayName() {
        return WFS_SOURCE_DISPLAY_NAME;
    }
}
