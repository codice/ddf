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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.CatalogProvider;
import org.junit.Before;
import org.junit.Test;

public class DescribeCommandTest extends ConsoleOutputCommon {

  private static final String TEST_CATALOG_PROVIDER_TITLE = "test catalog provider title";

  private static final String TEST_CATALOG_PROVIDER_DESCRIPTION =
      "test catalog provider description";

  private static final String TEST_CATALOG_PROVIDER_ID = "test catalog provider id";

  private static final String TEST_CATALOG_PROVIDER_VERSION = "test catalog provider version";

  private static final String TEST_CATALOG_FRAMEWORK_TITLE = "test catalog framework title";

  private static final String TEST_CATALOG_FRAMEWORK_DESCRIPTION =
      "test catalog framework description";

  private static final String TEST_CATALOG_FRAMEWORK_ID = "test catalog framework id";

  private static final String TEST_CATALOG_FRAMEWORK_VERSION = "test catalog framework version";

  private DescribeCommand describeCommand;

  @Before
  public void setUp() throws Exception {
    describeCommand = new DescribeCommand();

    CatalogProvider catalogProvider = mock(CatalogProvider.class);
    doReturn(TEST_CATALOG_PROVIDER_TITLE).when(catalogProvider).getTitle();
    doReturn(TEST_CATALOG_PROVIDER_DESCRIPTION).when(catalogProvider).getDescription();
    doReturn(TEST_CATALOG_PROVIDER_ID).when(catalogProvider).getId();
    doReturn(TEST_CATALOG_PROVIDER_VERSION).when(catalogProvider).getVersion();
    describeCommand.catalogProvider = catalogProvider;

    CatalogFramework catalogFramework = mock(CatalogFramework.class);
    doReturn(TEST_CATALOG_FRAMEWORK_TITLE).when(catalogFramework).getTitle();
    doReturn(TEST_CATALOG_FRAMEWORK_DESCRIPTION).when(catalogFramework).getDescription();
    doReturn(TEST_CATALOG_FRAMEWORK_ID).when(catalogFramework).getId();
    doReturn(TEST_CATALOG_FRAMEWORK_VERSION).when(catalogFramework).getVersion();
    describeCommand.catalogFramework = catalogFramework;
  }

  @Test
  public void testDescribeProvider() throws Exception {
    describeCommand.isProvider = true;
    describeCommand.execute();
    assertThat(consoleOutput.getOutput(), containsString("title=" + TEST_CATALOG_PROVIDER_TITLE));
    assertThat(
        consoleOutput.getOutput(),
        containsString("description=" + TEST_CATALOG_PROVIDER_DESCRIPTION));
    assertThat(consoleOutput.getOutput(), containsString("id=" + TEST_CATALOG_PROVIDER_ID));
    assertThat(
        consoleOutput.getOutput(), containsString("version=" + TEST_CATALOG_PROVIDER_VERSION));
  }

  @Test
  public void testDescribeFramework() throws Exception {
    describeCommand.isProvider = false;
    describeCommand.execute();
    assertThat(consoleOutput.getOutput(), containsString("title=" + TEST_CATALOG_FRAMEWORK_TITLE));
    assertThat(
        consoleOutput.getOutput(),
        containsString("description=" + TEST_CATALOG_FRAMEWORK_DESCRIPTION));
    assertThat(consoleOutput.getOutput(), containsString("id=" + TEST_CATALOG_FRAMEWORK_ID));
    assertThat(
        consoleOutput.getOutput(), containsString("version=" + TEST_CATALOG_FRAMEWORK_VERSION));
  }
}
