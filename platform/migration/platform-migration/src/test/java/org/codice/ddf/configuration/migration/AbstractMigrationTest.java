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
package org.codice.ddf.configuration.migration;

import java.io.BufferedInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.google.common.base.Charsets;

public class AbstractMigrationTest {
    protected static final String MIGRATABLE_ID = "test-migratable";

    protected static final String VERSION = "3.1415";

    protected static final String PRODUCT_VERSION = "test-1.0";

    protected static final String TITLE = "Test Migratable";

    protected static final String DESCRIPTION = "Exporting test data";

    protected static final String ORGANIZATION = "Test Organization";

    protected final Migratable MIGRATABLE = Mockito.mock(Migratable.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected Path ROOT;

    protected Path DDF_HOME;

    protected Path DDF_BIN;

    /**
     * Retrieves all zip entries representing files from the specified zip file.
     *
     * @param path the path to the zip file
     * @return a map keyed by entry names with the corresponding entry
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static Map<String, ZipEntry> getEntriesFrom(Path path) throws IOException {
        return AbstractMigrationTest.getEntriesFrom(new BufferedInputStream(new FileInputStream(path.toFile())));
    }

    /**
     * Retrieves all zip entries representing files from the provided byte array output stream.
     *
     * @param baos the byte array output stream from which to retrieved all zip entries
     * @return a map keyed by entry names with the corresponding entry
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static Map<String, ZipEntry> getEntriesFrom(ByteArrayOutputStream baos)
            throws IOException {
        baos.close(); // not really required!
        return AbstractMigrationTest.getEntriesFrom(new ByteArrayInputStream(baos.toByteArray()));
    }

    /**
     * Retrieves all zip entries representing files from the provided input stream.
     *
     * @param in the input stream from which to retrieved all zip entries (will be closed)
     * @return a map keyed by entry names with the corresponding entry
     * @throws IOException if an I/O error occurs while reading the stream
     */
    public static Map<String, ZipEntry> getEntriesFrom(InputStream in) throws IOException {
        try (final ZipInputStream zin = (in instanceof ZipInputStream) ?
                (ZipInputStream) in :
                new ZipInputStream(in)) {
            final Map<String, ZipEntry> entries = new HashMap<>();

            while (true) {
                final java.util.zip.ZipEntry ze = zin.getNextEntry();

                if (ze == null) {
                    return entries;
                } else if (!ze.isDirectory()) {
                    entries.put(ze.getName(), new ZipEntry(ze, IOUtils.toByteArray(zin)));
                }
            }
        }
    }

    /**
     * Creates test files with the given names in the specified directory resolved under ${ddf.home}.
     * <p>
     * <i>Note:</i> Each files will be created with the filename (no directory) as its content.
     *
     * @param dir   the directory where to create the test files
     * @param names the names of all test files to create in the specified directory
     * @return a list of all relativized paths from ${ddf.home} for all test files created
     * @throws IOException if an I/O error occurs while creating the test files
     */
    public List<Path> createFiles(Path dir, String... names) throws IOException {
        return createFiles(new ArrayList<>(), dir, names);
    }

    /**
     * Creates test files with the given names in the specified directory resolved under ${ddf.home}
     * and adds their corresponding relativized from ${ddf.home} paths to the given list.
     * <p>
     * <i>Note:</i> Each files will be created with the filename (no directory) as its content.
     *
     * @param paths a list of paths where to add all paths for the test files created
     * @param dir   the directory where to create the test files
     * @param names the names of all test files to create in the specified directory
     * @return <code>paths</code> for chaining
     * @throws IOException if an I/O error occurs while creating the test files
     */
    public List<Path> createFiles(List<Path> paths, Path dir, String... names) throws IOException {
        final File rdir = DDF_HOME.resolve(dir)
                .toFile();

        for (final String name : names) {
            final File file = new File(rdir, name);

            FileUtils.writeStringToFile(file, name, Charsets.UTF_8);
            paths.add(DDF_HOME.relativize(file.toPath()
                    .toRealPath(LinkOption.NOFOLLOW_LINKS)));
        }
        return paths;
    }

    /**
     * Creates a test file with the given name in the specified directory resolved under ${ddf.home}.
     * <p>
     * <i>Note:</i> The file will be created with the filename (no directory) as its content.
     *
     * @param dir  the directory where to create the test file
     * @param name the name of the test file to create in the specified directory
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public Path createFile(Path dir, String name) throws IOException {
        final File file = new File(DDF_HOME.resolve(dir)
                .toFile(), name);

        FileUtils.writeStringToFile(file, name, Charsets.UTF_8);
        final Path path = file.toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        return path.startsWith(DDF_HOME) ? DDF_HOME.relativize(path) : path;
    }

    /**
     * Creates a test file with the given name under ${ddf.home}.
     * <p>
     * <i>Note:</i> The file will be created with the filename (no directory) as its content.
     *
     * @param name the name of the test file to create in the specified directory
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public Path createFile(String name) throws IOException {
        return createFile(DDF_HOME, name);
    }

    /**
     * Creates a test file at the given path.
     * <p>
     * <i>Note:</i> The file will be created with the filename (no directory) as its content.
     *
     * @param path the path of the test file to create in the specified directory
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public Path createFile(Path path) throws IOException {
        return createFile(path.getParent(),
                path.getFileName()
                        .toString());
    }

    /**
     * Creates a test file with the given name in the specified directory resolved under ${ddf.home}.
     * <p>
     * <i>Note:</i> The file will be created with the filename (no directory) as its content.
     *
     * @param dir      the directory where to create the test file
     * @param name     the name of the test file to create in the specified directory
     * @param resource the resource to copy to the test file
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public Path createFileFromResource(Path dir, String name, String resource) throws IOException {
        final File file = new File(DDF_HOME.resolve(dir)
                .toFile(), name);
        final InputStream is = AbstractMigrationTest.class.getResourceAsStream(resource);

        if (is == null) {
            throw new FileNotFoundException("resource '" + resource + "' not found");
        }
        FileUtils.copyInputStreamToFile(is, file);
        final Path path = file.toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        return path.startsWith(DDF_HOME) ? DDF_HOME.relativize(path) : path;
    }

    /**
     * Creates a test file at the given path from the specified resource.
     *
     * @param path     the path of the test file to create in the specified directory
     * @param resource the resource to copy to the test file
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public Path createFileFromResource(Path path, String resource) throws IOException {
        return createFileFromResource(path.getParent(),
                path.getFileName()
                        .toString(),
                resource);
    }

    /**
     * Creates a test softlink with the given name in the specified directory resolved under ${ddf.home}.
     *
     * @param dir  the directory where to create the test softlink
     * @param name the name of the test softlink to create in the specified directory
     * @param dest the destination path for the softlink
     * @return a path corresponding to the test softlink created (relativized from ${ddf.home})
     * @throws IOException                   if an I/O error occurs while creating the test softlink
     * @throws UnsupportedOperationException if the implementation does not support symbolic links
     */
    public Path createSoftLink(Path dir, String name, Path dest) throws IOException {
        final Path path = DDF_HOME.resolve(dir)
                .resolve(name);

        Files.createSymbolicLink(path, dest);
        final Path apath = path.toRealPath(LinkOption.NOFOLLOW_LINKS);

        return apath.startsWith(DDF_HOME) ? DDF_HOME.relativize(apath) : apath;
    }

    /**
     * Creates a test softlink with the given name under ${ddf.home}.
     *
     * @param name the name of the test softlink to create in the specified directory
     * @param dest the destination path for the softlink
     * @return a path corresponding to the test softlink created (relativized from ${ddf.home})
     * @throws IOException                   if an I/O error occurs while creating the test softlink
     * @throws UnsupportedOperationException if the implementation does not support symbolic links
     */
    public Path createSoftLink(String name, Path dest) throws IOException {
        return createSoftLink(DDF_HOME, name, dest);
    }

    /**
     * Creates a test softlink at the given path.
     *
     * @param path the path of the test softlink to create in the specified directory
     * @param dest the destination path for the softlink
     * @return a path corresponding to the test softlink created (relativized from ${ddf.home})
     * @throws IOException                   if an I/O error occurs while creating the test softlink
     * @throws UnsupportedOperationException if the implementation does not support symbolic links
     */
    public Path createSoftLink(Path path, Path dest) throws IOException {
        return createSoftLink(path.getParent(),
                path.getFileName()
                        .toString(),
                dest);
    }

    /**
     * Creates a test directory under ${ddf.home} with the given name(s).
     *
     * @param dirs the directory pathnames to create under ${ddf.home} (one per level)
     * @return the newly created directory
     * @throws IOException if an I/O error occurs while creating the test directory
     */
    public Path createDirectory(String... dirs) throws IOException {
        return testFolder.newFolder((String[]) ArrayUtils.addAll(new String[] {"ddf"}, dirs))
                .toPath();
    }

    @Before
    public void baseBefore() throws Exception {
        ROOT = testFolder.getRoot()
                .toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);
        DDF_HOME = testFolder.newFolder("ddf")
                .toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        System.setProperty("ddf.home", DDF_HOME.toString());
    }

    public void initMigratableMock() {
        initMigratableMock(MIGRATABLE, MIGRATABLE_ID);
    }

    public void initMigratableMock(Migratable migratable, String id) {
        Mockito.when(migratable.getId())
                .thenReturn(id);
        Mockito.when(migratable.getVersion())
                .thenReturn(VERSION);
        Mockito.when(migratable.getTitle())
                .thenReturn(TITLE);
        Mockito.when(migratable.getDescription())
                .thenReturn(DESCRIPTION);
        Mockito.when(migratable.getOrganization())
                .thenReturn(ORGANIZATION);
    }

    public void verifyReportHasMatchingError(MigrationReport report, Class exceptionClass,
            String message) {
        assertThat("Report has an error message", report.hasErrors(), is(true));
        MigrationException exception = report.errors()
                .findFirst()
                .get();

        assertThat(exception.getClass(), equalTo(exceptionClass));
        assertThat(exception.getMessage(), containsString(message));
    }

    public void verifyReportHasMatchingWarning(MigrationReport report, Class warningClass,
            String message) {
        assertThat("Report has a warning message", report.hasWarnings(), is(true));
        MigrationWarning warning = report.warnings()
                .findFirst()
                .get();

        assertThat(warning.getClass(), equalTo(warningClass));
        assertThat(warning.getMessage(), containsString(message));
    }

    public static class ZipEntry extends java.util.zip.ZipEntry {
        private final byte[] content;

        private ZipEntry(java.util.zip.ZipEntry ze, byte[] content) {
            super(ze);
            this.content = content;
        }

        public byte[] getContent() {
            return content;
        }

        public String getContentAsString() {
            return new String(content, Charsets.UTF_8);
        }
    }
}
