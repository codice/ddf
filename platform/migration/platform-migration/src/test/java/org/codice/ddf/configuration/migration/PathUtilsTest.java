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
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PathUtilsTest extends AbstractMigrationTest {
    private static final String UNIX_NAME = "path/path2/file.ext";

    private static final String WINDOWS_NAME = "path\\path2\\file.ext";

    private static final String MIXED_NAME = "path\\path2/file.ext";

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
        final Path PATH = Paths.get("/etc/test.cfg");

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
        final Path PATH = Paths.get("/test/script.sh");

        final Path path = PATH_UTILS.resolveAgainstDDFHome(PATH.toString());

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(PATH));
    }
}
