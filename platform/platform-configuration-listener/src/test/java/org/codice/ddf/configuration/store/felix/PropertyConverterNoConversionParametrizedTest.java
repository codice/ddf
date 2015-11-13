/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.store.felix;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableMap;

@RunWith(Parameterized.class)
public class PropertyConverterNoConversionParametrizedTest {

    @Parameterized.Parameters(name = "line: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{""}, {"key1"}, {"key1="}, {"key1=F"}, {"key1=B"},
                {"key1=*"}, {"key1= F"}, {"key1=F \"10.0\""}, {"key1= F\"10.0\""}, {"="}, {"=F"},
                {"=F\"1.1\""}, {"=B\"true\""}, {"key1=\"value\""}, {"key1=B\"true\""},
                {"InvalidK&ey=F\"1.1\""}, {"Invalid Key=F\"1.1\""}, {"invalid line"}});
    }

    @Parameterized.Parameter
    public String line;

    private static final String NEW_LINE = "\r\n";

    private PropertyValueConverter valueConverter;

    private StringBuilder filteredOutput = new StringBuilder();

    private PropertyConverter propertyConverter;

    @Before
    public void setUp() {
        valueConverter = mock(PropertyValueConverter.class);
        propertyConverter = new PropertyConverter(filteredOutput);
        Map<String, PropertyValueConverter> valueConverters = ImmutableMap.of("f", valueConverter);
        propertyConverter.setValueConverters(valueConverters);
    }

    @Test
    public void testNoConversion() {

        propertyConverter.accept(line);

        assertThat(filteredOutput.toString(), equalTo(line + NEW_LINE));
        verify(valueConverter, never()).convert(anyString(), anyObject());
    }
}