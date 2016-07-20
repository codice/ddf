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
package org.codice.ddf.configuration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Test;

public class AbsolutePathResolverTest {

    @Test
    public void testNullPath() {
        System.clearProperty("ddf.home");
        assertThat(new AbsolutePathResolver(null).getPath(), nullValue());
    }

    @Test
    public void ddfHomeNotSet() {
        System.clearProperty("ddf.home");
        assertThat(new AbsolutePathResolver(Paths.get("test", "path").toString()).getPath(),
                equalTo(Paths.get(System.getProperty("user.dir"), "test", "path").toString()));
    }

    @Test
    public void testTransformedPath() {
        System.setProperty("ddf.home", Paths.get(System.getProperty("user.dir"), "testTransformedPath").toString());
        assertThat(new AbsolutePathResolver(Paths.get("test", "path").toString()).getPath(),
                equalTo(Paths.get(System.getProperty("user.dir"), "testTransformedPath", "test", "path").toString()));
    }

    @Test
    public void trailingFileSeparator() {
        System.setProperty("ddf.home", Paths.get(System.getProperty("user.dir"), "trailingFileSeparator").toString() + File.separator);
        assertThat(new AbsolutePathResolver("test" + File.separator + "path" + File.separator).getPath(),
                equalTo(Paths.get(System.getProperty("user.dir"), "trailingFileSeparator", "test", "path").toString() + File.separator));

    }
}
