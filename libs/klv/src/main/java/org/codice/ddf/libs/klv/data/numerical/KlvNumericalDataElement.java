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
package org.codice.ddf.libs.klv.data.numerical;

import java.util.Optional;

import org.codice.ddf.libs.klv.KlvDataElement;
import org.codice.ddf.libs.klv.data.Klv;

import com.google.common.base.Preconditions;

/**
 * Represents a data element with a numerical value.
 */
public abstract class KlvNumericalDataElement<T extends Number> extends KlvDataElement<T> {

    protected final Optional<T> errorIndicatorValue;

    /**
     * Constructs a {@code KlvNumericalDataElement} that describes how to interpret the value of a
     * numerical element with the given key.
     *
     * @param key  the data element's key
     * @param name a name describing the data element's value
     * @throws IllegalArgumentException if any arguments are null
     */
    public KlvNumericalDataElement(final byte[] key, final String name) {
        this(key, name, Optional.empty());
    }

    /**
     * Constructs a {@code KlvNumericalDataElement} that describes how to interpret the value of a
     * numerical element with the given key.
     *
     * @param key                 the data element's key
     * @param name                a name describing the data element's value
     * @param errorIndicatorValue value that indicates an encoded error
     * @throws IllegalArgumentException if any arguments are null
     */
    public KlvNumericalDataElement(final byte[] key, final String name,
            Optional<T> errorIndicatorValue) {
        super(key, name);
        Preconditions.checkArgument(errorIndicatorValue != null, "The errorIndicatorValue cannot be null.");
        this.errorIndicatorValue = errorIndicatorValue;
    }

    @Override
    protected abstract void decodeValue(Klv klv);

    @Override
    protected abstract KlvNumericalDataElement<T> copy();

    /**
     * If an error indicator value is present, then compare it to the decoded value.
     *
     * @return true if error was encoded
     */
    @Override
    public boolean isErrorIndicated() {
        return errorIndicatorValue.isPresent() && errorIndicatorValue.get()
                .equals(value);
    }
}
