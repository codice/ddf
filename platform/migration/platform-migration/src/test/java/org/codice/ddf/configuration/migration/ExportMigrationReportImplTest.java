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

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationReportImplTest extends AbstractMigrationSupport {

  private static final String[] DIRS = new String[] {"path", "path2"};

  private static final String FILENAME = "file.ext";

  private static final String UNIX_NAME = "path/path2/" + FILENAME;

  private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

  private static final String PROPERTY = MigrationEntryImpl.METADATA_PROPERTY;

  private final MigrationReportImpl report = Mockito.mock(MigrationReportImpl.class);

  private final ExportMigrationContextImpl context = Mockito.mock(ExportMigrationContextImpl.class);

  private ExportMigrationReportImpl xreport;

  @Before
  public void setup() throws Exception {
    initMigratableMock();
    xreport = new ExportMigrationReportImpl(report, migratable);
  }

  @Test
  public void testConstructor() throws Exception {
    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(xreport.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(metadata, Matchers.aMapWithSize(4));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, migratable.getVersion()));
    Assert.assertThat(
        metadata, Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, migratable.getTitle()));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION, migratable.getDescription()));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            MigrationContextImpl.METADATA_ORGANIZATION, migratable.getOrganization()));
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ExportMigrationReportImpl(null, migratable);
  }

  @Test
  public void testConstructorWithNullMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new ExportMigrationReportImpl(report, null);
  }

  @Test
  public void testGetOperation() throws Exception {
    xreport.getOperation();

    Mockito.verify(report).getOperation();
  }

  @Test
  public void testGetStartTime() throws Exception {
    xreport.getStartTime();

    Mockito.verify(report).getStartTime();
  }

  @Test
  public void testEndTime() throws Exception {
    xreport.getEndTime();

    Mockito.verify(report).getEndTime();
  }

  @Test
  public void testRecordWithInfoString() throws Exception {
    final String info = "info";

    Assert.assertThat(xreport.record(info), Matchers.sameInstance(xreport));

    Mockito.verify(report).record(Mockito.same(info));
  }

  @Test
  public void testRecordWithInfoFormatAndArgs() throws Exception {
    final String format = "format %s";
    final String arg = "arg";

    Assert.assertThat(xreport.record(format, arg), Matchers.sameInstance(xreport));

    Mockito.verify(report).record(Mockito.same(format), Mockito.same(arg));
  }

  @Test
  public void testRecord() throws Exception {
    final MigrationInformation info = new MigrationInformation("info");

    Assert.assertThat(xreport.record(info), Matchers.sameInstance(xreport));

    Mockito.verify(report).record(Mockito.same(info));
  }

  @Test
  public void testDoAfterCompletion() throws Exception {
    final Consumer<MigrationReport> code = Mockito.mock(Consumer.class);

    Assert.assertThat(xreport.doAfterCompletion(code), Matchers.sameInstance(xreport));

    Mockito.verify(report).doAfterCompletion(Mockito.same(code));
  }

  @Test
  public void testWasSucessful() throws Exception {
    xreport.wasSuccessful();

    Mockito.verify(report).wasSuccessful();
  }

  @Test
  public void testWasSucessfulWithCode() throws Exception {
    final Runnable code = Mockito.mock(Runnable.class);

    xreport.wasSuccessful(code);

    Mockito.verify(report).wasSuccessful(Mockito.same(code));
  }

  @Test
  public void testWasIOSucessfulWithCode() throws Exception {
    final ThrowingRunnable<IOException> code = Mockito.mock(ThrowingRunnable.class);

    xreport.wasIOSuccessful(code);

    Mockito.verify(report).wasIOSuccessful(Mockito.same(code));
  }

  @Test
  public void testHasInfos() throws Exception {
    xreport.hasInfos();

    Mockito.verify(report).hasInfos();
  }

  @Test
  public void testHasWarnings() throws Exception {
    xreport.hasWarnings();

    Mockito.verify(report).hasWarnings();
  }

  @Test
  public void testHasErrors() throws Exception {
    xreport.hasErrors();

    Mockito.verify(report).hasErrors();
  }

  @Test
  public void testVerifyCompletion() throws Exception {
    xreport.verifyCompletion();

    Mockito.verify(report).verifyCompletion();
  }

  @Test
  public void testGetReport() throws Exception {
    Assert.assertThat(xreport.getReport(), Matchers.sameInstance(report));
  }

  private void initContext() {
    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(xreport);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);
  }

  @Test
  public void testRecordFile() throws Exception {
    final Path cfgPath = ddfHome.resolve("file.cfg");

    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, cfgPath);

    Assert.assertThat(xreport.recordFile(entry), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_FILES),
            Matchers.instanceOf(List.class)));
    final List<Object> fmetadata = (List<Object>) metadata.get(MigrationContextImpl.METADATA_FILES);

    Assert.assertThat(
        fmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> cmetadata = (Map<String, Object>) fmetadata.get(0);

    Assert.assertThat(
        cmetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(1),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName())));
  }

  @Test
  public void testRecordDirectoryWhenNotFiltered() throws Exception {
    final Set<String> files = ImmutableSet.of(UNIX_NAME);
    final Path dirPath = ddfHome.resolve("dir");

    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, dirPath);

    Assert.assertThat(xreport.recordDirectory(entry, null, files), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_FOLDERS),
            Matchers.instanceOf(List.class)));
    final List<Object> fmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_FOLDERS);

    Assert.assertThat(
        fmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> cmetadata = (Map<String, Object>) fmetadata.get(0);

    Assert.assertThat(
        cmetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(4),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FILTERED, (Object) false),
            Matchers.hasEntry(
                MigrationEntryImpl.METADATA_LAST_MODIFIED, (Object) entry.getLastModifiedTime()),
            Matchers.hasEntry(
                Matchers.equalTo(MigrationEntryImpl.METADATA_FILES),
                Matchers.<Object>sameInstance(files))));
  }

  @Test
  public void testRecordDirectoryWhenFiltered() throws Exception {
    final Set<String> files = ImmutableSet.of(UNIX_NAME);
    final Path dirPath = ddfHome.resolve("dir");

    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, dirPath);

    Assert.assertThat(
        xreport.recordDirectory(entry, p -> true, files), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_FOLDERS),
            Matchers.instanceOf(List.class)));
    final List<Object> fmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_FOLDERS);

    Assert.assertThat(
        fmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> cmetadata = (Map<String, Object>) fmetadata.get(0);

    Assert.assertThat(
        cmetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(4),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FILTERED, (Object) true),
            Matchers.hasEntry(
                MigrationEntryImpl.METADATA_LAST_MODIFIED, (Object) entry.getLastModifiedTime()),
            Matchers.hasEntry(
                Matchers.equalTo(MigrationEntryImpl.METADATA_FILES),
                Matchers.<Object>sameInstance(files))));
  }

  @Test
  public void testRecordExternalFile() throws Exception {
    createFile(createDirectory(DIRS), FILENAME);
    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, FILE_PATH);

    Assert.assertThat(xreport.recordExternal(entry, false), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_EXTERNALS),
            Matchers.instanceOf(List.class)));
    final List<Object> xmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_EXTERNALS);

    Assert.assertThat(
        xmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(4),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FOLDER, (Object) false),
            Matchers.hasKey(MigrationEntryImpl.METADATA_CHECKSUM),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_SOFTLINK, (Object) false)));
  }

  @Test
  public void testRecordExternalDirectory() throws Exception {
    final Path dir = createDirectory(DIRS);

    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, dir);

    Assert.assertThat(xreport.recordExternal(entry, false), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_EXTERNALS),
            Matchers.instanceOf(List.class)));
    final List<Object> xmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_EXTERNALS);

    Assert.assertThat(
        xmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FOLDER, (Object) true),
            Matchers.not(Matchers.hasKey(MigrationEntryImpl.METADATA_CHECKSUM)),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_SOFTLINK, (Object) false)));
  }

  @Test
  public void testRecordExternalFileWhenUnableToComputeChecksum() throws Exception {
    createFile(createDirectory(DIRS), FILENAME);
    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, FILE_PATH);

    entry.getFile().delete(); // will ensure the checksum cannot be computed

    Assert.assertThat(xreport.recordExternal(entry, false), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_EXTERNALS),
            Matchers.instanceOf(List.class)));
    final List<Object> xmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_EXTERNALS);

    Assert.assertThat(
        xmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FOLDER, (Object) false),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_SOFTLINK, (Object) false)));
  }

  @Test
  public void testRecordExternalSoftlink() throws Exception {
    final Path absoluteFilePath =
        ddfHome.resolve(createFile(createDirectory(DIRS), FILENAME)).toAbsolutePath();
    final String filename2 = "file2.ext";
    final Path absoluteFilePath2 =
        createSoftLink(absoluteFilePath.getParent(), filename2, absoluteFilePath);

    initContext();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, absoluteFilePath2);

    Assert.assertThat(xreport.recordExternal(entry, true), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_EXTERNALS),
            Matchers.instanceOf(List.class)));
    final List<Object> xmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_EXTERNALS);

    Assert.assertThat(
        xmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(4),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, entry.getName()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_FOLDER, (Object) false),
            Matchers.hasKey(MigrationEntryImpl.METADATA_CHECKSUM),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_SOFTLINK, (Object) true)));
  }

  @Test
  public void testRecordSystemProperty() throws Exception {
    initContext();
    final ExportMigrationSystemPropertyReferencedEntryImpl entry =
        new ExportMigrationSystemPropertyReferencedEntryImpl(
            context, PROPERTY, FILE_PATH.toString());

    Assert.assertThat(xreport.recordSystemProperty(entry), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_SYSTEM_PROPERTIES),
            Matchers.instanceOf(List.class)));
    final List<Object> smetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_SYSTEM_PROPERTIES);

    Assert.assertThat(
        smetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) smetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_PROPERTY, PROPERTY),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_REFERENCE, FILE_PATH.toString())));
  }

  @Test
  public void testRecordJavaProperty() throws Exception {
    initContext();
    final Path propertiesPath = ddfHome.resolve("file.properties");
    final ExportMigrationJavaPropertyReferencedEntryImpl entry =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, FILE_PATH.toString());

    Assert.assertThat(xreport.recordJavaProperty(entry), Matchers.sameInstance(xreport));

    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_JAVA_PROPERTIES),
            Matchers.instanceOf(List.class)));
    final List<Object> jmetadata =
        (List<Object>) metadata.get(MigrationContextImpl.METADATA_JAVA_PROPERTIES);

    Assert.assertThat(
        jmetadata,
        Matchers.allOf(
            Matchers.iterableWithSize(1), Matchers.contains(Matchers.instanceOf(Map.class))));
    final Map<String, Object> emetadata = (Map<String, Object>) jmetadata.get(0);

    Assert.assertThat(
        emetadata,
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_PROPERTY, PROPERTY),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_REFERENCE, FILE_PATH.toString()),
            Matchers.hasEntry(MigrationEntryImpl.METADATA_NAME, propertiesPath.toString())));
  }

  @Test
  public void testGetMetadataWhenNothingRegistered() throws Exception {
    final Map<String, Object> metadata = xreport.getMetadata();

    Assert.assertThat(metadata, Matchers.aMapWithSize(4));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, migratable.getVersion()));
    Assert.assertThat(
        metadata, Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, migratable.getTitle()));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION, migratable.getDescription()));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            MigrationContextImpl.METADATA_ORGANIZATION, migratable.getOrganization()));
  }
}
