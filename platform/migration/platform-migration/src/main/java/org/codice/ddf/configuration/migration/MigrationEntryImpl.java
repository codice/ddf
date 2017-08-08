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

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an abstract and base implementation of the {@link MigrationEntry}.
 */
public abstract class MigrationEntryImpl implements MigrationEntry {
    static final String METADATA_NAME = "name";

    static final String METADATA_CHECKSUM = "checksum";

    static final String METADATA_SOFTLINK = "softlink";

    static final String METADATA_SIZE = "size";

    static final String METADATA_PROPERTY = "property";

    static final String METADATA_REFERENCE = "reference";

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationEntryImpl.class);

    protected boolean stored = false;

    protected MigrationEntryImpl() {}

    /**
     * This method is used to properly convert a zip entry name into a relative path suitable for
     * the current OS. We cannot take advantage of the {@link java.nio.file.Paths#get} method since
     * it doesn't convert occurrences of the separators inside the string parameters it receives.
     * Further more, we cannot rely on zip entry names to be defined with <code>\</code> or
     * <code>/</code> since the zip standard doesn't indicate which one to use and worst, every
     * entries in a zip file can be different.
     *
     * @param name the zip entry name to sanitize
     * @return the corresponding sanitized name
     */
    protected static String sanitizeSeparators(String name) {
        return FilenameUtils.separatorsToUnix(name);
    }

    @Override
    public MigrationReport getReport() {
        return getContext().getReport();
    }

    /**
     * Gets the identifier for the {@link Migratable} service responsible for this entry.
     *
     * @return the responsible migratable service id or <code>null</code> if this is an entry defined
     * by the migration framework (e.g. Version.txt)
     */
    @Override
    @Nullable
    public String getId() {
        return getContext().getId();
    }

    @Override
    public int hashCode() {
        return 31 * getContext().hashCode() + getName().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof MigrationEntryImpl) {
            final MigrationEntryImpl me = (MigrationEntryImpl) o;

            return getContext().equals(me.getContext()) && getPath().equals(me.getPath());
        }
        return false;
    }

    @Override
    public int compareTo(@Nullable MigrationEntry me) {
        if (me == this) {
            return 0;
        } else if (me == null) {
            return 1;
        }
        final int c = getName().compareTo(me.getName());

        if (c != 0) {
            return c;
        }
        final String id = getId();
        final String meid = me.getId();

        if (id == null) {
            return (meid == null) ? 0 : -1;
        } else if (meid == null) {
            return 1;
        }
        return id.compareTo(meid);
    }

    @Override
    public String toString() {
        final String id = getId();

        return (id != null) ? (id + '@' + getName()) : getName();
    }

    protected abstract MigrationContext getContext();
}
