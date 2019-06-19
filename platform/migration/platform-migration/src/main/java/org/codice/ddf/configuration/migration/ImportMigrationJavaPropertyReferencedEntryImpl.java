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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import javax.annotation.Nullable;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in a Java properties file which value
 * references another migration entry.
 */
public class ImportMigrationJavaPropertyReferencedEntryImpl
    extends ImportMigrationPropertyReferencedEntryImpl {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationJavaPropertyReferencedEntryImpl.class);

  /** Holds the path for the properties file where the reference is defined. */
  private final Path propertiesPath;

  ImportMigrationJavaPropertyReferencedEntryImpl(
      ImportMigrationContextImpl context, Map<String, Object> metadata) {
    super(context, metadata);
    this.propertiesPath =
        Paths.get(
            FilenameUtils.separatorsToSystem(
                JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_NAME, true)));
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
    if (o == this) {
      return true;
    } else if (!super.equals(o)) {
      return false;
    } // else - they would be at least of the same class
    final ImportMigrationJavaPropertyReferencedEntryImpl me =
        (ImportMigrationJavaPropertyReferencedEntryImpl) o;

    return propertiesPath.equals(me.getPropertiesPath());
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
    final ImportMigrationJavaPropertyReferencedEntryImpl ime =
        (ImportMigrationJavaPropertyReferencedEntryImpl) me;

    return propertiesPath.compareTo(ime.getPropertiesPath());
  }

  @Override
  protected String toDebugString() {
    return String.format(
        "Java property reference [%s] from [%s] as file [%s] from [%s]",
        getProperty(), propertiesPath, getAbsolutePath(), getPath());
  }

  @Override
  protected void verifyPropertyAfterCompletion() {
    final MigrationReport report = getReport();

    report.doAfterCompletion(
        r ->
            AccessUtils.doPrivileged(
                () -> {
                  LOGGER.debug("Verifying {}...", toDebugString());
                  final String val;

                  try {
                    val = getJavaPropertyValue();
                  } catch (IOException e) {
                    r.record(
                        new MigrationException(
                            Messages.IMPORT_JAVA_PROPERTY_LOAD_ERROR,
                            getProperty(),
                            propertiesPath,
                            getPath(),
                            e));
                    return;
                  }
                  if (val == null) {
                    r.record(
                        new MigrationException(
                            Messages.IMPORT_JAVA_PROPERTY_NOT_DEFINED_ERROR,
                            getProperty(),
                            propertiesPath,
                            getPath()));
                  } else if (StringUtils.isBlank(val)) {
                    r.record(
                        new MigrationException(
                            Messages.IMPORT_JAVA_PROPERTY_IS_EMPTY_ERROR,
                            getProperty(),
                            propertiesPath,
                            getPath()));
                  } else {
                    verifyReferencedFileAfterCompletion(r, val);
                  }
                }));
  }

  private void verifyReferencedFileAfterCompletion(MigrationReport r, String val) {
    try {
      if (!getAbsolutePath()
          .toRealPath(LinkOption.NOFOLLOW_LINKS)
          .equals(
              getContext()
                  .getPathUtils()
                  .resolveAgainstDDFHome(Paths.get(val))
                  .toRealPath(LinkOption.NOFOLLOW_LINKS))) {
        r.record(
            new MigrationException(
                Messages.IMPORT_JAVA_PROPERTY_ERROR,
                getProperty(),
                propertiesPath,
                getPath(),
                "is now set to [" + val + ']'));
      }
    } catch (IOException e) {
      // cannot determine the location of either so it must not exist or be
      // different anyway
      r.record(
          new MigrationException(
              Messages.IMPORT_JAVA_PROPERTY_ERROR,
              getProperty(),
              propertiesPath,
              getPath(),
              String.format("is now set to [%s]; %s", val, e.getMessage()),
              e));
    }
  }

  @SuppressWarnings({ //
    "squid:S2093", /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */
    "squid:S2095" /* stream is closed in the finally clause */
  })
  private String getJavaPropertyValue() throws IOException {
    final Properties props = new Properties();
    InputStream is = null;

    try {
      is =
          new BufferedInputStream(
              new FileInputStream(
                  getContext().getPathUtils().resolveAgainstDDFHome(propertiesPath).toFile()));
      props.load(is);
    } finally {
      IOUtils.closeQuietly(is); // we do not care if we cannot close it
    }
    return props.getProperty(getProperty());
  }
}
