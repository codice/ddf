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
 **/
package org.codice.ddf.platform.io;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.common.collect.Lists;

@RunWith(JUnit4.class)
public class ConfigStrategyTest {

    private PersistenceStrategy strategy = new ConfigStrategy();

    private ByteArrayOutputStream outputStream;

    private Dictionary<String, Object> data;

    @Before
    public void before() throws Exception {
        outputStream = new ByteArrayOutputStream();
        data = new Hashtable<>();
    }

    @After
    public void after() throws Exception {
        outputStream.close();
    }

    @Test
    public void testGetExtension() {
        assertThat(strategy.getExtension(), is("config"));
    }

    @Test
    public void testReadWriteInts() throws Exception {
        runTestWithValues(1, 2);
    }

    @Test
    public void testReadWriteChars() throws Exception {
        runTestWithValues('a', 'b');
    }

    @Test
    public void testReadWriteStrings() throws Exception {
        runTestWithValues("aa", "bb");
    }

    @Test
    public void testReadWriteBooleans() throws Exception {
        runTestWithValues(true, false);
    }

    @Test
    public void testReadWriteLists() throws Exception {
        runTestWithValues(Lists.asList("aa", new String[] {"ab"}),
                Lists.asList("ba", new String[] {"bb"}));
    }

    private void runTestWithValues(Object val1, Object val2) throws Exception {
        data.put("key1", val1);
        data.put("key2", val2);
        strategy.write(outputStream, data);
        InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        Dictionary<String, Object> results = strategy.read(inputStream);
        inputStream.close();
        // For .config the type will be preserved (except for new Object()'s which still are strings)
        assertThat(results.get("key1"), is(val1));
        assertThat(results.get("key2"), is(val2));
    }
}
