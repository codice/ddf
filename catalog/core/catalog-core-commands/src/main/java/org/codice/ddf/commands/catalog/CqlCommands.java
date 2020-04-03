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
package org.codice.ddf.commands.catalog;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

public abstract class CqlCommands extends CatalogCommands {

  public static final String DATE_FORMAT = "MM-dd-yyyy";

  public static final String DEFAULT_TEMPORAL_PROPERTY = Core.CREATED;

  private static final String MUTUALLY_EXCLUSIVE_OPTION_MESSAGE =
      " NOTE: Does not apply to CQL filters. Does not stack with other --lastXXXX options.";

  private static final String LASTXXXX_MESSAGE =
      MUTUALLY_EXCLUSIVE_OPTION_MESSAGE
          + " Smaller --lastXXXX time units take precedence over larger time units.";

  private static final String DATE_FORMATTING_MESSAGE =
      " Dates should be formatted as MM-dd-yyyy such as 06-10-2014.";

  private static final String SEARCH_PHRASE_OPTION_NAME = "--searchPhrase";

  @Option(
      name = "--cql",
      required = false,
      aliases = {"-cqlFilter"},
      multiValued = false,
      description =
          "Option to filter by metacards that match a CQL Filter expression. It is recommended to use the search command (catalog:search) first to see which metacards will be filtered.\n\n"
              + "CQL Examples:\n\t"
              + "Textual:   --cql \"title like 'some text'\"\n\t"
              + "Temporal:  --cql \"modified before 2012-09-01T12:30:00Z\"\n\t"
              + "Spatial:   --cql \"DWITHIN(location, POINT (1 2) , 10, kilometers)\"\n\t"
              + "Complex:   --cql \"title like 'some text' AND modified before 2012-09-01T12:30:00Z\"")
  String cqlFilter = null;

  @Option(
      name = "--temporal",
      required = false,
      aliases = {"-dt"},
      multiValued = false,
      description =
          "Option to use temporal criteria to filter. The default is to use \"keyword like "
              + WILDCARD
              + " \".")
  boolean isUseTemporal = false;

  @Option(
      name = "--temporalProperty",
      required = false,
      aliases = {"-tp"},
      multiValued = false,
      description =
          "Option to select which temporal property by which to filter with --XXXDate and--lastXXXX "
              + "options. Valid values include, but are not limited to, \""
              + Core.MODIFIED
              + "\", \""
              + Core.CREATED
              + "\", \""
              + Metacard.EFFECTIVE
              + "\", and \""
              + Core.EXPIRATION
              + "\". Defaults to \""
              + DEFAULT_TEMPORAL_PROPERTY
              + "\" if "
              + "not specified or input not recognized.")
  String temporalProperty;

  @Option(
      name = "--startDate",
      required = false,
      aliases = {"-start"},
      multiValued = false,
      description =
          "Flag to specify a start date range to by which to filter." + DATE_FORMATTING_MESSAGE)
  String startDate;

  @Option(
      name = "--endDate",
      required = false,
      aliases = {"-end"},
      multiValued = false,
      description =
          "Flag to specify a start date range to by which to filter." + DATE_FORMATTING_MESSAGE)
  String endDate;

  @Option(
      name = "--lastSeconds",
      required = false,
      aliases = {"-sec", "-seconds"},
      multiValued = false,
      description = "Option to filter by the last N seconds." + LASTXXXX_MESSAGE)
  int lastSeconds;

  @Option(
      name = "--lastMinutes",
      required = false,
      aliases = {"-min", "-minutes"},
      multiValued = false,
      description = "Option to filter by the last N minutes." + LASTXXXX_MESSAGE)
  int lastMinutes;

  @Option(
      name = "--lastHours",
      required = false,
      aliases = {"-h", "-hours"},
      multiValued = false,
      description = "Option to filter by the last N hours." + LASTXXXX_MESSAGE)
  int lastHours;

  @Option(
      name = "--lastDays",
      required = false,
      aliases = {"-d", "-days"},
      multiValued = false,
      description = "Option to filter by the last N days." + LASTXXXX_MESSAGE)
  int lastDays;

  @Option(
      name = "--lastWeeks",
      required = false,
      aliases = {"-w", "-weeks"},
      multiValued = false,
      description = "Option to filter by the last N weeks." + LASTXXXX_MESSAGE)
  int lastWeeks;

  @Option(
      name = "--lastMonths",
      required = false,
      aliases = {"-m", "-months"},
      multiValued = false,
      description = "Option to filter by the last N months." + LASTXXXX_MESSAGE)
  int lastMonths;

  @Option(
      name = SEARCH_PHRASE_OPTION_NAME,
      required = false,
      aliases = {"-phrase", "-like"},
      multiValued = false,
      description =
          "Option to filter by a specific phrase."
              + MUTUALLY_EXCLUSIVE_OPTION_MESSAGE
              + " --lastXXXX options take precedence over this option.")
  String searchPhrase = WILDCARD;

  @Option(
      name = "--caseSensitive",
      required = false,
      aliases = {"-case"},
      multiValued = false,
      description = "Option to set the " + SEARCH_PHRASE_OPTION_NAME + " to be case sensitive.")
  boolean caseSensitive = false;

  @Reference List<MetacardType> metacardTypes;

  private long filterCurrentTime;

  protected Filter getFilter() throws ParseException, CQLException {
    return getFilter(filterBuilder);
  }

  protected Filter getFilter(FilterBuilder filterBuilder) throws CQLException, ParseException {
    filterCurrentTime = System.currentTimeMillis();

    if (cqlFilter != null) {
      return ECQL.toFilter(cqlFilter);
    }

    final long start = getFilterStartTime();
    final long end = filterCurrentTime;
    final String temporalPropertyToFilter = getTemporalProperty();
    final SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);

    if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {
      return filterBuilder
          .attribute(temporalPropertyToFilter)
          .is()
          .during()
          .dates(formatter.parse(startDate), formatter.parse(endDate));
    } else if (start > 0 && end > 0) {
      return filterBuilder
          .attribute(temporalPropertyToFilter)
          .is()
          .during()
          .dates(new Date(start), new Date(end));
    } else if (isUseTemporal) {
      return filterBuilder.attribute(temporalPropertyToFilter).is().during().last(start);
    } else {
      if (caseSensitive) {
        return filterBuilder
            .attribute(Metacard.ANY_TEXT)
            .is()
            .like()
            .caseSensitiveText(searchPhrase);
      } else {
        return filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase);
      }
    }
  }

  protected long getFilterStartTime() {
    if (lastSeconds > 0) {
      return filterCurrentTime - TimeUnit.SECONDS.toMillis(lastSeconds);
    } else if (lastMinutes > 0) {
      return filterCurrentTime - TimeUnit.MINUTES.toMillis(lastMinutes);
    } else if (lastHours > 0) {
      return filterCurrentTime - TimeUnit.HOURS.toMillis(lastHours);
    } else if (lastDays > 0) {
      return filterCurrentTime - TimeUnit.DAYS.toMillis(lastDays);
    } else if (lastWeeks > 0) {
      Calendar weeks = GregorianCalendar.getInstance();
      weeks.setTimeInMillis(filterCurrentTime);
      weeks.add(Calendar.WEEK_OF_YEAR, -1 * lastWeeks);
      return weeks.getTimeInMillis();
    } else if (lastMonths > 0) {
      Calendar months = GregorianCalendar.getInstance();
      months.setTimeInMillis(filterCurrentTime);
      months.add(Calendar.MONTH, -1 * lastMonths);
      return months.getTimeInMillis();
    } else {
      return 0;
    }
  }

  protected String getTemporalProperty() {
    if (metacardTypes != null && StringUtils.isNotEmpty(temporalProperty)) {
      return metacardTypes.stream()
          .map(MetacardType::getAttributeDescriptors)
          .flatMap(Set::stream)
          .filter(this::isDateType)
          .map(AttributeDescriptor::getName)
          .filter(temporalProperty::equalsIgnoreCase)
          .findFirst()
          .orElse(DEFAULT_TEMPORAL_PROPERTY);
    }

    return DEFAULT_TEMPORAL_PROPERTY;
  }

  private Boolean isDateType(AttributeDescriptor attributeDescriptor) {
    return attributeDescriptor.getType().getAttributeFormat() == AttributeType.AttributeFormat.DATE;
  }

  protected Boolean hasFilter() {
    return isAnyNonNull(cqlFilter, startDate, endDate)
        || isAnyNotZero(lastSeconds, lastMinutes, lastHours, lastDays, lastWeeks, lastMonths);
  }

  private Boolean isAnyNonNull(String... parameters) {
    return Arrays.stream(parameters).anyMatch(Objects::nonNull);
  }

  private Boolean isAnyNotZero(int... parameters) {
    return Arrays.stream(parameters).anyMatch(p -> p > 0);
  }
}
