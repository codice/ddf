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
package ddf.catalog.pubsub.predicate;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.pubsub.EventProcessorImpl.DateType;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteria;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;
import java.util.Date;
import java.util.Map;
import org.codice.ddf.platform.util.DateUtils;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemporalPredicate implements Predicate {
  private static final Logger LOGGER = LoggerFactory.getLogger(TemporalPredicate.class);

  private Date end;

  private Date start;

  private long offset;

  private DateType type;

  /**
   * Instantiates a new temporal predicate.
   *
   * @param start the start date
   * @param end the end date
   * @param type What date
   */
  public TemporalPredicate(Date start, Date end, DateType type) {
    if (end != null) {
      this.end = new Date(end.getTime());
    }
    if (start != null) {
      this.start = new Date(start.getTime());
    }
    this.offset = 0;
    this.type = type;
  }

  public TemporalPredicate(long offset, DateType type) {
    this.offset = offset;
    this.start = null;
    this.end = null;
    this.type = type;
  }

  public static boolean isTemporal(String startXML, String endXML) {
    return !startXML.isEmpty() && !endXML.isEmpty();
  }

  public boolean matches(Event properties) {
    LOGGER.debug("ENTERING: matches");

    TemporalEvaluationCriteria tec = null;
    Date date = null;

    Map<String, Object> contextualMap =
        (Map<String, Object>) properties.getProperty(PubSubConstants.HEADER_CONTEXTUAL_KEY);
    String operation = (String) properties.getProperty(PubSubConstants.HEADER_OPERATION_KEY);
    LOGGER.debug("operation = {}", operation);
    if (contextualMap != null) {
      String metadata = (String) contextualMap.get("METADATA");

      // If deleting a catalog entry and the entry's location data is NULL is only the word
      // "deleted" (i.e., the
      // source is deleting the catalog entry and did not send any location data with the
      // delete event), then
      // cannot apply any geospatial filtering - just send the event on to the subscriber
      if (PubSubConstants.DELETE.equals(operation)
          && PubSubConstants.METADATA_DELETED.equals(metadata)) {
        LOGGER.debug(
            "Detected a DELETE operation where metadata is just the word 'deleted', so send event on to subscriber");
        return true;
      }
    }

    Metacard entry = (Metacard) properties.getProperty(PubSubConstants.HEADER_ENTRY_KEY);
    if (entry != null) {
      LOGGER.debug("entry id: {}", entry.getId());

      switch (this.type) {
        case MODIFIED:
          LOGGER.debug("search by modified: {}", entry.getAttribute(Core.MODIFIED).getValue());
          date = (Date) entry.getAttribute(Core.MODIFIED).getValue();
          break;
        case METACARD_MODIFIED:
          LOGGER.debug(
              "search by metacard modified: {}", entry.getAttribute(Core.METACARD_MODIFIED));
          date = (Date) entry.getAttribute(Core.METACARD_MODIFIED).getValue();
          break;
        case EFFECTIVE:
          LOGGER.debug("search by effective: {}", entry.getEffectiveDate());
          date = entry.getEffectiveDate();
          break;
        case CREATED:
          LOGGER.debug("search by created: {}", entry.getAttribute(Core.CREATED).getValue());
          date = (Date) entry.getAttribute(Core.CREATED).getValue();
          break;
        case METACARD_CREATED:
          LOGGER.debug("search by metacard created: {}", entry.getAttribute(Core.METACARD_CREATED));
          date = (Date) entry.getAttribute(Core.METACARD_CREATED).getValue();
          break;
        case EXPIRATION:
          LOGGER.debug("search by expiration: {}", entry.getExpirationDate());
          date = entry.getExpirationDate();
          break;
        default:
          LOGGER.debug("unsupported type: {}", type);
          throw new IllegalArgumentException("Unsupported date type for TemporalPredicate");
      }

      if (offset > 0) {
        this.end = new Date();
        long startTimeMillis = end.getTime() - offset;
        this.start = new Date(startTimeMillis);

        LOGGER.debug("time period lowerBound = {}", start);
        LOGGER.debug("time period upperBound = {}", end);
      }
      tec = new TemporalEvaluationCriteriaImpl(end, start, date);
    }

    LOGGER.debug("EXITING: matches");

    return TemporalEvaluator.evaluate(tec);
  }

  public Date getEnd() {
    return DateUtils.copy(end);
  }

  public Date getStart() {
    return DateUtils.copy(start);
  }

  public DateType getType() {
    return type;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (start != null) {
      sb.append("\tstart = " + start.toString() + "\n");
    }

    // end date can be null for non-absolute (i.e., MODIFIED) criteria
    if (end != null) {
      sb.append("\tend = " + end.toString() + "\n");
    }

    sb.append("\toffset = " + Long.toString(offset) + "\n");
    sb.append("\t(DateType) type = " + type.toString() + "\n");

    return sb.toString();
  }
}
