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
  private PathUtils pathUtils;

  @Before
  public void setup() throws Exception {
    pathUtils = new PathUtils();
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(pathUtils.getDDFHome(), Matchers.equalTo(ddfHome));
  }

  @Test(expected = IOError.class)
  public void testConstructorWhenUndefinedDDFHome() throws Exception {
    FileUtils.forceDelete(ddfHome.toFile());

    new PathUtils();
  }

  @Test
  public void testGetDDFHome() throws Exception {
    Assert.assertThat(pathUtils.getDDFHome(), Matchers.equalTo(ddfHome));
  }

  @Test
  public void testIsRelativeToDDFHomeWhenRelative() throws Exception {
    Assert.assertThat(
        pathUtils.isRelativeToDDFHome(ddfHome.resolve("test")), Matchers.equalTo(true));
  }

  @Test
  public void testIsRelativeToDDFHomeWhenNot() throws Exception {
    Assert.assertThat(pathUtils.isRelativeToDDFHome(root.resolve("test")), Matchers.equalTo(false));
  }

  @Test
  public void testRelativizeFromDDFHomeWhenRelative() throws Exception {
    final Path path = ddfHome.resolve("test");

    Assert.assertThat(pathUtils.relativizeFromDDFHome(path), Matchers.equalTo(Paths.get("test")));
  }

  @Test
  public void testRelativeFromDDFHomeWhenNot() throws Exception {
    final Path path = root.resolve("test");

    Assert.assertThat(pathUtils.relativizeFromDDFHome(path), Matchers.sameInstance(path));
  }

  @Test
  public void testResolveAgainstDDFHomeWhenPathIsRelative() throws Exception {
    final Path relativePath = Paths.get("etc/test.cfg");

    final Path path = pathUtils.resolveAgainstDDFHome(relativePath);

    Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
    Assert.assertThat(path, Matchers.equalTo(ddfHome.resolve(relativePath)));
  }

  @Test
  public void testResolveAgainstDDFHomeWhenPathIsAbsolute() throws Exception {
    // resolving against DDF_HOME ensures that on Windows the absolute path gets the same drive as
    // DDF_HOME
    final Path absolutePath = ddfHome.resolve(Paths.get("/etc", "test.cfg"));

    final Path path = pathUtils.resolveAgainstDDFHome(absolutePath);

    Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
    Assert.assertThat(path, Matchers.sameInstance(absolutePath));
  }

  @Test
  public void testResolveAgainstDDFHomeWithStringWhenPathIsRelative() throws Exception {
    final Path relativePath = Paths.get("test/script.sh");

    final Path path = pathUtils.resolveAgainstDDFHome(relativePath.toString());

    Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
    Assert.assertThat(path, Matchers.equalTo(ddfHome.resolve(relativePath)));
  }

  @Test
  public void testResolveAgainstDDFHomeWithStringWhenPathIsAbsolute() throws Exception {
    // resolving against DDF_HOME ensures that on Windows the absolute path gets the same drive as
    // DDF_HOME
    final Path absolutePath = ddfHome.resolve(Paths.get("/test", "script.sh"));

    final Path path = pathUtils.resolveAgainstDDFHome(absolutePath.toString());

    Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
    Assert.assertThat(path, Matchers.equalTo(absolutePath));
  }

  @Test
  public void testGetChecksumForWithFile() throws Exception {
    final Path path = ddfHome.resolve(createFile("test.txt")).toAbsolutePath();

    final String checksum = pathUtils.getChecksumFor(path);

    Assert.assertThat(checksum, Matchers.notNullValue());
  }

  @Test
  public void testGetChecksumForWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    pathUtils.getChecksumFor(null);
  }

  @Test(expected = IOException.class)
  public void testGetChecksumForWithIOException() throws Exception {
    final Path path = ddfHome.resolve("test.txt");

    pathUtils.getChecksumFor(path);
  }

  @Test
  public void testGetChecksumForWithSoftlink() throws Exception {
    final Path absoluteFilePath = ddfHome.resolve(createFile("test.txt")).toAbsolutePath();
    final Path path2 =
        ddfHome
            .resolve(createSoftLink(absoluteFilePath.getParent(), "test2.txt", absoluteFilePath))
            .toAbsolutePath();

    final String checksum = pathUtils.getChecksumFor(absoluteFilePath);
    final String checksum2 = pathUtils.getChecksumFor(path2);

    Assert.assertThat(checksum2, Matchers.equalTo(checksum));
  }
}
