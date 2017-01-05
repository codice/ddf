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

import org.codice.ddf.catalog.async.data.impl.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.impl.api.internal.ProcessResource;

import ddf.catalog.data.Metacard;

public class ProcessCreateItemImpl implements ProcessCreateItem {

    private ProcessResource processResource;

    private Metacard metacard;

    private boolean isMetacardModified;

    public ProcessCreateItemImpl(ProcessResource processResource, Metacard metacard) {
        this(processResource, metacard, true);
    }

    public ProcessCreateItemImpl(ProcessResource processResource, Metacard metacard,
            boolean isMetacardModified) {
        this.processResource = processResource;
        this.metacard = metacard;
        this.isMetacardModified = isMetacardModified;
    }

    @Override
    public Metacard getMetacard() {
        return metacard;
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
