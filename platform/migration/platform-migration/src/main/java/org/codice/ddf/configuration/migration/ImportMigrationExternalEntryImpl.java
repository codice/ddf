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
package org.codice.ddf.configuration.migration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.ImportPathMigrationWarning;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the {@link org.codice.ddf.migration.ImportMigrationEntry}
 * representing an external file that was exported.
 */
public class ImportMigrationExternalEntryImpl extends ImportMigrationEntryImpl {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ImportMigrationExternalEntryImpl.class);

    private final String checksum;

    private final boolean softlink;

    ImportMigrationExternalEntryImpl(ImportMigrationContextImpl context,
            Map<String, Object> metadata) {
        super(context, JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_NAME, true));
        this.checksum = JsonUtils.getStringFrom(metadata,
                MigrationEntryImpl.METADATA_CHECKSUM,
                false);
        this.softlink = JsonUtils.getBooleanFrom(metadata,
                MigrationEntryImpl.METADATA_SOFTLINK,
                false);
    }

    @Override
    public long getLastModifiedTime() {
        return -1L;
    }

    @Override
    public Optional<InputStream> getInputStream() throws IOException {
        return Optional.empty();
    }

    @Override
    public boolean store(boolean required) {
        if (stored == null) {
            super.stored =
                    false; // until proven otherwise in case the next line throws an exception
            LOGGER.debug("Verifying {}{}...", (required ? "required " : ""), toDebugString());
            super.stored = verifyRealFile(required);
        }
        return stored;
    }

    protected String toDebugString() {
        return String.format("external file [%s] from [%s]", getAbsolutePath(), getPath());
    }

    /**
     * Verifies the corresponding existing file to see if it matches the original one based on the
     * exported info.
     *
     * @param required <code>true</code> if the file was required to be exported; <code>false</code>
     *                 if it was optional
     * @return <code>false</code> if an error was detected during verification; <code>true</code>
     * otherwise
     */
    private boolean verifyRealFile(boolean required) {
        final MigrationReport report = getReport();
        final Path apath = getAbsolutePath();
        final File file = getFile();

        if (!file.exists()) {
            if (required) {
                report.record(new ImportPathMigrationException(apath, "doesn't exist"));
                return false;
            }
            return true;
        }
        if (softlink) {
            if (!Files.isSymbolicLink(getAbsolutePath())) {
                report.record(new ImportPathMigrationWarning(apath, "is not a symbolic link"));
                return false;
            }
        } else if (!Files.isRegularFile(getAbsolutePath(), LinkOption.NOFOLLOW_LINKS)) {
            report.record(new ImportPathMigrationWarning(apath, "is not a regular file"));
        }
        if (checksum != null) {
            try {
                final String rchecksum = getContext().getPathUtils()
                        .getChecksumFor(getAbsolutePath());

                if (!rchecksum.equals(checksum)) {
                    report.record(new ImportPathMigrationWarning(apath,
                            String.format(
                                    "checksum doesn't match the original; expecting '%s' but was '%s'",
                                    checksum,
                                    rchecksum)));
                    return false;
                }
            } catch (IOException e) {
                LOGGER.info("failed to compute MD5 checksum for '" + getName() + "': ", e);
                report.record(new ImportPathMigrationWarning(apath,
                        "checksum could not be calculated; " + e.getMessage()));
                return false;
            }
        }
        return true;
    }
}
