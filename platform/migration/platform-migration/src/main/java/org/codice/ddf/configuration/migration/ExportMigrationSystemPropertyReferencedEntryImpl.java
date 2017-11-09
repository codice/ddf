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

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;

/**
 * Defines a migration entry representing a property defined in system properties which value
 * references another migration entry.
 */
public class ExportMigrationSystemPropertyReferencedEntryImpl
    extends ExportMigrationPropertyReferencedEntryImpl {

  /**
   * Instantiates a new system property referenced migration entry given a migratable context,
   * property name and pathname.
   *
   * @param context the migration context associated with this entry
   * @param property the property name for this entry
   * @param pathname the pathname for this entry
   * @throws IllegalArgumentException if <code>context</code>, <code>property</code>, or <code>
   * pathname</code> is <code>null</code>
   */
  ExportMigrationSystemPropertyReferencedEntryImpl(
      ExportMigrationContextImpl context, String property, String pathname) {
    super(context, property, pathname);
  }

  @Override
  protected void recordEntry() {
    getReport().recordSystemProperty(this);
  }

  @Override
  protected String toDebugString() {
    return String.format(
        "system property reference [%s] as file [%s] to [%s]",
        getProperty(), getAbsolutePath(), getPath());
  }

  @Override
  protected MigrationWarning newWarning(String reason) {
    return new MigrationWarning(
        Messages.EXPORT_SYSTEM_PROPERTY_WARNING, getProperty(), getPath(), reason);
  }

  @Override
  protected MigrationException newError(String reason, Object cause) {
    return new MigrationException(
        Messages.EXPORT_SYSTEM_PROPERTY_ERROR, getProperty(), getPath(), reason, cause);
  }
}
