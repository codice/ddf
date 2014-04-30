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
package org.codice.ddf.admin.configuration.plugin;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;

/**
 * A configuration plugin that passes the configuration's PID to the created 
 * Service so that the Service and its Configuration can be correlated in 
 * the future.
 */
public class ConfigPidConfigurationPlugin implements ConfigurationPlugin {

    /**
     * When a user creates/modifies a configuration (e.g., via the Felix Web 
     * Console Configuration Manager), this method is invoked prior to the 
     * {@link org.osgi.service.cm.ManagedService#updated(Dictionary)} method 
     * to add a configuration PID to the {@code Dictionary}. If the bean that
     * is getting created/modified has a 
     * {@code setConfigurationPid(java.lang.String)} method and the 
     * ManagedService/ManagedServiceFactory utilizes a container-managed 
     * update strategy, the configuration PID will subsequently be injected 
     * into the bean.
     * 
     * @param reference
     *            Reference to the Managed Service or Managed Service Factory
     * @param properties
     *            The configuration properties. This argument must not contain 
     *            the "service.bundleLocation" property. The value of this 
     *            property may be obtained from the 
     *            Configuration.getBundleLocation method
     */
    public void modifyConfiguration(ServiceReference<?> reference,
            Dictionary<String, Object> properties) {
        // if for some reason service.pid was null, this would throw a null 
        // pointer and break everything
        if (properties != null && properties.get("service.pid") != null) {
            properties.put("configurationPid", properties.get("service.pid"));
        }
    }
}
