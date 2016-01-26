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
package org.codice.ddf.catalog.migratable.impl;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import ddf.catalog.data.Result;

class CatalogWriterCallable implements Callable<Void> {
    private final File exportFile;

    private final List<Result> fileResults;

    private final MigrationFileWriter fileWriter;

    public CatalogWriterCallable(final File exportFile, final List<Result> fileResults,
            final MigrationFileWriter fileWriter) {
        this.exportFile = exportFile;
        this.fileResults = fileResults;
        this.fileWriter = fileWriter;
    }

    @Override
    public Void call() throws Exception {
        fileWriter.writeMetacards(exportFile, fileResults);
        return null;
    }

    @Override
    public int hashCode() {
        int result = exportFile.hashCode();
        result = 31 * result + fileResults.hashCode();
        result = 31 * result + fileWriter.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CatalogWriterCallable) {
            CatalogWriterCallable comparer = (CatalogWriterCallable) obj;
            if (this.exportFile.equals(comparer.exportFile) &&
                    this.fileWriter.equals(comparer.fileWriter) &&
                    this.fileResults.equals(comparer.fileResults)) {
                return true;
            }
        }
        return false;
    }
}
