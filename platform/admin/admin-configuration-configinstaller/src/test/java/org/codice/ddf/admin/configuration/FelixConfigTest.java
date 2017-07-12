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
package org.codice.ddf.admin.configuration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class FelixConfigTest {
    private static final String FELIX_FILENAME_PROP = "felix.fileinstall.filename";

    private static final String MALFORMED_URL = "htp:/www.google.com";

    private static final String URL_BUT_NOT_URI = "http:// ";

    private static final Dictionary<String, Object> PROPS_NO_FELIX = new Hashtable<>();

    private static final Dictionary<String, Object> PROPS_WITH_FELIX = new Hashtable<>();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    public File temporaryFile;

    @Mock
    private Configuration configuration;

    private FelixConfig felixConfig;

    @BeforeClass
    public static void beforeClass() {
        PROPS_NO_FELIX.put("prop1", "value1");
        PROPS_NO_FELIX.put("prop2", "value2");
        PROPS_WITH_FELIX.put("prop1", "value1");
        PROPS_WITH_FELIX.put("prop2", "value2");
    }

    @Before
    public void before() throws Exception {
        temporaryFile = temporaryFolder.newFile();
        when(configuration.getProperties()).thenReturn(PROPS_WITH_FELIX);
    }

    @Test
    public void testExtractFelixPropWhenPropMapNull() {
        when(configuration.getProperties()).thenReturn(null);
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }

    @Test
    public void testExtractFelixPropWhenFelixPropNotFound() {
        when(configuration.getProperties()).thenReturn(PROPS_NO_FELIX);
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }

    @Test
    public void testFileFromURL() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP,
                temporaryFile.toURI()
                        .toURL());
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(notNullValue()));
        assertThat(felixConfig.getFelixFile(), is(temporaryFile));
    }

    @Test
    public void testFileFromURI() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, temporaryFile.toURI());
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(notNullValue()));
        assertThat(felixConfig.getFelixFile(), is(temporaryFile));
    }

    @Test
    public void testFileFromString() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, "file:" + temporaryFile.getAbsolutePath());
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(notNullValue()));
        assertThat(felixConfig.getFelixFile(), is(temporaryFile));
    }

    @Test
    public void testFileFromURLwithBadURISyntax() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, new URL(URL_BUT_NOT_URI));
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }

    @Test
    public void testFileFromMalformedURLstring() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, MALFORMED_URL);
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }

    @Test
    public void testFileFromStringWithBadURISyntax() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, URL_BUT_NOT_URI);
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }

    @Test
    public void testFileFromUnexpectedType() throws Exception {
        PROPS_WITH_FELIX.put(FELIX_FILENAME_PROP, new Object());
        felixConfig = new FelixConfig(configuration);
        assertThat(felixConfig.getFelixFile(), is(nullValue()));
    }
}
