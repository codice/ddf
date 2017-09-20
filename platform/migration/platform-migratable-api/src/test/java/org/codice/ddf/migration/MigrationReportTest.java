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
package org.codice.ddf.migration;

import java.util.stream.Stream;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class MigrationReportTest {
  private static final String FORMAT = "Some %s";

  private static final String ARG = "message";

  private static final String MSG = "Some message";

  private static final String[] MESSAGE_STRINGS =
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

  private static final String[] ERROR_STRINGS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("error")).toArray(String[]::new);

  private static final String[] WARNING_STRINGS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("warning")).toArray(String[]::new);

  private static final String[] INFO_STRINGS =
      Stream.of(MESSAGE_STRINGS).filter(m -> m.startsWith("info")).toArray(String[]::new);

  private final MigrationMessage[] messages = new MigrationMessage[MESSAGE_STRINGS.length];

  private final MigrationReport report =
      Mockito.mock(MigrationReport.class, Mockito.CALLS_REAL_METHODS);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() {
    int i = 0;

    for (final String msg : MESSAGE_STRINGS) {
      if (msg.startsWith("warning")) {
        messages[i++] = new MigrationWarning(msg);
      } else if (msg.startsWith("info")) {
        messages[i++] = new MigrationInformation(msg);
      } else {
        messages[i++] = new MigrationException(msg);
      }
    }

    Mockito.when(report.messages()).thenReturn(Stream.of(messages));
  }

  @Test
  public void testRecordMessage() throws Exception {
    Mockito.when(report.record(Mockito.any(MigrationMessage.class))).thenReturn(report);

    report.record(MSG);

    Mockito.verify(report).record(Mockito.eq(MSG));
  }

  @Test
  public void testRecordMessageWithNullMessage() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null message"));

    report.record((String) null);
  }

  @Test
  public void testRecordMessageWithFormat() throws Exception {
    Mockito.when(report.record(Mockito.any(MigrationMessage.class))).thenReturn(report);

    report.record(FORMAT, ARG);

    Mockito.verify(report).record(Mockito.eq(FORMAT), Mockito.eq(ARG));
  }

  @Test
  public void testRecordMessageWithNullFormat() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null format"));

    report.record(null, ARG);
  }

  @Test
  public void testErrors() throws Exception {
    final String[] emsgs =
        report.errors().map(MigrationException::getMessage).toArray(String[]::new);

    Assert.assertThat(emsgs, Matchers.arrayContaining(ERROR_STRINGS));

    Mockito.verify(report).messages();
  }

  @Test
  public void testWarnings() throws Exception {
    final String[] wmsgs =
        report.warnings().map(MigrationWarning::getMessage).toArray(String[]::new);

    Assert.assertThat(wmsgs, Matchers.arrayContaining(WARNING_STRINGS));

    Mockito.verify(report).messages();
  }

  @Test
  public void testInfos() throws Exception {
    final String[] emsgs =
        report.infos().map(MigrationInformation::getMessage).toArray(String[]::new);

    Assert.assertThat(emsgs, Matchers.arrayContaining(INFO_STRINGS));

    Mockito.verify(report).messages();
  }
}
