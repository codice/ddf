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
package org.codice.ddf.admin.sources;

import static org.codice.ddf.admin.api.commons.SourceUtils.DISCOVER_SOURCES_ID;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.sources.commons.test.ValidUrlTestMethod;

import com.google.common.collect.ImmutableMap;

// TODO: tbatie - 12/14/16 - Let's figure out a better name than impl
public class SourceConfigurationHandlerImpl implements ConfigurationHandler<SourceConfiguration> {

    public static final String SOURCE_CONFIGURATION_HANDLER_ID = "sources";

    public static final String SOURCE_CONFIGURATION_HANDLERS_ID = "sourceConfigurationHandlers";

    public static final String SOURCE_CONFIG_HANDLER_ID_KEY = "id";

    public static final String SOURCE_CONFIG_HANDLER_NAME_KEY = "name";

    List<SourceConfigurationHandler> sourceConfigurationHandlers;

    private List<TestMethod> testMethods = Arrays.asList(new ValidUrlTestMethod());

    /*********************************************************
     * ConfigurationHandler methods
     *********************************************************/

    @Override
    public TestReport test(String testId, SourceConfiguration config) {
        Optional<TestMethod> testMethod = testMethods.stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get()
                        .test(config) :
                new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    public ProbeReport probe(String probeId, SourceConfiguration config) {
        // TODO: tbatie - 11/23/16 - Do check for required fields
        switch (probeId) {
        case DISCOVER_SOURCES_ID:
            ProbeReport sourcesProbeReport = new ProbeReport();

            List<ProbeReport> sourceProbeReports = sourceConfigurationHandlers.stream()
                    .map(handler -> handler.probe(DISCOVER_SOURCES_ID, config))
                    .collect(Collectors.toList());

            List<Object> discoveredSources = sourceProbeReports.stream()
                    .filter(probeReport -> !probeReport.containsUnsuccessfulMessages())
                    .map(report -> report.getProbeResults()
                            .get(DISCOVER_SOURCES_ID))
                    .collect(Collectors.toList());

            List<ConfigurationMessage> probeSourceMessages = sourceProbeReports.stream()
                    .filter(probeReport -> !probeReport.containsFailureMessages())
                    .map(ProbeReport::getMessages)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());

            sourcesProbeReport.addProbeResult(DISCOVER_SOURCES_ID, discoveredSources);
            sourcesProbeReport.addMessages(probeSourceMessages);
            return sourcesProbeReport;

        case SOURCE_CONFIGURATION_HANDLERS_ID:
            List<ImmutableMap> collect = sourceConfigurationHandlers.stream()
                    .map(handler -> ImmutableMap.builder()
                            .put(SOURCE_CONFIG_HANDLER_ID_KEY, handler.getConfigurationHandlerId())
                            .put(SOURCE_CONFIG_HANDLER_NAME_KEY, handler.getSourceDisplayName())
                            .build())
                    .collect(Collectors.toList());

            return new ProbeReport().addProbeResult(SOURCE_CONFIGURATION_HANDLERS_ID, collect);

        default:
            return new ProbeReport(Arrays.asList(buildMessage(FAILURE, "No such probe.")));
        }
    }

    public TestReport persist(SourceConfiguration config, String persistId) {
        return new TestReport(buildMessage(FAILURE, "Cannot persist a SourceConfiguration."));
    }

    @Override
    public List getConfigurations() {
        return sourceConfigurationHandlers.stream()
                .map(configHandler -> configHandler.getConfigurations())
                .flatMap(List<Object>::stream)
                .collect(Collectors.toList());
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(SOURCE_CONFIGURATION_HANDLER_ID,
                SOURCE_CONFIGURATION_HANDLER_ID,
                testMethods);
    }

    @Override
    public String getConfigurationHandlerId() {
        return SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return SourceConfiguration.class;
    }

    /*********************************************************
     * Helper methods
     *********************************************************/

    public void setSourceConfigurationHandlers(
            List<SourceConfigurationHandler> sourceConfigurationHandlers) {
        this.sourceConfigurationHandlers = sourceConfigurationHandlers;
    }
}
