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

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PathUtilsTest extends AbstractMigrationTest {
    private PathUtils PATH_UTILS;

    @Before
    public void before() throws Exception {
        PATH_UTILS = new PathUtils();
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(PATH_UTILS.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test(expected = IOError.class)
    public void testConstructorWhenUndefinedDDFHome() throws Exception {
        FileUtils.forceDelete(DDF_HOME.toFile());

        new PathUtils();
    }

    @Test
    public void testGetDDFHome() throws Exception {
        Assert.assertThat(PATH_UTILS.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test
    public void testIsRelativeToDDFHomeWhenRelative() throws Exception {
        Assert.assertThat(PATH_UTILS.isRelativeToDDFHome(DDF_HOME.resolve("test")),
                Matchers.equalTo(true));
    }

    @Test
    public void testIsRelativeToDDFHomeWhenNot() throws Exception {
        Assert.assertThat(PATH_UTILS.isRelativeToDDFHome(ROOT.resolve("test")),
                Matchers.equalTo(false));
    }

    @Test
    public void testRelativizeFromDDFHomeWhenRelative() throws Exception {
        final Path PATH = DDF_HOME.resolve("test");

        Assert.assertThat(PATH_UTILS.relativizeFromDDFHome(PATH),
                Matchers.equalTo(Paths.get("test")));
    }

    @Test
    public void testRelativeFromDDFHomeWhenNot() throws Exception {
        final Path PATH = ROOT.resolve("test");

        Assert.assertThat(PATH_UTILS.relativizeFromDDFHome(PATH), Matchers.sameInstance(PATH));
    }

    @Test
    public void testResolveAgainstDDFHomeWhenPathIsRelative() throws Exception {
        final Path PATH = Paths.get("etc/test.cfg");

        final Path path = PATH_UTILS.resolveAgainstDDFHome(PATH);

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(DDF_HOME.resolve(PATH)));
    }

    @Test
    public void testResolveAgainstDDFHomeWhenPathIsAbsolute() throws Exception {
        // resolving against DDF_HOME ensures that on Windows the absolute path gets the same drive as DDF_HOME
        final Path PATH = DDF_HOME.resolve(Paths.get("/etc", "test.cfg"));

        final Path path = PATH_UTILS.resolveAgainstDDFHome(PATH);

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.sameInstance(PATH));
    }

    @Test
    public void testResolveAgainstDDFHomeWithStringWhenPathIsRelative() throws Exception {
        final Path PATH = Paths.get("test/script.sh");

        final Path path = PATH_UTILS.resolveAgainstDDFHome(PATH.toString());

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(DDF_HOME.resolve(PATH)));
    }

    @Test
    public void testResolveAgainstDDFHomeWithStringWhenPathIsAbsolute() throws Exception {
        // resolving against DDF_HOME ensures that on Windows the absolute path gets the same drive as DDF_HOME
        final Path PATH = DDF_HOME.resolve(Paths.get("/test", "script.sh"));

        final Path path = PATH_UTILS.resolveAgainstDDFHome(PATH.toString());

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(PATH));
    }

    @Test
    public void testGetChecksumForWithFile() throws Exception {
        final Path PATH = DDF_HOME.resolve(createFile("test.txt"))
                .toAbsolutePath();

        final String checksum = PATH_UTILS.getChecksumFor(PATH);

        Assert.assertThat(checksum, Matchers.notNullValue());
    }

    @Test
    public void testGetChecksumForWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        PATH_UTILS.getChecksumFor(null);
    }

    @Test(expected = IOException.class)
    public void testGetChecksumForWithIOException() throws Exception {
        final Path PATH = DDF_HOME.resolve("test.txt");

        PATH_UTILS.getChecksumFor(PATH);
    }

    @Test
    public void testGetChecksumForWithSoftlink() throws Exception {
        final Path ABSOLUTE_FILE_PATH = DDF_HOME.resolve(createFile("test.txt"))
                .toAbsolutePath();
        final Path PATH2 = DDF_HOME.resolve(createSoftLink(ABSOLUTE_FILE_PATH.getParent(),
                "test2.txt",
                ABSOLUTE_FILE_PATH))
                .toAbsolutePath();

        final String checksum = PATH_UTILS.getChecksumFor(ABSOLUTE_FILE_PATH);
        final String checksum2 = PATH_UTILS.getChecksumFor(PATH2);

        Assert.assertThat(checksum2, Matchers.equalTo(checksum));
    }

}
