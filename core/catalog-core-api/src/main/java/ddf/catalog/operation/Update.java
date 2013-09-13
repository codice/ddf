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
package ddf.catalog.operation;

import ddf.catalog.data.Metacard;

/**
 * Represents an update of a single {@link Metacard}, which has an older (previous) version and a
 * new (current) version.
 * 
 * @author michael.menousek@lmco.com
 * 
 */
public interface Update {
    /**
     * Get the value of the new version of the related {@link Metacard} for this {@link Update}
     * 
     * @return the new version of the {@link Metacard}
     */
    public Metacard getNewMetacard();

    /**
     * Get the value of the old version of the related {@link Metacard} for this {@link Update}
     * 
     * @return the old version of the {@link Metacard}
     */
    public Metacard getOldMetacard();
}
