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

import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationEntryImplTest extends AbstractMigrationTest {
    private static final String UNIX_NAME = "path/path2/file.ext";

    private static final String WINDOWS_NAME = "path\\path2\\file.ext";

    private static final String MIXED_NAME = "path\\path2/file.ext";

    private static final Path FILE_PATH = Paths.get(UNIX_NAME);

    private final ExportMigrationContextImpl CONTEXT = Mockito.mock(ExportMigrationContextImpl.class);

    //private final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, PA)
    @Test
    public void testConstructor() throws Exception {

    }
}
