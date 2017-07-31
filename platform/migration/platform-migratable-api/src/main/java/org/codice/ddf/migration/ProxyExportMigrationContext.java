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
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * The <code>ProxyExportMigrationContext</code> class provides an implementation of the
 * {@link ExportMigrationContext} that proxies to another context.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ProxyExportMigrationContext extends ProxyMigrationContext<ExportMigrationContext>
        implements ExportMigrationContext {
    public ProxyExportMigrationContext(ExportMigrationContext proxy) {
        super(proxy);
    }

    @Override
    public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
        return proxy.getSystemPropertyReferencedEntry(name);
    }

    @Override
    public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name,
            BiPredicate<MigrationReport, String> validator) {
        return proxy.getSystemPropertyReferencedEntry(name, validator);
    }

    @Override
    public ExportMigrationEntry getEntry(Path path) {
        return proxy.getEntry(path);
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path) {
        return proxy.entries(path);
    }

    @Override
    public Stream<ExportMigrationEntry> entries(Path path, PathMatcher filter) {
        return proxy.entries(path, filter);
    }
}
