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
package org.codice.ddf.migration;

import java.util.Iterator;

import org.apache.commons.lang.Validate;

/**
 * Exception that indicates multiple problems with the migration operation. The first error will
 * be attached as a cause while all the remaining ones will be attached as suppressed exceptions.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class CompoundMigrationException extends MigrationException {
    /**
     * Instantiates a new compound migration exception.
     *
     * @param errors the migration exceptions to compound together
     * @throws IllegalArgumentException if <code>errors</code> is <code>null</code> or empty
     */
    public CompoundMigrationException(Iterator<MigrationException> errors) {
        super(CompoundMigrationException.getFirstErrorFrom(errors));
        errors.forEachRemaining(this::addSuppressed);
    }

    private static MigrationException getFirstErrorFrom(Iterator<MigrationException> errors) {
        Validate.notNull(errors, "invalid null errors");
        Validate.isTrue(errors.hasNext(), "missing errors");
        return errors.next();
    }
}
