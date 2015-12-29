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

package org.codice.ddf.configuration.admin;

import java.nio.file.Path;

import javax.validation.constraints.NotNull;

/**
 * Interface configuration file change listener must implement.
 *
 * @see ConfigurationFilesPoller#register(ChangeListener)
 */
public interface ChangeListener {

    /**
     * Method called when a new file has been created.
     *
     * @param file file that was created
     */
    void notify(@NotNull Path file);
}
