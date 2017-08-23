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

import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.github.npathai.hamcrestopt.OptionalMatchers;

public class MigrationSuccessfulInformationTest {
    private static final String FORMAT = "test-%s";

    private static final String ARG = "message";

    private static final String MESSAGE = "test-message";

    private final MigrationSuccessfulInformation INFO = new MigrationSuccessfulInformation(MESSAGE);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testConstructorWithMessage() throws Exception {
        Assert.assertThat(INFO.getMessage(), Matchers.equalTo(MESSAGE));
    }

    @Test
    public void testConstructorWithNullMessage() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null message"));

        new MigrationSuccessfulInformation(null);
    }

    @Test
    public void testConstructorWithFormat() throws Exception {
        final MigrationSuccessfulInformation INFO = new MigrationSuccessfulInformation(FORMAT, ARG);

        Assert.assertThat(INFO.getMessage(), Matchers.equalTo(MESSAGE));
    }

    @Test
    public void testConstructorWithNullFormat() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null format message"));

        new MigrationSuccessfulInformation(null, ARG);
    }

    @Test
    public void testDowngradeToWarning() throws Exception {
        final Optional<MigrationWarning> warning = INFO.downgradeToWarning();

        Assert.assertThat(warning, OptionalMatchers.isEmpty());
    }

    @Test
    public void testToString() throws Exception {
        Assert.assertThat(INFO.toString(), Matchers.equalTo(MESSAGE));
    }
}
