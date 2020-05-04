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
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CollectionConfigTest {

  private static final String TEST_COLLECTION_NAME = "test";

  private static final Path CONFIGSETS_PATH = Paths.get("src", "test", "resources", "configsets");

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private CollectionConfig collectionConfig;

  @Test
  public void getMissingCollectionButGetDefaultInstead() {
    collectionConfig = new CollectionConfig("DoesNotExist", CONFIGSETS_PATH);
    assertThat(collectionConfig.getShardCount(), is(1));
    assertThat(collectionConfig.getReplicationFactor(), is(1));
    assertThat(collectionConfig.getMaximumShardsPerNode(), is(1));
  }

  @Test
  public void getMissingCollectionButGetSystemPropertyDefaultsInstead() throws IOException {
    File tempLocation = tempFolder.newFolder();
    collectionConfig = new CollectionConfig(TEST_COLLECTION_NAME, tempLocation.toPath());
    assertThat(collectionConfig.getShardCount(), is(2));
    assertThat(collectionConfig.getReplicationFactor(), is(2));
    assertThat(collectionConfig.getMaximumShardsPerNode(), is(2));
  }

  @Test
  public void getCollection() {
    collectionConfig = new CollectionConfig(TEST_COLLECTION_NAME, CONFIGSETS_PATH);
    assertThat(collectionConfig.getShardCount(), is(3));
    assertThat(collectionConfig.getReplicationFactor(), is(3));
    assertThat(collectionConfig.getMaximumShardsPerNode(), is(3));
  }
}
