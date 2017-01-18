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
package org.codice.ddf.catalog.async.data.impl;

import static org.apache.commons.lang.Validate.notNull;

import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;

import ddf.catalog.data.Metacard;

public class ProcessUpdateItemImpl extends ProcessItemImpl implements ProcessUpdateItem {

    private ProcessResource processResource;

    private Metacard oldMetacard;

    private boolean isMetacardModified;

    public ProcessUpdateItemImpl(ProcessResource processResource, Metacard newMetacard,
            Metacard oldMetacard) {
        this(processResource, newMetacard, oldMetacard, true);
    }

    public ProcessUpdateItemImpl(ProcessResource processResource, Metacard newMetacard,
            Metacard oldMetacard, boolean isMetacardModified) {
        super(newMetacard);

        notNull(processResource, "ProcessUpdateItemImpl argument processResource may not be null");
        notNull(oldMetacard, "ProcessUpdateItemImpl argument newMetacard may not be null");

        this.processResource = processResource;
        this.oldMetacard = oldMetacard;
        this.isMetacardModified = isMetacardModified;
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
