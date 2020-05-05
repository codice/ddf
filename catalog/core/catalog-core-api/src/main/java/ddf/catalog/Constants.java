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
package ddf.catalog;

import ddf.catalog.data.Result;
import ddf.catalog.event.EventProcessor;

/** The Constants class is used to capture key values that can be re-used throughout DDF. */
public final class Constants {

  /** This constant is the name used to pass the metacard as part of a CatalogResponse. */
  public static final String METACARD_PROPERTY = "metacard";

  /**
   * This constant should be used to pass the users credentials in the form of a Subject object
   * through any request/response property map.
   */
  public static final String SUBJECT_PROPERTY = "subject";

  /** The Constant LOCAL_SITE_ID_VALUE. public final static String LOCAL_SITE_ID_VALUE = "local"; */
  /**
   * The constant SERVICE_SHORTNAME.
   *
   * @deprecated use {@link #SERVICE_ID}
   */
  public static final String SERVICE_SHORTNAME = "shortname";

  /**
   * The constant to be used by Catalog Services to register a unique name by which they can be
   * referred
   */
  public static final String SERVICE_ID = "id";

  /** The constant SERVICE_TITLE. */
  public static final String SERVICE_TITLE = "title";

  /** The constant SERVICE_DESCRIPTION. */
  public static final String SERVICE_DESCRIPTION = "description";

  /** The constant HTTP_INVOCATION_ABSOLUTE_PATH_URI */
  public static final String HTTP_INVOCATION_ABSOLUTE_PATH_URI = "http-absolute-path";

  // /** The Constant FACTORY_PID. */
  // public final static String FACTORY_PID = "factory-pid";

  /** The constant FEDERATED_SITE_STATE. */
  public static final String FEDERATED_SITE_STATE = "federated-site-state";

  /** The constant FEDERATED_SITE_STATE_ACTIVE. */
  public static final String FEDERATED_SITE_STATE_ACTIVE = "active";

  /** The constant FEDERATED_SITE_STATE_INACTIVE. */
  public static final String FEDERATED_SITE_STATE_INACTIVE = "inactive";

  /**
   * The constant EVENTS_ENTRY_NAME.
   *
   * @deprecated Use {@link EventProcessor#EVENT_METACARD} instead
   */
  public static final String EVENTS_ENTRY_NAME = EventProcessor.EVENT_METACARD;

  /**
   * The constant EVENTS_TIME_NAME.
   *
   * @deprecated Use {@link EventProcessor#EVENT_TIME} instead
   */
  public static final String EVENTS_TIME_NAME = EventProcessor.EVENT_TIME;

  /**
   * The constant EVENTS_TOPIC_CREATED.
   *
   * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_CREATED} instead
   */
  public static final String EVENTS_TOPIC_CREATED = EventProcessor.EVENTS_TOPIC_CREATED;

  /**
   * The constant EVENTS_TOPIC_UPDATED.
   *
   * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_UPDATED} instead
   */
  public static final String EVENTS_TOPIC_UPDATED = EventProcessor.EVENTS_TOPIC_UPDATED;

  /**
   * The constant EVENTS_TOPIC_DELETED.
   *
   * @deprecated Use {@link EventProcessor#EVENTS_TOPIC_DELETED} instead
   */
  public static final String EVENTS_TOPIC_DELETED = EventProcessor.EVENTS_TOPIC_DELETED;

  /** The constant DEFAULT_PAGE_SIZE. */
  public static final Integer DEFAULT_PAGE_SIZE = 20;

  /**
   * The constant DEFAULT_START_INDEX for the default index to start query results with in a query
   * response.
   */
  public static final Integer DEFAULT_START_INDEX = 0;

  /** @deprecated Use {@link Result#RELEVANCE} instead */
  public static final String SORT_POLICY_VALUE_RELEVANCE = Result.RELEVANCE;

  /** @deprecated Use {@link Result#DISTANCE} instead */
  public static final String SORT_POLICY_VALUE_DISTANCE = Result.DISTANCE;

  /** @deprecated Use the appropriate temporal field from {@link ddf.catalog.data.Metacard} */
  public static final String SORT_POLICY_VALUE_TEMPORAL = "TEMPORAL";

  public static final String SUBSCRIPTION_KEY = "subscription";

  /** Constant for the String representation of the MIME type for a JPEG image */
  public static final String MIME_TYPE_JPEG = "image/jpeg";

  public static final String OAUTH_RESOURCE_OWNER_USERNAME = "resource.owner.username";

  public static final String INGEST_LOGGER_NAME = "ingestLogger";

  public static final String REMOTE_DESTINATION_KEY = "remote-destination";

  public static final String LOCAL_DESTINATION_KEY = "local-destination";

  public static final String OPERATION_TRANSACTION_KEY = "operation-transaction";

  public static final String CONTENT_PATHS = "content-paths";

  public static final String ATTRIBUTE_OVERRIDES_KEY = "attributeOverrides";

  public static final String ATTRIBUTE_UPDATE_MAP_KEY = "attributeUpdateMap";

  public static final String STORE_REFERENCE_KEY = "storeReference";

  public static final String EXPERIMENTAL_FACET_PROPERTIES_KEY = "facet-properties";

  public static final String EXPERIMENTAL_FACET_RESULTS_KEY = "facet-results";

  public static final String QUERY_HIGHLIGHT_KEY = "highlight";

  public static final String SUGGESTION_QUERY_KEY = "suggestion-query";

  public static final String SUGGESTION_CONTEXT_KEY = "suggestion-context";

  public static final String SUGGESTION_DICT_KEY = "suggestion-dictionary";

  public static final String SUGGESTION_RESULT_KEY = "suggestion-result";

  public static final String SUGGESTION_BUILD_KEY = "suggestion-build";

  public static final String ADDITIONAL_SORT_BYS = "additional-sort-bys";
}
