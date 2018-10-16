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
package org.codice.ddf.catalog.ui.security;

import static org.boon.HTTP.APPLICATION_JSON;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.put;

import com.google.common.collect.ImmutableMap;
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
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.boon.json.JsonFactory;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class UserApplication implements SparkApplication {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserApplication.class);

  private final EndpointUtil util;

  private final PersistentStore persistentStore;

  private final SubjectIdentity subjectIdentity;

  public UserApplication(
      EndpointUtil util, PersistentStore persistentStore, SubjectIdentity subjectIdentity) {
    this.util = util;
    this.persistentStore = persistentStore;
    this.subjectIdentity = subjectIdentity;
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
              JsonFactory.create().parser().parseMap(util.safeGetBody(req));

          if (preferences == null) {
            preferences = new HashMap<>();
          }

          setUserPreferences(subject, preferences);

          return preferences;
        },
        util::getJson);

    exception(EntityTooLargeException.class, util::handleEntityTooLargeException);

    exception(IOException.class, util::handleIOException);

    exception(RuntimeException.class, util::handleRuntimeException);
  }

  private void setUserPreferences(Subject subject, Map<String, Object> preferences) {
    String json = JsonFactory.create().toJson(preferences);

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

        return JsonFactory.create().parser().parseMap(new String(json, Charset.defaultCharset()));
      }
    } catch (PersistenceException e) {
      LOGGER.info(
          "PersistenceException while trying to retrieve persisted preferences for user {}",
          userid,
          e);
    }

    return Collections.emptyMap();
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
}
