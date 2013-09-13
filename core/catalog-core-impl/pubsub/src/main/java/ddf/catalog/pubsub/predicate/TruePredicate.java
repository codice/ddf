/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.pubsub.predicate;

import org.osgi.service.event.Event;

/**
 * A Predicate that always returns true. Meant to be used where criteria is missing and a predicate
 * is still needed to continue with execution
 * 
 * @author abarakat
 * 
 */
public class TruePredicate implements Predicate {

    /**
     * This method always returns true.
     */
    public boolean matches(Event properties) {
        return true;
    }

}
