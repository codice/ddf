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
package org.codice.ddf.commands.catalog;

import static org.codice.ddf.commands.catalog.CommandSupport.ERROR_COLOR;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.codice.ddf.commands.util.DigitalSignature;
import org.fusesource.jansi.Ansi;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Tests the {@link DumpCommand} output. */
public class DumpCommandTest extends CommandCatalogFrameworkCommon {

  static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

  static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(ERROR_COLOR).toString();

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private DigitalSignature signer;

  @Before
  public void setup() throws Exception {
    this.signer = mock(DigitalSignature.class);

    when(signer.createDigitalSignature(
            any(InputStream.class), any(String.class), any(String.class)))
        .thenReturn(new byte[0]);
    doReturn(true)
        .when(signer)
        .verifyDigitalSignature(any(InputStream.class), any(InputStream.class), any(String.class));
  }

  /**
   * Check for bad output directory.
   *
   * @throws Exception
   */
  @Test
  public void testNonExistentOutputDirectory() throws Exception {
    // given
    DumpCommand dumpCommand = new DumpCommand(signer);
    dumpCommand.dirPath = "nosuchdirectoryanywherehereman";
    dumpCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;

    // when
    dumpCommand.executeWithSubject();

    // then
    String message =
        String.format("Directory [nosuchdirectoryanywherehereman%s] must exist.", File.separator);
    String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
    assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));
  }

  /**
   * Check for output directory is really a file.
   *
   * @throws Exception
   */
  @Test
  public void testOutputDirectoryIsFile() throws Exception {
    // given
    DumpCommand dumpCommand = new DumpCommand(signer);
    File testFile = testFolder.newFile("somefile.txt");
    String testFilePath = testFile.getAbsolutePath();
    dumpCommand.dirPath = testFilePath;
    dumpCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;

    // when
    dumpCommand.executeWithSubject();

    // then
    try {
      String message = "Path [" + testFilePath + "] must be a directory.";
      String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
      assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));
    } finally {
      testFile.delete();
    }
  }

  /**
   * Check for normal operation
   *
   * @throws Exception
   */
  @Test
  public void testNormalOperation() throws Exception {
    // given
    DumpCommand dumpCommand = new DumpCommand(signer);
    dumpCommand.securityLogger = mock(SecurityLogger.class);
    dumpCommand.catalogFramework = givenCatalogFramework(getResultList("id1", "id2"));
    dumpCommand.filterBuilder = new GeotoolsFilterBuilder();
    File outputDirectory = testFolder.newFolder("somedirectory");
    String outputDirectoryPath = outputDirectory.getAbsolutePath();
    dumpCommand.dirPath = outputDirectoryPath;
    dumpCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;

    // when
    dumpCommand.executeWithSubject();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" 2 file(s) dumped in "));
  }

  /**
   * Check for normal operation without any files
   *
   * @throws Exception
   */
  @Test
  public void testNormalOperationNoFiles() throws Exception {
    // given
    DumpCommand dumpCommand = new DumpCommand(signer);
    dumpCommand.securityLogger = mock(SecurityLogger.class);
    dumpCommand.catalogFramework = givenCatalogFramework(getEmptyResultList());
    dumpCommand.filterBuilder = new GeotoolsFilterBuilder();
    File outputDirectory = testFolder.newFolder("somedirectory");
    String outputDirectoryPath = outputDirectory.getAbsolutePath();
    dumpCommand.dirPath = outputDirectoryPath;
    dumpCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;

    // when
    dumpCommand.executeWithSubject();

    // then
    String expectedPrintOut = " 0 file(s) dumped in ";
    assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));
  }

  /**
   * Check for normal operation when there is local content associated with metacards
   *
   * @throws Exception
   */
  @Test
  public void testNormalOperationWithContent() throws Exception {
    // given
    List<Result> resultList = getResultList("id1", "id2");
    MetacardImpl metacard1 = new MetacardImpl(resultList.get(0).getMetacard());
    MetacardImpl metacard2 = new MetacardImpl(resultList.get(1).getMetacard());
    metacard1.setResourceURI(new URI("content:" + metacard1.getId()));
    metacard2.setResourceURI(new URI("content:" + metacard2.getId() + "#preview"));

    DumpCommand dumpCommand = new DumpCommand(signer);
    dumpCommand.securityLogger = mock(SecurityLogger.class);
    dumpCommand.catalogFramework = givenCatalogFramework(resultList);
    dumpCommand.filterBuilder = new GeotoolsFilterBuilder();
    File outputDirectory = testFolder.newFolder("somedirectory");
    String outputDirectoryPath = outputDirectory.getAbsolutePath();
    dumpCommand.dirPath = outputDirectoryPath;
    dumpCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;

    // when
    dumpCommand.executeWithSubject();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" 2 file(s) dumped in "));
  }

  /**
   * If a transform fails, assert that no file is dumped
   *
   * @throws Exception
   */
  @Test
  public void testNoFileDumpedWhenTransformFails() throws Exception {
    // mock transformer to throw exception
    MetacardTransformer transformer = mock(MetacardTransformer.class);
    when(transformer.transform(any(), any())).thenThrow(CatalogTransformerException.class);
    List<MetacardTransformer> transformers = new ArrayList<>();
    transformers.add(transformer);

    // given
    List<Result> resultList = getResultList("id1", "id2");
    MetacardImpl metacard1 = new MetacardImpl(resultList.get(0).getMetacard());
    MetacardImpl metacard2 = new MetacardImpl(resultList.get(1).getMetacard());
    metacard1.setResourceURI(new URI("content:" + metacard1.getId()));
    metacard2.setResourceURI(new URI("content:" + metacard2.getId() + "#preview"));

    TestDumpCommand dumpCommand = new TestDumpCommand(transformers, signer);
    dumpCommand.catalogFramework = givenCatalogFramework(resultList);
    dumpCommand.filterBuilder = new GeotoolsFilterBuilder();
    File outputDirectory = testFolder.newFolder("somedirectory");
    String outputDirectoryPath = outputDirectory.getAbsolutePath();
    dumpCommand.dirPath = outputDirectoryPath;
    dumpCommand.transformerId = "someOtherTransformer";
    dumpCommand.securityLogger = mock(SecurityLogger.class);

    // when
    dumpCommand.executeWithSubject();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" 0 file(s) dumped in "));
  }

  @Test
  public void testNormalOperationsAsCompressedFile() throws Exception {
    File outputDirectory = testFolder.newFolder("somedirectory");
    String outputDirectoryPath = outputDirectory.getAbsolutePath();
    List<Result> results =
        ImmutableList.of(
            new ResultImpl(getMetacard("metacardId1")), new ResultImpl(getMetacard("metacardId2")));

    TestDumpCommand dumpCommand = new TestDumpCommand(Collections.emptyList(), signer);
    dumpCommand.securityLogger = mock(SecurityLogger.class);
    dumpCommand.catalogFramework = givenCatalogFramework(results);
    dumpCommand.filterBuilder = new GeotoolsFilterBuilder();
    dumpCommand.dirPath = outputDirectoryPath;
    dumpCommand.zipFileName = "foobar.zip";

    dumpCommand.executeWithSubject();

    FileInputStream inputStream =
        new FileInputStream(outputDirectory + File.separator + "foobar.zip");
    assertZipContents(
        inputStream,
        ImmutableList.of(
            "metacards" + File.separator + "metacardId1",
            "metacards" + File.separator + "metacardId2"));

    String expectedPrintOut = " 2 file(s) dumped in ";
    assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));
  }

  private void assertZipContents(InputStream inputStream, List<String> ids) throws IOException {
    ZipInputStream zipInputStream = new ZipInputStream(inputStream);
    List<String> entryNames = new ArrayList<>();

    ZipEntry zipEntry = zipInputStream.getNextEntry();
    while (zipEntry != null) {
      entryNames.add(zipEntry.getName());
      zipEntry = zipInputStream.getNextEntry();
    }
    assertThat(entryNames.size(), is(ids.size()));

    for (String id : ids) {
      assertThat(entryNames, hasItem(id));
    }
  }

  private Metacard getMetacard(String id) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(id);
    return metacard;
  }

  private class TestDumpCommand extends DumpCommand {
    private List<MetacardTransformer> list;

    TestDumpCommand(List<MetacardTransformer> list, DigitalSignature signer) {
      super(signer);
      this.list = list;
    }

    @Override
    protected List<MetacardTransformer> getTransformers() {
      return list;
    }
  }
}
