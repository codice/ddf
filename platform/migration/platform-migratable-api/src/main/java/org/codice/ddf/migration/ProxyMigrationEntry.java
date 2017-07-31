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

import java.nio.file.Path;

import org.apache.commons.lang.Validate;

/**
 * The <code>ProxyMigrationEntry</code> class provides an implementation of the
 * {@link MigrationEntry} that proxies to another entry.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 * <p>
 * @param <T> the type of migration entry being proxied
 */
public class ProxyMigrationEntry<T extends MigrationEntry> implements MigrationEntry {
    protected final T proxy;

    public ProxyMigrationEntry(T proxy) {
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

    @Override
    public String getName() {
        return proxy.getName();
    }

    @Override
    public Path getPath() {
        return proxy.getPath();
    }

    @Override
    public long getLastModifiedTime() {
        return proxy.getLastModifiedTime();
    }

    @Override
    public long getSize() {
        return proxy.getSize();
    }

    @Override
    public void store() {
        proxy.store();
    }

    @Override
    public int hashCode() {
        return proxy.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ProxyMigrationEntry) {
            return proxy.equals(((ProxyMigrationEntry)obj).proxy);
        }
        return proxy.equals(obj);
    }

    @Override
    public String toString() {
        return proxy.toString();
    }

    @Override
    public int compareTo(MigrationEntry o) {
        if (o instanceof ProxyMigrationEntry) {
            return proxy.compareTo(((ProxyMigrationEntry)o).proxy);
        }
        return proxy.compareTo(o);
    }
}
