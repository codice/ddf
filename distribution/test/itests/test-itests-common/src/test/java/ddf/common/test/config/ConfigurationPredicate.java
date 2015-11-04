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
 **/
package ddf.common.test.config;

import org.osgi.service.cm.Configuration;

/**
 * Base interface for all {@link Configuration} related predicates.
 *
 * @deprecated Use {@link ddf.common.test.WaitCondition} instead.
 */
public interface ConfigurationPredicate {
    /**
     * Tests whether the predicate's condition has been met or not.
     *
     * @param configuration {@link Configuration} object to run the predicate on. Predicate class
     *                      should assume that this can be {@code null}.
     * @return {@code true} only if the predicate's condition has been met
     */
    boolean test(Configuration configuration);

    /**
     * Should be overridden to provide a detailed description of what the predicate does, e.g.,
     * "property XYZ equals 123". This message will be used in log statements and exception
     * messages.
     */
    String toString();
}
