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
package org.codice.ddf.admin.core.api;

import java.util.Map;

/**
 * Provides all of the configuration details for a particular PID. This object is analogous to the
 * {@link org.osgi.service.cm.Configuration} object
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface ConfigurationDetails extends Map<String, Object> {

    String ID = "id";

    String FPID = "fpid";

    String NAME = "name";

    String BUNDLE = "bundle";

    String BUNDLE_NAME = "bundle_name";

    String BUNDLE_LOCATION = "bundle_location";

    String CONFIGURATION_PROPERTIES = "properties";

    String ENABLED = "enabled";

    String DISABLED_SERVICE_IDENTIFIER = "_disabled";

    default String getId() {
        return (String) get(ID);
    }

    default void setId(String id) {
        put(ID, id);
    }

    default String getFactoryPid() {
        return (String) get(FPID);
    }

    default void setFactoryPid(String fpid) {
        put(FPID, fpid);
    }

    default String getName() {
        return (String) get(NAME);
    }

    default void setName(String name) {
        put(NAME, name);
    }

    default long getBundle() {
        return (long) get(BUNDLE);
    }

    default void setBundle(long bundle) {
        put(BUNDLE, bundle);
    }

    default String getBundleName() {
        return (String) get(BUNDLE_NAME);
    }

    default void setBundleName(String bundleName) {
        put(BUNDLE_NAME, bundleName);
    }

    default String getBundleLocation() {
        return (String) get(BUNDLE_LOCATION);
    }

    default void setBundleLocation(String bundleLocation) {
        put(BUNDLE_LOCATION, bundleLocation);
    }

    default ConfigurationProperties getConfigurationProperties() {
        return (ConfigurationProperties) get(CONFIGURATION_PROPERTIES);
    }

    default void setConfigurationProperties(ConfigurationProperties configurationProperties) {
        put(CONFIGURATION_PROPERTIES, configurationProperties);
    }

    default boolean isEnabled() {
        return (boolean) get(ENABLED);
    }

    default void setEnabled(boolean enabled) {
        put(ENABLED, enabled);
    }
}
