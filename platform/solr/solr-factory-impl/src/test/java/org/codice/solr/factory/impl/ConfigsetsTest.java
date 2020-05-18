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
package org.codice.solr.factory.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ConfigsetsTest {

  private static final String TEST_COLLECTION_NAME = "test";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  public File tempLocation;

  private Configsets configsets;

  @Before
  public void beforeTest() throws Exception {
    tempLocation = tempFolder.newFolder();
    configsets = new Configsets(tempLocation.toPath());
  }

  @Test
  public void writingDefaultToDisk() {
    File defaultConf = tempLocation.toPath().resolve(Paths.get("default", "conf")).toFile();
    assertThat(defaultConf.exists(), is(true));
    assertThat(defaultConf.listFiles().length, is(8));
  }

  @Ignore
  @Test
  public void getMissingCollectionButGetDefaultInstead() {
    Path collectionLocation = configsets.get(TEST_COLLECTION_NAME);
    assertThat(
        collectionLocation.toString(),
        endsWith(File.separator + "default" + File.separator + "conf"));
  }

  @Ignore
  @Test
  public void getCollection() {
    tempLocation.toPath().resolve(Paths.get(TEST_COLLECTION_NAME, "conf")).toFile().mkdirs();
    Path collectionLocation = configsets.get(TEST_COLLECTION_NAME);
    assertThat(
        collectionLocation.toString(),
        endsWith(File.separator + TEST_COLLECTION_NAME + File.separator + "conf"));
  }
}
