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

import javax.validation.constraints.NotNull;

/**
 * Interface configuration file change listener must implement.
 *
 * @see ConfigurationFilesPoller#register(ChangeListener)
 */
public interface ChangeListener {

    /**
<<<<<<< HEAD:platform/platform-configuration-migration/src/main/java/org/codice/ddf/configuration/admin/ChangeListener.java
     * Method called when a new file has been created.
     *
     * @param file file that was created
     */
    void notify(@NotNull Path file);
=======
     * Constructor
     *
     * @param message message regarding migration
     */
    public MigrationWarning(@NotNull String message) {
        notNull(message, "message cannot be null");
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }
>>>>>>> DDF-1750 Updates unit tests, fixes compilation errors:platform/platform-configuration-listener/src/main/java/org/codice/ddf/configuration/status/MigrationWarning.java
}
