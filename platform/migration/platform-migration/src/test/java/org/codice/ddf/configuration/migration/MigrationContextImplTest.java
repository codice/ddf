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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

public class MigrationContextImplTest extends AbstractMigrationTest {
    private static final String MIGRATABLE_ID = "test-migratable";

    private static final String VERSION = "3.1415";

    private final MigrationReport REPORT = new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());

    private final Migratable MIGRATABLE = Mockito.mock(Migratable.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private MigrationContextImpl CONTEXT;

    @Before
    public void before() throws Exception {
        Mockito.when(MIGRATABLE.getId())
                .thenReturn(MIGRATABLE_ID);

        CONTEXT = new MigrationContextImpl(REPORT);
    }

    @Test
    public void testConstructorWithReport() throws Exception {
        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo("?"));
        Assert.assertThat(CONTEXT.migratable, Matchers.nullValue());
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test
    public void testConstructorWithNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new MigrationContextImpl(null);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportWhenUndefinedDDFHome() throws Exception {
        FileUtils.forceDelete(DDF_HOME.toFile());

        new MigrationContextImpl(REPORT);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportWhenUndefinedCurrentWorkingDirectory() throws Exception {
        FileUtils.forceDelete(DDF_BIN.toFile());

        new MigrationContextImpl(REPORT);
    }

    @Test
    public void testConstructorWithReportAndIdAndVersion() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.migratable, Matchers.nullValue());
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test
    public void testConstructorWithNullReportAndIdAndVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new MigrationContextImpl(null, MIGRATABLE_ID, VERSION);
    }

    @Test
    public void testConstructorWithReportAndNullIdAndVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable identifier"));

        new MigrationContextImpl(REPORT, (String) null, VERSION);
    }

    @Test
    public void testConstructorWithReportAndIdAndNullVersion() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT, MIGRATABLE_ID, null);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.migratable, Matchers.nullValue());
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportWithIdAndVersionWhenUndefinedDDFHome() throws Exception {
        FileUtils.forceDelete(DDF_HOME.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE_ID, VERSION);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportWithdAndVersionWhenUndefinedCurrentWorkingDirectory()
            throws Exception {
        FileUtils.forceDelete(DDF_BIN.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE_ID, VERSION);
    }

    @Test
    public void testConstructorWithReportAndMigratable() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT, MIGRATABLE);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo("?"));
        Assert.assertThat(CONTEXT.migratable, Matchers.sameInstance(MIGRATABLE));
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test
    public void testConstructorWithNullReportAndMigratable() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new MigrationContextImpl(null, MIGRATABLE);
    }

    @Test
    public void testConstructorWithReportAndNullMigratable() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable"));

        new MigrationContextImpl(REPORT, null);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportAndMigratableWhenUndefinedDDFHome() throws Exception {
        FileUtils.forceDelete(DDF_HOME.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportAndMigratableWhenUndefinedCurrentWorkingDirectory()
            throws Exception {
        FileUtils.forceDelete(DDF_BIN.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE);
    }

    @Test
    public void testConstructorWithReportAndMigratableAndVersion() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT, MIGRATABLE, VERSION);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.migratable, Matchers.sameInstance(MIGRATABLE));
    }

    @Test
    public void testConstructorWithNullReportAndMigratableAndVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new MigrationContextImpl(null, MIGRATABLE, VERSION);
    }

    @Test
    public void testConstructorWithReportAndNullMigratableAndVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable"));

        new MigrationContextImpl(REPORT, (Migratable) null, VERSION);
    }

    @Test
    public void testConstructorWithReportAndMigratableAndNullVersion() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT, MIGRATABLE, null);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.migratable, Matchers.sameInstance(MIGRATABLE));
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportAndMigratableAndVersionWhenUndefinedDDFHome()
            throws Exception {
        FileUtils.forceDelete(DDF_HOME.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE, VERSION);
    }

    @Test(expected = IOError.class)
    public void testConstructorWithReportAndMigratableAndVersionWhenUndefinedCurrentWorkingDirectory()
            throws Exception {
        FileUtils.forceDelete(DDF_BIN.toFile());

        new MigrationContextImpl(REPORT, MIGRATABLE, VERSION);
    }

    @Test
    public void testGetDDFHome() throws Exception {
        Assert.assertThat(CONTEXT.getDDFHome(), Matchers.equalTo(DDF_HOME));
    }

    @Test
    public void testIsRelativeToDDFHomeWhenRelative() throws Exception {
        Assert.assertThat(CONTEXT.isRelativeToDDFHome(DDF_HOME.resolve("test")),
                Matchers.equalTo(true));
    }

    @Test
    public void testIsRelativeToDDFHomeWhenNot() throws Exception {
        Assert.assertThat(CONTEXT.isRelativeToDDFHome(testFolder.getRoot()
                .toPath()
                .resolve("test")), Matchers.equalTo(false));
    }

    @Test
    public void testRelativizeFromDDFHomeWhenRelative() throws Exception {
        final Path PATH = DDF_HOME.resolve("test");

        Assert.assertThat(CONTEXT.relativizeFromDDFHome(PATH), Matchers.equalTo(Paths.get("test")));
    }

    @Test
    public void testRelativeFromDDFHomeWhenNot() throws Exception {
        final Path PATH = testFolder.getRoot()
                .toPath()
                .resolve("test");

        Assert.assertThat(CONTEXT.relativizeFromDDFHome(PATH), Matchers.sameInstance(PATH));
    }

    @Test
    public void testResolveAgainstDDFHomeWhenPathIsRelative() throws Exception {
        final Path PATH = Paths.get("etc/test.cfg");

        final Path path = CONTEXT.resolveAgainstDDFHome(PATH);

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(DDF_HOME.resolve(PATH)));
    }

    @Test
    public void testResolveAgainstDDFHomeWhenPathIsAbsolute() throws Exception {
        final Path PATH = Paths.get("/etc/test.cfg");

        final Path path = CONTEXT.resolveAgainstDDFHome(PATH);

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.sameInstance(PATH));
    }

    @Test
    public void testResolveAgainstUserDirectoryWhenPathIsRelative() throws Exception {
        final Path PATH = Paths.get("test/script.sh");

        final Path path = CONTEXT.resolveAgainstUserDirectory(PATH.toString());

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(DDF_BIN.resolve(PATH)));
    }

    @Test
    public void testResolveAgainstUserDirectoryWhenPathIsAbsolute() throws Exception {
        final Path PATH = Paths.get("/test/script.sh");

        final Path path = CONTEXT.resolveAgainstUserDirectory(PATH.toString());

        Assert.assertThat(path.isAbsolute(), Matchers.equalTo(true));
        Assert.assertThat(path, Matchers.equalTo(PATH));
    }

    @Test
    public void testEqualsWhenIdentical() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);

        Assert.assertThat(CONTEXT.equals(CONTEXT), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWithNotContext() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);

        Assert.assertThat(CONTEXT.equals(new Object()), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenIdsAreEqual() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);
        final MigrationContextImpl CONTEXT2 = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);

        Assert.assertThat(CONTEXT.equals(CONTEXT2), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWhenIdIsNull() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT);
        final MigrationContextImpl CONTEXT2 = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);

        Assert.assertThat(CONTEXT.equals(CONTEXT2), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenOtherIdIsNull() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT,
                MIGRATABLE_ID,
                VERSION);
        final MigrationContextImpl CONTEXT2 = new MigrationContextImpl(REPORT);

        Assert.assertThat(CONTEXT.equals(CONTEXT2), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenBothIdAreNull() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT);
        final MigrationContextImpl CONTEXT2 = new MigrationContextImpl(REPORT);

        Assert.assertThat(CONTEXT.equals(CONTEXT2), Matchers.equalTo(true));
    }

    @Test
    public void testProcessMetadata() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT);
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION);

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
    }

    @Test(expected = MigrationException.class)
    public void testProcessMetadataWhenVersionIsMissing() throws Exception {
        final MigrationContextImpl CONTEXT = new MigrationContextImpl(REPORT);
        final Map<String, Object> METADATA = Collections.emptyMap();

        CONTEXT.processMetadata(METADATA);
    }
}
