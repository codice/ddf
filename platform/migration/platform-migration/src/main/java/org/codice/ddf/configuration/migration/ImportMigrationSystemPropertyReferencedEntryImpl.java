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

import java.io.IOException;
import java.nio.file.LinkOption;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in system properties which value
 * references another migration entry.
 */
public class ImportMigrationSystemPropertyReferencedEntryImpl
    extends ImportMigrationPropertyReferencedEntryImpl {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ImportMigrationSystemPropertyReferencedEntryImpl.class);

  ImportMigrationSystemPropertyReferencedEntryImpl(
      ImportMigrationContextImpl context, Map<String, Object> metadata) {
    super(context, metadata);
  }

  @Override
  protected String toDebugString() {
    return String.format(
        "system property reference [%s] as file [%s] from [%s]",
        getProperty(), getAbsolutePath(), getPath());
  }

  @Override
  protected void verifyPropertyAfterCompletion() {
    final MigrationReport report = getReport();

    report.doAfterCompletion(
        r ->
            AccessUtils.doPrivileged(
                () -> {
                  LOGGER.debug("Verifying {}...", toDebugString());
                  final String val = System.getProperty(getProperty());

                  if (val == null) {
                    r.record(
                        new MigrationException(
                            Messages.IMPORT_SYSTEM_PROPERTY_NOT_DEFINED_ERROR,
                            getProperty(),
                            getPath()));
                  } else if (StringUtils.isBlank(val)) {
                    r.record(
                        new MigrationException(
                            Messages.IMPORT_SYSTEM_PROPERTY_IS_EMPTY_ERROR,
                            getProperty(),
                            getPath()));
                  } else {
                    try {
                      if (!getAbsolutePath()
                          .toRealPath(LinkOption.NOFOLLOW_LINKS)
                          .equals(
                              getContext()
                                  .getPathUtils()
                                  .resolveAgainstDDFHome(val)
                                  .toRealPath(LinkOption.NOFOLLOW_LINKS))) {
                        r.record(
                            new MigrationException(
                                Messages.IMPORT_SYSTEM_PROPERTY_ERROR,
                                getProperty(),
                                getPath(),
                                "is now set to [" + val + ']'));
                      }
                    } catch (IOException e) {
                      // cannot determine the location of either so it must not exist or be
                      // different anyway
                      r.record(
                          new MigrationException(
                              Messages.IMPORT_SYSTEM_PROPERTY_ERROR,
                              getProperty(),
                              getPath(),
                              String.format("is now set to [%s]; %s", val, e.getMessage()),
                              e));
                    }
                  }
                }));
  }
}
