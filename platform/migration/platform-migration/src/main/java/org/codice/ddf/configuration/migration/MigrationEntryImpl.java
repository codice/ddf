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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an abstract and base implementation of the {@link MigrationEntry}.
 */
public abstract class MigrationEntryImpl<T extends MigrationContextImpl> implements MigrationEntry {
    protected static final Path DDF_HOME = Paths.get(System.getProperty("ddf.home"));

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationEntryImpl.class);

    static final String METADATA_NAME = "name";

    static final String METADATA_CHECKSUM = "checksum";

    static final String METADATA_SOFTLINK = "softlink";

    static final String METADATA_SIZE = "size";

    static final String METADATA_PROPERTY = "property";

    static final String METADATA_REFERENCE = "reference";

    protected final T context;

    protected final Path path;

    protected boolean stored = false;

    /**
     * Instantiates a new migration entry given a migratable context and entry name.
     *
     * @param context the migration context associated with this entry
     * @param path    the entry's actual path
     * @throws IllegalArgumentException if <code>context</code> or <code>path</code> is <code>null</code>
     */
    protected MigrationEntryImpl(T context, Path path) {
        Validate.notNull(context, "invalid null migration context");
        Validate.notNull(path, "invalid null entry path");
        this.context = context;
        this.path = path;
    }

    /**
     * Instantiates a new migration entry given a migratable context and entry name.
     *
     * @param context the migration context associated with this entry
     * @param name    the entry's actual name
     * @throws IllegalArgumentException if <code>context</code> or <code>name</code> is <code>null</code>
     */
    protected MigrationEntryImpl(T context, String name) {
        this(context, Paths.get(MigrationEntryImpl.sanitizeSeparators(name)));
    }

    /**
     * Instantiates a new migration entry by parsing the provided name for a migratable identifier
     * and an entry name.
     *
     * @param contextProvider a provider for migration contexts given a migratable id
     * @param fqn             a fully qualified name including the migratable id and the entry's actual name
     * @throws IllegalArgumentException if <code>contextProvider</code> or <code>fqn</code> is <code>null</code> or empty
     */
    protected MigrationEntryImpl(Function<String, T> contextProvider, String fqn) {
        Validate.notNull(contextProvider, "invalid null migration context provider");
        Validate.notEmpty(fqn, "invalid fully qualified name");
        final Path sfqn = Paths.get(MigrationEntryImpl.sanitizeSeparators(fqn));
        final int count = sfqn.getNameCount();

        if (count > 1) {
            this.context = contextProvider.apply(sfqn.getName(0)
                    .toString());
            this.path = sfqn.subpath(1, count);
        } else { // system entry
            this.context = contextProvider.apply(null);
            this.path = sfqn;
        }

    }

    /**
     * This method is used to properly convert a zip entry name into a relative path suitable for
     * the current OS. We cannot take advantage of the {@link Paths#get} method since it doesn't
     * convert occurrences of the separators inside the string parameters it receives. Further
     * more, we cannot rely on zip entry names to be defined with <code>\</code> or <code>/</code>
     * since the zip standard doesn't indicate which one to use and worst, every entries in a zip
     * file can be different.
     *
     * @param name the zip entry name to sanitize
     * @return the corresponding sanitized name
     */
    protected static String sanitizeSeparators(String name) {
        return FilenameUtils.separatorsToUnix(name);
    }

    @Override
    public MigrationReport getReport() {
        return context.getReport();
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
        return context.getId();
    }

    @Override
    public String getName() {
        return path.toString();
    }

    @Override
    public Path getPath() {
        return path;
    }

    /**
     * Gets a fully qualified name for this entry that contains both the migratable id and the entry's
     * name.
     *
     * @return a fully qualified name for this entry
     */
    public String getFQN() {
        final String id = context.getId();

        return (id != null) ? (id + File.separatorChar + getName()) : getName();
    }

    @Override
    public int hashCode() {
        return 31 * context.hashCode() + getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof MigrationEntryImpl) {
            final MigrationEntryImpl me = (MigrationEntryImpl) o;

            return context.equals(me.context) && path.equals(me.path);
        }
        return false;
    }

    @Override
    public int compareTo(MigrationEntry me) {
        if (me == this) {
            return 0;
        } else if (me == null) {
            return -1;
        }
        final int c = getName().compareTo(me.getName());

        if (c != 0) {
            return c;
        }
        final String id = getId();
        final String meid = me.getId();

        if (id == null) {
            return (meid == null) ? 0 : -1;
        }
        return id.compareTo(meid);
    }

    @Override
    public String toString() {
        return getFQN();
    }

    /**
     * Gets an absolute path based on the current distribution for this entry.
     *
     * @return an absolute path based on the current distribution
     */
    protected Path getAbsolutePath() {
        return MigrationEntryImpl.DDF_HOME.resolve(path);
    }

    T getContext() {
        return context;
    }
}
