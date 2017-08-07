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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.test.util.ThrowableMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class MigrationEntryImplTest extends AbstractMigrationTest {
    private static final String UNIX_NAME = "path/path2/file.ext";

    private static final String WINDOWS_NAME = "path\\path2\\file.ext";

    private static final String MIXED_NAME = "path\\path2/file.ext";

    private static final Path FILE_PATH = Paths.get(UNIX_NAME);

    private static final String MIGRATABLE_ID = "test-migratable";

    private static final String MIGRATABLE_FQN = Paths.get(MIGRATABLE_ID)
            .resolve(FILE_PATH)
            .toString();

    private static final String SYSTEM_FQN = "file.hey";

    private final MigrationContextImpl CONTEXT = Mockito.mock(MigrationContextImpl.class);

    private final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
            Mockito.withSettings()
                    .useConstructor(CONTEXT, FILE_PATH)
                    .defaultAnswer(Mockito.CALLS_REAL_METHODS));

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSanitizeSeparatorsWithLinuxSeparators() throws Exception {
        Assert.assertThat(MigrationEntryImpl.sanitizeSeparators(UNIX_NAME),
                Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testSanitizeSeparatorsWithWindowsSeparators() throws Exception {
        Assert.assertThat(MigrationEntryImpl.sanitizeSeparators(WINDOWS_NAME),
                Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testSanitizeSeparatorsWithMixedSeparators() throws Exception {
        Assert.assertThat(MigrationEntryImpl.sanitizeSeparators(MIXED_NAME),
                Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithRelativePath() {
        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, FILE_PATH)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
    }

    @Test
    public void testConstructorWithAbsolutePathUnderDDFHome() {
        final Path ABSOLUTE_FILE_PATH = DDF_HOME.resolve(UNIX_NAME)
                .toAbsolutePath();

        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, ABSOLUTE_FILE_PATH)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
    }

    @Test
    public void testConstructorWithAbsolutePathNotUnderDDFHome() {
        final String ABSOLUTE_UNIX_NAME = "/path/path2/file.ext";
        final Path ABSOLUTE_FILE_PATH = Paths.get(ABSOLUTE_UNIX_NAME);

        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, ABSOLUTE_FILE_PATH)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
    }

    @Test
    public void testConstructorWithPathAndNullContext() {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        // unfortunately, right now Mockito has no way to control which of the 2 ctors is called
        // thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
        //        "null context")));

        Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(null, FILE_PATH)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithNullPath() {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        // unfortunately, right now Mockito has no way to control which of the 2 ctors is called
        // thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
        //        "null path")));

        Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, (Path) null)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithUnixName() {
        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, UNIX_NAME)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
    }

    @Test
    public void testConstructorWithWindowsName() {
        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, WINDOWS_NAME)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
    }

    @Test
    public void testConstructorWithMixedName() {
        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, MIXED_NAME)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
    }

    @Test
    public void testConstructorWithNullName() {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        // unfortunately, right now Mockito has no way to control which of the 2 ctors is called
        // thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
        //        "null name")));

        Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, (String) null)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithProviderAndMigratableFQN() {
        final Function<String, MigrationContextImpl> PROVIDER = Mockito.mock(Function.class);

        Mockito.when(PROVIDER.apply(Mockito.anyString()))
                .thenReturn(CONTEXT);

        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(PROVIDER, MIGRATABLE_FQN)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));

        Mockito.verify(PROVIDER, Mockito.only())
                .apply(MIGRATABLE_ID);
    }

    @Test
    public void testConstructorWithProviderAndSystemFQN() {
        final Function<String, MigrationContextImpl> PROVIDER = Mockito.mock(Function.class);

        Mockito.when(PROVIDER.apply(Mockito.nullable(String.class)))
                .thenReturn(CONTEXT);

        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(PROVIDER, SYSTEM_FQN)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(Paths.get(SYSTEM_FQN)));

        Mockito.verify(PROVIDER, Mockito.only())
                .apply(null);
    }

    @Test
    public void testConstructorWithNullProvider() {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        // unfortunately, right now Mockito has no way to control which of the 2 ctors is called
        // thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
        //        "null context provider")));

        Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(null, MIGRATABLE_FQN)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithProviderAndNullFQN() {
        final Function<String, MigrationContextImpl> PROVIDER = Mockito.mock(Function.class);

        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
                "null fully qualified name")));

        Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(PROVIDER, null)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testGetReport() {
        final MigrationReport REPORT = Mockito.mock(MigrationReport.class);

        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);

        Assert.assertThat(ENTRY.getReport(), Matchers.sameInstance(REPORT));
    }

    @Test
    public void testGetId() {
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);

        Assert.assertThat(ENTRY.getId(), Matchers.equalTo(MIGRATABLE_ID));
    }

    @Test
    public void testGetName() {
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testGetFQNWhenMigratableFQN() {
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);

        Assert.assertThat(ENTRY.getFQN(), Matchers.equalTo(MIGRATABLE_FQN));
    }

    @Test
    public void testGetFQNWhenSystemFQN() {
        final Function<String, MigrationContextImpl> PROVIDER = Mockito.mock(Function.class);

        Mockito.when(PROVIDER.apply(Mockito.nullable(String.class)))
                .thenReturn(CONTEXT);

        final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(PROVIDER, SYSTEM_FQN)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.getFQN(), Matchers.equalTo(SYSTEM_FQN));
    }

    // cannot test equals() on mocks

    @Test
    public void testCompareToWhenEquals() throws Exception {
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, FILE_PATH)
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithLesserPath() throws Exception {
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, Paths.get(UNIX_NAME + '2'))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithGreaterPath() throws Exception {
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT,
                                Paths.get(StringUtils.right(UNIX_NAME, UNIX_NAME.length() - 1)))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWithLesserId() throws Exception {
        final MigrationContextImpl CONTEXT2 = Mockito.mock(MigrationContextImpl.class);
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT2, Paths.get(UNIX_NAME))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID + '2');

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithGreaterId() throws Exception {
        final MigrationContextImpl CONTEXT2 = Mockito.mock(MigrationContextImpl.class);
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT2, Paths.get(UNIX_NAME))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(StringUtils.right(MIGRATABLE_ID, MIGRATABLE_ID.length() - 1));
        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWhenIdIsNull() throws Exception {
        final MigrationContextImpl CONTEXT2 = Mockito.mock(MigrationContextImpl.class);
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT2, Paths.get(UNIX_NAME))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Mockito.when(CONTEXT.getId())
                .thenReturn(null);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID);
        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWhenOtherIdIsNull() throws Exception {
        final MigrationContextImpl CONTEXT2 = Mockito.mock(MigrationContextImpl.class);
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT2, Paths.get(UNIX_NAME))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(null);
        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWhenBothIdsAreNull() throws Exception {
        final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, Paths.get(UNIX_NAME))
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS));

        Mockito.when(CONTEXT.getId())
                .thenReturn(null);
        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithSameInstance() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithNull() throws Exception {
        Assert.assertThat(ENTRY.compareTo(null), Matchers.greaterThan(0));
    }

    @Test
    public void testGetAbsolutePath() {
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(DDF_HOME.resolve(FILE_PATH)));
    }
}
