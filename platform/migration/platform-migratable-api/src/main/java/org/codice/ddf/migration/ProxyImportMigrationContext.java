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
import java.util.stream.Stream;

/**
 * The <code>ProxyImportMigrationContext</code> class provides an implementation of the
 * {@link ImportMigrationContext} that proxies to another context.
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public class ProxyImportMigrationContext extends ProxyMigrationContext<ImportMigrationContext>
        implements ImportMigrationContext {
    public ProxyImportMigrationContext(ImportMigrationContext proxy) {
        super(proxy);
    }

    @Override
    public Optional<ImportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
        return proxy.getSystemPropertyReferencedEntry(name);
    }

    @Override
    public ImportMigrationEntry getEntry(Path path) {
        return proxy.getEntry(path);
    }

    @Override
    public Stream<ImportMigrationEntry> entries() {
        return proxy.entries();
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path) {
        return proxy.entries(path);
    }

    @Override
    public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter) {
        return proxy.entries(path, filter);
    }

    public boolean cleanDirectory(Path path) {
        return proxy.cleanDirectory(path);
    }
}
