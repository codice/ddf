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

import java.util.Optional;
import java.util.stream.Stream;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;

/** Base class for report-type test cases which handles setup of various messages. */
public class AbstractMigrationReportSupport extends AbstractMigrationSupport {
  protected static final String[] MESSAGE_STRINGS =
      new String[] {
        "warning1",
        "info2",
        "error3",
        "info4",
        "info5",
        "warning6",
        "error7",
        "error8",
        "warning9",
        "warning10",
        "info11"
      };

  protected static final String[] POTENTIAL_WARNING_MESSAGE_STRINGS =
      Stream.of(MESSAGE_STRINGS).filter(m -> !m.startsWith("info")).toArray(String[]::new);

  protected static final String[] ERRORS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("error")).toArray(String[]::new);

  protected static final String[] WARNINGS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("warning")).toArray(String[]::new);

  protected static final String[] INFOS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("info")).toArray(String[]::new);

  protected final MigrationReportImpl report;

  protected final MigrationException[] exceptions = new MigrationException[ERRORS.length];

  protected AbstractMigrationReportSupport(MigrationOperation operation) {
    this.report = new MigrationReportImpl(operation, Optional.empty());
  }

  @Before
  public void baseReportSetup() {
    int i = 0;

    for (final String msg : MESSAGE_STRINGS) {
      if (msg.startsWith("info")) {
        report.record(msg);
      } else if (msg.startsWith("warning")) {
        report.record(new MigrationWarning(msg));
      } else {
        final MigrationException e = new MigrationException(msg);

        exceptions[i++] = e;
        report.record(e);
      }
    }
  }
}
