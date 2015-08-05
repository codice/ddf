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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.Update;

/**
 * Default implementation of {@link Update}
 *
 */
public class UpdateImpl implements Update {

    protected Metacard newMetacard;

    protected Metacard oldMetacard;

    /**
     * Create a new {@link UpdateImpl}
     *
     * @param newMetacard
     * @param oldMetacard
     */
    public UpdateImpl(Metacard newMetacard, Metacard oldMetacard) {
        super();
        this.newMetacard = newMetacard;
        this.oldMetacard = oldMetacard;
    }

    @Override
    public Metacard getNewMetacard() {
        return newMetacard;
    }

    @Override
    public Metacard getOldMetacard() {
        return oldMetacard;
    }

}
