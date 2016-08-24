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
package org.codice.ddf.itests.common.config;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.codice.ddf.itests.common.AdminConfig;
import org.osgi.service.cm.Configuration;


public class ConfigureTestCommons {

    public static final String METACARD_VALIDATITY_FILTER_PLUGIN_SERVICE_PID = "ddf.catalog.metacard.validation.MetacardValidityFilterPlugin";

    public static final String METACARD_VALIDATITY_MARKER_PLUGIN_SERVICE_PID = "ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin";

    public static final String CACHING_FEDERATION_STRATEGY_PID = "ddf.catalog.federation.impl.CachingFederationStrategy";


    public static void configureMetacardValidityFilterPlugin(List<String> securityAttributeMappings, AdminConfig configAdmin)
            throws IOException {
        Configuration config = configAdmin.getConfiguration(
                METACARD_VALIDATITY_FILTER_PLUGIN_SERVICE_PID,
                null);
        Dictionary properties = new Hashtable<>();
        properties.put("attributeMap", securityAttributeMappings);
        config.update(properties);
    }

    public static void configureShowInvalidMetacards(String showErrors, String showWarnings, AdminConfig configAdmin)
            throws IOException {
        Configuration config = configAdmin.getConfiguration(
                CACHING_FEDERATION_STRATEGY_PID,
                null);

        Dictionary properties = new Hashtable<>();
        properties.put("showErrors", showErrors);
        properties.put("showWarnings", showWarnings);
        config.update(properties);
    }

    public static void configureFilterInvalidMetacards(String filterErrors, String filterWarnings, AdminConfig configAdmin)
            throws IOException {
        Configuration config = configAdmin.getConfiguration(
                METACARD_VALIDATITY_FILTER_PLUGIN_SERVICE_PID,
                null);

        Dictionary properties = new Hashtable<>();
        properties.put("filterErrors", filterErrors);
        properties.put("filterWarnings", filterWarnings);
        config.update(properties);
    }

    public static void configureEnforceValidityErrorsAndWarnings(String enforceErrors,
            String enforceWarnings, AdminConfig configAdmin) throws IOException {
        Configuration config = configAdmin.getConfiguration(
                METACARD_VALIDATITY_MARKER_PLUGIN_SERVICE_PID,
                null);

        Dictionary properties = new Hashtable<>();
        properties.put("enforceErrors", enforceErrors);
        properties.put("enforceWarnings", enforceWarnings);
        config.update(properties);
    }

    public static void configureEnforcedMetacardValidators(List<String> enforcedValidators, AdminConfig configAdmin)
            throws IOException {

        // Update metacardMarkerPlugin config with no enforcedMetacardValidators
        Configuration config = configAdmin.getConfiguration(
                METACARD_VALIDATITY_MARKER_PLUGIN_SERVICE_PID,
                null);

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("enforcedMetacardValidators", enforcedValidators);
        config.update(properties);
    }
}
