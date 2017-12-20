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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MetacardBackupFileStorageTest {

  private static final String OUTPUT_PATH_TEMPLATE =
      File.separator + "tmp" + File.separator + "test-output" + File.separator;

  private CamelContext camelContext = new DefaultCamelContext();

  private MetacardFileStorageRoute metacardFileStorageRoute =
      new MetacardFileStorageRoute(camelContext);

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    metacardFileStorageRoute.setOutputPathTemplate(OUTPUT_PATH_TEMPLATE);
  }

  @Test
  public void testOutputPathTemplate() {
    assertThat(metacardFileStorageRoute.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
  }

  @Test
  public void testRefresh() throws Exception {
    String newBackupDir = "target" + File.separator + "temp";
    boolean backupInvalidCards = false;
    boolean keepDeletedMetacards = false;
    String metacardTransformerId = "testTransformer";

    Map<String, Object> properties = new HashMap<>();
    properties.put("outputPathTemplate", newBackupDir);
    properties.put("backupInvalidMetacards", backupInvalidCards);
    properties.put("keepDeletedMetacards", keepDeletedMetacards);
    properties.put("metacardTransformerId", metacardTransformerId);

    metacardFileStorageRoute.refresh(properties);
    assertThat(metacardFileStorageRoute.getOutputPathTemplate(), is(newBackupDir));
    assertThat(metacardFileStorageRoute.isBackupInvalidMetacards(), is(backupInvalidCards));
    assertThat(metacardFileStorageRoute.isKeepDeletedMetacards(), is(keepDeletedMetacards));
    assertThat(metacardFileStorageRoute.getMetacardTransformerId(), is(metacardTransformerId));
  }

  @Test
  public void testDeleteFile() throws IOException {
    File tempFile = temporaryFolder.newFile();
    MetacardFileStorageRoute.deleteFile(tempFile.getParent(), tempFile.getName());
    assertThat(tempFile.exists(), is(false));
  }

  @Test
  public void testRefreshBadValues() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("outputPathTemplate", 2);
    properties.put("id", 5);
    metacardFileStorageRoute.refresh(properties);
    assertThat(metacardFileStorageRoute.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
  }

  @Test
  public void testRefreshEmptyStrings() throws Exception {
    Map<String, Object> properties = new HashMap<>();
    properties.put("outputPathTemplate", "");
    metacardFileStorageRoute.refresh(properties);
    assertThat(metacardFileStorageRoute.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
  }
}
