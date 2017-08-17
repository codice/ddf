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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;

//TODO: This unit test class is NOT complete.
@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationJavaPropertyReferencedEntryImplTest extends AbstractMigrationTest {

    public static final String PROPERTIES_PATH = "Properties path";

    private final static Map<String, Object> METADATA_MAP =
            ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                    PROPERTIES_PATH,
                    MigrationEntryImpl.METADATA_REFERENCE,
                    "Reference name",
                    MigrationEntryImpl.METADATA_PROPERTY,
                    "Property value");

    @Mock
    public ImportMigrationContextImpl mockContext;

    @Mock
    public PathUtils mockPathUtils;

    public ImportMigrationJavaPropertyReferencedEntryImpl entry;

    public Path path;

    public MigrationReport report;

    @Before
    public void setup() throws Exception {
        final File file = new File(ROOT.toFile(), "testname");

        FileUtils.writeStringToFile(file, file.getName(), Charsets.UTF_8);
        path = file.toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        report = new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

        when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(path);
//        when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn("Checksum");

        when(mockContext.getPathUtils()).thenReturn(mockPathUtils);
//        when(mockContext.getReport()).thenReturn(report);
        when(mockContext.getOptionalEntry(any(Path.class))).thenReturn(Optional.of(mock(
                ImportMigrationEntry.class)));

        entry = new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
    }

    @Test
    public void getPropertiesPath() {
        assertThat(entry.getPropertiesPath().toString(), equalTo(PROPERTIES_PATH));
    }

    @Test
    public void hashCodeShouldBeEqual() {
        ImportMigrationJavaPropertyReferencedEntryImpl entry2 = new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
        assertThat(entry.hashCode(), equalTo(entry2.hashCode()));
    }

    @Test
    public void hashCodeShouldNotBeEqualWithDifferentPropertiesPath() {
        Map<String, Object> metadataMap =
                ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                        "Different properties path",
                        MigrationEntryImpl.METADATA_REFERENCE,
                        "Reference name",
                        MigrationEntryImpl.METADATA_PROPERTY,
                        "Property value");
        ImportMigrationJavaPropertyReferencedEntryImpl entry2 = new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, metadataMap);
        assertThat(entry.hashCode(), not(equalTo(entry2.hashCode())));
    }

    @Test
    public void shouldBeEqual() {
        ImportMigrationJavaPropertyReferencedEntryImpl entry2 = new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
        assertThat("The entries are equal", entry.equals(entry2), is(true));
    }

    @Test
    public void shouldNotBeEqualBecauseSuperIsNotEqual() {
        assertThat("The entries are not equal", entry.equals(null), is(false));
    }
}


