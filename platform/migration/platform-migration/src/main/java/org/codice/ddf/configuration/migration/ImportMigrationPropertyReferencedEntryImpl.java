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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property which value references another migration entry.
 */
public abstract class ImportMigrationPropertyReferencedEntryImpl extends ImportMigrationEntryImpl {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationPropertyReferencedEntryImpl.class);

  private final String property;

  private final ImportMigrationEntryImpl referenced;

  private volatile boolean verifierRegistered = false;

  ImportMigrationPropertyReferencedEntryImpl(
      ImportMigrationContextImpl context, Map<String, Object> metadata) {
    super(
        context,
        JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_REFERENCE, true),
        true);
    this.property = JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_PROPERTY, true);
    this.referenced =
        context
            .getOptionalEntry(getPath())
            .orElseThrow(
                () ->
                    new MigrationException(
                        Messages.IMPORT_METADATA_FORMAT_ERROR,
                        "referenced path [" + getName() + "] is missing"));
  }

  @Override
  public long getLastModifiedTime() {
    return referenced.getLastModifiedTime();
  }

  @Override
  public boolean isFile() {
    return referenced.isFile();
  }

  @Override
  public boolean isDirectory() {
    return referenced.isDirectory();
  }

  @Override
  public boolean restore(boolean required) {
    if (restored == null) {
      super.restored = false; // until proven otherwise in case next line throws exception
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Importing {}{}...", (required ? "required " : ""), toDebugString());
      }
      if (referenced.restore(required)) {
        super.restored = true;
        verifyPropertyAfterCompletionOnce();
      }
    }
    return restored;
  }

  @Override
  public boolean restore(boolean required, PathMatcher filter) {
    Validate.notNull(filter, "invalid null path filter");
    if (restored == null) {
      super.restored = false; // until proven otherwise
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Importing {}{} with path filter...", (required ? "required " : ""), toDebugString());
      }
      if (referenced.restore(required, filter)) {
        super.restored = true;
        // don't bother verifying if the filter didn't match!
        if (required || filter.matches(referenced.getPath())) {
          verifyPropertyAfterCompletionOnce();
        }
      }
    }
    return restored;
  }

  @Override
  public boolean restore(
      BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> consumer) {
    Validate.notNull(consumer, "invalid null consumer");
    if (restored == null) {
      super.restored = false; // until proven otherwise in case next line throws exception
      if (referenced.restore(consumer)) {
        super.restored = true;
        verifyPropertyAfterCompletionOnce();
      }
    }
    return restored;
  }

  @Override
  public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
    return referenced.getPropertyReferencedEntry(name);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + property.hashCode();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this) {
      return true;
    } else if (!super.equals(o)) {
      return false;
    } // else - they would be at least of the same class
    final ImportMigrationPropertyReferencedEntryImpl me =
        (ImportMigrationPropertyReferencedEntryImpl) o;

    return property.equals(me.getProperty());
  }

  @SuppressWarnings("squid:S2259" /* super.compareTo() will never return 0 if null is passed */)
  @Override
  public int compareTo(@Nullable MigrationEntry me) {
    if (me == this) {
      return 0;
    }
    final int c = super.compareTo(me);

    if (c != 0) {
      return c;
    } // else they would be at least of the same class
    final ImportMigrationPropertyReferencedEntryImpl ime =
        (ImportMigrationPropertyReferencedEntryImpl) me;

    return property.compareTo(ime.getProperty());
  }

  @Override
  protected Optional<InputStream> getInputStream(boolean checkAccess) throws IOException {
    final Optional<InputStream> is = referenced.getInputStream(checkAccess);

    verifyPropertyAfterCompletionOnce();
    return is;
  }

  /**
   * Called after the referenced migration entry is restored to register code to be invoked after
   * the migration operation completion to verify if the property value references the referenced
   * migration entry.
   */
  protected abstract void verifyPropertyAfterCompletion();

  @SuppressWarnings(
      "PMD.DefaultPackage" /* designed to be called from ImportMigrationContextImpl within this package and from subclasses */)
  @VisibleForTesting
  String getProperty() {
    return property;
  }

  @VisibleForTesting
  ImportMigrationEntry getReferencedEntry() {
    return referenced;
  }

  private void verifyPropertyAfterCompletionOnce() {
    if (!verifierRegistered) {
      this.verifierRegistered = true;
      verifyPropertyAfterCompletion();
    }
  }
}
