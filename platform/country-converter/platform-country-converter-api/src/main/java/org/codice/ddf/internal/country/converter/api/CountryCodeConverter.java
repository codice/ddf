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
package org.codice.ddf.internal.country.converter.api;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Service to convert country codes into various formats.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * <p>
 */
public interface CountryCodeConverter {

    /**
     * Converts FIPS 10-4 into ISO 3166-1 alpha 3. If a FIPS 10-4 code maps to more than one
     * ISO 3166-1 alpha 3 code, all of them will be returned in a list of strings.
     *
     * @param fipsCountryCode a FIPS 10-4 country code
     * @return an ISO 3166-1 alpha-3 country code in a List<String> or an empty list if there isn't
     * a valid conversion.
     */
    List<String> convertFipsToIso3(@Nullable String fipsCountryCode);

    /**
     * Converts ISO 3166-1 alpha 3 into FIPS 10-4. If an ISO 3166-1 alpha 3 code maps to more than
     * one FIPS 10-4 code, all of them will be returned in a list of strings.
     *
     * @param iso3alphaCountryCode an ISO 3166 alpha 3 country code
     * @return an ISO 3166-1 country code in a List<String> or an empty list if there isn't a valid
     * conversion.
     */
    List<String> convertIso3ToFips(@Nullable String iso3alphaCountryCode);
}
