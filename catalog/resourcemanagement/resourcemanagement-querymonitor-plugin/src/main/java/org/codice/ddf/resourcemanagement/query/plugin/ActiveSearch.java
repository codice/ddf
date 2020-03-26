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
package org.codice.ddf.resourcemanagement.query.plugin;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.Source;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.impl.SubjectUtils;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.geotools.filter.text.ecql.ECQL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ActiveSearch} provides relevant data pertaining to a search that is currently active (a
 * search that has not yet completed). Includes the client information {@link String}, CQL query
 * {@link String}, start time of the search {@link java.util.Date}, source being searched {@link
 * ddf.catalog.source.Source}, and uniqueID {@link java.util.UUID}
 */
public class ActiveSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(ActiveSearch.class);

  private String cqlQuery = "";

  private Date startTime;

  private Source source;

  private UUID uniqueID = null;

  private String clientInfo = "";

  public static final int USER_PAD_LENGTH = 20;

  public static final int SOURCE_PAD_LENGTH = 40;

  public static final int SEARCH_PAD_LENGTH = 33;

  public static final int TIME_PAD_LENGTH = 30;

  public static final int UUID_PAD_LENGTH = 37;

  public static final int USER_MAX_LENGTH = 20;

  public static final int SOURCE_MAX_LENGTH = 40;

  public static final int SEARCH_MAX_LENGTH = 33;

  public static final int TIME_MAX_LENGTH = 30;

  public static final int UUID_MAX_LENGTH = 37;

  public static final String FORMAT_STRING =
      "%1$-"
          + USER_PAD_LENGTH
          + "."
          + USER_MAX_LENGTH
          + "s %2$-"
          + SOURCE_PAD_LENGTH
          + "."
          + SOURCE_MAX_LENGTH
          + "s %3$-"
          + SEARCH_PAD_LENGTH
          + "."
          + SEARCH_MAX_LENGTH
          + "s %4$-"
          + TIME_PAD_LENGTH
          + "."
          + TIME_MAX_LENGTH
          + "s %5$-"
          + UUID_PAD_LENGTH
          + "."
          + UUID_MAX_LENGTH
          + "s %n";

  public String getCQL() {
    return cqlQuery;
  }

  /**
   * Returns a {@link Date} representing the approximate time this search started.
   *
   * @return {@link Date} representing the approximate time the search started.
   */
  public Date getStartTime() {
    return (Date) startTime.clone();
  }

  /**
   * Returns the source information for this {@link ActiveSearch}
   *
   * @return {@link Source} representing the source being queried by this {@link ActiveSearch}. Can
   *     return {@code null} when the object was created using a null source parameter to the
   *     constructor.
   */
  public Source getSource() {
    return source;
  }

  /**
   * Returns the {@link UUID} for this {@link ActiveSearch}
   *
   * @return {@link UUID} representing the unique identifier for this {@link ActiveSearch}.
   */
  public UUID getUniqueID() {
    return uniqueID;
  }

  /**
   * Returns the client information for this {@link ActiveSearch}
   *
   * @return {@link String} representing the client info that queried the {@link Source}.
   */
  public String getClientInfo() {
    return clientInfo;
  }

  /**
   * Constructor for an {@link ActiveSearch}.
   *
   * @param cqlQuery {@link String} that represents the query.
   * @param source {@link Source} that the search is querying.
   * @param uniqueID {@link UUID} a unique ID that is used to uniquely identify the search.
   */
  public ActiveSearch(String cqlQuery, Source source, UUID uniqueID, String clientInfo) {
    if (cqlQuery == null) {
      cqlQuery = "unknownCQLQuery";
      LOGGER.debug("CQLQuery for new ActiveSearch is null. Being set as 'unknownCQLQuery'.");
    }
    this.cqlQuery = cqlQuery;
    if (source == null) {
      LOGGER.debug("Source for new ActiveSearch is null.");
    }
    this.source = source;
    startTime = new Date();
    if (uniqueID == null) {
      this.uniqueID = UUID.randomUUID();
      LOGGER.debug("uniqueID for new ActiveSearch is null. Setting the ID as {}", uniqueID);
    } else {
      this.uniqueID = uniqueID;
    }
    if (clientInfo == null) {
      this.clientInfo = "unknownClient";
    } else {
      this.clientInfo = clientInfo;
    }
  }

  private static String getCqlFromQueryRequest(QueryRequest request) {
    String cqlQuery;
    if (request == null) {
      return "";
    } else {
      cqlQuery = ECQL.toCQL(request.getQuery());
    }
    return cqlQuery;
  }

  public String toFormattedString() {
    if (source == null) {
      return String.format(
          FORMAT_STRING,
          clientInfo,
          "Unknown Source",
          cqlQuery,
          startTime.toString(),
          uniqueID.toString());
    } else {
      return String.format(
          FORMAT_STRING,
          clientInfo,
          source.getId(),
          cqlQuery,
          startTime.toString(),
          uniqueID.toString());
    }
  }

  public ActiveSearch(Source source, QueryRequest request) {
    this(getCqlFromQueryRequest(request), source, UUID.randomUUID(), "client");
    if (request == null) {
      LOGGER.debug("QueryRequest in ActiveSearch Constructor was null.");
    } else {
      clientInfo =
          SubjectUtils.getName(
              (Subject) request.getPropertyValue(SecurityConstants.SECURITY_SUBJECT),
              "unknown",
              true);
      if (clientInfo.contains(",CN=")) {
        clientInfo = StringUtils.substringBetween(clientInfo, ",CN=", ",OU=");
      }
    }
  }
}
