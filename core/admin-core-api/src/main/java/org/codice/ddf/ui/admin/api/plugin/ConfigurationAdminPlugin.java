/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.ui.admin.api.plugin;

import java.util.Map;

import org.osgi.framework.BundleContext;

public interface ConfigurationAdminPlugin {
    /**
     * Returns a map of configuration data that should be appended to the configurationDataMap
     * parameter. The configurationDataMap that is passed into this function is unmodifiable and is
     * passed in to simply expose what information already exists.
     * 
     * @param configurationPid
     *            service.pid for the ConfigurationAdmin configuration
     * @param configurationDataMap
     *            map of what properties have already been added to the configuration in question
     * @param bundleContext
     *            used to retrieve list of services
     * @return Map defining additional properties to add to the configuration
     */
    public Map<String, Object> getConfigurationData(String configurationPid,
            Map<String, Object> configurationDataMap, BundleContext bundleContext);
}
