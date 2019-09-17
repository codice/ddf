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
package org.codice.ddf.catalog.ui.security.app;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.put;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.filter.FilterBuilder;
import ddf.security.Subject;
import ddf.security.SubjectIdentity;
import ddf.security.SubjectUtils;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.security.Constants;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.geotools.filter.text.ecql.ECQL;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class UserApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserApplication.class);

  private static final String MAX_PAGE_SIZE_PROPERTY = "catalog.maxPageSize";

  private static final Integer DEFAULT_MAX_PAGE_SIZE = 1000;

  public static final Integer MAX_PAGE_SIZE = determineAndRetrieveMaxPageSize();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private final EndpointUtil util;

  private final PersistentStore persistentStore;

  private final SubjectIdentity subjectIdentity;

  private final FilterBuilder filterBuilder;

  public UserApplication(
      EndpointUtil util,
      PersistentStore persistentStore,
      SubjectIdentity subjectIdentity,
      FilterBuilder filterBuilder) {
    this.util = util;
    this.persistentStore = persistentStore;
    this.subjectIdentity = subjectIdentity;
    this.filterBuilder = filterBuilder;
  }

  private static Integer determineAndRetrieveMaxPageSize() {
    return NumberUtils.toInt(System.getProperty(MAX_PAGE_SIZE_PROPERTY), DEFAULT_MAX_PAGE_SIZE);
  }

  @Override
  public void init() {
    get(
        "/user",
        (req, res) -> {
          Subject subject = (Subject) SecurityUtils.getSubject();
          res.type(APPLICATION_JSON);
          return getSubjectAttributes(subject);
        },
        util::getJson);

    put(
        "/user/preferences",
        APPLICATION_JSON,
        (req, res) -> {
          Subject subject = (Subject) SecurityUtils.getSubject();

          if (subject.isGuest()) {
            res.status(401);
            return ImmutableMap.of("message", "Guest cannot save preferences.");
          }

          Map<String, Object> preferences =
              GSON.fromJson(util.safeGetBody(req), MAP_STRING_TO_OBJECT_TYPE);

          if (preferences == null) {
            preferences = new HashMap<>();
          }

          setUserPreferences(subject, preferences);

          return preferences;
        },
        util::getJson);

    delete(
        "/user/notifications",
        APPLICATION_JSON,
        (req, res) -> {
          Subject subject = (Subject) SecurityUtils.getSubject();

          if (subject.isGuest()) {
            res.status(401);
            return ImmutableMap.of("message", "Guest cannot save notifications.");
          }

          Map<String, List<String>> notification =
              GSON.fromJson(util.safeGetBody(req), MAP_STRING_TO_OBJECT_TYPE);

          deleteNotifications(notification.get("alerts"));

          return "";
        },
        util::getJson);

    exception(EntityTooLargeException.class, util::handleEntityTooLargeException);

    exception(IOException.class, util::handleIOException);

    exception(RuntimeException.class, util::handleRuntimeException);
  }

  private void setUserPreferences(Subject subject, Map<String, Object> preferences) {
    String json = GSON.toJson(preferences);

    LOGGER.trace("preferences JSON text:\n {}", json);

    String userid = subjectIdentity.getUniqueIdentifier(subject);

    LOGGER.trace("Update preferences for: {}", userid);

    PersistentItem item = new PersistentItem();
    item.addIdProperty(userid);
    item.addProperty("user", userid);
    item.addProperty(
        "preferences_json",
        "_bin",
        Base64.getEncoder().encodeToString(json.getBytes(Charset.defaultCharset())));

    try {
      persistentStore.add(PersistenceType.PREFERENCES_TYPE.toString(), item);
    } catch (PersistenceException e) {
      LOGGER.info(
          "PersistenceException while trying to persist preferences for user {}", userid, e);
    }
  }

  private Set<String> getSubjectRoles(Subject subject) {
    return new TreeSet<>(SubjectUtils.getAttribute(subject, Constants.ROLES_CLAIM_URI));
  }

  private Map getSubjectPreferences(Subject subject) {
    String userid = subjectIdentity.getUniqueIdentifier(subject);

    try {
      String filter = String.format("user = '%s'", userid);
      List<Map<String, Object>> preferencesList =
          persistentStore.get(PersistenceType.PREFERENCES_TYPE.toString(), filter);
      if (preferencesList.size() == 1) {
        byte[] json = (byte[]) preferencesList.get(0).get("preferences_json_bin");

        Map<String, Object> attributes =
            GSON.fromJson(new String(json, Charset.defaultCharset()), MAP_STRING_TO_OBJECT_TYPE);
        attributes.put("alerts", getSubjectNotifications(subject));
        return attributes;
      }
    } catch (PersistenceException e) {
      LOGGER.info(
          "PersistenceException while trying to retrieve persisted preferences for user {}",
          userid,
          e);
    }

    return Collections.emptyMap();
  }

  private List<Map<String, Object>> getSubjectNotifications(Subject subject) {
    String userid = subjectIdentity.getUniqueIdentifier(subject);
    try {
      String filter = String.format("user = '%s'", userid);
      List<Map<String, Object>> notificationsList =
          persistentStore.get(
              PersistenceType.NOTIFICATION_TYPE.toString(), filter, 0, MAX_PAGE_SIZE);
      return notificationsList.stream().map(this::mapAttributes).collect(Collectors.toList());
    } catch (PersistenceException e) {
      LOGGER.debug(
          "PersistenceException while trying to retrieve persisted notifications for user {}",
          userid,
          e);
    }
    return Collections.emptyList();
  }

  private Map<String, Object> mapAttributes(Map<String, Object> persistentItem) {
    persistentItem = PersistentItem.stripSuffixes(persistentItem);
    /*
     *  The persistent store implementation only returns a collection if there is
     *  more than one item in the field, else it returns a string
     *  We need to check if it is a string, and if so add it to a collection
     */
    Object srcs = persistentItem.getOrDefault("src", Collections.emptySet());
    if (srcs instanceof String) {
      srcs = ImmutableSet.of((String) srcs);
    }

    Object metacardIds = persistentItem.getOrDefault("metacardIds", Collections.emptySet());
    if (metacardIds instanceof String) {
      metacardIds = ImmutableSet.of((String) metacardIds);
    }
    return new ImmutableMap.Builder<String, Object>()
        .put("id", persistentItem.get("id"))
        .put("src", srcs)
        .put("metacardIds", metacardIds)
        .put("queryId", persistentItem.getOrDefault("queryId", ""))
        .put("serverGenerated", persistentItem.getOrDefault("serverGenerated", "true"))
        .put("when", persistentItem.getOrDefault("when", DateTime.now().toInstant().getMillis()))
        .build();
  }

  private Map<String, Object> getSubjectAttributes(Subject subject) {
    // @formatter:off
    Map<String, Object> required =
        ImmutableMap.of(
            "userid", subjectIdentity.getUniqueIdentifier(subject),
            "username", SubjectUtils.getName(subject),
            "isGuest", subject.isGuest(),
            "roles", getSubjectRoles(subject),
            "preferences", getSubjectPreferences(subject));
    // @formatter:on

    String email = SubjectUtils.getEmailAddress(subject);

    if (StringUtils.isEmpty(email)) {
      return required;
    }

    return ImmutableMap.<String, Object>builder().putAll(required).put("email", email).build();
  }

  private void deleteNotifications(List<String> ids) {
    if (ids.isEmpty()) {
      LOGGER.debug("Received empty list of notification ids to delete");
      return;
    }
    List<Filter> idsToDelete =
        ids.stream()
            .map(id -> filterBuilder.attribute("id").is().equalTo().text(id))
            .collect(Collectors.toList());
    try {
      persistentStore.delete(
          PersistenceType.NOTIFICATION_TYPE.toString(),
          ECQL.toCQL(filterBuilder.anyOf(idsToDelete)));
    } catch (PersistenceException e) {
      LOGGER.debug(
          "PersistenceException while trying to delete persisted notifications with ids {} ",
          ids,
          e);
    }
  }
}
