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
package ddf.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * WktStandard supports converting WKT into standard representations based on the WKT grammar in the
 * OpenGIS Simple Feature Access specification.
 *
 * <p>DDF normalizes MULTIPOINT geometries to the style defined by the WKT grammar. For example,
 * <code>MULTIPOINT ((0 0), (10 10))</code> is in the normalized format. Some systems require a
 * different format of MULTIPOINT. The previous example would denormalize as <code>
 * MULTIPOINT (0 0, 10 10)</code>
 */
public class WktStandard {

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final Pattern WKT_MULTIPOINT_PATTERN =
      Pattern.compile(
          "MULTIPOINT\\s*\\(((\\s*\\(\\s*\\-?\\d+(\\.\\d*)?\\s+\\-?\\d+(\\.\\d*)?\\s*\\)\\s*,?\\s*)+)\\)",
          Pattern.CASE_INSENSITIVE);

  /** Hiding class constructor since this is a utility class */
  private WktStandard() {}

  /**
   * Normalize the given WKT to conform to the WKT grammar.
   *
   * @param wkt WKT to normalize
   * @return normalized WKT
   */
  public static String normalize(String wkt) {
    if (StringUtils.isBlank(wkt)) {
      return wkt;
    }

    WKTReader wktReader = new WKTReader(GEOMETRY_FACTORY);
    try {
      // using JTS to normalize WKT into the correct format
      return wktReader.read(wkt).toText();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Cannot parse wkt.", e);
    }
  }

  /**
   * Denormalize the given WKT to support backwards compatibility.
   *
   * @param wkt wkt to denormalize
   * @return denormalized WKT
   */
  public static String denormalize(String wkt) {
    if (wkt == null) {
      return wkt;
    }

    Matcher matcher = WKT_MULTIPOINT_PATTERN.matcher(wkt);
    if (matcher.find()) {
      matcher.reset();
      StringBuffer resultWkt = new StringBuffer(wkt.length());
      while (matcher.find()) {
        String currentMultiPoint = matcher.group(0);
        String currentMultiPointText = matcher.group(1);

        matcher.appendReplacement(
            resultWkt,
            currentMultiPoint.replace(
                currentMultiPointText, currentMultiPointText.replaceAll("[\\(\\)]", "")));
      }
      matcher.appendTail(resultWkt);

      return resultWkt.toString();
    } else {
      return wkt;
    }
  }
}
