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
package org.codice.ddf.configuration.persistence.felix;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FelixPersistenceStrategyTest {

    private Dictionary<String, Object> properties;

    private ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        properties = new Hashtable<>();
        properties.put("key1", "value1");
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void write() throws IOException {

        FelixPersistenceStrategy felixPersistenceStrategy = new FelixPersistenceStrategy();
        felixPersistenceStrategy.write(outputStream, properties);

        assertThat(outputStream.toString(), equalTo("key1=\"value1\"\r\n"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteWithNullOutputStream() throws IOException {
        new FelixPersistenceStrategy().write(null, properties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteWithNullProperties() throws IOException {
        new FelixPersistenceStrategy().write(outputStream, null);
    }
}
