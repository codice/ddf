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

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.codice.ddf.util.function.EBiConsumer;

/**
 * The <code>ImportMigrationEntryProxy</code> class provides an implementation of the
 * {@link ImportMigrationEntry} that proxies to another entry.
 * <p>
 * <b>
 * This code is experimental. While this class is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ImportMigrationEntryProxy extends MigrationEntryProxy<ImportMigrationEntry>
        implements ImportMigrationEntry {
    public ImportMigrationEntryProxy(ImportMigrationEntry proxy) {
        super(proxy);
    }

    @Override
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
        return proxy.getPropertyReferencedEntry(name);
    }

    @Override
    public boolean restore() {
        return proxy.restore();
    }

    @Override
    public boolean restore(boolean required) {
        return proxy.restore(required);
    }

    @Override
    public boolean restore(
            EBiConsumer<MigrationReport, Optional<InputStream>, IOException> consumer) {
        return proxy.restore(consumer);
    }

    @Override
    public Optional<InputStream> getInputStream() throws IOException {
        return proxy.getInputStream();
    }
}
