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
package ddf.catalog.data;

import java.net.URI;

/**
 * Used to return the type and version of the content that is currently stored in a {@link Source}.
 * 
 */
public interface ContentType {

    /**
     * Get the name of this {@code ContentType}.
     * 
     * @return name of this {@code ContentType}
     */
    public String getName();

    /**
     * Get the version of this {@code ContentType}.
     * 
     * @return version of this {@code ContentType}
     */
    public String getVersion();

    /**
     * Get the namespace URI of this {@code ContentType}.
     * 
     * @return namespace URI of this {@code ContentType}
     */
    public URI getNamespace();
}
