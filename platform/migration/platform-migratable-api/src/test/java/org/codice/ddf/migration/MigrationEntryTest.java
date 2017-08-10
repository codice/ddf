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

public class MigrationEntryTest {
    private static final String PROPERTY_NAME = "test.property";

    private final MigrationEntry ENTRY = Mockito.mock(MigrationEntry.class,
            Mockito.CALLS_REAL_METHODS);

    @Test
    public void testStoreReturnsFalse() throws Exception {
        Mockito.when(ENTRY.store(Mockito.eq(true)))
                .thenReturn(false);

        Assert.assertThat(ENTRY.store(), Matchers.equalTo(false));

        Mockito.verify(ENTRY)
                .store(true);
    }

    @Test
    public void testStoreReturnsTrue() throws Exception {
        Mockito.when(ENTRY.store(Mockito.eq(true)))
                .thenReturn(true);

        Assert.assertThat(ENTRY.store(), Matchers.equalTo(true));

        Mockito.verify(ENTRY)
                .store(true);
    }
}
