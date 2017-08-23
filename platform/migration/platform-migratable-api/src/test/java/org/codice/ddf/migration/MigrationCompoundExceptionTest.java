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

import java.util.Arrays;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MigrationCompoundExceptionTest {
    private static final MigrationException EXCEPTION1 = new MigrationException("test1");

    private static final MigrationException EXCEPTION2 = new MigrationException("test2");

    private static final MigrationException EXCEPTION3 = new MigrationException("test3");

    private final MigrationCompoundException EXCEPTION =
            new MigrationCompoundException(Arrays.asList(EXCEPTION1, EXCEPTION2, EXCEPTION3)
                    .iterator());

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(EXCEPTION.getMessage(), Matchers.equalTo(EXCEPTION1.getMessage()));
        Assert.assertThat(EXCEPTION.getCause(), Matchers.sameInstance(EXCEPTION1));
        Assert.assertThat(EXCEPTION.getSuppressed(),
                Matchers.arrayContaining(Matchers.sameInstance(EXCEPTION2),
                        Matchers.sameInstance(EXCEPTION3)));
    }

    @Test
    public void testConstructorWithOnlyOneException() throws Exception {
        final MigrationCompoundException EXCEPTION =
                new MigrationCompoundException(Collections.singletonList(EXCEPTION1)
                        .iterator());

        Assert.assertThat(EXCEPTION.getMessage(), Matchers.equalTo(EXCEPTION1.getMessage()));
        Assert.assertThat(EXCEPTION.getCause(), Matchers.sameInstance(EXCEPTION1));
        Assert.assertThat(EXCEPTION.getSuppressed(), Matchers.emptyArray());
    }

    @Test
    public void testConstructorWithNullErrors() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null errors"));

        new MigrationCompoundException(null);
    }

    @Test
    public void testConstructorWithNoErrors() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("missing errors"));

        new MigrationCompoundException(Collections.emptyIterator());
    }
}
