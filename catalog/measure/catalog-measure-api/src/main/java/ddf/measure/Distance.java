/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.measure;

import static org.apache.commons.lang.Validate.notNull;

import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import si.uom.SI;
import systems.uom.common.USCustomary;

/** This class currently relies on JScience 4.3.1 to perform all distance conversions. */
public final class Distance {

  private static final Logger LOGGER = LoggerFactory.getLogger(Distance.class);

  private double distanceInMeters = 0.0;

  /**
   * @param distance scalar value
   * @param unitOfMeasure the units of measure of the scalar distance
   */
  public Distance(double distance, LinearUnit unitOfMeasure) {

    this.distanceInMeters = getAsMeters(distance, unitOfMeasure);
  }

  private double getAsMeters(double distance, LinearUnit unitOfMeasure) {

    double convertedDistance;

    if (unitOfMeasure == null) {
      return distance;
    }

    if (distance <= 0) {
      return 0.0;
    }

    switch (unitOfMeasure) {
      case FOOT_U_S:
        convertedDistance = USCustomary.FOOT.getConverterTo(SI.METRE).convert(distance);
        break;
      case YARD:
        convertedDistance = USCustomary.YARD.getConverterTo(SI.METRE).convert(distance);
        break;
      case MILE:
        convertedDistance = USCustomary.MILE.getConverterTo(SI.METRE).convert(distance);
        break;
      case NAUTICAL_MILE:
        convertedDistance = USCustomary.NAUTICAL_MILE.getConverterTo(SI.METRE).convert(distance);
        break;
      case KILOMETER:
        convertedDistance = distance * 1000.0d;
        break;
      case METER:
        convertedDistance = distance;
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid " + LinearUnit.class.getSimpleName() + " for conversion.");
    }

    return convertedDistance;
  }

  public double getAs(LinearUnit unitOfMeasure) {

    double result;

    if (unitOfMeasure == null) {
      return distanceInMeters;
    }

    if (distanceInMeters <= 0) {
      return 0.0;
    }

    switch (unitOfMeasure) {
      case METER:
        result = distanceInMeters;
        break;
      case KILOMETER:
        result = distanceInMeters / 1000.0d;
        break;
      case FOOT_U_S:
        result = SI.METRE.getConverterTo(USCustomary.FOOT).convert(distanceInMeters);
        break;
      case MILE:
        result = SI.METRE.getConverterTo(USCustomary.MILE).convert(distanceInMeters);
        break;
      case NAUTICAL_MILE:
        result = SI.METRE.getConverterTo(USCustomary.NAUTICAL_MILE).convert(distanceInMeters);
        break;
      case YARD:
        result = SI.METRE.getConverterTo(USCustomary.YARD).convert(distanceInMeters);
        break;
      default:
        LOGGER.debug("Could not convert distance units, assuming distance is in meters.");
        result = distanceInMeters;
        break;
    }

    return result;
  }

  /** Enumeration for the linear units supported by DDF. */
  public enum LinearUnit {
    METER("meter"),
    KILOMETER("kilometer"),
    NAUTICAL_MILE("nautical_mile", "nauticalmile"),
    MILE("mile"),
    FOOT_U_S("foot", "foot_u_s"),
    YARD("yard");

    private Set<String> stringRepresentationSet;

    /**
     * Initializes the enum constants with the list a strings that can be used when calling {@link
     * #fromString(String)}.
     *
     * @param stringRepresentations list of valid string representation for this enum constant. At
     *     least one string representation must be provided when defining {@link
     *     ddf.measure.Distance.LinearUnit} constant. The strings are case insensitive.
     */
    LinearUnit(String... stringRepresentations) {
      if (stringRepresentations == null || stringRepresentations.length == 0) {
        throw new IllegalArgumentException(
            "LinearUnit enumeration "
                + name()
                + " must be defined with at least one valid string representation");
      }

      this.stringRepresentationSet = new HashSet<>();

      for (String stringRepresentation : stringRepresentations) {
        this.stringRepresentationSet.add(stringRepresentation.toLowerCase());
      }
    }

    /**
     * Returns the {@link ddf.measure.Distance.LinearUnit} enum constant corresponding to the string
     * provided. It should be used as a replacement for the default {@code valueOf()} method.
     *
     * <p>This method supports all the string representations provided when the enum constants are
     * created. As opposed to {@code valueOf()}, this method is case-insensitive and will for
     * instance work with <i>nauticalmile</i>, <i>NAUTICALMILE</i> and <i>nauticalMile</i>.
     *
     * @param enumValueString string representing the enum constant to return. Cannot be {@code
     *     null} or empty.
     * @return enum constant corresponding to the string representation provided
     * @throws IllegalArgumentException thrown if the string provided doesn't map to any enum
     *     constant, is {@code null} or empty
     */
    public static LinearUnit fromString(String enumValueString) {
      notNull(enumValueString, "Enumeration string cannot be null");

      for (LinearUnit linearUnit : values()) {
        if (linearUnit.stringRepresentationSet.contains(enumValueString.toLowerCase())) {
          return linearUnit;
        }
      }

      throw new IllegalArgumentException(
          "Invalid enumeration string provided for LinearUnit: " + enumValueString);
    }
  }
}
