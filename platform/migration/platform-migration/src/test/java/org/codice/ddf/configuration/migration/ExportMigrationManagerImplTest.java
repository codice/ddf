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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;
import javax.crypto.CipherOutputStream;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationManagerImplTest extends AbstractMigrationReportSupport {

  private static final String MIGRATABLE_ID2 = "test-migratable-2";

  private static final String MIGRATABLE_ID3 = "test-migratable-3";

  private final Migratable migratable2 = Mockito.mock(Migratable.class);

  private final Migratable migratable3 = Mockito.mock(Migratable.class);

  private final Migratable[] migratables = new Migratable[] {migratable, migratable2, migratable3};

  private Path exportFile;

  private CipherUtils mockCipherUtils;

  private ExportMigrationManagerImpl mgr;

  public ExportMigrationManagerImplTest() {
    super(MigrationOperation.EXPORT);
  }

  @Before
  public void setup() throws Exception {
    createSystemPropertyFiles();
    exportFile = ddfHome.resolve(createDirectory("exported")).resolve("exported.zip");
    mockCipherUtils = Mockito.mock(CipherUtils.class);
    CipherOutputStream cos = Mockito.mock(CipherOutputStream.class);
    Mockito.when(mockCipherUtils.getZipPath()).thenReturn(exportFile);
    Mockito.when(mockCipherUtils.getChecksumPath())
        .thenReturn(MigrationZipConstants.getDefaultChecksumPathFor(exportFile));
    Mockito.when(mockCipherUtils.getKeyPath())
        .thenReturn(MigrationZipConstants.getDefaultKeyPathFor(exportFile));
    Mockito.when(mockCipherUtils.getCipherOutputStream(Mockito.any(OutputStream.class)))
        .thenReturn(cos);
    initMigratableMock();
    initMigratableMock(migratable2, MIGRATABLE_ID2);
    initMigratableMock(migratable3, MIGRATABLE_ID3);

    mgr =
        new ExportMigrationManagerImpl(report, exportFile, mockCipherUtils, Stream.of(migratables));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ExportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3)));
  }

  @Test
  public void testConstructorWithDuplicateMigratableIds() throws Exception {
    final Migratable migratable2_2 = Mockito.mock(Migratable.class);

    initMigratableMock(migratable2_2, MIGRATABLE_ID2);
    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ExportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3)));
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ExportMigrationManagerImpl(null, exportFile, mockCipherUtils, Stream.empty());
  }

  @Test
  public void testConstructorWithInvalidReport() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid migration operation"));

    new ExportMigrationManagerImpl(report, exportFile, mockCipherUtils, Stream.empty());
  }

  @Test
  public void testConstructorWithNullExportFile() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null export file"));

    new ExportMigrationManagerImpl(report, null, mockCipherUtils, Stream.empty());
  }

  @Test
  public void testConstructorWithNullMigratables() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratables"));

    new ExportMigrationManagerImpl(report, exportFile, mockCipherUtils, null);
  }

  @Test
  public void testConstructorWithNullCipherUtils() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null cipher utils"));

    new ExportMigrationManagerImpl(report, exportFile, null, Stream.empty());
  }

  @Test
  public void testConstructorWhenUnableToCreateZipFile() throws Exception {
    exportFile =
        exportFile.getParent(); // using a dir instead of a file should trigger file not found

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("failed to create export file"));
    thrown.expectCause(Matchers.instanceOf(FileNotFoundException.class));

    new ExportMigrationManagerImpl(report, exportFile, mockCipherUtils, Stream.empty());
  }

  @Test
  public void testDoExport() throws Exception {
    mgr.doExport(PRODUCT_BRANDING, PRODUCT_VERSION);

    assertMetaData(mgr.getMetadata());

    Mockito.verify(migratable).doExport(Mockito.notNull());
    Mockito.verify(migratable2).doExport(Mockito.notNull());
    Mockito.verify(migratable3).doExport(Mockito.notNull());
  }

  @Test
  public void testDoExportWhenOneMigratableAborts() throws Exception {
    final MigrationException me = new MigrationException("testing");

    Mockito.doThrow(me).when(migratable2).doExport(Mockito.any());

    thrown.expect(Matchers.sameInstance(me));

    mgr.doExport(PRODUCT_BRANDING, PRODUCT_VERSION);

    Mockito.verify(migratable3, Mockito.never()).doExport(Mockito.notNull());
  }

  @Test
  public void testDoExportWithNullProductBranding() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null product branding"));

    mgr.doExport(null, PRODUCT_VERSION);
  }

  @Test
  public void testDoExportWithNullProductVersion() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null product version"));

    mgr.doExport(PRODUCT_BRANDING, null);
  }

  @Test
  public void testClose() throws Exception {
    CipherUtils cipherUtils = new CipherUtils(exportFile);

    mgr = new ExportMigrationManagerImpl(report, exportFile, cipherUtils, Stream.of(migratables));

    mgr.doExport(PRODUCT_BRANDING, PRODUCT_VERSION);

    mgr.close();

    final Map<String, MigrationZipEntry> entries =
        AbstractMigrationSupport.getEntriesFrom(exportFile);

    Assert.assertThat(entries, Matchers.aMapWithSize(1));
    Assert.assertThat(entries, Matchers.hasKey(MigrationContextImpl.METADATA_FILENAME.toString()));
    final Map<String, Object> ometadata =
        JsonUtils.MAPPER.parseMap(
            decrypt(
                entries.get(MigrationContextImpl.METADATA_FILENAME.toString()).getContent(),
                MigrationZipConstants.getDefaultKeyPathFor(exportFile)));

    assertMetaData(ometadata);
  }

  @Test
  public void testCloseWhenAlreadyClosed() throws Exception {
    final ZipOutputStream zos = Mockito.mock(ZipOutputStream.class);
    final ExportMigrationManagerImpl mgr =
        new ExportMigrationManagerImpl(
            report, exportFile, mockCipherUtils, Stream.of(migratables), zos);

    Mockito.doNothing().when(zos).closeEntry();

    mgr.close();

    mgr.close();

    Mockito.verify(zos).closeEntry();
  }

  @Test
  public void testCloseWhileFailingToCreateMetadataEntry() throws Exception {
    final ZipOutputStream zos = Mockito.mock(ZipOutputStream.class);
    final ExportMigrationManagerImpl mgr =
        new ExportMigrationManagerImpl(
            report, exportFile, mockCipherUtils, Stream.of(migratables), zos);
    final IOException ioe = new IOException("testing");

    Mockito.doNothing().when(zos).closeEntry();
    Mockito.doThrow(ioe).when(zos).putNextEntry(Mockito.any());

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("failed to create metadata"));
    thrown.expectCause(Matchers.sameInstance(ioe));

    mgr.close();
  }

  @Test
  public void testCloseWhileFailingToCloseLastEntry() throws Exception {
    final ZipOutputStream zos = Mockito.mock(ZipOutputStream.class);
    final ExportMigrationManagerImpl mgr =
        new ExportMigrationManagerImpl(
            report, exportFile, mockCipherUtils, Stream.of(migratables), zos);
    final IOException ioe = new IOException("testing");

    Mockito.doThrow(ioe).when(zos).closeEntry();

    thrown.expect(Matchers.sameInstance(ioe));

    mgr.close();
  }

  private void createSystemPropertyFiles() throws IOException {
    createFiles(Paths.get("etc"), "custom.system.properties", "system.properties");
  }

  private void assertMetaData(Map<String, Object> metadata) {
    Assert.assertThat(metadata, Matchers.aMapWithSize(9));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            MigrationContextImpl.METADATA_VERSION, MigrationContextImpl.CURRENT_VERSION));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_PRODUCT_BRANDING, PRODUCT_BRANDING));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_PRODUCT_VERSION, PRODUCT_VERSION));
    Assert.assertThat(metadata, Matchers.hasKey(MigrationContextImpl.METADATA_DATE));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(MigrationContextImpl.METADATA_DDF_HOME, System.getProperty("ddf.home")));
    Assert.assertThat(
        metadata,
        Matchers.hasEntry(
            Matchers.equalTo(MigrationContextImpl.METADATA_MIGRATABLES),
            Matchers.instanceOf(Map.class)));
    final Map<String, Object> mmetadatas =
        (Map<String, Object>) metadata.get(MigrationContextImpl.METADATA_MIGRATABLES);

    Assert.assertThat(mmetadatas, Matchers.aMapWithSize(migratables.length));
    Stream.of(migratables)
        .forEach(
            m -> {
              Assert.assertThat(
                  mmetadatas,
                  Matchers.hasEntry(Matchers.equalTo(m.getId()), Matchers.instanceOf(Map.class)));
              final Map<String, Object> mmetadata = (Map<String, Object>) mmetadatas.get(m.getId());

              Assert.assertThat(mmetadata, Matchers.aMapWithSize(4));
              Assert.assertThat(
                  mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, VERSION));
              Assert.assertThat(
                  mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, TITLE));
              Assert.assertThat(
                  mmetadata,
                  Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION, DESCRIPTION));
              Assert.assertThat(
                  mmetadata,
                  Matchers.hasEntry(MigrationContextImpl.METADATA_ORGANIZATION, ORGANIZATION));
            });
  }
}
