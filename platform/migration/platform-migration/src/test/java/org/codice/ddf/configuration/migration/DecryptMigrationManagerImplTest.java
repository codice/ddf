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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DecryptMigrationManagerImplTest extends AbstractMigrationReportSupport {

  private static final String NAME = "name";

  private static final String CONTENT = "content";

  private Path exportFile;

  private Path decryptFile;

  private DecryptMigrationManagerImpl mgr;

  private final MigrationZipFile zip = Mockito.mock(MigrationZipFile.class);

  public DecryptMigrationManagerImplTest() {
    super(MigrationOperation.DECRYPT);
  }

  @Before
  public void setup() throws Exception {
    final Path exportedDir = ddfHome.resolve(createDirectory("exported"));

    exportFile = exportedDir.resolve("exported.dar");
    decryptFile = exportedDir.resolve("exported.zip");

    Mockito.when(zip.getZipPath()).thenReturn(exportFile);
    Mockito.when(zip.getChecksumPath())
        .thenReturn(MigrationZipConstants.getDefaultChecksumPathFor(exportFile));
    Mockito.when(zip.getKeyPath())
        .thenReturn(MigrationZipConstants.getDefaultKeyPathFor(exportFile));

    mgr =
        Mockito.mock(
            DecryptMigrationManagerImpl.class,
            Mockito.withSettings()
                .useConstructor(report, zip, decryptFile)
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(mgr.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(mgr.getExportFile(), Matchers.sameInstance(exportFile));
    Assert.assertThat(mgr.getDecryptedFile(), Matchers.sameInstance(decryptFile));
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new DecryptMigrationManagerImpl(null, zip, decryptFile);
  }

  @Test
  public void testConstructorWithInvalidReport() throws Exception {
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid migration operation"));

    new DecryptMigrationManagerImpl(report, zip, decryptFile);
  }

  @Test
  public void testConstructorWithNullExportFile() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip file"));

    new DecryptMigrationManagerImpl(report, null, decryptFile);
  }

  @Test
  public void testConstructorWithNullDecryptFile() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null decrypt file"));

    new DecryptMigrationManagerImpl(report, zip, null);
  }

  @Test
  public void testDoDecrypt() throws Exception {
    final ZipEntry ze = new ZipEntry("1");
    final ZipEntry ze2 = new ZipEntry("2");
    final ZipEntry ze3 = new ZipEntry("3");

    Mockito.doReturn(Stream.of(ze, ze2, ze3)).when(zip).stream();
    Mockito.doNothing().when(mgr).copyToOutputZipFile(Mockito.notNull());

    mgr.doDecrypt(PRODUCT_BRANDING, PRODUCT_VERSION);

    Mockito.verify(mgr).doDecrypt(PRODUCT_BRANDING, PRODUCT_VERSION);
    Mockito.verify(mgr).copyToOutputZipFile(ze);
    Mockito.verify(mgr).copyToOutputZipFile(ze2);
    Mockito.verify(mgr).copyToOutputZipFile(ze3);

    Mockito.verifyNoMoreInteractions(mgr);
  }

  @Test
  public void testCopyToOutputZipFile() throws Exception {
    final ZipEntry ze = Mockito.mock(ZipEntry.class);

    Mockito.doReturn(NAME).when(ze).getName();
    Mockito.doReturn(false).when(ze).isDirectory();
    Mockito.doReturn(new ByteArrayInputStream(CONTENT.getBytes(Charset.defaultCharset())))
        .when(zip)
        .getInputStream(ze);

    mgr.copyToOutputZipFile(ze);
    mgr.close();

    Mockito.verify(zip).getInputStream(ze);

    final Map<String, MigrationZipEntry> entries =
        AbstractMigrationSupport.getEntriesFrom(decryptFile);

    Assert.assertThat(entries, Matchers.aMapWithSize(1));
    Assert.assertThat(entries, Matchers.hasKey(NAME));
    final MigrationZipEntry entry = entries.get(NAME);

    Assert.assertThat(entry.isDirectory(), equalTo(false));
    Assert.assertThat(entry.getContentAsString(), equalTo(CONTENT));
  }

  @Test
  public void testCopyToOutputZipFileWithDirectoryEntry() throws Exception {
    // a directory entry in a zip ends with / no matter the OS
    final String name = NAME + "/";
    final ZipEntry ze = Mockito.mock(ZipEntry.class);

    Mockito.doReturn(name).when(ze).getName();
    Mockito.doReturn(true).when(ze).isDirectory();

    mgr.copyToOutputZipFile(ze);
    mgr.close();

    Mockito.verify(zip, Mockito.never()).getInputStream(ze);

    final Map<String, MigrationZipEntry> entries =
        AbstractMigrationSupport.getEntriesFrom(decryptFile);

    Assert.assertThat(entries, Matchers.aMapWithSize(1));
    Assert.assertThat(entries, Matchers.hasKey(name));
    final MigrationZipEntry entry = entries.get(name);

    Assert.assertThat(entry.isDirectory(), equalTo(true));
    Assert.assertThat(entry.getContent().length, equalTo(0));
  }

  @Test
  public void testCopyToOutputZipFileWithIOException() throws Exception {
    final IOException e = new IOException("testing");
    final ZipEntry ze = Mockito.mock(ZipEntry.class);

    Mockito.doReturn(NAME).when(ze).getName();
    Mockito.doReturn(false).when(ze).isDirectory();
    Mockito.doThrow(e).when(zip).getInputStream(ze);

    mgr.copyToOutputZipFile(ze);
    mgr.close();

    reportHasErrorMessage(Matchers.containsString("path [" + NAME + "] could not be decrypted"));
  }

  private void reportHasErrorMessage(Matcher<String> matcher) {
    final List<MigrationException> es = report.errors().collect(Collectors.toList());
    final long count = es.stream().filter((e) -> matcher.matches(e.getMessage())).count();
    final Description d = new StringDescription();

    matcher.describeTo(d);

    assertThat(
        "There are "
            + count
            + " matching error(s) with "
            + d
            + " in the migration report.\nErrors are: "
            + es.stream()
                .map(MigrationException::getMessage)
                .collect(Collectors.joining("\",\n\t\"", "[\n\t\"", "\"\n]")),
        count,
        equalTo(1L));
  }
}
