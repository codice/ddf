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

import static org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration.OPENSEARCH_SOURCE_DISPLAY_NAME;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.config.federation.sources.OpenSearchSourceConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.Report;
import org.codice.ddf.admin.sources.opensearch.persist.CreateOpenSearchSourcePersistMethod;
import org.codice.ddf.admin.sources.opensearch.persist.DeleteOpenSearchSourcePersistMethod;
import org.codice.ddf.admin.sources.opensearch.probe.DiscoverOpenSearchSourceProbeMethod;
import org.codice.ddf.admin.sources.opensearch.test.OpenSearchUrlTestMethod;

public class OpenSearchSourceConfigurationHandler extends DefaultConfigurationHandler<SourceConfiguration>
        implements SourceConfigurationHandler<SourceConfiguration> {

    public static final String OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID =
            OpenSearchSourceConfiguration.CONFIGURATION_TYPE;

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return Arrays.asList(new DiscoverOpenSearchSourceProbeMethod());
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return Arrays.asList(new OpenSearchUrlTestMethod());
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return Arrays.asList(new CreateOpenSearchSourcePersistMethod(),
                new DeleteOpenSearchSourcePersistMethod());
    }

    @Override
    public ProbeReport probe(String probeId, SourceConfiguration configuration) {
        return super.probe(probeId, new OpenSearchSourceConfiguration(configuration));
    }

    @Override
    public Report test(String testId, SourceConfiguration configuration) {
        return super.test(testId, new OpenSearchSourceConfiguration(configuration));
    }

    @Override
    public Report persist(String persistId, SourceConfiguration configuration) {
        return super.persist(persistId, new OpenSearchSourceConfiguration(configuration));
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
    public String getConfigurationHandlerId() {
        return OPENSEARCH_SOURCE_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new OpenSearchSourceConfiguration().getConfigurationType();
    }

    @Override
    public String getSourceDisplayName() {
        return OPENSEARCH_SOURCE_DISPLAY_NAME;
    }
}
