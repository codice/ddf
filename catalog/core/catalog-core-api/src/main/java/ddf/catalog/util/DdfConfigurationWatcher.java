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
package ddf.catalog.util;

import java.util.Map;

/**
 * This interface is used to specify a source as a watcher of updates to the DDF system
 * configuration settings. Whenever the source is configured, or updates are made to the DDF System
 * Settings, the source will receive the entire list of the most current DDF system settings.
 * 
 * It is up to the DdfConfigurationWatcher to determine which DDF system settings are of interest,
 * if their values have changed, and how to react to their values.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public interface DdfConfigurationWatcher {

    // TODO parameterize this raw type
    /**
     * Invoked by the DdfConfigurationManager when the DDF System Settings are modified. The Map of
     * configuration properties contains the entire list of system settings, not just the ones that
     * have changed.
     * 
     * @param configuration
     *            the entire list of DDF system settings
     */
    public void ddfConfigurationUpdated(Map configuration);

}
