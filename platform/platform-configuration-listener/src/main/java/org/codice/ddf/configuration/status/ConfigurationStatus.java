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
package org.codice.ddf.configuration.status;

import static org.apache.commons.lang.Validate.notNull;

import java.nio.file.Path;

import javax.validation.constraints.NotNull;
/**
 * Class that provides configuration status for failed imports.
 */
public class ConfigurationStatus {

    private final Path path;

    /**
     * Constructor
     * 
     * @param path path of the failed import.
     */
    public ConfigurationStatus(@NotNull Path path) {
        notNull(path, "path cannot be null");
        this.path = path;
    }

    public Path getPath() {
        return this.path;
    }
}
