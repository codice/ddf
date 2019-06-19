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
import com.google.common.collect.ImmutableMap;
import java.io.IOError;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MigrationContextImplTest extends AbstractMigrationReportSupport {
  private MigrationContextImpl<MigrationReport> context;

  public MigrationContextImplTest() {
    super(MigrationOperation.EXPORT);
  }

  @Before
  public void setup() throws Exception {
    initMigratableMock();
    context = new MigrationContextImpl<>(report);
  }

  @Test
  public void testConstructorWithReport() throws Exception {
    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getId(), Matchers.nullValue());
    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.migratable, Matchers.nullValue());
  }

  @Test
  public void testConstructorWithNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new MigrationContextImpl<>(null);
  }

  @Test(expected = IOError.class)
  public void testConstructorWithReportWhenUndefinedDDFHome() throws Exception {
    FileUtils.forceDelete(ddfHome.toFile());

    new MigrationContextImpl<>(report);
  }

  @Test
  public void testConstructorWithReportAndId() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.migratable, Matchers.nullValue());
  }

  @Test
  public void testConstructorWithIdAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new MigrationContextImpl<>(null, MIGRATABLE_ID);
  }

  @Test
  public void testConstructorWithReportAndNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable identifier"));

    new MigrationContextImpl<>(report, (String) null);
  }

  @Test(expected = IOError.class)
  public void testConstructorWithReportAndIdWhenUndefinedDDFHome() throws Exception {
    FileUtils.forceDelete(ddfHome.toFile());

    new MigrationContextImpl<>(report, MIGRATABLE_ID);
  }

  @Test
  public void testConstructorWithReportAndMigratable() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, migratable);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.migratable, Matchers.sameInstance(migratable));
  }

  @Test
  public void testConstructorWithNullReportAndMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new MigrationContextImpl<>(null, migratable);
  }

  @Test
  public void testConstructorWithReportAndNullMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new MigrationContextImpl<>(report, (Migratable) null);
  }

  @Test(expected = IOError.class)
  public void testConstructorWithReportAndMigratableWhenUndefinedDDFHome() throws Exception {
    FileUtils.forceDelete(ddfHome.toFile());

    new MigrationContextImpl<>(report, migratable);
  }

  @Test
  public void testConstructorWithReportAndMigratableAndVersion() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, migratable, VERSION);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.migratable, Matchers.sameInstance(migratable));
  }

  @Test
  public void testConstructorWithNullReportAndMigratableAndVersion() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new MigrationContextImpl<>(null, migratable, VERSION);
  }

  @Test
  public void testConstructorWithReportAndNullMigratableAndVersion() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new MigrationContextImpl<>(report, (Migratable) null, VERSION);
  }

  @Test
  public void testConstructorWithReportAndMigratableAndNullVersion() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null version"));

    new MigrationContextImpl<>(report, migratable, null);
  }

  @Test(expected = IOError.class)
  public void testConstructorWithReportAndMigratableAndVersionWhenUndefinedDDFHome()
      throws Exception {
    FileUtils.forceDelete(ddfHome.toFile());

    new MigrationContextImpl<>(report, migratable, VERSION);
  }

  @Test
  public void testEqualsWhenIdentical() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);

    Assert.assertThat(context.equals(context), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWithNotContext() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);

    Assert.assertThat(context.equals(new Object()), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenIdsAreEqual() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);
    final MigrationContextImpl<MigrationReport> context2 =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);

    Assert.assertThat(context.equals(context2), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWhenIdIsNull() throws Exception {
    final MigrationContextImpl<MigrationReport> context = new MigrationContextImpl<>(report);
    final MigrationContextImpl<MigrationReport> context2 =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);

    Assert.assertThat(context.equals(context2), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenOtherIdIsNull() throws Exception {
    final MigrationContextImpl<MigrationReport> context =
        new MigrationContextImpl<>(report, MIGRATABLE_ID);
    final MigrationContextImpl<MigrationReport> context2 = new MigrationContextImpl<>(report);

    Assert.assertThat(context.equals(context2), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenBothIdAreNull() throws Exception {
    final MigrationContextImpl<MigrationReport> context = new MigrationContextImpl<>(report);
    final MigrationContextImpl<MigrationReport> context2 = new MigrationContextImpl<>(report);

    Assert.assertThat(context.equals(context2), Matchers.equalTo(true));
  }

  @Test
  public void testProcessMetadata() throws Exception {
    final MigrationContextImpl<MigrationReport> context = new MigrationContextImpl<>(report);
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context.processMetadata(metadata);

    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.hasValue(VERSION));
  }

  @Test(expected = MigrationException.class)
  public void testProcessMetadataWhenVersionIsMissing() throws Exception {
    final MigrationContextImpl<MigrationReport> context = new MigrationContextImpl<>(report);
    final Map<String, Object> metadata = Collections.emptyMap();

    context.processMetadata(metadata);
  }

  @Test
  public void testProcessMetadataWhenVersionIsInvalid() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, 1.2F);

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(
        Matchers.containsString("[" + MigrationContextImpl.METADATA_VERSION + "]"));

    context.processMetadata(metadata);
  }
}
