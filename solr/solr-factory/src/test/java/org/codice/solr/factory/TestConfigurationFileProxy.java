/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.solr.factory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the ConfigurationFileProxy
 * 
 */
public class TestConfigurationFileProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigurationFileProxy.class);

    private static final String FILE_NAME_1 = "something.txt";

    private static final String FILE_NAME_2 = "somethingElse.txt";

    private File file1;

    private File file2;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Before
    public void setup() throws IOException {
        file1 = createTestFile(FILE_NAME_1);
        file2 = createTestFile(FILE_NAME_2);
    }

    private File createTestFile(String filename) throws IOException {
        File file = tempFolder.newFile(filename);

        OutputStream stream = new FileOutputStream(file);
        IOUtils.write(StringUtils.substringBeforeLast(filename, ".txt"), stream);
        IOUtils.closeQuietly(stream);

        return file;
    }

    /**
     * Tests that files are indeed written to disk.
     */
    @Test
    public void testWritingToDisk() {

        final BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(ConfigurationStore.getInstance()) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        File tempLocation = new File("target/temp");

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);
    }

    /**
     * Tests that if a change was made to a file or if the file already exists, the bundle would not
     * overwrite that file.
     * 
     * @throws java.io.IOException
     */
    @Test
    public void testKeepingExistingFiles() throws IOException {

        File tempLocation = new File("target/temp");

        File file1 = new File(tempLocation, FILE_NAME_1);
        File file2 = new File(tempLocation, FILE_NAME_2);

        delete(file1, file2);

        final BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(ConfigurationStore.getInstance()) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);

        LOGGER.info("Contents Before Writing:{}", FileUtils.readFileToString(file1));

        String newContents = FILE_NAME_2;

        FileUtils.writeStringToFile(file1, newContents);

        LOGGER.info("Contents switched to:{}", FileUtils.readFileToString(file1));

        proxy.writeBundleFilesTo(tempLocation);

        String fileContents = FileUtils.readFileToString(file1);

        LOGGER.info("Final File contents:{}", fileContents);

        assertThat(fileContents, is(newContents));

    }

    /**
     * Tests if a file is missing that the bundle would write onto disk the config file for the
     * user.
     *
     * @throws java.io.IOException
     */
    @Test
    public void testReplacement() throws IOException {

        // given
        File tempLocation = new File("target/temp");

        File file1 = new File(tempLocation, FILE_NAME_1);
        File file2 = new File(tempLocation, FILE_NAME_2);

        delete(file1, file2);

        if (tempLocation.list() != null) {
            assertThat(tempLocation.list().length, is(0));
        }

        final BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(ConfigurationStore.getInstance()) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        // when
        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);

        delete(file1);

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
     * @return
     */
    private BundleContext givenBundleContext() {
        BundleContext bundleContext = mock(BundleContext.class);
        Bundle bundle = mock(Bundle.class);

        when(bundleContext.getBundle()).thenReturn(bundle);

        // needs to return a new Enumeration each time
        when(bundle.findEntries(isA(String.class), isA(String.class), isA(Boolean.class))).then(
                new Answer<Enumeration>() {
                    @Override
                    public Enumeration answer(InvocationOnMock invocation) throws Throwable {
                        List<URL> urls = new ArrayList<URL>();
                        urls.add(file1.toURI().toURL());
                        urls.add(file2.toURI().toURL());
                        return new EnumerationStub(urls);
                    }
                });
        return bundleContext;
    }

    /**
     * @param tempLocation
     */
    private void verifyFilesExist(File tempLocation) {
        assertThat(tempLocation.listFiles().length, is(2));
        assertThat(tempLocation.list(), hasItemInArray(FILE_NAME_1));
        assertThat(tempLocation.list(), hasItemInArray(FILE_NAME_2));
    }

    private class EnumerationStub implements Enumeration<URL> {

        private List<URL> urls;

        private int index = 0;

        public EnumerationStub(List<URL> urls) {
            this.urls = urls;
        }

        @Override
        public boolean hasMoreElements() {
            return index < urls.size();
        }

        @Override
        public URL nextElement() {

            URL currentElement = urls.get(index);
            index++;
            return currentElement;
        }

    };
}
