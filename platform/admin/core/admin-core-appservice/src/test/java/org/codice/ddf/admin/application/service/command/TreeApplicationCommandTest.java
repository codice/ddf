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
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.TreeSet;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.impl.ApplicationImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationNodeImpl;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeApplicationCommandTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(TreeApplicationCommand.class);

  /**
   * Tests the {@link TreeApplicationCommand} class and its associated methods
   *
   * @throws Exception
   */
  @Test
  public void testTreeApplicationCommand() throws Exception {
    ApplicationService testAppService = mock(ApplicationServiceImpl.class);

    TreeApplicationCommand treeApplicationCommand = new TreeApplicationCommand();

    Set<ApplicationNode> treeSet = new TreeSet<>();
    ApplicationNode testNode1 = mock(ApplicationNodeImpl.class);
    ApplicationNode testNode2 = mock(ApplicationNodeImpl.class);
    treeSet.add(testNode1);
    Set<ApplicationNode> childSet = new TreeSet<>();
    childSet.add(testNode2);
    Application testApp = mock(ApplicationImpl.class);

    when(testApp.getName()).thenReturn("TestApp");
    when(testNode1.getApplication()).thenReturn(testApp);
    when(testNode2.getApplication()).thenReturn(testApp);
    when(testNode2.getChildren()).thenReturn(new TreeSet<ApplicationNode>());
    when(testNode1.getChildren()).thenReturn(childSet);
    when(testAppService.getApplicationTree()).thenReturn(treeSet);

    treeApplicationCommand.doExecute(testAppService);
    verify(testAppService).getApplicationTree();
  }
}
