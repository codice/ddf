/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.validation;

import ddf.catalog.data.Metacard;

/**
 * A service that validates a Metacard and provides information if problems
 * exist with the Metacard data.
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author Ashraf Barakat, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface MetacardValidator {

    /**
     * Validates a {@link Metacard}
     * 
     * @param metacard
     *            {@link Metacard} to validate
     * @throws ValidationException
     *             if any validation error occurs
     */
    public void validate(Metacard metacard) throws ValidationException;

}
