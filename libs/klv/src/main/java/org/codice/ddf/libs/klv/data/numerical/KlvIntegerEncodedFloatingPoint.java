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

import org.codice.ddf.libs.klv.data.Klv;

import com.google.common.base.Preconditions;

/**
 * Represents a KLV element containing a floating-point value that has been encoded as an integer
 * value.
 */
public class KlvIntegerEncodedFloatingPoint extends KlvNumericalDataElement<Double> {
    private final KlvNumericalDataElement<?> klvRawDataValue;

    private final long encodedRangeMin;

    private final long encodedRangeMax;

    private final double actualRangeMin;

    private final double actualRangeMax;

    /**
     * Constructs a {@code KlvIntegerEncodedFloatingPoint} representing a KLV element containing a
     * floating-point value that has been encoded as an integer value.
     * <p/>
     * The value returned by this {@code KlvIntegerEncodedFloatingPoint} is calculated as follows:
     * <li>The value returned by {@code klvRawDataValue} is converted to a <strong>long</strong></li>
     * <li>Scale the value by calculating: (value - {@code encodedRangeMin}) / ({@code encodedRangeMax} - {@code encodedRangeMin})</li>
     * <li>Apply the scaled value to the actual range: scaled value * ({@code actualRangeMax} - {@code actualRangeMin}) + {@code actualRangeMin}</li>
     *
     * @param klvRawDataValue the {@link KlvNumericalDataElement} that retrieves the raw numerical value
     * @param encodedRangeMin the minimum integer-encoded value
     * @param encodedRangeMax the maximum integer-encoded value
     * @param actualRangeMin  the minimum decoded floating-point value
     * @param actualRangeMax  the maximum decoded floating-point value
     */
    public KlvIntegerEncodedFloatingPoint(final KlvNumericalDataElement<?> klvRawDataValue,
            final long encodedRangeMin, final long encodedRangeMax, final double actualRangeMin,
            final double actualRangeMax) {
        this(klvRawDataValue,
                encodedRangeMin,
                encodedRangeMax,
                actualRangeMin,
                actualRangeMax,
                Optional.empty());
    }

    /**
     * Constructs a {@code KlvIntegerEncodedFloatingPoint} representing a KLV element containing a
     * floating-point value that has been encoded as an integer value.
     * <p/>
     * The value returned by this {@code KlvIntegerEncodedFloatingPoint} is calculated as follows:
     * <li>The value returned by {@code klvRawDataValue} is converted to a <strong>long</strong></li>
     * <li>Scale the value by calculating: (value - {@code encodedRangeMin}) / ({@code encodedRangeMax} - {@code encodedRangeMin})</li>
     * <li>Apply the scaled value to the actual range: scaled value * ({@code actualRangeMax} - {@code actualRangeMin}) + {@code actualRangeMin}</li>
     *
     * @param klvRawDataValue     the {@link KlvNumericalDataElement} that retrieves the raw numerical value
     * @param encodedRangeMin     the minimum integer-encoded value
     * @param encodedRangeMax     the maximum integer-encoded value
     * @param actualRangeMin      the minimum decoded floating-point value
     * @param actualRangeMax      the maximum decoded floating-point value
     * @param errorIndicatorValue value that indicates an encoded error
     */
    public KlvIntegerEncodedFloatingPoint(final KlvNumericalDataElement<?> klvRawDataValue,
            final long encodedRangeMin, final long encodedRangeMax, final double actualRangeMin,
            final double actualRangeMax, Optional<Double> errorIndicatorValue) {
        super(klvRawDataValue.getKey(), klvRawDataValue.getName(), errorIndicatorValue);

        Preconditions.checkArgument(encodedRangeMax > encodedRangeMin,
                "The encoded value range is incorrect.");
        Preconditions.checkArgument(actualRangeMax > actualRangeMin,
                "The actual value range is incorrect.");

        this.klvRawDataValue = klvRawDataValue;
        this.encodedRangeMin = encodedRangeMin;
        this.encodedRangeMax = encodedRangeMax;
        this.actualRangeMin = actualRangeMin;
        this.actualRangeMax = actualRangeMax;
    }

    private static double convert(final long encodedValue, final long encodedRangeMin,
            final long encodedRangeMax, final double actualRangeMin, final double actualRangeMax) {
        final double scaledValue =
                ((double) (encodedValue - encodedRangeMin)) / (encodedRangeMax - encodedRangeMin);
        final double actualRange = actualRangeMax - actualRangeMin;
        return scaledValue * actualRange + actualRangeMin;
    }

    @Override
    protected void decodeValue(final Klv klv) {
        klvRawDataValue.decodeValue(klv);
        value = convert(klvRawDataValue.getValue()
                .longValue(), encodedRangeMin, encodedRangeMax, actualRangeMin, actualRangeMax);
    }

    @Override
    protected KlvIntegerEncodedFloatingPoint copy() {
        return new KlvIntegerEncodedFloatingPoint(klvRawDataValue.copy(),
                encodedRangeMin,
                encodedRangeMax,
                actualRangeMin,
                actualRangeMax,
                errorIndicatorValue);
    }

    /**
     * If there is a floating point error indicator, then use it. Otherwise, check the
     * raw data for an error indicator.
     *
     * @return true if an error indicator is encoded
     */
    @Override
    public boolean isErrorIndicated() {
        if (errorIndicatorValue.isPresent()) {
            return super.isErrorIndicated();
        }

        return klvRawDataValue != null && klvRawDataValue.isErrorIndicated();
    }
}
