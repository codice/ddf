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

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.base.Charsets;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.OptionalMigratable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ImportMigrationManagerImplTest extends AbstractMigrationReportSupport {

  private static final String MIGRATABLE_ID2 = "test-migratable-2";

  private static final String MIGRATABLE_ID3 = "test-migratable-3";

  private final Migratable migratable2 = Mockito.mock(Migratable.class);

  private final Migratable migratable3 =
      Mockito.mock(
          Migratable.class, Mockito.withSettings().extraInterfaces(OptionalMigratable.class));

  private final Migratable[] migratables = new Migratable[] {migratable, migratable2, migratable3};

  private Path exportFile;

  private ImportMigrationManagerImpl mgr;

  private MigrationZipFile mockMigrationZipFile = Mockito.mock(MigrationZipFile.class);

  private ZipEntry zipEntry;

  public ImportMigrationManagerImplTest() {
    super(MigrationOperation.IMPORT);
  }

  @Before
  public void setup() throws Exception {
    exportFile = ddfHome.resolve(createDirectory("exported")).resolve("exported.dar");

    mockMigrationZipFile = Mockito.mock(MigrationZipFile.class);
    Mockito.when(mockMigrationZipFile.getZipPath()).thenReturn(exportFile);
    Mockito.when(mockMigrationZipFile.getChecksumPath())
        .thenReturn(MigrationZipConstants.getDefaultChecksumPathFor(exportFile));
    Mockito.when(mockMigrationZipFile.getKeyPath())
        .thenReturn(MigrationZipConstants.getDefaultKeyPathFor(exportFile));

    initMigratableMock();
    initMigratableMock(migratable2, MIGRATABLE_ID2);
    initMigratableMock(migratable3, MIGRATABLE_ID3);

    zipEntry =
        getMetadataZipEntry(
            Optional.of(MigrationContextImpl.CURRENT_VERSION),
            Optional.of(PRODUCT_BRANDING),
            Optional.of(PRODUCT_VERSION));

    // use answer to ensure we create a new stream each time if called multiple times
    Mockito.doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                return Stream.of(zipEntry);
              }
            })
        .when(mockMigrationZipFile)
        .stream();

    mgr =
        new ImportMigrationManagerImpl(
            report,
            mockMigrationZipFile,
            Collections.emptySet(),
            Stream.of(migratables),
            PRODUCT_BRANDING,
            PRODUCT_VERSION);
  }

  private MigrationZipEntry getMetadataZipEntry(
      Optional<String> version, Optional<String> productBranding, Optional<String> productVersion)
      throws IOException {
    final StringBuilder sb = new StringBuilder();

    sb.append("{\"dummy\":\"dummy");
    version.ifPresent(
        v ->
            sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_VERSION)
                .append("\":\"")
                .append(v));
    productBranding.ifPresent(
        b ->
            sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_PRODUCT_BRANDING)
                .append("\":\"")
                .append(b));
    productVersion.ifPresent(
        v ->
            sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_PRODUCT_VERSION)
                .append("\":\"")
                .append(v));
    sb.append("\",\"").append(MigrationContextImpl.METADATA_MIGRATABLES).append("\":{");
    boolean first = true;

    for (final Migratable m : migratables) {
      if (first) {
        first = false;
      } else {
        sb.append(",");
      }
      sb.append("\"")
          .append(m.getId())
          .append("\":{\"")
          .append(MigrationContextImpl.METADATA_VERSION)
          .append("\":\"")
          .append(m.getVersion())
          .append("\",\"")
          .append(MigrationContextImpl.METADATA_TITLE)
          .append("\":\"")
          .append(m.getTitle())
          .append("\",\"")
          .append(MigrationContextImpl.METADATA_DESCRIPTION)
          .append("\":\"")
          .append(m.getDescription())
          .append("\",\"")
          .append(MigrationContextImpl.METADATA_ORGANIZATION)
          .append("\":\"")
          .append(m.getOrganization())
          .append("\"}");
    }
    sb.append("}}");
    final MigrationZipEntry ze = Mockito.mock(MigrationZipEntry.class);

    Mockito.when(ze.getName()).thenReturn(MigrationContextImpl.METADATA_FILENAME.toString());
    Mockito.when(ze.isDirectory()).thenReturn(false);
    // use answer to ensure we create a new stream each time if called multiple times
    Mockito.doAnswer(
            AdditionalAnswers.answer(
                zea -> new ByteArrayInputStream(sb.toString().getBytes(Charsets.UTF_8))))
        .when(mockMigrationZipFile)
        .getInputStream(ze);
    return ze;
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ImportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3),
            Matchers.nullValue())); // null correspond to the system context
  }

  @Test
  public void testConstructorWithAdditionalMigratable() throws Exception {
    final Migratable migratable4 = Mockito.mock(Migratable.class);

    initMigratableMock(migratable4, "test-migratable-4");

    mgr =
        new ImportMigrationManagerImpl(
            report,
            mockMigrationZipFile,
            Collections.emptySet(),
            Stream.concat(Stream.of(migratables), Stream.of(migratable4)),
            PRODUCT_BRANDING,
            PRODUCT_VERSION);

    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ImportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3),
            Matchers.sameInstance(migratable4),
            Matchers.nullValue())); // null correspond to the system context
  }

  @Test
  public void testConstructorWithLessMigratables() throws Exception {
    mgr =
        new ImportMigrationManagerImpl(
            report,
            mockMigrationZipFile,
            Collections.emptySet(),
            Stream.of(migratable, migratable3),
            PRODUCT_BRANDING,
            PRODUCT_VERSION);

    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ImportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable3),
            Matchers.nullValue(),
            // null correspond to the system context
            Matchers.nullValue())); // null for migratable2
    Assert.assertThat(
        mgr.getContexts().stream().map(ImportMigrationContextImpl::getId).toArray(String[]::new),
        Matchers.arrayContaining(
            Matchers.equalTo(MIGRATABLE_ID),
            Matchers.equalTo(MIGRATABLE_ID3),
            Matchers.nullValue(),
            // null correspond to the system context
            Matchers.equalTo(MIGRATABLE_ID2)));
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
            .map(ImportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3),
            Matchers.nullValue())); // null correspond to the system context
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ImportMigrationManagerImpl(
        null,
        mockMigrationZipFile,
        Collections.emptySet(),
        Stream.empty(),
        PRODUCT_BRANDING,
        PRODUCT_VERSION);
  }

  @Test
  public void testConstructorWithInvalidReport() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid migration operation"));

    new ImportMigrationManagerImpl(
        report,
        mockMigrationZipFile,
        Collections.emptySet(),
        Stream.empty(),
        PRODUCT_BRANDING,
        PRODUCT_VERSION);
  }

  @Test
  public void testConstructorWithNullZipFile() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip file"));

    new ImportMigrationManagerImpl(
        report, null, Collections.emptySet(), Stream.empty(), PRODUCT_BRANDING, PRODUCT_VERSION);
  }

  @Test
  public void testConstructorWithNullMigratables() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratables"));

    new ImportMigrationManagerImpl(
        report,
        mockMigrationZipFile,
        Collections.emptySet(),
        null,
        PRODUCT_BRANDING,
        PRODUCT_VERSION);
  }

  @Test
  public void testConstructorWhenZipIsOfInvalidVersion() throws Exception {
    zipEntry =
        getMetadataZipEntry(
            Optional.of(VERSION), Optional.of(PRODUCT_BRANDING), Optional.of(PRODUCT_VERSION));

    thrown.expect(MigrationException.class);
    thrown.expectMessage("unsupported exported version");

    new ImportMigrationManagerImpl(
        report,
        mockMigrationZipFile,
        Collections.emptySet(),
        Stream.empty(),
        PRODUCT_BRANDING,
        PRODUCT_VERSION);
  }

  @Test
  public void testDoImport() throws Exception {
    mgr.doImport(Arrays.asList(PRODUCT_VERSION));

    Mockito.verify(migratable).doImport(Mockito.notNull());
    Mockito.verify(migratable2).doImport(Mockito.notNull());
    Mockito.verify(migratable3, Mockito.never()).doImport(Mockito.notNull());
  }

  @Test
  public void testDoImportWithMigratable3NowMandatory() throws Exception {
    final ImportMigrationManagerImpl mgr =
        new ImportMigrationManagerImpl(
            report,
            mockMigrationZipFile,
            Collections.singleton(MIGRATABLE_ID3),
            Stream.of(migratables),
            PRODUCT_BRANDING,
            PRODUCT_VERSION);

    mgr.doImport(Arrays.asList(PRODUCT_VERSION));

    Mockito.verify(migratable).doImport(Mockito.notNull());
    Mockito.verify(migratable2).doImport(Mockito.notNull());
    Mockito.verify(migratable3).doImport(Mockito.notNull());
  }

  @Test
  public void testConstructorWithDirectoryEntriesInZip() throws Exception {
    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .map(ImportMigrationContextImpl::getMigratable)
            .toArray(Migratable[]::new),
        Matchers.arrayContaining(
            Matchers.sameInstance(migratable),
            Matchers.sameInstance(migratable2),
            Matchers.sameInstance(migratable3),
            Matchers.nullValue())); // null correspond to the system context
    Assert.assertThat(
        mgr.getContexts()
            .stream()
            .filter(c -> c.getMigratable() == migratable)
            .map(ImportMigrationContextImpl::getEntries)
            .findFirst(),
        OptionalMatchers.hasValue(Matchers.anEmptyMap()));
  }

  @Test
  public void testDoImportWhenOneMigratableAborts() throws Exception {
    final MigrationException me = new MigrationException("testing");

    Mockito.doThrow(me).when(migratable2).doImport(Mockito.any());

    thrown.expect(Matchers.sameInstance(me));

    mgr.doImport(Arrays.asList(PRODUCT_VERSION));

    Mockito.verify(migratable).doImport(Mockito.notNull());
    Mockito.verify(migratable2).doImport(Mockito.notNull());
    Mockito.verify(migratable3, Mockito.never()).doImport(Mockito.notNull());
  }

  @Test
  public void testDoImportWithNullProductBranding() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null product branding"));

    mgr.doImport(Arrays.asList(PRODUCT_VERSION));
  }

  @Test
  public void testDoImportWithNullProductVersion() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null product version"));

    mgr.doImport(null);
  }

  @Test
  public void testDoImportWithInvalidProductBranding() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("mismatched product"));

    mgr.doImport(Arrays.asList(PRODUCT_VERSION));
  }

  @Test
  public void testDoImportWithInvalidProductVersion() throws Exception {
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("mismatched product version"));

    mgr.doImport(Arrays.asList(PRODUCT_VERSION + "2"));
  }
}
