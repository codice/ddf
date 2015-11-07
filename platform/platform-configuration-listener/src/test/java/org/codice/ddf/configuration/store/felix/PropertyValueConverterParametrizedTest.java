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

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class PropertyValueConverterParametrizedTest {

    @Parameterized.Parameters(name = "propertyValue: {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // Valid single values
                {"\"\"", "\"c\""}, {"\"123\"", "\"c123\""}, {"\"123,456\"", "\"c123,456\""},
                // Invalid single values
                {"\"123", "\"123"}, {"123\"", "123\""}, {"123", "123"},
                // Valid arrays with single value
                {"[\"\"]", "[\"c\"]"}, {"[\"123\"]", "[\"c123\"]"}, {"[ \"123\"]", "[\"c123\"]"},
                {"[\"123\" ]", "[\"c123\"]"},
                // Invalid arrays with single value
                {"[\"123]", "[\"123]"}, {"[123\"]", "[123\"]"}, {"[123]", "[123]"},
                {"[\"123\"][", "[\"123\"]["}, {"][\"123\"]", "][\"123\"]"},
                {"][\"123\"][", "][\"123\"]["},
                // Valid arrays with multiple values
                {"[\"123\",\"345\"]", "[\"c123\",\"c345\"]"},
                {"[ \"123\" , \"345\" ]", "[\"c123\",\"c345\"]"},
                {"[\"123\",\"345\",\"567\"]", "[\"c123\",\"c345\",\"c567\"]"},
                {"[ \"123\" , \"345\" , \"567\" ]", "[\"c123\",\"c345\",\"c567\"]"},
                // Invalid arrays with multiple values
                {"[\"123\",345\"]", "[\"123\",345\"]"}, {"[\"123\",\"345]", "[\"123\",\"345]"},
                {"[\"123,\"345\"]", "[\"123,\"345\"]"}, {"[123\",\"345\"]", "[123\",\"345\"]"},
                {"[\"123\",345\",\"567\"]", "[\"123\",345\",\"567\"]"},
                // Valid vectors with single value
                {"(\"\")", "(\"c\")"}, {"(\"123\")", "(\"c123\")"}, {"( \"123\")", "(\"c123\")"},
                {"(\"123\" )", "(\"c123\")"},
                // Invalid vectors with single value
                {"(\"123)", "(\"123)"}, {"(123\")", "(123\")"}, {"(123)", "(123)"},
                {"(\"123\")(", "(\"123\")("}, {")(\"123\")", ")(\"123\")"},
                {")(\"123\")(", ")(\"123\")("},
                // Valid vectors with multiple values
                {"(\"123\",\"345\")", "(\"c123\",\"c345\")"},
                {"( \"123\" , \"345\" )", "(\"c123\",\"c345\")"},
                {"(\"123\",\"345\",\"567\")", "(\"c123\",\"c345\",\"c567\")"},
                {"( \"123\" , \"345\" , \"567\" )", "(\"c123\",\"c345\",\"c567\")"},
                // Invalid vectors with multiple values
                {"(\"123\",345\")", "(\"123\",345\")"}, {"(\"123\",\"345)", "(\"123\",\"345)"},
                {"(\"123,\"345\")", "(\"123,\"345\")"}, {"(123\",\"345\")", "(123\",\"345\")"},
                {"(\"123\",345\",\"567\")", "(\"123\",345\",\"567\")"}});
    }

    @Parameterized.Parameter
    public String propertyValue;

    @Parameterized.Parameter(1)
    public String convertedValue;

    private static class PropertyValueConverterUnderTest extends PropertyValueConverter {

        @Override
        protected void convertSingleValue(String value, StringBuilder output) {
            // Output the value with a "c" in front of it to indicate that this method was called.
            output.append('c').append(value);
        }
    }

    @Test
    public void convert() {
        StringBuilder output = new StringBuilder();

        PropertyValueConverterUnderTest propertyValueConverter = new PropertyValueConverterUnderTest();
        propertyValueConverter.convert(propertyValue, output);

        assertThat(output.toString(), equalTo(convertedValue));
    }
}
