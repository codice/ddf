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
package org.codice.ddf.platform.util.properties;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PropertiesFileReaderTest {
  @ClassRule public static TemporaryFolder testFolder = new TemporaryFolder();

  private static final Path NONEXISTENT_PATH = Paths.get("this", "path");

  private static final int VALUES_PER_FILE = 4;

  private static String propertiesTestDirectoryPath;

  private static String propertiesTestEmptyDirectoryPath;

  private static String propertiesTestSingleFilePath;

  private PropertiesFileReader reader = new PropertiesFileReader();

  @BeforeClass
  public static void setup() throws Exception {
    File propertiesEmptyDirectory = testFolder.newFolder("empty");
    File propertiesTestDirectory = testFolder.newFolder("properties");

    propertiesTestEmptyDirectoryPath = propertiesEmptyDirectory.getPath();
    propertiesTestDirectoryPath = propertiesTestDirectory.getPath();
    propertiesTestSingleFilePath =
        Paths.get(propertiesTestDirectoryPath, "test1.properties").toString();

    File propsFile1 = new File(propertiesTestDirectory, "test1.properties");
    File propsFile2 = new File(propertiesTestDirectory, "test2.properties");
    File propsFile3 = new File(propertiesTestDirectory, "test3.properties");

    File randomFile = new File(propertiesTestDirectory, "random.cfg");
    assert randomFile.createNewFile();

    generateAndSaveProperties(propsFile1, "test1value", VALUES_PER_FILE);
    generateAndSaveProperties(propsFile2, "test2value", VALUES_PER_FILE);
    generateAndSaveProperties(propsFile3, "test3value", VALUES_PER_FILE);
  }

  @Test
  public void testLoadDirectoryNullPath() throws Exception {
    List<Map<String, String>> resultsDirectory = reader.loadPropertiesFilesInDirectory(null);
    assertEmpty(resultsDirectory);
  }

  @Test
  public void testLoadFileNullPath() throws Exception {
    Map<String, String> resultsOneFile = reader.loadSinglePropertiesFile(null);
    assertEmpty(resultsOneFile);
  }

  @Test
  public void testLoadDirectoryEmptyStringForPath() throws Exception {
    List<Map<String, String>> resultsDirectory = reader.loadPropertiesFilesInDirectory("");
    assertEmpty(resultsDirectory);
  }

  @Test
  public void testLoadFileEmptyStringForPath() throws Exception {
    Map<String, String> resultsOneFile = reader.loadSinglePropertiesFile("");
    assertEmpty(resultsOneFile);
  }

  @Test
  public void testLoadDirectoryNonExistentPath() throws Exception {
    List<Map<String, String>> resultsDirectory =
        reader.loadPropertiesFilesInDirectory(NONEXISTENT_PATH.toString());
    assertEmpty(resultsDirectory);
  }

  @Test
  public void testLoadFileNonExistentPath() throws Exception {
    Map<String, String> resultsOneFile =
        reader.loadSinglePropertiesFile(NONEXISTENT_PATH.toString());
    assertEmpty(resultsOneFile);
  }

  @Test
  public void testLoadPropertiesFilesInDirectory() throws Exception {
    List<Map<String, String>> resultsDirectory =
        reader.loadPropertiesFilesInDirectory(propertiesTestDirectoryPath);
    validateAllProps(resultsDirectory);
  }

  @Test
  public void testLoadPropertiesFilesInEmptyDirectory() throws Exception {
    List<Map<String, String>> resultsDirectory =
        reader.loadPropertiesFilesInDirectory(propertiesTestEmptyDirectoryPath);
    assertEmpty(resultsDirectory);
  }

  @Test
  public void testLoadPropertiesFileInDirectoryGivenFileNotDirectory() throws Exception {
    List<Map<String, String>> resultsDirectory =
        reader.loadPropertiesFilesInDirectory(propertiesTestSingleFilePath);
    assertEmpty(resultsDirectory);
  }

  @Test
  public void testLoadSinglePropertiesFile() throws Exception {
    Map<String, String> resultsOneFile =
        reader.loadSinglePropertiesFile(propertiesTestSingleFilePath);
    validateSinglePropertiesFile(resultsOneFile, 1);
  }

  @Test
  public void testLoadSinglePropertiesFileGivenDirectoryNotFile() throws Exception {
    Map<String, String> resultsOneFile =
        reader.loadSinglePropertiesFile(propertiesTestDirectoryPath);
    assertEmpty(resultsOneFile);
  }

  private static void generateAndSaveProperties(File destination, String valuePrefix, int quantity)
      throws IOException {
    Properties properties = new Properties();
    for (int i = 1; i <= quantity; i++) {
      properties.put(format("%s%d", "key", i), format("%s%d", valuePrefix, i));
    }
    properties.store(new FileWriter(destination), "For testing purposes");
  }

  private static void assertEmpty(Map<String, String> map) {
    assertThat(
        "File targeted for property loading should NOT have produced results",
        map.entrySet(),
        hasSize(0));
  }

  private static void assertEmpty(List<Map<String, String>> list) {
    assertThat(
        "Directory targeted for property loading should NOT have produced results",
        list,
        hasSize(0));
  }

  private static void validateAllProps(List<Map<String, String>> propCollection) {
    propCollection.forEach(
        props -> validateSinglePropertiesFile(props, resolvePropFileNumber(props)));
  }

  private static int resolvePropFileNumber(Map<String, String> props) {
    return Character.getNumericValue(props.get("key1").toCharArray()[4]);
  }

  private static void validateSinglePropertiesFile(Map<String, String> props, int testNumber) {
    assertThat(props.get("key1"), is(format("test%dvalue1", testNumber)));
    assertThat(props.get("key2"), is(format("test%dvalue2", testNumber)));
    assertThat(props.get("key3"), is(format("test%dvalue3", testNumber)));
    assertThat(props.get("key4"), is(format("test%dvalue4", testNumber)));
  }
}
