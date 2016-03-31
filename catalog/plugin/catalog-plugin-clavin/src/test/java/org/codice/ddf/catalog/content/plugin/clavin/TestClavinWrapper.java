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
package org.codice.ddf.catalog.content.plugin.clavin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.index.IndexDirectoryBuilder;
import com.bericotech.clavin.resolver.ResolvedLocation;

public class TestClavinWrapper {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File indexLocation;

    private ClavinWrapper clavinWrapper;

    private ClavinPlugin clavinPlugin;

    private File ioTxtFile;

    @Before
    public void beforeClassSetup() throws IOException, ClavinException {
        clavinWrapper = new ClavinWrapper();
        clavinPlugin = new ClavinPlugin();
        indexLocation = temporaryFolder.newFolder();
        clavinWrapper.setIndexLocation(indexLocation.getAbsolutePath());

        ioTxtFile = temporaryFolder.newFile("IO.txt");
        FileOutputStream ioTxtOutStream = new FileOutputStream(ioTxtFile);
        InputStream ioTxtInStream = ClavinWrapper.class.getResourceAsStream("/IO.txt");
        IOUtils.copy(ioTxtInStream, ioTxtOutStream);

        clavinWrapper.createIndex(ioTxtFile);
    }

    @After
    public void afterClassTearDown() {
        FileUtils.deleteQuietly(indexLocation);
    }

    @Test
    public void indexExistsThrowsNullPointerExceptionGivenNull() {
        expectedEx.expect(NullPointerException.class);
        boolean b = clavinWrapper.indexExists(null);
    }

    @Test
    public void indexExistsReturnsFalseGivenEmptyFilenameFile() {
        boolean b = clavinWrapper.indexExists(new File(""));
        assertThat(b, is(equalTo(Boolean.FALSE)));
    }

    @Test
    public void indexExistsReturnsFalseGivenNonLuceneDirectory() throws IOException {
        boolean b = clavinWrapper.indexExists(temporaryFolder.newFolder());
        assertThat(b, is(equalTo(Boolean.FALSE)));
    }

    @Test
    public void indexExistsReturnsTrueGivenLuceneDirectory() throws IOException, ClavinException {
        boolean b = clavinWrapper.indexExists(indexLocation);
        assertThat(b, is(equalTo(Boolean.TRUE)));
    }

    @Test
    public void getIndexDirectoryBuilderReturnsNonNull() throws Exception {
        IndexDirectoryBuilder indexDirectoryBuilder = clavinWrapper.getIndexDirectoryBuilder();
        assertThat(indexDirectoryBuilder, is(notNullValue()));
    }

    @Test
    public void getGeoParserReturnsNonNull() throws Exception {
        GeoParser geoParser = clavinWrapper.getGeoParser();
        assertThat(geoParser, is(notNullValue()));
    }

    @Test
    public void createIndexThrowsNullPointerExceptionGivenNullIndexLocation()
            throws IOException, ClavinException {
        expectedEx.expect(NullPointerException.class);
        clavinWrapper.setIndexLocation(null);
        clavinWrapper.createIndex(new File("IO.txt"));
    }

    @Test
    public void createIndexThrowsIOExceptionGivenNonExistentResourceFile()
            throws IOException, ClavinException {
        expectedEx.expect(IOException.class);
        clavinWrapper.setIndexLocation(temporaryFolder.newFolder()
                .getAbsolutePath());
        clavinWrapper.createIndex(new File("non-existent-file"));
    }

    @Test
    public void parseThrowsExceptionWhenParserNotReady() throws Exception {
        expectedEx.expect(ClavinException.class);
        expectedEx.expectMessage("Error opening gazetteer index.");

        clavinWrapper.setIndexLocation(temporaryFolder.newFolder()
                .getAbsolutePath());
        clavinWrapper.parse("");
    }

    @Test
    public void parseReturnsEmptyGivenEmptyDocument() throws Exception {
        List<ResolvedLocation> locations = clavinWrapper.parse("");
        assertThat(locations.isEmpty(), is(true));
    }

    @Test
    public void processThrowsNullPointerExceptionGivenNullInput() throws Exception {
        expectedEx.expect(NullPointerException.class);
        clavinPlugin.process(null);
    }

}
