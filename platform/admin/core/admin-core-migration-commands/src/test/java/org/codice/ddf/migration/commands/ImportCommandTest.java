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
package org.codice.ddf.migration.commands;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ImportCommandTest extends AbstractMigrationCommandSupport {

  @Before
  public void setup() throws Exception {
    initCommand(new ImportCommand(service, security, eventAdmin, session, false, true));
  }

  @Test
  public void testConstructor() throws Exception {
    final ImportCommand command = new ImportCommand();

    Assert.assertThat(
        command.exportDirectory, Matchers.equalTo(ddfHome.resolve(MigrationCommand.EXPORTED)));
  }

  @Test
  public void testExecuteWithSubjectAndProfileDisabled() throws Exception {
    command.executeWithSubject();

    Mockito.verify(service)
        .doImport(
            Mockito.eq(exportedPath), Mockito.same(Collections.emptySet()), Mockito.notNull());
    verifyPostedEvent("import");
  }

  @Test
  public void testExecuteWithSubjectAndProfileEnabled() throws Exception {
    initCommand(new ImportCommand(service, security, eventAdmin, session, true, true));

    command.executeWithSubject();

    final ArgumentCaptor<Set<String>> mandatoryMigratables = ArgumentCaptor.forClass(Set.class);

    Mockito.verify(service)
        .doImport(Mockito.eq(exportedPath), mandatoryMigratables.capture(), Mockito.notNull());
    verifyPostedEvent("import");
    Assert.assertThat(mandatoryMigratables.getValue(), Matchers.contains("ddf.profile"));
  }

  @Test
  public void testConfirmWarning() throws Exception {
    when(session.readLine(anyString(), isNull(Character.class))).thenReturn("yes");

    initCommand(new ImportCommand(service, security, eventAdmin, session, false, false));
    command.executeWithSubject();

    Mockito.verify(service)
        .doImport(
            Mockito.eq(exportedPath), Mockito.same(Collections.emptySet()), Mockito.notNull());
    verifyPostedEvent("import");
  }

  @Test
  public void testCancelAfterWarning() throws Exception {
    when(session.readLine(anyString(), isNull(Character.class))).thenReturn("no");

    initCommand(new ImportCommand(service, security, eventAdmin, session, false, false));
    command.executeWithSubject();

    verifyZeroInteractions(service);
  }

  @Test
  public void testInvalidInputWarning() throws Exception {
    when(session.readLine(anyString(), isNull(Character.class))).thenReturn("not yes or no");

    initCommand(new ImportCommand(service, security, eventAdmin, session, false, false));
    command.executeWithSubject();

    verifyZeroInteractions(service);
  }
}
