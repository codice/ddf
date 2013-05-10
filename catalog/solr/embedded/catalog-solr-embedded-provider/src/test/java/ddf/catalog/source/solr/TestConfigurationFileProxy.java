/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.source.solr;

import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Tests the ConfigurationFileProxy
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
public class TestConfigurationFileProxy {

    private static final Logger LOGGER = Logger
            .getLogger(TestConfigurationFileProxy.class);

    private static final String FILE_NAME_1 = "something.txt";

    private static final String FILE_NAME_2 = "somethingElse.txt";

    /**
     * Tests that files are indeed written to disk.
     */
    @Test
    public void testWritingToDisk() {

        BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(
                bundleContext, ConfigurationStore.getInstance());

        File tempLocation = new File("target/temp");

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);
    }

    /**
     * Tests that if a change was made to a file or if the file already exists,
     * the bundle would not overwrite that file.
     * 
     * @throws IOException
     */
    @Test
    public void testKeepingExistingFiles() throws IOException {

        File tempLocation = new File("target/temp");

        File file1 = new File(tempLocation, FILE_NAME_1);
        File file2 = new File(tempLocation, FILE_NAME_2);

        delete(file1, file2);

        BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(
                bundleContext, ConfigurationStore.getInstance());

        proxy.writeBundleFilesTo(tempLocation);

        verifyFilesExist(tempLocation);

        LOGGER.info("Contents Before Writing:" + FileUtils.readFileToString(file1));

        String newContents = FILE_NAME_2;
        
        FileUtils.writeStringToFile(file1, newContents);

        LOGGER.info("Contents switched to:" + FileUtils.readFileToString(file1));

        proxy.writeBundleFilesTo(tempLocation);

        String fileContents = FileUtils.readFileToString(file1);

        LOGGER.info("Final File contents:" + fileContents);

        assertThat(fileContents, is(newContents));

    }

    /**
     * Tests if a file is missing that the bundle would write onto disk the
     * config file for the user.
     * 
     * @throws IOException
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

        BundleContext bundleContext = givenBundleContext();

        ConfigurationFileProxy proxy = new ConfigurationFileProxy(
                bundleContext, ConfigurationStore.getInstance());

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
        when(
                bundle.findEntries(isA(String.class), isA(String.class),
                        isA(Boolean.class))).then(new Answer<Enumeration>() {
            @Override
            public Enumeration answer(InvocationOnMock invocation)
                    throws Throwable {
                List<URL> urls = new ArrayList<URL>();
                urls.add(TestConfigurationFileProxy.class.getResource("/"
                        + FILE_NAME_1));
                urls.add(TestConfigurationFileProxy.class.getResource("/"
                        + FILE_NAME_2));
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
