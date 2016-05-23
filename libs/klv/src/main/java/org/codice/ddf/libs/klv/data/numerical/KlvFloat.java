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
package org.codice.ddf.libs.klv.data.numerical;

import java.util.Optional;

import org.codice.ddf.libs.klv.data.Klv;

/**
 * Represents a KLV data element that has a <strong>float</strong> value.
 */
public class KlvFloat extends KlvNumericalDataElement<Float> {
    /**
     * Constructs a {@code KlvFloat} representing a KLV data element that has a
     * <strong>float</strong> value.
     *
     * @param key  the data element's key
     * @param name a name describing the data element's value
     */
    public KlvFloat(final byte[] key, final String name) {
        super(key, name);
    }

    /**
     * Constructs a {@code KlvFloat} representing a KLV data element that has a
     * <strong>float</strong> value.
     *
     * @param key  the data element's key
     * @param name a name describing the data element's value
     * @param errorIndicatorValue value that indicates an encoded error
     */
    public KlvFloat(final byte[] key, final String name, Optional<Float> errorIndicatorValue) {
        super(key, name, errorIndicatorValue);
    }

    @Override
    protected void decodeValue(final Klv klv) {
        value = klv.getValueAsFloat();
    }

    @Override
    protected KlvFloat copy() {
        return new KlvFloat(keyBytes, name, errorIndicatorValue);
    }
}
