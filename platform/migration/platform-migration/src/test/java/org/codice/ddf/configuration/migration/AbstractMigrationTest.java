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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.rules.TemporaryFolder;

public class AbstractMigrationTest {
    /**
     * We are forced to make this a class rule since the tested code sets up DDF_HOME statically.
     */
    @ClassRule
    public static TemporaryFolder testFolder = new TemporaryFolder();

    public static Path DDF_HOME;

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("ddf.home",
                testFolder.getRoot()
                        .getAbsolutePath());
        AbstractMigrationTest.DDF_HOME = testFolder.getRoot().getAbsoluteFile().toPath();
    }

    @After
    public void baseAfter() throws Exception {
        // we need to manually delete the test folder since we are forced to use a class rule
        testFolder.delete();
        // make sure to recreate the root folder while keeping the same location such as to not invalidate DDF_HOME
        testFolder.getRoot().mkdirs();
    }

    /**
     * Creates test files with the given names in the specified directory.
     *
     * @param dir   the directory where to create the test files
     * @param names the names of all test files to create in the specified directory
     * @return a list of all relativized paths from ${ddf.home} for all test files created
     * @throws IOException if an I/O error occurs while creating the test files
     */
    public static List<Path> createFiles(Path dir, String... names) throws IOException {
        return AbstractMigrationTest.createFiles(new ArrayList<>(), dir, names);
    }

    /**
     * Creates test files with the given names in the specified directory and adds their corresponding
     * relativized from ${ddf.home} paths to the given list.
     *
     * @param paths a list of paths where to add all paths for the test files created
     * @param dir   the directory where to create the test files
     * @param names the names of all test files to create in the specified directory
     * @return <code>paths</code> for chaining
     * @throws IOException if an I/O error occurs while creating the test files
     */
    public static List<Path> createFiles(List<Path> paths, Path dir, String... names) throws IOException {
        final File rdir = AbstractMigrationTest.DDF_HOME.resolve(dir).toFile();

        for (final String name : names) {
            final File file = new File(rdir, name);

            file.createNewFile();
            paths.add(DDF_HOME.relativize(file.toPath()));
        }
        return paths;
    }

    /**
     * Creates a test file with the given name in the specified directory.
     *
     * @param dir  the directory where to create the test file
     * @param name the name of the test path to create in the specified directory
     * @return a path corresponding to the test file created (relativized from ${ddf.home})
     * @throws IOException if an I/O error occurs while creating the test file
     */
    public static Path createFile(Path dir, String name) throws IOException {
        final File file = new File(AbstractMigrationTest.DDF_HOME.resolve(dir).toFile(), name);

        file.createNewFile();
        return DDF_HOME.relativize(file.toPath());
    }

    /**
     * Retrieves all zip entries from the provided input stream.
     *
     * @param in the input stream from which to retrieved all zip entries (will be closed)
     * @return a map keyed by entry names of all entries in the zip input stream
     * @throws IOException if an I/O error occurs whle reading the stream
     */
    public Map<String, ZipEntry> getEntriesFrom(InputStream in) throws IOException {
        try (final ZipInputStream zin = (in instanceof ZipInputStream) ? (ZipInputStream)in : new ZipInputStream(in)) {
            final Map<String, ZipEntry> entries = new HashMap<>();

            while (true) {
                final ZipEntry ze = zin.getNextEntry();

                if (ze == null) {
                    return entries;
                }
                entries.put(ze.getName(), ze);
            }
        }
    }

}
