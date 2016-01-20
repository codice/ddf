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
package org.codice.ddf.migration;

import java.util.Collection;
import java.util.Collections;

/**
 * This class provides metadata with details of a {@link Migratable}'s export. 
 * 
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public class MigrationMetadata {
    
    private final Collection<MigrationWarning> migrationWarnings;

    /**
     * Constructor
     * 
     * @param warnings {@link Collection} of warnings describing possible issues
     * with the exported data.
     */
    public MigrationMetadata(Collection<MigrationWarning> warnings) {
        migrationWarnings = warnings;
    }

    /**
     * Gets the list of warnings produced by a {@link Migratable}'s export operation.
     * 
     * @return Collection of migration warnings.
     */
    public Collection<MigrationWarning> getMigrationWarnings() {
        return Collections.unmodifiableCollection(migrationWarnings);
    }
}
