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
package org.codice.ddf.configuration.persistence.felix;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableMap;

@RunWith(Parameterized.class)
public class PropertyConverterConversionParametrizedTest {

    @Parameterized.Parameters(name = "line: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {{"key1=F\"1.1\"", "key1=F", "\"1.1\""},
                {"key1=f\"1.1\"", "key1=f", "\"1.1\""}, {"key1=F[abc]", "key1=F", "[abc]"},
                {"key1=f[abc]", "key1=f", "[abc]"}, {"key1=F(abc)", "key1=F", "(abc)"},
                {"key1=f(abc)", "key1=f", "(abc)"}, {"key_1=F[abc]", "key_1=F", "[abc]"},
                {"key.abc=F[abc]", "key.abc=F", "[abc]"},
                {"key-abc=F[abc]", "key-abc=F", "[abc]"}});
    }

    @Parameterized.Parameter
    public String line;

    @Parameterized.Parameter(1)
    public String key;

    @Parameterized.Parameter(2)
    public String value;

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
    public void testConversion() {

        propertyConverter.accept(line);

        // Since the value converter is mocked out, no converted value will be added to the filtered
        // output so we need to assert it only contains the key and type, no value.
        assertThat(filteredOutput.toString(), equalTo(key + NEW_LINE));
        verify(valueConverter).convert(eq(value), any(StringBuilder.class));
    }
}
