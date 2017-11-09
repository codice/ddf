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

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class PathUtilsTest extends AbstractMigrationSupport {

  private PathUtils pathUtils;

  private final MigrationReport report =
      new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

  @Before
  public void setup() throws Exception {
    pathUtils = new PathUtils();
  }

  @Test
  public void testDeleteQuietlyADirectory() throws Exception {
    Assert.assertThat(PathUtils.deleteQuietly(ddfHome, "what"), Matchers.equalTo(true));

    Assert.assertThat(ddfHome.toFile().exists(), Matchers.equalTo(false));
  }

  @Test
  public void testDeleteQuietlyAFile() throws Exception {
    final Path path = ddfHome.resolve(createFile("test.txt"));

    Assert.assertThat(PathUtils.deleteQuietly(path, "what"), Matchers.equalTo(true));

    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
  }

  @Test
  public void testDeleteQuietlyWhenFail() throws Exception {
    final Path path = ddfHome.resolve("unknown");

    Assert.assertThat(PathUtils.deleteQuietly(path, "what"), Matchers.equalTo(false));
  }

  @Test
  public void testDeleteQuietlyWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    PathUtils.deleteQuietly(null, "what");
  }

  @Test
  public void testDeleteQuietlyWithNullType() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null type"));

    PathUtils.deleteQuietly(ddfHome, null);
  }

  @Test
  public void testCleanQuietlyADirectoryWithFiles() throws Exception {
    final Path path = createDirectory("dir");
    final Path path2 = createDirectory("dir", "dir2");
    final Path path3 = createDirectory("dir3");
    final Path path4 = createDirectory("dir", "dir2", "dir4");
    final Path file = ddfHome.resolve(createFile(path, "test.txt"));
    final Path file0 = ddfHome.resolve(createFile(ddfHome, "test0.txt"));
    final Path file3 = ddfHome.resolve(createFile(path3, "test3.txt"));
    final Path file33 = ddfHome.resolve(createFile(path3, "test33.txt"));

    Assert.assertThat(PathUtils.cleanQuietly(ddfHome, report), Matchers.equalTo(false));

    Assert.assertThat(ddfHome.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(path3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path4.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file33.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanQuietlyADirectoryWithoutFiles() throws Exception {
    final Path path = createDirectory("dir");
    final Path path2 = createDirectory("dir", "dir2");
    final Path path3 = createDirectory("dir3");
    final Path path4 = createDirectory("dir", "dir2", "dir4");

    Assert.assertThat(PathUtils.cleanQuietly(ddfHome, report), Matchers.equalTo(true));

    Assert.assertThat(ddfHome.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(path3.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(path4.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanQuietlyADirectoryWithoutFilesAndFailing() throws Exception {
    final File file = Mockito.mock(File.class);
    final File file2 = Mockito.mock(File.class);
    final File file3 = Mockito.mock(File.class);
    final File file4 = Mockito.mock(File.class);
    final Path path = Mockito.mock(Path.class);
    final Path path2 = Mockito.mock(Path.class);
    final Path path3 = Mockito.mock(Path.class);
    final Path path4 = Mockito.mock(Path.class);

    Mockito.when(path.toFile()).thenReturn(file);
    Mockito.when(path2.toFile()).thenReturn(file2);
    Mockito.when(path3.toFile()).thenReturn(file3);
    Mockito.when(path4.toFile()).thenReturn(file3);
    Mockito.when(file.isDirectory()).thenReturn(true);
    Mockito.when(file2.isDirectory()).thenReturn(true);
    Mockito.when(file3.isDirectory()).thenReturn(true);
    Mockito.when(file4.isDirectory()).thenReturn(true);
    Mockito.when(file.listFiles()).thenReturn(new File[] {file2, file3});
    Mockito.when(file2.listFiles()).thenReturn(new File[] {file4});
    Mockito.when(file3.listFiles()).thenReturn(new File[] {});
    Mockito.when(file4.listFiles()).thenReturn(null);
    Mockito.when(file4.delete()).thenReturn(true);
    Mockito.when(file2.delete()).thenReturn(false);
    Mockito.when(file3.delete()).thenReturn(true);

    Assert.assertThat(PathUtils.cleanQuietly(path, report), Matchers.equalTo(false));

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));

    Mockito.verify(file, Mockito.never()).delete();
    Mockito.verify(file2).delete();
    Mockito.verify(file3).delete();
    Mockito.verify(file4).delete();
  }

  @Test
  public void testCleanQuietlyAFile() throws Exception {
    final Path path = ddfHome.resolve(createFile("test.txt"));

    Assert.assertThat(PathUtils.cleanQuietly(path, report), Matchers.equalTo(true));

    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanQuietlyAFileWhenFail() throws Exception {
    final File file = Mockito.mock(File.class);
    final Path path = Mockito.mock(Path.class);

    Mockito.when(path.toFile()).thenReturn(file);
    Mockito.when(file.isDirectory()).thenReturn(false);
    Mockito.when(file.delete()).thenReturn(false);

    Assert.assertThat(PathUtils.cleanQuietly(path, report), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanQuietlyWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    PathUtils.cleanQuietly(null, report);
  }

  @Test
  public void testCleanQuietlyWithNullType() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    PathUtils.cleanQuietly(ddfHome, null);
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
