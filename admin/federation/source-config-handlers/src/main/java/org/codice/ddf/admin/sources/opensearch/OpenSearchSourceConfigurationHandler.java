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
import org.codice.ddf.admin.sources.opensearch.persist.CreateOpenSearchSourcePersistMethod;
import org.codice.ddf.admin.sources.opensearch.persist.DeleteOpenSearchSourcePersistMethod;
import org.codice.ddf.admin.sources.opensearch.probe.DiscoverOpenSearchSourceProbeMethod;
import org.codice.ddf.admin.sources.opensearch.test.OpenSearchUrlTestMethod;

public class OpenSearchSourceConfigurationHandler
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            "OpenSearchSourceConfigurationHandler";

    private static final String OPENSEARCH_SOURCE_DISPLAY_NAME = "OpenSearch Source";

    public static final String OPENSEARCH_FACTORY_PID = "OpenSearchSource";

    private List<TestMethod> testMethods = Arrays.asList(new OpenSearchUrlTestMethod());

    private List<ProbeMethod> probeMethods =
            Arrays.asList(new DiscoverOpenSearchSourceProbeMethod());

    private List<PersistMethod> persistMethods =
            Arrays.asList(new CreateOpenSearchSourcePersistMethod(),
                    new DeleteOpenSearchSourcePersistMethod());

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration baseConfiguration) {
        OpenSearchSourceConfiguration configuration = new OpenSearchSourceConfiguration(
                baseConfiguration);
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
        OpenSearchSourceConfiguration configuration = new OpenSearchSourceConfiguration(
                baseConfiguration);
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
        OpenSearchSourceConfiguration config = new OpenSearchSourceConfiguration(configuration);
        Optional<PersistMethod> persistMethod = persistMethods.stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();
        return persistMethod.isPresent() ?
                persistMethod.get()
                        .persist(config) :
                new ProbeReport((new ConfigurationMessage(NO_PERSIST_FOUND)));
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
        return new CapabilitiesReport(OpenSearchSourceConfiguration.class.getSimpleName(),
                OpenSearchSourceConfiguration.class);
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
