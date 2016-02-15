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
package org.codice.ddf.ui.admin.api;

import java.util.List;
import java.util.Map;

/**
 * Interface for the System Properties Admin MBean. Allows exposing the system
 * properties out via a JMX MBean interface.
 */
public interface SystemPropertiesAdminMBean {
    public static final String OBJECT_NAME =
            "org.codice.ddf.ui.admin.api:type=SystemPropertiesAdminMBean";

    /**
     * Reads and returns system properties
     *
     * @return A list of SystemPropertyDetails items.
     */
    List<SystemPropertyDetails> readSystemProperties();

    /**
     * Updates System Properties
     *
     * @param updatedSystemProperties A key value mapping of property name and value to be written
     */
    void writeSystemProperties(Map<String, String> updatedSystemProperties);
}
