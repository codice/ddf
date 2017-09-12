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
package org.codice.ddf.migration;

import org.apache.commons.lang.Validate;

/**
 * The <code>MigrationContextProxy</code> class provides an implementation of the
 * {@link MigrationContext} that proxies to another context.
 * <p>
 * <b>
 * This code is experimental. While this class is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 *
 * @param <T> the type of migration context being proxied
 */
public class MigrationContextProxy<T extends MigrationContext> implements MigrationContext {
    protected final T proxy;

    public MigrationContextProxy(T proxy) {
        Validate.notNull(proxy, "invalid null proxy");
        this.proxy = proxy;
    }

    @Override
    public MigrationReport getReport() {
        return proxy.getReport();
    }

    @Override
    public String getId() {
        return proxy.getId();
    }
}
