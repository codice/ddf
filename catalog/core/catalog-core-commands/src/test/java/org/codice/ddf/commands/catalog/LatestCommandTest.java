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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import org.junit.Test;

public class LatestCommandTest extends CommandCatalogFrameworkCommon {

  /**
   * When no title is provided, output should still be displayed.
   *
   * @throws Exception
   */
  @Test
  public void testNoTitle() throws Exception {
    // given
    LatestCommand latestCommand = new LatestCommand();
    latestCommand.catalogFramework = givenCatalogFramework(getResultList("id1", "id2"));
    latestCommand.filterBuilder = new GeotoolsFilterBuilder();

    // when
    latestCommand.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString("id1"));
    assertThat(consoleOutput.getOutput(), containsString("id2"));
  }
}
