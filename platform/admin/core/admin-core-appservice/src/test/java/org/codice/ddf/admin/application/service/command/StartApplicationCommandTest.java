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
package org.codice.ddf.admin.application.service.command;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Test;

public class StartApplicationCommandTest {

  private static final String APP_NAME = "TestApp";

  /**
   * Tests the {@link StartApplicationCommand} class and its associated methods
   *
   * @throws Exception
   */
  @Test
  public void testStartApplicationCommand() throws Exception {
    ApplicationService testAppService = mock(ApplicationServiceImpl.class);

    StartApplicationCommand startApplicationCommand = new StartApplicationCommand();
    startApplicationCommand.appName = APP_NAME;

    startApplicationCommand.doExecute(testAppService);
    verify(testAppService).startApplication(APP_NAME);
  }
}
