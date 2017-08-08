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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportPathMigrationWarning;
import org.codice.ddf.migration.MigrationImporter;
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

    private final Long size;

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
        this.size = JsonUtils.getLongFrom(metadata, MigrationEntryImpl.METADATA_SIZE, false);
    }

    @Override
    public long getLastModifiedTime() {
        return -1L;
    }

    @Override
    public long getSize() {
        return (size == null) ? -1L : size;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(getFile()));
    }

    @Override
    public void store() {
        if (!stored) {
            final Path apath = getAbsolutePath();

            LOGGER.debug("Verifying external file [{}] from [{}]...", apath, getPath());
            super.stored = true;
            verifyRealFile(r -> new ImportPathMigrationWarning(apath, r));
        }
    }

    @Override
    public void store(MigrationImporter importer) {
        Validate.notNull(importer, "invalid null importer");
        store();
    }

    /**
     * Verifies the corresponding existing file to see if it matches the original one based on the
     * exported info.
     *
     * @param builder a function used to generate a warning which will receive the reason for the warning
     */
    private void verifyRealFile(Function<String, ImportPathMigrationWarning> builder) {
        final MigrationReport report = getReport();
        final File file = getFile();

        if (!file.exists()) {
            report.record(builder.apply("doesn't exist"));
            return;
        }
        if ((size != null) && (size == 0L) && file.exists()) {
            report.record(builder.apply("exists when it shouldn't"));
        } else if (softlink) {
            if (!Files.isSymbolicLink(getAbsolutePath())) {
                report.record(builder.apply("is not a symbolic link"));
            }
        } else if (!Files.isRegularFile(getAbsolutePath())) {
            report.record(builder.apply("is not a regular file"));
        }
        final long rsize = file.length();

        if ((size != null) && (size != -1L) && (size != rsize)) {
            report.record(builder.apply(String.format(
                    "length doesn't match the original; expecting %d bytes but was %d bytes",
                    size,
                    rsize)));
        }
        if (checksum != null) {
            InputStream is = null;

            try {
                is = new FileInputStream(file);
                final String rchecksum = DigestUtils.md5Hex(is);

                if (rchecksum.equals(checksum)) {
                    report.record(builder.apply(String.format(
                            "checksum doesn't match the original; expecting '%s' but was '%s'",
                            checksum,
                            rchecksum)));
                }
            } catch (IOException e) {
                LOGGER.info("failed to compute MD5 checksum for '" + getName() + "': ", e);
                report.record(builder.apply("checksum could not be calculated; " + e.getMessage()));
            } finally {
                IOUtils.closeQuietly(is); // don't care about errors when closing
            }
        }
    }
}
