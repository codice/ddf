/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.measure;

import static javax.measure.unit.NonSI.MILE;

import javax.measure.unit.NonSI;
import javax.measure.unit.SI;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

/**
 * This class currently relies on JScience 4.3.1 to perform all distance conversions.
 */
public final class Distance {

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(Distance.class));

    private double distanceInMeters = 0.0;

    /**
     * The Enum LinearUnit for the distance in meters
     */
    public enum LinearUnit {
        METER, KILOMETER, NAUTICAL_MILE, MILE, FOOT_U_S, YARD
    }

    /**
     * 
     * @param distance
     *            scalar value
     * @param unitOfMeasure
     *            the units of measure of the scalar distance
     */
    public Distance(double distance, LinearUnit unitOfMeasure) {

        this.distanceInMeters = getAsMeters(distance, unitOfMeasure);

    }

    private double getAsMeters(double distance, LinearUnit unitOfMeasure) {

        double convertedDistance = 0.0;

        if (unitOfMeasure == null) {
            return Double.valueOf(distance);
        }

        if (distance <= 0) {
            return Double.valueOf(0);
        }

        switch (unitOfMeasure) {
        case FOOT_U_S:
            convertedDistance = NonSI.FOOT_SURVEY_US.getConverterTo(SI.METER).convert(distance);
            break;
        case YARD:
            convertedDistance = NonSI.YARD.getConverterTo(SI.METER).convert(distance);
            break;
        case MILE:
            convertedDistance = NonSI.MILE.getConverterTo(SI.METER).convert(distance);
            break;
        case NAUTICAL_MILE:
            convertedDistance = NonSI.NAUTICAL_MILE.getConverterTo(SI.METER).convert(distance);
            break;
        case KILOMETER:
            convertedDistance = SI.KILOMETER.getConverterTo(SI.METER).convert(distance);
            break;
        case METER:
            convertedDistance = distance;
            break;
        default:
            throw new IllegalArgumentException("Invalid " + LinearUnit.class.getSimpleName()
                    + " for conversion.");
        }
        return convertedDistance;
    }

    public double getAs(LinearUnit unitOfMeasure) {

        double result = distanceInMeters;

        if (unitOfMeasure == null) {
            return Double.valueOf(distanceInMeters);
        }

        if (distanceInMeters <= 0) {
            return Double.valueOf(0);
        }

        switch (unitOfMeasure) {
        case METER:
            result = distanceInMeters;
            break;
        case KILOMETER:
            result = SI.METER.getConverterTo(SI.KILOMETER).convert(distanceInMeters);
            break;
        case FOOT_U_S:
            result = SI.METER.getConverterTo(NonSI.FOOT_SURVEY_US).convert(distanceInMeters);
            break;
        case MILE:
            result = SI.METER.getConverterTo(MILE).convert(distanceInMeters);
            break;
        case NAUTICAL_MILE:
            result = SI.METER.getConverterTo(NonSI.NAUTICAL_MILE).convert(distanceInMeters);
            break;
        case YARD:
            result = SI.METER.getConverterTo(NonSI.YARD).convert(distanceInMeters);
            break;
        default:
            LOGGER.warn("Could not convert distance units, assuming distance is in meters.");
            result = distanceInMeters;
            break;
        }

        return result;

    }

}
