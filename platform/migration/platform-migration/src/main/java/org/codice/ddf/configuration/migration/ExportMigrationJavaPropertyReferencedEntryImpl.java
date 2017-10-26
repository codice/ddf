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

import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;

/**
 * Defines a migration entry representing a property defined in a Java properties file whose value
 * references another migration entry.
 */
public class ExportMigrationJavaPropertyReferencedEntryImpl
    extends ExportMigrationPropertyReferencedEntryImpl {

  /** Holds the path for the properties file where the reference is defined. */
  private final Path propertiesPath;

  /**
   * Instantiates a new java property referenced migration entry given a migratable context,
   * properties path, name, and pathname.
   *
   * @param context the migration context associated with this entry
   * @param propertiesPath the path to the Java property file
   * @param property the property name for this entry
   * @param pathname the pathname for this entry
   * @throws IllegalArgumentException if <code>context</code>, <code>propertiesPath</code>, <code>
   * property</code>, or <code>pathname</code> is <code>null</code>
   */
  ExportMigrationJavaPropertyReferencedEntryImpl(
      ExportMigrationContextImpl context, Path propertiesPath, String property, String pathname) {
    super(context, property, pathname);
    Validate.notNull(propertiesPath, "invalid null properties path");
    this.propertiesPath = propertiesPath;
  }

  public Path getPropertiesPath() {
    return propertiesPath;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + propertiesPath.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!super.equals(o)) {
      return false;
    } // else - they would be at least of the same class
    final ExportMigrationJavaPropertyReferencedEntryImpl me =
        (ExportMigrationJavaPropertyReferencedEntryImpl) o;

    return propertiesPath.equals(me.propertiesPath);
  }

  @SuppressWarnings("squid:S2259" /* the super.compareTo() will never return 0 if null is passed */)
  @Override
  public int compareTo(@Nullable MigrationEntry me) {
    final int c = super.compareTo(me);

    if (c != 0) {
      return c;
    } // else they would be at least of the same class
    final ExportMigrationJavaPropertyReferencedEntryImpl eme =
        (ExportMigrationJavaPropertyReferencedEntryImpl) me;

    return propertiesPath.compareTo(eme.propertiesPath);
  }

  @Override
  protected void recordEntry() {
    getReport().recordJavaProperty(this);
  }

  @Override
  protected String toDebugString() {
    return String.format(
        "Java property reference [%s] from [%s] as file [%s] to [%s]",
        getProperty(), propertiesPath, getAbsolutePath(), getPath());
  }

  @Override
  protected MigrationWarning newWarning(String reason) {
    return new MigrationWarning(
        Messages.EXPORT_JAVA_PROPERTY_WARNING, getProperty(), propertiesPath, getPath(), reason);
  }

  @Override
  protected MigrationException newError(String reason, Object cause) {
    return new MigrationException(
        Messages.EXPORT_JAVA_PROPERTY_ERROR,
        getProperty(),
        propertiesPath,
        getPath(),
        reason,
        cause);
  }
}
