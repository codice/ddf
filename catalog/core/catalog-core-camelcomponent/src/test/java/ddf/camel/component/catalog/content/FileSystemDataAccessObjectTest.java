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
package ddf.camel.component.catalog.content;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemDataAccessObjectTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  FileSystemDataAccessObject fileSystemDataAccessObject = new FileSystemDataAccessObject();

  @Test
  public void testFileSystemPersistenceProviderHelper() throws Exception {
    File testDir = temporaryFolder.newFolder("testStore");
    String testString = "test string";

    // store the test string object
    fileSystemDataAccessObject.store(testDir.getPath(), ".test", "/example", testString);

    // assert that a file was made to store the string object
    assertThat(Files.exists(Paths.get(testDir.getPath() + "/example.test")), is(true));

    // load the string object that was stored
    assertThat(
        fileSystemDataAccessObject
            .loadFromPersistence(testDir.getPath(), ".test", "/example")
            .equals(testString),
        is(true));

    // make a filename filter to find the .test file
    FilenameFilter filenameFilter = fileSystemDataAccessObject.getFilenameFilter(".test");

    // load stored keys
    Set<String> keys =
        fileSystemDataAccessObject.loadAllKeys(testDir.getPath(), ".test", filenameFilter);

    // assert that the file that was stored was in the key set
    assertThat(keys.contains("example"), is(true));

    // clear out the stored keys
    fileSystemDataAccessObject.clear(testDir.getPath(), filenameFilter);

    // assert that the key that was stored was cleared out
    assertThat(
        fileSystemDataAccessObject.loadFromPersistence(testDir.getPath(), ".test", "/example"),
        is(nullValue()));
  }
}
