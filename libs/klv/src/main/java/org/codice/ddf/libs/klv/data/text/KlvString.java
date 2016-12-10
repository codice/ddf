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
package org.codice.ddf.libs.klv.data.text;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.codice.ddf.libs.klv.KlvDataElement;
import org.codice.ddf.libs.klv.data.Klv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a KLV data element that has a <strong>String</strong> value.
 */
public class KlvString extends KlvDataElement<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KlvString.class);

    private final String encoding;

    /**
     * Constructs a {@code KlvString} representing a KLV data element that has a
     * <strong>String</strong> value encoded as UTF-8.
     *
     * @param key  the data element's key
     * @param name a name describing the data element's value
     */
    public KlvString(final byte[] key, final String name) {
        this(key, name, StandardCharsets.UTF_8.name());
    }

    /**
     * Constructs a {@code KlvString} representing a KLV data element that has a
     * <strong>String</strong> value encoded in the given encoding type.
     *
     * @param key      the data element's key
     * @param name     a name describing the data element's value
     * @param encoding the string's encoding method
     */
    public KlvString(final byte[] key, final String name, final String encoding) {
        super(key, name);
        this.encoding = encoding;
    }

    @Override
    protected void decodeValue(final Klv klv) {
        try {
            value = klv.getValueAsString(encoding);
        } catch (UnsupportedEncodingException e1) {
            LOGGER.debug(
                    "Couldn't retrieve string value from KLV using encoding {}. Attempting to use the platform's default charset.",
                    encoding,
                    e1);
            try {
                value = klv.getValueAsString(Charset.defaultCharset()
                        .name());
            } catch (UnsupportedEncodingException e2) {
                value = null;
                LOGGER.debug(
                        "Couldn't retrieve string value from KLV using the platform's default encoding {}. Setting this KlvString's (name: {}) value to null.",
                        Charset.defaultCharset()
                                .name(),
                        name,
                        e2);
            }
        }
    }

    @Override
    protected KlvDataElement copy() {
        return new KlvString(keyBytes, name, encoding);
    }
}
