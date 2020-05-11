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
package org.codice.ddf.commands.spatial.gazetteer;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.SourceUnavailableException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GazetteerBuildSuggesterIndexCommandTest {
  @Mock private CatalogFramework catalogFramework;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder filterBuilder;

  private ConsoleInterceptor consoleInterceptor;

  private GazetteerBuildSuggesterIndexCommand command;

  @Before
  public void setUp() {
    consoleInterceptor = new ConsoleInterceptor();
    consoleInterceptor.interceptSystemOut();
    command = new GazetteerBuildSuggesterIndexCommand();
    command.setFilterBuilder(filterBuilder);
    command.setCatalogFramework(catalogFramework);
  }

  @After
  public void tearDown() {
    consoleInterceptor.resetSystemOut();
  }

  @Test
  public void successfulRunPrintsNoErrors() {
    command.executeWithSubject();

    final String output = consoleInterceptor.getOutput();
    assertThat(output, not(containsString("Error:")));
  }

  @Test
  public void unsuccessfulRunPrintsErrors() throws Exception {
    final String exceptionMessage = "The source is unavailable.";
    doThrow(new SourceUnavailableException(exceptionMessage)).when(catalogFramework).query(any());

    command.executeWithSubject();

    final String output = consoleInterceptor.getOutput();
    assertThat(output, containsString("Error: " + exceptionMessage));
  }
}
