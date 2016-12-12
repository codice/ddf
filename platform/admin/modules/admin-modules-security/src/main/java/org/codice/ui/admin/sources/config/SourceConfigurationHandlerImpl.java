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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.NamespaceContext;

import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;

public class SourceConfigurationHandlerImpl implements ConfigurationHandler<SourceConfiguration> {

    public static final String SOURCE_CONFIGURATION_HANDLER_ID = "sources";

    public static final String VALID_URL_TEST_ID = "testValidUrl";

    public static final String MANUAL_URL_TEST_ID = "testManualUrl";

    public static final String DISCOVER_SOURCES_ID = "discoverSources";

    public static final String NONE_FOUND = "None found";

    public static final int PING_TIMEOUT = 3000;

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
            List<ProbeReport> sourceHandlerProbeReports = sourceConfigurationHandlers.stream()
                    .map(handler -> handler.probe(DISCOVER_SOURCES_ID, config))
                    .filter(probeReport -> !probeReport.containsUnsuccessfulMessages())
                    .collect(Collectors.toList());

            List<Object> discoveredSources = sourceHandlerProbeReports.stream()
                    .map(report -> report.getProbeResults()
                            .get(DISCOVER_SOURCES_ID))
                    .collect(Collectors.toList());

            List<ConfigurationMessage> probeSourceMessages = new ArrayList<>();
            sourceHandlerProbeReports.stream()
                    .map(report -> report.getMessages())
                    .forEach(results -> probeSourceMessages.addAll(results));

            sourcesProbeReport.addProbeResult(DISCOVER_SOURCES_ID, discoveredSources);
            sourcesProbeReport.addMessages(probeSourceMessages);
            return sourcesProbeReport;
        default:
            return new ProbeReport(Arrays.asList(buildMessage(FAILURE, "No such probe.")));
        }
    }

    public TestReport persist(SourceConfiguration config) {
        return new TestReport(buildMessage(FAILURE, "Cannot persist a SourceConfiguration."));
    }

    @Override
    public List<SourceConfiguration> getConfigurations() {
        SourceConfiguration sampleSrcConfig = new SourceConfiguration().displayName(
                "Same display name")
                .endpointUrl("endpoint url")
                .factoryPid("pid")
                .sourceHostName("host name")
                .sourceName("Source name")
                .sourceUserName("Source User Name")
                .sourceUserPassword("*******")
                .sourcePort(8993)
                .trustedCertAuthority(false);

        return Arrays.asList(sampleSrcConfig);
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(SourceConfiguration.class.getSimpleName(), SourceConfiguration.class);
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
