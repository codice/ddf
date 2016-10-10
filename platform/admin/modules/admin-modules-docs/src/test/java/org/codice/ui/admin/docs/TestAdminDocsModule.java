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
package org.codice.ui.admin.docs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestAdminDocsModule {

    @ClassRule
    public static TemporaryFolder tempFolder;

    public static File mockDdfHome;

    @BeforeClass
    public static void beforeClass() throws IOException {
        tempFolder = new TemporaryFolder();
        tempFolder.create();
        mockDdfHome = tempFolder.newFolder("ddf");
        System.setProperty("karaf.home", mockDdfHome.getPath());
    }

    @Test
    public void noDocsFolder() {
        DocsSparkApplication docsSparkApp = new DocsSparkApplication();
        assertEquals(null, docsSparkApp.getDocumentationHtml());
    }

    @Test
    public void docsFolderPresentNoDocHtml() throws IOException {
        File docFolder = tempFolder.newFolder("ddf", "documentation");

        try {
            DocsSparkApplication docsSparkApp = new DocsSparkApplication();
            assertEquals(null, docsSparkApp.getDocumentationHtml());
        } finally {
            docFolder.delete();
        }
    }

    @Test
    public void docsFolderAndDocHtmlPresent() throws IOException {
        File docFolder = tempFolder.newFolder("ddf", "documentation", "html");
        Path docHtml = Files.createFile(Paths.get(docFolder.getPath(), "documentation.html"));

        try {
            DocsSparkApplication docsSparkApp = new DocsSparkApplication();
            assertEquals(docHtml.toString(), docsSparkApp.getDocumentationHtml().getPath());
        } finally {
            Files.delete(docHtml);
            docFolder.delete();
        }
    }
}
