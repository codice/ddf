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

package org.codice.ui.admin.sources.config.opensearch;

import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.DISCOVER_SOURCES_ID;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.MANUAL_URL_TEST_ID;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.util.ArrayList;
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

public class OpenSearchSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            "OpenSearchSourceConfigurationHandler";

    private static final String OPENSEARCH_SOURCE_DISPLAY_NAME = "OpenSearch Source";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        OpenSearchSourceConfiguration configuration = new OpenSearchSourceConfiguration(baseConfiguration);
        List<ConfigurationMessage> results = new ArrayList<>();
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            Optional<String> url = OpenSearchSourceUtils.confirmEndpointUrl(configuration);
            if (url.isPresent()) {
                configuration.endpointUrl(url.get());
                configuration.factoryPid(OPENSEARCH_FACTORY_PID);
                results.add(new ConfigurationMessage("Discovered OpenSearch endpoint.", SUCCESS));
                return new ProbeReport(results).addProbeResult(DISCOVER_SOURCES_ID,
                        configuration.configurationHandlerId(OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID));

            } else if(configuration.certError()) {
                results.add(buildMessage(FAILURE, "The discovered URL has incorrectly configured SSL certificates and is insecure."));
                return new ProbeReport(results);
            } else {
                results.add(buildMessage(FAILURE, "No OpenSearch endpoint found."));
                return new ProbeReport(results);
            }
        default:
            results.add(new ConfigurationMessage("No such probe.", FAILURE));
            return new ProbeReport(results);
        }
    }

    @Override
    public TestReport test(String testId, SourceConfiguration configuration) {
        switch (testId) {
        case MANUAL_URL_TEST_ID:
            OpenSearchSourceConfiguration config = new OpenSearchSourceConfiguration(configuration);
            Optional<ConfigurationMessage> message = SourceUtils.endpointIsReachable(config);
            if(message.isPresent()) {
                return new TestReport(message.get());
            }
            return OpenSearchSourceUtils.discoverUrlCapabilities(config);
        default:
            return new TestReport(buildMessage(FAILURE, "No such test."));
        }
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        //TODO: add reflection methods to make configMap work
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case "create":
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create Open Search Source")) :
                    new TestReport(buildMessage(SUCCESS, "Open Search Source created"));
        case "delete":
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete Open Search Source")) :
                    new TestReport(buildMessage(SUCCESS, "Open Search Source deleted"));
        default:
            return new TestReport(buildMessage(FAILURE, "Uknown persist id: " + persistId));
        }
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        return configurator.getManagedServiceConfigs(OPENSEARCH_FACTORY_PID)
                .values()
                .stream()
                .map(serviceProps -> new OpenSearchSourceConfiguration(serviceProps))
                .collect(Collectors.toList());
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(OpenSearchSourceConfiguration.class.getSimpleName(), OpenSearchSourceConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return OpenSearchSourceConfiguration.class;
    }

    @Override
    public String getSourceDisplayName() {
        return OPENSEARCH_SOURCE_DISPLAY_NAME;
    }
}
