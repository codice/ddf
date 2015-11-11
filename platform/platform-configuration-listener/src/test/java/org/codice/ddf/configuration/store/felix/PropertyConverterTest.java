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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class PropertyConverterTest {

    private static final String NEW_LINE = "\r\n";

    @Mock
    private PropertyValueConverter valueConverter;

    private StringBuilder filteredOutput = new StringBuilder();

    private PropertyConverter propertyConverter;

    @Before
    public void setUp() {
        propertyConverter = new PropertyConverter(filteredOutput);
        Map<String, PropertyValueConverter> valueConverters = ImmutableMap.of("f", valueConverter);
        propertyConverter.setValueConverters(valueConverters);
    }

    @Test
    public void acceptWithEmptyLine() {
        testNoConversion("");
    }

    @Test
    public void acceptLineWithNoTypeOrValue() {
        testNoConversion("key=");
    }

    @Test
    public void acceptLineWithTypeToConvertButNoValue() {
        testNoConversion("key=F");
    }

    @Test
    public void acceptLineWithTypeButNoValue() {
        testNoConversion("key=B");
    }

    @Test
    public void acceptLineWithTypeToConvertButNoKey() {
        testNoConversion("=F\"1.1\"");
    }

    @Test
    public void acceptLineWithNoKey() {
        testNoConversion("=B\"true\"");
    }

    @Test
    public void acceptLineWithNoType() {
        testNoConversion("key1=\"value\"");
    }

    @Test
    public void acceptLineWithTypeThatDoesNotNeedConversion() {
        testNoConversion("key1=B\"true\"");
    }

    @Test
    public void acceptLineWithInvalidFormat() {
        String line = "some invalid line";

        propertyConverter.accept(line);

        assertThat(filteredOutput.toString(), equalTo(line + NEW_LINE));
        verify(valueConverter, never()).convert(anyString(), anyObject());
    }

    @Test
    public void acceptLineWithLowerCaseTypeThatNeedsConversion() {
        testConversion("f");
    }

    @Test
    public void acceptLineWithUpperCaseTypeThatNeedsConversion() {
        testConversion("F");
    }

    private void testNoConversion(String line) {

        propertyConverter.accept(line);

        assertThat(filteredOutput.toString(), equalTo(line + NEW_LINE));
        verify(valueConverter, never()).convert(anyString(), anyObject());
    }

    private void testConversion(String type) {
        String key = "key1=" + type;
        String value = "\"1.1\"";
        String line = key + value;

        propertyConverter.accept(line);

        // Since the value converter is mocked out, no converted value will be added to the filtered
        // output so we need to assert it only contains the key and type, no value.
        assertThat(filteredOutput.toString(), equalTo(key + NEW_LINE));
        verify(valueConverter).convert(eq(value), any(StringBuilder.class));
    }
}
