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
package ddf.content.plugin.cataloger;

import java.io.InputStream;

import ddf.catalog.data.Metacard;
import ddf.catalog.util.Describable;

/**
 * Interface defining the contract for all transformers that parse content and create
 * {@link Metacard}s.
 * 
 * @since 2.1.0
 */
public interface ContentTransformer extends Describable {
    /**
     * Creates a {@link Metacard} by parsing the specified {@link InputStream}.
     * 
     * @param stream
     *            the {@link InputStream} to parse
     * @return the {@link Metacard} created
     */
    public Metacard createMetacard(InputStream stream);

    /**
     * Updates an existing {@link Metacard} in the catalog using the specified {@link InputStream}.
     * 
     * @param currentMetacard
     *            the {@link Metacard} to be updated
     * @param stream
     *            the {@link InputStream} to parse for updating the {@link Metacard}
     * @return the updated {@link Metacard}
     */
    public Metacard updateMetacard(Metacard currentMetacard, InputStream stream);
}
