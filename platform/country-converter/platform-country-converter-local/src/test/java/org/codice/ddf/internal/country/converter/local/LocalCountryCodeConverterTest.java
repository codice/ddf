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
package org.codice.ddf.internal.country.converter.local;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class LocalCountryCodeConverterTest {

    private LocalCountryCodeConverter localCountryCodeConverter = new LocalCountryCodeConverter();

    @Before
    public void setup() {
        localCountryCodeConverter.setCountryCodeMappingsFile("fipsToIsoTest.properties");
    }

    @Test
    public void testFipstoIso3Conversion() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("RS");
        assertThat(iso3.get(0), is("RUS"));
    }

    @Test
    public void testFipsToIso3MultipleCountryCodesConversion() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("ZZ");
        assertThat(iso3, containsInAnyOrder("ZZZ", "QQQ"));
    }

    @Test
    public void testFipsToIsoInvalidFilePath() {
        localCountryCodeConverter.setCountryCodeMappingsFile("invalid file path");
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("ZZ");
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testFipsToIso3ConversionInvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3("11");
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testFipsToIso3ConversionNullCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertFipsToIso3(null);
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testIso3ToFipsConversion() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips("RUS");
        assertThat(fipsCountryCodes, containsInAnyOrder("RS"));
    }

    @Test
    public void testIso3ToFipsMultipleCountryCodesConversion() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips("PSE");
        assertThat(fipsCountryCodes, containsInAnyOrder("WE", "GZ"));
    }

    @Test
    public void testIso3ToFipsConversionInvalidCountryCode() {
        List<String> iso3 = localCountryCodeConverter.convertIso3ToFips("111");
        assertThat(iso3.isEmpty(), is(true));
    }

    @Test
    public void testIso3toFipsConversionNullCountryCode() {
        List<String> fipsCountryCodes = localCountryCodeConverter.convertIso3ToFips(null);
        assertThat(fipsCountryCodes.isEmpty(), is(true));
    }
}
