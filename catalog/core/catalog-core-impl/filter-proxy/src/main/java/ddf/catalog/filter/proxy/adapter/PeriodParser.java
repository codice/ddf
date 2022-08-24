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
package ddf.catalog.filter.proxy.adapter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.joda.time.Period;

/* Parses a period similar to ISO8601 format that contains decimal values. ISO8601 doesn't allow
decimal values so this class converts any decimal values to seconds and adds
them to the seconds field of the period. For example: "P8.2M" will become "P8MT525601S" */
public class PeriodParser {

  private PeriodParser() {}

  private enum TimeUnit {
    YEARS, // 0
    MONTHS, // 1
    WEEKS, // 2
    DAYS, // 3
    HOURS, // 4
    MINUTES, // 5
    SECONDS // 6
  }

  public static Period parse(String periodText, Pattern regex) {

    Matcher matcher = regex.matcher(periodText);
    if (matcher.matches()) {
      long[] unitDurationsInSeconds = getUnitDurationsInSeconds();
      BigDecimal[] fullDecimalValues = parseFullDecimalValues(periodText, matcher);
      int[] roundedValues = roundValuesDownToNearestInt(fullDecimalValues);
      int sumOfFractionalValuesInSeconds =
          sumOfFractionalValuesInSeconds(fullDecimalValues, unitDurationsInSeconds);

      Period period =
          new Period(
              roundedValues[0],
              roundedValues[1],
              roundedValues[2],
              roundedValues[3],
              roundedValues[4],
              roundedValues[5],
              roundedValues[6],
              0);

      return period.plusSeconds(sumOfFractionalValuesInSeconds);
    } else {
      throw new IllegalArgumentException("Period not in valid format: " + periodText);
    }
  }

  private static long[] getUnitDurationsInSeconds() {
    long[] unitDurationsInSeconds = new long[7];
    unitDurationsInSeconds[0] = ChronoUnit.YEARS.getDuration().getSeconds();
    unitDurationsInSeconds[1] = ChronoUnit.MONTHS.getDuration().getSeconds();
    unitDurationsInSeconds[2] = ChronoUnit.WEEKS.getDuration().getSeconds();
    unitDurationsInSeconds[3] = ChronoUnit.DAYS.getDuration().getSeconds();
    unitDurationsInSeconds[4] = ChronoUnit.HOURS.getDuration().getSeconds();
    unitDurationsInSeconds[5] = ChronoUnit.MINUTES.getDuration().getSeconds();
    unitDurationsInSeconds[6] = (long) 1; // seconds per second
    return unitDurationsInSeconds;
  }

  private static BigDecimal[] parseFullDecimalValues(String text, Matcher matcher) {
    BigDecimal[] fullDecimalValues = new BigDecimal[7];
    int unitNumber;
    String unitName;

    for (TimeUnit unit : TimeUnit.values()) {
      unitNumber = unit.ordinal();
      unitName = unit.toString().toLowerCase();
      fullDecimalValues[unitNumber] = parseNumber(text, matcher.group(unitNumber + 1), unitName);
    }
    return fullDecimalValues;
  }

  private static BigDecimal parseNumber(CharSequence text, String parsed, String errorText) {
    if (parsed == null) {
      return BigDecimal.ZERO;
    }
    try {
      return new BigDecimal(parsed);
    } catch (NumberFormatException ex) {
      throw new DateTimeParseException(
          "Text cannot be parsed to a Duration: " + errorText, text, 0, ex);
    }
  }

  private static int[] roundValuesDownToNearestInt(BigDecimal[] fullDecimalValues) {
    int[] roundedValues = new int[7];

    for (int i = 0; i < fullDecimalValues.length; i++) {
      roundedValues[i] = fullDecimalValues[i].setScale(0, RoundingMode.DOWN).intValue();
    }
    return roundedValues;
  }

  private static int sumOfFractionalValuesInSeconds(
      BigDecimal[] fullDecimalValues, long[] unitDurationsInSeconds) {
    BigDecimal fractionalValue;
    BigDecimal valueInSeconds;
    BigDecimal sum = BigDecimal.ZERO;

    for (int i = 0; i < fullDecimalValues.length; i++) {
      fractionalValue = fullDecimalValues[i].remainder(BigDecimal.ONE);
      valueInSeconds = fractionalValue.multiply(new BigDecimal(unitDurationsInSeconds[i]));
      sum = sum.add(valueInSeconds);
    }
    return sum.intValue();
  }
}
