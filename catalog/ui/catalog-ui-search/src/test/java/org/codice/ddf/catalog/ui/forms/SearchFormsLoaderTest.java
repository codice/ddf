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
package org.codice.ddf.catalog.ui.forms;

import static junit.framework.TestCase.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import ddf.catalog.data.Metacard;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateMetacardImpl;
import org.codice.ddf.catalog.ui.forms.data.ResultTemplateMetacardImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SearchFormsLoaderTest {
  private static final URL LOADER_RESOURCES_DIR =
      SearchFormsLoaderTest.class.getResource("/forms/loader");

  @Test
  public void testEmptyConfigurationDirectory() throws Exception {
    List<Metacard> metacards = new SearchFormsLoader(getConfigDirectory()).get();
    expectedCounts(metacards, 0, 0, 0);
  }

  @Test
  public void testValidConfiguration() throws Exception {
    List<Metacard> metacards = new SearchFormsLoader(getConfigDirectory("valid")).get();
    expectedCounts(metacards, 3, 2, 1);
  }

  @Test
  public void testMissingXmlLinks() throws Exception {
    List<Metacard> metacards = new SearchFormsLoader(getConfigDirectory("missing")).get();
    expectedCounts(metacards, 1, 0, 1);
  }

  @Test
  public void testInvalidStructure() throws Exception {
    List<Metacard> metacards = new SearchFormsLoader(getConfigDirectory("invalid-structure")).get();
    expectedCounts(metacards, 1, 0, 1);
  }

  @Test
  public void testInvalidEntries() throws Exception {
    List<Metacard> metacards = new SearchFormsLoader(getConfigDirectory("invalid-entries")).get();
    expectedCounts(metacards, 2, 1, 1);
  }

  private static void expectedCounts(
      List<Metacard> metacards, int total, int queryTemplates, int resultTemplates) {
    assertThat(
        "Expected total number of generated metacards to be " + total, metacards, hasSize(total));
    assertThat(
        "Expected number of generated query template metacards to be " + queryTemplates,
        metacards.stream().filter(QueryTemplateMetacardImpl::isQueryTemplateMetacard).count(),
        is((long) queryTemplates));
    assertThat(
        "Expected number of generated result template metacards to be " + resultTemplates,
        metacards.stream().filter(ResultTemplateMetacardImpl::isResultTemplateMetacard).count(),
        is((long) resultTemplates));
  }

  private static File getConfigDirectory() throws Exception {
    File dir = Paths.get(LOADER_RESOURCES_DIR.toURI()).toFile();
    if (!dir.exists()) {
      fail("Invalid setup parameter 'target', the directory does not exist");
    }
    return dir;
  }

  private static File getConfigDirectory(String target) throws Exception {
    File dir = Paths.get(LOADER_RESOURCES_DIR.toURI()).resolve(target).toFile();
    if (!dir.exists()) {
      fail("Invalid setup parameter 'target', the directory does not exist");
    }
    return dir;
  }
}
