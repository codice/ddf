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
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationEntry;

/**
 * Defines a migration entry representing a property whose value references another migration entry.
 */
public abstract class ExportMigrationPropertyReferencedEntryImpl extends ExportMigrationEntryImpl {

  private final String property;

  /**
   * Instantiates a new property referenced migration entry given a migratable context, property
   * name and pathname.
   *
   * @param context the migration context associated with this entry
   * @param property the property name for this entry
   * @param pathname the pathname for this entry
   * @throws IllegalArgumentException if <code>context</code>, <code>property</code>, or <code>
   * pathname</code> is <code>null</code>
   */
  ExportMigrationPropertyReferencedEntryImpl(
      ExportMigrationContextImpl context, String property, String pathname) {
    super(context, pathname);
    Validate.notNull(property, "invalid null property");
    this.property = property;
  }

  public String getProperty() {
    return property;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + property.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!super.equals(o)) {
      return false;
    } // else - they would be at least of the same class
    final ExportMigrationPropertyReferencedEntryImpl me =
        (ExportMigrationPropertyReferencedEntryImpl) o;

    return property.equals(me.property);
  }

  @SuppressWarnings("squid:S2259" /* the super.compareTo() will never return 0 if null is passed */)
  @Override
  public int compareTo(@Nullable MigrationEntry me) {
    final int c = super.compareTo(me);

    if (c != 0) {
      return c;
    } // else they would be at least of the same class
    final ExportMigrationPropertyReferencedEntryImpl eme =
        (ExportMigrationPropertyReferencedEntryImpl) me;

    return property.compareTo(eme.property);
  }
}
