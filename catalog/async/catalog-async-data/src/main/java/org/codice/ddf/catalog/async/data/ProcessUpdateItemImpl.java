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
package org.codice.ddf.catalog.async.data;

import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;

import ddf.catalog.data.Metacard;

public class ProcessUpdateItemImpl implements ProcessUpdateItem {

    private ProcessResource processResource;

    private Metacard oldMetacard;

    private Metacard newMetacard;

    private boolean isMetacardModified;

    public ProcessUpdateItemImpl(ProcessResource processResource, Metacard newMetacard,
            Metacard oldMetacard) {
        this(processResource, newMetacard, oldMetacard, true);
    }

    public ProcessUpdateItemImpl(ProcessResource processResource, Metacard newMetacard,
            Metacard oldMetacard, boolean isMetacardModified) {
        this.processResource = processResource;
        this.newMetacard = newMetacard;
        this.oldMetacard = oldMetacard;
        this.isMetacardModified = isMetacardModified;
    }

    @Override
    public Metacard getMetacard() {
        return newMetacard;
    }

    @Override
    public Metacard getOldMetacard() {
        return oldMetacard;
    }

    @Override
    public ProcessResource getProcessResource() {
        return processResource;
    }

    @Override
    public boolean isMetacardModified() {
        return isMetacardModified;
    }

    public void markMetacardAsModified() {
        isMetacardModified = true;
    }
}
