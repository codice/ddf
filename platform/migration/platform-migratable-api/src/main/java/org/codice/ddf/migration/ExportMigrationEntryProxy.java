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
import java.io.OutputStream;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.codice.ddf.util.function.EBiConsumer;

/**
 * The <code>ExportMigrationEntryProxy</code> class provides an implementation of the
 * {@link ExportMigrationEntry} that proxies to another entry.
 * <p>
 * <b>
 * This code is experimental. While this class is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ExportMigrationEntryProxy extends MigrationEntryProxy<ExportMigrationEntry>
        implements ExportMigrationEntry {
    public ExportMigrationEntryProxy(ExportMigrationEntry proxy) {
        super(proxy);
    }

    @Override
    public Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name) {
        return proxy.getPropertyReferencedEntry(name);
    }

    @Override
    public Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator) {
        return proxy.getPropertyReferencedEntry(name, validator);
    }

    @Override
    public boolean store() {
        return proxy.store();
    }

    @Override
    public boolean store(boolean required) {
        return proxy.store(required);
    }

    @Override
    public boolean store(EBiConsumer<MigrationReport, OutputStream, IOException> consumer) {
        return proxy.store(consumer);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return proxy.getOutputStream();
    }
}
