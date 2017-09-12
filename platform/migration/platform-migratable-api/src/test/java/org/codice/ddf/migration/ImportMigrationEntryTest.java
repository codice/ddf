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
package org.codice.ddf.migration;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class ImportMigrationEntryTest {
    private final ImportMigrationEntry ENTRY = Mockito.mock(ImportMigrationEntry.class,
            Mockito.CALLS_REAL_METHODS);

    @Test
    public void testRestoreReturnsFalse() throws Exception {
        Mockito.when(ENTRY.restore(Mockito.eq(true)))
                .thenReturn(false);

        Assert.assertThat(ENTRY.restore(), Matchers.equalTo(false));

        Mockito.verify(ENTRY)
                .restore(true);
    }

    @Test
    public void testRestoreReturnsTrue() throws Exception {
        Mockito.when(ENTRY.restore(Mockito.eq(true)))
                .thenReturn(true);

        Assert.assertThat(ENTRY.restore(), Matchers.equalTo(true));

        Mockito.verify(ENTRY)
                .restore(true);
    }
}
