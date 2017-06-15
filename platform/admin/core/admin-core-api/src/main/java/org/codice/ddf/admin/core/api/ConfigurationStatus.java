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
 * Enabled or Disabled status of a configuration
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface ConfigurationStatus extends Map<String, String> {

    String DISABLED_EXTENSION = "_disabled";

    String NEW_FACTORY_PID = "newFactoryPid";

    String NEW_PID = "newPid";

    String ORIGINAL_PID = "originalPid";

    String ORIGINAL_FACTORY_PID = "originalFactoryPid";

    /**
     * Returns the updated configuration PID after the configuration has been either enabled
     * or disabled. This property is relevant only to managed service factory configurations.
     * @return configuration PID
     */
    default String getNewFactoryPid() {
        return get(NEW_FACTORY_PID);
    }

    default void setNewFactoryPid(String newFactoryPid) {
        put(NEW_FACTORY_PID, newFactoryPid);
    }

    /**
     * Returns the updated configuration PID after the configuration has been either enabled
     * or disabled. This property is relevant only to managed service configurations.
     * @return configuration PID
     */
    default String getNewPid() {
        return get(NEW_PID);
    }

    default void setNewPid(String newPid) {
        put(NEW_PID, newPid);
    }

    /**
     * Returns the original configuration PID after the configuration has been either enabled
     * or disabled. This property is relevant only to managed service configurations.
     * @return configuration PID
     */
    default String getOriginalPid() {
        return get(ORIGINAL_PID);
    }

    default void setOriginalPid(String originalPid) {
        put(ORIGINAL_PID, originalPid);
    }

    /**
     * Returns the original configuration PID after the configuration has been either enabled
     * or disabled. This property is relevant only to managed service factory configurations.
     * @return configuration PID
     */
    default String getOriginalFactoryPid() {
        return get(ORIGINAL_FACTORY_PID);
    }

    default void setOriginalFactoryPid(String originalFactoryPid) {
        put(ORIGINAL_FACTORY_PID, originalFactoryPid);
    }

    /**
     * Returns true if the configuration is currently disabled
     * @return true if the configuration is currently disabled
     */
    default boolean isDisabled() {
        return getOriginalFactoryPid().endsWith(DISABLED_EXTENSION);
    }
}
