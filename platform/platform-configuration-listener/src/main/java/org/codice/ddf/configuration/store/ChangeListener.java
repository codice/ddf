/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.configuration.store;

import javax.validation.constraints.NotNull;

/**
 * Interface configuration file change listener must implement.
 *
 * @see FileHandler#registerForChanges(ChangeListener)
 */
public interface ChangeListener {

    /**
     * Update types enumeration
     */
    enum ChangeType {
        CREATED, UPDATED, DELETED
    }

    /**
     * Method called when the properties associated with a configuration PID have changed.
     *
     * @param configurationPid persistence identifier (PID) of the configuration that has changed
     * @param changeType       type of change that occurred
     */
    void update(@NotNull String configurationPid, @NotNull ChangeType changeType);
}
