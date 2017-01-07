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

package org.codice.ddf.admin.sources.opensearch;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

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
import org.codice.ddf.admin.sources.opensearch.test.OpenSearchUrlTestMethod;

public class OpenSearchSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String DISCOVER_SOURCES_ID = "discoverSources";

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            "OpenSearchSourceConfigurationHandler";

    private static final String OPENSEARCH_SOURCE_DISPLAY_NAME = "OpenSearch Source";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    private List<TestMethod> testMethods = Arrays.asList(new OpenSearchUrlTestMethod());

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
    public TestReport test(String testId, SourceConfiguration baseConfiguration) {
        OpenSearchSourceConfiguration configuration = new OpenSearchSourceConfiguration(baseConfiguration);
        Optional<TestMethod> testMethod = testMethods.stream()
                .filter(method -> method.id().equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get().test(configuration) :
                new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(SourceConfiguration configuration, String persistId) {
        //TODO: add reflection methods to make configMap work
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch(persistId) {
        case CREATE:
            configurator.createManagedService(configuration.factoryPid(), configuration.configMap());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to create OpenSearch Source")) :
                    new TestReport(buildMessage(SUCCESS, "OpenSearch Source created"));
        case DELETE:
            // TODO: tbatie - 12/20/16 - Passed in factory pid and commit totally said it passed, should have based servicePid
            configurator.deleteManagedService(configuration.servicePid());
            report = configurator.commit();
            return report.containsFailedResults() ? new TestReport(buildMessage(FAILURE, "Failed to delete OpenSearch Source")) :
                    new TestReport(buildMessage(SUCCESS, "OpenSearch Source deleted"));
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
