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

package org.codice.ui.admin.sources.config;

import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;

import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.probe.ProbeReport;
import org.codice.ui.admin.wizard.api.test.TestReport;

import com.google.common.collect.ImmutableMap;

// TODO: tbatie - 12/14/16 - Let's figure out a better name than impl
public class SourceConfigurationHandlerImpl implements ConfigurationHandler<SourceConfiguration> {

    public static final String SOURCE_CONFIGURATION_HANDLER_ID = "sources";

    public static final String VALID_URL_TEST_ID = "testValidUrl";

    public static final String MANUAL_URL_TEST_ID = "testManualUrl";

    public static final String DISCOVER_SOURCES_ID = "discoverSources";

    public static final String NONE_FOUND = "None found";

    public static final String SOURCE_CONFIGURATION_HANDLERS_ID = "sourceConfigurationHandlers";

    public static final String SOURCE_CONFIG_HANDLER_ID_KEY = "id";

    public static final String SOURCE_CONFIG_HANDLER_NAME_KEY = "name";

    public static final int PING_TIMEOUT = 500;

    /*********************************************************
     * NamespaceContext for Xpath queries
     *********************************************************/
    public static final NamespaceContext OWS_NAMESPACE_CONTEXT = new NamespaceContext() {
        @Override
        public String getNamespaceURI(String prefix) {
            return prefix.equals("ows") ? "http://www.opengis.net/ows" : null;
        }

        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }

        @Override
        public Iterator getPrefixes(String namespaceURI) {
            return null;
        }
    };

    List<SourceConfigurationHandler> sourceConfigurationHandlers;

    /*********************************************************
     * ConfigurationHandler methods
     *********************************************************/

    @Override
    public TestReport test(String testId, SourceConfiguration config) {
        switch (testId) {
        case VALID_URL_TEST_ID:
            return validUrlTest(config);
        default:
            return new TestReport(buildMessage(NO_TEST_FOUND));
        }
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
        return new CapabilitiesReport(SourceConfiguration.class.getSimpleName(),
                SourceConfiguration.class);
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

    private TestReport validUrlTest(SourceConfiguration config) {
        try (Socket connection = new Socket()) {
            connection.connect(new InetSocketAddress(config.sourceHostName(), config.sourcePort()),
                    PING_TIMEOUT);
            connection.close();
            return new TestReport(buildMessage(SUCCESS, "Was able to reach source successfully"));
        } catch (IOException e) {
            return new TestReport(buildMessage(FAILURE,
                    "Unable to reach specified hostname and port."));
        }
    }

    public void setSourceConfigurationHandlers(
            List<SourceConfigurationHandler> sourceConfigurationHandlers) {
        this.sourceConfigurationHandlers = sourceConfigurationHandlers;
    }
}
