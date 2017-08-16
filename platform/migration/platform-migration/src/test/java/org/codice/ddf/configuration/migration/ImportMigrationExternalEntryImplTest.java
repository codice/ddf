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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.ImportPathMigrationWarning;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationExternalEntryImplTest extends AbstractMigrationTest {

    public static final String ENTRY_NAME = "Entry name";

    public static final String CHECKSUM = "Checksum";

    private final static Map<String, Object> METADATA_MAP =
            ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                    ENTRY_NAME,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    CHECKSUM,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    false);

    @Mock
    public ImportMigrationContextImpl mockContext;

    @Mock
    public PathUtils mockPathUtils;

    public ImportMigrationExternalEntryImpl entry;

    public Path path;

    public MigrationReport report;

    @Before
    public void setup() throws Exception {
        final File file = new File(ROOT.toFile(), "testname");

        FileUtils.writeStringToFile(file, file.getName(), Charsets.UTF_8);
        path = file.toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        when(mockContext.getPathUtils()).thenReturn(mockPathUtils);

        report = new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
        when(mockContext.getReport()).thenReturn(report);

        when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(path);
        when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn(CHECKSUM);

        entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
    }

    @Test
    public void getLastModifiedTime() {
        assertThat(entry.getLastModifiedTime(), equalTo(-1L));
    }

    @Test
    public void getInputStream() throws Exception {
        assertThat(entry.getInputStream(), equalTo(Optional.empty()));
    }

    @Test
    public void storeSuccessfullyWithMatchingChecksum() throws Exception {
        entry.store(true);

        verify(mockPathUtils).getChecksumFor(any(Path.class));
        assertThat("Entry was verified successfully.", entry.stored, is(true));
    }

    @Test
    public void storeSuccessfullyWithoutChecksum() throws Exception {
        entry = new ImportMigrationExternalEntryImpl(mockContext,
                ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                        ENTRY_NAME,
                        MigrationEntryImpl.METADATA_SOFTLINK,
                        false));
        entry.store(true);

        assertThat("Entry was verified successfully.", entry.stored, is(true));
    }

    @Test
    public void storeSuccessfullyWhenOptionalFileDoesNotExist() throws Exception {
        if (path.toFile()
                .delete()) {
            entry.store(false);

            verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
            assertThat("Entry was verified successfully.", entry.stored, is(true));
        } else {
            throw new AssertionError("Was unable to delete the file.");
        }
    }

    @Test
    public void storeFailsWhenRequiredFileDoesNotExist() throws Exception {
        if (path.toFile()
                .delete()) {
            entry.store(true);

            verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
            assertThat("Entry failed verification because it does not exist.",
                    entry.stored,
                    is(false));
            verifyReportHasMatchingError(report,
                    ImportPathMigrationException.class,
                    "doesn't exist");
        } else {
            throw new AssertionError("Was unable to delete the file.");
        }
    }

    @Test
    public void storeFailsWhenFileIsNotSoftLink() throws Exception {
        entry = new ImportMigrationExternalEntryImpl(mockContext,
                ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                        ENTRY_NAME,
                        MigrationEntryImpl.METADATA_SOFTLINK,
                        true));
        entry.store(true);

        verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
        assertThat("Entry failed verification because it was not a symbolic link.",
                entry.stored,
                is(false));
        verifyReportHasMatchingWarning(report,
                ImportPathMigrationWarning.class,
                "not a symbolic link");
    }

    @Test
    public void storeRecordsWarningWhenFileIsNotNormal() throws Exception {
        final File file = new File(ROOT.toFile(), "symlink");

        Path symlink = DDF_HOME.resolve(createSoftLink("symlink", path));

        when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

        entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
        entry.store(true);

        verify(mockPathUtils).getChecksumFor(any(Path.class));
        assertThat("Entry was verified successfully.", entry.stored, is(true));
        verifyReportHasMatchingWarning(report,
                ImportPathMigrationWarning.class,
                "is not a regular file");
    }

    @Test
    public void storeFailsWhenChecksumDoesNotMatch() throws Exception {
        when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn("Different-Checksum");

        entry.store(true);

        verify(mockPathUtils).getChecksumFor(any(Path.class));
        assertThat("Entry failed verification because the checksum didn't match.",
                entry.stored,
                is(false));
        verifyReportHasMatchingWarning(report,
                ImportPathMigrationWarning.class,
                "checksum doesn't match");
    }

    @Test
    public void storeFailsWhenChecksumCheckThrowsIOException() throws Exception {
        when(mockPathUtils.getChecksumFor(any(Path.class))).thenThrow(IOException.class);

        entry.store(true);

        verify(mockPathUtils).getChecksumFor(any(Path.class));
        assertThat("Entry failed verification because of an IOException.", entry.stored, is(false));
        verifyReportHasMatchingWarning(report,
                ImportPathMigrationWarning.class,
                "checksum could not be calculated");
    }
}

