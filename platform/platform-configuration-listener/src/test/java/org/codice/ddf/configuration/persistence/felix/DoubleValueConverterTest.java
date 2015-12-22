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

import org.junit.Test;

public class DoubleValueConverterTest {

    @Test
    public void convert() {
        StringBuilder output = new StringBuilder();
        Double value = 12.55d;

        DoubleValueConverter valueConverter = new DoubleValueConverter();
        valueConverter.convertSingleValue(value.toString(), output);

        assertThat(output.toString(), equalTo(String.valueOf(Double.doubleToLongBits(value))));
    }

    @Test
    public void convertInvalidDouble() {
        StringBuilder output = new StringBuilder();
        String value = "12.2.2";

        DoubleValueConverter valueConverter = new DoubleValueConverter();
        valueConverter.convertSingleValue(value, output);

        assertThat(output.toString(), equalTo(value));
    }
}
