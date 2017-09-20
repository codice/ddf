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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the {@link ConfigurationFileProxy} class. */
public class ConfigurationFileProxyTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileProxyTest.class);

  private static final String TEST_CORE_NAME = "test";

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private ConfigurationStore store;

  @Before
  public void beforeTest() throws Exception {
    File tempLocation = tempFolder.newFolder();

    store = ConfigurationStore.getInstance();
    store.setDataDirectoryPath(tempLocation.getPath());
  }

  /** Tests that files are indeed written to disk. */
  @Test
  public void testWritingToDisk() throws Exception {

    ConfigurationFileProxy proxy = new ConfigurationFileProxy(store);

    proxy.writeSolrConfiguration(TEST_CORE_NAME);

    verifyFilesExist(proxy);
  }

  /**
   * Tests that if a change was made to a file or if the file already exists, the file proxy would
   * not overwrite that file.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testKeepingExistingFiles() throws Exception {

    File tempLocation = tempFolder.newFolder();

    ConfigurationFileProxy proxy = new ConfigurationFileProxy(store);

    proxy.writeSolrConfiguration(TEST_CORE_NAME);

    verifyFilesExist(proxy);

    File solrXml = Paths.get(tempLocation.getAbsolutePath(), "solr.xml").toFile();

    FileUtils.writeStringToFile(solrXml, TEST_CORE_NAME);

    LOGGER.info("Contents switched to:{}", FileUtils.readFileToString(solrXml));

    proxy.writeSolrConfiguration(TEST_CORE_NAME);

    String fileContents = FileUtils.readFileToString(solrXml);

    LOGGER.info("Final File contents:{}", fileContents);

    assertThat(fileContents, is(TEST_CORE_NAME));
  }

  /**
   * Tests if a file is missing that the file proxy would write onto disk the config file for the
   * user.
   *
   * @throws java.io.IOException
   */
  @Test
  public void testReplacement() throws Exception {

    // given
    File tempLocation = tempFolder.newFolder();

    if (tempLocation.list() != null) {
      assertThat(tempLocation.list().length, is(0));
    }

    ConfigurationFileProxy proxy = new ConfigurationFileProxy(store);

    // when
    proxy.writeSolrConfiguration(TEST_CORE_NAME);

    verifyFilesExist(proxy);

    File solrXml = Paths.get(tempLocation.getAbsolutePath(), "solr.xml").toFile();

    delete(solrXml);

    proxy.writeSolrConfiguration(TEST_CORE_NAME);

    // then
    verifyFilesExist(proxy);
  }

  private void delete(File... files) {

    for (File f : files) {
      f.delete();
    }
  }

  private void verifyFilesExist(ConfigurationFileProxy proxy) throws URISyntaxException {
    for (String file : ConfigurationFileProxy.SOLR_CONFIG_FILES) {
      assertThat(Paths.get(proxy.getResource(file, "test").toURI()).toFile().exists(), is(true));
    }
  }
}
