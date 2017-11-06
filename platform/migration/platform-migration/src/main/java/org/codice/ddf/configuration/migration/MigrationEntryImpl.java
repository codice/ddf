/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import javax.annotation.Nullable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationReport;

/** This class provides an abstract and base implementation of the {@link MigrationEntry}. */
public abstract class MigrationEntryImpl implements MigrationEntry {

  public static final String METADATA_NAME = "name";

  public static final String METADATA_FOLDER = "folder";

  public static final String METADATA_SOFTLINK = "softlink";

  public static final String METADATA_PROPERTY = "property";

  public static final String METADATA_REFERENCE = "reference";

  public static final String METADATA_CHECKSUM = "checksum";

  public static final String METADATA_FILTERED = "filtered";

  public static final String METADATA_LAST_MODIFIED = "last-modified";

  public static final String METADATA_FILES = "files";

  protected MigrationEntryImpl() {}

  @Override
  public MigrationReport getReport() {
    return getContext().getReport();
  }

  /**
   * Gets the identifier for the {@link org.codice.ddf.migration.Migratable} service responsible for
   * this entry.
   *
   * @return the responsible migratable service id or <code>null</code> if this is an entry defined
   *     by the migration framework (e.g. Version.txt)
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
    } else if (o == null) {
      return false;
    } else if (getClass().equals(o.getClass())) {
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
    int c = getName().compareTo(me.getName());

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
    c = id.compareTo(meid);
    if (c != 0) {
      return c;
    }
    return getClass().getName().compareTo(me.getClass().getName());
  }

  @Override
  public String toString() {
    final String id = getId();

    return (id != null) ? (id + '@' + getName()) : getName();
  }

  protected abstract MigrationContext getContext();
}
