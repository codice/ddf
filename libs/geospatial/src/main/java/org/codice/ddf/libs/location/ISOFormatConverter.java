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
 **/
package org.codice.ddf.libs.location;

import java.util.Locale;

/**
 * Converts a country code from a ISO 3166-1 alpha-2 format to an ISO 3166-1 alpha 3 format
 */
public class ISOFormatConverter {

    public static final String ENGLISH_LANG = "en";

    /**
     * @param language          the language to return the converted country code in
     * @param alpha2CountryCode ISO 3166-1 alpha-2 formatted country code
     * @return an ISO 3166-1 alpha-3 country code
     */
    public static String convert(String language, String alpha2CountryCode) {
        Locale locale = new Locale(language, alpha2CountryCode);
        return locale.getISO3Country();
    }
}
