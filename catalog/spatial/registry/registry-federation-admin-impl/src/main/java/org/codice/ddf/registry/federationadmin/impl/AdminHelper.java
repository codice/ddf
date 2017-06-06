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
package org.codice.ddf.registry.federationadmin.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.registry.api.internal.RegistryStore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.Source;

public class AdminHelper {
    private static final String REGISTRY_FILTER = String.format(
            "(|(%s=*Registry*Store*)(%s=*registry*store*))",
            ConfigurationAdmin.SERVICE_FACTORYPID,
            ConfigurationAdmin.SERVICE_FACTORYPID);

    private static final String MAP_ENTRY_ID = "id";

    private static final String DISABLED = "_disabled";

    private final ConfigurationAdmin configurationAdmin;

    private final org.codice.ddf.admin.core.api.ConfigurationAdmin configAdmin;

    public AdminHelper(org.codice.ddf.admin.core.api.ConfigurationAdmin configAdmin,
            ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        this.configAdmin = configAdmin;
    }

    private BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        if (bundle != null) {
            return bundle.getBundleContext();
        }
        return null;
    }

    public List<Source> getRegistrySources() throws InvalidSyntaxException {
        List<ServiceReference<? extends Source>> refs = new ArrayList<>();
        refs.addAll(getBundleContext().getServiceReferences(RegistryStore.class, null));
        return refs.stream()
                .map(e -> getBundleContext().getService(e))
                .collect(Collectors.toList());
    }

    public List<Service> getMetatypes() {
        return configAdmin.listServices(REGISTRY_FILTER, REGISTRY_FILTER);
    }

    public List<Configuration> getConfigurations(Service metatype)
            throws InvalidSyntaxException, IOException {
        return Arrays.asList(configurationAdmin.listConfigurations(
                String.format("(|(%s=%s)(%s=%s%s))",
                        ConfigurationAdmin.SERVICE_FACTORYPID,
                        metatype.getId(),
                        ConfigurationAdmin.SERVICE_FACTORYPID,
                        metatype.getId(),
                        DISABLED)));
    }

    public Configuration getConfiguration(ConfiguredService cs) throws IOException {
        return configurationAdmin.getConfiguration(cs.getConfigurationPid());
    }

    public String getBundleName(Configuration config) {
        return configAdmin.getName(getBundleContext().getBundle(config.getBundleLocation()));
    }

    public long getBundleId(Configuration config) {
        return getBundleContext().getBundle(config.getBundleLocation())
                .getBundleId();
    }

    public String getName(Configuration config) {
        return configAdmin.getObjectClassDefinition(config)
                .getName();
    }
}
