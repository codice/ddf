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
package org.codice.ddf.libs.klv;

import java.util.Arrays;

import javax.xml.bind.DatatypeConverter;

import org.codice.ddf.libs.klv.data.Klv;

import com.google.common.base.Preconditions;

/**
 * Describes how to interpret the value of a certain data element.
 */
public abstract class KlvDataElement<T> {
    protected final byte[] keyBytes;

    protected final String key;

    protected final String name;

    protected T value;

    /**
     * Constructs a {@code KlvDataElement} that describes how to interpret the value of a data
     * element with the given key.
     *
     * @param key  the data element's key
     * @param name a name describing the data element's value
     * @throws IllegalArgumentException if any arguments are null
     */
    public KlvDataElement(final byte[] key, final String name) {
        Preconditions.checkArgument(key != null, "The key cannot be null.");
        Preconditions.checkArgument(name != null, "The name cannot be null.");

        keyBytes = Arrays.copyOf(key, key.length);
        this.key = DatatypeConverter.printHexBinary(key);
        this.name = name;
    }

    public final byte[] getKey() {
        return Arrays.copyOf(keyBytes, keyBytes.length);
    }

    protected final String getKeyAsString() {
        return key;
    }

    public final String getName() {
        return name;
    }

    protected abstract void decodeValue(Klv klv);

    public T getValue() {
        return value;
    }

    protected abstract KlvDataElement copy();

    /**
     * If the data element was encoded with an error indicator value and the value matches that
     * indicator, then this method will return {@code true}. Child classes should override the
     * default implementation if they support error indicator values.
     *
     * @return true if error was encoded
     */
    public boolean isErrorIndicated() {
        return false;
    }

}
