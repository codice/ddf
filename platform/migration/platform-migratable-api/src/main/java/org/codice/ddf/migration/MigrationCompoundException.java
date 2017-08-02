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

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * Exception that indicates multiple problems with the configuration migration. The first error will
 * be attached as a caused while all the remaining ones will be attached as suppressed exceptions.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class MigrationCompoundException extends RuntimeException {
    private static final long serialVersionUID = 1;

    /**
     * Instantiates a new exception.
     *
     * @param errors the migration exceptions to compound together
     */
    public MigrationCompoundException(@Nullable Collection<MigrationException> errors) {
        super(MigrationCompoundException.getFirstErrorMessageFrom(errors));
        if ((errors != null) && !errors.isEmpty()) {
            boolean first = true;

            for (MigrationException e : errors) {
                if (first) {
                    first = false;
                    initCause(e);
                } else {
                    addSuppressed(e);
                }
            }
        }
    }

    private static String getFirstErrorMessageFrom(Collection<MigrationException> errors) {
        if ((errors == null) || errors.isEmpty()) {
            return null;
        }
        return errors.iterator()
                .next()
                .getMessage();
    }
}
