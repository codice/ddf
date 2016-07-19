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
package ddf.catalog.data.types;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface Metacard {

    /**
     * {@link ddf.catalog.data.Attribute} name for accessing the creation date of the {@link Metacard}. <br/>
     */
    String CREATED = "metacard.created";

    /**
     * {@link ddf.catalog.data.Attribute} name for accessing the modified date of the {@link Metacard}. <br/>
     */
    String MODIFIED = "metacard.modified";

    /**
     * {@link ddf.catalog.data.Attribute} name for accessing the owner of the {@link Metacard}. <br/>
     */
    String OWNER = "metacard.owner";

    /**
     * {@link ddf.catalog.data.Attribute} name for accessing the tags of the {@link Metacard}. <br/>
     */
    String TAGS = "metacard.tags";

    /**
     * {@link ddf.catalog.data.Attribute} name for accessing the security permissions of the {@link Metacard}. <br/>
     */
    String PERMISSIONS = "metacard.permissions";
}
