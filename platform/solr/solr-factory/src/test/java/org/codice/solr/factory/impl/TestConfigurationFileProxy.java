/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.solr.factory.impl;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the ConfigurationFileProxy
 */
public class TestConfigurationFileProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigurationFileProxy.class);

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();


    /**
     * Tests that files are indeed written to disk.
     */
    @Test
    public void testWritingToDisk() throws IOException {

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(ConfigurationStore.getInstance());

        File tempLocation = tempFolder.newFolder();

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);
    }

    /**
     * Tests that if a change was made to a file or if the file already exists, the file proxy would not
     * overwrite that file.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testKeepingExistingFiles() throws IOException {

        File tempLocation = tempFolder.newFolder();

        ConfigurationFileProxy proxy =
                new ConfigurationFileProxy(ConfigurationStore.getInstance());

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);

        File solrXml = Paths.get(tempLocation.getAbsolutePath(), "solr.xml")
                .toFile();

        FileUtils.writeStringToFile(solrXml, "test");

        LOGGER.info("Contents switched to:{}", FileUtils.readFileToString(solrXml));

        proxy.writeBundleFilesTo(tempLocation);

        String fileContents = FileUtils.readFileToString(solrXml);

        LOGGER.info("Final File contents:{}", fileContents);

        assertThat(fileContents, is("test"));

    }

    /**
     * Tests if a file is missing that the file proxy would write onto disk the config file for the
     * user.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testReplacement() throws IOException {

        // given
        File tempLocation = tempFolder.newFolder();

        if (tempLocation.list() != null) {
            assertThat(tempLocation.list().length, is(0));
        }

        ConfigurationFileProxy proxy =
                new ConfigurationFileProxy(ConfigurationStore.getInstance());

        // when
        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);

        File solrXml = Paths.get(tempLocation.getAbsolutePath(), "solr.xml")
                .toFile();

        delete(solrXml);

        proxy.writeBundleFilesTo(tempLocation);

        // then
        verifyFilesExist(tempLocation);

    }

    private void delete(File... files) {

        for (File f : files) {
            f.delete();
        }
    }

    /**
     * @param tempLocation
     */
    private void verifyFilesExist(File tempLocation) {
        File[] files = tempLocation.listFiles();
        if (null != files) {
            assertThat(files.length, is(8));
        } else {
            fail();
        }
        for (String file : ConfigurationFileProxy.SOLR_CONFIG_FILES) {
            assertThat(tempLocation.list(), hasItemInArray(file));
        }
    }

}
