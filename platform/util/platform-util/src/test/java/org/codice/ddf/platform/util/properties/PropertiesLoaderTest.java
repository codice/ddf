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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PropertiesLoaderTest {

  @Mock private Properties propertiesMock;

  @ClassRule public static TemporaryFolder temporaryFolder = new TemporaryFolder();
  private static final String PROPERTIES_FILENAME = "test.properties";
  private static final String NON_EXISTENT_FILENAME = "nonExistentFile.properties";
  private static File propertiesFile;

  private static final Map<Object, Object> EXAMPLE_MAP =
      ImmutableMap.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "value4value");

  // The system properties replacement is only done at the end when loadProperties is called.
  // This map will be used when testing the individual methods that are visible for testing
  private static final Map<Object, Object> EXAMPLE_MAP_WITH_SYS_PROP_KEY =
      ImmutableMap.of("key1", "value1", "key2", "value2", "key3", "value3", "key4", "${value4}");

  private static final PropertiesLoader PROPERTIES_LOADER = PropertiesLoader.getInstance();

  @BeforeClass
  public static void setUpClass() throws Exception {

    // set up string to put in properties file
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<Object, Object> entry : EXAMPLE_MAP.entrySet()) {
      sb.append(String.format("%s=%s%n", entry.getKey(), entry.getValue()));
    }

    // initialize file
    propertiesFile = temporaryFolder.newFile(PROPERTIES_FILENAME);
    Files.write(propertiesFile.toPath(), sb.toString().getBytes());

    System.setProperty("value4", "value4value");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    System.clearProperty("value4");
  }

  @Test
  public void testToMapWithProperties() throws Exception {
    when(propertiesMock.entrySet()).thenReturn(EXAMPLE_MAP.entrySet());

    Map<?, ?> entries = PROPERTIES_LOADER.toMap(propertiesMock);
    assertThat(entries, equalTo(EXAMPLE_MAP));
  }

  @Test
  public void testToMapWithNull() throws Exception {
    Map<?, ?> entries = PROPERTIES_LOADER.toMap(null);
    assertThat(entries.entrySet(), is(empty()));
  }

  @Test
  public void testToMapWithEmptyProperties() throws Exception {
    Map<?, ?> entries = PROPERTIES_LOADER.toMap(new Properties());
    assertThat(entries.entrySet(), is(empty()));
  }

  @Test
  public void testLoadPropertiesWithoutPropertiesFile() throws Exception {
    Properties testProperties = PROPERTIES_LOADER.loadProperties(null, null);
    assertThat(testProperties.entrySet(), is(empty()));
  }

  @Test
  public void testLoadPropertiesWithEmptyPropertiesFile() throws Exception {
    File emptyFile = temporaryFolder.newFile("tempFile");
    Properties testProperties = PROPERTIES_LOADER.loadProperties(emptyFile.getPath(), null);

    assertThat(testProperties.entrySet(), is(empty()));
  }

  @Test
  public void testLoadPropertiesWithProperties() throws Exception {
    Properties testProperties = PROPERTIES_LOADER.loadProperties(propertiesFile.getCanonicalPath());

    compareSets(testProperties.entrySet(), EXAMPLE_MAP.entrySet());
  }

  // TODO: 2018-12-07 Fix this
  @Ignore
  @Test
  public void testAttemptLoadWithSpring() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadWithSpring(
            PROPERTIES_FILENAME, this.getClass().getClassLoader());

    compareSets(testProperties.entrySet(), EXAMPLE_MAP.entrySet());
  }

  @Test
  public void testAttemptLoadWithSpringFileDNE() throws Exception {
    Properties testProperties = PropertiesLoader.attemptLoadWithSpring(NON_EXISTENT_FILENAME, null);

    assertThat(testProperties.entrySet(), is(empty()));
  }

  // TODO: 2018-12-07 Fix this
  @Ignore
  @Test
  public void testAttemptLoadWithSpringAndClassLoader() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadWithSpringAndClassLoader(
            PROPERTIES_FILENAME, this.getClass().getClassLoader());

    compareSets(testProperties.entrySet(), EXAMPLE_MAP.entrySet());
  }

  @Test
  public void testAttemptLoadWithSpringAndClassLoaderFileDNE() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadWithSpringAndClassLoader(NON_EXISTENT_FILENAME, null);

    assertThat(testProperties.entrySet(), is(empty()));
  }

  @Test
  public void testAttemptLoadWithFileSystem() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadWithFileSystem(propertiesFile.getPath(), null);

    compareSets(testProperties.entrySet(), EXAMPLE_MAP.entrySet());
  }

  @Test
  public void testAttemptLoadWithFileSystemFileDNE() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadWithFileSystem(NON_EXISTENT_FILENAME, null);

    assertThat(testProperties.entrySet(), is(empty()));
  }

  // TODO: 2018-12-07 Fix this
  @Ignore
  @Test
  public void testAttemptLoadAsResource() throws Exception {
    Properties testProperties =
        PropertiesLoader.attemptLoadAsResource("/" + PROPERTIES_FILENAME, null);

    compareSets(testProperties.entrySet(), EXAMPLE_MAP.entrySet());
  }

  @Test
  public void testAttemptLoadAsResourceFileDNE() throws Exception {
    Properties testProperties = PropertiesLoader.attemptLoadAsResource(NON_EXISTENT_FILENAME, null);

    assertThat(testProperties.entrySet(), is(empty()));
  }

  @Test
  public void testSubstituteSystemPropertyPlaceholders() throws Exception {
    // set up map to replace system properties
    Map<Object, Object> testMapSystemProperties = new HashMap<>();
    testMapSystemProperties.put("Java_Home", "${java.home}");
    testMapSystemProperties.put("Java_Version", "${java.version}");

    // set up map with replaced system properties
    Map<Object, Object> testMapSystemPropertiesAfter = new HashMap<>();
    testMapSystemPropertiesAfter.put("Java_Home", System.getProperty("java.home"));
    testMapSystemPropertiesAfter.put("Java_Version", System.getProperty("java.version"));

    // set up mock and insert into method
    when(propertiesMock.entrySet()).thenReturn(testMapSystemProperties.entrySet());
    Properties testProperties =
        PropertiesLoader.substituteSystemPropertyPlaceholders(propertiesMock);

    compareSets(testProperties.entrySet(), testMapSystemPropertiesAfter.entrySet());
  }

  @Test
  public void testLoadPropertiesWithoutReplacingSystemProperties() throws Exception {
    Properties testProperties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(
            PROPERTIES_FILENAME, null);
    compareSets(testProperties.entrySet(), EXAMPLE_MAP_WITH_SYS_PROP_KEY.entrySet());

    testProperties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(
            PROPERTIES_FILENAME, this.getClass().getClassLoader());
    compareSets(testProperties.entrySet(), EXAMPLE_MAP_WITH_SYS_PROP_KEY.entrySet());

    testProperties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(
            "/" + PROPERTIES_FILENAME, null);
    compareSets(testProperties.entrySet(), EXAMPLE_MAP_WITH_SYS_PROP_KEY.entrySet());

    testProperties =
        PROPERTIES_LOADER.loadPropertiesWithoutSystemPropertySubstitution(
            getClass().getClassLoader().getResource(PROPERTIES_FILENAME).getPath(), null);
    compareSets(testProperties.entrySet(), EXAMPLE_MAP_WITH_SYS_PROP_KEY.entrySet());
  }

  private void compareSets(Set actual, Set expected) {
    if (actual == null) {
      assertThat("Should be null collection", expected == null);
      return;
    }

    assertThat(actual.size(), is(expected.size()));
    for (Object o : expected) {
      assertThat("Value not in collection", actual.contains(o));
    }
  }
}
