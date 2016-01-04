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

import java.io.IOException;
import java.util.Collection;

/**
 * The {@link ConfigurationStatusService} interface provides a method that
 * returns a {@link Collection} of {@link MigrationWarning} objects.
 * <p/>
 * Implementers of this interface provide status information about configuration files
 * that failed to import (i.e. configuration files that were move to the failed 
 * directory).
 */
public interface ConfigurationStatusService {
    
    /**
     * Gets a collection of configuration files that failed to import.
     * 
     * @return a {@link Collection} of {@ConfigurationStatus} objects
     * 
     * @throws IOException when the failed directory cannot be read.
     */
    public Collection<MigrationWarning> getFailedConfigurationFiles() throws IOException;
    
}
